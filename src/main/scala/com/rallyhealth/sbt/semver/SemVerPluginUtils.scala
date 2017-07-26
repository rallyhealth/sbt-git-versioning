package com.rallyhealth.sbt.semver

import com.rallyhealth.sbt.semver.SemVerPlugin.autoImport.{semVerEnforceAfterVersion, _}
import com.rallyhealth.sbt.versioning.GitVersioningPlugin.autoImport.{versionFromGit, versionOverride}
import com.rallyhealth.sbt.versioning._
import com.typesafe.tools.mima.core._
import com.typesafe.tools.mima.plugin._
import sbt.Keys._
import sbt._

import scala.tools.nsc.util.JavaClassPath

/** Functionality to support the SemVer SBT plugin. */
object SemVerPluginUtils {

  val SemVerPrefix = s"[SemVer]"
  val ConfigErrorPrefix = s"$SemVerPrefix Config problem:"
  private val abortPrefix = s"$SemVerPrefix Check aborted:"
  private val noViolationsSuffix = "-- nothing to compare for SemVer violations"

  /**
    * Compares the current, working [[SemanticVersion]] from the [[version]] setting with the previous [[SemanticVersion]]
    * from git history using [[compare()]]. Then this calls [[SemVerHalter.haltBuild()]] to determine whether the build
    * should be halted.
    */
  def semVerCheck: Def.Initialize[Task[Unit]] = Def.task {

    // This method is ugly and not unit tested because I have no idea how to test a "Def.task {...}" method
    // effectively. It depends on a lot of different SBT settings, and extracting them is a) messy and b) they are
    // often hard to mock

    // "version" SettingKey is assumed to be a SemanticVersion; see the overload in RallyVersioningPlugin
    val semver: SemanticVersion = SemanticVersion.fromString(version.value).getOrElse(
      throw new IllegalArgumentException(s"version=${version.value} is not a valid SemVer"))
    val enforceAfterVersion: Option[ReleaseVersion] = semVerEnforceAfterVersion.value.map(parseAsCleanOrThrow)

    val preReleaseEnabled = semVerPreRelease.value
    val taskStreams: TaskStreams = streams.value
    implicit val logger: sbt.Logger = taskStreams.log

    if (semver.isPreRelease && !preReleaseEnabled) {
      logger.info(s"$SemVerPrefix Checking DISABLED for pre-release version=$semver")
    } else if (enforceAfterVersion.isDefined && semver <= enforceAfterVersion.get) {
      logger.info(s"$SemVerPrefix Checking DISABLED for version=$semver less than or equal to" +
        s" enforceAfterVersion=${enforceAfterVersion.get} $noViolationsSuffix")
    } else {

      val maybeTargetVersion = calcTargetVersion(
        versionOverride.value, semVerLimit.value, versionFromGit.value, logger)

      maybeTargetVersion.foreach { targetVersion =>

        lazy val miMaExecutor: MiMaExecutor = createMiMaExecutor(
          (fullClasspath in MimaKeys.mimaFindBinaryIssues).value, taskStreams)

        CustomMiMaLibMiMaExecutor.classPath(MimaKeys.mimaCurrentClassfiles.value) match {
          case Some(_) =>

            lazy val currMimaInfo = CurrMimaInput(MimaKeys.mimaCurrentClassfiles.value, targetVersion)

            def createPrevMimaInfoData(ver: ReleaseVersion): PrevMimaInput.Data = PrevMimaInput.Data(
              ver, name.value, organization.value, streams.value, ivyScala.value, ivySbt.value)

            val typePrefix = s"$SemVerPrefix Check type:"

            val gitDriver = new GitDriverImpl((baseDirectory in ThisProject).value)
            gitDriver.branchState match {

              case GitBranchStateTwoReleases(_, headVersion, _, prevVersion) =>
                if (gitDriver.workingState.isDirty) {
                  logger.info(s"$typePrefix comparing most recent release with post-tag changes")
                  val headMimaInfoData = createPrevMimaInfoData(headVersion)
                  performSemVerCheck(headMimaInfoData, currMimaInfo, miMaExecutor, preReleaseEnabled, logger)

                } else {
                  // we use "currVersion" in the logic below and not "headVersion" because "currVersion" can be different
                  // than "headVersion" when using version.override
                  logger.info(s"$typePrefix comparing two most recent releases (tag before publish scenario)")
                  val prevMimaInfoData = createPrevMimaInfoData(prevVersion)
                  performSemVerCheck(prevMimaInfoData, currMimaInfo, miMaExecutor, preReleaseEnabled, logger)
                }

              case GitBranchStateOneReleaseNotHead(_, _, prevVersion) =>
                logger.info(s"$typePrefix comparing most recent release with post-tag changes")
                val prevMimaInfoData = createPrevMimaInfoData(prevVersion)
                performSemVerCheck(prevMimaInfoData, currMimaInfo, miMaExecutor, preReleaseEnabled, logger)

              case GitBranchStateOneReleaseHead(_, headVersion) =>
                if (gitDriver.workingState.isDirty) {
                  logger.info(s"$typePrefix comparing most recent release with post-tag changes")
                  val prevMimaInfoData = createPrevMimaInfoData(headVersion)
                  performSemVerCheck(prevMimaInfoData, currMimaInfo, miMaExecutor, preReleaseEnabled, logger)
                } else {
                  logger.info(s"$abortPrefix one previous release, no extra commits, git is clean $noViolationsSuffix")
                }

              case GitBranchStateNoReleases(_) =>
                logger.info(s"$abortPrefix no previous release $noViolationsSuffix")

              case GitBranchStateNoCommits =>
                logger.info(s"$abortPrefix no commits $noViolationsSuffix")
            }

          case None =>
          // MimaKeys.mimaCurrentClassfiles has given us a path (something like "target/scala-2.11/classes") that
          // does not actually exist. this is typically a root project or an empty project. we're skipping it.
        }
      }
    }
  }

  /** Creates the [[MiMaExecutor]] */
  private def createMiMaExecutor(classPath: Classpath, taskStreams: TaskStreams): MiMaExecutor = {
    val javaClassPath = {
      // from SbtMima.makeMima
      val classPathString = classPath
        .map(attributedFile => attributedFile.data.getAbsolutePath)
        .mkString(System.getProperty("path.separator"))
      new JavaClassPath(DefaultJavaContext.classesInPath(classPathString).toIndexedSeq, DefaultJavaContext)
    }
    new CustomMiMaLibMiMaExecutor(javaClassPath)
  }

  /**
    * Figures out the version we will be using for SemVer checking, the "target" version. The target version either:
    * - [[versionOverride]] (if available) is the version the developer is trying to release.
    * - [[versionFromGit]] (if a clean release) is the version of the actual code.
    * - [[semVerLimit]] (if available) the version the developer wants to stay under.
    * If none of these are available then SemVer checking will be disabled.
    *
    * Why do we not consider [[version]]? The [[version]] is not necessarily what the developer wants -- it is either
    * [[versionOverride]] (which is the version the developer wants to release) or [[versionFromGit]] (which is either
    * the last release version, or a version that [[GitVersioningPlugin]] makes up so it doesn't conflict with
    * the last release version).
    *
    * @param maybeVersionOverrideStr From [[versionOverride]]
    * @param versionLimitStr From [[semVerLimit]]
    * @param versionFromGit From [[versionFromGit]] from [[GitDriver.calcCurrentVersion()]] -- NOT [[version]] since
    * could be either [[versionOverride]] or [[versionFromGit]]
    * @return The version we will target for the SemVer comparison, or None to disable SemVer checking.
    * @throws IllegalArgumentException If the inputs are invalid with relation to each other, see
    * [[checkTargetVersionComponents()]].
    */
  def calcTargetVersion(
    maybeVersionOverrideStr: Option[String], versionLimitStr: String, versionFromGit: SemanticVersion, logger: Logger
  ): Option[SemVerTargetVersion] = {

    val maybeVerOverride: Option[ReleaseVersion] =
      maybeVersionOverrideStr.map(parseAsCleanOrThrow)
    val maybeVerLimit: Option[ReleaseVersion] =
      Option(versionLimitStr).map(_.trim).filter(_.nonEmpty).map(parseAsCleanOrThrow)

    // first do the sanity / configuration checks
    checkTargetVersionComponents(maybeVerOverride, maybeVerLimit, versionFromGit)

    // second, if we got here the checks passed (i.e. no exception was thrown above)
    (maybeVerOverride, maybeVerLimit, versionFromGit) match {

      // versionOverride has the highest precedence
      case (Some(verOverride), _, _) =>
        logger.info(s"$SemVerPrefix Checking ENABLED with SPECIFIC target versionOverride=$verOverride")
        Some(SemVerSpecificTargetVersion(verOverride))

      // clean, release versionFromGit has the second highest precedence because it's the actual state of the code
      case (None, _, verFromGit: ReleaseVersion) if !verFromGit.isDirty =>
        logger.info(s"$SemVerPrefix Checking ENABLED with SPECIFIC target versionFromGit=$versionFromGit")
        Some(SemVerSpecificTargetVersion(verFromGit))

      // semVerLimit has the third highest precedence (limit < specific)
      case (None, Some(verLimit), _) =>
        logger.info(s"$SemVerPrefix Checking ENABLED with LIMIT target semVerLimit=$verLimit")
        Some(SemVerLimitTargetVersion(verLimit))

      case (None, None, _) =>
        logger.info(
          s"$SemVerPrefix Checking DISABLED with empty semVerLimit, empty versionOverride, and dirty and/or" +
            " SNAPSHOT versionFromGit")
        None
    }
  }

  private def parseAsCleanOrThrow(value: String): ReleaseVersion = {
    ReleaseVersion.unapply(value)
      .filter(!_.isDirty)
      .getOrElse(
        throw new IllegalArgumentException(s"$ConfigErrorPrefix cannot parse value=$value as clean release version"))
  }

  /**
    * This checks that the arguments to [[calcTargetVersion()]] are valid with relation to each other. Each of
    * these inputs are defined separately, some by humans, and humans (and programs written by humans) make mistakes.
    * This ensures that we are in a valid state before moving forward, that the user or computer didn't come up with
    * a nonsensical situation.
    *
    * @param maybeVersionOverride From [[versionOverride]]
    * @param maybeVersionLimit From [[semVerLimit]]
    * @param versionFromGit From [[versionFromGit]] from [[GitDriver.calcCurrentVersion()]] -- NOT [[version]] since
    * could be either [[versionOverride]] or [[versionFromGit]]
    * @return The version we will target for the SemVer comparison, or None to disable SemVer checking.
    * @throws IllegalArgumentException If the inputs are invalid with relation to each other
    */
  private def checkTargetVersionComponents(
    maybeVersionOverride: Option[ReleaseVersion],
    maybeVersionLimit: Option[ReleaseVersion],
    versionFromGit: SemanticVersion
  ): Unit = {

    // Implementation note: You will see this snippet several times
    //   ... verGit: SnapshotVersion) if <variable> <comparison> verGit.undoNextVersion().toRelease =>
    // which says "if git is on a snapshot, strip off the snapshot stuff for a cleaner comparison".
    // We have to do this because a snapshot means untagged commits after a release, and depending on what <variable>
    // and <comparison> are we might not be able to allow that

    // first do the sanity / configuration checks
    Option((maybeVersionOverride, maybeVersionLimit, versionFromGit)).collect {

      case (Some(verOverride), Some(verLimit), _) if verOverride > verLimit =>
        s"semVerLimit=$verLimit cannot be less than versionOverride=$verOverride;" +
          " your limit wasn't updated after the last release, and would likely prevent ANY new changes"

      // this is most likely a user error -- you are forcing a version with uncommitted changes. you typically force
      // a version to make a RELEASE. without a use case to allow this I'm treating it as an error
      case (Some(verOverride), _, verGit) if verGit.isDirty =>
        s"versionOverride=$verOverride but version=$versionFromGit is dirty;" +
          " publishing dirty versions (using versionOverride) defeats repeatable builds"

      // we do NOT check EQUAL here because its okay if versionOverride matches the versionFromGit: it usually means
      // the developer tagged FIRST and ran the release job SECOND. That's a valid order of operations -- the developer
      // is still publishing a clean, tagged, release. (Unfortunately we are forced to ignore the possibility of
      // spoofing the release -- remove the tag from one commit, tag a different commit, re-release.)
      case (Some(verOverride), _, verGit: ReleaseVersion) if verOverride < verGit =>
        s"versionOverride=$verOverride cannot be less than RELEASE version=$verGit;" +
          " cannot create new releases less than previously released version"

      // we do check EQUAL here because if they are equal (after converting to a release) that means the developer is
      // trying to release a snapshot that comes AFTER the release as the release
      case (Some(verOverride), _, verGit: SnapshotVersion) if verOverride <= verGit.undoNextVersion().toRelease =>
        s"versionOverride=$verOverride cannot be less than or equal to SNAPSHOT version=$verGit;" +
          " cannot create new release with un-tagged commits that are less previously released version"

      // we do NOT check EQUAL here because its okay if semVerLimit matches the versionFromGit: it usually means
      // the developer tagged FIRST and updated the limit. That's a valid order of operations -- SemVer will still
      // catch if new changes are made beyond that tag
      case (_, Some(verLimit), verGit: ReleaseVersion) if verLimit < verGit =>
        s"semVerLimit=$verLimit cannot be less than or equal to version=$verGit;" +
          " your limit prevents new ANY changes, update it to something greater than the last release"

      // Same as above, but we have to fix the version due to the incrementing done by GitDriver.calcCurrentVersion()
      case (_, Some(verLimit), verGit: SnapshotVersion) if verLimit <= verGit.undoNextVersion().toRelease =>
        s"semVerLimit=$verLimit cannot be less than or equal version=$verGit;" +
          " your limit prevents new ANY changes, update it to something greater than the last release"

    }.foreach {
      msg => throw new IllegalArgumentException(s"$ConfigErrorPrefix $msg")
    }
  }

  private def performSemVerCheck(
    prev: PrevMimaInput.Data,
    curr: CurrMimaInput,
    miMaExecutor: MiMaExecutor,
    preReleaseEnabled: Boolean,
    logger: Logger
  ): Unit = {

    logger.info(s"$SemVerPrefix Check starting (prev=${prev.version} vs curr=${curr.target.version}) ...")

    val preHalter = new PreCheckSemVerHalter(prev.version, curr.target, preReleaseEnabled)
    preHalter.log(logger)
    preHalter.haltBuild()

    // Don't waste time running the SemVer check if this is a major change. A major change allows anything.
    // This "optimization" has an important functional aspect: if you renamed the artifact, say from "foo" to
    // "bar", this check will avoid trying to fetch the old major version of "bar" (which doesn't exist).
    // Also, when the artifact was renamed the packages should have been renamed as well, and that renaming
    // would be a major change, so we're safe to skip the SemVer check.
    val delta = SemVerDiff.calc(prev.version, curr.target).delta
    if (delta.major > 0) {

      val logMessage = curr.target match {
        case SemVerLimitTargetVersion(v) => s"limit=$v allows major upgrades, no check needed."
        case SemVerSpecificTargetVersion(v) => s"curr=$v is a major upgrade, no check needed."
      }
      logger.info(s"$abortPrefix $logMessage")

    } else {

      val prevMimaInput = PrevMimaInput.create(prev)
      val result = compare(prevMimaInput, curr, miMaExecutor)

      val postHalter = new PostCheckSemVerHalter(result, preReleaseEnabled)
      postHalter.log(logger)
      postHalter.haltBuild()
    }
  }

  /**
    * Compares two [[MimaInput]]s and returns whether the changes are legal given the [[MimaInput]]s. For
    * example, if only [[SemanticVersion.minor]] is incremented then only new members can be added; no members can be
    * removed.
    *
    * @param prev Information for the previously released incarnation of this project.
    * @param curr Information for the current to-be-released project.
    * @param miMaExecutor The class that does the MiMa check.
    */
  def compare(prev: PrevMimaInput, curr: CurrMimaInput, miMaExecutor: MiMaExecutor): SemVerResult = {

    require(
      prev.version < curr.target.version,
      s"prev.version=${prev.version} cannot be equal or greater than curr.version=${curr.target.version}")
    require(prev.path != curr.path, s"prev.path=${prev.path} cannot equal curr.path=${curr.path}")

    val result = new SemVerResultBuilder(prev.version, curr.target)

    val problemsBackwards: List[Problem] = miMaExecutor.backwardProblems(prev.path, curr.path)
    val problemsForwards: List[Problem] = miMaExecutor.forwardProblems(prev.path, curr.path)

    problemsBackwards.foreach(problem => result.backward(problem.description("current")))
    problemsForwards.foreach(problem => result.forward(problem.description("previous")))

    result.build()
  }
}
