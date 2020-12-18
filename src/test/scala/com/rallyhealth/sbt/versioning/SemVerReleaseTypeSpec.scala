package com.rallyhealth.sbt.versioning

import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class SemVerReleaseTypeSpec
  extends AnyWordSpec
  with Matchers
  with TypeCheckedTripleEquals {

  "Major > Minor > Patch" in {
    assert(SemVerReleaseType.Major === SemVerReleaseType.Major)
    assert(SemVerReleaseType.Major > SemVerReleaseType.Minor)
    assert(SemVerReleaseType.Major > SemVerReleaseType.Patch)

    assert(SemVerReleaseType.Minor < SemVerReleaseType.Major)
    assert(SemVerReleaseType.Minor === SemVerReleaseType.Minor)
    assert(SemVerReleaseType.Minor > SemVerReleaseType.Patch)

    assert(SemVerReleaseType.Patch < SemVerReleaseType.Major)
    assert(SemVerReleaseType.Patch < SemVerReleaseType.Minor)
    assert(SemVerReleaseType.Patch === SemVerReleaseType.Patch)
  }

}
