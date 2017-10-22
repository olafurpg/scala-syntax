package scala.meta.internal.format

import java.io.InputStream
import java.nio.charset.StandardCharsets
import scala.annotation.tailrec
import scala.meta._
import scala.meta.contrib.Trivia
import scala.meta.internal.tokens.TokenStreamPosition
import scala.meta.internal.trees.Origin
import scala.meta.tokens.Token.Comment
import org.scalafmt.internal.Context
import org.scalameta.logger
import org.typelevel.paiges.Doc

/**
 * Metadata about comments attached to a tree node.
 *
 * Can include any kind of comments: /* */, // or /** */
 */
case class Comments(leading: List[Comment], trailing: List[Comment])
    extends InputStream {
  val line: Doc = Doc.lineNoFlat
  @tailrec
  final def print(ss: List[Comment], accum: Doc = Doc.empty): Doc = ss match {
    case Nil => accum
    case comment :: tail =>
      val s = comment.syntax
      val before =
        if (s.startsWith("//")) Doc.space
        else Doc.empty
      val after =
        if (s.startsWith("//")) line
        else if (s.startsWith("/**")) line
        else if (s.startsWith("/*")) Doc.space
        else Doc.empty
      val doc = before + Doc.text(s.trim) + after
      print(tail, doc + accum)
  }

  def wrap(doc: Doc): Doc = print(leading) + doc + print(trailing)
  override def read(): Int = 1
}

object Comments {
  def Comment(comment: String): Comment = new Comment(
    Input.Stream(new InputStream {
      override def read(): Int = 1
    }, StandardCharsets.UTF_8),
    dialects.Scala212,
    -1,
    -1,
    comment
  )
  val default = Comments(Nil, Nil)
  def print(tree: Tree, treePrinted: Doc)(implicit ctx: Context): Doc =
    Comments(tree).wrap(treePrinted)
  def apply(tree: Tree)(implicit ctx: Context): Comments = tree.origin match {
    case Origin.Parsed(Input.Stream(c: Comments, _), _, _) => c
    case _ => Comments(ctx.comments.leading(tree), ctx.comments.trailing(tree))
  }

  // Hacky approach to comments. TODO(olafur) keep track of trailing/leading
  // whitespace before each comment.
  def inferCommentsFromTokens(
      tokens: Tokens,
      pos: TokenStreamPosition
  ): Comments = {
    def slurp(it: Iterator[Token]): List[Comment] =
      it.takeWhile(t => t.is[Trivia]).collect { case c: Comment => c }.toList
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
      withComments(x => x.copy(leading = Comment(comment) :: x.leading))

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
      withComments(x => x.copy(trailing = Comment(comment) :: x.trailing))

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
