package net.stoerr.grokconstructor.automatic

import net.stoerr.grokconstructor.automatic.AutomaticDiscoveryView.{FixedString, NamedRegex, RegexPart}
import net.stoerr.grokconstructor.webframework.{WebView, WebViewWithHeaderAndSidebox}
import net.stoerr.grokconstructor.{GrokPatternLibrary, JoniRegex, RandomTryLibrary, StartMatch}
import javax.servlet.http.HttpServletRequest
import scala.collection.MapView
import scala.xml.NodeSeq

/**
 * We try to find all sensible regular expressions consisting of grok patterns and fixed strings that
 * match all of a given collection of lines. The algorithm is roughly: in each step we look whether the first characters
 * of all rest-lines are equal and are not letters/digits. If they are, we take that for the regex. If they aren't we try to match all grok
 * regexes against the string. The regexes are partitioned into sets that match exactly the same prefixes of all
 * rest-lines, sort these according to the average length of the matches and try these.
 * @author <a href="http://www.stoerr.net/">Hans-Peter Stoerr</a>
 * @since 08.03.13
 */
class AutomaticDiscoveryView(val request: HttpServletRequest) extends WebViewWithHeaderAndSidebox {

  lazy val namedRegexps: Map[String, JoniRegex] = form.grokPatternLibrary.map {
    case (name, regex) => name -> JoniRegex(GrokPatternLibrary.replacePatterns(regex, form.grokPatternLibrary))
  }
  lazy val namedRegexpsList: List[(String, JoniRegex)] = namedRegexps.toList
  override val title: String = "Automatic grok discovery"
  val form: AutomaticDiscoveryForm = AutomaticDiscoveryForm(request)
  /** We try at most this many calls to avoid endless loops because of
    * the combinatorical explosion */
  var callCountdown = 1000

  override def action: String = AutomaticDiscoveryView.path

  def maintext: NodeSeq = <p>
    This was
    <a href="http://www.stoerr.net/">my</a>
    first attempt to support creating <a href="http://logstash.net/docs/latest/filters/grok">grok expressions</a>.
    It generates potentially all regular expressions that consist of fixed strings for things that are not alphanumeric and grok patterns from the library, and match all of a given
    set of logfile lines. If there are several patterns from the grok library that match the same strings in every log line they are grouped together and presented as a drop down list.
    Unfortunately, the number of possible regular expressions grows exponentially with the length of the lines, such that this is not really usable in practice. Thus, the result list is cut
    off at 200 results. <a href="http://en.wiktionary.org/wiki/your_mileage_may_vary">YMMV</a>. </p> ++ <p>
    Please enter some loglines for which you want generate possible grok patterns and then press</p> ++
    submit("Go!")

  def sidebox: NodeSeq = <p>You can also just try this out with a</p> ++ buttonanchor(AutomaticDiscoveryView.path + "?randomize", "random example")

  if (null != request.getParameter("example")) {
    val trial = RandomTryLibrary.example(request.getParameter("example").toInt)
    form.loglines.value = Some(trial.loglines)
    form.multilineRegex.value = trial.multiline
    form.multilineNegate.values = List(form.multilineNegate.name)
    form.groklibs.values = List("grok-patterns")
  }

  def formparts: NodeSeq = form.loglinesEntry ++ form.grokpatternEntry

  override def result: NodeSeq = {
    val linesOpt = form.multilineFilter(form.loglines.valueSplitToLines).toList
    resultTable(matchingRegexpStructures(linesOpt))
  }

  override def doforward: Option[Either[String, WebView]] = if (null == request.getParameter("randomize")) None
  else Some(Left(fullpath(AutomaticDiscoveryView.path) + "?example=" + RandomTryLibrary.randomExampleNumber()))

  def resultTable(results: Iterator[List[RegexPart]]): xml.Node = table(
    rowheader("At most 200 possible grok regex combinations that match all lines") ++ results.take(200).toList.map {
      result =>
        row(result map {
          case FixedString(str) => <span>
            {'»' + str + '«'}
          </span>
          case NamedRegex(patterns) if patterns.size == 1 => <span>
            {"%{" + patterns.head + "}"}
          </span>
          case NamedRegex(patterns) => <select>
            {patterns.sorted map {
              pattern => <option>
                {"%{" + pattern + "}"}
              </option>
            }}
          </select>
        })
    })

  def matchingRegexpStructures(lines: List[String]): Iterator[List[RegexPart]] = {
    if (callCountdown <= 0) return Iterator(List(FixedString("SEARCH TRUNCATED")))
    callCountdown -= 1
    if (lines.forall(_.isEmpty)) return Iterator(List())
    val commonPrefix = AutomaticDiscoveryView.biggestCommonPrefixExceptDigitsOrLetters(lines)
    if (0 < commonPrefix.length) {
      val restlines = lines.map(_.substring(commonPrefix.length))
      matchingRegexpStructures(restlines).map(FixedString(commonPrefix) :: _)
    } else {
      val regexpand = for ((name, regex) <- namedRegexpsList) yield (name, lines.map(regex.matchStartOf))
      val candidatesThatMatchAllLines = regexpand.filter(!_._2.exists(_.isEmpty))
      val candidates = candidatesThatMatchAllLines.filterNot(!_._2.exists(_.get.length > 0))
      val candidateToMatches = candidates.map {
        case (name, matches) => (name, matches.map(_.get))
      }
      val candidatesGrouped: MapView[List[StartMatch], List[String]] = candidateToMatches.groupBy(_._2).view.mapValues(_.map(_._1))
      val candidatesSorted = candidatesGrouped.toList.sortBy(-_._1.map(_.length).sum)
      val res = for ((matches, names) <- candidatesSorted) yield {
        val restlines = matches.map(_.rest)
        matchingRegexpStructures(restlines).map(NamedRegex(names) :: _)
      }
      res.fold(Iterator())(_ ++ _)
    }
  }

}

object AutomaticDiscoveryView {

  val path = "/automatic"

  /** The longest string that is a prefix of all lines. */
  def biggestCommonPrefixExceptDigitsOrLetters(lines: List[String]): String =
    if (lines.size != 1) lines.reduce(commonPrefixExceptDigitsOrLetters)
    else wrapString(lines.head).takeWhile(!_.isLetterOrDigit).unwrap

  def commonPrefixExceptDigitsOrLetters(str1: String, str2: String): String =
    wrapString(str1).zip(wrapString(str2)).takeWhile(p => p._1 == p._2 && !p._1.isLetterOrDigit).map(_._1).mkString("")

  sealed trait RegexPart

  case class FixedString(str: String) extends RegexPart

  case class NamedRegex(regexps: List[String]) extends RegexPart

}
