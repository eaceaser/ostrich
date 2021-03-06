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
class TransactionalStats(val reporter: StatsReporter) {
  /**
   * Coalesce all w3c events (counters, timings, etc.) that happen in this thread within this
   * transaction, and log them as a single line at the end. This is useful for logging everything
   * that happens within an HTTP request/response cycle, or similar.
   */
  private val tl = new ThreadLocal[ThreadUnsafeStatsCollection]() {
    override def initialValue() = new ThreadUnsafeStatsCollection(1000)
  }

  def get = tl.get()
  def transaction[T](f: StatsProvider => T): T = {
    get.clearAll()
    try {
      f(get)
    } finally {
      reporter.report(get)
    }
  }
}
