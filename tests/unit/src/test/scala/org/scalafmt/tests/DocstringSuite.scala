package org.scalafmt.tests

import org.scalafmt.internal.AssociatedTrivias
import scala.meta._
import scala.meta.internal.format.Comments._
import org.scalafmt.internal.TreePrinter

object DocstringSuite extends BaseScalaPrinterTest {
  test("docstring") {
    val foo = q"def foo = 2".withLeadingComment("/** Returns 2 */\n")
    val bar = q"def bar = 3".withTrailingComment(" // FIXME")
    val qux = q"def qux = 4".withLeadingComment(
      """/**
        |  * Example multiline docstring
        |  *
        |  * @param a The "a"
        |  */
        |""".stripMargin
    )
    val zzz = q"def zzz = 5"
      .withLeadingComment("// Comment 2\n") // Note: reverse order, change?
      .withLeadingComment("// Comment 1\n")
    val Foo =
      q"class Foo { $foo; $bar; $qux; $zzz }"
        .withLeadingComment("/** Foo is great */\n")
    val source = source"""
package a.b
import b.c
$Foo
"""
    val obtained = TreePrinter.print(source).render(100)
    // Note that indentation is corrected :D
    val expected =
      """
        |package a.b
        |import b.c
        |
        |/** Foo is great */
        |class Foo {
        |  /** Returns 2 */
        |  def foo = 2
        |
        |  def bar = 3 // FIXME
        |
        |  /**
        |    * Example multiline docstring
        |    *
        |    * @param a The "a"
        |    */
        |  def qux = 4
        |
        |  // Comment 1
        |  // Comment 2
        |  def zzz = 5
        |}
      """.stripMargin
    assertNoDiff(obtained, expected)
  }

  test("existing") {
    val tree =
      """
        |package a
        |
        |/** This is a docstring
        |  *
        |  * @param a is an int
        |  */
        |case class Foo(/* aaaa */ a: Int) { // trailing
        |
        |  /** This is a method */
        |  def d = a
        |}
      """.stripMargin.parse[Source].get
    val trivia = AssociatedTrivias(tree)
    val syntheticMethod =
      q"def b: Int = a".withLeadingComment("/** Returns a */\n")
    val syntheticMethod2 =
      q"def c: Int = a".withLeadingComment("/** Returns a again */\n")
    val transformed = tree.transform {
      case c: Defn.Class =>
        c.copy(
          templ = c.templ.copy(
            stats = c.templ.stats ++ List(syntheticMethod, syntheticMethod2)
          )
        )
    }
    val obtained = TreePrinter.print(transformed, trivia).render(100)
    println(obtained)

    // package a
    //
    // /** This is a docstring
    //  *
    //  * @param a is an int
    //  */
    // case class Foo(a: Int) {
    //   /** Returns a */
    //   def b: Int = a
    //
    //   /** Returns a again */
    //   def c: Int = a
    // }
  }

}
