package com.rallyhealth.sbt.versioning

/**
  * Defines major/minor/patch values that do not represent an exact version,
  * but rather a subset of version components that may be compared against [[SemanticVersion]]'s components.
  */
case class LowerBound(major: Int, minor: Int, patch: Int) {

  require(major >= 0, "major may not be negative: " + major)
  require(minor >= 0, "minor may not be negative: " + minor)
  require(patch >= 0, "patch may not be negative: " + patch)

  override def toString: String = s"$major.$minor.$patch"
}

object LowerBound {

  import scala.language.implicitConversions

  private val Pattern = "(\\d+)\\.(\\d+)\\.(\\d+)".r

  implicit def stringToLowerBound(s: String): LowerBound = s match {
    case Pattern(major, minor, patch) => LowerBound(major.toInt, minor.toInt, patch.toInt)
    case invalid => throw new IllegalArgumentException(s"invalid LowerBound: $invalid")
  }

  implicit def stringToSomeLowerBound(s: String): Option[LowerBound] = Some(s)
}
