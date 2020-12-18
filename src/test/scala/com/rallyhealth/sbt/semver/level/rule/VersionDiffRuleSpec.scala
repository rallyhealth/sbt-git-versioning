package com.rallyhealth.sbt.semver.level.rule

import com.rallyhealth.sbt.semver.TestSnapshotVersion
import com.rallyhealth.sbt.versioning.SemVerReleaseType.{Major, Minor, Patch}
import com.rallyhealth.sbt.versioning.{ReleaseVersion, SemVerReleaseType, SemanticVersion}
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class VersionDiffRuleSpec
  extends AnyWordSpec
  with Matchers
  with TypeCheckedTripleEquals {

  val releaseVersions = Seq(
    ReleaseVersion(0, 0, 0),
    ReleaseVersion(0, 0, 1),
    ReleaseVersion(0, 1, 1),
    ReleaseVersion(1, 0, 0),
    ReleaseVersion(1, 1, 1)
  )

  case class Scenario(
    prevRelease: Option[ReleaseVersion],
    shouldAllowForVersions: Map[SemVerReleaseType, Seq[SemanticVersion]]
  )

  val scenarios = Seq(
    Scenario(
      prevRelease = None,
      shouldAllowForVersions = Map(
        Major -> Seq(
          ReleaseVersion(0, 0, 0),
          ReleaseVersion(0, 0, 1),
          ReleaseVersion(0, 1, 1),
          ReleaseVersion(1, 0, 0),
          ReleaseVersion(1, 1, 1),
          TestSnapshotVersion(0, 0, 0),
          TestSnapshotVersion(0, 1, 1),
          TestSnapshotVersion(1, 0, 0),
          TestSnapshotVersion(1, 1, 1)
        )
      )
    ),
    Scenario(
      prevRelease = Some(ReleaseVersion(0, 0, 0)),
      shouldAllowForVersions = Map(
        Patch -> Seq(
          ReleaseVersion(0, 0, 1),
          ReleaseVersion(0, 0, 2)
        ),
        Minor -> Seq(
          ReleaseVersion(0, 1, 0),
          ReleaseVersion(0, 1, 1),
          TestSnapshotVersion(0, 0, 1), // because snapshots
          TestSnapshotVersion(0, 0, 2), // because snapshots
          TestSnapshotVersion(0, 1, 0),
          TestSnapshotVersion(0, 1, 1)
        ),
        Major -> Seq(
          ReleaseVersion(1, 0, 0),
          ReleaseVersion(1, 1, 1),
          TestSnapshotVersion(1, 0, 0),
          TestSnapshotVersion(1, 1, 1)
        )
      )
    )
  )

  for (scn <- scenarios) {
    s"prevRelease: ${scn.prevRelease}" should {
      for ((expected, versions) <- scn.shouldAllowForVersions) {
        s"allow $expected" when {
          for (ver <- versions) {
            s"current version: $ver" in {
              val maybeReleaseType = VersionDiffRule(ver, scn.prevRelease).calcLevel().map(_.releaseType)
              assert(maybeReleaseType === Some(expected))
            }
          }
        }
      }
    }
  }
}
