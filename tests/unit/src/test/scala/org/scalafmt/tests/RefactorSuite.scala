package org.scalafmt.tests

import scala.meta._
import org.scalafmt.internal.TreePrinter


object RefactorSuite extends BaseScalaPrinterTest {

  test("basic") {
    pprint.log("HELLO!")
    val code =
      """
        |object a {
        |  def bar = 2
        |
        |  // maps foo
        |  def foo = List(1) map foo
        |}
      """.stripMargin.parse[Source].get


    val transformed = code.transform {
      case Term.ApplyInfix(lhs, op, Nil, args) =>
        q"$lhs.$op(..$args)"
    }

    val obtained = TreePrinter.print(transformed).render(100)

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
