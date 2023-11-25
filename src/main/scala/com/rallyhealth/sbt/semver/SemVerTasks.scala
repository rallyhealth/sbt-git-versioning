package com.rallyhealth.sbt.semver

import com.rallyhealth.sbt.versioning.GitVersioningPlugin._
import com.rallyhealth.sbt.versioning._
import sbt.{Def, Task}
import sbt.Keys.streams

object SemVerTasks {

  private val noViolationsSuffix = "-- nothing to compare for SemVer violations"

  def prevRelease: Def.Initialize[Task[Option[ReleaseVersion]]] = Def.task[Option[ReleaseVersion]] {
    val logger: sbt.Logger = streams.value.log
    val git = gitDriver.value
    val SemVerPrefix = "[SemVer]"
    val typePrefix = s"$SemVerPrefix Check type:"
    val abortPrefix = s"$SemVerPrefix Check aborted:"

    git.branchState(7) match {
      case GitBranchStateTwoReleases(_, headVersion, _, prevVersion) =>
        if (git.workingState.isDirty) {
          logger.debug(s"$typePrefix comparing most recent release with post-tag changes")
          Some(headVersion)
        } else {
          // we use "currVersion" in the logic below and not "headVersion" because "currVersion" can be different
          // than "headVersion" when using version.override
          logger.debug(s"$typePrefix comparing two most recent releases (tag before publish scenario)")
          Some(prevVersion)
        }

      case GitBranchStateOneReleaseNotHead(_, _, prevVersion) =>
        logger.debug(s"$typePrefix comparing most recent release with post-tag changes")
        Some(prevVersion)

      case GitBranchStateOneReleaseHead(_, headVersion) =>
        if (git.workingState.isDirty) {
          logger.debug(s"$typePrefix comparing most recent release with post-tag changes")
          Some(headVersion)
        } else {
          logger.debug(s"$abortPrefix one previous release, no extra commits, git is clean $noViolationsSuffix")
          None
        }

      case GitBranchStateNoReleases(_) =>
        logger.debug(s"$abortPrefix no previous release $noViolationsSuffix")
        None

      case GitBranchStateNoCommits =>
        logger.debug(s"$abortPrefix no commits $noViolationsSuffix")
        None
    }
  }
}
