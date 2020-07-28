package com.rallyhealth.sbt.semver

import sbt.{Level, Logger}

import scala.collection.mutable.ListBuffer

class MockLogger extends Logger {

  val messages = new ListBuffer[MockLogger.Message]

  override def trace(t: => Throwable): Unit = throw new UnsupportedOperationException

  override def success(message: => String): Unit = messages += MockLogger.Message(Level.Info, message)

  override def log(level: Level.Value, message: => String): Unit = messages += MockLogger.Message(level, message)
}

object MockLogger {

  case class Message(level: Level.Value, message: String)

}
