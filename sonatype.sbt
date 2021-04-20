// Your profile name of the sonatype account. The default is the same with the organization value
sonatypeProfileName := "com.rallyhealth"

// To sync with Maven central, you need to supply the following information:
publishMavenStyle := true

// publish to Maven Central
ThisBuild / sonatypeCredentialHost := "s01.oss.sonatype.org"
ThisBuild / publishTo := Some {
  if (isSnapshot.value)
    Resolver.url("Sonatype", url("https://s01.oss.sonatype.org/content/repositories/snapshots"))
  else
    Resolver.url("Sonatype", url("https://s01.oss.sonatype.org/content/repositories/releases"))
}

import xerial.sbt.Sonatype.GitHubHosting
sonatypeProjectHosting := Some(GitHubHosting("rallyhealth", "sbt-git-versioning", "jeff.n.may@gmail.com"))
