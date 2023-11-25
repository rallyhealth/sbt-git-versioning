package com.rallyhealth.sbt.versioning

sealed trait SemVerReleaseType extends Ordered[SemVerReleaseType] {

  override def compare(that: SemVerReleaseType): Int = {
    val order = SemVerReleaseType.AscSeverityOrder
    order.indexOf(this) compare order.indexOf(that)
  }
}

object SemVerReleaseType {

  case object Major extends SemVerReleaseType

  case object Minor extends SemVerReleaseType

  case object Patch extends SemVerReleaseType

  val AscSeverityOrder: Seq[SemVerReleaseType] = Seq(Patch, Minor, Major)

  @throws[IllegalArgumentException]("for invalid values")
  def fromStringOrThrow(s: String): SemVerReleaseType = {
    s match {
      case SemVerReleaseType(valid) => valid
      case invalid => throw new IllegalArgumentException(s"release type $invalid must be one of: {major, minor, patch}")
    }
  }

  def unapply(s: String): Option[SemVerReleaseType] = {
    s.trim.toLowerCase match {
      case "major" => Some(Major)
      case "minor" => Some(Minor)
      case "patch" => Some(Patch)
      case other => None
    }
  }

  implicit class ReleaseableSemanticVersion(val originalVersion: SemanticVersion) extends AnyVal {

    /**
      * Increases the version by some [[SemVerReleaseType]].
      */
    def release(releaseType: SemVerReleaseType): ReleaseVersion = {
      // Convert the unknown version to a release version without any increments yet.
      val v = originalVersion.toRelease.copy(versionIdentifiers = SemVerIdentifierList.empty)

      releaseType match {
        case Major => v.copy(
          major = v.major + 1,
          minor = 0,
          patch = 0
        )
        case Minor => v.copy(
          minor = v.minor + 1,
          patch = 0
        )
        case Patch =>
          // Release versions should be incremented normally.
          // Snapshot versions are already incremented as a patch over the previous version
          originalVersion match {
            case rv: ReleaseVersion => v.copy(patch = v.patch + 1)
            case _                  => v
          }
      }
    }
  }

}
