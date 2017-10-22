package org.scalafmt.internal

import scala.meta.Term
import scala.meta.contrib.AssociatedComments
import scala.meta.internal.format.Comments
import scala.meta.internal.format.TreeComments
import scala.meta.tokens.Tokens
import org.scalafmt.Options

case class Context(
    options: Options = Options.default,
    comments: TreeComments = TreeComments.empty
)
object Context {
  val default = Context()
}
