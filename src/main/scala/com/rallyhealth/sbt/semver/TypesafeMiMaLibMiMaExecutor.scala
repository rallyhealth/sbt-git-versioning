package com.rallyhealth.sbt.semver

import com.typesafe.tools.mima.core._
import com.typesafe.tools.mima.core.util.log.Logging
import com.typesafe.tools.mima.lib
import com.typesafe.tools.mima.lib.MiMaLib
import sbt.File

import scala.tools.nsc.util.JavaClassPath

/**
  * Standard implementation that uses a [[MiMaLib]]. Here for reference.
  *
  * This class is NOT thread-safe.
  */
@deprecated("use RallyCustomMiMaLibMiMaExecutor", "1.0.3")
class TypesafeMiMaLibMiMaExecutor(javaClassPath: JavaClassPath, logging: Logging) extends MiMaExecutor {

  // Copied from com.typesafe.tools.mima.SbtMima.makeMima(). I have no idea what this does, but it is DEFINITELY
  // required. It sets up something with the logger. If you don't do this you get weird NullPointerExceptions when
  // the MiMa code uses com.typesafe.tools.mima.core.Config
  Config.setup("sbt-mima-plugin", Array.empty)

  override def backwardProblems(oldDir: File, newDir: File): List[Problem] = {
    createMiMaLib().collectProblems(oldDir.getAbsolutePath, newDir.getAbsolutePath)
  }

  override def forwardProblems(oldDir: File, newDir: File): List[Problem] =
    createMiMaLib().collectProblems(newDir.getAbsolutePath, oldDir.getAbsolutePath)

  /** Creates a new [[MiMaLib]] instance, since they are not thread safe and cannot be re-used. */
  private def createMiMaLib(): MiMaLib = {
    new lib.MiMaLib(javaClassPath, logging)
  }
}
