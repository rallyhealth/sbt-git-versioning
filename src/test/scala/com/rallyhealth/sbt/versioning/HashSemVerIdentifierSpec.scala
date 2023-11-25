package com.rallyhealth.sbt.versioning

import org.scalatest.FunSpec

import scala.util.Random

class HashSemVerIdentifierSpec extends FunSpec {

  describe("constructor") {

    it("without 'g' prefix") {
      val tag = HashSemVerIdentifier("0123456")
      assert(tag.hash === "0123456")
      assert(tag.toString === "0123456")
    }

    it("with 'g' prefix") {
      intercept[IllegalArgumentException] {
        HashSemVerIdentifier("g0123456")
      }
    }
  }

  describe("safeMake") {

    it("without 'g' prefix") {
      val tag = HashSemVerIdentifier.safeMake("0123456")
      assert(tag.hash === "0123456")
      assert(tag.toString === "0123456")
    }

    it("with 'g' prefix") {
      val tag = HashSemVerIdentifier.safeMake("g0123456")
      assert(tag.hash === "0123456")
      assert(tag.toString === "0123456")
    }
  }

  describe("hash length") {

    it("more than 40") {
      val random = new Random
      (41 until 50).foreach { len =>
        val hash = Iterator.continually(random.nextInt(10)).take(len).map(_.toString).mkString("")
        // no exception (Git claims only 40 but there's no reason we can't support more)
        HashSemVerIdentifier(hash)
      }
    }
  }
}

object HashSemVerIdentifierSpec {

  /** Helper method to expand a short hash into a longer hash, with zeros, so its stable. */
  def expandHash(abbreviatedHash: String): String = abbreviatedHash.padTo(40, '0')
}
