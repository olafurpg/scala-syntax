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
        |/**
        | * Docstring
        | */
        |object a {
        |  // maps foo
        |  def bar = 2
        |
        |  // maps foo
        |  @deprecated() // bar
        |  def foo = List(1 /* barr */) map /* buzz */foo
        |
        |
        |  /**
        |   * Docstring
        |   */
        |  def qux = 1
        |}
      """.stripMargin.parse[Source].get
    val comments = AssociatedComments(code)
    val done = mutable.Set.empty[Token]

    val attached = code.transform {
      case t =>
        val leading = comments.leading(t).filterNot(done)
        val trailing = comments.trailing(t).filterNot(done)
        done ++= leading
        done ++= trailing
        t.withComments(leading.toList, trailing.toList)
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
      case q"2" => q"42"
      case Term.ApplyInfix(lhs, op, Nil, args) => q"$lhs.$op(..$args)"
    }

    visit(code)
    visit(attached)
    visit(transformed)

    val obtained = TreePrinter.print(transformed).render(100)
    println(obtained)

  }

}
