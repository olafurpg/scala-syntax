package org.scalafmt.tests

import scala.meta._
import scala.meta.contrib.AssociatedComments
import org.scalafmt.internal.TreePrinter
import scala.meta.internal.format.Comments._
import scala.collection.mutable
import scala.meta.internal.format.Comments

object RefactorSuite extends BaseScalaPrinterTest {

  test("basic") {
    val code =
      """
        |object a {
        |  def bar = 2
        |
        |  // maps foo
        |  @deprecated()
        |  def foo = List(1) map foo
        |}
      """.stripMargin.parse[Source].get
    val comments = AssociatedComments(code)
    val done = mutable.Set.empty[Token]

    val attached = code.transform {
      case t =>
        val leading = comments.leading(t).filterNot(done)
        val trailing = comments.trailing(t).filterNot(done)
        val withLeading = leading.foldLeft(t) {
          case (t, c) =>
            done += c
            t.withLeadingComment(c.syntax)
        }
        val withTrailing = trailing.foldLeft(withLeading) {
          case (t, c) =>
            done += c
            t.withTrailingComment(c.syntax)
        }
        val c = Comments(withTrailing)
        if (leading.nonEmpty) {
          pprint.log(t.syntax)
          pprint.log(c)
        }
        withTrailing
    }
    def visit(tree: sourcecode.Text[Tree]): Unit = {
      pprint.log(tree.source)
      tree.value.traverse {
        case t =>
          val c = Comments(t)
          if (c.nonEmpty) pprint.log(c)
      }
    }

    val transformed = attached.transform {
      case t @ Term.ApplyInfix(lhs, op, Nil, args) =>
        visit(t)
        q"$lhs.$op(..$args)"
    }

    visit(code)
    visit(attached)
    visit(transformed)

    val obtained = TreePrinter.print(transformed).render(100)
    println(obtained)

    val expected =
      """
        |object a {
        |  def bar = 2
        |
        |  // maps foo
        |  def foo = List(1).map(foo)
        |}
      """.stripMargin

    assertNoDiff(obtained, expected)

  }

}
