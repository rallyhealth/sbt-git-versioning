package com.rallyhealth.sbt.semver

import com.rallyhealth.sbt.semver.SemVerResult.UpgradeLegalityType
import com.rallyhealth.sbt.versioning.{ReleaseVersion, SemVerIdentifierList}
import org.scalatest.FunSpec

class SemVerResultSpec extends FunSpec {

  private val backwardProblems = Seq("deleted a field")
  private val forwardProblems = Seq("added a new field")

  describe("patch required and XXX desired") {
    val prev = ReleaseVersion(1, 2, 3, SemVerIdentifierList.empty, isDirty = false)

    describe("patch") {
      val curr = ReleaseVersion(1, 2, 4, SemVerIdentifierList.empty, isDirty = false)

      it("SemVerLimitTargetVersion") {
        val result = buildSemVerResult(prev, SemVerLimitTargetVersion(curr), Seq.empty, Seq.empty)
        assert(result.upgradeLegality.legality === UpgradeLegalityType.Legal)
        assert(result.upgradeLegality.reason.contains("=patch"))
      }

      describe("SemVerSpecificTargetVersion") {
        val result = buildSemVerResult(prev, SemVerSpecificTargetVersion(curr), Seq.empty, Seq.empty)
        assert(result.upgradeLegality.legality === UpgradeLegalityType.Legal)
        assert(result.upgradeLegality.reason.contains("=patch"))
      }
    }

    describe("minor") {
      val curr = ReleaseVersion(1, 3, 0, SemVerIdentifierList.empty, isDirty = false)

      it("SemVerLimitTargetVersion") {
        val result = buildSemVerResult(prev, SemVerLimitTargetVersion(curr), Seq.empty, Seq.empty)
        assert(result.upgradeLegality.legality === UpgradeLegalityType.Legal)
        assert(result.upgradeLegality.reason.contains("=patch"))
      }

      describe("SemVerSpecificTargetVersion") {
        val result = buildSemVerResult(prev, SemVerSpecificTargetVersion(curr), Seq.empty, Seq.empty)
        assert(result.upgradeLegality.legality === UpgradeLegalityType.Legal)
        assert(result.upgradeLegality.reason.contains("=patch"))
      }
    }

    describe("major") {
      val curr = ReleaseVersion(2, 0, 0, SemVerIdentifierList.empty, isDirty = false)

      it("SemVerLimitTargetVersion") {
        val result = buildSemVerResult(prev, SemVerLimitTargetVersion(curr), Seq.empty, Seq.empty)
        assert(result.upgradeLegality.legality === UpgradeLegalityType.Legal)
        assert(result.upgradeLegality.reason.contains("=patch"))
      }

      describe("SemVerSpecificTargetVersion") {
        val result = buildSemVerResult(prev, SemVerSpecificTargetVersion(curr), Seq.empty, Seq.empty)
        assert(result.upgradeLegality.legality === UpgradeLegalityType.Legal)
        assert(result.upgradeLegality.reason.contains("=patch"))
      }
    }
  }

  describe("minor required and XXX desired") {
    val prev = ReleaseVersion(1, 2, 3, SemVerIdentifierList.empty, isDirty = false)

    describe("patch") {
      val curr = ReleaseVersion(1, 2, 4, SemVerIdentifierList.empty, isDirty = false)

      it("SemVerLimitTargetVersion") {
        val result = buildSemVerResult(prev, SemVerLimitTargetVersion(curr), Seq.empty, forwardProblems)
        assert(result.upgradeLegality.legality === UpgradeLegalityType.Error)
        assert(result.upgradeLegality.reason.contains("=minor"))
      }

      it("SemVerSpecificTargetVersion") {
        val result = buildSemVerResult(prev, SemVerSpecificTargetVersion(curr), Seq.empty, forwardProblems)
        assert(result.upgradeLegality.legality === UpgradeLegalityType.Error)
        assert(result.upgradeLegality.reason.contains("=minor"))
      }
    }

    describe("minor") {
      val curr = ReleaseVersion(1, 3, 0, SemVerIdentifierList.empty, isDirty = false)

      it("SemVerLimitTargetVersion") {
        val result = buildSemVerResult(prev, SemVerLimitTargetVersion(curr), Seq.empty, forwardProblems)
        assert(result.upgradeLegality.legality === UpgradeLegalityType.Legal)
        assert(result.upgradeLegality.reason.contains("=minor"))
      }

      it("SemVerSpecificTargetVersion") {
        val result = buildSemVerResult(prev, SemVerSpecificTargetVersion(curr), Seq.empty, forwardProblems)
        assert(result.upgradeLegality.legality === UpgradeLegalityType.Legal)
        assert(result.upgradeLegality.reason.contains("=minor"))
      }
    }

    describe("major") {
      val curr = ReleaseVersion(2, 0, 0, SemVerIdentifierList.empty, isDirty = false)

      it("SemVerLimitTargetVersion") {
        val result = buildSemVerResult(prev, SemVerLimitTargetVersion(curr), Seq.empty, forwardProblems)
        assert(result.upgradeLegality.legality === UpgradeLegalityType.Legal)
        assert(result.upgradeLegality.reason.contains("=minor"))
      }

      it("SemVerSpecificTargetVersion") {
        val result = buildSemVerResult(prev, SemVerSpecificTargetVersion(curr), Seq.empty, forwardProblems)
        assert(result.upgradeLegality.legality === UpgradeLegalityType.Legal)
        assert(result.upgradeLegality.reason.contains("=minor"))
      }
    }
  }

  describe("major required and XXX desired") {
    val prev = ReleaseVersion(1, 2, 3, SemVerIdentifierList.empty, isDirty = false)

    describe("patch") {
      val curr = ReleaseVersion(1, 2, 4, SemVerIdentifierList.empty, isDirty = false)

      it("SemVerLimitTargetVersion") {
        val result = buildSemVerResult(prev, SemVerLimitTargetVersion(curr), backwardProblems, forwardProblems)
        assert(result.upgradeLegality.legality === UpgradeLegalityType.Error)
        assert(result.upgradeLegality.reason.contains("=major"))
      }

      it("SemVerSpecificTargetVersion") {
        val result = buildSemVerResult(prev, SemVerSpecificTargetVersion(curr), backwardProblems, forwardProblems)
        assert(result.upgradeLegality.legality === UpgradeLegalityType.Error)
        assert(result.upgradeLegality.reason.contains("=major"))
      }
    }

    describe("minor") {
      val curr = ReleaseVersion(1, 3, 0, SemVerIdentifierList.empty, isDirty = false)

      it("SemVerLimitTargetVersion") {
        val result = buildSemVerResult(prev, SemVerLimitTargetVersion(curr), backwardProblems, forwardProblems)
        assert(result.upgradeLegality.legality === UpgradeLegalityType.Error)
        assert(result.upgradeLegality.reason.contains("=major"))
      }

      it("SemVerSpecificTargetVersion") {
        val result = buildSemVerResult(prev, SemVerSpecificTargetVersion(curr), backwardProblems, forwardProblems)
        assert(result.upgradeLegality.legality === UpgradeLegalityType.Error)
        assert(result.upgradeLegality.reason.contains("=major"))
      }
    }

    describe("major") {
      val curr = ReleaseVersion(2, 0, 0, SemVerIdentifierList.empty, isDirty = false)

      it("SemVerLimitTargetVersion") {
        val result = buildSemVerResult(prev, SemVerLimitTargetVersion(curr), backwardProblems, forwardProblems)
        assert(result.upgradeLegality.legality === UpgradeLegalityType.Legal)
        assert(result.upgradeLegality.reason.contains("=major"))
      }

      it("SemVerSpecificTargetVersion") {
        val result = buildSemVerResult(prev, SemVerSpecificTargetVersion(curr), backwardProblems, forwardProblems)
        assert(result.upgradeLegality.legality === UpgradeLegalityType.Legal)
        assert(result.upgradeLegality.reason.contains("=major"))
      }
    }
  }

  describe("downgrade") {
    val prev = ReleaseVersion(3, 3, 3, SemVerIdentifierList.empty, isDirty = false)

    describe("patch") {
      val curr = ReleaseVersion(3, 3, 2, SemVerIdentifierList.empty, isDirty = false)

      it("SemVerLimitTargetVersion") {
        val result = SemVerResult(prev, SemVerLimitTargetVersion(curr), Seq.empty)
        assert(result.upgradeLegality.legality === UpgradeLegalityType.Fatal)
        assert(result.upgradeLegality.reason.contains("downgrade"))
      }
      it("SemVerSpecificTargetVersion") {
        val result = SemVerResult(prev, SemVerSpecificTargetVersion(curr), Seq.empty)
        assert(result.upgradeLegality.legality === UpgradeLegalityType.Fatal)
        assert(result.upgradeLegality.reason.contains("downgrade"))
      }
    }

    describe("minor") {
      val curr = ReleaseVersion(3, 2, 3, SemVerIdentifierList.empty, isDirty = false)

      it("SemVerLimitTargetVersion") {
        val result = SemVerResult(prev, SemVerLimitTargetVersion(curr), Seq.empty)
        assert(result.upgradeLegality.legality === UpgradeLegalityType.Fatal)
        assert(result.upgradeLegality.reason.contains("downgrade"))
      }

      it("SemVerSpecificTargetVersion") {
        val result = SemVerResult(prev, SemVerSpecificTargetVersion(curr), Seq.empty)
        assert(result.upgradeLegality.legality === UpgradeLegalityType.Fatal)
        assert(result.upgradeLegality.reason.contains("downgrade"))
      }
    }

    describe("major") {
      val curr = ReleaseVersion(2, 3, 3, SemVerIdentifierList.empty, isDirty = false)

      it("SemVerLimitTargetVersion") {
        val result = SemVerResult(prev, SemVerLimitTargetVersion(curr), Seq.empty)
        assert(result.upgradeLegality.legality === UpgradeLegalityType.Fatal)
        assert(result.upgradeLegality.reason.contains("downgrade"))
      }

      it("SemVerSpecificTargetVersion") {
        val result = SemVerResult(prev, SemVerSpecificTargetVersion(curr), Seq.empty)
        assert(result.upgradeLegality.legality === UpgradeLegalityType.Fatal)
        assert(result.upgradeLegality.reason.contains("downgrade"))
      }
    }
  }

  describe("not normalized") {
    val prev = ReleaseVersion(1, 2, 3, SemVerIdentifierList.empty, isDirty = false)

    describe("patch") {
      val curr = ReleaseVersion(1, 2, 7, SemVerIdentifierList.empty, isDirty = false)

      it("SemVerLimitTargetVersion") {
        val result = SemVerResult(prev, SemVerLimitTargetVersion(curr), Seq.empty)
        assert(result.upgradeLegality.legality === UpgradeLegalityType.Legal)
      }

      it("SemVerSpecificTargetVersion") {
        val result = SemVerResult(prev, SemVerSpecificTargetVersion(curr), Seq.empty)
        assert(result.upgradeLegality.legality === UpgradeLegalityType.Fatal)
        assert(result.upgradeLegality.reason.contains("not normalized"))
      }
    }

    describe("minor") {
      val curr = ReleaseVersion(1, 3, 1, SemVerIdentifierList.empty, isDirty = false)

      it("SemVerLimitTargetVersion") {
        val result = SemVerResult(prev, SemVerLimitTargetVersion(curr), Seq.empty)
        assert(result.upgradeLegality.legality === UpgradeLegalityType.Legal)
      }

      it("SemVerSpecificTargetVersion") {
        val result = SemVerResult(prev, SemVerSpecificTargetVersion(curr), Seq.empty)
        assert(result.upgradeLegality.legality === UpgradeLegalityType.Fatal)
        assert(result.upgradeLegality.reason.contains("not normalized"))
      }
    }

    describe("major") {
      val curr = ReleaseVersion(2, 1, 1, SemVerIdentifierList.empty, isDirty = false)

      it("SemVerLimitTargetVersion") {
        val result = SemVerResult(prev, SemVerLimitTargetVersion(curr), Seq.empty)
        assert(result.upgradeLegality.legality === UpgradeLegalityType.Legal)
      }

      it("SemVerSpecificTargetVersion") {
        val result = SemVerResult(prev, SemVerSpecificTargetVersion(curr), Seq.empty)
        assert(result.upgradeLegality.legality === UpgradeLegalityType.Fatal)
        assert(result.upgradeLegality.reason.contains("not normalized"))
      }
    }
  }

  describe("unchanged") {
    val prev = ReleaseVersion(3, 3, 3, SemVerIdentifierList.empty, isDirty = false)

    describe("clean") {
      val curr = prev.copy()

      it("SemVerLimitTargetVersion") {
        val result = SemVerResult(prev, SemVerLimitTargetVersion(curr), Seq.empty)
        assert(result.upgradeLegality.legality === UpgradeLegalityType.Legal)
        assert(result.upgradeLegality.reason.contains("unchanged"))
      }

      it("SemVerSpecificTargetVersion") {
        val result = SemVerResult(prev, SemVerSpecificTargetVersion(curr), Seq.empty)
        assert(result.upgradeLegality.legality === UpgradeLegalityType.Legal)
        assert(result.upgradeLegality.reason.contains("unchanged"))
      }
    }

    describe("backward errors") {
      val curr = prev.copy()

      it("SemVerLimitTargetVersion") {
        val result = buildSemVerResult(prev, SemVerLimitTargetVersion(curr), backwardProblems, Seq.empty)
        assert(result.upgradeLegality.legality === UpgradeLegalityType.Error)
        assert(result.upgradeLegality.reason.contains("unchanged"))
      }

      it("SemVerSpecificTargetVersion") {
        val result = buildSemVerResult(prev, SemVerSpecificTargetVersion(curr), backwardProblems, Seq.empty)
        assert(result.upgradeLegality.legality === UpgradeLegalityType.Error)
        assert(result.upgradeLegality.reason.contains("unchanged"))
      }
    }

    describe("forward errors") {
      val curr = prev.copy()

      it("SemVerLimitTargetVersion") {
        val result = buildSemVerResult(prev, SemVerLimitTargetVersion(curr), Seq.empty, forwardProblems)
        assert(result.upgradeLegality.legality === UpgradeLegalityType.Error)
        assert(result.upgradeLegality.reason.contains("unchanged"))
      }

      it("SemVerSpecificTargetVersion") {
        val result = buildSemVerResult(prev, SemVerSpecificTargetVersion(curr), Seq.empty, forwardProblems)
        assert(result.upgradeLegality.legality === UpgradeLegalityType.Error)
        assert(result.upgradeLegality.reason.contains("unchanged"))
      }
    }

    describe("dirty") {
      val curr = prev.copy(isDirty = true)

      it("SemVerLimitTargetVersion") {
        val result = SemVerResult(prev, SemVerLimitTargetVersion(curr), Seq.empty)
        assert(result.upgradeLegality.legality === UpgradeLegalityType.Error)
        assert(!result.upgradeLegality.reason.contains("unchanged"))
      }

      it("SemVerSpecificTargetVersion") {
        val result = SemVerResult(prev, SemVerSpecificTargetVersion(curr), Seq.empty)
        assert(result.upgradeLegality.legality === UpgradeLegalityType.Error)
        assert(!result.upgradeLegality.reason.contains("unchanged"))
      }
    }
  }

  private def buildSemVerResult(
    prev: ReleaseVersion, curr: SemVerTargetVersion, backwardProblems: Seq[String], forwardProblems: Seq[String]
  ): SemVerResult = {

    val result = new SemVerResultBuilder(prev, curr)
    forwardProblems.foreach(result.forward)
    backwardProblems.foreach(result.backward)
    result.build()
  }
}
