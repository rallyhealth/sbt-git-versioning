import sbt.plugins.SbtPlugin

name := "sbt-git-versioning"
organizationName := "Rally Health"
organization := "com.rallyhealth.sbt"

licenses := Seq("MIT" -> url("http://opensource.org/licenses/MIT"))

bintrayOrganization := Some("rallyhealth")
bintrayRepository := "sbt-plugins"

// SbtPlugin requires sbt 1.2.0+
// See: https://developer.lightbend.com/blog/2018-07-02-sbt-1-2-0/#sbtplugin-for-plugin-development
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

// Uncomment to default to sbt 0.13 for debugging
// sbtVersion in pluginCrossBuild := "0.13.18"
// scalaVersion := "2.10.6"

// We don't use SBT 1.3.x because there isn't a version of MiMa 0.3.0 built for SBT 1.3.x, only for 1.2.x and 0.13.x
// https://github.com/lightbend/mima#usage
crossSbtVersions := List("0.13.18", "1.4.7")

publishMavenStyle := false

resolvers += Resolver.bintrayRepo("typesafe", "sbt-plugins")

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.0.5" % Test,
  "se.sawano.java" % "alphanumeric-comparator" % "1.4.1"
)

// you need to enable the plugin HERE to depend on code in the plugin JAR. you can't add it as part of
// libraryDependencies nor put it in plugins.sbt
// We use MiMa 0.3.0 because it is the only version that exists for both SBT 1.2.x and 0.13.x
// Also MiMa 0.6.x has some source incompatible changes, so we'd have to fork the source to support 0.6.x and 0.3.x
// https://github.com/lightbend/mima#usage
addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "0.3.0")
addSbtPlugin("com.dwijnand" % "sbt-compat" % "1.2.6")

// disable scaladoc generation
sources in(Compile, doc) := Seq.empty

publishArtifact in packageDoc := false
