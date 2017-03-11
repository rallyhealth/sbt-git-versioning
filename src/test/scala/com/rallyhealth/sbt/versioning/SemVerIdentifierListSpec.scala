package com.rallyhealth.sbt.versioning

import com.rallyhealth.sbt.versioning.StringSemVerIdentifier.string2StringIdentifier
import org.scalatest.FunSpec

class SemVerIdentifierListSpec extends FunSpec {

  describe("even lists") {

    it("equal") {
      val x = SemVerIdentifierList(Seq("a", "b"))
      val y = SemVerIdentifierList(Seq("a", "b"))
      assert(x === y)
      assert(y === x)
    }

    it("head !=") {
      val x = SemVerIdentifierList(Seq("a", "b"))
      val y = SemVerIdentifierList(Seq("c", "b"))
      assert(x < y)
      assert(y > x)
      assert(x !== y)
      assert(y !== x)
    }

    it("last !=") {
      val x = SemVerIdentifierList(Seq("a", "b"))
      val y = SemVerIdentifierList(Seq("a", "c"))
      assert(x < y)
      assert(y > x)
      assert(x !== y)
      assert(y !== x)
    }

    it("empty") {
      val x = SemVerIdentifierList(Seq.empty)
      val y = SemVerIdentifierList(Seq.empty)
      assert(x === y)
      assert(y === x)
    }
  }

  describe("uneven lists") {

    it("one off") {
      val x = SemVerIdentifierList(Seq("a"))
      val y = SemVerIdentifierList(Seq("a", "b"))
      assert(x < y)
      assert(y > x)
      assert(x !== y)
      assert(y !== x)
    }

    it("one off (not equal)") {
      val x = SemVerIdentifierList(Seq("a"))
      val y = SemVerIdentifierList(Seq("b", "c"))
      assert(x < y)
      assert(y > x)
      assert(x !== y)
      assert(y !== x)
    }

    it("multiple off") {
      val x = SemVerIdentifierList(Seq("a"))
      val y = SemVerIdentifierList(Seq("a", "b", "c"))
      assert(x < y)
      assert(y > x)
      assert(x !== y)
      assert(y !== x)
    }

    it("multiple off (not equal)") {
      val x = SemVerIdentifierList(Seq("a"))
      val y = SemVerIdentifierList(Seq("b", "c", "d"))
      assert(x < y)
      assert(y > x)
      assert(x !== y)
      assert(y !== x)
    }

    it("one empty") {
      // an empty list of identifiers is considered GREATER than a non-empty list, see http://semver.org/#spec-item-11
      val x = SemVerIdentifierList(Seq.empty)
      val y = SemVerIdentifierList(Seq("a", "b"))
      assert(x > y)
      assert(y < x)
      assert(x !== y)
      assert(y !== x)
    }
  }
}
