package com.rallyhealth.sbt.util

import sbt.{ControlEvent, Level, LogEvent}

object NullSbtLogger extends sbt.BasicLogger {

  override def control(event: ControlEvent.Value, message: ⇒ String): Unit = ()

  override def log(level: Level.Value, message: ⇒ String): Unit = ()

  override def logAll(events: Seq[LogEvent]): Unit = ()

  override def success(message: ⇒ String): Unit = ()

  override def trace(t: ⇒ Throwable): Unit = ()
}
