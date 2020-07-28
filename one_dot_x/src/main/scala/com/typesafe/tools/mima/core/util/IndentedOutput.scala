package com.typesafe.tools.mima.core.util

// Backfilled from the mima source
// https://github.com/lightbend/mima/blob/0.3.0/core/src/main/scala/com/typesafe/tools/mima/core/util/IndentedOutput.scala
object IndentedOutput {
  var indentMargin = 2
  private var indent = 0
  def printLine(str: String) = println(" " * indent + str)
  def indented[T](op: => T): T = try {
    indent += indentMargin
    op
  } finally {
    indent -= indentMargin
  }
}
