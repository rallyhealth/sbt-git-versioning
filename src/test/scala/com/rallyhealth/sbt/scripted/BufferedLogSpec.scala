package com.rallyhealth.sbt.scripted

import java.io.{BufferedReader, InputStream, InputStreamReader}

import com.rallyhealth.sbt.scripted.BufferedLogSpec._
import org.scalatest.FunSpec

import scala.util.Random

class BufferedLogSpec extends FunSpec {

  describe("removeFormatting") {

    it("remove nothing") {
      val reader = new BufferedReader(new InputStreamReader(loadFile("/scriptedOutput-example1.txt")))
      Iterator
        .continually(reader.readLine()).takeWhile(_ != null)
        .slice(2, 6) // not all the lines have ANSI cruft
        .foreach { original =>
          val clean = BufferedLog.removeFormatting(original)
          assert(clean.length !== original.length, s"Lengths original=${original.length} clean=${clean.length}")
        }
    }

    it("remove ANSI terminal codes") {
      (0 until 10)
        .map(_ => Iterator.continually(Random.nextPrintableChar()).take(Random.nextInt(128)).mkString(""))
        .foreach { original =>
          val clean = BufferedLog.removeFormatting(original)
          assert(clean.length === original.length, s"Lengths original=${original.length} clean=${clean.length}")
        }
    }
  }

  describe("parseLog") {

    it("no tests") {
      val rawLog = (0 until 10)
        .map(_ => Iterator.continually(Random.nextPrintableChar()).take(Random.nextInt(128)).mkString(""))
      val parsedLog = BufferedLog.parseLog(rawLog.toIterator)
      assert(parsedLog.isEmpty, rawLog.mkString("\n"))
    }

    it("one test starts at the very beginning") {
      val rawLog = (0 until 10)
        .map {
          case index if index == 0 => BufferedLog.StartTestLogEvent.forTestName(index.toString)
          case _ => Iterator.continually(Random.nextPrintableChar()).take(Random.nextInt(128)).mkString("")
        }
      val parsedLog = BufferedLog.parseLog(rawLog.toIterator)
      assert(parsedLog.size === 9, rawLog.mkString("\n"))

      val grouped = parsedLog.groupBy(_.testName)
      assert(grouped.size === 1, rawLog.mkString("\n"))
      grouped.foreach {
        case (testName, logLines) =>
          assert(logLines.size === 9, "for testName " + testName + rawLog.mkString("\n", "\n", ""))
      }
    }

    it("one test starts in the middle") {
      val rawLog = (0 until 10)
        .map {
          case index if index == 5 => BufferedLog.StartTestLogEvent.forTestName(index.toString)
          case _ => Iterator.continually(Random.nextPrintableChar()).take(Random.nextInt(128)).mkString("")
        }
      val parsedLog = BufferedLog.parseLog(rawLog.toIterator)
      assert(parsedLog.size === 4, rawLog.mkString("\n"))

      val grouped = parsedLog.groupBy(_.testName)
      assert(grouped.size === 1, rawLog.mkString("\n"))
      grouped.foreach {
        case (testName, logLines) =>
          assert(logLines.size === 4, "for testName " + testName + rawLog.mkString("\n", "\n", ""))
      }
    }

    it("simple tests") {
      val rawLog = (0 until 100)
        .map {
          case index if index % 10 == 0 => BufferedLog.StartTestLogEvent.forTestName(index.toString)
          case _ => Iterator.continually(Random.nextPrintableChar()).take(Random.nextInt(128)).mkString("")
        }
      val parsedLog = BufferedLog.parseLog(rawLog.toIterator)
      assert(parsedLog.size === 90, rawLog.mkString("\n")) // not 100 because we drop the first 10 tests

      val grouped = parsedLog.groupBy(_.testName)
      assert(grouped.size === 10, rawLog.mkString("\n"))
      grouped.foreach {
        case (testName, logLines) =>
          assert(logLines.size === 9, "for testName " + testName + rawLog.mkString("\n", "\n", ""))
      }
    }

    it("test demarcation line with random prefix and suffix") {
      val rawLog = (0 until 10)
        .map {
          case index if index == 0 =>
            val prefix = Iterator.continually(Random.nextPrintableChar()).take(Random.nextInt(20)).mkString("")
            val suffix = Iterator.continually(Random.nextPrintableChar()).take(Random.nextInt(20)).mkString("")
            prefix + BufferedLog.StartTestLogEvent.forTestName(index.toString) + suffix
          case _ =>
            Iterator.continually(Random.nextPrintableChar()).take(Random.nextInt(128)).mkString("")
        }
      val parsedLog = BufferedLog.parseLog(rawLog.toIterator)
      assert(parsedLog.size === 9, rawLog.mkString("\n"))

      val grouped = parsedLog.groupBy(_.testName)
      assert(grouped.size === 1, rawLog.mkString("\n"))
      grouped.foreach {
        case (testName, logLines) =>
          assert(logLines.size === 9, "for testName " + testName + rawLog.mkString("\n", "\n", ""))
      }
    }

    it("test name with spaces") {
      val rawLog = (0 until 10)
        .map {
          case index if index == 0 => BufferedLog.StartTestLogEvent.forTestName("name with spaces")
          case _ => Iterator.continually(Random.nextPrintableChar()).take(Random.nextInt(128)).mkString("")
        }
      val parsedLog = BufferedLog.parseLog(rawLog.toIterator)
      assert(parsedLog.size === 9, rawLog.mkString("\n"))

      val grouped = parsedLog.groupBy(_.testName)
      assert(grouped.size === 1, rawLog.mkString("\n"))
      grouped.foreach {
        case (testName, logLines) =>
          assert(logLines.size === 9, "for testName " + testName + rawLog.mkString("\n", "\n", ""))
      }
    }

  }
}

object BufferedLogSpec {

  private def loadFile(fileName: String): InputStream = {
    getClass.getResourceAsStream(fileName)
  }
}
