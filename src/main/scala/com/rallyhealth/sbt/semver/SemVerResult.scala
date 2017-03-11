package com.rallyhealth.sbt.semver

import com.rallyhealth.sbt.semver.SemVerDiff.Delta
import com.rallyhealth.sbt.semver.SemVerDiff.Delta.DiffType
import com.rallyhealth.sbt.semver.SemVerResult._
import com.rallyhealth.sbt.versioning.{ReleaseVersion, SemanticVersion, SnapshotVersion}

import scala.collection.mutable.ListBuffer

/**
  * An immutable object that contains the results of comparing two [[ReleaseVersion]]s using [[MiMaExecutor]].
  *
  * This class does not have the responsibility for deciding whether to stop the build or let it continue. That
  * responsibility is for [[SemVerHalter]].
  *
  * @param prev The previous version that was released in the past.
  */
case class SemVerResult(prev: ReleaseVersion, target: SemVerTargetVersion, problems: Seq[Problem]) {

  require(!prev.isDirty, s"prev=$prev cannot be dirty")

  val backwardsProblems: Seq[Problem] = problems.filter(problem => problem.direction == Direction.Backward)
  val forwardsProblems: Seq[Problem] = problems.filter(problem => problem.direction == Direction.Forward)

  /**
    * What the user MUST do, computed from looking at the errors, i.e. Direction of problems.
    *
    * This will never be [[com.rallyhealth.sbt.semver.SemVerDiff.Delta.DiffType.None]] because all changes should
    * increment something, so the smallest returned value will be "patch".
    */
  val requiredDiff: DiffType.Value = {
    if (backwardsProblems.nonEmpty) DiffType.Major
    else if (forwardsProblems.nonEmpty) DiffType.Minor
    else DiffType.Patch
  }

  /** What the user WANTS to do, computed from the different between [[prev]] and [[target]]. */
  val actualDiff: SemVerDiff = SemVerDiff.calc(prev, target)

  /**
    * Is this a legal upgrade? Is it acceptable according to SemVer rules to publish [[target]] if the previous published
    * version is [[prev]]?
    *
    * Note [[SemanticVersion.isDirty]] and SNAPSHOT does not play any part in determining [[UpgradeLegality]]. Those
    * are not official parts of SemVer. They are used in determining whether to halt a release from being published.
    */
  val upgradeLegality: UpgradeLegality = {
    SemVerDiff.calc(prev, target) match {

      case _: DowngradeSemVerDiff =>
        UpgradeLegality(UpgradeLegalityType.Fatal, s"downgrade from prev=$prev")

      case _: NotNormalizedSemVerDiff =>
        val msg = "not normalized (e.g. you can't jump to 1.5.0 from 1.2.3, it should be 1.3.0)"
        UpgradeLegality(UpgradeLegalityType.Fatal, msg)

      case _: UnchangedSemVerDiff =>
        calcUnchangedSemVerDiff()

      case _: DeltaSemVerDiff =>
        calcDeltaUpgradeLegality()
    }

  }

  /**
    * Computing the legality of [[UnchangedSemVerDiff]] is a bit more involved since we have to check if other things
    * might have changed.
    *
    * I think this is actually an impossible situation to get in, given all the other checks in other parts of the
    * system. I can't think of a way to test this in an SBT scripted test. I'll leave it as is for now but I think it
    * could be simplified or removed.
    */
  private def calcUnchangedSemVerDiff(): UpgradeLegality = {
    if (prev == target.version && problems.isEmpty)
      UpgradeLegality(
        UpgradeLegalityType.Legal, s"unchanged from prev=$prev")
    else if (prev != target.version)
      UpgradeLegality(
        UpgradeLegalityType.Error, s"non-numerical change compared to prev=$prev (e.g. dirty and/or SNAPSHOT)")
    else // if (problems.nonEmpty)
      UpgradeLegality(
        UpgradeLegalityType.Error, s"unchanged from prev=$prev but there are errors")
  }

  /**
    * Computing the legality of [[Delta]] is a bit more involved since we have to check what the user
    * wants to do and compare it to what the user must do according to the problems we've found.
    */
  private def calcDeltaUpgradeLegality(): UpgradeLegality = {

    val legalResult = UpgradeLegality(UpgradeLegalityType.Legal, s"required diffType=$requiredDiff achieved")
    val errorResult = UpgradeLegality(UpgradeLegalityType.Error, s"required diffType=$requiredDiff NOT achieved")

    (actualDiff.delta.diff, requiredDiff) match {
      // a match made in heaven
      case (x, y) if x == y => legalResult
      // anything goes with a major upgrade
      case (DiffType.Major, _) => legalResult
      // a minor is a larger net than a patch
      case (DiffType.Minor, DiffType.Patch) => legalResult
      // a patches and minors require at least a patch difference
      case (_, DiffType.Patch) => legalResult
      case _ => errorResult
    }
  }
}

object SemVerResult {

  case class Problem(direction: Direction.Value, message: String)

  /** Indicates whether the error is a backwards or forwards compatible error */
  object Direction extends scala.Enumeration {

    val Backward = Value("backward")
    val Forward = Value("forward")
  }

  object UpgradeLegalityType extends scala.Enumeration {

    /**
      * The upgrade is illegal no matter what other config or state might say -- its a fundamental violation of
      * SemVer, e.g. [[DowngradeSemVerDiff]].
      */
    val Fatal = Value("fatal")

    /**
      * The upgrade is illegal if you are you on a clean [[ReleaseVersion]]. Its a warning for dirty and/or
      * [[SnapshotVersion]]s.
      */
    val Error = Value("error")

    /** Upgrade is perfectly legal in all situations. */
    val Legal = Value("legal")
  }

  /**
    * A compact description of whether an upgrade is "legal", i.e. acceptable according to SemVer rules.
    *
    * Note [[SemanticVersion.isDirty]] and SNAPSHOT does not play any part in determining [[UpgradeLegality]]. Those
    * are not official parts of SemVer. They are used in determining whether to halt a release from being published.
    *
    * @param reason Human-friendly reason why the upgrade is legal or not
    */
  case class UpgradeLegality(legality: UpgradeLegalityType.Value, reason: String)

}

/** A mutable object that helps construct a [[SemVerResult]]. */
class SemVerResultBuilder(prev: ReleaseVersion, curr: SemVerTargetVersion) {

  private val problems: ListBuffer[Problem] = new ListBuffer[Problem]

  def backward(problem: String): Unit = problems += Problem(Direction.Backward, problem)

  def forward(problem: String): Unit = problems += Problem(Direction.Forward, problem)

  def build(): SemVerResult = SemVerResult(prev, curr, problems)
}
