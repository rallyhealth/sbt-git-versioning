import java.io.{File, FileOutputStream, FileWriter, OutputStream, PrintStream, PrintWriter}
import java.lang.reflect.Method

import sbt.Def.Initialize
import sbt.Keys._
import sbt._
import sbt.classpath.ClasspathUtilities
import sbt.complete.{DefaultParsers, Parser}

import scala.util.Try

/**
  * A copy-and-paste of `sbt.ScriptedPlugin` (https://github.com/sbt/sbt/blob/0.13/scripted/plugin/src/main/scala/sbt/ScriptedPlugin.scala)
  * with one MAIN purpose: allow the `scripted` test to capture the FULL output of the SBT console. All logic to do
  * this is in [[ScriptedPluginCustomizations]] so it should be easy to find. Read the comment on that class
  * for all the nasty details.
  *
  * There are also some smaller, non-functional changes:
  * - Clarifying comments added.
  * - Fixed all deprecated syntax.
  * - Fixed minor warnings.
  * - Removed `scriptedBufferLog` (because it would break [[ScriptedPluginCustomizations]]).
  * - Added comments cause there's a lot of confusing stuff and the original is comment bare.
  *
  * I did not want to copy and paste `sbt.ScriptedPlugin` but I could not get a valid reference to override. I tried
  * many variants of:
  * {{{
  *   libraryDependencies += "org.scala-sbt" % "scripted-plugin" % sbtVersion.value
  *   ...
  *   object GitVersioningScriptedPlugin extends sbt.ScriptedPlugin {
  *     override def scriptedTask: Initialize[InputTask[Unit]] = Def.inputTask {
  *   }
  * }}}
  * but no matter what I tried it never seemed to take.
  */
object GitVersioningScriptedPlugin extends Plugin {
  def scriptedConf: Configuration = config("scripted-sbt") hide

  def scriptedLaunchConf: Configuration = config("scripted-sbt-launch") hide

  val scriptedSbt: SettingKey[String] = SettingKey[String]("scripted-sbt")
  val sbtLauncher: TaskKey[File] = TaskKey[File]("sbt-launcher")

  /** The root directory for the `scripted` tests, typically "src/sbt-test" */
  val sbtTestDirectory: SettingKey[File] = SettingKey[File]("sbt-test-directory")

  val scriptedClasspath: TaskKey[PathFinder] = TaskKey[PathFinder]("scripted-classpath")
  /** AnyRef is actually `sbt.test.ScriptedRunner` */
  val scriptedTests: TaskKey[AnyRef] = TaskKey[AnyRef]("scripted-tests")
  val scriptedRun: TaskKey[Method] = TaskKey[Method]("scripted-run")
  val scriptedLaunchOpts: SettingKey[Seq[String]] = SettingKey[Seq[String]](
    "scripted-launch-opts", "options to pass to jvm launching scripted tasks")
  val scriptedDependencies: TaskKey[Unit] = TaskKey[Unit]("scripted-dependencies")
  val scripted: InputKey[Unit] = InputKey[Unit]("scripted")

  def scriptedTestsTask: Initialize[Task[AnyRef]] = Def.task {
    val loader = ClasspathUtilities.toLoader(scriptedClasspath.value, scalaInstance.value.loader)
    ModuleUtilities.getObject("sbt.test.ScriptedTests", loader)
  }

  def scriptedRunTask: Initialize[Task[Method]] = scriptedTests map {
    (m) =>
      // handle to the `run()` on a `sbt.ScriptedRunner` instance for dynamic invoking. Interestingly the method that
      // is returned is deprecated saying "No longer used, 0.13.9".
      // https://github.com/sbt/sbt/blob/0.13/scripted/sbt/src/main/scala/sbt/test/ScriptedTests.scala#L123
      m.getClass.getMethod(
        "run", classOf[File], classOf[Boolean], classOf[Array[String]], classOf[File], classOf[Array[String]])
  }

  private def scriptedParser(scriptedBase: File): Parser[Seq[String]] = {
    import DefaultParsers._
    val pairs = (scriptedBase * AllPassFilter * AllPassFilter * "test").get map { (f: File) =>
      val p = f.getParentFile
      (p.getParentFile.getName, p.getName)
    }
    val pairMap = pairs.groupBy(_._1).mapValues(_.map(_._2).toSet)

    val id = charClass(c => !c.isWhitespace && c != '/').+.string
    val groupP = token(id.examples(pairMap.keySet)) <~ token('/')

    def nameP(group: String) = token("*".id | id.examples(pairMap(group)))

    val testID = for (group <- groupP; name <- nameP(group)) yield (group, name)
    (token(Space) ~> matched(testID)).*
  }

  def scriptedTask: Initialize[InputTask[Unit]] = Def.inputTask {
    val args = scriptedParser(sbtTestDirectory.value).parsed
    // disable buffering or else we won't have the log on disk to check
    val scriptedBufferLog = false

    val prereq: Unit = scriptedDependencies.value

    val log = streams.value.log
    val origStreamsData = ScriptedPluginCustomizations.writeStdOutToDisk(log, sbtTestDirectory.value)
    try {
      // dynamically invokes `sbt.test.ScriptedRunner.run()
      // https://github.com/sbt/sbt/blob/0.13/scripted/sbt/src/main/scala/sbt/test/ScriptedTests.scala#L123
      scriptedRun.value.invoke(
        scriptedTests.value, sbtTestDirectory.value, scriptedBufferLog: java.lang.Boolean,
        args.toArray, sbtLauncher.value, scriptedLaunchOpts.value.toArray)
    } catch {
      case e: java.lang.reflect.InvocationTargetException => throw e.getCause
    } finally {
      origStreamsData.cleanUp()
    }
  }

  val scriptedSettings = Seq(
    ivyConfigurations ++= Seq(scriptedConf, scriptedLaunchConf),
    scriptedSbt := sbtVersion.value,
    sbtLauncher := getJars(scriptedLaunchConf).map(_.get.head).value,
    sbtTestDirectory := sourceDirectory.value / "sbt-test",
    libraryDependencies ++= Seq(
      "org.scala-sbt" % "scripted-sbt" % scriptedSbt.value % scriptedConf.toString,
      "org.scala-sbt" % "sbt-launch" % scriptedSbt.value % scriptedLaunchConf.toString
    ),
    scriptedClasspath := getJars(scriptedConf).value,
    scriptedTests := scriptedTestsTask.value,
    scriptedRun := scriptedRunTask.value,
    scriptedDependencies := {
      (compile in Test).value
      publishLocal.value
    },
    scriptedLaunchOpts := Seq(),
    scripted := scriptedTask.evaluated
  )

  private[this] def getJars(config: Configuration): Initialize[Task[PathFinder]] = Def.task {
    PathFinder(Classpaths.managedJars(config, classpathTypes.value, update.value).map(_.data))
  }
}

/**
  * Holds all additional and/or customized logic for [[GitVersioningScriptedPlugin]] so it should be easy to find.
  *
  * The MAIN purpose of this class is to allow the `scripted` test to capture the FULL output of the SBT console.
  *
  * But that's not straightforward. The `sbt.test.ScriptedRunner` creates its own [[ConsoleLogger]] (with no way to
  * substitute your own -- [[https://github.com/sbt/sbt/blob/0.13/scripted/sbt/src/main/scala/sbt/test/ScriptedTests.scala#L123]]).
  * That [[ConsoleLogger]] references the standard [[System.out]] and [[System.err]] ([[https://github.com/sbt/sbt-zero-thirteen/blob/0.13/util/log/src/main/scala/sbt/ConsoleLogger.scala#L144]])
  * and is passed along to the `sbt.test.SbtRunner` which runs the `scripted` test in a separate SBT instance but
  * re-uses the same STDOUT and STDERR streams ([[https://github.com/sbt/sbt/blob/0.13/scripted/sbt/src/main/scala/sbt/test/SbtHandler.scala#L68]]).
  *
  * Ideally I could have updated [[extraLoggers]] in the build.sbt of `scripted` test to capture this information
  * on [[initialize]] but I discovered that doesn't actually give you all the information, it gives you some, and
  * only the output from tasks that were properly written to use the [[LogManager]] (which is what [[extraLoggers]]
  * effects). Unfortunately that's inconsistently used withing SBT (big shocker).
  *
  * So, rather than re-writing ALL of the `scripted-plugin` (to both capture that [[ConsoleLogger]] AND find a way
  * to get it to the new SBT process) I took the approach of buffering the STDOUT and STDERR used by the `scripted`
  * task into a file that the `scripted` SBT process can read.
  *
  * Capturing the STDOUT and STDERR is relatively straightforward, but how do I tell the `scripted` SBT where that
  * file is? The `scripted` SBT is not run in the directory its defined in. A new temporary directory is created
  * and the contents of the `scripted` test are copied to the new directory by `sbt.test.ScriptedTests` using
  * [[Resources.readWriteResourceDirectory()]]. So before that copy occurs we create a file in the `scripted` test
  * directory that contains the full path of the file we buffered the STDOUT and STDERR into.
  *
  * When the `scripted` test starts up it can use its [[initialize]] hook to find the file, read the buffered STDOUT
  * and STDERR file, and read it when requested.
  */
object ScriptedPluginCustomizations {

  /**
    * Contains all information about the file that contains the buffered STDOUT and STDERR that the `scripted` test
    * will read.
    */
  private case class BufferedStreamData(
    sideChannelFiles: Seq[File], bufferedFile: File, bufferedFileStream: FileOutputStream) {

    def this(sideChannelFiles: Seq[File], bufferedFile: File) =
      this(sideChannelFiles, bufferedFile, new FileOutputStream(bufferedFile, true))

    /**
      * Saves the current STDOUT and STDERR in a [[OriginalStreamData]], sets the [[System.out]] and [[System.err]] to a
      * new stream that writes to [[bufferedFile]], and returns the saved [[OriginalStreamData]].
      */
    def replace(): OriginalStreamData = {

      val originalStreams = new OriginalStreamData(this)

      def replaceImpl(getFn: => PrintStream, setFn: PrintStream => Unit): Unit = {
        // we want to flush in two places -- as soon as we get the data from the original PrintStream, and then
        // again to the on-disk file
        val newStream = new PrintStream(new FlushingOutputStream(
          new TeeOutputStream(Seq(getFn, new FlushingOutputStream(bufferedFileStream)))))
        setFn(newStream)
      }

      replaceImpl(System.out, System.setOut)
      replaceImpl(System.err, System.setErr)
      replaceImpl(scala.Console.out, scala.Console.setOut)
      replaceImpl(scala.Console.err, scala.Console.setErr)

      originalStreams
    }

    /**
      * Should be called to close the [[bufferedFileStream]] and delete the [[bufferedFile]], typically by
      * [[OriginalStreamData]]. This method will not throw.
      */
    def cleanUp(): Unit = {
      sideChannelFiles.foreach(file => if (file.exists) Try(file.delete()))
      Try(bufferedFileStream.close())
      // NOTE: we don't delete the log because its useful for debugging
    }
  }

  /**
    * Contains the original [[System.out]] and [[System.err]] values, first captured by [[writeStdOutToDisk()]]
    *
    * When done with this call [[cleanUp()]].
    */
  class OriginalStreamData(
    sysOut: PrintStream, sysErr: PrintStream, conOut: PrintStream, conErr: PrintStream, bufferedStreams: BufferedStreamData) {

    def this(bufferedStreams: BufferedStreamData) =
      this(System.out, System.err, scala.Console.out, scala.Console.err, bufferedStreams)

    def bufferedFile: File = bufferedStreams.bufferedFile

    /** Resets the original STDOUT and STDERR and cleans up the [[BufferedStreamData]]. This method will not throw. */
    def cleanUp(): Unit = {
      Try(System.setOut(sysOut))
      Try(System.setErr(sysErr))
      Try(scala.Console.setOut(conOut))
      Try(scala.Console.setErr(conErr))
      bufferedStreams.cleanUp()
    }
  }

  /**
    * A specialized [[OutputStream]] that flushes after each operation, so avoid problems where we search for something
    * in the log but it hasn't been flushed to disk yet.
    */
  private class FlushingOutputStream(underlying: OutputStream) extends OutputStream {

    override def write(b: Int): Unit = {
      underlying.write(b)
      underlying.flush()
    }

    override def write(b: Array[Byte]): Unit = {
      underlying.write(b)
      underlying.flush()
    }

    override def write(b: Array[Byte], off: Int, len: Int): Unit = {
      underlying.write(b, off, len)
      underlying.flush()
    }

    override def flush(): Unit = {
      underlying.flush()
      underlying.flush()
    }

    override def close(): Unit = {
      underlying.close()
      underlying.flush()
    }
  }

  /**
    * Similar to the unix tool "tee" and the various `TeeInputStream` implementations, this simply sends the output
    * to all underlying [[OutputStream]]s.
    */
  private class TeeOutputStream(underlying: Seq[OutputStream]) extends OutputStream {

    override def write(b: Int): Unit = underlying.foreach(_.write(b))

    override def write(b: Array[Byte]): Unit = underlying.foreach(_.write(b))

    override def write(b: Array[Byte], off: Int, len: Int): Unit = underlying.foreach(_.write(b, off, len))

    override def flush(): Unit = underlying.foreach(_.flush())

    override def close(): Unit = underlying.foreach(_.close())
  }

  private object DirectoryFilter extends FileFilter {
    override def accept(file: File): Boolean = file.isDirectory
  }

  /**
    * Saves the current STDOUT and STDERR, sets the [[System.out]] and [[System.err]] to copy the data to a temporary
    * file, stores that file name in a file in the `scripted` test's root directory, and returns the saved
    * STDOUT and STDERR.
    *
    * @param sbtTestDirectory The value of [[GitVersioningScriptedPlugin.sbtTestDirectory]]
    */
  def writeStdOutToDisk(log: Logger, sbtTestDirectory: File): OriginalStreamData = {

    val bufferedFile = File.createTempFile("scriptedOutput-", ".log.txt")
    val sideChannelFiles = createSideChannelFiles(sbtTestDirectory, bufferedFile)
    val bufferedStreams = new BufferedStreamData(sideChannelFiles, bufferedFile)

    sideChannelFiles.foreach(
      f => log.debug("[GitVersioningScriptedPlugin] BufferedStreamData.sideChannelFile: " + f.getAbsolutePath))
    log.info("[GitVersioningScriptedPlugin] BufferedStreamData.bufferedFile: " + bufferedFile.getAbsolutePath)

    bufferedStreams.replace()
  }

  /**
    * This writes the side channel file into EACH group/test directory. Every group/test directory needs its own file
    * because we do not know which test the user is running -- they might run all, only a single group, or a specific
    * group and test.
    *
    * A test directory is one that that contains a "test" file -- that file name is hard coded in
    * `sbt.test.ScriptedTests` https://github.com/sbt/sbt/blob/0.13/scripted/sbt/src/main/scala/sbt/test/ScriptedTests.scala#L19
    *
    * @param sbtTestDirectory The value of [[GitVersioningScriptedPlugin.sbtTestDirectory]]
    * @return Side channel files, one for each group/test directory, so the can be cleaned up by [[BufferedStreamData]].
    */
  private def createSideChannelFiles(sbtTestDirectory: File, bufferedFile: File): Seq[File] = {

    // we append the milliseconds to make the file name unique and easy to find the latest file
    val fileName = s"scriptedOutput-${System.currentTimeMillis()}.link.txt"

    sbtTestDirectory
      .listFiles(DirectoryFilter)
      .flatMap(_.listFiles(DirectoryFilter))
      .map { groupTestDir =>

        val sideChannelFile = new File(groupTestDir, fileName)
        Try(sideChannelFile.deleteOnExit())

        val sideChannelFileWriter = new PrintWriter(new FileWriter(sideChannelFile, false))
        sideChannelFileWriter.println(bufferedFile.getAbsolutePath)
        sideChannelFileWriter.close()

        sideChannelFile
      }
  }
}
