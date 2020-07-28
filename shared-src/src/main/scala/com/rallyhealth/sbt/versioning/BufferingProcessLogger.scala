package com.rallyhealth.sbt.versioning

import scala.collection.mutable
import scala.sys.process._

/** A [[ProcessLogger]] that buffers all messages into String instance fields. */
class BufferingProcessLogger extends ProcessLogger {

  val stdout: mutable.Buffer[String] = mutable.Buffer[String]()
  val stderr: mutable.Buffer[String] = mutable.Buffer[String]()

  override def out(s: => String): Unit = stdout += s

  override def err(s: => String): Unit = stderr += s

  override def buffer[T](f: => T): T = f
}
