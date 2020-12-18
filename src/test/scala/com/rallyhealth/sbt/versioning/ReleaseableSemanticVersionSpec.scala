package com.rallyhealth.sbt.versioning

import com.rallyhealth.sbt.versioning.SemVerReleaseType.{Major, Minor, Patch, ReleaseableSemanticVersion}
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class ReleaseableSemanticVersionSpec
  extends AnyFunSpec
  with Matchers
  with TypeCheckedTripleEquals {

  private val testCases = Seq(
    TestCase(
      name = "Clean snapshots should be bumped, for everything but patch",
      version = SnapshotVersion(1, 2, 3, SemVerIdentifierList.empty, isDirty = false, "0123abc", 1),
      expectedReleases = Seq(
        Patch -> ReleaseVersion(1, 2, 3, SemVerIdentifierList.empty, isDirty = false),
        Minor -> ReleaseVersion(1, 3, 0, SemVerIdentifierList.empty, isDirty = false),
        Major -> ReleaseVersion(2, 0, 0, SemVerIdentifierList.empty, isDirty = false)
      )
    ),
    TestCase(
      name = "Dirty snapshots should stay dirty",
      version = SnapshotVersion(1, 2, 3, SemVerIdentifierList.empty, isDirty = true, "0123abc", 1),
      expectedReleases = Seq(
        Patch -> ReleaseVersion(1, 2, 3, SemVerIdentifierList.empty, isDirty = true),
        Minor -> ReleaseVersion(1, 3, 0, SemVerIdentifierList.empty, isDirty = true),
        Major -> ReleaseVersion(2, 0, 0, SemVerIdentifierList.empty, isDirty = true)
      )
    ),
    TestCase(
      name = "Snapshot identifiers should be stripped",
      version = SnapshotVersion(1, 2, 3, Seq("identifier"), isDirty = true, "0123abc", 1),
      expectedReleases = Seq(
        Patch -> ReleaseVersion(1, 2, 3, SemVerIdentifierList.empty, isDirty = true),
        Minor -> ReleaseVersion(1, 3, 0, SemVerIdentifierList.empty, isDirty = true),
        Major -> ReleaseVersion(2, 0, 0, SemVerIdentifierList.empty, isDirty = true)
      )
    ),
    TestCase(
      name = "Release versions should be bumped",
      version = ReleaseVersion(1, 2, 3, SemVerIdentifierList.empty, isDirty = false),
      expectedReleases = Seq(
        Patch -> ReleaseVersion(1, 2, 4, SemVerIdentifierList.empty, isDirty = false),
        Minor -> ReleaseVersion(1, 3, 0, SemVerIdentifierList.empty, isDirty = false),
        Major -> ReleaseVersion(2, 0, 0, SemVerIdentifierList.empty, isDirty = false)
      )
    ),
    TestCase(
      name = "Release identifiers should be stripped",
      version = ReleaseVersion(1, 2, 3, Seq("identifier"), isDirty = false),
      expectedReleases = Seq(
        Patch -> ReleaseVersion(1, 2, 4, SemVerIdentifierList.empty, isDirty = false),
        Minor -> ReleaseVersion(1, 3, 0, SemVerIdentifierList.empty, isDirty = false),
        Major -> ReleaseVersion(2, 0, 0, SemVerIdentifierList.empty, isDirty = false)
      )
    )
  )

  for (tc <- testCases) {
    describe(s"${tc.name} for ${tc.version.toString}") {
      for ((releaseType, expected) <- tc.expectedReleases) {
        it(s"$releaseType -> $expected") {
          val actual = tc.version.release(releaseType)
          assert(actual === expected)
        }
      }
    }
  }
}

case class TestCase(
  name: String,
  version: SemanticVersion,
  expectedReleases: Seq[(SemVerReleaseType, SemanticVersion)]
)
