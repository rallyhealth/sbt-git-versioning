import sbt.plugins.SbtPlugin

name := "sbt-git-versioning"
organizationName := "Rally Health"
organization := "com.rallyhealth.sbt"

licenses := Seq("MIT" -> url("http://opensource.org/licenses/MIT"))

bintrayOrganization := Some("rallyhealth")
bintrayRepository := "sbt-plugins"

enablePlugins(SbtPlugin)

scalacOptions ++= {
  val linting = CrossVersion.partialVersion(scalaVersion.value) match {
    // some imports are necessary for compat with 2.10.
    // 2.12 needs to chill out with the unused imports warnings.
    case Some((2, 12)) => "-Xlint:-unused,_"
    case _ => "-Xlint"
  }
  Seq("-Xfatal-warnings", linting)
}

sbtVersion := "1.4.4"

publishMavenStyle := false

resolvers += Resolver.bintrayRepo("typesafe", "sbt-plugins")

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.2.3" % Test,
  "se.sawano.java" % "alphanumeric-comparator" % "1.4.1"
)

// you need to enable the plugin HERE to depend on code in the plugin JAR. you can't add it as part of
// libraryDependencies nor put it in plugins.sbt
// We use MiMa 0.3.0 because it is the only version that exists for both SBT 1.2.x and 0.13.x
// Also MiMa 0.6.x has some source incompatible changes, so we'd have to fork the source to support 0.6.x and 0.3.x
// https://github.com/lightbend/mima#usage
addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "0.8.1")
addSbtPlugin("com.dwijnand" % "sbt-compat" % "1.2.6")

// disable scaladoc generation
Compile / doc / sources := Seq.empty

publishArtifact in packageDoc := false
