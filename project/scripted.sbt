libraryDependencies += {
  if (sbtVersion.value.startsWith("0.")) "org.scala-sbt" % "scripted-plugin" % sbtVersion.value
  else "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value
}
