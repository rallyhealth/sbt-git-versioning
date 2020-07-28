package com.rallyhealth.sbt.semver.mima

import com.rallyhealth.sbt.semver.CustomMiMaLibMiMaExecutor
import com.typesafe.tools.mima.core.Config
import sbt.Keys.{Classpath, TaskStreams}

object MiMaExecutorShim {
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
