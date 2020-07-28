package com.rallyhealth.sbt.versioning

import org.scalatest.FunSpec

class StringSemVerIdentifierSpec extends FunSpec {

  // http://semver.org/#spec-item-9
  it("from SemVer docs #9") {
    val versionsFromDocs = Seq("1.0.0-alpha", "1.0.0-alpha.1", "1.0.0-0.3.7", "1.0.0-x.7.z.92")

    val identifiersFromDocs = versionsFromDocs.map(_.dropWhile(_ != '-').drop(1)) // drop prefix
    val identifiers = identifiersFromDocs.map(StringSemVerIdentifier(_).value)
    assert(identifiers === identifiersFromDocs)

    val versions = versionsFromDocs.flatMap(SemanticVersion.fromString).map(_.toString)
    assert(versions === versionsFromDocs)
  }
}
