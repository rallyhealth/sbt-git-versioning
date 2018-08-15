package com.rallyhealth.sbt.semver

import com.rallyhealth.sbt.semver.mima.MiMaExecutor
import com.typesafe.tools.mima.core.util.IndentedOutput.indented
import com.typesafe.tools.mima.core._
import com.typesafe.tools.mima.lib.MiMaLib
import com.typesafe.tools.mima.lib.analyze.Analyzer
import com.typesafe.tools.mima.plugin.MimaKeys
import sbt.{File => SbtFile}

import scala.collection.mutable.ListBuffer
import scala.tools.nsc.io.{AbstractFile, File}

/**
  * Custom re-implementation of [[MiMaLib]] that does not [[Config.fatal]] on an error.
  *
  * This class is NOT thread-safe.
  *
  * Why re-implement this? This will call [[Config.fatal]] when it encounters a path that is not directory or jar file.
  * That's definitely an error, no question but [[Config.fatal]] does not just throw an exception. Its a full JVM shut
  * down: it runs [[System.exit(-1)]] and then throws an [[Error]] for good measure. Sometimes it doesn't even get to
  * print out the error to the console before the JVM shuts down!
  *
  * Google produced only [[https://github.com/typesafehub/migration-manager/issues/55]] which wasn't very helpful.
  * Unfortunately I could not re-create this with a unit or scripted test either. I only replicated it in the wild.
  * It would just halt SBT trying to compile the root project. The path wouldn't exist because the root is not a real
  * project. That path comes from [[MimaKeys.mimaCurrentClassfiles]]. I do not know why it contains a non-existent path
  * if the failure is so harsh.
  *
  * I copied the code from [[https://github.com/typesafehub/migration-manager/blob/master/reporter/src/main/scala/com/typesafe/tools/mima/lib/MiMaLib.scala]]
  * and modified it:
  * - root() does not call [[Config.fatal]], it throws an [[IllegalArgumentException]].
  * - Added method return types.
  * - Removed useless logging.
  * - Clear list of problems before collecting (so we can re-use the logic).
  * - Less stringly-typed
  */
class CustomMiMaLibMiMaExecutor(classpath: CompilerClassPath) extends MiMaExecutor {

  // Copied from com.typesafe.tools.mima.SbtMima.makeMima(). I have no idea what this does, but it is DEFINITELY
  // required. It sets up something with the logger. If you don't do this you get weird NullPointerExceptions when
  // the MiMa code uses com.typesafe.tools.mima.core.Config
  Config.setup("sbt-mima-plugin", Array.empty)

  override def backwardProblems(oldDir: SbtFile, newDir: SbtFile): List[Problem] = collectProblems(oldDir, newDir)

  override def forwardProblems(oldDir: SbtFile, newDir: SbtFile): List[Problem] = collectProblems(newDir, oldDir)

  private def root(file: SbtFile): Definitions = classPath(file.getAbsolutePath) match {
    case cp @ Some(_) =>
      new Definitions(cp, classpath)
    case None =>
      throw new IllegalArgumentException("can only run MiMa on a directory or jar file: " + file.getAbsoluteFile)
  }

  private def classPath(name: String): Option[CompilerClassPath] = {
    val f = new File(new java.io.File(name))
    val dir = AbstractFile.getDirectory(f)
    if (dir == null) None
    else Some(dirClassPath(dir))
  }

  private val problems = new ListBuffer[Problem]

  private def raise(problem: Problem): problems.type = {
    problems += problem
  }

  private def comparePackages(oldPkg: PackageInfo, newPkg: PackageInfo): Unit = {
    for (oldClazz <- oldPkg.accessibleClasses) {
      newPkg.classes get oldClazz.bytecodeName match {
        case None if oldClazz.isImplClass =>
          // if it is missing a trait implementation class, then no error should be reported
          // since there should be already errors, i.e., missing methods...
          ()

        case None => raise(MissingClassProblem(oldClazz))

        case Some(newClazz) => Analyzer(oldClazz, newClazz).foreach(raise)
      }
    }
  }

  private def traversePackages(oldPkg: PackageInfo, newPkg: PackageInfo): Unit = {
    comparePackages(oldPkg, newPkg)
    indented {
      for (p <- oldPkg.packages.valuesIterator) {
        newPkg.packages get p.name match {
          case None =>
            traversePackages(p, NoPackageInfo)
          case Some(q) =>
            traversePackages(p, q)
        }
      }
    }
  }

  /** Return a list of problems for the two versions of the library. */
  private def collectProblems(oldPath: SbtFile, newPath: SbtFile): List[Problem] = {
    problems.clear()
    val oldRoot = root(oldPath)
    val newRoot = root(newPath)
    traversePackages(oldRoot.targetPackage, newRoot.targetPackage)
    problems.toList
  }
}
