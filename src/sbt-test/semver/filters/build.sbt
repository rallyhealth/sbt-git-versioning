import com.typesafe.tools.mima.core._

scalaVersion in ThisBuild := "2.11.12"

organization in ThisBuild := "com.rallyhealth.test.scripted"

logLevel := sbt.Level.Info

enablePlugins(SemVerPlugin)

mimaBinaryIssueFilters += ProblemFilters.exclude[Problem]("Thing")
