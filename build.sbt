import sbt._

sbtPlugin := true

name := "git-versioning-sbt-plugin"
organizationName := "Rally Health"
organization := "com.rallyhealth.sbt"
version := "0.0.1"
licenses := Seq("MIT" -> url("http://opensource.org/licenses/MIT"))

bintrayOrganization := Some("rallyhealth")
bintrayRepository := "sbt-plugins"

//credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")
publishMavenStyle := false

//resolvers += "Artifactory Libs Releases" at "https://artifacts.werally.in/artifactory/libs-release"
resolvers += Resolver.bintrayRepo("typesafe", "sbt-plugins")

scalacOptions ++= Seq("-Xfatal-warnings", "-Xlint")

val sbtMimaVersion = "0.1.13"

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "2.2.6" % Test,
  // you need this dependency to depend on the real MiMa code (which lives as part of SBT, not the plugin JAR)
  "com.typesafe" %% "mima-core" % sbtMimaVersion,
  "com.typesafe" %% "mima-reporter" % sbtMimaVersion,
  "se.sawano.java" % "alphanumeric-comparator" % "1.4.1"
)

// you need to enable the plugin HERE to depend on code in the plugin JAR. you can't add it as part of
// libraryDependencies nor put it in plugins.sbt
addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % sbtMimaVersion)

// disable scaladoc generation
sources in(Compile, doc) := Seq.empty

publishArtifact in packageDoc := false

