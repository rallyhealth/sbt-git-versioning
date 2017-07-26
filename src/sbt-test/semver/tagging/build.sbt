scalaVersion in ThisBuild := "2.11.8"

organization in ThisBuild := "com.rallyhealth.test.scripted"

logLevel := sbt.Level.Info

enablePlugins(SemVerPlugin)

semVerLimit := "0.999.0"

semVerPreRelease := true
