package com.rallyhealth.sbt.versioning

import com.rallyhealth.util.NullSbtLogger
import com.rallyhealth.sbt.versioning.SemanticVersion._
import com.rallyhealth.sbt.versioning._
import org.scalatest.FunSpec

import scala.util.Random

class SemanticVersionSpec extends FunSpec {

  implicit val logger = NullSbtLogger

  describe("ReleaseVersion") {

    describe("dirty is false") {

      it("basic") {
        val version = ReleaseVersion(1, 2, 3, SemVerIdentifierList.empty, isDirty = false)
        assert(version.toString === "1.2.3")
        assert(version.major === 1, version)
        assert(version.minor === 2, version)
        assert(version.patch === 3, version)
        assert(!version.isDirty, version)
        assert(version.identifiers.values.isEmpty)
        assert(ReleaseVersion.regex.pattern.matcher(version.toString).matches, version.toString)
      }

      it("with identifier") {
        val version = ReleaseVersion(1, 2, 3, Seq("rc1"), isDirty = false)
        assert(version.toString === "1.2.3-rc1")
        assert(version.major === 1, version)
        assert(version.minor === 2, version)
        assert(version.patch === 3, version)
        assert(!version.isDirty, version)
        assert(version.identifiers.values.map(_.toString) === Seq("rc1"))
        assert(ReleaseVersion.regex.pattern.matcher(version.toString).matches, version.toString)
      }

      it("with multiple identifier") {
        val version = ReleaseVersion(1, 2, 3, Seq("rc1", "m1"), isDirty = false)
        assert(version.toString === "1.2.3-rc1-m1")
        assert(version.major === 1, version)
        assert(version.minor === 2, version)
        assert(version.patch === 3, version)
        assert(!version.isDirty, version)
        assert(version.identifiers.values.map(_.toString) === Seq("rc1", "m1"))
        assert(ReleaseVersion.regex.pattern.matcher(version.toString).matches, version.toString)
      }
    }

    describe("dirty is true") {

      it("basic") {
        val version = ReleaseVersion(1, 2, 3, SemVerIdentifierList.empty, isDirty = true)
        assert(version.toString === "1.2.3-dirty-SNAPSHOT")
        assert(version.major === 1, version)
        assert(version.minor === 2, version)
        assert(version.patch === 3, version)
        assert(version.isDirty, version)
        assert(version.identifiers.values.map(_.toString) === Seq("dirty", "SNAPSHOT"))
        assert(ReleaseVersion.regex.pattern.matcher(version.toString).matches, version.toString)
      }

      it("with identifier") {
        val version = ReleaseVersion(1, 2, 3, Seq("rc1"), isDirty = true)
        assert(version.toString === "1.2.3-rc1-dirty-SNAPSHOT")
        assert(version.major === 1, version)
        assert(version.minor === 2, version)
        assert(version.patch === 3, version)
        assert(version.isDirty, version)
        assert(version.identifiers.values.map(_.toString) === Seq("rc1", "dirty", "SNAPSHOT"))
        assert(ReleaseVersion.regex.pattern.matcher(version.toString).matches, version.toString)
      }

      it("with multiple identifiers") {
        val version = ReleaseVersion(1, 2, 3, Seq("rc1", "alpha"), isDirty = true)
        assert(version.toString === "1.2.3-rc1-alpha-dirty-SNAPSHOT")
        assert(version.major === 1, version)
        assert(version.minor === 2, version)
        assert(version.patch === 3, version)
        assert(version.isDirty, version)
        assert(version.identifiers.values.map(_.toString) === Seq("rc1", "alpha", "dirty", "SNAPSHOT"))
        assert(ReleaseVersion.regex.pattern.matcher(version.toString).matches, version.toString)
      }
    }
  }

  describe("SnapshotVersion") {

    describe("dirty is false") {

      it("basic") {
        val version = SnapshotVersion(1, 2, 3, SemVerIdentifierList.empty, isDirty = false, "1234567", 99)
        assert(version.toString === "1.2.3-99-1234567-SNAPSHOT")
        assert(version.commitHash === HashSemVerIdentifier("1234567"), version)
        assert(version.commitsSincePrevRelease === CommitsSemVerIdentifier(99), version)
        assert(version.major === 1, version)
        assert(version.minor === 2, version)
        assert(version.patch === 3, version)
        assert(!version.isDirty, version)
        assert(version.identifiers.values.map(_.toString) === Seq("99", "1234567", "SNAPSHOT"))
        assert(SnapshotVersion.regex.pattern.matcher("1.2.3-99-1234567-SNAPSHOT").matches, version.toString)
      }

      it("with identifier") {
        val version = SnapshotVersion(1, 2, 3, Seq("rc1"), isDirty = false, "1234567", 9001)
        assert(version.toString === "1.2.3-rc1-9001-1234567-SNAPSHOT")
        assert(version.commitHash === HashSemVerIdentifier("1234567"), version)
        assert(version.commitsSincePrevRelease === CommitsSemVerIdentifier(9001), version)
        assert(version.major === 1, version)
        assert(version.minor === 2, version)
        assert(version.patch === 3, version)
        assert(!version.isDirty, version)
        assert(version.identifiers.values.map(_.toString) === Seq("rc1", "9001", "1234567", "SNAPSHOT"))
        assert(SnapshotVersion.regex.pattern.matcher(version.toString).matches, version.toString)
      }

      it("with multiple identifiers") {
        val version = SnapshotVersion(1, 2, 3, Seq("rc1", "beta"), isDirty = false, "1234567", 9001)
        assert(version.toString === "1.2.3-rc1-beta-9001-1234567-SNAPSHOT")
        assert(version.commitHash === HashSemVerIdentifier("1234567"), version)
        assert(version.commitsSincePrevRelease === CommitsSemVerIdentifier(9001), version)
        assert(version.major === 1, version)
        assert(version.minor === 2, version)
        assert(version.patch === 3, version)
        assert(!version.isDirty, version)
        assert(version.identifiers.values.map(_.toString) === Seq("rc1", "beta", "9001", "1234567", "SNAPSHOT"))
        assert(SnapshotVersion.regex.pattern.matcher(version.toString).matches, version.toString)
      }
    }

    describe("dirty is true") {

      it("basic") {
        val version = SnapshotVersion(1, 2, 3, SemVerIdentifierList.empty, isDirty = true, "1234567", 9001)
        assert(version.toString === "1.2.3-9001-1234567-dirty-SNAPSHOT")
        assert(version.commitHash === HashSemVerIdentifier("1234567"), version)
        assert(version.commitsSincePrevRelease === CommitsSemVerIdentifier(9001), version)
        assert(version.major === 1, version)
        assert(version.minor === 2, version)
        assert(version.patch === 3, version)
        assert(version.isDirty, version)
        assert(version.identifiers.values.map(_.toString) === Seq("9001", "1234567", "dirty", "SNAPSHOT"))
        assert(SnapshotVersion.regex.pattern.matcher(version.toString).matches, version.toString)
      }

      it("with identifier") {
        val version = SnapshotVersion(1, 2, 3, Seq("rc1"), isDirty = true, "1234567", 9001)
        assert(version.toString === "1.2.3-rc1-9001-1234567-dirty-SNAPSHOT")
        assert(version.commitHash === HashSemVerIdentifier("1234567"), version)
        assert(version.commitsSincePrevRelease === CommitsSemVerIdentifier(9001), version)
        assert(version.major === 1, version)
        assert(version.minor === 2, version)
        assert(version.patch === 3, version)
        assert(version.isDirty, version)
        assert(version.identifiers.values.map(_.toString) === Seq("rc1", "9001", "1234567", "dirty", "SNAPSHOT"))
        assert(SnapshotVersion.regex.pattern.matcher(version.toString).matches, version.toString)
      }

      it("with multiple identifiers") {
        val version = SnapshotVersion(1, 2, 3, Seq("rc1", "beta"), isDirty = true, "1234567", 99)
        assert(version.toString === "1.2.3-rc1-beta-99-1234567-dirty-SNAPSHOT")
        assert(version.commitHash === HashSemVerIdentifier("1234567"), version)
        assert(version.commitsSincePrevRelease === CommitsSemVerIdentifier(99), version)
        assert(version.major === 1, version)
        assert(version.minor === 2, version)
        assert(version.patch === 3, version)
        assert(version.isDirty, version)
        assert(version.identifiers.values.map(_.toString) === Seq("rc1", "beta", "99", "1234567", "dirty", "SNAPSHOT"))
        assert(SnapshotVersion.regex.pattern.matcher(version.toString).matches, version.toString)
      }

      it("dirty snapshot vs release") {
        val current = SemanticVersion.fromString("1.0.0-1-f650aaa-dirty-SNAPSHOT").get
        val enforceAfterVersion = SemanticVersion.fromString("1.0.0").get
        assert(current <= enforceAfterVersion)
      }
    }
  }

  describe("fromString") {

    describe("ReleaseVersion") {

      describe("dirty is false") {

        it("basic") {
          val version = fromString("v1.2.3").get.asInstanceOf[ReleaseVersion]
          assert(version.toString === "1.2.3")
          assert(version.major === 1, version)
          assert(version.minor === 2, version)
          assert(version.patch === 3, version)
          assert(!version.isDirty, version)
          assert(version.identifiers.values.isEmpty)
          assert(ReleaseVersion.regex.pattern.matcher(version.toString).matches, version.toString)
        }

        it("without leading v") {
          val version = fromString("1.2.3").get.asInstanceOf[ReleaseVersion]
          assert(version.toString === "1.2.3")
          assert(version.major === 1, version)
          assert(version.minor === 2, version)
          assert(version.patch === 3, version)
          assert(!version.isDirty, version)
          assert(version.identifiers.values.isEmpty)
          assert(ReleaseVersion.regex.pattern.matcher(version.toString).matches, version.toString)
        }

        it("with identifier") {
          val version = fromString("v1.2.3-rc1").get.asInstanceOf[ReleaseVersion]
          assert(version.toString === "1.2.3-rc1")
          assert(version.major === 1, version)
          assert(version.minor === 2, version)
          assert(version.patch === 3, version)
          assert(!version.isDirty, version)
          assert(version.identifiers.values.map(_.toString) === Seq("rc1"))
          assert(ReleaseVersion.regex.pattern.matcher(version.toString).matches, version.toString)
        }

        it("with multiple identifiers") {
          val version = fromString("v1.2.3-rc1-beta").get.asInstanceOf[ReleaseVersion]
          assert(version.toString === "1.2.3-rc1-beta")
          assert(version.major === 1, version)
          assert(version.minor === 2, version)
          assert(version.patch === 3, version)
          assert(!version.isDirty, version)
          assert(version.identifiers.values.map(_.toString) === Seq("rc1", "beta"))
          assert(ReleaseVersion.regex.pattern.matcher(version.toString).matches, version.toString)
        }
      }

      describe("dirty is true") {

        it("basic") {
          val version = fromString("v1.2.3-dirty-SNAPSHOT").get.asInstanceOf[ReleaseVersion]
          assert(version.toString === "1.2.3-dirty-SNAPSHOT")
          assert(version.major === 1, version)
          assert(version.minor === 2, version)
          assert(version.patch === 3, version)
          assert(version.isDirty, version)
          assert(version.identifiers.values.map(_.toString) === Seq("dirty", "SNAPSHOT"))
          assert(ReleaseVersion.regex.pattern.matcher(version.toString).matches, version.toString)
        }

        it("without leading v") {
          val version = fromString("1.2.3-dirty-SNAPSHOT").get.asInstanceOf[ReleaseVersion]
          assert(version.toString === "1.2.3-dirty-SNAPSHOT")
          assert(version.major === 1, version)
          assert(version.minor === 2, version)
          assert(version.patch === 3, version)
          assert(version.isDirty, version)
          assert(version.identifiers.values.map(_.toString) === Seq("dirty", "SNAPSHOT"))
          assert(ReleaseVersion.regex.pattern.matcher(version.toString).matches, version.toString)
        }

        it("with identifier") {
          val version = fromString("v1.2.3-rc1-dirty-SNAPSHOT").get.asInstanceOf[ReleaseVersion]
          assert(version.toString === "1.2.3-rc1-dirty-SNAPSHOT")
          assert(version.major === 1, version)
          assert(version.minor === 2, version)
          assert(version.patch === 3, version)
          assert(version.isDirty, version)
          assert(version.identifiers.values.map(_.toString) === Seq("rc1", "dirty", "SNAPSHOT"))
          assert(ReleaseVersion.regex.pattern.matcher(version.toString).matches, version.toString)
        }

        it("with multiple identifiers") {
          val version = fromString("v1.2.3-rc1-beta-dirty-SNAPSHOT").get.asInstanceOf[ReleaseVersion]
          assert(version.toString === "1.2.3-rc1-beta-dirty-SNAPSHOT")
          assert(version.major === 1, version)
          assert(version.minor === 2, version)
          assert(version.patch === 3, version)
          assert(version.isDirty, version)
          assert(version.identifiers.values.map(_.toString) === Seq("rc1", "beta", "dirty", "SNAPSHOT"))
          assert(ReleaseVersion.regex.pattern.matcher(version.toString).matches, version.toString)
        }
      }
    }

    describe("SnapshotVersion") {

      describe("dirty is false") {

        it("basic") {
          val version = fromString("v1.2.3-9001-1234567-SNAPSHOT").get.asInstanceOf[SnapshotVersion]
          assert(version.toString === "1.2.3-9001-1234567-SNAPSHOT")
          assert(version.commitHash === HashSemVerIdentifier("1234567"), version)
          assert(version.commitsSincePrevRelease === CommitsSemVerIdentifier(9001), version)
          assert(version.major === 1, version)
          assert(version.minor === 2, version)
          assert(version.patch === 3, version)
          assert(!version.isDirty, version)
          assert(version.identifiers.values.map(_.toString) === Seq("9001", "1234567", "SNAPSHOT"))
          assert(SnapshotVersion.regex.pattern.matcher(version.toString).matches, version.toString)
        }

        it("without leading v") {
          val version = fromString("1.2.3-9001-1234567-SNAPSHOT").get.asInstanceOf[SnapshotVersion]
          assert(version.toString === "1.2.3-9001-1234567-SNAPSHOT")
          assert(version.commitHash === HashSemVerIdentifier("1234567"), version)
          assert(version.commitsSincePrevRelease === CommitsSemVerIdentifier(9001), version)
          assert(version.major === 1, version)
          assert(version.minor === 2, version)
          assert(version.patch === 3, version)
          assert(!version.isDirty, version)
          assert(version.identifiers.values.map(_.toString) === Seq("9001", "1234567", "SNAPSHOT"))
          assert(SnapshotVersion.regex.pattern.matcher(version.toString).matches, version.toString)
        }

        it("without the 'g' prefix") {
          val version = fromString("v1.2.3-9001-1234567-SNAPSHOT").get.asInstanceOf[SnapshotVersion]
          assert(version.toString === "1.2.3-9001-1234567-SNAPSHOT")
          assert(version.commitHash === HashSemVerIdentifier("1234567"), version)
          assert(version.commitsSincePrevRelease === CommitsSemVerIdentifier(9001), version)
          assert(version.major === 1, version)
          assert(version.minor === 2, version)
          assert(version.patch === 3, version)
          assert(!version.isDirty, version)
          assert(version.identifiers.values.map(_.toString) === Seq("9001", "1234567", "SNAPSHOT"))
          assert(SnapshotVersion.regex.pattern.matcher(version.toString).matches, version.toString)
        }

        it("with identifier") {
          val version = fromString("v1.2.3-rc1-9001-1234567-SNAPSHOT").get.asInstanceOf[SnapshotVersion]
          assert(version.toString === "1.2.3-rc1-9001-1234567-SNAPSHOT")
          assert(version.commitHash === HashSemVerIdentifier("1234567"), version)
          assert(version.commitsSincePrevRelease === CommitsSemVerIdentifier(9001), version)
          assert(version.major === 1, version)
          assert(version.minor === 2, version)
          assert(version.patch === 3, version)
          assert(!version.isDirty, version)
          assert(version.identifiers.values.map(_.toString) === Seq("rc1", "9001", "1234567", "SNAPSHOT"))
          assert(SnapshotVersion.regex.pattern.matcher(version.toString).matches, version.toString)
        }

        it("with multiple identifiers") {
          val version = fromString("v1.2.3-rc1-m1-9001-1234567-SNAPSHOT").get.asInstanceOf[SnapshotVersion]
          assert(version.toString === "1.2.3-rc1-m1-9001-1234567-SNAPSHOT")
          assert(version.commitHash === HashSemVerIdentifier("1234567"), version)
          assert(version.commitsSincePrevRelease === CommitsSemVerIdentifier(9001), version)
          assert(version.major === 1, version)
          assert(version.minor === 2, version)
          assert(version.patch === 3, version)
          assert(!version.isDirty, version)
          assert(version.identifiers.values.map(_.toString) === Seq("rc1", "m1", "9001", "1234567", "SNAPSHOT"))
          assert(SnapshotVersion.regex.pattern.matcher(version.toString).matches, version.toString)
        }
      }

      describe("dirty is true") {

        it("basic") {
          val version = fromString("v1.2.3-9001-1234567-dirty-SNAPSHOT").get.asInstanceOf[SnapshotVersion]
          assert(version.commitHash === HashSemVerIdentifier("1234567"), version)
          assert(version.commitsSincePrevRelease === CommitsSemVerIdentifier(9001), version)
          assert(version.major === 1, version)
          assert(version.minor === 2, version)
          assert(version.patch === 3, version)
          assert(version.isDirty, version)
          assert(version.identifiers.values.map(_.toString) === Seq("9001", "1234567", "dirty", "SNAPSHOT"))
          assert(SnapshotVersion.regex.pattern.matcher(version.toString).matches, version.toString)
        }

        it("without leading v") {
          val version = fromString("1.2.3-9001-1234567-dirty-SNAPSHOT").get.asInstanceOf[SnapshotVersion]
          assert(version.toString === "1.2.3-9001-1234567-dirty-SNAPSHOT")
          assert(version.commitHash === HashSemVerIdentifier("1234567"), version)
          assert(version.commitsSincePrevRelease === CommitsSemVerIdentifier(9001), version)
          assert(version.major === 1, version)
          assert(version.minor === 2, version)
          assert(version.patch === 3, version)
          assert(version.isDirty, version)
          assert(version.identifiers.values.map(_.toString) === Seq("9001", "1234567", "dirty", "SNAPSHOT"))
          assert(SnapshotVersion.regex.pattern.matcher(version.toString).matches, version.toString)
        }

        it("without the 'g' prefix") {
          val version = fromString("v1.2.3-9001-1234567-dirty-SNAPSHOT").get.asInstanceOf[SnapshotVersion]
          assert(version.toString === "1.2.3-9001-1234567-dirty-SNAPSHOT")
          assert(version.commitHash === HashSemVerIdentifier("1234567"), version)
          assert(version.commitsSincePrevRelease === CommitsSemVerIdentifier(9001), version)
          assert(version.major === 1, version)
          assert(version.minor === 2, version)
          assert(version.patch === 3, version)
          assert(version.isDirty, version)
          assert(version.identifiers.values.map(_.toString) === Seq("9001", "1234567", "dirty", "SNAPSHOT"))
          assert(SnapshotVersion.regex.pattern.matcher(version.toString).matches, version.toString)
        }

        it("with identifier") {
          val version = fromString("v1.2.3-rc1-9001-1234567-dirty-SNAPSHOT").get.asInstanceOf[SnapshotVersion]
          assert(version.toString === "1.2.3-rc1-9001-1234567-dirty-SNAPSHOT")
          assert(version.commitHash === HashSemVerIdentifier("1234567"), version)
          assert(version.commitsSincePrevRelease === CommitsSemVerIdentifier(9001), version)
          assert(version.major === 1, version)
          assert(version.minor === 2, version)
          assert(version.patch === 3, version)
          assert(version.isDirty, version)
          assert(version.identifiers.values.map(_.toString) === Seq("rc1", "9001", "1234567", "dirty", "SNAPSHOT"))
          assert(SnapshotVersion.regex.pattern.matcher(version.toString).matches, version.toString)
        }

        it("with multiple identifiers") {
          val version = fromString("v1.2.3-r1-m1-99-1234567-dirty-SNAPSHOT").get.asInstanceOf[SnapshotVersion]
          assert(version.toString === "1.2.3-r1-m1-99-1234567-dirty-SNAPSHOT")
          assert(version.commitHash === HashSemVerIdentifier("1234567"), version)
          assert(version.commitsSincePrevRelease === CommitsSemVerIdentifier(99), version)
          assert(version.major === 1, version)
          assert(version.minor === 2, version)
          assert(version.patch === 3, version)
          assert(version.isDirty, version)
          assert(version.identifiers.values.map(_.toString) === Seq("r1", "m1", "99", "1234567", "dirty", "SNAPSHOT"))
          assert(SnapshotVersion.regex.pattern.matcher(version.toString).matches, version.toString)
        }
      }
    }

    describe("no version") {

      it("head tag") {
        val version = fromString("HEAD")
        assert(version.isEmpty)
      }

      it("head branch tag") {
        val version = fromString("HEAD -> asdfsdf")
        assert(version.isEmpty)
      }

      it("weird version") {
        val version = fromString("1.2.3.4")
        assert(version.isEmpty)
      }

      it("weird RCU tag") {
        val version = fromString("1.2.3/release99")
        assert(version.isEmpty)
      }
    }
  }

  describe("parseDynVerString") {
    it("parses release") {
      val version = parseDynVerString("1.2.3").get.asInstanceOf[ReleaseVersion]
      assert(version.toString === "1.2.3")
      assert(version.major === 1, version)
      assert(version.minor === 2, version)
      assert(version.patch === 3, version)
      assert(!version.isDirty, version)
      assert(ReleaseVersion.regex.pattern.matcher(version.toString).matches, version.toString)
    }
    it("parses snapshot") {
      val version = parseDynVerString("1.2.3-1-704e6a3b").get.asInstanceOf[SnapshotVersion]
      assert(version.toString === "1.2.3-1-704e6a3b-SNAPSHOT")
      assert(version.commitHash === HashSemVerIdentifier("704e6a3b"), version)
      assert(version.commitsSincePrevRelease === CommitsSemVerIdentifier(1), version)
      assert(version.major === 1, version)
      assert(version.minor === 2, version)
      assert(version.patch === 3, version)
      assert(!version.isDirty, version)
      assert(SnapshotVersion.regex.pattern.matcher(version.toString).matches, version.toString)
    }
    it("parses timestamped to dirty") {
      val version = parseDynVerString("1.2.3-1-704e6a3b-20200420-0914").get.asInstanceOf[SnapshotVersion]
      assert(version.toString === "1.2.3-1-704e6a3b-dirty-SNAPSHOT")
      assert(version.commitHash === HashSemVerIdentifier("704e6a3b"), version)
      assert(version.commitsSincePrevRelease === CommitsSemVerIdentifier(1), version)
      assert(version.major === 1, version)
      assert(version.minor === 2, version)
      assert(version.patch === 3, version)
      assert(version.isDirty, version)
      assert(SnapshotVersion.regex.pattern.matcher(version.toString).matches, version.toString)
    }
  }

  describe("fromCommit") {

    describe("ReleaseVersion") {

      describe("dirty") {

        it("false") {
          val version = ReleaseVersion.fromCommit(
            GitCommit(HashSemVerIdentifierSpec.expandHash("0123abc"), 7, Seq("v1.2.3")))
          assert(version.get.toString === "1.2.3")
        }

        it("true") {
          val version = ReleaseVersion.fromCommit(
            GitCommit(HashSemVerIdentifierSpec.expandHash("0123abc"), 7, Seq("v1.2.3-dirty-SNAPSHOT")))
          assert(version.isEmpty)
        }
      }

      it("multiple") {
        val version = ReleaseVersion.fromCommit(
          GitCommit(HashSemVerIdentifierSpec.expandHash("0123abc"), 7, Seq("v1.2.3", "v9.9.9", "v0.0.1")))
        assert(version.get.toString === "9.9.9")
      }

      it("multiple with identifiers") {
        val version = ReleaseVersion.fromCommit(
          GitCommit(HashSemVerIdentifierSpec.expandHash("0123abc"), 7, Seq("v1.0.0", "v0.1.0", "v1.0.0-rc1")))
        assert(version.get.toString === "1.0.0")
      }
    }

    describe("SnapshotVersion") {

      describe("dirty") {

        it("false") {
          val version = ReleaseVersion.fromCommit(
            GitCommit(HashSemVerIdentifierSpec.expandHash("0123abc"), 7, Seq("v1.2.3-9001-1234567-SNAPSHOT")))
          assert(version.isEmpty)
        }

        it("true") {
          val version = ReleaseVersion.fromCommit(
            GitCommit(HashSemVerIdentifierSpec.expandHash("0123abc"), 7, Seq("v1.2.3-9001-1234567-dirty-SNAPSHOT")))
          assert(version.isEmpty)
        }
      }

      it("multiple") {
        val version = ReleaseVersion.fromCommit(GitCommit(
          HashSemVerIdentifierSpec.expandHash("0123abc"),
          7,
          Seq("v1.2.3-9001-1234567-SNAPSHOT", "v9.9.9-9001-1234567-SNAPSHOT", "v0.0.1-9001-1234567-SNAPSHOT")))
        assert(version.isEmpty)
      }
    }

    describe("mixed ReleaseVersion and SnapshotVersion") {

      it("typical") {
        val version = ReleaseVersion.fromCommit(GitCommit(
          HashSemVerIdentifierSpec.expandHash("0123abc"),
          7,
          Seq(
            "v1.2.3",
            "v1.2.3-9001-1234567-SNAPSHOT",
            "v9.9.9-9001-1234567-SNAPSHOT",
            "v9.9.9",
            "v0.0.1",
            "v0.0.1-9001-1234567-SNAPSHOT")))
        assert(version.get.toString === "9.9.9")
      }

      it("snapshot versions are all larger") {
        val version = ReleaseVersion.fromCommit(GitCommit(
          HashSemVerIdentifierSpec.expandHash("0123abc"),
          7,
          Seq(
            "v1.2.3-9001-1234567-SNAPSHOT",
            "v9.9.9-9001-1234567-SNAPSHOT",
            "v0.0.1",
            "v0.0.1-9001-1234567-SNAPSHOT")))
        assert(version.get.toString === "0.0.1")
      }
    }

    describe("no version") {

      it("head tag") {
        val version = ReleaseVersion.fromCommit(
          GitCommit(HashSemVerIdentifierSpec.expandHash("0123abc"), 7, Seq("HEAD")))
        assert(version.isEmpty)
      }

      it("head branch tag") {
        val version = ReleaseVersion.fromCommit(
          GitCommit(HashSemVerIdentifierSpec.expandHash("0123abc"), 7, Seq("HEAD -> asdataf")))
        assert(version.isEmpty)
      }

      it("weird version") {
        val version = ReleaseVersion.fromCommit(
          GitCommit(HashSemVerIdentifierSpec.expandHash("0123abc"), 7, Seq("1.2.3.4")))
        assert(version.isEmpty)
      }

      it("weird RCU tag") {
        val version = ReleaseVersion.fromCommit(
          GitCommit(HashSemVerIdentifierSpec.expandHash("0123abc"), 7, Seq("1.2.3/release99")))
        assert(version.isEmpty)
      }
    }
  }

  describe("compare") {

    describe("release to release") {

      it("same") {
        val x = ReleaseVersion(1, 2, 3, SemVerIdentifierList.empty, isDirty = false)
        assert(x === x)
        assert(!(x > x))
        assert(!(x < x))
      }

      it("diff on major") {
        val x = ReleaseVersion(1, 0, 0, SemVerIdentifierList.empty, isDirty = false)
        val y = ReleaseVersion(10, 0, 0, SemVerIdentifierList.empty, isDirty = false)
        assert(x < y)
        assert(y > x)
        assert(x !== y)
        assert(y !== x)
      }

      it("diff on dirty") {
        val x = ReleaseVersion(1, 0, 0, SemVerIdentifierList.empty, isDirty = false)
        val y = ReleaseVersion(1, 0, 0, SemVerIdentifierList.empty, isDirty = true)
        assert(x < y)
        assert(y > x)
        assert(x !== y)
        assert(y !== x)
      }

      it("diff on even identifiers") {
        val x = ReleaseVersion(1, 0, 0, Seq("alpha"), isDirty = false)
        val y = ReleaseVersion(1, 0, 0, Seq("beta"), isDirty = false)
        assert(x < y)
        assert(y > x)
        assert(x !== y)
        assert(y !== x)
      }

      // http://semver.org/#spec-item-11
      it("from SemVer docs #11") {
        val fromDocs = Seq(
          "1.0.0-alpha",
          "1.0.0-alpha.1",
          "1.0.0-alpha.beta",
          "1.0.0-beta",
          "1.0.0-beta.2",
          "1.0.0-beta.11",
          "1.0.0-rc.1",
          "1.0.0")

        val origOrder = fromDocs.flatMap(str => ReleaseVersion.unapply(str))

        assert(fromDocs.size === origOrder.size)

        fromDocs.indices.foreach { _ =>
          val randomOrder = Random.shuffle(origOrder)
          assert(origOrder === randomOrder.sorted[SemanticVersion])
        }
      }
    }

    describe("snapshot to snapshot") {

      it("same") {
        val x = SnapshotVersion(1, 0, 0, SemVerIdentifierList.empty, isDirty = false, "1234567", 9001)
        assert(x == x)
        assert(!(x > x))
        assert(!(x < x))
      }

      it("diff on commit count") {
        val x = SnapshotVersion(1, 0, 0, SemVerIdentifierList.empty, isDirty = false, "1234567", 1)
        val y = SnapshotVersion(1, 0, 0, SemVerIdentifierList.empty, isDirty = false, "1234567", 2)
        assert(x < y)
        assert(y > x)
        assert(x !== y)
        assert(y !== x)
      }

      it("diff on hash") {
        val x = SnapshotVersion(1, 0, 0, SemVerIdentifierList.empty, isDirty = false, "0000000", 1)
        val y = SnapshotVersion(1, 0, 0, SemVerIdentifierList.empty, isDirty = false, "9999999", 1)
        assert(x < y)
        assert(y > x)
        assert(x !== y)
        assert(y !== x)
      }

      it("diff on even identifiers") {
        val x = SnapshotVersion(1, 0, 0, Seq("alpha"), isDirty = false, "1234567", 9001)
        val y = SnapshotVersion(1, 0, 0, Seq("beta"), isDirty = false, "1234567", 9001)
        assert(x < y)
        assert(y > x)
        assert(x !== y)
        assert(y !== x)
      }
    }

    describe("release to snapshot") {

      it("release from snapshot, same major.minor.patch") {
        val snapshot = SnapshotVersion(1, 0, 0, SemVerIdentifierList.empty, isDirty = false, "0000000", 1)
        val release = snapshot.toRelease
        assert(snapshot < release)
        assert(release > snapshot)
        assert(snapshot !== release)
        assert(release !== snapshot)
      }

      it("release from snapshot, different major.minor.patch, larger release") {
        val snapshot = SnapshotVersion(1, 0, 0, SemVerIdentifierList.empty, isDirty = false, "0000000", 1)
        val release = ReleaseVersion(2, 0, 0, SemVerIdentifierList.empty, isDirty = false)
        assert(snapshot < release)
        assert(release > snapshot)
        assert(snapshot !== release)
        assert(release !== snapshot)
      }

      it("release from snapshot, different major.minor.patch, smaller release") {
        val snapshot = SnapshotVersion(2, 0, 0, SemVerIdentifierList.empty, isDirty = false, "0000000", 1)
        val release = ReleaseVersion(1, 0, 0, SemVerIdentifierList.empty, isDirty = false)
        assert(release < snapshot)
        assert(snapshot > release)
        assert(snapshot !== release)
        assert(release !== snapshot)
      }

      it("snapshot from release, same major.minor.patch") {
        val release = ReleaseVersion(1, 0, 1, SemVerIdentifierList.empty, isDirty = false)
        val snapshot = SnapshotVersion(1, 0, 1, SemVerIdentifierList.empty, isDirty = false, "01234567", 1)
        assert(snapshot < release)
        assert(release > snapshot)
        assert(snapshot !== release)
        assert(release !== snapshot)
      }

      it("snapshot from release, different major.minor.patch, larger release") {
        val release = ReleaseVersion(1, 0, 2, SemVerIdentifierList.empty, isDirty = false)
        val snapshot = SnapshotVersion(1, 0, 1, SemVerIdentifierList.empty, isDirty = false, "01234567", 1)
        assert(snapshot < release)
        assert(release > snapshot)
        assert(snapshot !== release)
        assert(release !== snapshot)
      }

      it("snapshot from release, different major.minor.patch, smaller release") {
        val release = ReleaseVersion(1, 0, 0, SemVerIdentifierList.empty, isDirty = false)
        val snapshot = SnapshotVersion(1, 0, 1, SemVerIdentifierList.empty, isDirty = false, "01234567", 1)
        assert(release < snapshot)
        assert(snapshot > release)
        assert(snapshot !== release)
        assert(release !== snapshot)
      }
    }
  }
}
