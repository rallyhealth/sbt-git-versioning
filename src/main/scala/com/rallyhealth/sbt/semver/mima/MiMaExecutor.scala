package com.rallyhealth.sbt.semver.mima

import com.rallyhealth.sbt.semver.CustomMiMaLibMiMaExecutor
import com.typesafe.tools.mima.core._
import com.typesafe.tools.mima.lib.MiMaLib
import sbt.File
import sbt.Keys.{Classpath, TaskStreams}

/**
  * Adapter that mimics the interface of [[MiMaLib]] but allows us to test the logic we build around MiMa.
  */
trait MiMaExecutor {

  /**
    * A backward problem means that something has changed in the NEW code that prevents code built against the OLD code
    * from using the NEW code. That means its NOT a drop in replacement, the OLD code must be updated. Breaking backward
    * compatibility typically forces a MAJOR upgrade.
    *
    * Examples:
    * - Renaming a class/field/method/type
    * - Deleting a class/field/method/type
    * - Adding or narrowing the type (return, parameter, generic, etc.) of a class/field/method/type
    */
  def backwardProblems(oldDir: File, newDir: File): List[Problem]

  /**
    * A forward problem means that something has changed in the NEW code that is compatible with OLD code but is still
    * a visible change. That means it is a drop in replacement, the OLD code will NOT need to be updated. But it does
    * mean that its identical -- new stuff may have been added. When forward compatibility problems exist it means
    * you need to make a MINOR upgrade.
    *
    * Examples:
    * - Adding a class/field/method/type
    * - Annotating a class/field/method/type
    * - Loosening the type (return, parameter, generic, etc.) of a class/field/method/type
    *
    * Note that the parameters are in the same order as [[backwardProblems()]], hopefully this will reduce
    * confusion while making this interface easier to mock.
    */
  def forwardProblems(oldDir: File, newDir: File): List[Problem]
}

object MiMaExecutor {
  def apply(classPath: Classpath, taskStreams: TaskStreams): MiMaExecutor = {
    val javaClassPath = {
      // from SbtMima.makeMima
      Config.setup("sbt-mima-plugin", Array.empty)
      val classPathString = classPath
        .map(attributedFile => attributedFile.data.getAbsolutePath)
        .mkString(System.getProperty("path.separator"))
      com.typesafe.tools.mima.core.reporterClassPath(classPathString)
    }
    new CustomMiMaLibMiMaExecutor(javaClassPath)
  }
}
