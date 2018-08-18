package com.rallyhealth.sbt.semver

import java.io.File

import com.rallyhealth.sbt.semver.mima.MiMaExecutor
import com.typesafe.tools.mima.core.Problem

class MockMiMaExecutor(backwardProblems: List[Problem], forwardProblems: List[Problem]) extends MiMaExecutor {

  override def backwardProblems(oldDir: File, newDir: File): List[Problem] = backwardProblems

  override def forwardProblems(oldDir: File, newDir: File): List[Problem] = forwardProblems
}
