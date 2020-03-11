package com.rallyhealth.sbt.versioning

import com.rallyhealth.sbt.versioning.LowerBoundedSemanticVersion._
import org.scalatest.{FunSpec, Matchers}

import scala.language.implicitConversions

class LowerBoundedSemanticVersionSpec extends FunSpec with Matchers {

  private val hash1 = HashSemVerIdentifier("0123abc")
  // for these tests we need a full hash, not an abbreviation
  private val hashAndCount1 = HashAndCount(hash1, 1)

  describe("BoundedSemanticVersion") {

    describe("ReleaseVersion") {

      it("equal bound") {
        val version = ReleaseVersion(1, 2, 3, SemVerIdentifierList.empty, isDirty = false)
        val bound = LowerBound(1, 2, 3)
        val result = version.lowerBound(bound, hashAndCount1)
        assert(result === version)
      }

      it("lower bound") {
        val version = ReleaseVersion(1, 2, 3, SemVerIdentifierList.empty, isDirty = false)
        val bound = LowerBound(1, 0, 0)
        val result = version.lowerBound(bound, hashAndCount1)
        assert(result === version)
      }

      it("higher bound") {
        val version = ReleaseVersion(1, 2, 3, SemVerIdentifierList.empty, isDirty = false)
        val bound = LowerBound(1, 2, 4)
        an[IllegalArgumentException] shouldBe thrownBy {
          version.lowerBound(bound, hashAndCount1)
        }
      }
    }

    describe("SnapshotVersion") {

      it("equal bound") {
        val version = SnapshotVersion(1, 2, 3, SemVerIdentifierList.empty, isDirty = false, hashAndCount1, 1)
        val result = version.lowerBound(LowerBound(1, 2, 3), hashAndCount1)
        assert(result === version)
      }

      it("lower bound") {
        val version = SnapshotVersion(1, 2, 3, SemVerIdentifierList.empty, isDirty = false, hashAndCount1, 1)
        val bound = LowerBound(1, 0, 0)
        val result = version.lowerBound(bound, hashAndCount1)
        assert(result === version)
        assert(result.toString === s"1.2.3-1-$hash1-SNAPSHOT")
      }

      it("greater bound") {
        val version = SnapshotVersion(1, 2, 3, SemVerIdentifierList.empty, isDirty = false, hashAndCount1, 1)
        val bound = LowerBound(2, 0, 0)
        val result = version.lowerBound(bound, hashAndCount1)
        assert(result.toString === s"2.0.0-1-$hash1-SNAPSHOT")
      }

      it("version is dirty") {
        val version = SnapshotVersion(1, 2, 3, SemVerIdentifierList.empty, isDirty = true, hashAndCount1, 1)
        val bound = LowerBound(2, 0, 0)
        val result = version.lowerBound(bound, hashAndCount1)
        assert(result.toString === s"2.0.0-1-$hash1-dirty-SNAPSHOT")
      }

      it("bound is dirty") {
        val version = SnapshotVersion(1, 2, 3, SemVerIdentifierList.empty, isDirty = false, hashAndCount1, 1)
        val bound = LowerBound(2, 0, 0)
        val result = version.lowerBound(bound, hashAndCount1)
        assert(result.toString === s"2.0.0-1-$hash1-SNAPSHOT")
      }

      it("identifiers") {
        val version = SnapshotVersion(1, 0, 0, Seq("rc.0"), isDirty = false, hashAndCount1, 1)
        val bound = LowerBound(2, 0, 0)
        val result = version.lowerBound(bound, hashAndCount1)
        assert(result === SnapshotVersion(2, 0, 0, SemVerIdentifierList.empty, false, hashAndCount1, 1))
      }

    }
  }

  implicit def hashAndCountAsHash(hc: HashAndCount): HashSemVerIdentifier = hc.hash
}
