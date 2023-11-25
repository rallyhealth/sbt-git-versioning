package com.rallyhealth.sbt.versioning

import se.sawano.java.text.AlphanumericComparator

import scala.language.implicitConversions
import scala.util.matching.Regex

/**
  * A [[SemanticVersion]] has several identifiers, extra chunks of data that follow the major/minor/version.
  * Some well known examples are "dirty", "SNAPSHOT", "alpha", "m1", "rc2".
  */
sealed trait SemVerIdentifier extends Ordered[SemVerIdentifier]

object SemVerIdentifier {

  case object Empty extends SemVerIdentifier {
    override def compare(that: SemVerIdentifier): Int = that match {
      case Empty => 0
      case _ => -1
    }
  }
}

/**
  * A simple string identifier.
  *
  * See [[http://semver.org/#spec-item-9]]
  */
case class StringSemVerIdentifier(value: String) extends SemVerIdentifier {

  require(StringSemVerIdentifier.regex.pattern.matcher(value).matches(), s"value=$value does not match regex")

  override def toString: String = value

  override def compare(that: SemVerIdentifier): Int = that match {
    case thatString: StringSemVerIdentifier => StringSemVerIdentifier.comparator.compare(this.value, thatString.value)
    case _ => toString.compare(that.toString)
  }
}

object StringSemVerIdentifier {

  /**
    * Regex from [[http://semver.org/#spec-item-9]]. This follows the examples because the docs and examples are
    * inconsistent:
    * - The doc pattern says "[0-9A-Za-z-]" when in the examples allow "." but has an example "1.0.0-x.7.z.92"
    * - The doc says "Numeric identifiers MUST NOT include leading zeroes." but has an example "1.0.0-0.3.7"
    */
  val regex: Regex = "[0-9A-Za-z.]+".r

  val dirty = StringSemVerIdentifier("dirty")
  val snapshot = StringSemVerIdentifier("SNAPSHOT")

  private val comparator = new AlphanumericComparator()

  implicit def string2StringIdentifier(value: String): StringSemVerIdentifier = StringSemVerIdentifier(value)

}

case class CommitsSemVerIdentifier(commits: Int) extends SemVerIdentifier {
  GitCommitWithCount.assertCount(commits)

  override def toString: String = commits.toString

  override def compare(that: SemVerIdentifier): Int = that match {
    case thatCommits: CommitsSemVerIdentifier => commits - thatCommits.commits
    case _ => toString.compare(that.toString)
  }
}

object CommitsSemVerIdentifier {

  val regex: Regex = "[0-9]+".r

  implicit def int2IntegerIdentifier(value: Int): CommitsSemVerIdentifier = CommitsSemVerIdentifier(value)
}

/**
  * Sometimes the commit's hash is included as a [[SemVerIdentifier]]. Usually this comes from the git log output.
  *
  * Hashes usually go after the [[StringSemVerIdentifier]] in a version's string format.
  *
  * See also [[GitCommit]]
  *
  * @param hash The abbreviated object hash, see [[GitCommit.abbreviatedHash]]. This can be the full 40 digits but it
  * is not necessary.
  */
case class HashSemVerIdentifier(hash: String) extends SemVerIdentifier {

  require(
    HashSemVerIdentifier.regex.pattern.matcher(hash).matches(),
    "must consist of only hexadecimal characters optionally prefixed with 'g'")
  require(
    hash.head != 'g',
    "hash must not be prefixed with a 'g'; we do not use Git's hash format")

  override def toString: String = hash

  override def compare(that: SemVerIdentifier): Int = that match {
    case thatHash: HashSemVerIdentifier => hash.compareTo(thatHash.hash)
    case _ => super.compareTo(that)
  }
}

object HashSemVerIdentifier {

  val regex: Regex = "g?[0-9a-f]+".r

  /**
    * If necessary removes the pre-pended 'g' to match [[GitVersioningPlugin]] format.
    *
    * We do not use Git's format (with the 'g') because existing tools and code assume no 'g'.
    */
  def safeMake(value: String): HashSemVerIdentifier = {
    val hash = value.head match {
      case 'g' => value.tail
      case _ => value
    }
    HashSemVerIdentifier(hash)
  }

  implicit def string2HashIdentifier(value: String): HashSemVerIdentifier = safeMake(value)
}
