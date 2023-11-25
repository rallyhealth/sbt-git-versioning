package com.rallyhealth.sbt.versioning

import java.io.File

import com.rallyhealth.sbt.versioning.GitDriverImplSpec.MockDriver
import org.scalatest.FunSpec

import scala.sys.process._
import scala.util.Random

class GitDriverImplSpec extends FunSpec {

  private val workingDir = new File("doesNotExist.delete").getAbsoluteFile.getParentFile

  describe("gitState") {

    it("isRepoDirty") {
      // did it do something? I don't care what it did really, as long as it didn't throw
      val driver = new GitDriverImpl(workingDir)
      val dirty = driver.workingState.isDirty
      // I need to do something or the compiler could eliminate the call
      assert(dirty || !dirty)
    }

    it("commitCount") {
      // did it do something? I don't care what it did really, as long as it didn't throw
      val driver = new GitDriverImpl(workingDir)
      assert(driver.getCommitCount(None) > 1)
    }

    it("branchState") {
      // did it do something? I don't care what it did really, as long as it didn't throw
      val driver = new GitDriverImpl(workingDir)
      val branchState = driver.branchState(7)
      // I need to do something or the compiler could eliminate the call
      assert(branchState === branchState)
    }
  }

  describe("getCommitCount") {

    val driver = new GitDriverImpl(workingDir)

    it("no limit (count to head)") {
      val commitCount = driver.getCommitCount(None)
      assert(commitCount > 1) // did it do something? I don't care what it did really
    }

    it("limit (count back 3)") {
      // find a previous commit (specifically 3 commits ago)
      val prevHash =
        Process(s"""git rev-list --first-parent --max-count=3 HEAD""".trim, workingDir).!!.trim.split('\n').lastOption
      val commitCount = driver.getCommitCount(prevHash)
      assert(commitCount === 2)
    }
  }

  describe("isClean") {

    val tempDir = new File(System.getProperty("java.io.tmpdir"))

    it("empty == clean") {
      val fakeGitRepo = "testing_temp_git_repo_" + Random.nextInt(1000000000)
      Process(s"""git init $fakeGitRepo""".trim, tempDir).!!
      val newWorkingDir = new File(System.getProperty("java.io.tmpdir"), fakeGitRepo)

      val driver = new GitDriverImpl(newWorkingDir)
      val isCleanOutput = Process(s"""git status --porcelain""".trim, newWorkingDir).!!.trim
      val isClean = isCleanOutput.isEmpty
      assert(isClean !== driver.workingState.isDirty, isCleanOutput)
    }

    it("add untracked files == clean") {
      val fakeGitRepo = "testing_temp_git_repo_" + Random.nextInt(1000000000)
      Process(s"""git init $fakeGitRepo""".trim, tempDir).!!
      val newWorkingDir = new File(System.getProperty("java.io.tmpdir"), fakeGitRepo)

      val isCleanOutput = Process(s"""git status --porcelain""".trim, newWorkingDir).!!.trim
      val isClean = isCleanOutput.isEmpty
      assert(isClean) // sanity check

      val fileName = "testing_only_should_be_deleted"
      val tempFile = new File(newWorkingDir, fileName)
      assert(tempFile.createNewFile())

      val driver = new GitDriverImpl(newWorkingDir)
      assert(!driver.workingState.isDirty)
    }

    it("add tracked files == dirty") {
      val fakeGitRepo = "testing_temp_git_repo_" + Random.nextInt(1000000000)
      Process(s"""git init $fakeGitRepo""".trim, tempDir).!!
      val newWorkingDir = new File(System.getProperty("java.io.tmpdir"), fakeGitRepo)

      val isCleanOutput = Process(s"""git status --porcelain""".trim, newWorkingDir).!!.trim
      val isClean = isCleanOutput.isEmpty
      assert(isClean) // sanity check

      val fileName = "testing_only_should_be_deleted"
      val tempFile = new File(newWorkingDir, fileName)
      assert(tempFile.createNewFile())
      Process(s"""git add $fileName""".trim, newWorkingDir).!!

      val driver = new GitDriverImpl(newWorkingDir)
      assert(driver.workingState.isDirty)
    }
  }

  describe("calcCurrentVersion") {

    // for these tests we need a full hash, not an abbreviation
    val gitHash = HashSemVerIdentifierSpec.expandHash("0123abc")
    val gitHashAlt = HashSemVerIdentifierSpec.expandHash("4567def")

    describe("some commit is tagged") {

      describe("GitBranchStateNoCommits") {

        it("dirty") {
          val branchState = GitBranchStateNoCommits
          val workingState = GitWorkingState(isDirty = true)
          val gitDriver = MockDriver(branchState, workingState)
          val result = gitDriver.calcCurrentVersion(ignoreDirty = false, abbrevLength = 7)
          assert(result.toString === "0.0.1-dirty-SNAPSHOT") // check against the README.md
          assert(result === ReleaseVersion.initialVersion.copy(isDirty = true))
        }

        it("clean") {
          val branchState = GitBranchStateNoCommits
          val workingState = GitWorkingState(isDirty = false)
          val gitDriver = MockDriver(branchState, workingState)
          val result = gitDriver.calcCurrentVersion(ignoreDirty = false, abbrevLength = 7)
          assert(result.toString === "0.0.1") // check against the README.md
          assert(result === ReleaseVersion.initialVersion)
        }
      }

      describe("GitBranchStateNoReleases") {

        it("head is release commit") {
          intercept[IllegalArgumentException] {
            GitBranchStateNoReleases(GitCommitWithCount(GitCommit(gitHash, 7, Seq("1.0.0")), 1))
          }
        }

        it("dirty") {
          val branchState = GitBranchStateNoReleases(GitCommitWithCount(GitCommit(gitHash, 7, Seq.empty), 1))
          val workingState = GitWorkingState(isDirty = true)
          val gitDriver = MockDriver(branchState, workingState)
          val result = gitDriver.calcCurrentVersion(ignoreDirty = false, abbrevLength = 7)
          assert(result.toString === "0.0.1-1-0123abc-dirty-SNAPSHOT") // check against the README.md
          assert(result === SnapshotVersion(0, 0, 1, SemVerIdentifierList.empty, isDirty = true, gitHash.take(7), 1))
        }

        it("clean") {
          val branchState = GitBranchStateNoReleases(GitCommitWithCount(GitCommit(gitHash, 7, Seq.empty), 1))
          val workingState = GitWorkingState(isDirty = false)
          val gitDriver = MockDriver(branchState, workingState)
          val result = gitDriver.calcCurrentVersion(ignoreDirty = false, abbrevLength = 7)
          assert(result.toString === "0.0.1-1-0123abc-SNAPSHOT") // check against the README.md
          assert(result === SnapshotVersion(0, 0, 1, SemVerIdentifierList.empty, isDirty = false, gitHash.take(7), 1))
        }
      }

      describe("GitBranchStateOneReleaseHead") {

        it("dirty release") {
          val prevRelease = ReleaseVersion(1, 0, 0, SemVerIdentifierList.empty, isDirty = true)
          intercept[IllegalArgumentException] {
            GitBranchStateOneReleaseHead(GitCommit(gitHash, 7, Seq("1.0.0")), prevRelease)
          }
        }

        it("head tag != version") {
          val prevRelease = ReleaseVersion(1, 0, 0, SemVerIdentifierList.empty, isDirty = false)
          intercept[IllegalArgumentException] {
            GitBranchStateOneReleaseHead(GitCommit(gitHash, 7, Seq("2.0.0")), prevRelease)
          }
        }

        it("clean") {
          val prevRelease = ReleaseVersion(1, 1, 0, SemVerIdentifierList.empty, isDirty = false)
          val branchState = GitBranchStateOneReleaseHead(GitCommit(gitHash, 7, Seq("1.1.0")), prevRelease)
          val workingState = GitWorkingState(isDirty = false)
          val gitDriver = MockDriver(branchState, workingState)
          val result = gitDriver.calcCurrentVersion(ignoreDirty = false, abbrevLength = 7)
          assert(result.toString === "1.1.0") // check against the README.md
          assert(result === prevRelease)
        }
      }

      describe("GitBranchStateOneReleaseNotHead") {

        it("head == prev") {
          val commit = GitCommit(gitHash, 7, Seq("1.0.0"))
          intercept[IllegalArgumentException] {
            GitBranchStateOneReleaseNotHead(GitCommitWithCount(commit, 1), commit)
          }
        }

        it("head.hash == prev.hash") {
          intercept[IllegalArgumentException] {
            GitBranchStateOneReleaseNotHead(
              GitCommitWithCount(GitCommit(gitHash, 7, Seq.empty), 1), GitCommit(gitHash, 7, Seq("1.0.0")))
          }
        }

        it("head is release commit") {
          val prevRelease = ReleaseVersion(1, 0, 0, SemVerIdentifierList.empty, isDirty = false)
          intercept[IllegalArgumentException] {
            GitBranchStateOneReleaseNotHead(
              GitCommitWithCount(GitCommit(gitHash, 7, Seq("2.0.0")), 1),
              GitCommit(gitHashAlt, 7, Seq("1.0.0")),
              prevRelease)
          }
        }

        it("head tag != version") {
          val prevRelease = ReleaseVersion(1, 0, 3, SemVerIdentifierList.empty, isDirty = false)
          intercept[IllegalArgumentException] {
            GitBranchStateOneReleaseNotHead(
              GitCommitWithCount(GitCommit(gitHash, 7, Seq.empty), 1),
              GitCommit(gitHashAlt, 7, Seq("2.0.0")),
              prevRelease)
          }
        }

        it("dirty release") {
          val prevRelease = ReleaseVersion(1, 0, 0, SemVerIdentifierList.empty, isDirty = true)
          intercept[IllegalArgumentException] {
            GitBranchStateOneReleaseNotHead(
              GitCommitWithCount(GitCommit(gitHash, 7, Seq.empty), 1),
              GitCommit(gitHashAlt, 7, Seq("1.0.0-dirty-SNAPSHOT")),
              prevRelease)
          }
        }

        it("dirty") {
          val prevRelease = ReleaseVersion(1, 0, 3, SemVerIdentifierList.empty, isDirty = false)
          val branchState = GitBranchStateOneReleaseNotHead(
            GitCommitWithCount(GitCommit(gitHash, 7, Seq.empty), 1),
            GitCommit(gitHashAlt, 7, Seq("1.0.3")),
            prevRelease)
          val workingState = GitWorkingState(isDirty = true)
          val gitDriver = MockDriver(branchState, workingState)
          val result = gitDriver.calcCurrentVersion(ignoreDirty = false, abbrevLength = 7)
          assert(result.toString === "1.0.4-1-0123abc-dirty-SNAPSHOT") // check against the README.md
          assert(result === SnapshotVersion(1, 0, 4, SemVerIdentifierList.empty, isDirty = true, gitHash.take(7), 1))
        }

        it("clean") {
          val prevRelease = ReleaseVersion(1, 0, 3, SemVerIdentifierList.empty, isDirty = false)
          val branchState = GitBranchStateOneReleaseNotHead(
            GitCommitWithCount(GitCommit(gitHash, 7, Seq.empty), 1),
            GitCommit(gitHashAlt, 7, Seq("1.0.3")),
            prevRelease)
          val workingState = GitWorkingState(isDirty = false)
          val gitDriver = MockDriver(branchState, workingState)
          val result = gitDriver.calcCurrentVersion(ignoreDirty = false, abbrevLength = 7)
          assert(result.toString === "1.0.4-1-0123abc-SNAPSHOT") // check against the README.md
          assert(result === SnapshotVersion(1, 0, 4, SemVerIdentifierList.empty, isDirty = false, gitHash.take(7), 1))
        }
      }

      describe("GitBranchStateTwoReleases") {

        it("head == prev") {
          val commit = GitCommit(gitHash, 7, Seq("1.0.0"))
          intercept[IllegalArgumentException] {
            GitBranchStateTwoReleases(commit, commit)
          }
        }

        it("head.hash == prev.hash") {
          intercept[IllegalArgumentException] {
            GitBranchStateTwoReleases(GitCommit(gitHash, 7, Seq("2.0.0")), GitCommit(gitHash, 7, Seq("1.0.0")))
          }
        }

        it("head tag != head version") {
          val headCommit = GitCommit(gitHash, 7, Seq("2.0.0"))
          val prevCommit = GitCommit(gitHash, 7, Seq("1.0.0"))
          intercept[IllegalArgumentException] {
            GitBranchStateTwoReleases(
              headCommit,
              ReleaseVersion(3, 0, 0, SemVerIdentifierList.empty, isDirty = false),
              prevCommit,
              ReleaseVersion(1, 0, 0, SemVerIdentifierList.empty, isDirty = false))
          }
        }

        it("prev tag != prev version") {
          val headCommit = GitCommit(gitHash, 7, Seq("2.0.0"))
          val prevCommit = GitCommit(gitHash, 7, Seq("1.0.0"))
          intercept[IllegalArgumentException] {
            GitBranchStateTwoReleases(
              headCommit,
              ReleaseVersion(2, 0, 0, SemVerIdentifierList.empty, isDirty = false),
              prevCommit,
              ReleaseVersion(1, 1, 0, SemVerIdentifierList.empty, isDirty = false))
          }
        }

        it("dirty head release") {
          val headCommit = GitCommit(gitHash, 7, Seq("2.0.0"))
          val prevCommit = GitCommit(gitHash, 7, Seq("1.0.0"))
          intercept[IllegalArgumentException] {
            GitBranchStateTwoReleases(
              headCommit,
              ReleaseVersion(2, 0, 0, SemVerIdentifierList.empty, isDirty = true),
              prevCommit,
              ReleaseVersion(1, 0, 0, SemVerIdentifierList.empty, isDirty = false))
          }
        }

        it("dirty prev release") {
          val headCommit = GitCommit(gitHash, 7, Seq("2.0.0"))
          val prevCommit = GitCommit(gitHash, 7, Seq("1.0.0"))
          intercept[IllegalArgumentException] {
            GitBranchStateTwoReleases(
              headCommit,
              ReleaseVersion(2, 0, 0, SemVerIdentifierList.empty, isDirty = false),
              prevCommit,
              ReleaseVersion(1, 0, 0, SemVerIdentifierList.empty, isDirty = true))
          }
        }

        it("dirty") {
          val headCommit = GitCommit(gitHash, 7, Seq("1.1.0"))
          val prevCommit = GitCommit(gitHashAlt, 7, Seq("1.0.0"))
          val branchState = GitBranchStateTwoReleases(headCommit, prevCommit)
          val workingState = GitWorkingState(isDirty = true)
          val gitDriver = MockDriver(branchState, workingState)
          val result = gitDriver.calcCurrentVersion(ignoreDirty = false, abbrevLength = 7)
          assert(result.toString === "1.1.0-dirty-SNAPSHOT") // check against the README.md
          assert(result === ReleaseVersion(1, 1, 0, SemVerIdentifierList.empty, isDirty = true))
        }

        it("clean") {
          val headCommit = GitCommit(gitHash, 7, Seq("1.1.0"))
          val prevCommit = GitCommit(gitHashAlt, 7, Seq("1.0.0"))
          val branchState = GitBranchStateTwoReleases(headCommit, prevCommit)
          val workingState = GitWorkingState(isDirty = false)
          val gitDriver = MockDriver(branchState, workingState)
          val result = gitDriver.calcCurrentVersion(ignoreDirty = false, abbrevLength = 7)
          assert(result.toString === "1.1.0") // check against the README.md
          assert(result === ReleaseVersion(1, 1, 0, SemVerIdentifierList.empty, isDirty = false))
        }
      }
    }

    describe("ignoreDirty") {

      it("GitBranchStateNoCommits") {
        val branchState = GitBranchStateNoCommits
        val workingState = GitWorkingState(isDirty = true)
        val gitDriver = MockDriver(branchState, workingState)
        val result = gitDriver.calcCurrentVersion(ignoreDirty = true, abbrevLength = 7)
        assert(result.toString === "0.0.1")
        assert(result === ReleaseVersion.initialVersion)
      }

      it("GitBranchStateNoReleases") {
        val branchState = GitBranchStateNoReleases(GitCommitWithCount(GitCommit(gitHash, 7, Seq.empty), 1))
        val workingState = GitWorkingState(isDirty = true)
        val gitDriver = MockDriver(branchState, workingState)
        val result = gitDriver.calcCurrentVersion(ignoreDirty = true, abbrevLength = 7)
        assert(result.toString === "0.0.1-1-0123abc-SNAPSHOT")
        assert(result === SnapshotVersion(0, 0, 1, SemVerIdentifierList.empty, isDirty = false, gitHash.take(7), 1))
      }

      it("GitBranchStateOneReleaseHead") {
        val branchState = GitBranchStateOneReleaseHead(GitCommit(gitHash, 7, Seq("1.0.0")))
        val workingState = GitWorkingState(isDirty = true)
        val gitDriver = MockDriver(branchState, workingState)
        val result = gitDriver.calcCurrentVersion(ignoreDirty = true, abbrevLength = 7)
        assert(result.toString === "1.0.0")
        assert(result === ReleaseVersion(1, 0, 0, SemVerIdentifierList.empty, isDirty = false))
      }

      it("GitBranchStateOneReleaseNotHead") {
        val branchState = GitBranchStateOneReleaseNotHead(
          GitCommitWithCount(GitCommit(gitHash, 7, Seq.empty), 1), GitCommit(gitHashAlt, 7, Seq("1.0.0")))
        val workingState = GitWorkingState(isDirty = true)
        val gitDriver = MockDriver(branchState, workingState)
        val result = gitDriver.calcCurrentVersion(ignoreDirty = true, abbrevLength = 7)
        assert(result.toString === "1.0.1-1-0123abc-SNAPSHOT")
        assert(result === SnapshotVersion(1, 0, 1, SemVerIdentifierList.empty, isDirty = false, gitHash.take(7), 1))
      }

      it("GitBranchStateTwoReleases") {
        val branchState = GitBranchStateTwoReleases(
          GitCommit(gitHash, 7, Seq("2.0.0")), GitCommit(gitHashAlt, 7, Seq("1.0.0")))
        val workingState = GitWorkingState(isDirty = true)
        val gitDriver = MockDriver(branchState, workingState)
        val result = gitDriver.calcCurrentVersion(ignoreDirty = true, abbrevLength = 7)
        assert(result.toString === "2.0.0")
        assert(result === ReleaseVersion(2, 0, 0, SemVerIdentifierList.empty, isDirty = false))
      }
    }
  }

  describe("branchState") {
    val tempDir = new File(System.getProperty("java.io.tmpdir"))
    val fakeGitRepo = "testing_temp_git_repo_" + Random.nextInt(1000000000)
    Process(s"""git init $fakeGitRepo""".trim, tempDir).!!
    val newWorkingDir = new File(System.getProperty("java.io.tmpdir"), fakeGitRepo)
    val fileName = "testing_only_should_be_deleted"
    val tempFile = new File(newWorkingDir, fileName)
    assert(tempFile.createNewFile())
    Process(s"""git add $fileName""".trim, newWorkingDir).!!
    Process(s"""git commit -m 'Empty'""".trim, newWorkingDir).!!

    val driver = new GitDriverImpl(newWorkingDir)
    
    it("abbrevLength 4") {
      val version = driver.calcCurrentVersion(ignoreDirty = false, abbrevLength = 4)
      assert(version.toString.length == "0.0.1-1-abcd-SNAPSHOT".length)
    }

    it("abbrevLength 7") {
      val version = driver.calcCurrentVersion(ignoreDirty = false, abbrevLength = 7)
      assert(version.toString.length == "0.0.1-1-abcdefg-SNAPSHOT".length)
    }

    it("abbrevLength 40") {
      val version = driver.calcCurrentVersion(ignoreDirty = false, abbrevLength = 40)
      assert(version.toString.length == "0.0.1-1-abcdefghijklmnopqrstuvxyz123456789123456-SNAPSHOT".length)
    }    

  }
}

object GitDriverImplSpec {

  case class MockDriver(branchStateMock: GitBranchState, workingState: GitWorkingState) extends GitDriver {

    override def branchState(abbrevLength: Int) = branchStateMock

    override def getCommitCount(hash: Option[String]): Int = throw new UnsupportedOperationException
  }

}
