import sbt.complete.DefaultParsers._

organization := "com.rallyhealth.test.scripted"

scalaVersion := "2.11.8"

scalacOptions ++= Seq("-Xfatal-warnings", "-Xlint")

publish := {}

logLevel := sbt.Level.Info

lazy val assertVersion = inputKey[Unit]("Checks that the version matches the expected value.")
lazy val setRelease = inputKey[Unit]("Sets the release property to the specified value")
lazy val clearRelease = inputKey[Unit]("Removes the release property")

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
setRelease := {
  val value = spaceDelimited("<arg>").parsed.head
  sys.props += "release" -> value
}

clearRelease := {
  sys.props -= "release"
}
