scalaVersion in ThisBuild := "2.11.12"

organization in ThisBuild := "com.rallyhealth.test.scripted"

logLevel := sbt.Level.Info

enablePlugins(SemVerPlugin)

lazy val root = (project in file("."))
  .aggregate(foo, bar)
  .settings(publish := {})
  .settings(publishLocal := {})

lazy val foo = project
  .enablePlugins(SemVerPlugin)

lazy val bar = project
  .enablePlugins(SemVerPlugin)
