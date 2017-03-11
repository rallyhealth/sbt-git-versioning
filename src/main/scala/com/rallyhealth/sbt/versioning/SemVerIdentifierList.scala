package com.rallyhealth.sbt.versioning

import scala.language.implicitConversions

/** Convenience/helper class that makes dealing with multiple [[SemVerIdentifier]]s a little easier. */
case class SemVerIdentifierList(values: Seq[SemVerIdentifier]) extends Ordered[SemVerIdentifierList] {

  override val toString: String = {
    if (values.isEmpty)
      ""
    else
      values.mkString(SemVerIdentifierList.separatorChar.toString)
  }

  /**
    * [[http://semver.org/#spec-item-9]]:
    * Pre-release versions have a lower precedence than the associated normal version.
    * A pre-release version indicates that the version is unstable and might not satisfy
    * the intended compatibility requirements as denoted by its associated normal version.
    * Examples: 1.0.0-alpha, 1.0.0-alpha.1, 1.0.0-0.3.7, 1.0.0-x.7.z.92.
    *
    * [[http://semver.org/#spec-item-11]]:
    * Precedence for two pre-release versions with the same major, minor, and patch version MUST be determined
    * by comparing each dot separated identifier from left to right until a difference is found as follows:
    * - identifiers consisting of only digits are compared numerically
    * - and identifiers with letters or hyphens are compared lexically in ASCII sort order.
    *
    * Numeric identifiers always have lower precedence than non-numeric identifiers.
    */
  override def compare(that: SemVerIdentifierList): Int = {
    val thisIdentifiers = values.sorted
    val thatIdentifiers = that.values.sorted

    if (thisIdentifiers.isEmpty && thatIdentifiers.isEmpty) {
      0
      // an empty list of identifiers is considered GREATER than a non-empty list, see http://semver.org/#spec-item-11
    } else if (thisIdentifiers.isEmpty) {
      1
      // an empty list of identifiers is considered GREATER than a non-empty list, see http://semver.org/#spec-item-11
    } else if (thatIdentifiers.isEmpty) {
      -1
    } else {
      thisIdentifiers.zipAll(thatIdentifiers, SemVerIdentifier.Empty, SemVerIdentifier.Empty).foldLeft(0) {
        case (result, _) if result != 0 => result
        case (_, (a, b)) => a.compare(b)
      }
    }
  }

  def :+(other: SemVerIdentifier): SemVerIdentifierList = SemVerIdentifierList(values :+ other)

  def ++(other: SemVerIdentifierList): SemVerIdentifierList = SemVerIdentifierList(values ++ other.values)

  def ++(other: Seq[SemVerIdentifier]): SemVerIdentifierList = SemVerIdentifierList(values ++ other)

  def ++(other: Option[SemVerIdentifier]): SemVerIdentifierList = this ++ other.toSeq
}

object SemVerIdentifierList {

  val separatorChar = '-'

  val empty = SemVerIdentifierList(Seq.empty)

  implicit def identifier2IdentifierList(id: SemVerIdentifier): SemVerIdentifierList =
    SemVerIdentifierList(Seq(id))

  implicit def identifierSeq2IdentifierList(values: Seq[SemVerIdentifier]): SemVerIdentifierList =
    SemVerIdentifierList(values)

  implicit def stringIdentifierSeq2IdentifierList(values: Seq[String]): SemVerIdentifierList =
    SemVerIdentifierList(values.map(StringSemVerIdentifier(_)))
}
