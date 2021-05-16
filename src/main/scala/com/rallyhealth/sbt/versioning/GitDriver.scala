package com.rallyhealth.sbt.versioning

import java.io.File

import sbt.util._
import scala.sys.process._

/**
  * Driver that allows executing common commands against a git working directory.
  *
  * See also [[GitFetcher]].
  */
trait GitDriver {

  /** Used to find the previous version and to check whether the HEAD and version commit are the same   
   * @param abbrevLength The length used in the abbreviated hash for commits.
   */
  def branchState(abbrevLength: Int): GitBranchState

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
    * @param abbrevLength The length used in the abbreviated hash for commits.
    */
  def calcCurrentVersion(ignoreDirty: Boolean, abbrevLength: Int): SemanticVersion = {

    val currVersion: SemanticVersion = branchState(abbrevLength) match {

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
  require(isGitCompatible, "Must be git version 2.X.X or greater")

  private class GitException(msg: String) extends Exception(msg)

  // Validate that the git version is over 2.X.X
  protected def isGitCompatible: Boolean = {
    val outputLogger = new BufferingProcessLogger
    val exitCode: Int = Process(s"""git --version""", dir) ! outputLogger
    // git --version returns: git version x.y.z
    val gitSemver = """git version (\d+)\.(\d+)\.(\d+).*""".r
    exitCode match {
      case 0 =>
        val gitVersion = outputLogger.stdout.mkString("").trim.toLowerCase
        gitVersion match {
          case gitSemver(major, minor, patch) =>
            major.toInt > 1
          case _ =>
            throw new GitException(
              s"""Version output was not of the form 'git version x.y.z'
              |version was '${gitVersion}'""".stripMargin)
        }
      case unexpected =>
        throw new GitException(
          s"""Unexpected git exit status: $unexpected
             |stderr:
             |${outputLogger.stderr.mkString("\n")}
             |stdout:
             |${outputLogger.stdout.mkString("\n")}""".stripMargin
        )
    }
  }
  private def isGitRepo(dir: File): Boolean = {
    // this does NOT use runCommand() because that uses this method to check if the directory is a git directory
    val outputLogger = new BufferingProcessLogger
    // http://stackoverflow.com/a/16925062
    val exitCode: Int = Process(s"""git rev-parse --is-inside-work-tree""", dir) ! outputLogger
    exitCode match {
      case 0 => outputLogger.stdout.mkString("").trim.toLowerCase == "true"
      case 128 => false // https://stackoverflow.com/a/19441790
      case unexpected =>
        throw new GitException(
          s"""Unexpected git exit status: $unexpected
             |stderr:
             |${outputLogger.stderr.mkString("\n")}
             |stdout:
             |${outputLogger.stdout.mkString("\n")}""".stripMargin
        )
    }
  }

  override def branchState(abbrevLength: Int): GitBranchState = {
    gitLog("--max-count=1", abbrevLength).headOption match {

      case Some(headCommit) =>

        // we only care about the RELEASE commits so let's get them in order from reflog
        val releaseRefs: Seq[(GitCommit, ReleaseVersion)] = gitForEachRef("", abbrevLength).collect { case gc @ ReleaseVersion(rv) => (gc, rv) }

        // We only care about the current release and previous release so let's take the top two.
        // Then we want to find out what which git log commit is associated to the reflog sha.
        // Note: gitForEachRef will return return reference shas and the tags associated with them.
        // the reference shas are not always the same as the commit shas associated with git log.
        // That is why we have to run the command here to find the correct sha
        // Note: do not move git log into gitForEachRef as on long revisions it will take a LOT of time
        val releases: Seq[(GitCommit, ReleaseVersion)] = releaseRefs.take(2).map(tp =>
          (gitLog(s"${tp._1.fullHash} --max-count=1", abbrevLength).head, tp._2)
        )

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
    * Executes git rev-parse to determine the current branch/HEAD commit
    */
  private def gitBranch: String = {
    val cmd = s"git rev-parse --abbrev-ref HEAD"
    val (exitCode, output) = runCommand(cmd, throwIfNonZero = false)
    exitCode match {
      // you get 128 when you run a git cmd in a dir not under git vcs
      case 0 =>
        val res = output.map { line =>
          line
        }
        res.head
      case 128 =>
        throw new IllegalStateException(
          s"Error 128: a git cmd was run in a dir that is not under git vcs or git rev-parse failed to run.")
    }
  }

  /**
    * Returns an ordered list of versions that are merged into your branch.
    */
  private def gitForEachRef(arguments: String, abbrevLength: Int): Seq[GitCommit] = {
    require(isGitRepo(dir), "Must be in a git repository")
    require(isGitCompatible, "Must be git version 2.X.X or greater")

    // Note: nested shell commands, piping and redirection will not work with runCommand since it is just
    // invoking an OS process. You could invoke a shell and pass expressions if needed.
    val cmd = s"git for-each-ref --sort=-v:refname refs/tags --merged=${gitBranch}"

    /**
      * Example output:
      * {{{
      * 686623c25b52e40fe6270ab57419551b88e89dfe tag    refs/tags/v1.0.0
      * fb22d49dd7d7bf5b5f130c4ff3b66667d97bc308 commit refs/tags/v0.0.3
      * 5ca402250fd63e6ac3a9b51d457b89c092195098 commit refs/tags/v0.0.2
      * }}}
      */

    // val (exitCode, output) = runCommand(cmd, throwIfNonZero = false)
    val (exitCode, output) = runCommand(cmd, throwIfNonZero = false)
    exitCode match {
      // you get 128 when you run 'git log' on a repository with no commits
      case 0 | 128 =>
        val abbreviatedHashLength = findAbbreviatedHashLength(abbrevLength)
        output map { line =>
          GitCommit.fromGitRef(line, abbreviatedHashLength)
        }
      case ret => throw new IllegalStateException(s"Non-zero exit code when running git log: $ret")
    }
  }

  /**
    * Executes a single "git log" command.
    */
  private def gitLog(arguments: String, abbrevLength: Int): Seq[GitCommit] = {
    require(isGitRepo(dir), "Must be in a git repository")
    require(isGitCompatible, "Must be git version 2.X.X or greater")

    // originally this used "git describe", but that doesn't always work the way you want. its definition of "nearest"
    // tag is not always what you think it means: it does NOT search backward to the root, it will search other
    // branches too. See http://www.xerxesb.com/2010/12/20/git-describe-and-the-tale-of-the-wrong-commits/
    // The old command was the following:
    //   git log --oneline --decorate=short --first-parent --simplify-by-decoration --no-abbrev-commit
    //   which has the argument --first-parent. The problem is that first-parent will hide release that are done in in another
    //   branch and merged into master.

    val cmd = s"git log --oneline --decorate=short --simplify-by-decoration --no-abbrev-commit $arguments"
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
        val abbreviatedHashLength = findAbbreviatedHashLength(abbrevLength)
        output map { line =>
          GitCommit.fromGitLog(line, abbreviatedHashLength)
        }
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
  private def findAbbreviatedHashLength(abbrevLength: Int): Int = {
    val (exitCode, output) = runCommand(s"git rev-parse --short=${abbrevLength} HEAD", throwIfNonZero = false)
    exitCode match {
      // you get 128 when you run 'git rev-parse' on a repository with no commits
      case 0 | 128 =>
        math.max(output.mkString("").trim.length, abbrevLength)
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
    require(isGitCompatible, "Must be git version 2.X.X or greater")
    val outputLogger = new BufferingProcessLogger
    val exitCode: Int = Process(cmd, dir) ! outputLogger
    val result = (exitCode, outputLogger.stdout)

    if (throwIfNonZero && exitCode != 0)
      throw new IllegalStateException(s"Non-zero exit code when running '$cmd': $exitCode")

    result
  }

}
