package com.rallyhealth.sbt.semver.mima

import com.rallyhealth.sbt.semver.SemVerPlugin.MiMa._
import com.rallyhealth.sbt.semver.SemVerPlugin._
import com.rallyhealth.sbt.semver.SemVerPlugin.autoImport._
import com.rallyhealth.sbt.semver.SemVerVersionTooLowException
import com.rallyhealth.sbt.versioning.{ReleaseVersion, SemVerReleaseType}
import com.typesafe.tools.mima.plugin.MimaKeys
import sbt._

object SemVerMiMaTasks {

  def semVerCheckMiMa: Def.Initialize[Task[Unit]] = Def.task {
    for {
      prev <- maybePrevInput.value
      curr = MiMaInput(MimaKeys.mimaCurrentClassfiles.value, semVerVersion.value.toRelease)
    } {
      val result: MiMaResult = miMaChecker.value.compare(prev.file, curr.file)

      val attempted: SemVerReleaseType = semVerEnforcementLevel.value
      if (result.releaseType > attempted) {
        throw new SemVerVersionTooLowException(prev.version, curr.version, attempted, result)
      }
    }
  }
}

case class MiMaInput(file: File, version: ReleaseVersion)
