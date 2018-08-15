package com.rallyhealth.sbt.semver.level.rule

import com.rallyhealth.sbt.semver.TestSnapshotVersion
import com.rallyhealth.sbt.versioning.SemVerReleaseType.Major
import com.rallyhealth.sbt.versioning.{ReleaseVersion, SemVerReleaseType, SemanticVersion}
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.{Matchers, WordSpec}

class EnforceAfterVersionRuleSpec
  extends WordSpec
  with Matchers
  with TypeCheckedTripleEquals {

  case class Scenario(enforceAfter: Option[ReleaseVersion], majorAllowed: Seq[SemanticVersion] = Nil, noOpinion: Seq[SemanticVersion] = Nil)

  val scenarios = Seq(
    Scenario(
      enforceAfter = None,
      noOpinion = Seq(
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
    ),
    Scenario(
      enforceAfter = Some(ReleaseVersion(1, 0, 0)),
      majorAllowed = Seq(
        ReleaseVersion(0, 0, 0),
        ReleaseVersion(0, 0, 1),
        ReleaseVersion(0, 1, 1),
        ReleaseVersion(1, 0, 0),
        TestSnapshotVersion(0, 0, 0),
        TestSnapshotVersion(0, 1, 1),
        TestSnapshotVersion(1, 0, 0)
      ),
      noOpinion = Seq(
        ReleaseVersion(1, 1, 1),
        TestSnapshotVersion(1, 1, 1)
      )
    )
  )

  "maybeEnforceAfterVersion" when {

    for (Scenario(maybeEnforceAfter, majorAllowed, noOpinion) <- scenarios) {
      maybeEnforceAfter.toString should {

        def calcRelaseType(ver: SemanticVersion): Option[SemVerReleaseType] = {
          val maybeLevel = EnforceAfterVersionRule(ver, maybeEnforceAfter).calcLevel()
          maybeLevel.map(_.releaseType)
        }

        for (ver <- majorAllowed) {
          s"allow major changes for $ver" in {
            assert(calcRelaseType(ver) === Some(Major))
          }
        }

        for (ver <- noOpinion) {
          s"not have an opinion about $ver" in {
            assert(calcRelaseType(ver) === None)
          }
        }
      }
    }
  }
}
