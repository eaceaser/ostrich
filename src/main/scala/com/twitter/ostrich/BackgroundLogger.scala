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

import scala.collection.mutable
import com.twitter.xrayspecs.{Duration, Time}
import com.twitter.xrayspecs.TimeConversions._
import net.lag.logging.Logger


/**
 * Log all collected stats as "w3c-style" lines to a java logger at a regular interval.
 */
class BackgroundStatsLogger(val reporter: StatsReporter, val period: Duration, includeJvmStats: Boolean) 
  extends PeriodicBackgroundProcess("BackgroundStatsLogger", period) {
  def this(reporter: StatsReporter, period: Duration) = this(reporter, period, true)

  val collection = Stats.fork()
  def periodic() {
    val report = new mutable.HashMap[String, Any]

    if (includeJvmStats) {
      Stats.getJvmStats() foreach { case (key, value) => report("jvm_" + key) = value }
    }

/*    collection.getCounterStats(true) foreach { case (key, value) => report(key) = value }
    Stats.getGaugeStats(true) foreach { case (key, value) => report(key) = value }

    collection.getTimingStats(true) foreach { case (key, timing) =>
      report(key + "_count") = timing.count
      report(key + "_min") = timing.minimum
      report(key + "_max") = timing.maximum
      report(key + "_avg") = timing.average
      report(key + "_std") = timing.standardDeviation
    } */

    reporter.report(collection)
  }
}
