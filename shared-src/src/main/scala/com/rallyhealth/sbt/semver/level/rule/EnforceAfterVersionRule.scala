package com.rallyhealth.sbt.semver.level.rule

import com.rallyhealth.sbt.semver.level.{SemVerEnforcementLevel, SemVerLevelRule}
import com.rallyhealth.sbt.versioning.SemVerReleaseType.Major
import com.rallyhealth.sbt.versioning.{ReleaseVersion, SemanticVersion}

case class EnforceAfterVersionRule(current: SemanticVersion, maybeEnforceAfterVersion: Option[ReleaseVersion]) extends SemVerLevelRule {

  override def calcLevel(): Option[DisabledEnforceAfterVersion] = {
    maybeEnforceAfterVersion
      .filter(current <= _)
      .map(enforceAfterVersion => DisabledEnforceAfterVersion(enforceAfterVersion))
  }
}

case class DisabledEnforceAfterVersion(enforceAfterVersion: ReleaseVersion) extends SemVerEnforcementLevel(
  releaseType = Major,
  explanation = s"semVerEnforceAfterVersion := $enforceAfterVersion was used to allow major changes until the specified version."
)
