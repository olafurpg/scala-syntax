package scala.meta.internal.format

import scala.meta._
import scala.meta.tokens.Token
import scala.meta.tokens.Token.Comment
import scala.collection.immutable.List
import scala.meta.contrib.Trivia
import org.scalameta.logger

/**
 * Fork of contrib's AssociatedComments.
 */
sealed abstract class TreeComments(
    leadingMap: Map[Position, List[Comment]],
    trailingMap: Map[Position, List[Comment]]
) {
  private def pretty(map: Map[Position, List[Comment]]): String =
    map
      .map {
        case (pos, comments) =>
          val commentStructure =
            comments.map(comment => logger.revealWhitespace(comment.syntax))
          s"    ${pos.text} => $commentStructure"
      }
      .mkString("\n")
  def syntax: String =
    s"""|TreeComments(
        |  Leading =
        |${pretty(leadingMap)}
        |
        |  Trailing =
        |${pretty(trailingMap)}
        |)""".stripMargin

  override def toString: String = syntax

  def leading(tree: Tree): List[Comment] =
    (for {
      _ <- tree.children.headOption.filter(_.pos.start == tree.pos.start)
      token <- tree.tokens.headOption
      comments <- leadingMap.get(token.pos)
    } yield comments).getOrElse(Nil)

  def trailing(tree: Tree): List[Comment] =
    (for {
      _ <- tree.children.lastOption.filter(_.pos.end == tree.pos.end)
      token <- tree.tokens.lastOption
      comments <- trailingMap.get(token.pos)
    } yield comments).getOrElse(Nil)

  def hasComment(tree: Tree): Boolean =
    trailing(tree).nonEmpty || leading(tree).nonEmpty
}

object TreeComments {
  val empty: TreeComments = new TreeComments(Map.empty, Map.empty) {}

  def apply(tree: Tree): TreeComments = apply(tree.tokens)
  def apply(tokens: Tokens): TreeComments = {
    import scala.meta.tokens.Token._
    val leadingBuilder = Map.newBuilder[Position, List[Comment]]
    val trailingBuilder = Map.newBuilder[Position, List[Comment]]
    val leading = List.newBuilder[Comment]
    val trailing = List.newBuilder[Comment]
    var isLeading = true
    var lastToken: Token = tokens.head
    tokens.foreach {
      case c: Comment =>
        if (isLeading) leading += c
        else trailing += c
      case Token.LF() => isLeading = true
      case Trivia() =>
      case currentToken =>
        val t = trailing.result()
        if (t.nonEmpty) {
          trailingBuilder += lastToken.pos -> trailing.result()
          trailing.clear()
        }
        val l = leading.result()
        if (l.nonEmpty) {
          leadingBuilder += currentToken.pos -> leading.result()
          leading.clear()
        }
        if (!currentToken.is[Comma]) {
          lastToken = currentToken
        }
        isLeading = false
    }
    new TreeComments(leadingBuilder.result(), trailingBuilder.result()) {}
  }
}
