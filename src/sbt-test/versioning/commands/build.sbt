import sbt.complete.DefaultParsers._

import scala.io.Source

organization := "com.rallyhealth.test.scripted"

scalaVersion := "2.11.12"

scalacOptions ++= Seq("-Xfatal-warnings", "-Xlint")

publish := {}

logLevel := sbt.Level.Info

lazy val assertVersion = inputKey[Unit]("Checks that the version matches the expected value.")
lazy val checkVersionFile = taskKey[Unit]("Checks that version.txt contains the correct version.")

/**
  * Checks that the [[version]] matches the provided pattern. The pattern looks like
  * {{{
  * 0.0.1-1-<hash>-SNAPSHOT
  * }}}
  * where `<hash>` is replaced with `[0-9a-f]{7,}` before comparing.
  */
assertVersion := {
  // Replace the hash with a stable placeholder so we can do assertions against it.
  val actual = version.value.replaceAll("[0-9a-f]{7,}", "<hash>")
  val expected = spaceDelimited("<arg>").parsed.head

  assert(expected == actual, s"expected: $expected actual: $actual")
}

checkVersionFile := {
  val expected = version.value
  val actual = Source.fromFile("version.txt").mkString
  assert(expected == actual, s"expected: $expected actual: $actual")
}
