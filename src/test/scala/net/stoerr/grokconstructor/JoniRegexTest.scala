package net.stoerr.grokconstructor

import org.scalatest.flatspec.AnyFlatSpec
import org.joni.Regex
import org.scalatestplus.junit.JUnitRunner
import org.junit.runner.RunWith
import org.scalatest.matchers.should.Matchers._

/**
 * @author <a href="http://www.stoerr.net/">Hans-Peter Stoerr</a>
 * @since 06.02.13
 */
@RunWith(classOf[JUnitRunner])
class JoniRegexTest extends AnyFlatSpec {

  def startsWith(s: String, r: Regex) = {
    val bytes = s.getBytes
    val matcher = r.matcher(bytes)
    matcher.`match`(0, bytes.length, 0)
  }

  val r1 = new Regex("a.*c")

  "A org.joni.Regex" should "find regex at the start and return the matched length" in {
    startsWith("abc", r1) should equal(3)
    startsWith("abcd", r1) should equal(3)
    startsWith("abblacdxxx", r1) should equal(6)
  }

  it should "not find a regex that matches somewhere else or not at all" in {
    startsWith("bcd", r1) should equal(-1)
    startsWith("cabcd", r1) should equal(-1)
  }

  it should "recognize names of groups for matches" in {
    val rnamed = new Regex("bla(?<foo>blu)bluf")
    val bytes = "hublablubluf".getBytes
    val matcher = rnamed.matcher(bytes)
    val found = matcher.search(0, bytes.length, 0)
    found should equal(2)
  }

  val rn = JoniRegex("a.*c")

  "JoniRegex.matchStartOf" should "return None if the regex is not found at the start" in {
    for (str <- List("cabcd", "bcd", ""))
      rn.matchStartOf(str) should equal(None)
  }

  it should "return the appropriate length, match and rest if the regex matches the start" in {
    rn.matchStartOf("abc") should equal(Some(StartMatch(length = 3, matched = "abc", rest = "")))
    rn.matchStartOf("acdfsdf") should equal(Some(StartMatch(2, "ac", "dfsdf")))
  }

  "JoniRegex.find" should "find a regex and return the correct values" in {
    val rnamed = new JoniRegex("la(?<foo>blu)bl")
    val jmatch = rnamed.findIn("blablubluf").get
    jmatch.before should equal("b")
    jmatch.after should equal("uf")
    jmatch.matched should equal("lablubl")
    jmatch.namedgroups should equal(Map("foo" -> "blu"))
  }

  it should "not find anything if it isn't there" in {
    val rnamed = new JoniRegex("la(?<foo>blu)bl")
    rnamed.findIn("nixda") should equal(None)
  }

  "JoniRegexQuoter.quote" should "turn a string into a regular expression that only matches the string" in {
    import JoniRegexQuoter.quote

    quote("bla") should equal("bla")
    val allspecialchars = "bla[.(|?*+{^$blu\\"
    quote(allspecialchars) should equal("bla\\[\\.\\(\\|\\?\\*\\+\\{\\^\\$blu\\\\")
    val matched = new JoniRegex(quote(allspecialchars)).matchStartOf(allspecialchars)
    matched.get.rest should equal("")
  }

}
