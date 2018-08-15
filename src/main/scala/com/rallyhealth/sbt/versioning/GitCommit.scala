package com.rallyhealth.sbt.versioning

import scala.util.matching.Regex

/**
  * @param tags Sorted (as strings) in descending order (they should be versions so they should sort properly).
  * @param fullHash The full 40 digit hexadecimal commit object name (i.e. commit hash). This is the full 40 digits to
  * avoid any chance of collisions. See [[abbreviatedHash]].
  * @param abbreviatedHash A shorter version of [[fullHash]] for use by [[HashSemVerIdentifier]].
  */
case class GitCommit(fullHash: String, abbreviatedHash: String, tags: Seq[String]) {
  require(
    fullHash.startsWith(abbreviatedHash), s"fullHash=$fullHash does not start with abbreivatedHash=$abbreviatedHash")
  require(fullHash.length >= 40, "hash must consist 40 hexadecimal characters -- a full git object name")
  require(
    GitCommit.regex.pattern.matcher(fullHash).matches(),
    "hash must consist 40 hexadecimal characters -- a full git object name")
}

object GitCommit {

  val regex: Regex = "[0-9a-f]{40}".r

  def apply(fullHash: String, abbreviatedHashLength: Int, tags: Seq[String]): GitCommit =
    GitCommit(fullHash, fullHash.take(abbreviatedHashLength), tags)

  /**
    * Converts a single line from a "git log" command into a [[GitCommit]].
    *
    * @param logOutput Output from "git log --oneline --decorate=short --simplify-by-decoration", looks like
    * "dc96343 (tag: v0.3.0) Setting version to 0.3.0"
    * @param abbreviatedHashLength The length of abbreviated hashes. This is must be determined from git, and is used
    * when outputting the hash.
    */
  def fromGitLog(logOutput: String, abbreviatedHashLength: Int): GitCommit = {

    val hash = {
      val pattern = ("^(" + HashSemVerIdentifier.regex + ")").r
      pattern.findFirstIn(logOutput).getOrElse(
        throw new IllegalArgumentException("no hash prefix in git log: " + logOutput))
    }

    val tags = {
      val pattern = "tag:\\s+([^\\),]+)".r
      pattern.findAllMatchIn(logOutput).toSeq
        .map(_.group(1).trim) // get only the version, not the whole matching string
        .filter(_.nonEmpty) // drop any blanks
        .sorted.reverse
    }

    GitCommit(hash, abbreviatedHashLength, tags)
  }
}

/**
  * A [[GitCommit]] that includes a count of the number of commits beyond the most recent [[ReleaseVersion]] tagged
  * commit. This count is necessary to create a [[SnapshotVersion]].
  */
case class GitCommitWithCount(commit: GitCommit, count: Int) {
  GitCommitWithCount.assertCount(count)
}

object GitCommitWithCount {

  def assertCount(commits: Int): Unit = require(commits >= 0, s"commits=$commits cannot be less than 0")
}
