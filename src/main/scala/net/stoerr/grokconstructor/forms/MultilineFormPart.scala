package net.stoerr.grokconstructor.forms

import net.stoerr.grokconstructor.{GrokPatternLibrary, JoniRegex}
import scala.xml.NodeSeq

/**
 * Form-Part that simulates http://logstash.net/docs/latest/filters/multiline .
 * We do only support what=>previous until someone asks for something different.
 * @author <a href="http://www.stoerr.net/">Hans-Peter Stoerr</a>
 * @since 26.02.13
 */
trait MultilineFormPart extends GrokPatternFormPart {

  /** If non empty, we will put the loglines through a
    * http://logstash.net/docs/latest/filters/multiline filter */
  val multilineRegex: InputText = InputText("multiline")

  private val negatekey = "negate"
  /** Whether to negate the multilineRegex: if false we will append
    * lines that do <em>not</em> match the filter, else we will append
    * lines that do match the filter. */
  val multilineNegate: InputMultipleChoice = InputMultipleChoice("multilinenegate", Map(negatekey -> <span>negate the multiline regex</span>), List())

  def multilineEntry: NodeSeq =
    multilineRegex.inputText("If you want to use logstash's multiline filter please specify the used pattern (can include grok Patterns):", 80) ++
      multilineNegate.checkboxes

  def multilinehiddenfields: NodeSeq = multilineRegex.hiddenField ++ multilineNegate.hiddenField

  def multilineFilter(lines: Seq[String]): Seq[String] = {
    if (multilineRegex.value.isEmpty || multilineRegex.value.get.isEmpty || lines.isEmpty) return lines
    val lineswithmatch: Seq[(Boolean, String)] = lines.map(l => (continuationLine(l), l))
    /** Partition in groups where each group starts with an item where _1 is true. */
    def group(currentgroup: List[String], list: List[(Boolean, String)]): List[List[String]] = list match {
      case (false, l) :: Nil if currentgroup.isEmpty => List(List(l))
      case (false, l) :: Nil => List(currentgroup, List(l))
      case (true, l) :: Nil => List(currentgroup ++ List(l))
      case (false, l) :: rest if currentgroup.isEmpty => group(List(l), rest)
      case (false, l) :: rest => currentgroup :: group(List(l), rest)
      case (true, l) :: rest => group(currentgroup ++ List(l), rest)
      case _ => List.empty
    }
    val linesgrouped = group(List(), lineswithmatch.toList)
    linesgrouped.map(_.reduce(_ + "\n" + _))
  }

  private def continuationLine(line: String): Boolean = {
    val regex = new JoniRegex(GrokPatternLibrary.replacePatterns(multilineRegex.value.get, grokPatternLibrary))
    val ismatched = regex.findIn(line).isDefined
    if (multilineNegate.values.contains(negatekey)) !ismatched else ismatched
  }

}
