package com.rallyhealth.sbt.semver

import com.rallyhealth.sbt.semver.SemVerDiff.Delta
import com.rallyhealth.sbt.versioning.{ReleaseVersion, SemVerIdentifierList}
import org.scalatest.FunSpec

class SemVerDiffSpec extends FunSpec {

  describe("unchanged") {

    it("same versions") {
      val version = ReleaseVersion(1, 2, 3)
      assert(SemVerDiff.calc(version, version) === Delta(0, 0, 0))
    }

    it("dirty prev") {
      val prev = ReleaseVersion(1, 2, 3, SemVerIdentifierList.empty, isDirty = true)
      val curr = prev.copy(isDirty = true)
      intercept[IllegalArgumentException] {
        SemVerDiff.calc(prev, curr)
      }
    }

    it("dirty curr") {
      val prev = ReleaseVersion(1, 2, 3)
      val curr = prev.copy(isDirty = true)
      assert(SemVerDiff.calc(prev, curr) === Delta(0, 0, 0))
    }
  }

  describe("downgrade") {

    it("patch") {
      val prev = ReleaseVersion(3, 3, 3)
      val curr = ReleaseVersion(3, 3, 2)
      assert(SemVerDiff.calc(prev, curr) === Delta(0, 0, -1))
    }

    it("minor") {
      val prev = ReleaseVersion(3, 3, 3)
      val curr = ReleaseVersion(3, 2, 3)
      assert(SemVerDiff.calc(prev, curr) === Delta(0, -1, 0))
    }

    it("minor and patch") {
      val prev = ReleaseVersion(3, 3, 3)
      val curr = ReleaseVersion(3, 2, 2)
      assert(SemVerDiff.calc(prev, curr) === Delta(0, -1, -1))
    }

    it("full minor") {
      val prev = ReleaseVersion(3, 3, 3)
      val curr = ReleaseVersion(3, 2, 0)
      assert(SemVerDiff.calc(prev, curr) === Delta(0, -1, -3))
    }

    it("large full minor") {
      val prev = ReleaseVersion(3, 3, 3)
      val curr = ReleaseVersion(3, 1, 0)
      assert(SemVerDiff.calc(prev, curr) === Delta(0, -2, -3))
    }

    it("major") {
      val prev = ReleaseVersion(3, 3, 3)
      val curr = ReleaseVersion(2, 3, 3)
      assert(SemVerDiff.calc(prev, curr) === Delta(-1, 0, 0))
    }

    it("major, minor, and patch") {
      val prev = ReleaseVersion(3, 3, 3)
      val curr = ReleaseVersion(2, 2, 2)
      assert(SemVerDiff.calc(prev, curr) === Delta(-1, -1, -1))
    }

    it("full major") {
      val prev = ReleaseVersion(3, 3, 3)
      val curr = ReleaseVersion(2, 0, 0)
      assert(SemVerDiff.calc(prev, curr) === Delta(-1, -3, -3))
    }

    it("large full major") {
      val prev = ReleaseVersion(3, 3, 3)
      val curr = ReleaseVersion(1, 0, 0)
      assert(SemVerDiff.calc(prev, curr) === Delta(-2, -3, -3))
    }
  }

  describe("not normalized") {

    it("patch") {
      val prev = ReleaseVersion(3, 3, 3)
      val curr = ReleaseVersion(3, 3, 7)
      assert(SemVerDiff.calc(prev, curr) === Delta(0, 0, 4))

    }

    it("minor") {
      val prev = ReleaseVersion(3, 3, 3)
      val curr = ReleaseVersion(3, 5, 0)
      assert(SemVerDiff.calc(prev, curr) === Delta(0, 2, -3))
    }

    it("minor and patch") {
      val prev = ReleaseVersion(3, 3, 3)
      val curr = ReleaseVersion(3, 5, 1)
      assert(SemVerDiff.calc(prev, curr) === Delta(0, 2, -2))
    }

    it("major") {
      val prev = ReleaseVersion(3, 3, 3)
      val curr = ReleaseVersion(5, 0, 0)
      assert(SemVerDiff.calc(prev, curr) === Delta(2, -3, -3))
    }

    it("major and minor") {
      val prev = ReleaseVersion(3, 3, 3)
      val curr = ReleaseVersion(5, 1, 0)
      assert(SemVerDiff.calc(prev, curr) === Delta(2, -2, -3))
    }

    it("major, minor, patch") {
      val prev = ReleaseVersion(3, 3, 3)
      val curr = ReleaseVersion(5, 1, 2)
      assert(SemVerDiff.calc(prev, curr) === Delta(2, -2, -1))
    }
  }

  describe("release") {

    it("patch") {
      val prev = ReleaseVersion(3, 3, 3)
      val curr = ReleaseVersion(3, 3, 4)
      assert(SemVerDiff.calc(prev, curr) === Delta(0, 0, 1))
    }

    it("minor") {
      val prev = ReleaseVersion(3, 3, 3)
      val curr = ReleaseVersion(3, 4, 0)
      assert(SemVerDiff.calc(prev, curr) === Delta(0, 1, -3))
    }

    it("full major") {
      val prev = ReleaseVersion(3, 3, 3)
      val curr = ReleaseVersion(4, 0, 0)
      assert(SemVerDiff.calc(prev, curr) === Delta(1, -3, -3))
    }
  }
}
