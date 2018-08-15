package com.rallyhealth.sbt.semver.level.rule

import com.rallyhealth.sbt.semver.SemVerDiff
import com.rallyhealth.sbt.semver.level.{SemVerEnforcementLevel, SemVerLevelRule}
import com.rallyhealth.sbt.versioning.SemVerReleaseType._
import com.rallyhealth.sbt.versioning.{ReleaseVersion, SemVerReleaseType, SemanticVersion, SnapshotVersion}

case class VersionDiffRule(current: SemanticVersion, maybePrevRelease: Option[ReleaseVersion]) extends SemVerLevelRule {

  override def calcLevel(): Option[SemVerEnforcementLevel] = {
    maybePrevRelease match {
      case None => Some(DisabledBecauseNoPreviousRelease)
      case Some(prevRelease) =>
        assert(prevRelease != current, "Prev release and current version must be different.")

        val currentRelease = current.toRelease
        val delta = SemVerDiff.calc(prevRelease, currentRelease)
        val releaseType =
          if (delta.major > 0) Major
          else if (delta.minor > 0) Minor
          else Patch

        if (releaseType == Patch && current.isInstanceOf[SnapshotVersion]) {
          Some(MinorOkayForSnapshot(current))
        } else {
          Some(new NormalVersionBump(releaseType, prevRelease, currentRelease))
        }
    }
  }
}

case object DisabledBecauseNoPreviousRelease extends SemVerEnforcementLevel(
  releaseType = Major,
  explanation = "No previous versions to compare against."
)

class NormalVersionBump(
  releaseType: SemVerReleaseType,
  prevRelease: ReleaseVersion,
  currentRelease: ReleaseVersion
) extends SemVerEnforcementLevel(releaseType, s"$prevRelease => $currentRelease")

case class MinorOkayForSnapshot(version: SemanticVersion) extends SemVerEnforcementLevel(
  releaseType = Minor,
  explanation = "SNAPSHOT rules are relaxed for convenience and may include up to minor changes. " +
  "This is not part of the SemVer spec, but RallyVersioning can't do this automatically " +
  "without requiring you to manually bump semVerLimit every time you add a method. " +
  "Minor version bumps are enforced only at release time."
)
