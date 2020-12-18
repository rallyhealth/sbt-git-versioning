package com.rallyhealth.sbt.semver.level.rule

import com.rallyhealth.sbt.semver.TestSnapshotVersion
import com.rallyhealth.sbt.versioning.ReleaseVersion
import com.rallyhealth.sbt.versioning.SemVerReleaseType.Major
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.LoneElement
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class InitialDevelopmentRuleSpec
  extends AnyWordSpec
  with Matchers
  with LoneElement
  with TypeCheckedTripleEquals {

  val scenarios = Seq(
    ReleaseVersion(0, 0, 0) -> Some(Major),
    ReleaseVersion(0, 0, 1) -> Some(Major),
    ReleaseVersion(0, 1, 1) -> Some(Major),
    ReleaseVersion(1, 0, 0) -> None,
    ReleaseVersion(1, 1, 1) -> None,
    TestSnapshotVersion(0, 0, 0) -> Some(Major),
    TestSnapshotVersion(0, 1, 1) -> Some(Major),
    TestSnapshotVersion(1, 0, 0) -> None,
    TestSnapshotVersion(1, 1, 1) -> None
  )

  for ((version, expected) <- scenarios) {
    s"conclude ${version} is $expected" in {
      val maybeLevel = InitialDevelopmentRule(version).calcLevel()
      val maybeReleaseType = maybeLevel.map(_.releaseType)

      assert(maybeReleaseType === expected)
    }
  }
}
