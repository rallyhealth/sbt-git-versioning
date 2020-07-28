package com.rallyhealth.sbt.versioning

object LowerBoundedSemanticVersion {

  implicit class BoundedSemanticVersion(val version: SemanticVersion) extends AnyVal {

    /**
      * A new [[SemanticVersion]] increased up to the provided lower bound or else the original [[version]].
      *
      * @param bound the lower bound semantic version
      * @param hashAndCount Used to fill in the hash if this returns a new [[SemanticVersion]]
      * @return A new [[SemanticVersion]] increased up to the provided lower bound, or else the original [[version]].
      */
    def lowerBound(bound: LowerBound, hashAndCount: HashAndCount): SemanticVersion = {
      val lowerBoundedVersion = SnapshotVersion(
        bound.major,
        bound.minor,
        bound.patch,
        SemVerIdentifierList.empty,
        version.isDirty,
        hashAndCount.hash,
        hashAndCount.count
      )

      if (lowerBoundedVersion > version) {
        require(version.isSnapshot, s"gitVersioningSnapshotLowerBound=$bound is higher than release=$version. Refusing to do a release build.")
        lowerBoundedVersion
      } else version
    }
  }

  case class HashAndCount(hash: HashSemVerIdentifier, count: CommitsSemVerIdentifier)
}
