package com.rallyhealth.sbt.versioning

import sbt.ProcessLogger

import scala.collection.mutable

/** A [[ProcessLogger]] that buffers all messages into String instance fields. */
class BufferingProcessLogger extends ProcessLogger {

  val stdout: mutable.Buffer[String] = mutable.Buffer[String]()
  val stderr: mutable.Buffer[String] = mutable.Buffer[String]()

  override def info(s: => String): Unit = stdout += s

  override def error(s: => String): Unit = stderr += s

  override def buffer[T](f: => T): T = f
}
