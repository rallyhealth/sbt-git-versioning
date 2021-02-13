package com.rallyhealth.sbt.versioning

import java.io.File

import com.rallyhealth.sbt.versioning.GitFetcher.FetchResult
import com.rallyhealth.sbt.versioning.LowerBoundedSemanticVersion._
import sbt.Keys._
import sbt._
import sbt.complete.DefaultParsers._
import sbt.plugins.JvmPlugin

import scala.concurrent.duration._

/**
  * Enforces Semantic Version plus support for identifying "x.y.z-SNAPSHOT" and "x.y.z-dirty-SNAPSHOT" builds.
  */
object GitVersioningPlugin extends AutoPlugin {

  override def trigger: PluginTrigger = allRequirements

  /**
    * We hook test & publish which can only happen after the Defaults are set by the JvmPlugin.
    * Otherwise, our hooks will get overridden.
    *
    * See [[http://stackoverflow.com/a/25251194]]
    */
  override def requires: Plugins = JvmPlugin

  object autoImport {

    lazy val autoFetch: SettingKey[Boolean] = settingKey[Boolean](
      "`autoFetch` indicates whether to auto-fetch tags from remotes")

    lazy val autoFetchResult: SettingKey[Seq[FetchResult]] = settingKey[Seq[FetchResult]](
      "`autoFetchResult` is the result of the auto-fetch")

    lazy val autoFetchRemotes: SettingKey[Seq[String]] = settingKey[Seq[String]](
      "`autoFetchTags` are the names of git remotes to fetch tags from with `autoFetch` is true")

    lazy val autoFetchTimeout: SettingKey[Int] = settingKey[Int](
      "`autoFetchTimeout` is the timeout for git auto-fetching (in seconds)")

    /** Note this can be overriden by [[ignoreDirty]]. */
    lazy val isCleanRelease: SettingKey[Boolean] = settingKey[Boolean](
      "`isCleanRelease` is whether the 'version' is a clean release or not (NOT based on the working dir)")

    /** See [[GitDriver.calcCurrentVersion()]] */
    lazy val versionFromGit: SettingKey[SemanticVersion] = settingKey[SemanticVersion](
      "`versionFromGit` is The version as determined by git history")

    lazy val semanticVersion: SettingKey[SemanticVersion] = settingKey[SemanticVersion](
      "The typed representation of `version`"
    )

    /**
      * The sbt ecosystem relies on the value of the version setting key as the
      * source of truth for the version we're currently building.
      *
      * GitVersioningPlugin is based on git tags which are often incremented to later versions after CI builds complete.
      *
      * For example, given a most recent tag of v1.5.0, [[GitVersioningPlugin]] at CI time
      * will determine the version to be something like v1.5.1-4-aabcdef-SNAPSHOT.
      *
      * If we have checks (MiMa, SemVerPlugin, ShadingPlugin) that enforce that no major changes are introduced,
      * without correctly versioning as v2.0.0-whatever-SNAPSHOT, then the build would fail, incorrectly.
      *
      * [[gitVersioningSnapshotLowerBound]] offers a way to nudge the candidate version
      * up to v2.0.0-4-aabcdef-SNAPSHOT without tagging:
      *
      * {{{
      *   gitVersioningSnapshotLowerBound := "2.0.0"
      * }}}
      *
      * [[gitVersioningSnapshotLowerBound]] acts as a lower bound and has no effect
      * when its value is less than [[versionFromGit]].
      *
      * [[gitVersioningSnapshotLowerBound]] has no effect when [[versionOverride]] is present.
      */
    lazy val gitVersioningSnapshotLowerBound: SettingKey[Option[LowerBound]] = settingKey[Option[LowerBound]](
      "Produces snapshot versions whose major.minor.patch is at least this version."
    )

    /**
      * This is used by our Jenkins scripts to set the version before creating and publishing a release. This must
      * be a valid [[SemanticVersion]].
      *
      * The old `versionClassifier` was merged into this because it made the domain model excessively complicated --
      * it was part of the version but carried around separately until some arbitrary point where it was merged into
      * the version.
      */
    lazy val versionOverride: SettingKey[Option[String]] = settingKey[Option[String]](
      "`versionOverride` overrides the automatically determined `version`. This is set (often by a system property)" +
        " to the version you want to release before executing `publish` or `publishLocal`")

    lazy val generateVersion: TaskKey[File] = TaskKey[File](
      "generate-version",
      "Writes the version to a sbt-git-version.properties file.")

    lazy val ignoreDirty: SettingKey[Boolean] = settingKey[Boolean](
      "Forces clean builds, i.e. doesn't add '-dirty' to the version.")

    lazy val printVersion: TaskKey[Unit] = taskKey[Unit](
      "Prints the version that would be applied to this sbt project")

    lazy val gitVersioningMaybeRelease: SettingKey[Option[SemVerReleaseType]] = settingKey[Option[SemVerReleaseType]](
      "Specifies that the version from git should be incremented as a major, minor, or patch release."
    )

    lazy val writeVersion: InputKey[Unit] = inputKey[Unit]("""Writes the version to a file."""")
  }

  import autoImport._

  // These Commands are necessary to prevent the taskKey from being run in EVERY project of multi-project builds.
  lazy val printVersionCommand: Command = Command.command("printVersion") { state =>
    val e = Project.extract(state)
    val (newState, _) = e.runTask(printVersion, state)
    newState
  }

  lazy val writeVersionCommand: Command = Command.single("writeVersion") { (state, args) =>
    val e = Project.extract(state)
    val (newState, _) = e.runInputTask(writeVersion, args, state)
    newState
  }

  lazy val gitDriver: SettingKey[GitDriver] = settingKey[GitDriver](
    "Driver that allows executing common commands against a git working directory.")

  override lazy val buildSettings: Seq[Setting[_]] = Seq(

    // Note the expensive operation are scoped to "ThisBuild" so they are run once-per-project rather than
    // once-per-module

    autoFetch := {
      Option(System.getProperty("version.autoFetch"))
        .orElse(Option(System.getenv("VERSION_AUTOFETCH")))
        .exists(_.toBoolean)
    },

    autoFetchRemotes := Seq("upstream", "origin"),

    autoFetchTimeout := 15, // seconds

    autoFetchResult := {
      implicit val logger: Logger = sLog.value
      if (autoFetch.value) {
        logger.info("Fetching the most up-to-date tags from git remotes")
        GitFetcher.fetchRemotes(autoFetchRemotes.value, autoFetchTimeout.value.seconds)
      } else {
        logger.info(
          "Skipping fetching tags from git remotes; to enable, set the system property version.autoFetch=true")
        Seq.empty[FetchResult]
      }
    },

    gitDriver := new GitDriverImpl(baseDirectory.value),

    versionFromGit := {
      // This depends on but does not use [[autoFetchResult]]; that ensures the task is run but ignores the result.
      (autoFetchResult in ThisProject).value
      val gitVersion = gitDriver.value.calcCurrentVersion(ignoreDirty.value)

      ConsoleLogger().info(s"GitVersioningPlugin set versionFromGit=$gitVersion")

      gitVersion
    },

    versionOverride := {
      Option(System.getProperty("version.override")).map {
        versionOverrideStr =>
          val ver = ReleaseVersion.unapply(versionOverrideStr)
            .filter(!_.isDirty)
            .getOrElse {
              val msg = s"cannot parse versionOverride=$versionOverrideStr as clean release version"
              throw new IllegalArgumentException(msg)
            }

          ConsoleLogger().info(s"GitVersioningPlugin set versionOverride=$versionOverrideStr")
          ver.toString
      }
    },

    gitVersioningMaybeRelease := sys.props.get("release").map(SemVerReleaseType.fromStringOrThrow),

    gitVersioningSnapshotLowerBound := None,

    ignoreDirty := false,

    version := semanticVersion.value.toString,

    semanticVersion := {
      import SemVerReleaseType._
      val log = ConsoleLogger() // A setting cannot depend on a task (like streams.value)

      // version must be semver, see version's definition
      val verOverride = versionOverride.value.map(
        SemanticVersion.fromString(_)
          .getOrElse(throw new IllegalArgumentException(s"cannot parse version=${versionOverride.value}"))
      )

      gitVersioningMaybeRelease.value.foreach { release =>
        log.info(s"GitVersioningPlugin set release=$release")
      }

      gitVersioningSnapshotLowerBound.value.foreach { bound =>
        log.info(s"GitVersioningPlugin set gitVersioningSnapshotLowerBound=$bound")
      }

      lazy val boundedVersionFromGit: SemanticVersion = {
        // 1. Start with version from git.
        // 2. Apply major/minor/patch release.
        // 3. Apply snapshot lower bound.
        type VersionTransform = SemanticVersion => SemanticVersion

        val applyMajorMinorPatchRelease: VersionTransform = ver => gitVersioningMaybeRelease.value
          .foldLeft(ver)(_.release(_))

        val applySnapshotLowerBound: VersionTransform = ver => gitVersioningSnapshotLowerBound.value
          .foldLeft(ver)(_.lowerBound(_, gitDriver.value.branchState))

        val applyAllTransforms = applyMajorMinorPatchRelease andThen applySnapshotLowerBound
        applyAllTransforms(versionFromGit.value)
      }

      val version = verOverride.getOrElse(boundedVersionFromGit)
      log.info(s"GitVersioningPlugin set version=$version")
      version
    },

    isCleanRelease := {
      // version must be semver, see version's definition
      val isDirty = semanticVersion.value.isDirty

      ConsoleLogger().info(s"GitVersioningPlugin set isCleanRelease=${!isDirty}")

      !isDirty
    },

    printVersion := {
      val log = streams.value.log
      log.info(s"Version as determined by git history: ${versionFromGit.value}")
      versionOverride.value.foreach { verOverride =>
        log.info(s"Version as determined by system property (version.override): $verOverride")
      }
      gitVersioningMaybeRelease.value.foreach { relType =>
        log.info(s"Release type: $relType")
      }
      log.success(s"Successfully determined version: ${versionOverride.value.getOrElse(versionFromGit.value)}")
    },

    writeVersion := {
      val file = any.*.map(_.mkString).parsed
      IO.write(new File(file), version.value.getBytes)
    },

    commands ++= Seq(printVersionCommand, writeVersionCommand)
  )

  /**
    * Generates a sbt-git-version.properties file in the managedResources directory suitable for serving version requests during compile.
    */
  override def projectSettings: Seq[Def.Setting[_]] = Seq(
    generateVersion := {
      val log = streams.value.log
      val props = new java.util.Properties()
      props.setProperty("Version", version.value)
      props.setProperty("Name", name.value)
      props.setProperty("Created", System.currentTimeMillis().toString)

      val file = (resourceManaged in Compile).value / "sbt-git-version.properties"
      log.info(s"Writing version file $file")
      IO.write(props, "generated by sbt", file)
      file
    },

    /**
      * Trigger generating the version file on resources.
      */
    resourceGenerators in Compile += generateVersion.map(Seq(_)).taskValue
  )

}
