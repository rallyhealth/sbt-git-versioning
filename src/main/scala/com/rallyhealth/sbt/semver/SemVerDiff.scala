package com.rallyhealth.sbt.semver

import com.rallyhealth.sbt.semver.SemVerDiff.Delta
import com.rallyhealth.sbt.semver.SemVerDiff.Delta.DiffType
import com.rallyhealth.sbt.versioning.{ReleaseVersion, SemanticVersion, SnapshotVersion}

/**
  * Describes the difference between two [[SemanticVersion]] instances.
  */
sealed trait SemVerDiff {

  /**
    * All differences can report the delta in the version components. Sometimes its helpful, sometimes its not, but
    * it is always informative so its made available in all cases.
    */
  def delta: Delta
}

object SemVerDiff {

  /**
    * Describes the difference between two [[SemanticVersion]] instances. Note that NEGATIVE differences may be
    * returned, but do not necessarily signal a downgrade, e.g. 1.2.3 to 2.0.0 is +1.-2.-3.
    *
    * @param major Delta of major increment
    * @param minor Delta of minor increment
    * @param patch Delta of patch increment
    */
  case class Delta(major: Int, minor: Int, patch: Int) {

    val diff: DiffType.Value = {
      if (major != 0) DiffType.Major
      else if (minor != 0) DiffType.Minor
      else if (patch != 0) DiffType.Patch
      else DiffType.None
    }
  }

  object Delta {

    object DiffType extends scala.Enumeration {

      val Major = Value("major")
      val Minor = Value("minor")
      val Patch = Value("patch")
      val None = Value("none")
    }

  }

  /**
    * Creates a [[SemVerDiff]] from two different [[SemanticVersion]]s.
    *
    * @param prev The previous version that was released in the past.
    */
  def calc(prev: ReleaseVersion, target: SemVerTargetVersion): SemVerDiff = {

    require(!prev.isDirty, s"prev=$prev cannot be dirty")

    val delta = Delta(
      target.version.major - prev.major,
      target.version.minor - prev.minor,
      target.version.patch - prev.patch)

    (delta, target) match {
      case UnchangedSemVerDiff(diff) => diff
      case DowngradeSemVerDiff(diff) => diff
      case NotNormalizedSemVerDiff(diff) => diff
      case _ => DeltaSemVerDiff(delta)
    }
  }
}

/**
  * Indicates a normal, valid, change in all of the significant version components: major, minor, or patch. This is
  * the only legal/valid [[SemVerDiff]] type.
  */
case class DeltaSemVerDiff(delta: Delta) extends SemVerDiff

/**
  * Indicates the upgrade is not actually an increment, that is there is a *negative* increment for the largest
  * version component.
  */
case class DowngradeSemVerDiff(delta: Delta) extends SemVerDiff

object DowngradeSemVerDiff {

  def unapply(tuple: (Delta, SemVerTargetVersion)): Option[DowngradeSemVerDiff] = {
    val (delta, _) = tuple

    val isDowngrade = delta.major < 0 ||
      (delta.major == 0 && delta.minor < 0) ||
      (delta.major == 0 && delta.minor == 0 && delta.patch < 0)
    if (isDowngrade)
      Some(DowngradeSemVerDiff(delta))
    else
      None
  }
}

/**
  * Indicates the version components are NOT the minimal changes necessary.
  *
  * For example, if the previous version was 4.6.2 the legal normalized upgrades would be:
  * - 4.6.3
  * - 4.7.0
  * - 5.0.0
  * And not:
  * - 4.7.5
  * - 4.9.0
  * - 6.0.0
  * - 5.0.1
  * - 5.1.0
  *
  * This should only be checked with [[SemVerSpecificTargetVersion]] because only a specific version can be "not
  * normal" -- otherwise you wouldn't be able to have a limit higher than the next version you are trying to release
  *
  */
case class NotNormalizedSemVerDiff(delta: Delta) extends SemVerDiff

object NotNormalizedSemVerDiff {

  def unapply(tuple: (Delta, SemVerTargetVersion)): Option[NotNormalizedSemVerDiff] = {
    val (delta, target) = tuple

    target match {

      case SemVerSpecificTargetVersion(version) =>
        val isNormalized =
          if (delta.major != 0) delta.major == 1 && version.minor == 0 && version.patch == 0
          else if (delta.minor != 0) delta.minor == 1 && version.patch == 0
          else delta.patch == 1

        if (!isNormalized)
          Some(NotNormalizedSemVerDiff(delta))
        else
          None

      case _ =>
        None
    }
  }

}

/**
  * Indicates that there is no change in any of the significant version components: major, minor, or patch.
  *
  * This situation does not indicate an error. Often it occurs when the only difference is
  * [[SemanticVersion.isDirty]] or [[SnapshotVersion.commitsSincePrevRelease]].
  */
case class UnchangedSemVerDiff(delta: Delta) extends SemVerDiff

object UnchangedSemVerDiff {

  def unapply(tuple: (Delta, SemVerTargetVersion)): Option[UnchangedSemVerDiff] = {
    val (delta, _) = tuple

    val isUnchanged = delta.major == 0 && delta.minor == 0 && delta.patch == 0
    if (isUnchanged)
      Some(UnchangedSemVerDiff(delta))
    else
      None
  }
}
