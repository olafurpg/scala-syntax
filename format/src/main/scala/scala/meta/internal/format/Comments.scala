package scala.meta.internal.format

import java.io.InputStream
import java.nio.charset.StandardCharsets
import scala.meta._
import scala.meta.contrib.Trivia
import scala.meta.internal.tokens.TokenStreamPosition
import scala.meta.internal.trees.Origin
import scala.meta.tokens.Token.Comment
import org.scalameta.logger
import org.typelevel.paiges.Doc

/**
 * Metadata about comments attached to a tree node.
 *
 * Can include any kind of comments: /* */, // or /** */
 */
case class Comments(leading: List[String], trailing: List[String])
    extends InputStream {
  val line: Doc = Doc.lineNoFlat
  def join(ss: List[String]): Doc =
    Doc.intercalate(
      Doc.empty,
      ss.map { s =>
        val before =
          if (s.startsWith("//")) Doc.space
          else Doc.empty
        val after =
          if (s.startsWith("//")) line
          else if (s.startsWith("/**")) line
          else if (s.startsWith("/*")) Doc.space
          else Doc.empty
        before + Doc.text(s.trim) + after
      }
    )
  def wrap(doc: Doc): Doc = join(leading) + doc + join(trailing)
  override def read(): Int = 1
}

object Comments {
  val default = Comments(Nil, Nil)
  def print(tree: Tree, treePrinted: Doc): Doc =
    Comments(tree).wrap(treePrinted)
  def apply(tree: Tree): Comments = tree.origin match {
    case Origin.Parsed(Input.Stream(c: Comments, _), _, _) => c
    case Origin.Parsed(input, dialect, pos) =>
      tree.children match {
        case head :: _ if head.pos.start == tree.pos.start => default
        case _ =>
          tree.children match {
            case head :: _ =>
              logger.elem(head.syntax, head.pos.start, tree.pos.start)
            case _ =>
          }
          inferCommentsFromTokens(dialect(input).tokenize.get, pos)
      }
    case _ => default
  }

  // Hacky approach to comments. TODO(olafur) keep track of trailing/leading
  // whitespace before each comment.
  def inferCommentsFromTokens(
      tokens: Tokens,
      pos: TokenStreamPosition
  ): Comments = {
    def slurp(it: Iterator[Token]) =
      it.takeWhile(t => t.is[Trivia])
        .collect { case c: Comment => c.syntax }
        .toList
    val leading = slurp(
      if (pos.start == 0) tokens.view.takeWhile(_.is[Trivia]).reverseIterator
      else tokens.view(0, pos.start - 1).reverseIterator
    )
    val trailing = slurp(tokens.view(pos.end, tokens.length).iterator)
    Comments(leading, trailing)
  }
  implicit class XtensionTreeComments[T <: Tree](val tree: T) extends AnyVal {

    /**
     * Attach a leading comment to this tree node.
     *
     * {{{
     * > import Comments._
     * > q"def foo = 2".withLeadingComment("/** Returns 2 */")
     * res0: String = "
     * /** Returns 2 */
     * def foo = 2
     * "
     * }}}
     *
     * @param comment the string is formatted raw, except indentation is adapted
     * to the indentation of the tree node.
     */
    def withLeadingComment(comment: String): T =
      withComments(x => x.copy(leading = comment :: x.leading))

    /**
     * Attach a trailing comment to this tree node.
     *
     * {{{
     * > import Comments._
     * > q"def foo = 2".withTrailingComment("FIXME")
     * res0: String = "
     * def foo = 2 // FIXME
     * "
     * }}}
     *
     * @param comment the string is formatted raw, except indentation is adapted
     * to the indentation of the tree node.
     */
    def withTrailingComment(comment: String): T =
      withComments(x => x.copy(trailing = comment :: x.trailing))

    private def withComments(f: Comments => Comments): T = tree.withOrigin(
      tree.origin match {
        case o @ Origin.Parsed(i @ Input.Stream(c: Comments, _), _, _) =>
          o.copy(input = i.copy(stream = f(c)))
        case _ =>
          Origin.Parsed(
            Input.Stream(f(default), StandardCharsets.UTF_8),
            dialects.Scala212,
            TokenStreamPosition(-1, -1)
          )
      }
    )
  }
}
