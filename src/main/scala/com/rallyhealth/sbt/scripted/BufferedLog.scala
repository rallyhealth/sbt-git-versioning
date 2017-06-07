package com.rallyhealth.sbt.scripted

import java.io.{BufferedReader, FileReader, FilenameFilter}
import java.nio.file.Paths
import java.util.concurrent.atomic.AtomicReference

import com.rallyhealth.sbt.scripted.BufferedLog._
import sbt.Level

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.util.Try
import scala.util.matching.Regex

/**
  * Provides access to the buffered log created by `GitVersioningScriptedPlugin`. This is intended to be used by
  * [[ScriptedUtils]].
  */
class BufferedLog {

  private val bufferedFileData = new AtomicReference[FileParseAudit](null)
  private val sideChannelFile = new AtomicReference[java.io.File](null)

  /**
    * We cache the [[LogLine]] instances we read from [[bufferedFileData]] to avoid going to the file for every test.
    * This is populated by [[reloadLog()]].
    */
  private val cachedLogLines = new AtomicReference[List[LogLine]](List.empty)

  def bufferedFile: java.io.File = bufferedFileData.get.file

  /**
    * Performs a safety/sanity check before executing any methods other than [[initialize()]] to ensure that
    * [[initialize()]] was called.
    */
  protected def safetyCheck(): Unit = {
    Option(sideChannelFile.get) match {
      case Some(file) if file.exists() =>
      case Some(file) => sys.error(s"sideChannelFile=${file.getAbsolutePath} does not exist")
      case None => sys.error(s"sideChannelFile is None -- was initialize() run?")
    }

    Option(bufferedFileData.get()) match {
      case Some(data) if data.file.exists() =>
      case Some(data) => sys.error(s"bufferedFile=${data.file.getAbsolutePath} does not exist")
      case None => sys.error(s"bufferedFile is null -- was initialize() run?")
    }
  }

  /**
    * Finds the side-channel file created by `GitVersioningScriptedPlugin`, reads the STDOUT and STDERR buffered file path
    * from the side-channel file, and opens a [[BufferedReader]] on that file path.
    */
  def initialize(): Unit = {

    val workingDir = Paths.get(".").toAbsolutePath.normalize().toFile
    val maybeSideChannelFile = workingDir
      .listFiles(ScriptedOutputFilenameFilter)
      .sortBy(_.getName)
      .lastOption

    maybeSideChannelFile match {

      case Some(scFile) =>
        val bufFile: java.io.File = {
          val scFileReader = new BufferedReader(new FileReader(scFile))
          try {
            new java.io.File(scFileReader.readLine())
          } finally {
            Try(scFileReader.close())
          }
        }

        sideChannelFile.set(scFile)
        bufferedFileData.set(FileParseAudit(bufFile))

      case None =>
        sys.error("Failed to find side channel file in workingDir=" + workingDir)
    }
  }

  /**
    * Returns a list of all test names found in the cached log. This can be used to ensure that a test name is not
    * re-used.
    */
  def findTestNames(): List[String] = {
    reloadLog()
    cachedLogLines.get.foldLeft(new mutable.LinkedHashSet[String]) {
      case (result, ll) => result += ll.testName
    }.toList
  }

  /**
    * Returns all log entries for the provided test name.
    *
    * Since this is tailing an active log, it is possible that subsequent calls will return more log events than
    * previous attempts. It would be wise to try again if you are missing expected data.
    */
  def readLogLines(testName: String): List[String] = {
    reloadLog()
    val result = cachedLogLines.get.collect { case ll if ll.testName == testName => ll.line }

    // we should always have log lines, if we don't something else is wrong -- like the wrong test name
    if (result.isEmpty)
      throw new IllegalStateException(s"no log events found for testName=$testName")

    result
  }

  /**
    * Attempts to reload the log cached in this class if the file's size or modified date has changed. This should
    * be called before at the start of any method that needs to read the log.
    */
  private def reloadLog(): Unit = {
    safetyCheck()
    if (!bufferedFileData.get.changed()) return

    val reader = new BufferedReader(new FileReader(bufferedFileData.get.file))
    val rawLogIterator = Iterator.continually(reader.readLine()).takeWhile(_ != null)
    val newLogLines = parseLog(rawLogIterator)
    cachedLogLines.set(newLogLines)
  }
}

object BufferedLog {

  private object ScriptedOutputFilenameFilter extends FilenameFilter {
    override def accept(dir: java.io.File, name: String): Boolean =
      name.startsWith("scriptedOutput-") && name.endsWith(".link.txt")
  }

  /**
    * The start of a test should be marked by a special log event that we can easily find so we know when each test
    * starts.
    */
  object StartTestLogEvent {

    private val TestNameToken = "<testName>"
    private val LogEventFormat = s"[ScriptedUtils] Starting $TestNameToken ".padTo(70, '#')

    /** Creates a log message for the start of a test. This is meant to be used by [[ScriptedUtils.startTest()]]. */
    def forTestName(testName: String): String = LogEventFormat.replaceAllLiterally(TestNameToken, testName)

    private val regexLogEventFormat: Regex = {
      val pattern = forTestName("TEST_NAME")
        // we need to escape the "[ScriptedUtils]"
        .replaceAllLiterally("[", "\\[")
        .replaceAllLiterally("TEST_NAME", "([\\w\\s]+)")
      (".*" + pattern + ".*").r
    }

    /**
      * Extracts the test name from a log line (created by [[forTestName()]].
      *
      * @return Some(testName) if this line is the start of a test, None otherwise.
      */
    def extractTestName(line: String): Option[String] = line match {
      case regexLogEventFormat(testName) => Some(testName)
      case _ => None
    }
  }

  /** Captures all events from the log with the test that was running. */
  case class LogLine(testName: String, line: String)

  /**
    * Keeps track of a file and its state at the time of creating this object so you can quickly find out if the
    * file has changed.
    */
  private case class FileParseAudit(file: java.io.File, lastLength: Long, lastModified: Long) {

    /** @return True if the file on disk is different than this instance. */
    def changed(): Boolean = this != FileParseAudit(file)
  }

  private object FileParseAudit {
    def apply(file: java.io.File): FileParseAudit = FileParseAudit(file, file.length(), file.lastModified())
  }

  /**
    * Parses the log events into [[LogLine]] instances so we can easily choose only the lines for a specific test.
    * We need this because a search phrase may make sense for one test but not for another test, e.g. one test might
    * expect [error] and another test would not.
    *
    * @return The results of this should be cached to avoid re-parsing the log unnecessarily. See [[FileParseAudit]].
    */
  def parseLog(lines: Iterator[String]): List[LogLine] = {

    var currentTestName = ""
    val cleanLines = lines
      .map(x => removeFormatting(x))
      // we do not use debug lines for testing, they are just noise
      .filter(!_.startsWith(s"[${Level.Debug}]"))

    cleanLines.foldLeft(new ListBuffer[LogLine]) {
      case (result, line) =>

        StartTestLogEvent.extractTestName(line) match {
          // new test! but we don't care about that log line, its just noise
          case Some(testName) => currentTestName = testName
          // until we have found our first test we ignore these lines
          case None if currentTestName == "" =>
          // normal case
          case None => result += LogLine(currentTestName, line)
        }

        result

    }.toList
  }

  /**
    * Removes ANSI escape codes from the string so that we can do normal searching and grepping.
    * [[http://stackoverflow.com/a/14652763/11889]]
    */
  def removeFormatting(line: String): String = {
    line
      // remove all ANSI escape codes (terminal colors and such)
      // http://stackoverflow.com/a/14652763/11889
      //.replaceAll("\u001B\\[[;\\d]*[ -/]*[@-~]", "")
      .replaceAll("\u001B\\[[;\\d]*m", "")
      // I want to use [:cntrl:] but .replaceAll() apparently does not support POSIX character classes
      // http://www.regular-expressions.info/posixbrackets.html
      .replaceAll("[\\x00-\\x1F\\x7F]", "")
  }

}
