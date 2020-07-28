package com.rallyhealth.sbt.semver

import com.rallyhealth.sbt.semver.SemVerDiff.Delta.DiffType
import com.rallyhealth.sbt.versioning.{ReleaseVersion, SemanticVersion}

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
  def calc(prev: ReleaseVersion, target: ReleaseVersion): Delta = {
    require(!prev.isDirty, s"prev=$prev cannot be dirty")

    Delta(
      target.major - prev.major,
      target.minor - prev.minor,
      target.patch - prev.patch
    )
  }
}
