package com.rallyhealth.sbt.semver.mima

import com.typesafe.tools.mima.plugin.SbtMima
import sbt.Keys.TaskStreams
import sbt.internal.librarymanagement.IvySbt
import sbt.{CrossVersion, File, ModuleID, librarymanagement}

trait ModuleResolver {

  /**
    * Resolves/Downloads an artifact, returning the location on the filesystem.
    */
  def resolve(m: ModuleID): File
}

class IvyModuleResolver(ivyScala: Option[librarymanagement.ScalaModuleInfo], ivySbt: IvySbt, ts: TaskStreams) extends ModuleResolver {

  override def resolve(previousMID: ModuleID): File = {
    val previousModuleID: ModuleID = CrossVersion(previousMID, ivyScala) match {
      case Some(f) => previousMID.withName(f(previousMID.name))
      case None => previousMID
    }

    SbtMima.getPreviousArtifact(previousModuleID, ivySbt, ts)
  }
}
