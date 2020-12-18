package com.rallyhealth.sbt.versioning

import com.rallyhealth.sbt.versioning.GitCommit._
import com.rallyhealth.sbt.versioning.HashSemVerIdentifierSpec._
import org.scalactic.TripleEqualsSupport
import org.scalatest.funspec.AnyFunSpec

class GitCommitSpec extends AnyFunSpec with TripleEqualsSupport {

  describe("fromGitLog") {

    // Most of the weird test caes here from RCU

    it("without identifier") {
      val result = fromGitLog(s"${expandHash("dc96343")} (tag: v1.2.3) Setting version to 0.3.0", 7)
      assert(result.fullHash === expandHash("dc96343"))
      assert(result.tags === Seq("v1.2.3"))
    }

    it("no tag") {
      val result = fromGitLog(s"${expandHash("dc96343")} Setting version to 0.3.0", 7)
      assert(result.fullHash === expandHash("dc96343"))
      assert(result.tags.isEmpty)
    }

    it("ancient form of our tags -- we don't support them :p") {
      val result = fromGitLog(s"${expandHash("dc96343")} (tag: release/v1.99.0) Merge pull request #9964", 7)
      assert(result.fullHash === expandHash("dc96343"))
      assert(result.tags === Seq("release/v1.99.0"))
    }

    it("junk tag") {
      val result = fromGitLog(s"${expandHash("dc96343")} (tag: JUNK) Setting version to 0.3.0", 7)
      assert(result.fullHash === expandHash("dc96343"))
      assert(result.tags === Seq("JUNK"))
    }

    it("HEAD decoration but no tags") {
      val result = fromGitLog(s"${expandHash("dc96343")} (HEAD -> master) First version with 'real' code", 7)
      assert(result.fullHash === expandHash("dc96343"))
      assert(result.tags.isEmpty)
    }

    it("decoration but no tag") {
      val result = fromGitLog(s"${expandHash("dc96343")} (upstream/SEIT) Merge pull request #9955", 7)
      assert(result.fullHash === expandHash("dc96343"))
      assert(result.tags.isEmpty)
    }

    it("HEAD decorations with tag (before)") {
      val result = fromGitLog(s"${expandHash("dc96343")} (tag: v1.2.3, HEAD -> master) First version", 7)
      assert(result.fullHash === expandHash("dc96343"))
      assert(result.tags === Seq("v1.2.3"))
    }

    it("multiple tags") {
      val result = fromGitLog(s"${expandHash("dc96343")} (tag: v1.92.1, tag: v1.92.0) Merge pull request #9606", 7)
      assert(result.fullHash === expandHash("dc96343"))
      assert(result.tags === Seq("v1.92.1", "v1.92.0"))
    }

    it("tags are sorted") {
      val result = fromGitLog(s"${expandHash("dc96343")} (tag: v1.92.1, tag: v1.92.0, tag: v1.92.2) Stuff", 7)
      assert(result.fullHash === expandHash("dc96343"))
      assert(result.tags === Seq("v1.92.2", "v1.92.1", "v1.92.0"))
      assert(result.tags.head === "v1.92.2")
    }
  }
}
