package org.scalafmt.tests

import org.typelevel.paiges.Doc._

object HardLineSuite extends BaseScalaPrinterTest {
  test("line") {
    assert(line.flatten.isDefined)
    assertNoDiff((text("-") + line).grouped.render(2), "- ")
    assertNoDiff((text("-") + line).nested(2).grouped.render(1), "-\n   ")
  }

  test("noFlat") {
    assert(lineNoFlat.flatten.isEmpty)
    assertNoDiff((text("-") + lineNoFlat).grouped.render(2), "-\n")
    assertNoDiff((text("-") + lineNoFlat).nested(2).grouped.render(2), "-  \n")
  }

  test("noFlatNoIndent") {
    assert(lineNoFlatNoIndent.flatten.isEmpty)
    assertNoDiff(
      (text("-") + lineNoFlatNoIndent).nested(2).grouped.render(2),
      "-\n"
    )
  }
}
