package com.rallyhealth.sbt.versioning

import sbt.util.Logger

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future, TimeoutException}
import scala.util.control.NonFatal
import scala.sys.process.Process

/** Utility code to fetch history from Git. */
object GitFetcher {

  case class FetchResult(remoteName: String, tagName: String)

  // Example Match: * [new tag]         v0.2.7     -> v0.2.7
  private val tagResultRegex = """ \* \[new tag\][ ]+([\S]+).*""".r

  /**
    * Fetches tags from all remote sources.
    *
    * @param remotes Remote sources whose contents should be fetched AFTER fetching the list of tags.
    */
  def fetchRemotes(remotes: Seq[String], timeout: Duration)(implicit logger: Logger): Seq[FetchResult] = {
    val outputLogger = new BufferingProcessLogger
    val processResult = Process("git remote") ! outputLogger

    processResult match {
      case 0 =>
        logger.debug("Fetching remote sources...")
        val remotes = outputLogger.stdout

        val tagsToFetch = remotes.filter(remotes.contains)
        if (tagsToFetch.nonEmpty) {
          logger.info("Fetching tags from: " + tagsToFetch.mkString(", "))
          tagsToFetch.flatMap(remote => fetchTagsFromRemote(remote, timeout))
        } else {
          logger.debug("No tags to fetch")
          Seq.empty[FetchResult]
        }

      case exitCode =>
        logger.error(s"Fetching remotes failed enumerating remotes [git exitCode=$exitCode]")
        Seq.empty[FetchResult]
    }
  }

  private def fetchTagsFromRemote(remote: String, timeout: Duration)(implicit logger: Logger): Seq[FetchResult] = {

    val outputLogger = new BufferingProcessLogger
    val process = Process(s"git fetch $remote --tags").run(outputLogger)
    val resultFuture = Future {
      if (process.exitValue() == 0) {
        outputLogger.stderr.filter(_.contains("[new tag]")).flatMap {
          case tagResultRegex(tag) =>
            logger.debug(s"Fetched from remote=$remote tag=$tag")
            Some(FetchResult(remote, tag))
          case line =>
            logger.warn(s"Unable to parse git result=$line, skipping")
            None
        }
      } else {
        logger.error(s"Fetching remote=$remote failed [git exitCode=${process.exitValue()}]")
        Seq.empty[FetchResult]
      }
    }

    try {
      val result = Await.result(resultFuture, timeout)
      logger.debug(s"Successfully fetched $remote")
      result
    } catch {
      case _: TimeoutException =>
        process.destroy()
        logger.error(s"Fetching remote=$remote timed out [git exitCode=${process.exitValue()}]")
        Seq.empty
      case NonFatal(exc) =>
        logger.error(s"Fetching remote=$remote failed [git exitCode=${process.exitValue()}]")
        logger.trace(exc)
        Seq.empty
    }
  }
}
