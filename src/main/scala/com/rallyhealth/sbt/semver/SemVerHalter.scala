package com.rallyhealth.sbt.semver

import com.rallyhealth.sbt.semver.SemVerPluginUtils.SemVerPrefix
import com.rallyhealth.sbt.semver.SemVerResult.{UpgradeLegality, UpgradeLegalityType}
import com.rallyhealth.sbt.versioning.ReleaseVersion
import sbt.{Level, Logger}

/**
  * A utility class that will handle logging the results and halting the build if necessary. This class consumes the
  * output of [[SemVerPluginUtils.compare()]]
  */
trait SemVerHalter {

  protected def result: SemVerResult

  /** True to run actually run this method for pre-release versions, e.g. 0.0.1 or 0.1.0 */
  protected def preReleaseEnabled: Boolean

  /**
    * True if the build should be failed. Use [[haltBuild()]] to actually throw an exception and halt the build.
    */
  lazy val shouldHaltBuild: Boolean = result.upgradeLegality.legality match {

    case UpgradeLegalityType.Fatal =>
      true

    case UpgradeLegalityType.Error =>

      // builds should only be halted for clean releases; dirty and/or SNAPSHOTs are WIP so SemVer is informational
      if (!result.target.version.isDirty) {
        if (result.target.version.isPreRelease) {
          preReleaseEnabled
        } else {
          true
        }
      } else {
        false
      }

    case UpgradeLegalityType.Legal =>
      false
  }

  /**
    * Determines the level which messages should be logged. If we are going to halt log messages are more useful as
    * errors than warnings.
    */
  protected lazy val logLevel: Level.Value = if (shouldHaltBuild) Level.Error else Level.Info

  /**
    * Halt (i.e. fail) the build is done by throwing an exception. This method calls [[shouldHaltBuild()]] and throws
    * an exception if that method returns true.
    */
  def haltBuild(): Unit = {
    if (shouldHaltBuild) {
      // the sister message that says PASSES is in haltBuild()
      throw new IllegalStateException(
        s"Proposed RELEASE version=${result.target.version} FAILED SemVer rules, failing build")
    }
  }

  /**
    * Logs a lot of informational stuff about the build and any problems detected. If the build will fail this will
    * log as errors, otherwise it will log as warning.
    *
    * This method should print the most important information LAST to hopefully avoid scrolling backward to figure out
    * what is going on.
    */
  def log(logger: Logger): Unit

}

/**
  * Determines if the build should halt based only on the previous [[ReleaseVersion]] and [[SemVerTargetVersion]].
  * Some [[UpgradeLegalityType.Fatal]] can be determined based only on the versions.
  *
  * This [[SemVerHalter]] should be checked BEFORE using a [[MiMaExecutor]] to determine [[SemVerResult.Problem]]s. If
  * this does NOT halt the build then proceed with the SemVer check and use [[PostCheckSemVerHalter]] with the
  * [[SemVerResult.Problem]]s to determine if the build needs to be halted.
  */
class PreCheckSemVerHalter(prev: ReleaseVersion, target: SemVerTargetVersion, protected val preReleaseEnabled: Boolean)
  extends SemVerHalter {

  override protected val result: SemVerResult = new SemVerResultBuilder(prev, target).build()

  override def log(logger: Logger): Unit = {

    if (result.target.version.isPreRelease && !preReleaseEnabled)
      logger.info(
        s"$SemVerPrefix PRE-RELEASE version=${result.target.version} (i.e. SemVer checks are informational only)")

    result.upgradeLegality match {
      case UpgradeLegality(legality, reason) if legality == UpgradeLegalityType.Fatal =>
        logger.log(logLevel, s"$SemVerPrefix version=${result.target.version} FAILED: $reason")
      case _ =>
    }
  }
}

/**
  * Determines if the build should halt based only on the [[SemVerResult.Problem]]s found by a [[MiMaExecutor]].
  *
  * Before using this you should check first the versions with [[PreCheckSemVerHalter]]. It may decide to halt the
  * build before using the [[MiMaExecutor]].
  */
class PostCheckSemVerHalter(protected val result: SemVerResult, protected val preReleaseEnabled: Boolean)
  extends SemVerHalter {

  override def log(logger: Logger): Unit = {

    locally {
      val backward = result.backwardsProblems.size
      val forward = result.forwardsProblems.size
      val total = backward + forward
      val diffType = s"required diffType=${result.requiredDiff}"
      logger.log(logLevel, s"$SemVerPrefix Errors total=$total, backward=$backward, forward=$forward, $diffType")
    }

    result.problems
      // sort by direction-message for consistency
      .sortBy(x => s"${x.direction}-${x.message}")
      .zipWithIndex
      .foreach {
        case (msg, index) =>
          val size = result.problems.size
          val direction = msg.direction.toString.capitalize
          logger.log(logLevel, s"$SemVerPrefix (${index + 1}/$size) $direction -> ${msg.message}")
      }

    val isLegalUpgrade = result.upgradeLegality
    val passedOrFailed = if (isLegalUpgrade.legality == UpgradeLegalityType.Legal) "PASSED" else "FAILED"
    logger.log(logLevel, s"$SemVerPrefix version=${result.target.version} $passedOrFailed: ${isLegalUpgrade.reason}")
  }

}
