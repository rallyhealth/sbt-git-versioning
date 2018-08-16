package com.rallyhealth.sbt.versioning

object LowerBoundedSemanticVersion {

  implicit class BoundedSemanticVersion(val version: SemanticVersion) extends AnyVal {

    /**
      * A new [[SemanticVersion]] increased up to the provided lower bound or else the original [[version]].
      *
      * @param branchState Used to fill in the hash if this returns a new [[SemanticVersion]]
      * @return A new [[SemanticVersion]] increased up to the provided lower bound, or else the original [[version]].
      */
    def lowerBound(bound: LowerBound, branchState: GitBranchState): SemanticVersion = {
      lowerBound(bound, extractHash(branchState))
    }

    /**
      * A new [[SemanticVersion]] increased up to the provided lower bound or else the original [[version]].
      *
      * @param hash Used to fill in the hash if this returns a new [[SemanticVersion]]
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

      if (lowerBoundedVersion > version) lowerBoundedVersion else version
    }
  }

  private def extractHash(branchState: GitBranchState): HashAndCount = {
    // we just need the git hash,
    // but unfortunately GitBranchState makes this more complex
    branchState match {
      case GitBranchStateTwoReleases(head, _, _, _) => HashAndCount(head, 0)
      case GitBranchStateOneReleaseNotHead(head, _, _) => HashAndCount(head)
      case GitBranchStateOneReleaseHead(head, _) => HashAndCount(head, 0)
      case GitBranchStateNoReleases(head) => HashAndCount(head)
      case GitBranchStateNoCommits => HashAndCount("0" * 8, 0)
    }
  }

  case class HashAndCount(hash: HashSemVerIdentifier, count: CommitsSemVerIdentifier)

  object HashAndCount {

    def apply(gcc: GitCommitWithCount): HashAndCount = HashAndCount(gcc.commit.abbreviatedHash, gcc.count)

    def apply(gc: GitCommit, count: Int): HashAndCount = HashAndCount(gc.abbreviatedHash, count)
  }

}
