/*
 * All of this comes from http://eed3si9n.com/testing-sbt-plugins, except using `RallyScriptedPlugin`
 */
GitVersioningScriptedPlugin.scriptedSettings

/**
  * The scripted sbt projects also need to know any sbt opt overrides. For example:
  * - if the .ivy2 location is in another place
  * - if logging options should be changed
  */
lazy val defaultSbtOpts = settingKey[Seq[String]]("The contents of the default_sbt_opts env var.")
lazy val javaOptsDebuger = settingKey[String]("Opens a debug port for scripted tests on 8000")

defaultSbtOpts := {
  sys.env.get("default_sbt_opts").toSeq ++ sys.env.get("scripted_sbt_opts")
}

scriptedLaunchOpts := {
  scriptedLaunchOpts.value ++
    Seq("-Xmx1024M", "-Dplugin.version=" + version.value, javaOptsDebuger.value) ++
    defaultSbtOpts.value
}

javaOptsDebuger := "-agentlib:jdwp=transport=dt_socket,server=y,address=8000,suspend=n"
