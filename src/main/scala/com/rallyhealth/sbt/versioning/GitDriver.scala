package com.rallyhealth.sbt.versioning

import sbt.{Process, _}

/**
  * Driver that allows executing common commands against a git working directory.
  *
  * See also [[GitFetcher]].
  */
trait GitDriver {

  /** Used to find the previous version and to check whether the HEAD and version commit are the same   */
  def branchState: GitBranchState

  /** Used to determine whether the current version is dirty or not. */
  def workingState: GitWorkingState

  /**
    * Returns the number of commits in this repository, up to but not including the optional limit.
    *
    * @param hash A commit hash to stop counting. This will NOT be included in the count. If None this will count all
    * commits back to the first commit.
    */
  def getCommitCount(hash: Option[String]): Int

  /**
    * Returns the current version of the code from the branch version AND git's working state. (Including the current
    * state might seem weird but is technically part of the version -- the version includes not just the prior version
    * but also the contents of the index of the working directory. Without this we couldn't tell dirty or not.)
    *
    * @param ignoreDirty Forces clean builds, i.e. when true this will not add '-dirty' to the version (nor
    * force creating a [[SnapshotVersion]] from a [[ReleaseVersion]]).
    */
  def calcCurrentVersion(ignoreDirty: Boolean): SemanticVersion = {

    val currVersion: SemanticVersion = branchState match {

      case GitBranchStateTwoReleases(_, headVersion, _, _) =>
        headVersion

      case GitBranchStateOneReleaseNotHead(head, _, version) =>
        SnapshotVersion.createAfterRelease(version, head)

      case GitBranchStateOneReleaseHead(_, version) =>
        version

      case GitBranchStateNoReleases(head) =>
        SnapshotVersion.createInitialVersion(head)

      case GitBranchStateNoCommits =>
        ReleaseVersion.initialVersion
    }

    if (ignoreDirty)
      currVersion.setDirty(value = false)
    else
      currVersion.setDirty(workingState.isDirty)
  }
}

/**
  * Generic utilities for dealing with Git.
  *
  * See also [[GitFetcher]].
  *
  * @param dir Directory where we can query Git for information. This will be checked to confirm that it is an actual
  * git clone.
  */
class GitDriverImpl(dir: File) extends GitDriver {

  require(isGitRepo(dir), "Must be in a git repository")

  private def isGitRepo(dir: File): Boolean = {
    // this does NOT use runCommand() because that uses this method to check if the directory is a git directory
    val outputLogger = new BufferingProcessLogger
    // http://stackoverflow.com/a/16925062
    val exitCode: Int = Process(s"""git rev-parse --is-inside-work-tree""", dir) ! outputLogger
    exitCode match {
      case 0 => outputLogger.stdout.mkString("").trim.toLowerCase == "true"
      case _ => false
    }
  }

  override val branchState: GitBranchState = {
    gitLog("--max-count=1").headOption match {

      case Some(headCommit) =>

        // we only care about the RELEASE commits
        val releases: Seq[(GitCommit, ReleaseVersion)] = gitLog("").collect { case gc @ ReleaseVersion(rv) => (gc, rv) }

        val maybeCurrRelease = releases.headOption
        val maybePrevRelease = releases.drop(1).headOption

        (maybeCurrRelease, maybePrevRelease) match {

          case (Some((currCommit, _)), Some((prevCommit, _))) if currCommit == prevCommit =>
            throw new IllegalStateException(s"currCommit=$currCommit cannot equal prevCommit=$prevCommit")

          case (Some((currCommit, currVersion)), Some((prevCommit, prevVersion))) if currCommit == headCommit =>
            GitBranchStateTwoReleases(currCommit, currVersion, prevCommit, prevVersion)

          case (Some((currCommit, currVersion)), None) if currCommit == headCommit =>
            GitBranchStateOneReleaseHead(currCommit, currVersion)

          case (Some((currCommit, currVersion)), _) =>
            val headCommitWithCount = GitCommitWithCount(headCommit, getCommitCount(Some(currCommit.fullHash)))
            GitBranchStateOneReleaseNotHead(headCommitWithCount, currCommit, currVersion)

          case (None, _) =>
            GitBranchStateNoReleases(GitCommitWithCount(headCommit, getCommitCount(None)))
        }

      case None =>
        GitBranchStateNoCommits
    }
  }

  override def workingState: GitWorkingState = {
    GitWorkingState(!checkClean())
  }

  override def getCommitCount(hash: Option[String]): Int = {
    val limitStr = hash.map("^" + _).getOrElse("")
    val (_, output) = runCommand(s"""git rev-list --first-parent --count HEAD $limitStr""".trim)
    output.mkString("").trim.toInt
  }

  /**
    * Executes a single "git log" command.
    */
  private def gitLog(arguments: String): Seq[GitCommit] = {
    require(isGitRepo(dir), "Must be in a git repository")

    // originally this used "git describe", but that doesn't always work the way you want. its definition of "nearest"
    // tag is not always what you think it means: it does NOT search backward to the root, it will search other
    // branches too. See http://www.xerxesb.com/2010/12/20/git-describe-and-the-tale-of-the-wrong-commits/
    val cmd = s"git log --oneline --decorate=short --first-parent --simplify-by-decoration --no-abbrev-commit $arguments"
    /**
      * Example output:
      * {{{
      *    34f0ea0e25cf4c57bd0b732c03d19fc18492b827 (HEAD -> tagging-test, tag: rawr) [CODE-2] Add tests for tagging.
      *    a204a6127c290305c12a417eb1c6ac5490da86ae (upstream/master, master) [CODE-2] Remove "latest.integration" recommendation. (#43)
      *    69a1739e82f9c31e0a3a6a70e006b6d063ba403a (rcmurphy/master) NOJIRA: Adding contribution guidelines
      *    77edaa07f4552602a18f45b99e92c9f2f45e2eee (tag: v1.0.0-m0) Fix ivy artifactory resolver pattern. (#36)
      * }}}
      */
    val (exitCode, output) = runCommand(cmd, throwIfNonZero = false)
    exitCode match {
      // you get 128 when you run 'git log' on a repository with no commits
      case 0 | 128 =>
        val abbreviatedHashLength = findAbbreviatedHashLength()
        output.map(line => GitCommit.fromGitLog(line, abbreviatedHashLength))
      case ret => throw new IllegalStateException(s"Non-zero exit code when running git log: $ret")
    }
  }

  private def checkClean(): Boolean = {
    // I changed from using "git diff-index" because that returns an error when there are no commits
    // We only check tracked files (files that have been "git add"-ed before) because un-tracked files are likely
    // temporary junk created by build tools (e.g. Jenkins, Docker, npm, etc.)
    val (_, output) = runCommand("git status --porcelain --untracked-files=no")
    output.mkString("").isEmpty
  }

  /**
    * Returns the abbreviated hash length for a Git hash object. The length of a hash is variable depending on the
    * number of commits.
    *
    * See [[https://stackoverflow.com/questions/18134627/how-much-of-a-git-sha-is-generally-considered-necessary-to-uniquely-identify-a]]
    * and [[https://stackoverflow.com/questions/16413373/git-get-short-hash-from-regular-hash]]
    * and [[https://stackoverflow.com/questions/32405922/in-my-repo-how-long-must-the-longest-hash-prefix-be-to-prevent-any-overlap]]
    * for more information.
    */
  private def findAbbreviatedHashLength(): Int = {
    val (exitCode, output) = runCommand("git rev-parse --short HEAD", throwIfNonZero = false)
    exitCode match {
      // you get 128 when you run 'git rev-parse' on a repository with no commits
      case 0 | 128 =>
        // 7 is the default hash length, so let's use that if the commit length is shorter OR we have zero commits
        math.max(output.mkString("").trim.length, 7)
      case ret =>
        throw new IllegalStateException(s"Non-zero exit code when running git rev-parse: $ret")
    }
  }

  /**
    * Executes a single shell command, typically a 'git' command.
    *
    * @param throwIfNonZero If true throws an exception if the exit code is non-zero. Otherwise returns it. True
    * can simplify your logic if the exit code is only for failure status.
    * @return ( Exit code, standard output )
    */
  private def runCommand(cmd: String, throwIfNonZero: Boolean = true): (Int, Seq[String]) = {
    require(isGitRepo(dir), "Must be in a git repository")
    val outputLogger = new BufferingProcessLogger
    val exitCode: Int = Process(cmd, dir) ! outputLogger
    val result = (exitCode, outputLogger.stdout)

    if (throwIfNonZero && exitCode != 0)
      throw new IllegalStateException(s"Non-zero exit code when running '$cmd': $exitCode")

    result
  }

}
