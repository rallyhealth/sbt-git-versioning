package com.rallyhealth.sbt.semver.mima

import com.rallyhealth.sbt.semver.SemVerPlugin.MiMa._
import com.rallyhealth.sbt.semver.SemVerPlugin._
import com.rallyhealth.sbt.semver.SemVerPlugin.autoImport._
import com.rallyhealth.sbt.semver.SemVerVersionTooLowException
import com.rallyhealth.sbt.versioning.ReleaseVersion
import com.rallyhealth.sbt.versioning.SemVerReleaseType._
import com.typesafe.tools.mima.plugin.MimaKeys
import sbt._

object SemVerMiMaTasks {

  def semVerCheckMiMa: Def.Initialize[Task[Unit]] = Def.taskDyn {
    semVerEnforcementLevel.value match {
      case Major =>
        // Any changes are fine. Don't run MiMa.
        Def.task {}
      case Minor | Patch =>
        // Avoid resolving keys which may fail (e.g. maybePrevInput) unless we actually need them.
        // maybePrevInput fails if there isn't a previous version.
        Def.task {
          for {
            prev <- maybePrevInput.value
            curr = MiMaInput(MimaKeys.mimaCurrentClassfiles.value, semVerVersion.value.toRelease)
            if curr.file.exists()
          } {
            val result: MiMaResult = miMaChecker.value.compare(prev.file, curr.file)
            val attempted = semVerEnforcementLevel.value

            if (result.releaseType > attempted) {
              throw new SemVerVersionTooLowException(prev.version, curr.version, attempted, result)
            }
          }
        }
    }
  }
}

case class MiMaInput(file: File, version: ReleaseVersion)
