scala-syntax
[![Travis Build Status](https://travis-ci.org/scalacenter/scala-syntax.svg?branch=master)](https://travis-ci.org/scalacenter/scala-syntax)
[![AppVeyor Build status](https://ci.appveyor.com/api/projects/status/ys1uejcy2y2tgamf/branch/master?svg=true)](https://ci.appveyor.com/project/scalacenter/scala-syntax/branch/master)
[![codecov.io](http://codecov.io/github/scalacenter/scala-syntax/coverage.svg?branch=master)](http://codecov.io/github/scalacenter/scala-syntax?branch=master)
[![Join the chat at https://gitter.im/scalacenter/scalafix](https://badges.gitter.im/scala-syntax.svg)](https://gitter.im/scala-syntax)
========

# Scalameta tree pretty printer

Pretty printer for [Scalameta](http://scalameta.org/) trees using [Paiges](http://github.com/typelevel/paiges).

Improves the built-in Scalameta pretty printer (`Tree.syntax`) with the following properties:

* [X] Correctness: Handling of precedence rules, inserting parentheses where necessary.
* [ ] Trivia Preserving: ability to preserve comments and other syntactic elements when doing tree transformation.
* [X] Line Wrapping: large expressions don't appear in a single line with hundreds of columns.
* [ ] Better Performance

The end goal of this project is to enable a more powerful refactoring API for [Scalafix](https://scalacenter.github.io/scalafix/).
Currently, Scalafix rewrites are implemented using a fairly low-level token API, which is error-prone.

## Team

The current maintainers (people who can merge pull requests) are:

- Guillaume Massé - [`@MasseGuillaume`](https://github.com/MasseGuillaume)
- Ólafur Páll Geirsson - [`@olafurpg`](https://github.com/olafurpg)

## Codegen

It's possible to use scala-syntax for code generation as a replacement for the
Scalameta built-in `Tree.syntax` method.  The main benefit of using
scala-syntax over the built-in `.syntax` is that that docstrings are preserved
after `Tree.transform`.
To use scala-syntax for code generation in a sbt project `myCodeGeneration`

```
// build.sbt
lazy val Syntax = RootProject(
  uri(
    "git://github.com/olafurpg/scala-syntax.git#681a291548197289350e854f0770aa912ed4d908"
  )
)
lazy val syntax = ProjectRef(Syntax.build, "format")

lazy val myCodeGeneration = project
  .settings(...)
  .dependsOn(syntax)
```

Then, in your pipeline

1. parse the original tree that may contain docstrings
1. collect the `AssociatedTrivias` of the original tree before transforming it
1. transform the tree with `Tree.transform`
1. print the transformed tree with `TreePrinter.print(tree, trivia)`

In code, this will look roughly like this
```scala
import org.scalafmt.internal.AssociatedTrivias
import scala.meta._
import scala.meta.internal.format.Comments._
import org.scalafmt.internal.TreePrinter

val originalTree =
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
val trivia = AssociatedTrivias(originalTree)
val syntheticMethod =
  q"def b: Int = a".withLeadingComment("/** Returns a */\n")
val syntheticMethod2 =
  q"def c: Int = a".withLeadingComment("/** Returns a again */\n")
val transformedTree = originalTree.transform {
  case c: Defn.Class =>
    c.copy(
      templ = c.templ.copy(
        stats = c.templ.stats ++ List(syntheticMethod, syntheticMethod2)
      )
    )
}

val obtained = TreePrinter.print(transformedTree, trivia).render(100)
println(obtained)
// package a
//
// /** This is a docstring
//  *
//  * @param a is an int
//  */
// case class Foo(a: Int) {
//
//   /** This is a method */
//   def d = a
//
//   /** Returns a */
//   def b: Int = a
//
//   /** Returns a again */
//   def c: Int = a
// }
```
