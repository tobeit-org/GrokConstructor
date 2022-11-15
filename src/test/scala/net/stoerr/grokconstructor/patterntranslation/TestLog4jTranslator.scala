package net.stoerr.grokconstructor.patterntranslation

import net.stoerr.grokconstructor.patterntranslation.Log4jTranslator
import org.apache.logging.log4j.{Level, LogManager}
import org.apache.logging.log4j.ThreadContext.ContextStack
import org.apache.logging.log4j.core.LogEvent
import org.apache.logging.log4j.core.appender.ConsoleAppender
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory
import org.apache.logging.log4j.core.impl.Log4jLogEvent
import org.apache.logging.log4j.core.layout.PatternLayout
import org.apache.logging.log4j.core.net.Priority
import org.apache.logging.log4j.core.config.Configurator
import org.apache.logging.log4j.core.Logger
import org.apache.logging.log4j.message.{Message, SimpleMessage}
import org.apache.logging.log4j.core.impl.ContextDataFactory
import org.apache.logging.log4j.util.StringMap

import java.text.SimpleDateFormat
import java.util
import java.util.Date
import org.junit.runner.RunWith
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatestplus.junit.JUnitRunner

/**
 * @author <a href="http://www.stoerr.net/">Hans-Peter Stoerr</a>
 * @since 16.02.2015
 */
@RunWith(classOf[JUnitRunner])
class TestLog4jTranslator extends AnyFlatSpec {

  "Log4jTranslator" should "recognize conversion specifiers" in {
    def m(p: String) = p match {
      case Log4jTranslator.conversionSpecifier(leftjust, minwidth, maxwidth, name, argument) =>
        (leftjust, minwidth, maxwidth, name, argument)
    }
    assert(m("%c") ==(null, null, null, "c", null))
    assert(m("%20c") ==(null, "20", null, "c", null))
    assert(m("%-20c") ==("-", "20", null, "c", null))
    assert(m("%.30c") ==(null, null, "30", "c", null))
    assert(m("%20.30c") ==(null, "20", "30", "c", null))
    assert(m("%-20.30c") ==("-", "20", "30", "c", null))
    assert(m("%d{bla}") ==(null, null, null, "d", "bla"))
  }

  // format_modifiers = [left_justification_flag][minimum_field_width][.][maximum_field_width]
  // left_justification_flag = - for left justification (pad on the right) , not present -> right justification (pad on the left)
  // Bsp: %20c, %-20c , %.30c, %20.30c, %-20.30c
  it should "observe alignments" in {
    assert( """%{LOGLEVEL:loglevel}""" == Log4jTranslator.translate("%p"))
    assert( """ *%{LOGLEVEL:loglevel}""" == Log4jTranslator.translate("%20p"))
    assert( """%{LOGLEVEL:loglevel} *""" == Log4jTranslator.translate("%-20p"))
    assert( """%{LOGLEVEL:loglevel}""" == Log4jTranslator.translate("%.30p"))
    assert( """ *%{LOGLEVEL:loglevel}""" == Log4jTranslator.translate("%20.30p"))
    assert( """%{LOGLEVEL:loglevel} *""" == Log4jTranslator.translate("%-20.30p"))
  }

  it should "translate patterns" in {
    assert( """bla%{LOGLEVEL:loglevel}blu\{\}\[\]\(\)\|""" == Log4jTranslator.translate("bla%pblu{}[]()|"))
    assert( """(?<timestamp>%{TIMESTAMP_ISO8601}) %{LOGLEVEL:loglevel} * \[(?<logger>[A-Za-z0-9$_.]+) *\] (%{NOTSPACE:sessionId})? * (%{NOTSPACE:requestId})? - %{GREEDYDATA:message}$""" == Log4jTranslator.translate("%d{ISO8601} %-5.5p [%-30c{1}] %-32X{sessionId} %X{requestId} - %m%n"))
    assert( """(?<timestamp>%{TIMESTAMP_ISO8601}) %{LOGLEVEL:loglevel} * \[(?<logger>[A-Za-z0-9$_.]+) *\] (%{NOTSPACE:sessionId})? * - %{GREEDYDATA:message}$""" == Log4jTranslator.translate("%d{ISO8601} %-5.5p [%-30c{1}] %-32X{sessionId} - %m%n"))
  }

  it should "translate dateformats" in {
    assert( """(?<timestamp>%{YEAR}-%{MONTHNUM2}-%{MONTHDAY} %{HOUR}:%{MINUTE}:%{SECOND})""" == Log4jTranslator.translate("%d{yyyy-MM-dd HH:mm:ss}"))
    assert( """(?<timestamp>%{YEAR}-%{MONTHNUM2}-%{MONTHDAY} %{HOUR}:%{MINUTE}:%{SECOND}.%{NONNEGINT})""" == Log4jTranslator.translate("%d{YYYY-MM-dd HH:mm:ss.SSS}"))
  }

  // format_modifiers = [left_justification_flag][minimum_field_width][.][maximum_field_width]
  // left_justification_flag = - for left justification (pad on the right) , not present -> right justification (pad on the left)
  // Bsp: %20c, %-20c , %.30c, %20.30c, %-20.30c
  "log4j PatternLayout" should "correctly treat alignments" in {
    assert("INFO" == formatPriority("%p"))
    assert(" INFO" == formatPriority("%5p"))
    assert("INFO " == formatPriority("%-5p"))
    assert("INFO" == formatPriority("%.5p"))
    assert(" INFO" == formatPriority("%5.5p"))
    assert("INFO " == formatPriority("%-5.5p"))
  }

  private def formatPriority(pattern: String): String = {
    val layout = PatternLayout.newBuilder().withPattern(pattern).build()
    
    val event2 = Log4jLogEvent.newBuilder()
      .setLevel(Level.INFO)
      .setLoggerName("bla")
      .build()

    val formattedMessage = layout.toSerializable(event2)
    formattedMessage
  }

  "log4j PatternLayout" should "format messages" in {
    // val layout = new PatternLayout("%d{dd.MM.yyyy HH:mm:ss,SSS} - [%-5p] %c %X{sid} %m")
    val pattern = "%d{dd.MM.yyyy HH:mm:ss,SSS} - [%-5p] %c %X{sid} %m"
    val layout = PatternLayout.newBuilder().withPattern(pattern).build()
    // val mdc = new util.HashMap[String, String]()
    val mdc = ContextDataFactory.createContextData
    mdc.putValue("sid", "83k238d2")
    mdc.putValue("rid", "83482")
    val dateString = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss,SSS").format(new Date(1424339008197L))
    val message = new SimpleMessage("this is the message")
    val classe = getClass.toString
    val event3 = Log4jLogEvent.newBuilder()
      .setLevel(Level.INFO)
      .setLoggerFqcn(classe)
      .setLoggerName(LogManager.getLogger().getName)
      .setThreadName("main")
      .setMessage(message)
      .setThrown(new Throwable(new Exception("whatever")))
      .setTimeMillis(1424339008197L)
      .setContextData(mdc)
      .build()
    val formatted = layout.toSerializable(event3)
    // val event = new LoggingEvent(getClass.toString, Logger.getLogger(getClass), 1424339008197L, Level.INFO, "this is the message",
    //  "main", new ThrowableInformation(new Exception("whatever")), "theNdc", null, mdc)
    assert(dateString + " - [INFO ] net.stoerr.grokconstructor.patterntranslation.TestLog4jTranslator 83k238d2 this is the message" == formatted)
  }

}
