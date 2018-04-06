sbtPlugin := true

name := "sbt-git-versioning"
organizationName := "Rally Health"
organization := "com.rallyhealth.sbt"

enablePlugins(SemVerPlugin)
semVerLimit := "1.0.999"
licenses := Seq("MIT" -> url("http://opensource.org/licenses/MIT"))

bintrayOrganization := Some("rallyhealth")
bintrayRepository := "sbt-plugins"

publishMavenStyle := false

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

