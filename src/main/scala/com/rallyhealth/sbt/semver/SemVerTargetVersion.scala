package com.rallyhealth.sbt.semver

import com.rallyhealth.sbt.versioning.ReleaseVersion

/**
  * Qualifies the "current" (as opposed to "previous") [[ReleaseVersion]] when comparing for SemVer violations. The
  * version by itself is insufficient -- it's ambiguous how this should be used for checking certain kinds of errors.
  */
sealed trait SemVerTargetVersion {
  def version: ReleaseVersion
}

/**
  * If the user sets [[SemVerPlugin.autoImport.semVerLimit]] then the [[ReleaseVersion]] is a limit, an upper
  * bound. It is cannot be used for some errors, like [[NotNormalizedSemVerDiff]].
  *
  * This class should ALWAYS have higher precedence than [[SemVerSpecificTargetVersion]].
  */
case class SemVerLimitTargetVersion(version: ReleaseVersion) extends SemVerTargetVersion

/**
  * If the user sets [[com.rallyhealth.sbt.versioning.GitVersioningPlugin.autoImport.versionOverride]] then the
  * [[ReleaseVersion]] is the specific version that is being published.
  *
  * This class should ALWAYS have higher precedence than [[SemVerLimitTargetVersion]].
  */
case class SemVerSpecificTargetVersion(version: ReleaseVersion) extends SemVerTargetVersion
