package com.rallyhealth.sbt.semver

import com.rallyhealth.sbt.semver.level._
import com.rallyhealth.sbt.semver.level.rule._
import com.rallyhealth.sbt.semver.mima._
import com.rallyhealth.sbt.versioning.GitVersioningPlugin.autoImport.semanticVersion
import com.rallyhealth.sbt.versioning.{ReleaseVersion, SemVerReleaseType, SemanticVersion}
import com.typesafe.tools.mima.plugin.MimaPlugin.autoImport._
import com.typesafe.tools.mima.plugin.{MimaKeys, MimaPlugin}
import sbt.Keys._
import sbt._
import sbt.compat._  // Required import for sbt 0.13.x support
import sbt.plugins.JvmPlugin

/**
  * Allows enforcing proper semantic versioning of classes using the SBT MiMa plugin
  * [[https://github.com/typesafehub/migration-manager]], so we can avoid publishing new artifact (JAR) versions that
  * do not reflect the rules of semantic versioning -- e.g. not releasing a patch change. when its actually supposed to
  * be a minor change. The MiMa plugin checks the API changes, not JVM specific changes -- it cares about
  * adding/removing/changing PUBLIC and PROTECTED classes, fields, methods, etc.
  */
object SemVerPlugin extends AutoPlugin {

  /**
    * Explicitly declare (for documentation reasons) that SemVerPlugin is opt-in with .enablePlugins(SemVerPlugin).
    *
    * It should only be enabled for code that's shared. Doesn't make any sense to do it on a web-app or internal business logic.
    */
  override def trigger: PluginTrigger = noTrigger

  /**
    * We hook compile, test, & publish which can only happen after the Defaults are set by the JvmPlugin.
    * Otherwise, our hooks will get overridden.
    *
    * See [[http://stackoverflow.com/a/25251194]]
    */
  override def requires: Plugins = JvmPlugin && MimaPlugin

  object autoImport {

    /**
      * We include "equal" in the check because if someone wants to exceed the limit that change should be in their
      * PR so people can review and accept/reject it. If we didn't have equal then it wouldn't be as obvious during
      * review time, and the next person would have to clean up and update the limit.
      *
      * This is really a setting, but settings are checked at startup so if any are missing or throw exceptions SBT
      * fails to load. But tasks won't be checked until they are actually used, so its like a lazy setting.
      */
    lazy val semVerLimit: TaskKey[String] = taskKey[String](
      "'semVerLimit' is a threshold Release version, it is a version you do NOT want to exceed with these" +
        " changes. If non-empty the `semVerCheck` will ensure that your changes do not require a release EQUAL or" +
        " GREATER than this version. (no default value)")

    lazy val semVerEnforceAfterVersion: SettingKey[Option[String]] = settingKey[Option[String]](
      "'semVerEnforceAfterVersion' tells SemVer to only check if the current version is GREATER than this" +
        " Release version. This flag is necessary only in rare cases -- usually when you need to add a new artifact " +
        " where one did not exist previous. (default=None)")

    lazy val semVerCheck: TaskKey[Unit] = taskKey[Unit](
      "'semVerCheck' manually checks SemVer on this project (ignores 'semVerPreRelease')")

    lazy val semVerCheckOnCompile: SettingKey[Boolean] = settingKey[Boolean](
      "'semVerCheckOnCompile' enables checking SemVer on the 'compile' task. (default=true)")

    lazy val semVerCheckOnTest: SettingKey[Boolean] = settingKey[Boolean](
      "'semVerCheckOnTest' enables checking SemVer on the 'test' task. (default=true)")

    lazy val semVerCheckOnPublish: SettingKey[Boolean] = settingKey[Boolean](
      "'semVerCheckOnPublish' enables checking SemVer on the 'publish' task. (default=true)")

    lazy val semVerEnforcementLevelRules: TaskKey[Seq[SemVerLevelRule]] = taskKey[Seq[SemVerLevelRule]]("Defines the level of acceptable changes that should be enforced.")
    lazy val semVerEnforcementLevel: TaskKey[SemVerReleaseType] = taskKey[SemVerReleaseType]("The level of acceptable changes.")
    lazy val semVerCheckValidVersion: TaskKey[Unit] = taskKey[Unit]("Checks that the version is valid")
    // This Plugin does not directly support problem filters that are in MiMa, see "problemFilter" field in
    // https://github.com/typesafehub/migration-manager/blob/master/reporter/src/main/scala/com/typesafe/tools/mima/cli/Main.scala
    // Currently all the Problem analyzers are useful so at the time of writing we had no need for filtering
  }

  import autoImport._

  lazy val semVerVersion: SettingKey[SemanticVersion] = settingKey[SemanticVersion](
    "'version' settingKey parsed as a SemanticVersion."
  )

  private[semver] object MiMa {

    lazy val miMaChecker: TaskKey[MimaChecker] = taskKey[MimaChecker]("Thin wrapper around typesafe's Migration Manager.")
    lazy val maybePrevRelease: TaskKey[Option[ReleaseVersion]] = taskKey[Option[ReleaseVersion]]("A previous release for MiMa to compare against, if any.")
    lazy val maybePrevInput: TaskKey[Option[MiMaInput]] = taskKey[Option[MiMaInput]]("The previous artifact for MiMa to compare against, if any.")
    lazy val moduleResolver: TaskKey[ModuleResolver] = taskKey[ModuleResolver]("Resolves/Downloads an artifact, returning the location on the filesystem.")
  }

  override def buildSettings: Seq[Setting[_]] = Seq(
    // we do not want MiMa controlling failure
    mimaFailOnProblem := false,

    semVerEnforceAfterVersion := None,

    semVerVersion := semanticVersion.value,

    semVerLimit := "deprecated"
  )

  override lazy val projectSettings: Seq[Setting[_]] = Seq(

    semVerCheckOnCompile := true,

    semVerCheckOnTest := true,

    semVerCheckOnPublish := true,

    semVerCheck := {
      SemVerMiMaTasks.semVerCheckMiMa
        .dependsOn(semVerCheckValidVersion).value
    },

    semVerCheck := {
      // Wrap the real implementation with a deprecation check.
      val log = streams.value.log
      if (semVerLimit.value != "deprecated") {
        log.warn(
          """'semVerLimit' is DEPRECATED and will be ignored.
            |
            |If you want to raise the version prior to a release, instead use:
            |gitVersioningSnapshotLowerBound := "x.y.z"""".stripMargin
        )
      }
      semVerCheck.value
    },

    /*
     * WARNING to anyone who might think "This doesn't need Def.taskDyn {}" YES IT DOES -- I've written this at least a
     * dozen different ways trying to remove Def.taskDyn {} and it doesn't work. This might compile, it might run, but
     * it won't run correctly in ALL cases. This means it might run semVerCheck() more than once, which sucks, but that's
     * FAAAR better than not running at all. If you do change this YOU MUST RUN sbt-git-versioning-test/checkSemver.sh
     * because that will check all the different scenarios.
     *
     * I use Def.taskDyn {} here because otherwise the conditional actually doesn't do anything. Even though it looks
     * like a method its actually macro trickery that runs everything in parallel. The Def.taskDyn() prevents that, but
     * its very finicky See
     * - http://www.scala-sbt.org/0.13/docs/Howto-Dynamic-Task.html
     * - http://stackoverflow.com/a/29384375/11889
     *
     * The only way I can think of to truly eliminate Def.taskDyn {} is to eliminate the "OnXXX" flags above. Which
     * might be a good idea anyways...
     */

    compile := {
      val result = (compile in Compile).value

      // ensure that SemVer checking happens AFTER compilation
      Def.taskDyn {
        if (semVerCheckOnCompile.value) semVerCheck
        else Def.task {}
      }.value

      result
    },

    // Test is overridden because I can't override "(compile in Test") because I found that changing the compile
    // override above to "(compile in Compile)" causes SBT to hang. Since I can't disambiguate "compile" I'm left with
    // overriding "test" directly.
    test := {
      (test in Test).value

      // ensure that SemVer checking happens AFTER testing
      Def.taskDyn {
        if (semVerCheckOnTest.value) semVerCheck
        else Def.task {}
      }.value
    },

    publish := {
      // ensure that SemVer checking happens BEFORE publishing by using dependsOn()
      publish.dependsOn(createSemVerTaskOnPublish()).value
    },

    publishLocal := {
      // ensure that SemVer checking happens BEFORE publishing by using dependsOn()
      publishLocal.dependsOn(createSemVerTaskOnPublish()).value
    },

    semVerEnforcementLevelRules := {
      val current: SemanticVersion = semVerVersion.value

      Seq(
        InitialDevelopmentRule(current),
        EnforceAfterVersionRule(current, semVerEnforceAfterVersion.value.map(ReleaseVersion.parseAsCleanOrThrow)),
        VersionDiffRule(current, MiMa.maybePrevRelease.value)
      )
    },

    semVerEnforcementLevel := {
      semVerEnforcementLevelRules.value.view.flatMap(_.calcLevel()).head.releaseType
    },

    MiMa.miMaChecker := {
      val classpath = (fullClasspath in MimaKeys.mimaFindBinaryIssues).value
      new MimaChecker(MiMaExecutor(classpath, streams.value), MimaKeys.mimaBinaryIssueFilters.value)
    },

    MiMa.moduleResolver := new IvyModuleResolver(scalaModuleInfo.value, ivySbt.value, streams.value),

    MiMa.maybePrevRelease := SemVerTasks.prevRelease.value,

    MiMa.maybePrevInput := {
      val mimaModuleResolver = MiMa.moduleResolver.value

      MiMa.maybePrevRelease.value.map { relVer =>
        val moduleID = organization.value %% name.value % relVer.toString
        val file = mimaModuleResolver.resolve(moduleID)
        MiMaInput(file, relVer)
      }
    },
    semVerCheckValidVersion := {
      MiMa.maybePrevRelease.value.foreach { prev =>
        if (semVerVersion.value <= prev) {
          throw new VersionDowngradeException(semVerVersion.value, prev)
        }
      }
    }
  )

  /**
    * We have to use Def.taskDyn {} otherwise the conditional actually doesn't do anything, it will run all the tasks
    * in parallel, because while this looks like a method its actually some weird macro trickery.
    * See http://stackoverflow.com/a/29384375/11889
    */
  private def createSemVerTaskOnPublish(): Def.Initialize[Task[Unit]] =
    Def.taskDyn {
      if (semVerCheckOnPublish.value) semVerCheck
      else Def.task {}
    }
}
