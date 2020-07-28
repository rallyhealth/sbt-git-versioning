package com.rallyhealth.sbt.semver.level

import com.rallyhealth.sbt.versioning.SemVerReleaseType

/**
  * @see [[SemVerEnforcementLevel]]
  */
trait SemVerLevelRule {

  /**
    * Optionally decides the degree of breaking changes that are permissable.
    * The first [[SemVerLevelRule]] that returns [[Some]] [[SemVerEnforcementLevel]] wins.
    */
  def calcLevel(): Option[SemVerEnforcementLevel]
}

/**
  * Defines the degree of breaking changes in semver terms (major, minor, patch)
  * that are permissible under the current version.
  *
  * Normally, this is determined by previous release - current version,
  * for example 3.0.0 => 3.1.0 is a minor release.
  *
  * However, this may be adjusted or overridden under certain circumstances,
  * for example allowing SNAPSHOTS with patch level versions to introduce minor changes.
  */
class SemVerEnforcementLevel(val releaseType: SemVerReleaseType, val explanation: String) {

  override def toString = s"$releaseType changes allowed: $explanation"
}
