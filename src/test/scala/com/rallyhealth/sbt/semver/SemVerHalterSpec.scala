package com.rallyhealth.sbt.semver

import java.io.File

import com.rallyhealth.sbt.semver.SemVerHalterSpec._
import com.rallyhealth.sbt.semver.SemVerResult.{Direction, Problem, UpgradeLegalityType}
import com.rallyhealth.sbt.versioning._
import com.typesafe.tools.mima.core.{Problem => MiMaProblem, _}
import org.scalatest.FunSpec
import sbt.Level

import scala.reflect.internal.ClassfileConstants

/**
  * This class does not use a mocking framework to keep our dependencies light.
  */
class SemVerHalterSpec extends FunSpec {

  private val memberInfo = {
    val bytecodeName = "bytecodeName"
    // I tried using "NoClass" but it NPEs due to the null package reference, it doesn't use "NoPackageInfo" for
    // some reason. so this is the same as "NoClass" but substitutes "NoPackageInfo" instead of null
    val classInfo = new SyntheticClassInfo(NoPackageInfo, bytecodeName)
    new MemberInfo(classInfo, bytecodeName, ClassfileConstants.JAVA_ACC_PUBLIC, "sig")
  }

  describe("fatal upgrades") {

    describe("downgrade") {

      describe("regular release") {
        val prev = ReleaseVersion(1, 2, 3, SemVerIdentifierList.empty, isDirty = false)
        val curr = ReleaseVersion(1, 2, 2, SemVerIdentifierList.empty, isDirty = false)

        it("SemVerLimitTargetVersion") {
          val result = SemVerResult(prev, SemVerLimitTargetVersion(curr), Seq.empty)
          assert(result.upgradeLegality.legality === UpgradeLegalityType.Fatal, result.upgradeLegality)
          simpleCheck(result, shouldHaltBuild = true)
        }

        it("SemVerSpecificTargetVersion") {
          val result = SemVerResult(prev, SemVerSpecificTargetVersion(curr), Seq.empty)
          assert(result.upgradeLegality.legality === UpgradeLegalityType.Fatal, result.upgradeLegality)
          simpleCheck(result, shouldHaltBuild = true)
        }
      }

      describe("pre-release") {
        val prev = ReleaseVersion(0, 0, 3, SemVerIdentifierList.empty, isDirty = false)
        val curr = ReleaseVersion(0, 0, 2, SemVerIdentifierList.empty, isDirty = false)

        it("SemVerLimitTargetVersion") {
          val result = SemVerResult(prev, SemVerLimitTargetVersion(curr), Seq.empty)
          assert(result.upgradeLegality.legality === UpgradeLegalityType.Fatal, result.upgradeLegality)
          simpleCheck(result, shouldHaltBuild = true)
        }

        it("SemVerSpecificTargetVersion") {
          val result = SemVerResult(prev, SemVerSpecificTargetVersion(curr), Seq.empty)
          assert(result.upgradeLegality.legality === UpgradeLegalityType.Fatal, result.upgradeLegality)
          simpleCheck(result, shouldHaltBuild = true)
        }
      }
    }
  }

  describe("not normalized") {

    describe("regular release") {
      val prev = ReleaseVersion(1, 2, 3, SemVerIdentifierList.empty, isDirty = false)
      val curr = ReleaseVersion(1, 5, 0, SemVerIdentifierList.empty, isDirty = false)

      it("SemVerLimitTargetVersion") {
        val result = SemVerResult(prev, SemVerLimitTargetVersion(curr), Seq.empty)
        simpleCheck(result, shouldHaltBuild = false)
      }

      it("SemVerSpecificTargetVersion") {
        val result = SemVerResult(prev, SemVerSpecificTargetVersion(curr), Seq.empty)
        assert(result.upgradeLegality.legality === UpgradeLegalityType.Fatal, result.upgradeLegality)
        simpleCheck(result, shouldHaltBuild = true)
      }
    }

    describe("pre-release") {
      val prev = ReleaseVersion(0, 0, 2, SemVerIdentifierList.empty, isDirty = false)
      val curr = ReleaseVersion(0, 0, 5, SemVerIdentifierList.empty, isDirty = false)

      it("SemVerLimitTargetVersion") {
        val result = SemVerResult(prev, SemVerLimitTargetVersion(curr), Seq.empty)
        simpleCheck(result, shouldHaltBuild = false)
      }

      it("SemVerSpecificTargetVersion") {
        val result = SemVerResult(prev, SemVerSpecificTargetVersion(curr), Seq.empty)
        assert(result.upgradeLegality.legality === UpgradeLegalityType.Fatal, result.upgradeLegality)
        simpleCheck(result, shouldHaltBuild = true)
      }
    }
  }

  describe("backward problems") {
    val backwardsProblems = Seq(Problem(Direction.Backward, "deleted a field"))

    describe("clean release") {
      val prev = ReleaseVersion(1, 2, 3, SemVerIdentifierList.empty, isDirty = false)
      val curr = ReleaseVersion(1, 2, 4, SemVerIdentifierList.empty, isDirty = false)

      it("SemVerLimitTargetVersion") {
        val result = SemVerResult(prev, SemVerLimitTargetVersion(curr), backwardsProblems)
        assert(result.upgradeLegality.legality === UpgradeLegalityType.Error, result.upgradeLegality)
        simpleCheck(result, shouldHaltBuild = true)
      }

      it("SemVerSpecificTargetVersion") {
        val result = SemVerResult(prev, SemVerSpecificTargetVersion(curr), backwardsProblems)
        assert(result.upgradeLegality.legality === UpgradeLegalityType.Error, result.upgradeLegality)
        simpleCheck(result, shouldHaltBuild = true)
      }
    }

    describe("pre-release") {
      val prev = ReleaseVersion(0, 0, 2, SemVerIdentifierList.empty, isDirty = false)
      val curr = ReleaseVersion(0, 0, 3, SemVerIdentifierList.empty, isDirty = false)

      it("SemVerLimitTargetVersion") {
        val result = SemVerResult(prev, SemVerLimitTargetVersion(curr), backwardsProblems)
        assert(result.upgradeLegality.legality === UpgradeLegalityType.Error, result.upgradeLegality)
        simpleCheck(result, shouldHaltBuild = true)
      }

      it("SemVerSpecificTargetVersion") {
        val result = SemVerResult(prev, SemVerSpecificTargetVersion(curr), backwardsProblems)
        assert(result.upgradeLegality.legality === UpgradeLegalityType.Error, result.upgradeLegality)
        simpleCheck(result, shouldHaltBuild = true)
      }
    }

    describe("dirty release") {
      val prev = ReleaseVersion(1, 2, 3, SemVerIdentifierList.empty, isDirty = false)
      val curr = prev.copy(isDirty = true)

      it("SemVerLimitTargetVersion") {
        val result = SemVerResult(prev, SemVerLimitTargetVersion(curr), backwardsProblems)
        assert(result.upgradeLegality.legality === UpgradeLegalityType.Error, result.upgradeLegality)
        simpleCheck(result, shouldHaltBuild = false)
      }

      it("SemVerSpecificTargetVersion") {
        val result = SemVerResult(prev, SemVerSpecificTargetVersion(curr), backwardsProblems)
        assert(result.upgradeLegality.legality === UpgradeLegalityType.Error, result.upgradeLegality)
        simpleCheck(result, shouldHaltBuild = false)
      }
    }
  }

  describe("forward problems") {
    val forwardProblems = Seq(Problem(Direction.Forward, "added a new field"))

    describe("regular release") {
      val prev = ReleaseVersion(1, 2, 3, SemVerIdentifierList.empty, isDirty = false)
      val curr = ReleaseVersion(1, 2, 4, SemVerIdentifierList.empty, isDirty = false)

      it("SemVerLimitTargetVersion") {
        val result = SemVerResult(prev, SemVerLimitTargetVersion(curr), forwardProblems)
        assert(result.upgradeLegality.legality === UpgradeLegalityType.Error, result.upgradeLegality)
        simpleCheck(result, shouldHaltBuild = true)
      }

      it("SemVerSpecificTargetVersion") {
        val result = SemVerResult(prev, SemVerSpecificTargetVersion(curr), forwardProblems)
        assert(result.upgradeLegality.legality === UpgradeLegalityType.Error, result.upgradeLegality)
        simpleCheck(result, shouldHaltBuild = true)
      }
    }

    describe("pre-release") {
      val prev = ReleaseVersion(0, 0, 2, SemVerIdentifierList.empty, isDirty = false)
      val curr = ReleaseVersion(0, 0, 3, SemVerIdentifierList.empty, isDirty = false)

      it("SemVerLimitTargetVersion") {
        val result = SemVerResult(prev, SemVerLimitTargetVersion(curr), forwardProblems)
        assert(result.upgradeLegality.legality === UpgradeLegalityType.Error, result.upgradeLegality)
        simpleCheck(result, shouldHaltBuild = true)
      }

      it("SemVerSpecificTargetVersion") {
        val result = SemVerResult(prev, SemVerSpecificTargetVersion(curr), forwardProblems)
        assert(result.upgradeLegality.legality === UpgradeLegalityType.Error, result.upgradeLegality)
        simpleCheck(result, shouldHaltBuild = true)
      }
    }

    describe("dirty release") {
      val prev = ReleaseVersion(1, 2, 3, SemVerIdentifierList.empty, isDirty = false)
      val curr = prev.copy(isDirty = true)

      it("SemVerLimitTargetVersion") {
        val result = SemVerResult(prev, SemVerLimitTargetVersion(curr), forwardProblems)
        assert(result.upgradeLegality.legality === UpgradeLegalityType.Error, result.upgradeLegality)
        simpleCheck(result, shouldHaltBuild = false)
      }

      it("SemVerSpecificTargetVersion") {
        val result = SemVerResult(prev, SemVerSpecificTargetVersion(curr), forwardProblems)
        assert(result.upgradeLegality.legality === UpgradeLegalityType.Error, result.upgradeLegality)
        simpleCheck(result, shouldHaltBuild = false)
      }
    }
  }

  describe("no code problems") {

    describe("regular release") {
      val prev = ReleaseVersion(1, 2, 3, SemVerIdentifierList.empty, isDirty = false)
      val curr = ReleaseVersion(1, 2, 4, SemVerIdentifierList.empty, isDirty = false)

      it("SemVerLimitTargetVersion") {
        val result = SemVerResult(prev, SemVerLimitTargetVersion(curr), Seq.empty)
        assert(result.upgradeLegality.legality === UpgradeLegalityType.Legal, result.upgradeLegality)
        simpleCheck(result, shouldHaltBuild = false)
      }

      it("SemVerSpecificTargetVersion") {
        val result = SemVerResult(prev, SemVerSpecificTargetVersion(curr), Seq.empty)
        assert(result.upgradeLegality.legality === UpgradeLegalityType.Legal, result.upgradeLegality)
        simpleCheck(result, shouldHaltBuild = false)
      }
    }

    describe("pre-release") {
      val prev = ReleaseVersion(0, 0, 2, SemVerIdentifierList.empty, isDirty = false)
      val curr = ReleaseVersion(0, 0, 3, SemVerIdentifierList.empty, isDirty = false)

      it("SemVerLimitTargetVersion") {
        val result = SemVerResult(prev, SemVerLimitTargetVersion(curr), Seq.empty)
        assert(result.upgradeLegality.legality === UpgradeLegalityType.Legal, result.upgradeLegality)
        simpleCheck(result, shouldHaltBuild = false)
      }

      it("SemVerSpecificTargetVersion") {
        val result = SemVerResult(prev, SemVerSpecificTargetVersion(curr), Seq.empty)
        assert(result.upgradeLegality.legality === UpgradeLegalityType.Legal, result.upgradeLegality)
        simpleCheck(result, shouldHaltBuild = false)
      }
    }

    describe("dirty release") {
      val prev = ReleaseVersion(1, 2, 3, SemVerIdentifierList.empty, isDirty = false)
      val curr = prev.copy(isDirty = true)

      it("SemVerLimitTargetVersion") {
        val result = SemVerResult(prev, SemVerLimitTargetVersion(curr), Seq.empty)
        assert(result.upgradeLegality.legality === UpgradeLegalityType.Error, result.upgradeLegality)
        simpleCheck(result, shouldHaltBuild = false)
      }

      it("SemVerSpecificTargetVersion") {
        val result = SemVerResult(prev, SemVerSpecificTargetVersion(curr), Seq.empty)
        assert(result.upgradeLegality.legality === UpgradeLegalityType.Error, result.upgradeLegality)
        simpleCheck(result, shouldHaltBuild = false)
      }
    }
  }

  describe("code problems") {

    val problems = List(MissingFieldProblem(memberInfo))

    it("no problems - sanity") {
      runTestCase(VersionChangeTest(1, 0, 1, shouldHaltBuild = false), List.empty, List.empty)
      runTestCase(VersionChangeTest(1, 1, 0, shouldHaltBuild = false), List.empty, List.empty)
      runTestCase(VersionChangeTest(2, 0, 0, shouldHaltBuild = false), List.empty, List.empty)
    }

    it("forward problems only - requires a minor upgrade") {
      runTestCase(VersionChangeTest(1, 0, 1, shouldHaltBuild = true), List.empty, problems)
      runTestCase(VersionChangeTest(1, 1, 0, shouldHaltBuild = false), List.empty, problems)
      runTestCase(VersionChangeTest(2, 0, 0, shouldHaltBuild = false), List.empty, problems)
    }

    it("backward problems only - requires a major upgrade") {
      runTestCase(VersionChangeTest(1, 0, 1, shouldHaltBuild = true), problems, List.empty)
      runTestCase(VersionChangeTest(1, 1, 0, shouldHaltBuild = true), problems, List.empty)
      runTestCase(VersionChangeTest(2, 0, 0, shouldHaltBuild = false), problems, List.empty)
    }

    it("backward and forward problems - requires a major upgrade") {
      runTestCase(VersionChangeTest(1, 0, 1, shouldHaltBuild = true), problems, problems)
      runTestCase(VersionChangeTest(1, 1, 0, shouldHaltBuild = true), problems, problems)
      runTestCase(VersionChangeTest(2, 0, 0, shouldHaltBuild = false), problems, problems)
    }
  }
}

object SemVerHalterSpec {

  private case class VersionChangeTest(major: Int, minor: Int, patch: Int, shouldHaltBuild: Boolean)

  private def runTestCase(
    test: VersionChangeTest, backwardProblems: List[MiMaProblem], forwardProblems: List[MiMaProblem]): Unit = {

    val prevVersion = ReleaseVersion(1, 0, 0, SemVerIdentifierList.empty, isDirty = false)
    val prevMima = PrevMimaInput(new File("foo"), prevVersion)
    val mima = new MockMiMaExecutor(backwardProblems, forwardProblems)

    val dirtyFlags = Seq(true, false)
    dirtyFlags.foreach { isDirty =>

      val currMimaList: Seq[CurrMimaInput] = {
        val currVersion = ReleaseVersion(
          test.major, test.minor, test.patch, SemVerIdentifierList.empty, isDirty = isDirty)
        val targetList = Seq(SemVerLimitTargetVersion(currVersion), SemVerSpecificTargetVersion(currVersion))
        targetList.map(curr => CurrMimaInput(new File("bar"), curr))
      }

      currMimaList.foreach { currMima =>

        val result = SemVerPluginUtils.compare(prevMima, currMima, mima)

        assert(result.upgradeLegality.legality != UpgradeLegalityType.Fatal, "none of these tests should have fatal")

        val expectedLegality = if (test.shouldHaltBuild) UpgradeLegalityType.Error else UpgradeLegalityType.Legal
        assert(
          result.upgradeLegality.legality == expectedLegality,
          s"result.isLegalUpgrade.legality=${result.upgradeLegality.legality}" +
            s" vs test.shouldHaltBuild=${test.shouldHaltBuild}," +
            s" reason=${result.upgradeLegality.reason}")

        // a dirty result should never throw, its work-in-progress, only clean ReleaseVersions fail
        // How do you get a PR to fail? Your 'semVerLimitVersion' takes the place of the 'version' for checking
        if (isDirty) {
          simpleCheck(result, shouldHaltBuild = false)
        } else {
          simpleCheck(result, test.shouldHaltBuild)
        }
      }
    }
  }

  /** Runs a basic check that [[SemVerHalter.shouldHaltBuild]] and the logs are at the right level. */
  private def simpleCheck(result: SemVerResult, shouldHaltBuild: Boolean): Unit = {

    val preReleaseEnabled = result.target.version.isPreRelease
    val logger = new MockLogger

    val preHalter = new PreCheckSemVerHalter(result.prev, result.target, preReleaseEnabled)
    preHalter.log(logger)

    val postHalter = new PostCheckSemVerHalter(result, preReleaseEnabled)
    postHalter.log(logger)

    lazy val halterLog = logger.messages.map(x => s"[${x.level}] ${x.message}").mkString("\n")

    assert(preHalter.shouldHaltBuild == shouldHaltBuild || postHalter.shouldHaltBuild == shouldHaltBuild, halterLog)

    val level = if (shouldHaltBuild) Level.Error else Level.Info
    assert(logger.messages.forall(_.level.id <= level.id), halterLog)

    val (matchFlag, misMatchFlag) = result.upgradeLegality.legality match {
      case UpgradeLegalityType.Legal => ("PASSED", "FAILED")
      case _ => ("FAILED", "PASSED")
    }
    assert(logger.messages.exists(_.message.contains(matchFlag)), halterLog)
    assert(logger.messages.forall(!_.message.contains(misMatchFlag)), halterLog)
  }

}
