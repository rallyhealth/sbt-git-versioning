package com.rallyhealth.sbt.semver

import com.rallyhealth.sbt.versioning.{SemVerIdentifierList, SnapshotVersion}

object TestSnapshotVersion {

  def apply(
    major: Int,
    minor: Int,
    patch: Int,
    versionIdentifiers: SemVerIdentifierList = SemVerIdentifierList.empty,
    isDirty: Boolean = false,
    commitHash: String = "0123abc",
    commitsSincePrevRelease: Int = 1
  ) = {
    SnapshotVersion(
      major = major,
      minor = minor,
      patch = patch,
      versionIdentifiers = versionIdentifiers,
      isDirty = isDirty,
      commitHash = commitHash,
      commitsSincePrevRelease = commitsSincePrevRelease
    )
  }
}
