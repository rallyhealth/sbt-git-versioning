package com.rallyhealth.sbt.versioning

import java.util.concurrent.TimeUnit

import com.rallyhealth.util.NullSbtLogger
import org.scalatest.FunSuite

import scala.concurrent.duration.Duration

class GitFetcherSpec extends FunSuite {

  test("should not fail") {
    implicit val logger = NullSbtLogger
    GitFetcher.fetchRemotes(Seq.empty, Duration(5, TimeUnit.SECONDS))
  }

}
