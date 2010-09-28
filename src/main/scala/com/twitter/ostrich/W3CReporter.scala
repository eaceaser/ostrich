/*
 * Copyright 2009 Twitter, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.twitter.ostrich

import java.net.InetAddress
import java.text.SimpleDateFormat
import java.util.{Date, TimeZone}
import java.util.zip.CRC32
import scala.collection.Map
import scala.collection.mutable
import scala.util.Sorting._
import com.twitter.xrayspecs.Time
import com.twitter.xrayspecs.TimeConversions._
import net.lag.logging.Logger


object W3CReporter {
  protected val formatter = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss")
  formatter.setTimeZone(TimeZone.getTimeZone("GMT+0000"))
}

/**
 * Log "w3c-style" lines to a java logger, using a map of key/value pairs. On each call to
 * `report`, if the keys in the map have changed, or it's been "a while" since the header was
 * last logged, the header is logged again.
 */
class W3CReporter(logger: Logger, private var keys: Iterable[String], printHeader: Boolean, printCrc: Boolean) extends StatsReporter {
  import W3CReporter._
  private def fieldsHeader = keys.mkString("#Fields: ", " ", "")

  def this(logger: Logger, keys: Iterable[String]) = this(logger, keys, false, false)
  def this(logger: Logger, keys: Iterable[String], printHeader: Boolean) = this(logger, keys, printHeader, false)

  /**
   * The W3C header lines will be written out this often, even if the fields haven't changed.
   * (This lets log parsers resynchronize after a failure.)
   */
  var headerRepeatFrequencyInMilliseconds = 5 * 60 * 1000

  var nextHeaderDumpAt = Time.now

  private var crc = crc32(fieldsHeader)
  private var previousCrc = 0L

  def setColumns(cols: Iterable[String]) {
    keys = cols
    crc = crc32(fieldsHeader)
  }

  /**
   * Write a W3C stats line to the log. If the field names differ from the previously-logged line,
   * a new header block will be written.
   */
  def report(stats: StatsProvider) {
//    val fieldsHeader = keys.mkString("#Fields: ", " ", "")
//    val crc = crc32(fieldsHeader)

    if (printHeader && (crc != previousCrc || Time.now >= nextHeaderDumpAt)) {
      logHeader(fieldsHeader, crc)
      previousCrc = crc
    }

    logger.info(generateLine(keys, stats.toMap))
  }

  private def generateLine(keys: Iterable[String], stats: Map[String, Any]) = {
    val rv = keys.map { key => stats.get(key).map { stringify(_) }.getOrElse("-") }.mkString(" ")
    if (printCrc) crc + " " + rv else rv
  }

  private def logHeader(fieldsHeader: String, crc: Long) {
    val header =
      Array("#Version: 1.0", "\n",
            "#Date: ", formatter.format(new Date(Time.now.inMilliseconds)), "\n",
            "#CRC: ", crc.toString, "\n",
            fieldsHeader, "\n").mkString("")
    logger.info(header)
    nextHeaderDumpAt = headerRepeatFrequencyInMilliseconds.milliseconds.fromNow
  }

  private def crc32(header: String): Long = {
    val crc = new CRC32()
    crc.update(header.getBytes("UTF-8"))
    crc.getValue()
  }

  private def stringify(value: Any): String = value match {
    case s: String => s.replaceAll(" ", "_")
    case d: Date => formatter.format(d).replaceAll(" ", "_")
    case l: Long => l.toString()
    case i: Int => i.toString()
    case ip: InetAddress => ip.getHostAddress()
    case _ => "-"
  }
}
