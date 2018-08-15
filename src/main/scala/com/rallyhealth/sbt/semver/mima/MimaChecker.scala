package com.rallyhealth.sbt.semver.mima

import com.rallyhealth.sbt.versioning.SemVerReleaseType
import sbt.File

class MimaChecker(miMaExecutor: MiMaExecutor) {

  def compare(prev: File, curr: File): MiMaResult = {
    require(prev != curr, s"prev=$prev cannot equal curr=$curr")

    val problemsBackwards = miMaExecutor.backwardProblems(prev, curr)
    val problemsForwards = miMaExecutor.forwardProblems(prev, curr)

    val backwards = problemsBackwards.map(problem => MiMaProblem(Direction.Backward, problem.description("current")))
    val forwards = problemsForwards.map(problem => MiMaProblem(Direction.Forward, problem.description("previous")))

    MiMaResult(backwards, forwards)
  }
}

/** Indicates whether the error is a backwards or forwards compatible error */
object Direction extends scala.Enumeration {

  val Backward = Value("backward")
  val Forward = Value("forward")
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
