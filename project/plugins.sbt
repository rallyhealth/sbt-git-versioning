resolvers += Resolver.url(
  "Rally Plugin Releases",
  url("https://dl.bintray.com/rallyhealth/sbt-plugins"))(Resolver.ivyStylePatterns)

addSbtPlugin("com.rallyhealth.sbt" % "git-versioning-sbt-plugin" % "0.0.2")
addSbtPlugin("me.lessis" % "bintray-sbt" % "0.3.0")
