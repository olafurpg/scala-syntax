package org.scalafmt.internal

import org.scalafmt.Options

case class Context(options: Options)
object Context {
  val default = Context(Options.default)
}
