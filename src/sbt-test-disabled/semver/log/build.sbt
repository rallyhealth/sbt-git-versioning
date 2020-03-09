import sbt.complete.DefaultParsers._

scalaVersion := "2.11.12"

organization := "com.rallyhealth.test.scripted"

scalacOptions ++= Seq("-Xfatal-warnings", "-Xlint")

libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.1" % "test"

publish := {}

logLevel := sbt.Level.Info

enablePlugins(SemVerPlugin)

val utils = com.rallyhealth.scripted.ScriptedUtils

initialize := {
  utils.initialize()
  initialize.value
}

// Fundamental tasks
lazy val startTest = inputKey[Unit]("Marks the start of a test")
lazy val applyChange = inputKey[Unit]("Applies a change to the source: major, minor, patch, or original")
lazy val grepLog = inputKey[Unit]("Greps the scripted log for the given pattern")
lazy val searchLog = inputKey[Unit]("Searches the scripted log for the given phrase")
lazy val listTestNames = inputKey[Unit]("Prints out the name of all tests run")

startTest := utils.startTest(streams.value.log, spaceDelimited("<name>").parsed)

applyChange := utils.applyChange(streams.value.log, spaceDelimited("<changeType>").parsed)

grepLog := utils.grepLog(streams.value.log, spaceDelimited("[not] <regex>").parsed)

searchLog := utils.searchLog(streams.value.log, spaceDelimited("[not] <value>").parsed)

listTestNames := {
  spaceDelimited("").parsed
  utils.listTestNames(streams.value.log)
}

// Compound tasks
lazy val assertConfigError = inputKey[Unit]("Searches for messages that indicate a config problem")
lazy val assertDisabled = inputKey[Unit]("Searches for messages that indicate SemVer was disabled before running")
lazy val assertAborted = inputKey[Unit]("Searches for messages that indicate SemVer was aborted before running")
lazy val assertPassed = inputKey[Unit]("Searches for messages that indicate SemVer ran and passed")
lazy val assertFailed = inputKey[Unit]("Searches for messages that indicate SemVer ran but failed")

assertConfigError := {
  spaceDelimited("").parsed
  utils.assertConfigError(streams.value.log)
}
assertAborted := {
  spaceDelimited("").parsed
  utils.assertAborted(streams.value.log)
}
assertDisabled := {
  spaceDelimited("").parsed
  utils.assertDisabled(streams.value.log)
}
assertPassed := utils.assertPassed(streams.value.log, spaceDelimited("<changeType>").parsed)
assertFailed := utils.assertFailed(streams.value.log, spaceDelimited("<changeType>").parsed)
