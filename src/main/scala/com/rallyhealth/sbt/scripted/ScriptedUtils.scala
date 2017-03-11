package com.rallyhealth.sbt.scripted

import java.io._
import java.util.concurrent.atomic.AtomicReference
import java.util.regex.Pattern

import sbt.Logger

import scala.util.Try

/**
  * Implementation of the various assert-style methods that are used by the "test" script. This uses a
  * [[BufferedLog]] as the source of the data that this will assert against.
  *
  * This class lives within the `git-versioning-sbt-plugin` project and not in the `scripted` test code because IntelliJ
  * correctly recognizes these files, unlike the files within the `scripted` test code.
  */
object ScriptedUtils {

  private case class TestResult(pass: Boolean, failMessage: String)

  private val bufferedLog = new BufferedLog

  /** The name of the current test being run, which is changed by calls to [[startTest()]]. */
  private val currentTestName = new AtomicReference[String]("")

  /**
    * Finds the side-channel file created by `GitVersioningScriptedPlugin`, reads the STDOUT and STDERR buffered file path
    * from the side-channel file, and opens a [[BufferedReader]] on that file path.
    */
  def initialize(): Unit = bufferedLog.initialize()

  /**
    * Starts a new test, clearing the buffered log data and recording the test name. This should be called before EVERY
    * test so that your output will be clearly marked with the test name. (AFAIK scripted does not support such a
    * construct, which makes the output hard to read.)
    *
    * @param args One word which is the name of the test
    */
  def startTest(log: Logger, args: Seq[String]): Unit = args match {
    case seq if seq.isEmpty => sys.error("no name provided")
    case seq =>
      val testName = seq.mkString(" ").trim

      if (bufferedLog.findTestNames().contains(testName))
        throw new IllegalArgumentException("Duplicate test found: " + testName)

      log.info(BufferedLog.StartTestLogEvent.forTestName(testName))
      currentTestName.set(testName)
  }

  /**
    * Applies changes to the code by copying one of the "Equestria???.scala" files over "Equestria.scala" in the
    * source tree and renaming the class to "Equestria". This creates changes that can be vetted with SemVerPlugin.
    *
    * THIS METHOD ASSUMES THE DIRECTORY LOOKS LIKE semver/log. Feel free to generalize, but just letting you know
    * what you need to know to do that.
    *
    * @param args One word which is one of the following: major, minor, patch, or original
    */
  def applyChange(log: Logger, args: Seq[String]): Unit = {
    val changeType = args.mkString(" ")

    val workingDir = java.nio.file.Paths.get("src/main/scala/mlp").toAbsolutePath.normalize().toFile
    require(workingDir.exists())

    val sourceFileName = changeType.toLowerCase match {
      case "major" => "EquestriaMajor.scala"
      case "minor" => "EquestriaMinor.scala"
      case "patch" => "EquestriaPatch.scala"
      case "original" => "EquestriaOriginal.scala"
      case _ => sys.error(s"unexpected changeType=$changeType")
    }

    val reader = new BufferedReader(new FileReader(new File(workingDir, sourceFileName)))
    val writer = new PrintWriter(new FileWriter(new File(workingDir, "Equestria.scala")))
    try {
      Iterator
        .continually(reader.readLine())
        .takeWhile(_ != null)
        .map(_.replaceFirst("^class Equestria\\w*", "class Equestria"))
        .foreach(writer.println)
    } finally {
      Try(reader.close())
      Try(writer.close())
    }
  }

  /**
    * Greps each line of the log using the provided regex.
    *
    * HINT: Don't use "not" with complex patterns -- if the pattern is complex its easy to get wrong, and then you
    * could get false positives.
    *
    * @param cmdArgs Follows the format "testName [not] regex". If the second word is "not" then the test checks that
    * the regex does not match any line in the log. (The "not" command is here because I did not get the expected
    * success when removing "not" and using the -> negating command in the script. (shrug)
    */
  def grepLog(log: Logger, cmdArgs: Seq[String]): Unit = {

    def impl(): TestResult = {
      val (include, pattern) = cmdArgs.toList match {
        case args if args.isEmpty => sys.error(s"invalid command: ${cmdArgs.mkString(" ")}")
        case List(not, args @ _*) if not.toLowerCase == "not" => (false, Pattern.compile(args.mkString(" ")))
        case args => (true, Pattern.compile(args.mkString(" ")))
      }

      require(currentTestName.get().nonEmpty, "no test name; did you run 'reload' after 'startTest'?")

      val lines = bufferedLog.readLogLines(currentTestName.get)

      lazy val errorSuffix = makeErrorSuffix(lines, pattern.pattern())

      if (include)
        TestResult(lines.exists(line => pattern.matcher(line).find()), s"Could not find $errorSuffix")
      else
        TestResult(lines.forall(line => !pattern.matcher(line).find()), s"Found unwanted $errorSuffix")
    }

    repeatTest(() => impl())
  }

  /**
    * Searches each line of the log for the provided phrase.
    *
    * @param cmdArgs Follows the format "testName [not] value". If the second word is "not" then the test checks that
    * the value does not exist in any line in the log. (The "not" command is here because I did not get the expected
    * success when removing "not" and using the -> negating command in the script. (shrug)
    */
  def searchLog(log: Logger, cmdArgs: Seq[String]): Unit = {

    def impl(): TestResult = {
      val (include, value) = cmdArgs.toList match {
        case args if args.isEmpty => sys.error(s"invalid command: ${cmdArgs.mkString(" ")}")
        case List(not, args @ _*) if not.toLowerCase == "not" => (false, args.mkString(" "))
        case args => (true, args.mkString(" "))
      }

      val lines = bufferedLog.readLogLines(currentTestName.get)

      lazy val errorSuffix = makeErrorSuffix(lines, value)

      if (include)
        TestResult(lines.exists(line => line.contains(value)), s"Could not find $errorSuffix")
      else
        TestResult(lines.forall(line => !line.contains(value)), s"Found unwanted $errorSuffix")
    }

    repeatTest(() => impl())
  }

  /**
    * Repeats the provided test several times if it fails. This is necessary for log based tests logs tend to buffer
    * in memory. The streams have a `FlushingOutputStream` added to try and flush ASAP but that is no guarantee.
    */
  private def repeatTest(testFn: () => TestResult): Unit = {
    val TestAttempts = 3
    val TestAttemptSleepMs = 50
    var delay = 0
    val finalResult =
      (0 until TestAttempts).foldLeft(TestResult(pass = false, "")) {
        // if previous attempt succeeded do nothing
        case (prevResult, _) if prevResult.pass =>
          prevResult

        // if previous attempt failed, wait (for the log to flush to disk), bump the delay, and re-attempt
        case (_, _) =>
          Thread.sleep(delay)
          delay += TestAttemptSleepMs
          testFn()
      }

    scala.Predef.assert(finalResult.pass, finalResult.failMessage)
  }

  /** Makes a pretty suffix for error messages. */
  private def makeErrorSuffix(lines: Seq[String], value: String): String = {
    val prettyLinesForError = lines
      .zipWithIndex
      .takeRight(10)
      .map { case (l, i) => s"    [${i + 1}/${lines.size}] $l" }
      .mkString("\n", "\n", "")
    val filePath = bufferedLog.bufferedFile.getAbsolutePath
    s"'$value' for test '${currentTestName.get}' in log $filePath:$prettyLinesForError"
  }

  /**
    * Prints out the name of all tests run. This can be handy for seeing which tests were run, and finding poorly
    * named or duplicate tests.
    */
  def listTestNames(log: Logger): Unit = {
    val testNames = bufferedLog.findTestNames()
    val count = testNames.size
    log.info(s"Listing previously run test names, total=$count")
    testNames.zipWithIndex.foreach {
      case (name, index) => log.info(s"[${index + 1}/$count] $name")
    }
  }

  /**
    * Searches given file name and performs a `replaceAll()`
    *
    * @param args Follows the format "file regex replacement".
    */
  def replaceAllInFile(log: Logger, args: Seq[String]): Unit = {
    val workingDir = java.nio.file.Paths.get("").toAbsolutePath.normalize().toFile
    require(workingDir.exists())

    val (sourceFile, regex, replacement) = args.toList match {
      case List(file, rex, rep) => (new File(workingDir, file), rex, rep)
      case _ => sys.error(s"invalid command: ${args.mkString(" ")}")
    }

    val destFile = new File(workingDir, sourceFile.getName + ".changed")

    val reader = new BufferedReader(new FileReader(sourceFile))
    val writer = new PrintWriter(new FileWriter(destFile))
    try {
      Iterator
        .continually(reader.readLine())
        .takeWhile(_ != null)
        .map(_.replaceAll(regex, replacement))
        .foreach(writer.println)
    } finally {
      Try(reader.close())
      Try(writer.close())
    }

    java.nio.file.Files.move(destFile.toPath, sourceFile.toPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
  }

  /** Searches for messages that indicate a config problem */
  def assertConfigError(log: Logger): Unit = {
    searchLog(log, Seq("not", "UNRESOLVED DEPENDENCIES")) // indicate scripted didn't finish before running the script
    searchLog(log, Seq("Config problem"))
    // config issues should be determined before we decide to enable/disable SemVer
    searchLog(log, Seq("not", "Checking DISABLED"))
    searchLog(log, Seq("not", "Checking ENABLED"))
    // config issues are handled as exceptions
    searchLog(log, Seq("[error]"))
    searchLog(log, Seq("java.lang.Illegal"))
    // config issues should always block publishing
    searchLog(log, Seq("not", "published"))
  }

  /** Searches for messages that indicate SemVer was disabled before running. */
  def assertDisabled(log: Logger): Unit = {
    searchLog(log, Seq("not", "UNRESOLVED DEPENDENCIES")) // indicate scripted didn't finish before running the script
    searchLog(log, Seq("Checking DISABLED"))
    searchLog(log, Seq("not", "Check aborted"))
    searchLog(log, Seq("not", "Check starting"))
    searchLog(log, Seq("not", "failing build"))
    searchLog(log, Seq("not", "[error]"))
    searchLog(log, Seq("not", "java.lang.Illegal"))
  }

  /** Searches for messages that indicate SemVer was aborted before running. */
  def assertAborted(log: Logger): Unit = {
    searchLog(log, Seq("not", "UNRESOLVED DEPENDENCIES")) // indicate scripted didn't finish before running the script
    searchLog(log, Seq("Checking ENABLED"))
    searchLog(log, Seq("Check aborted"))
    searchLog(log, Seq("not", "PASSED"))
    searchLog(log, Seq("not", "FAILED"))
    searchLog(log, Seq("not", "failing build"))
    searchLog(log, Seq("not", "[error]"))
    searchLog(log, Seq("not", "java.lang.Illegal"))
  }

  /** Searches for messages that indicate SemVer ran and passed */
  def assertPassed(log: Logger, args: Seq[String]): Unit = {
    searchLog(log, Seq("not", "UNRESOLVED DEPENDENCIES")) // indicate scripted didn't finish before running the script
    searchLog(log, Seq("Checking ENABLED"))
    searchLog(log, Seq("Check type"))
    searchLog(log, Seq("Check starting"))
    searchLog(log, Seq("PASSED"))
    searchLog(log, Seq("not", "failing build"))
    searchLog(log, Seq("not", "[error]"))
    searchLog(log, Seq("not", "java.lang.Illegal"))
    assertDiffType(log, args)
  }

  /** Searches for messages that indicate SemVer ran but failed */
  def assertFailed(log: Logger, args: Seq[String]): Unit = {
    searchLog(log, Seq("not", "UNRESOLVED DEPENDENCIES")) // indicate scripted didn't finish before running the script
    searchLog(log, Seq("Checking ENABLED"))
    searchLog(log, Seq("Check type"))
    searchLog(log, Seq("Check starting"))
    searchLog(log, Seq("FAILED"))
    searchLog(log, Seq("failing build"))
    // failures are handled as exceptions
    searchLog(log, Seq("[error]"))
    searchLog(log, Seq("java.lang.Illegal"))
    // failure should always prevent publishing
    searchLog(log, Seq("not", "published"))
    assertDiffType(log, args)
  }

  private def assertDiffType(log: Logger, args: Seq[String]): Unit = {
    val changeType = args.mkString(" ")

    changeType.toLowerCase match {
      // These are for assertPassed() and assertFailed()
      case "major" =>
        searchLog(log, Seq("diffType=major"))
        grepLog(log, Seq("backward=[1-9][0-9]*, forward=[1-9][0-9]*"))
      case "minor" =>
        searchLog(log, Seq("diffType=minor"))
        grepLog(log, Seq("backward=0, forward=[1-9][0-9]*"))
      case "patch" =>
        searchLog(log, Seq("diffType=patch"))
        searchLog(log, Seq("backward=0, forward=0"))

      // These are only for assertFailed()
      case "unchanged" =>
        searchLog(log, Seq("unchanged"))
      case "not normalized" =>
        searchLog(log, Seq("not normalized"))
      case "downgrade" =>
        searchLog(log, Seq("downgrade"))

      case _ =>
        sys.error(s"unexpected changeType=$changeType")
    }
  }
}
