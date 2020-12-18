package com.rallyhealth.sbt.semver.mima

import com.rallyhealth.sbt.versioning.SemVerReleaseType
import com.typesafe.tools.mima.core.{Problem, ProblemFilter}
import com.typesafe.tools.mima.plugin.SbtMima
import sbt.{File, Keys}
import sbt.util.Logger

class MimaChecker(
  classpath: Keys.Classpath,
  scalaVersion: String,
  filters: Seq[ProblemFilter],
  logger: Logger
) {

  private def applyFilter(problem: Problem): Boolean = filters.forall(_(problem))

  def compare(prev: File, curr: File): MiMaResult = {
    require(prev != curr, s"prev=$prev cannot equal curr=$curr")
    val (problemsBackwards, problemsForwards) = SbtMima.runMima(prev, curr, classpath, "both", scalaVersion, logger)

    val filteredProblemsBackwards = problemsBackwards.filter(applyFilter)
    val filteredProblemsForwards = problemsForwards.filter(applyFilter)

    val backwards = filteredProblemsBackwards.map(problem => MiMaProblem(Direction.Backward, problem.description("current")))
    val forwards = filteredProblemsForwards.map(problem => MiMaProblem(Direction.Forward, problem.description("previous")))

    MiMaResult(backwards, forwards)
  }
}

/** Indicates whether the error is a backwards or forwards compatible error */
object Direction extends scala.Enumeration {

  final val Backward = Value("backward")
  final val Forward = Value("forward")
}

case class MiMaProblem(direction: Direction.Value, message: String)

case class MiMaResult(backward: List[MiMaProblem], forward: List[MiMaProblem]) {

  def problemsFor(releaseType: SemVerReleaseType): List[MiMaProblem] = {
    releaseType match {
      case SemVerReleaseType.Major => Nil
      case SemVerReleaseType.Minor => backward
      case SemVerReleaseType.Patch => backward ++ forward
    }
  }

  def releaseType: SemVerReleaseType = {
    if (backward.nonEmpty) SemVerReleaseType.Major
    else if (forward.nonEmpty) SemVerReleaseType.Minor
    else SemVerReleaseType.Patch
  }
}
