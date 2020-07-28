package com.rallyhealth.sbt.semver

import com.rallyhealth.sbt.semver.mima.MiMaResult
import com.rallyhealth.sbt.versioning.SemVerReleaseType._
import com.rallyhealth.sbt.versioning.{ReleaseVersion, SemVerReleaseType}

/**
  * MiMa does not agree with your attempted [[SemVerReleaseType]].
  */
class SemVerVersionTooLowException(
  val prev: ReleaseVersion,
  val curr: ReleaseVersion,
  val attempted: SemVerReleaseType,
  val miMaResult: MiMaResult
) extends Exception(
  {
    val maybeNewFunctionality = miMaResult.forward.headOption.map(_ => "new functionality").toList
    val maybeBreakingChanges = miMaResult.backward.headOption.map(_ => "binary incompatibilites").toList
    val violations = maybeNewFunctionality ++ maybeBreakingChanges
    val requiredVersion = prev.release(miMaResult.releaseType)

    val problems = miMaResult.problemsFor(attempted)
    val problemsString = problems
      // sort by direction-message for consistency
      .sortBy(x => s"${x.direction}-${x.message}")
      .zipWithIndex
      .map {
        case (msg, index) =>
          val size = problems.size
          val direction = msg.direction.toString.capitalize
          s"(${index + 1}/$size) $direction -> ${msg.message}"
      }
      .mkString(System.lineSeparator)

    s"""Your changes have ${violations.mkString(" and ")} which violates the http://semver.org rules for a $attempted release.
       |
       |Specifically, MiMa detected the following binary incompatibilities:
       |$problemsString
       |
       |These changes would require a ${miMaResult.releaseType} release instead (${prev} => ${requiredVersion}).
       |You can adjust the version by adding the following setting:
       |  gitVersioningSnapshotLowerBound in ThisBuild := "${requiredVersion}"""".stripMargin
  }
)
