package com.rallyhealth.sbt.versioning

/**
  * State of a git working directory that can be computed once and re-used. This data is created by [[GitDriver]].
  *
  * @param isDirty True if there are uncommitted tracked changes in the working directory. Untracked files are ignored.
  */
case class GitWorkingState(isDirty: Boolean)

/**
  * State of the current git branch that can be computed once and re-used. This data is created by [[GitDriver]].
  *
  * Each case class references zero, one, or two significant commits from git's history. The best way I can think to
  * show this is to provide an example log (with pretty columns) and inline the case classes and fields. (I've added
  * more log entries than are strictly necessary to be as clear as possible.)
  * {{{
  * Commit | Tag  | GitBranchState    | Fields
  * -------|------|-------------------|-------------------------------------------------------------------------------
  *  06    |      | OneReleaseNotHead | headCommit = 06 ,                     prevCommit = 05 , prevVersion = 3.0
  *  05    | v3.0 | TwoReleases       | headCommit = 05 , headVersion = 3.0 , prevCommit = 04 , prevVersion = 2.0
  *  04    | v2.0 | TwoReleases       | headCommit = 04 , headVersion = 2.0 , prevCommit = 01 , prevVersion = 1.0
  *  03    |      | OneReleaseNotHead | headCommit = 03 ,                     prevCommit = 01 , prevVersion = 1.0
  *  02    |      | OneReleaseNotHead | headCommit = 02 ,                     prevCommit = 01 , prevVersion = 1.0
  *  01    | v1.0 | OneReleaseHead    | headCommit = 01 , headVersion = 1.0
  *  00    |      | NoReleases        | headCommit = 00
  *  --    |      | NoCommits         |
  * }}}
  *
  * I've tried to be very explicit and create separate classes for each possible state (specifically avoiding using
  * [[Option]]s represent multiple states in one class). I learned that makes the logic harder to follow and introduces
  * cyclomatic complexity (i.e. nested logic).
  */
sealed trait GitBranchState {

  /** Similar to [[Predef.require()]] yet it tacks on "this" to the end of the message. */
  protected def require(requirement: Boolean, message: => Any) {
    if (!requirement)
      throw new IllegalArgumentException(s"requirement failed: $message (from $this)")
  }
}

/**
  * This is used when there are two (2) commits that are tagged as [[ReleaseVersion]]s, and the HEAD commit is the
  * first of the two (2) releases. This situation occurs immediately after tagging HEAD as a [[ReleaseVersion]].
  */
case class GitBranchStateTwoReleases(
  headCommit: GitCommit, headVersion: ReleaseVersion, prevCommit: GitCommit, prevVersion: ReleaseVersion)
  extends GitBranchState {

  // this checks hash directly because tags might be unequal and that's still bad
  require(headCommit.fullHash != prevCommit.fullHash, s"headCommit=$headCommit cannot be the prevCommit=$prevCommit")
  require(
    headCommit.tags.exists { case ReleaseVersion(v) => v == headVersion },
    s"headCommit=$headCommit should be tagged with headVersion=$headVersion")
  require(
    prevCommit.tags.exists { case ReleaseVersion(v) => v == prevVersion },
    s"prevCommit=$prevCommit should be tagged with prevVersion=$prevVersion")
  require(
    headCommit != prevCommit,
    s"headCommit=$headCommit cannot be the prevVersionCommit=$prevCommit")
  require(!headVersion.isDirty, s"headVersion=$headVersion cannot be dirty")
  require(!prevVersion.isDirty, s"prevVersion=$prevVersion cannot be dirty")
}

object GitBranchStateTwoReleases {

  def apply(headCommit: GitCommit, prevCommit: GitCommit): GitBranchStateTwoReleases =
    GitBranchStateTwoReleases(
      headCommit,
      ReleaseVersion.unapply(headCommit)
        .getOrElse(throw new IllegalArgumentException(s"headCommit=$headCommit must be tagged with a Release version")),
      prevCommit,
      ReleaseVersion.unapply(prevCommit)
        .getOrElse(throw new IllegalArgumentException(s"previousCommit=$prevCommit must be tagged with a Release version")))
}

/**
  * This is used when there are commits but HEAD is NOT tagged with a [[ReleaseVersion]], but a previous
  * commit is tagged with a [[ReleaseVersion]].
  *
  * @param headCommit The HEAD commit.
  * @param prevCommit The previous (non-head) commit with the release tag.
  * @param prevVersion The [[ReleaseVersion]] from `versionCommit`
  */
case class GitBranchStateOneReleaseNotHead(
  headCommit: GitCommitWithCount, prevCommit: GitCommit, prevVersion: ReleaseVersion)
  extends GitBranchState {

  // this checks hash directly because tags might be unequal and that's still bad
  require(headCommit.commit.fullHash != prevCommit.fullHash, s"headCommit=$headCommit cannot be the prevCommit=$prevCommit")
  require(
    headCommit.commit.tags.collect { case ReleaseVersion(v) => v }.isEmpty,
    s"headCommit=$headCommit should NOT be tagged as a release version")
  require(
    prevCommit.tags.exists { case ReleaseVersion(v) => v == prevVersion },
    s"prevCommit=$prevCommit should be tagged with prevVersion=$prevVersion")
  require(!prevVersion.isDirty, s"prevVersion=$prevVersion cannot be dirty")
}

object GitBranchStateOneReleaseNotHead {

  def apply(headCommit: GitCommitWithCount, prevCommit: GitCommit): GitBranchStateOneReleaseNotHead =
    GitBranchStateOneReleaseNotHead(
      headCommit,
      prevCommit,
      ReleaseVersion.unapply(prevCommit)
        .getOrElse(throw new IllegalArgumentException(s"previousCommit=$prevCommit must be tagged with a Release version")))
}

/**
  * This is used when there are commits and the HEAD is tagged with a [[ReleaseVersion]].
  *
  * @param headCommit The HEAD commit.
  */
case class GitBranchStateOneReleaseHead(headCommit: GitCommit, headVersion: ReleaseVersion) extends GitBranchState {

  require(!headVersion.isDirty, s"version=$headVersion cannot be dirty")
  require(
    headCommit.tags.exists { case ReleaseVersion(v) => v == headVersion },
    s"head=$headCommit should be tagged with version=$headVersion")
}

object GitBranchStateOneReleaseHead {

  def apply(headCommit: GitCommit): GitBranchStateOneReleaseHead =
    GitBranchStateOneReleaseHead(
      headCommit,
      ReleaseVersion.unapply(headCommit)
        .getOrElse(throw new IllegalArgumentException(s"headCommit=$headCommit must be tagged with a Release version")))
}

/**
  * This is used when there are commits but none that are tagged with a [[ReleaseVersion]].
  *
  * @param headCommit The HEAD commit.
  */
case class GitBranchStateNoReleases(headCommit: GitCommitWithCount) extends GitBranchState {

  require(
    headCommit.commit.tags.collect { case ReleaseVersion(v) => v }.isEmpty,
    s"headCommit=$headCommit should NOT be tagged as a release version")
}

/**
  * This is used when there are no commits.
  */
case object GitBranchStateNoCommits extends GitBranchState
