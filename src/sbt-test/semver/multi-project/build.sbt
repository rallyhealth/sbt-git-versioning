scalaVersion in ThisBuild := "2.11.8"

organization in ThisBuild := "com.rallyhealth.test.scripted"

logLevel := sbt.Level.Info

lazy val root = (project in file("."))
  .aggregate(enabled, disabled)
  .settings(publish := {})
  .settings(publishLocal := {})

// The original version of this named the projects "projectSemVerEnabled" and "projectSemVerDisabled" but apparently
// Ivy does not like camel case names, it .toLowers the names, and then SBT looks for "projectSemVerEnabled" and finds
// "projectsemverenabled" and throws:
//
// [info] [error] (projectSemVerEnabled/*:publishLocal) sbt.ResolveException: unresolved dependency:
//   com.rallyhealth.test.scripted#projectSemVerEnabled_2.11;1.0.0: java.text.ParseException: inconsistent module
//   descriptor file found in '/Users/patrick.bohan/.ivy2/local/com.
// rallyhealth.test.scripted / projectSemVerEnabled_2.11 / 1.0.0 / ivys / ivy.xml ': bad
//   module name: expected = 'projectSemVerEnabled_2.11 ' found = 'projectsemverenabled_2.11 ';
//
// So that's why I renamed the projects to simply "enabled" and "disabled".

lazy val disabled = project

lazy val enabled = project.enablePlugins(SemVerPlugin)
