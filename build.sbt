import sbt.plugins.SbtPlugin

name := "sbt-git-versioning"
organizationName := "Rally Health"
organization := "com.rallyhealth.sbt"

// enable after SemVerPlugin supports sbt-plugins
//enablePlugins(SemVerPlugin)
semVerLimit := "1.0.999"
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

// to default to sbt 1.0
// sbtVersion in pluginCrossBuild := "1.1.6"
// scalaVersion := "2.12.6"

crossSbtVersions := List("0.13.17", "1.2.1")

publishMavenStyle := false

resolvers += Resolver.bintrayRepo("typesafe", "sbt-plugins")

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.0.5" % Test,
  "se.sawano.java" % "alphanumeric-comparator" % "1.4.1"
)

// you need to enable the plugin HERE to depend on code in the plugin JAR. you can't add it as part of
// libraryDependencies nor put it in plugins.sbt
addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "0.3.0")
addSbtPlugin("com.dwijnand" % "sbt-compat" % "1.2.6")

// disable scaladoc generation
sources in(Compile, doc) := Seq.empty

publishArtifact in packageDoc := false
