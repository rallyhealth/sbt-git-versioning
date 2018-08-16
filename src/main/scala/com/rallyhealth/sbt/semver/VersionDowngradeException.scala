package com.rallyhealth.sbt.semver

import com.rallyhealth.sbt.versioning.{ReleaseVersion, SemanticVersion}

class VersionDowngradeException(val current: SemanticVersion, val prev: ReleaseVersion) extends Exception(
  s"prev.version=$prev cannot be equal or greater than curr.version=$current"
)
