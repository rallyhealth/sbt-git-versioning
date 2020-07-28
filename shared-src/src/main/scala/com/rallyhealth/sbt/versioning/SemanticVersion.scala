package com.rallyhealth.sbt.versioning

import com.rallyhealth.sbt.versioning.SemanticVersion.versionPrefix

import scala.util.matching.Regex

/**
  * Describes a single "version" of an artifact (that may or may not be published or even exist). Ideally the version
  * will follow Semantic Version philosophy and formatting, but we can't always guarantee that.
  *
  * A version is created from the working directory, not simply typically from a Git tag or artifact version. To be
  * clear a version may include commit count and whether it is dirty or not, which may seem weird but is technically
  * part of the version of the artifact. This is an important distinction, because its more inclusive than just the
  * version of what Git knows about, i.e. working changes are not recorded in git history.
  */
sealed trait SemanticVersion extends Ordered[SemanticVersion] {

  def major: Int

  def minor: Int

  def patch: Int

  def isDirty: Boolean

  /** Everything that is tacked on after the major.minor.patch */
  def identifiers: SemVerIdentifierList

  /**
    * Only the identifiers that matter to versioning.
    */
  def versionIdentifiers: SemVerIdentifierList

  def isRelease: Boolean

  def isSnapshot: Boolean = !isRelease

  override lazy val toString: String = {
    val tagStr = identifiers.toString
    s"$major.$minor.$patch" + (if (tagStr.isEmpty) "" else SemVerIdentifierList.separatorChar + tagStr)
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
  override def compare(that: SemanticVersion): Int = {

    val majorDelta = major.compare(that.major)
    if (majorDelta != 0) return majorDelta

    val minorDelta = minor.compare(that.minor)
    if (minorDelta != 0) return minorDelta

    val patchDelta = patch.compare(that.patch)
    if (patchDelta != 0) return patchDelta
    // we consider dirty to be greater than non-dirty, because dirty implies additional work
    val dirtyDelta = isDirty.compareTo(that.isDirty)
    if (dirtyDelta != 0) return dirtyDelta

    val identifiersDelta = versionIdentifiers.compare(that.versionIdentifiers)
    if (identifiersDelta != 0) return identifiersDelta

    0
  }

  /**
    * True if this is a initial development version -- the major version is 0, e.g. 0.0.1 or 0.1.0.
    * @see http://semver.org/#spec-item-4
    */
  def isInitialDevVersion: Boolean = major == 0

  /**
    * @return the current major, minor, patch, identifiers, and dirty status as a release version.
    */
  def toRelease: ReleaseVersion

}

object SemanticVersion {

  // Also see VersionSuffix.regex
  val versionPrefix: Regex = s"v?([0-9]+\\.[0-9.]+)((?:-${StringSemVerIdentifier.regex})*?)".r

  /** Splits a "x.y.z" string into a Tuple of Ints. */
  private object VersionExtractor {

    def unapply(version: String): Option[(Int, Int, Int)] = {
      version.split('.').map(_.toInt) match {
        case Array(x, y, z) => Some((x, y, z))
        case _ => None
      }
    }
  }

  /** Splits a "-id1-id2" string into sequence of [[StringSemVerIdentifier]]. */
  private object IdentifierExtractor {

    def unapply(version: String): Option[Seq[StringSemVerIdentifier]] = {
      version.split(SemVerIdentifierList.separatorChar).filter(_.nonEmpty) match {
        case data if data.isEmpty => Some(Seq.empty)
        case data => Some(data.map(StringSemVerIdentifier(_)))
      }
    }
  }

  /**
    * Creates a [[SemanticVersion]] from the provided string.
    *
    * This adheres to the README.md "Version Format" text.
    */
  def fromString(string: String): Option[SemanticVersion] = {

    def isDirtyVal(isDirty: String): Boolean = Option(isDirty).contains(StringSemVerIdentifier.dirty.value)

    string match {

      case SnapshotVersion.regex(VersionExtractor(ma, mi, pa), IdentifierExtractor(extraIds), commits, hash, isDirty) =>
        val commitNum = CommitsSemVerIdentifier(commits.toInt)
        Some(SnapshotVersion(ma, mi, pa, extraIds, isDirtyVal(isDirty), HashSemVerIdentifier.safeMake(hash), commitNum))

      case ReleaseVersion.regex(VersionExtractor(ma, mi, pa), IdentifierExtractor(extraIds), isDirty) =>
        Some(ReleaseVersion(ma, mi, pa, extraIds, isDirtyVal(isDirty)))

      case _ =>
        None
    }
  }

  def parseDynVerString(string: String): Option[SemanticVersion] = {
    // The dynver versions are different they don't have the explicit '-SNAPSHOT' you have to infer it from the existence of the hash
    val dirtyRegex = s"-${SnapshotVersion.dynverTimeStampPattern}".r
    val headRegex = s"HEAD-${SnapshotVersion.dynverTimeStampPattern}".r
    def isDirtyVal(versionSuffixes: String): Boolean = Option(versionSuffixes).map(dirtyRegex.pattern.matcher(_).matches).getOrElse(false)

    string match {

      case SnapshotVersion.dynVerRegex(VersionExtractor(ma, mi, pa), IdentifierExtractor(extraIds), commits, hash, isDirty) =>
        if (commits.toInt != 0) { // If you get a 0 commit version it shouldn't have have a hash section
          val commitNum = CommitsSemVerIdentifier(commits.toInt)
          val parsed = SnapshotVersion(ma, mi, pa, extraIds, isDirtyVal(isDirty), HashSemVerIdentifier.safeMake(hash), commitNum)
          Some(parsed)
        } else {
          // Not sure why this is modeled this way but this comes out with a snapshot despite being a `ReleaseVersion`
          val parsed = ReleaseVersion(ma, mi, pa, isDirty = isDirtyVal(isDirty))

          Some(parsed)
        }

      case ReleaseVersion.regex(VersionExtractor(ma, mi, pa), IdentifierExtractor(extraIds), isDirty) =>
        Some(ReleaseVersion(ma, mi, pa, extraIds, isDirtyVal(isDirty)))

      case headRegex(_*) =>
        Some(ReleaseVersion(0, 0, 1, SemVerIdentifierList.empty, true))

      case _ =>
        None
    }
  }
}

/**
  * A released version, a x.y.z type release. Contrast with [[SnapshotVersion]].
  *
  * @param isDirty if True the [[SemVerIdentifierList]]s will contain [[StringSemVerIdentifier.dirty]]
  */
case class ReleaseVersion(
  major: Int, minor: Int, patch: Int, versionIdentifiers: SemVerIdentifierList = SemVerIdentifierList.empty, isDirty: Boolean = false)
  extends SemanticVersion {

  override val identifiers: SemVerIdentifierList = {
    SemVerIdentifierList.empty ++
      versionIdentifiers ++
      (if (isDirty) Seq(StringSemVerIdentifier.dirty, StringSemVerIdentifier.snapshot) else Seq.empty)
  }

  override def compare(that: SemanticVersion): Int = that match {
    case thatSnapshot: SnapshotVersion =>
      val superDelta = super.compare(thatSnapshot.toRelease)
      // snapshots for this release are considered LESS than the release, see http://semver.org/#spec-item-11
      if (superDelta == 0)
        1
      else
        superDelta

    case thatRelease: ReleaseVersion =>
      super.compare(thatRelease)

  }

  override def toRelease: ReleaseVersion = this

  override def isRelease: Boolean = true
}

object ReleaseVersion {

  /** Regex that matches a [[ReleaseVersion]]. See README.md */
  val regex: Regex = {
    import SemanticVersion._
    (versionPrefix + s"(?:-(${StringSemVerIdentifier.dirty})-${StringSemVerIdentifier.snapshot})?").r
  }

  /** The first version, the "In the beginning" version, the version from which all other versions come forth. */
  val initialVersion = ReleaseVersion(0, 0, 1, SemVerIdentifierList.empty, isDirty = true)

  def unapply(commit: GitCommit): Option[ReleaseVersion] = fromCommit(commit)

  def unapply(string: String): Option[ReleaseVersion] =
    SemanticVersion.fromString(string).collect { case v: ReleaseVersion => v }

  def parseAsCleanOrThrow(value: String): ReleaseVersion = {
    unapply(value)
      .filter(!_.isDirty)
      .getOrElse(
        throw new IllegalArgumentException(s"[SemVer] Config problem: cannot parse value=$value as clean release version"))
  }

  /**
    * This returns the [[ReleaseVersion]] from the first [[GitCommit.tags]], using [[unapply()]], if such a tag
    * exists.
    *
    * This only returns [[ReleaseVersion]]s because a [[GitCommit]] should never be tagged with a [[SnapshotVersion]] --
    * those are always computed based on previous [[ReleaseVersion]]s.
    *
    * @return None if no tags that can be parsed as a [[ReleaseVersion]]
    */
  def fromCommit(commit: GitCommit): Option[ReleaseVersion] = {
    val releases =
      commit.tags.flatMap(SemanticVersion.fromString)
        .collect { case x: ReleaseVersion if !x.isDirty => x }

    releases.sorted[SemanticVersion].lastOption
  }

  import scala.language.implicitConversions

  /**
    * Sugar to allow easy setting of values using strings in the sbt console or builds.
    *
    * For example:
    * {{{
    *   val gitVersioningSnapshotLowerBound = settingKey[Option[ReleaseVersion]]("...")
    *   gitVersioningSnapshotLowerBound := "1.2.3"
    * }}}
    *
    * {{{
    *   sbt> set gitVersioningSnapshotLowerBound := "1.2.3"
    * }}}
    */
  implicit def fromStringToMaybeReleaseVersion(s: String): Option[ReleaseVersion] = unapply(s)
}

/**
  * A pre-release version, something that is not fully released, of the form x.y.z-SNAPSHOT. This will always
  * contain the [[StringSemVerIdentifier.snapshot]]
  */
case class SnapshotVersion(
  major: Int,
  minor: Int,
  patch: Int,
  versionIdentifiers: SemVerIdentifierList,
  isDirty: Boolean,
  commitHash: HashSemVerIdentifier,
  commitsSincePrevRelease: CommitsSemVerIdentifier)
  extends SemanticVersion {

  override val identifiers: SemVerIdentifierList = {
    (
      SemVerIdentifierList.empty ++
      versionIdentifiers :+
      commitsSincePrevRelease :+
      commitHash) ++
      (if (isDirty) Seq(StringSemVerIdentifier.dirty) else Seq.empty) :+
      StringSemVerIdentifier.snapshot
  }

  override def compare(that: SemanticVersion): Int = that match {
    case thatSnapshot: SnapshotVersion =>
      val superDelta = super.compare(thatSnapshot.toRelease)
      if (superDelta != 0) return superDelta

      val hashDelta = commitHash.compare(thatSnapshot.commitHash)
      if (hashDelta != 0) return hashDelta

      val commitsDelta = commitsSincePrevRelease.compare(thatSnapshot.commitsSincePrevRelease)
      if (commitsDelta != 0) return commitsDelta

      0

    case thatRelease: ReleaseVersion =>
      // Compare only major.minor.patch ignoring "dirty" as a tie-breaker.
      val majorMinorPatchDelta = toRelease.copy(isDirty = false).compare(thatRelease.copy(isDirty = false))

      // snapshots for this release are considered LESS than the release, see http://semver.org/#spec-item-11
      if (majorMinorPatchDelta == 0)
        -1
      else
        majorMinorPatchDelta
  }

  override def toRelease: ReleaseVersion = ReleaseVersion(major, minor, patch, versionIdentifiers, isDirty)

  override def isRelease: Boolean = false
}

object SnapshotVersion {

  private val dirtyIdentifierRegex = s"(?:-(${StringSemVerIdentifier.dirty}))?"
  private val commitCountAndHashPattern = "-(" + CommitsSemVerIdentifier.regex + ")-(" + HashSemVerIdentifier.regex + ")"
  val dynverTimeStampPattern = "\\d{8}-\\d{4}"

  /** Regex that matches a [[SnapshotVersion]]. See README.md */
  val regex: Regex = {
    import SemanticVersion._
    (versionPrefix + commitCountAndHashPattern + dirtyIdentifierRegex + s"-${StringSemVerIdentifier.snapshot}").r
  }

  val dynVerRegex: Regex = {
    (versionPrefix + commitCountAndHashPattern + s"(-$dynverTimeStampPattern)?").r
  }

  /**
    * The first version, the "In the beginning" version, the version from which all other versions come forth.
    * This is intended to be used when there are no commits or previous tags we can bas
    */
  def createInitialVersion(commit: GitCommitWithCount): SnapshotVersion =
    SnapshotVersion(0, 0, 1, SemVerIdentifierList.empty, isDirty = false, commit.sha.abbreviatedHash, commit.count)

  /**
    * Creates a [[SnapshotVersion]] when you have a previous [[ReleaseVersion]] and a commit that is AFTER the
    * commit that was tagged with [[ReleaseVersion]], i.e. [[ReleaseVersion]]'s commit != `commit` parameter
    *
    * @param release The previous [[ReleaseVersion]], the "base" version if you will.
    * @param commit The commit you want to use to make a [[SnapshotVersion]].
    */
  def createAfterRelease(release: ReleaseVersion, commit: GitCommitWithCount): SnapshotVersion = {
    require(!commit.sha.tags.contains(release.toString), "commit should NOT be tagged with version")

    SnapshotVersion(
      release.major,
      release.minor,
      release.patch + 1,
      release.identifiers,
      isDirty = false,
      commit.sha.abbreviatedHash,
      commit.count)
  }

}
