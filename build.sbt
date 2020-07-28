name := "sbt-git-versioning-root"

ThisBuild / organizationName := "Rally Health"
ThisBuild / organization := "com.rallyhealth.sbt"

ThisBuild / licenses := Seq("MIT" -> url("http://opensource.org/licenses/MIT"))

ThisBuild / bintrayOrganization := Some("rallyhealth")
ThisBuild / bintrayRepository := "sbt-plugins"

ThisBuild / scalacOptions ++= {
  val linting = CrossVersion.partialVersion(scalaVersion.value) match {
    // some imports are necessary for compat with 2.10.
    // 2.12 needs to chill out with the unused imports warnings.
    case Some((2, 12)) => "-Xlint:-unused,_"
    case _ => "-Xlint"
  }
  Seq("-Xfatal-warnings", linting)
}

lazy val defaultSbtOpts = settingKey[Seq[String]]("The contents of the default_sbt_opts env var.")
lazy val javaOptsDebugger = settingKey[String]("Opens a debug port for scripted tests on 8000")

ThisBuild / publishMavenStyle := false

resolvers += Resolver.bintrayRepo("typesafe", "sbt-plugins")

ThisBuild / resolvers += Resolver.bintrayRepo("typesafe", "sbt-plugins")
ThisBuild / libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.0.5" % Test,
  "se.sawano.java" % "alphanumeric-comparator" % "1.4.1"
)

lazy val shared = Project("shared", file(".shared-src"))
  .enablePlugins(SbtPlugin)

lazy val zero_thirteen = Project("zero_thirteen", file("zero_thirteen"))
  .settings(
    name := "sbt-git-versioning",
    // Uncomment to default to sbt 0.13 for debugging
    pluginCrossBuild / sbtVersion := "0.13.18",
    scalaVersion := "2.10.6",
    // you need to enable the plugin HERE to depend on code in the plugin JAR. you can't add it as part of
    // libraryDependencies nor put it in plugins.sbt
    // We use MiMa 0.3.0 because it is the only version that exists for both SBT 1.2.x and 0.13.x
    // Also MiMa 0.6.x has some source incompatible changes, so we'd have to fork the source to support 0.6.x and 0.3.x
    // https://github.com/lightbend/mima#usage
    addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "0.3.0"),
    addSbtPlugin("com.dwijnand" % "sbt-compat" % "1.2.6"),
    addSbtPlugin("com.dwijnand" % "sbt-dynver" % "4.0.0"),
    compileWithSourcesFrom("shared-src"),

    javaOptsDebugger := "-agentlib:jdwp=transport=dt_socket,server=y,address=8000,suspend=n",
    defaultSbtOpts := {
      sys.env.get("default_sbt_opts").toSeq ++ sys.env.get("scripted_sbt_opts")
    },
    scriptedLaunchOpts := {
      scriptedLaunchOpts.value ++
        Seq("-Xmx1024M", "-Dplugin.version=" + version.value, javaOptsDebugger.value) ++
        defaultSbtOpts.value
    }
  )
  .enablePlugins(SbtPlugin)

def compileWithSourcesFrom(subDir: String) = {
  val srcDir = file(".").getAbsoluteFile / subDir / "src"
  Seq(
    unmanagedSourceDirectories in Compile += srcDir / "main",
    unmanagedSourceDirectories in Compile += baseDirectory.value / "main",
    unmanagedSourceDirectories in Test += srcDir / "test",
    unmanagedSourceDirectories in Test += baseDirectory.value / "test",
    sbtTestDirectory := srcDir / "sbt-test"
  )
}

lazy val one_dot_x = Project("one_dot_x", file("./one_dot_x"))
  .settings(
    name := "sbt-git-versioning",
    crossSbtVersions := List("1.3.9"),
    scalaVersion := "2.12.11",
    // you need to enable the plugin HERE to depend on code in the plugin JAR. you can't add it as part of
    // libraryDependencies nor put it in plugins.sbt
    // 0.6.0 isn't the newest but it made crossing with the 0.3.0 version much easier and is far enough forward to support sbt 1.3
    addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "0.6.1"),
    addSbtPlugin("com.dwijnand" % "sbt-dynver" % "4.0.0"),

    // NOTE: The port here is different than the one in the 0.13 configuration
    javaOptsDebugger := "-agentlib:jdwp=transport=dt_socket,server=y,address=8001,suspend=n",
    scriptedLaunchOpts := {
      scriptedLaunchOpts.value ++
        Seq("-Xmx1024M", "-Dplugin.version=" + version.value, javaOptsDebugger.value) ++
        defaultSbtOpts.value
    },
    defaultSbtOpts := {
      sys.env.get("default_sbt_opts").toSeq ++ sys.env.get("scripted_sbt_opts")
    },
    compileWithSourcesFrom("shared-src")
  )
  .enablePlugins(SbtPlugin)

// disable scaladoc generation
ThisBuild / Compile / doc / sources := Seq.empty
ThisBuild / publishArtifact in packageDoc := false

ThisBuild / scriptedBufferLog := false
