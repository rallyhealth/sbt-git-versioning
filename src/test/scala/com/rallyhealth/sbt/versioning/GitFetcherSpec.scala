package com.rallyhealth.sbt.versioning

import java.util.concurrent.TimeUnit

import com.rallyhealth.sbt.util.NullSbtLogger
import org.scalatest.FunSuite
import sbt.Logger

import scala.concurrent.duration.Duration

class GitFetcherSpec extends FunSuite {

  test("should not fail") {
    implicit val logger: Logger = NullSbtLogger
    GitFetcher.fetchRemotes(Seq.empty, Duration(5, TimeUnit.SECONDS))
  }

}
