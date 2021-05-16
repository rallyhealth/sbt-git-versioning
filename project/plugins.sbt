resolvers += Resolver.url(
  "Rally Plugin Releases",
  url("https://dl.bintray.com/rallyhealth/sbt-plugins"))(Resolver.ivyStylePatterns)

addSbtPlugin("com.rallyhealth.sbt" % "sbt-git-versioning" % "1.6.0")
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.0")

