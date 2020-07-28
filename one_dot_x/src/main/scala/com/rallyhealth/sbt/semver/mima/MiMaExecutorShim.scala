package com.rallyhealth.sbt.semver.mima

import com.rallyhealth.sbt.semver.CustomMiMaLibMiMaExecutor
import com.typesafe.tools.mima.core.{Config, Settings, resolveClassPath}
import sbt.Keys.{Classpath, TaskStreams}

import scala.tools.nsc.classpath.{AggregateClassPath, ClassPathFactory}

object MiMaExecutorShim {
  def apply(classPath: Classpath, taskStreams: TaskStreams): MiMaExecutor = {
    val javaClassPath = {
      // from SbtMima.makeMima
      //https://github.com/lightbend/mima/commit/6de0212a8d180e950863c0e5b0c2e92f8d10de74#diff-8b0a4a3d64e68f52bd8d720053c8f099
      val cmd = "sbt-mima-plugin"
      Config.settings = new Settings
      val (_, resargs) = Config.settings.processArguments(List.empty, true)
      Config.baseClassPath = resolveClassPath()
      if (Config.settings.help.value) {
        println(usageMsg(cmd))
        System.exit(0)
      }
      val classPathString = classPath
        .map(attributedFile => attributedFile.data.getAbsolutePath)
        .mkString(System.getProperty("path.separator"))
      // Note for self https://github.com/lightbend/mima/commit/dc0f0a80414bc903d62b12c766825e89e3149750
      AggregateClassPath.createAggregate(new ClassPathFactory(Config.settings).classesInPath(classPathString): _*)
    }
    new CustomMiMaLibMiMaExecutor(javaClassPath)
  }

  private def usageMsg(cmd: String): String =
    Config.settings.visibleSettings.
      map(s => s.helpSyntax.format().padTo(21, ' ') + " " + s.helpDescription).
      toList.sorted.mkString("Usage: " + cmd + " <options>\nwhere possible options include:\n  ", "\n  ", "\n")
}
