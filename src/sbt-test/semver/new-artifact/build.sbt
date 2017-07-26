import sbt.complete.DefaultParsers._

scalaVersion in ThisBuild := "2.11.8"

organization in ThisBuild := "com.rallyhealth.test.scripted"

logLevel := sbt.Level.Info

enablePlugins(SemVerPlugin)

semVerLimit in ThisBuild := "2.999.0"

lazy val root = (project in file("."))
  .aggregate(foo, bar)
  .settings(publish := {})
  .settings(publishLocal := {})

lazy val foo = project
  .enablePlugins(SemVerPlugin)

lazy val bar = project
  .enablePlugins(SemVerPlugin)

val utils = com.rallyhealth.scripted.ScriptedUtils

initialize := {
  utils.initialize()
  initialize.value
}

// Fundamental tasks
lazy val startTest = inputKey[Unit]("Marks the start of a test")
lazy val grepLog = inputKey[Unit]("Greps the scripted log for the given pattern")
lazy val searchLog = inputKey[Unit]("Searches the scripted log for the given phrase")

startTest := utils.startTest(streams.value.log, spaceDelimited("<name>").parsed)
grepLog := utils.grepLog(streams.value.log, spaceDelimited("[not] <regex>").parsed)
searchLog := utils.searchLog(streams.value.log, spaceDelimited("[not] <value>").parsed)


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
