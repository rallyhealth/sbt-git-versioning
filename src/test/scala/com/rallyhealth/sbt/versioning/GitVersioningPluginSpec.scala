package com.rallyhealth.sbt.versioning

import com.rallyhealth.sbt.versioning.GitVersioningPlugin.BoundedSemanticVersion
import org.scalatest.FunSpec

class GitVersioningPluginSpec extends FunSpec {

  // for these tests we need a full hash, not an abbreviation
  private val gitHash1 = HashSemVerIdentifierSpec.expandHash("0123abc")
  private val gitHash2 = HashSemVerIdentifierSpec.expandHash("4567abc")

  describe("BoundedSemanticVersion") {

    describe("ReleaseVersion") {

      it("equal") {
        val version = ReleaseVersion(1, 2, 3, SemVerIdentifierList.empty, isDirty = false)
        val head = GitBranchStateOneReleaseHead(GitCommit(gitHash1, 7, Seq("1.0.0")))
        val result = version.lowerBound(version, head)
        assert(result === version)
      }

      it("less") {
        val version = ReleaseVersion(1, 2, 3, SemVerIdentifierList.empty, isDirty = false)
        val bound = ReleaseVersion(1, 0, 0, SemVerIdentifierList.empty, isDirty = false)
        val head = GitBranchStateOneReleaseHead(GitCommit(gitHash1, 7, Seq("1.0.0")))
        val result = version.lowerBound(bound, head)
        assert(result.toString === "1.2.3")
      }

      describe("GitBranchStateTwoReleases") {

        it("greater") {
          val version = ReleaseVersion(1, 2, 3, SemVerIdentifierList.empty, isDirty = false)
          val bound = ReleaseVersion(2, 0, 0, SemVerIdentifierList.empty, isDirty = false)
          val head = GitBranchStateTwoReleases(
            GitCommit(gitHash1, 7, Seq("1.2.3")),
            version,
            GitCommit(gitHash2, 7, Seq("1.0.0")),
            ReleaseVersion(1, 0, 0, SemVerIdentifierList.empty, isDirty = false))
          val result = version.lowerBound(bound, head)
          assert(result.toString === "2.0.0")
        }

        it("version is dirty") {
          val version = ReleaseVersion(1, 2, 3, SemVerIdentifierList.empty, isDirty = true)
          val bound = ReleaseVersion(2, 0, 0, SemVerIdentifierList.empty, isDirty = false)
          val head = GitBranchStateTwoReleases(
            GitCommit(gitHash1, 7, Seq("1.2.3")),
            version.copy(isDirty = false),
            GitCommit(gitHash2, 7, Seq("1.0.0")),
            ReleaseVersion(1, 0, 0, SemVerIdentifierList.empty, isDirty = false))
          val result = version.lowerBound(bound, head)
          assert(result.toString === "2.0.0-dirty-SNAPSHOT")
        }

        it("bound is dirty") {
          val version = ReleaseVersion(1, 2, 3, SemVerIdentifierList.empty, isDirty = false)
          val bound = ReleaseVersion(2, 0, 0, SemVerIdentifierList.empty, isDirty = true)
          val head = GitBranchStateTwoReleases(
            GitCommit(gitHash1, 7, Seq("1.2.3")),
            version,
            GitCommit(gitHash2, 7, Seq("1.0.0")),
            ReleaseVersion(1, 0, 0, SemVerIdentifierList.empty, isDirty = false))
          val result = version.lowerBound(bound, head)
          assert(result.toString === "2.0.0")
        }
      }

      describe("GitBranchStateOneReleaseNotHead") {

        it("greater") {
          val version = ReleaseVersion(1, 2, 3, SemVerIdentifierList.empty, isDirty = false)
          val bound = ReleaseVersion(2, 0, 0, SemVerIdentifierList.empty, isDirty = false)
          val head = GitBranchStateOneReleaseNotHead(
            GitCommitWithCount(GitCommit(gitHash1, 7, Seq.empty), 2),
            GitCommit(gitHash2, 7, Seq("1.0.0")),
            ReleaseVersion(1, 0, 0, SemVerIdentifierList.empty, isDirty = false))
          val result = version.lowerBound(bound, head)
          assert(result.toString === s"2.0.0-2-${gitHash1.take(7)}-SNAPSHOT")
        }

        it("version is dirty") {
          val version = ReleaseVersion(1, 2, 3, SemVerIdentifierList.empty, isDirty = true)
          val bound = ReleaseVersion(2, 0, 0, SemVerIdentifierList.empty, isDirty = false)
          val head = GitBranchStateOneReleaseNotHead(
            GitCommitWithCount(GitCommit(gitHash1, 7, Seq.empty), 2),
            GitCommit(gitHash2, 7, Seq("1.0.0")),
            ReleaseVersion(1, 0, 0, SemVerIdentifierList.empty, isDirty = false))
          val result = version.lowerBound(bound, head)
          assert(result.toString === s"2.0.0-2-${gitHash1.take(7)}-dirty-SNAPSHOT")
        }

        it("bound is dirty") {
          val version = ReleaseVersion(1, 2, 3, SemVerIdentifierList.empty, isDirty = false)
          val bound = ReleaseVersion(2, 0, 0, SemVerIdentifierList.empty, isDirty = true)
          val head = GitBranchStateOneReleaseNotHead(
            GitCommitWithCount(GitCommit(gitHash1, 7, Seq.empty), 2),
            GitCommit(gitHash2, 7, Seq("1.0.0")),
            ReleaseVersion(1, 0, 0, SemVerIdentifierList.empty, isDirty = false))
          val result = version.lowerBound(bound, head)
          assert(result.toString === s"2.0.0-2-${gitHash1.take(7)}-SNAPSHOT")
        }
      }

      describe("GitBranchStateOneReleaseHead") {

        it("greater") {
          val version = ReleaseVersion(1, 2, 3, SemVerIdentifierList.empty, isDirty = false)
          val bound = ReleaseVersion(2, 0, 0, SemVerIdentifierList.empty, isDirty = false)
          val head = GitBranchStateOneReleaseHead(GitCommit(gitHash1, 7, Seq("1.2.3")), version)
          val result = version.lowerBound(bound, head)
          assert(result.toString === "2.0.0")
        }

        it("version is dirty") {
          val version = ReleaseVersion(1, 2, 3, SemVerIdentifierList.empty, isDirty = true)
          val bound = ReleaseVersion(2, 0, 0, SemVerIdentifierList.empty, isDirty = false)
          val head = GitBranchStateOneReleaseHead(GitCommit(gitHash1, 7, Seq("1.2.3")), version.copy(isDirty = false))
          val result = version.lowerBound(bound, head)
          assert(result.toString === "2.0.0-dirty-SNAPSHOT")
        }

        it("bound is dirty") {
          val version = ReleaseVersion(1, 2, 3, SemVerIdentifierList.empty, isDirty = false)
          val bound = ReleaseVersion(2, 0, 0, SemVerIdentifierList.empty, isDirty = true)
          val head = GitBranchStateOneReleaseHead(GitCommit(gitHash1, 7, Seq("1.2.3")), version)
          val result = version.lowerBound(bound, head)
          assert(result.toString === "2.0.0")
        }
      }

      describe("GitBranchStateNoReleases") {

        it("greater") {
          val version = ReleaseVersion(1, 2, 3, SemVerIdentifierList.empty, isDirty = false)
          val bound = ReleaseVersion(2, 0, 0, SemVerIdentifierList.empty, isDirty = false)
          val head = GitBranchStateNoReleases(GitCommitWithCount(GitCommit(gitHash1, 7, Seq.empty), 2))
          val result = version.lowerBound(bound, head)
          assert(result.toString === s"2.0.0-2-${gitHash1.take(7)}-SNAPSHOT")
        }

        it("version is dirty") {
          val version = ReleaseVersion(1, 2, 3, SemVerIdentifierList.empty, isDirty = true)
          val bound = ReleaseVersion(2, 0, 0, SemVerIdentifierList.empty, isDirty = false)
          val head = GitBranchStateNoReleases(GitCommitWithCount(GitCommit(gitHash1, 7, Seq.empty), 2))
          val result = version.lowerBound(bound, head)
          assert(result.toString === s"2.0.0-2-${gitHash1.take(7)}-dirty-SNAPSHOT")
        }

        it("bound is dirty") {
          val version = ReleaseVersion(1, 2, 3, SemVerIdentifierList.empty, isDirty = false)
          val bound = ReleaseVersion(2, 0, 0, SemVerIdentifierList.empty, isDirty = true)
          val head = GitBranchStateNoReleases(GitCommitWithCount(GitCommit(gitHash1, 7, Seq.empty), 2))
          val result = version.lowerBound(bound, head)
          assert(result.toString === s"2.0.0-2-${gitHash1.take(7)}-SNAPSHOT")
        }
      }

      describe("GitBranchStateNoCommits") {

        it("greater") {
          val version = ReleaseVersion(1, 2, 3, SemVerIdentifierList.empty, isDirty = false)
          val bound = ReleaseVersion(2, 0, 0, SemVerIdentifierList.empty, isDirty = false)
          val result = version.lowerBound(bound, GitBranchStateNoCommits)
          assert(result.toString === "2.0.0")
        }

        it("version is dirty") {
          val version = ReleaseVersion(1, 2, 3, SemVerIdentifierList.empty, isDirty = true)
          val bound = ReleaseVersion(2, 0, 0, SemVerIdentifierList.empty, isDirty = false)
          val result = version.lowerBound(bound, GitBranchStateNoCommits)
          assert(result.toString === "2.0.0")
        }

        it("bound is dirty") {
          val version = ReleaseVersion(1, 2, 3, SemVerIdentifierList.empty, isDirty = false)
          val bound = ReleaseVersion(2, 0, 0, SemVerIdentifierList.empty, isDirty = true)
          val result = version.lowerBound(bound, GitBranchStateNoCommits)
          assert(result.toString === "2.0.0-dirty-SNAPSHOT")
        }
      }
    }

    describe("SnapshotVersion") {

      it("equal") {
        val version = SnapshotVersion(1, 2, 3, SemVerIdentifierList.empty, isDirty = false, gitHash1.take(7), 1)
        val head = GitBranchStateOneReleaseHead(GitCommit(gitHash1, 7, Seq("1.0.0")))
        val result = version.lowerBound(version.toRelease, head)
        assert(result.toString === s"1.2.3-1-${gitHash1.take(7)}-SNAPSHOT")
      }

      it("less") {
        val version = SnapshotVersion(1, 2, 3, SemVerIdentifierList.empty, isDirty = false, gitHash1.take(7), 1)
        val bound = ReleaseVersion(1, 0, 0, SemVerIdentifierList.empty, isDirty = false)
        val head = GitBranchStateOneReleaseHead(GitCommit(gitHash1, 7, Seq("1.0.0")))
        val result = version.lowerBound(bound, head)
        assert(result.toString === s"1.2.3-1-${gitHash1.take(7)}-SNAPSHOT")
      }

      it("greater") {
        val version = SnapshotVersion(1, 2, 3, SemVerIdentifierList.empty, isDirty = false, gitHash1.take(7), 1)
        val bound = ReleaseVersion(2, 0, 0, SemVerIdentifierList.empty, isDirty = false)
        val head = GitBranchStateTwoReleases(
          GitCommit(gitHash1, 7, Seq("1.2.3")),
          version.toRelease,
          GitCommit(gitHash2, 7, Seq("1.0.0")),
          ReleaseVersion(1, 0, 0, SemVerIdentifierList.empty, isDirty = false))
        val result = version.lowerBound(bound, head)
        assert(result.toString === s"2.0.0-1-${gitHash1.take(7)}-SNAPSHOT")
      }

      it("version is dirty") {
        val version = SnapshotVersion(1, 2, 3, SemVerIdentifierList.empty, isDirty = true, gitHash1.take(7), 1)
        val bound = ReleaseVersion(2, 0, 0, SemVerIdentifierList.empty, isDirty = false)
        val head = GitBranchStateTwoReleases(
          GitCommit(gitHash1, 7, Seq("1.2.3")),
          version.toRelease.copy(isDirty = false),
          GitCommit(gitHash2, 7, Seq("1.0.0")),
          ReleaseVersion(1, 0, 0, SemVerIdentifierList.empty, isDirty = false))
        val result = version.lowerBound(bound, head)
        assert(result.toString === s"2.0.0-1-${gitHash1.take(7)}-dirty-SNAPSHOT")
      }

      it("bound is dirty") {
        val version = SnapshotVersion(1, 2, 3, SemVerIdentifierList.empty, isDirty = false, gitHash1.take(7), 1)
        val bound = ReleaseVersion(2, 0, 0, SemVerIdentifierList.empty, isDirty = true)
        val head = GitBranchStateTwoReleases(
          GitCommit(gitHash1, 7, Seq("1.2.3")),
          version.toRelease,
          GitCommit(gitHash2, 7, Seq("1.0.0")),
          ReleaseVersion(1, 0, 0, SemVerIdentifierList.empty, isDirty = false))
        val result = version.lowerBound(bound, head)
        assert(result.toString === s"2.0.0-1-${gitHash1.take(7)}-SNAPSHOT")
      }
    }
  }
}
