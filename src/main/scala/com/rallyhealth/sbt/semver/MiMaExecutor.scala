package com.rallyhealth.sbt.semver

import com.rallyhealth.sbt.versioning.ReleaseVersion
import com.typesafe.tools.mima.core._
import com.typesafe.tools.mima.lib.MiMaLib
import com.typesafe.tools.mima.plugin.SbtMima
import sbt.Keys.TaskStreams
import sbt.{CrossVersion, File, IvySbt, IvyScala, ModuleID, _}

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

/**
  * Helper class to hold all the bits and parts that we need to describe a project for MiMa to perform a comparison.
  */
sealed trait MimaInput {

  /** Directory of class files or a file path for a JAR. */
  def path: File
}

case class CurrMimaInput(path: File, target: SemVerTargetVersion) extends MimaInput

/**
  * Helper class to hold all the bits and parts that we need to describe a project for MiMa to perform a comparison.
  *
  * @param path Directory of class files or a file path for a JAR.
  */
case class PrevMimaInput(path: File, version: ReleaseVersion) extends MimaInput

object PrevMimaInput {

  /**
    * Container object for constructing a [[PrevMimaInput]], populated from data from the project, working directory,
    * and Git. This data typically comes out of the ether and is accessibly only within a [[Def.task()]]. You can
    * safely create an instance of this within a [[Def.task()]] and use [[create()]] at some later point.
    *
    * @param artifact The artifact name for fetching the previous version from Ivy
    * @param org The organization name for fetching the previous version from Ivy
    */
  case class Data(
    version: ReleaseVersion,
    artifact: String,
    org: String,
    ts: TaskStreams,
    ivyScala: Option[IvyScala],
    ivySbt: IvySbt)

  /**
    * Constructs a [[PrevMimaInput]] using data from the project, working directory, and Git. This data typically
    * comes out of the ether and is accessibly only within a [[Def.task()]].
    *
    * Note this will likely return None if the organization or artifact name change -- there's no way to get that
    * without doing something like checking out the build.sbt file and running SBT just to get that data. This method
    * is not that smart.
    *
    * @return None if no previous version can be found.
    */
  def create(data: Data): PrevMimaInput = {

    val previousMID: ModuleID = data.org %% data.artifact % data.version.toString

    // magic copied directly from MiMa's code
    // https://github.com/typesafehub/migration-manager/blob/c0b01c7584b83b276608defba9b6df0517a03f19/sbtplugin/src/main/scala/com/typesafe/tools/mima/plugin/MimaPlugin.scala#L64
    val previousModuleID: ModuleID = CrossVersion(previousMID, data.ivyScala) match {
      case Some(f) => previousMID.copy(name = f(previousMID.name))
      case None => previousMID
    }

    val prevArtifact: File = SbtMima.getPreviousArtifact(previousModuleID, data.ivySbt, data.ts)
    PrevMimaInput(prevArtifact, data.version)
  }
}
