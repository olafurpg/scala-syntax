package org.scalafmt.tests

import scala.meta.internal.format.CustomTrees
import scala.meta._
import org.scalafmt.Format
import org.scalafmt.internal.TreePrinter

object DocstringSuite extends BaseScalaPrinterTest {
  test("docstring is printed") {
    val code = CustomTrees.Docstring("this is a comment", q"def a = 2")
    val toFormat = q"class A { $code; def b = 3 }"
    val obtained = TreePrinter.print(toFormat).render(100)
    assertNoDiff(obtained, "")
  }
}
