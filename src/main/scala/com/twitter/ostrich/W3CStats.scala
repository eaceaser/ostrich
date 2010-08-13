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

import net.lag.logging.Logger
import scala.collection.mutable
import com.twitter.xrayspecs.Time
import java.util.Date
import java.util.zip.CRC32
import java.net.InetAddress
import java.text.SimpleDateFormat


/**
 * Implements a W3C Extended Log and contains convenience methods for timing blocks and
 * exporting those timings in the w3c log.
 *
 * @param fields The fields, in order, as they will appear in the final w3c log output.
 */
class W3CStats(val logger: Logger, val fields: Array[String]) {
  val log = Logger.get(getClass.getName)
  val reporter = new W3CReporter(logger)
  var complainAboutUnregisteredFields = true
  val fieldNames: Set[String] = Set.empty ++ fields

  /**
   * Coalesce all w3c events (counters, timings, etc.) that happen in this thread within this
   * transaction, and log them as a single line at the end. This is useful for logging everything
   * that happens within an HTTP request/response cycle, or similar.
   */
  def transaction[T](f: W3CEntry => T): T = {
    val entry = new W3CEntry(logger, fields)
    try {
      f(entry)
    } finally {
      entry.flush()
    }
  }
}
