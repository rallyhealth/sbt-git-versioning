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
}

/**
  * A [[GitCommit]] that includes a count of the number of commits beyond the most recent [[ReleaseVersion]] tagged
  * commit. This count is necessary to create a [[SnapshotVersion]].
  */
case class GitCommitWithCount(sha: GitCommit, count: Int) {
  GitCommitWithCount.assertCount(count)
}

object GitCommitWithCount {

  def assertCount(commits: Int): Unit = require(commits >= 0, s"commits=$commits cannot be less than 0")
}
