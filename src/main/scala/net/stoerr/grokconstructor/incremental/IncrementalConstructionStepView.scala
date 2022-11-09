package net.stoerr.grokconstructor.incremental

import com.google.apphosting.api.DeadlineExceededException

import javax.servlet.http.HttpServletRequest
import net.stoerr.grokconstructor.matcher.MatcherEntryView
import net.stoerr.grokconstructor.webframework.{WebView, WebViewWithHeaderAndSidebox}
import net.stoerr.grokconstructor.{GrokPatternLibrary, JoniRegex, JoniRegexQuoter, RandomTryLibrary, TimeoutException}
import org.joni.exception.SyntaxException

import scala.collection.immutable.NumericRange
import scala.xml.{Elem, NodeSeq, Text}

/**
  * Performs a step in the incremental construction of the grok pattern.
  *
  * @author <a href="http://www.stoerr.net/">Hans-Peter Stoerr</a>
  * @since 02.03.13
  */
class IncrementalConstructionStepView(val request: HttpServletRequest) extends WebViewWithHeaderAndSidebox {

  override val title: String = "Incremental Construction of Grok Patterns in progress"
  val form: IncrementalConstructionForm = IncrementalConstructionForm(request)
  val currentRegex: String = form.constructedRegex.value.getOrElse("\\A") + getNamedNextPartOrEmpty
  val currentJoniRegex: JoniRegex = JoniRegex(GrokPatternLibrary.replacePatterns(currentRegex, form.grokPatternLibrary))
  val logLines: Seq[String] = form.multilineFilter(form.loglines.valueSplitToLines).toIndexedSeq
  val loglinesSplitted: Seq[(String, String)] = logLines.map({
    line =>
      val jmatch = currentJoniRegex.matchStartOf(line)
      (jmatch.get.matched, jmatch.get.rest)
  })
  val loglineRests: Seq[String] = loglinesSplitted.map(_._2)
  val constructionDone: Boolean = loglineRests.forall(_.isEmpty)

  val groknameToMatches: List[(String, List[String])] = try {
    for {
      grokname <- form.grokPatternLibrary.keys.toList
      regex = JoniRegex(GrokPatternLibrary.replacePatterns("%{" + grokname + "}", form.grokPatternLibrary))
      restlinematchOptions = loglineRests.map(regex.matchStartOf)
      if !restlinematchOptions.exists(_.isEmpty)
      /* In some cases a suggestion matches the rest of the line, but not as a continuation for the full line.
        * For example: \\tb with current regex a\\t has restline b , which matches %{WORD} but that has a word boundary. */
      newregex = new JoniRegex(currentJoniRegex.regex + regex.regex)
      if logLines.map(newregex.matchStartOf).forall(_.isDefined)
      restlinematches: List[String] = restlinematchOptions.map(_.get.matched).toList
    } yield (grokname, restlinematches)
  } catch {
    case timeoutException @ (_ : DeadlineExceededException | _ : InterruptedException)  =>
      throw new TimeoutException("Timeout executing the search for the next pattern.\n" +
        "Number one recommendation is to input more and more diverse log lines, which should all be matched by the pattern, into the log lines field." +
        "That restricts the search space - the more the better (within reasonable limits, of course).", timeoutException)
  }

  // TODO missing: add extra patterns by hand later
  /** List of pairs of a list of groknames that have identical matches on the restlines to the list of matches. */
  val groknameListToMatches: List[(List[String], List[String])] = groknameToMatches.groupBy(_._2).map(p => (p._2.map
  (_._1), p._1)).toList
  form.constructedRegex.value = Some(currentRegex)
  /** groknameListToMatches that have at least one nonempty match, sorted by the sum of the lengths of the matches. */
  val groknameListToMatchesCleanedup: List[(List[String], List[String])] = groknameListToMatches.filter(_._2.exists(!_.isEmpty)).sortBy(-_._2.map(_
    .length).sum)

  private val syntaxErrorInNextPartPerHand: Option[String] = try {
    JoniRegex(form.nextPartPerHand.value.getOrElse(""))
    None
  } catch {
    case patternSyntaxException: SyntaxException =>
      Some(patternSyntaxException.getMessage)
  }

  form.nameOfNextPart.value = None // reset for next form display
  form.nextPartPerHand.value = None

  override def action: String =
    if (!constructionDone) IncrementalConstructionStepView.path
    else MatcherEntryView.path

  override def doforward: Option[Either[String, WebView]] =
    if (null != request.getParameter("randomize"))
      Some(Left(IncrementalConstructionInputView.path + "?example=" + RandomTryLibrary.randomExampleNumber()))
    else if (null != request.getParameter("matchrests")) {
      val view = new MatcherEntryView(request)
      view.form.loglines.value = Some(loglineRests.mkString("\n"))
      Some(Right(view))
    } else None

  def maintext: NodeSeq = if (!constructionDone) <p>Please select the next component for the grok pattern.
    You can select can either select a fixed string (e.g. a separator), a (possibly named) pattern from the grok
    pattern library, or a pattern you explicitly specify.
    You can use the browser's back button to retry (using form resubmission).
    Make your selection and press</p> ++ submit("Continue!")
  else <p>All log lines are successfully matched. You can copy the regular expression from the form field below.
    You can also try out the constructed regex by calling the matcher.</p> ++ submit("Go to matcher")

  def sidebox: NodeSeq = <p>To try out how regular expressions on the unmatched rests press</p> ++ submit("Match " +
    "restlines!", "matchrests", "_blank")

  override def result: NodeSeq = <span/>

  def formparts: NodeSeq = form.constructedRegex.inputText("Constructed regular expression so far: ", 180, enabled =
    false) ++
    form.loglines.hiddenField ++
    form.constructedRegex.hiddenField ++
    form.grokhiddenfields ++
    form.multilinehiddenfields ++
    (if (syntaxErrorInNextPartPerHand.isDefined)
      <p class="box error">Syntax error in the handmade regex:
        <br/>{syntaxErrorInNextPartPerHand.get}
      </p>
    else <span/>) ++
    selectionPart

  def selectionPart: NodeSeq = {
    val alreadymatchedtable = table(
      rowheader2("Already matched", "Unmatched rest of the loglines to match") ++
        loglinesSplitted.map {
          case (start, rest) => row2(<code>
            {start}
          </code>, <code>
            {visibleWhitespaces(rest)}
          </code>)
        }
    )
    if (!constructionDone) {
      alreadymatchedtable ++
        formsection("To choose a continuation of your regular expression you can either choose a fixed string that is" +
          " common to all log file line rests as a separator:") ++
        <div class="ym-fbox-check">
          {commonprefixesOfLoglineRests.map(p => form.nextPart.radiobutton(JoniRegexQuoter.quote(p), <code>
          {'»' + visibleWhitespaces(p) + '«'}
        </code>)).reduceOption(_ ++ _).getOrElse(<span/>)}
        </div> ++
        formsection("or select one of the following expressions from the grok library that matches a segment of the " +
          "log lines:") ++
        form.nameOfNextPart.inputText("Optional: give name for the grok expression to retrieve it's match value", 20,
          1) ++
        table(
          rowheader2("Grok expression", "Matches at the start of the rest of the loglines") ++
            groknameListToMatchesCleanedup.map(grokoption)) ++
        formsection("or you can input a regex that will match the next part of all logfile lines:") ++
        <div class="ym-fbox-check">
          {form.nextPart.radiobutton(form.nextPartPerHandMarker, "continue with handmade regex")}
        </div> ++
        form.nextPartPerHand.inputText("regular expression for next component:", 170)
    } else alreadymatchedtable
  }

  private def commonprefixesOfLoglineRests: Iterator[String] = {
    val biggestprefix = biggestCommonPrefix(loglineRests)
    NumericRange.inclusive(1, biggestprefix.length, 1).iterator
      .map(biggestprefix.substring(0, _))
  }

  /** The longest string that is a prefix of all lines. */
  private def biggestCommonPrefix(lines: Seq[String]): String =
  if (lines.size > 1) lines.reduce(commonPrefix) else lines.head

  // unfortunately wrapString collides with TableMaker.stringToNode , so we use it explicitly
  private def commonPrefix(str1: String, str2: String) = wrapString(str1).zip(wrapString(str2)).takeWhile(p => (p._1
    == p._2)).map(_._1).mkString("")

  def grokoption(grokopt: (List[String], List[String])): Elem = grokopt match {
    case (groknames, restlinematches) =>
      row2(
        <div class="ym-fbox-check">
          {groknames.sorted.map(grokname =>
          form.nextPart.radiobutton("%{" + grokname + "}", <code/>.copy(child = new Text("%{" + grokname + "}")),
            form.grokPatternLibrary(grokname)
          ))
          .reduce(_ ++ _)}
        </div>, <pre/>.copy(child = new Text(visibleWhitespaces(restlinematches.mkString("\n"))))
      )
  }

  private def getNamedNextPartOrEmpty = {
    val nextPart = form.nextPart.value.getOrElse("")
    if (nextPart == form.nextPartPerHandMarker) {
      try {
        JoniRegex(form.nextPartPerHand.value.getOrElse("")).regex
      } catch {
        case _: SyntaxException =>
          ""
      }
    }
    else form.nameOfNextPart.value match {
      case None => nextPart
      case Some(name) => nextPart.replaceFirst( """^%\{(\w+)}$""", """%{$1:""" + name + "}")
    }
  }

}

object IncrementalConstructionStepView {
  val path = "/constructionstep"
}
