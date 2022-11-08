package net.stoerr.grokconstructor

import automatic.AutomaticDiscoveryView
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import AutomaticDiscoveryView._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers._

/**
 * Guess what: verifies algorithms in AutomaticDiscoveryView.
 * @author <a href="http://www.stoerr.net/">Hans-Peter Stoerr</a>
 * @since 07.02.13
 */
@RunWith(classOf[JUnitRunner])
class AutomaticDiscoveryViewTest extends AnyFlatSpec {

  "AutomaticDiscoveryView.commonPrefixExceptDigitsOrLetters" should "return the common prefix of two strings" in {
    commonPrefixExceptDigitsOrLetters("$#@@#$@", "$#%@#$") should equal("$#")
    commonPrefixExceptDigitsOrLetters("!@#", "#@!") should equal("")
    commonPrefixExceptDigitsOrLetters("", "$#$") should equal("")
    commonPrefixExceptDigitsOrLetters("", "") should equal("")
  }

  "AutomaticDiscoveryView.biggestCommonPrefixExceptDigitsOrLetters" should "return the biggest common prefix" in {
    biggestCommonPrefixExceptDigitsOrLetters(List("#@!%$%", "#@!&$*", "#@!*#&")) should equal("#@!")
    biggestCommonPrefixExceptDigitsOrLetters(List("!@#", "$#@")) should equal("")
    biggestCommonPrefixExceptDigitsOrLetters(List("$#@$bla")) should equal("$#@$")
  }

  "AutomaticDiscoveryView.matchingRegexpStructures" should "suggest all pattern combinations" in {
    /* val regexes = Map("A" -> "a|b", "B" -> "b|c", "S" -> "\\s*")
    val d = new AutomaticDiscoveryView(regexes)
    d.matchingRegexpStructures(List("-")).toList should equal(List(List(FixedString("-"))))
    d.matchingRegexpStructures(List("a", "b")).toList should equal(List(List(NamedRegex(List("A")))))
    d.matchingRegexpStructures(List("-a", "-b")).toList should equal(List(List(FixedString("-"), NamedRegex(List("A")))))
    d.matchingRegexpStructures(List("-a ", "-b")).toList should equal(List(List(FixedString("-"), NamedRegex(List("A")), NamedRegex(List("S")))))
    d.matchingRegexpStructures(List(" -a ", "-b")).toList should equal(List(List(NamedRegex(List("S")), FixedString("-"), NamedRegex(List("A")), NamedRegex(List("S")))))
    */
  }

}
