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


/**
 * A Timing collates durations of an event and can report
 * min/max/avg along with how often the event occurred.
 */
class Timing(useHistogram: Boolean) {
  def this() = this(true)

  lazy val log = Logger.get(getClass.getName)

  private var maximum = Math.MIN_INT
  private var minimum = Math.MAX_INT
  private var count: Int = 0
  private var histogram = if (useHistogram) Some(new Histogram()) else None
  private var mean: Double = 0.0
  private var partialVariance: Double = 0.0

  /**
   * Resets the state of this Timing. Clears the durations and counts collected so far.
   */
  def clear() = synchronized {
    maximum = Math.MIN_INT
    minimum = Math.MAX_INT
    count = 0
    histogram.foreach(_.clear())
  }

  /**
   * Adds a duration to our current Timing.
   */
  def add(n: Int): Long = synchronized {
    if (n > -1) {
      maximum = n max maximum
      minimum = n min minimum
      count += 1
      histogram.foreach(_.add(n))
      if (count == 1) {
        mean = n
        partialVariance = 0.0
      } else {
        val newMean = mean + (n - mean) / count
        partialVariance += (n - mean) * (n - newMean)
        mean = newMean
      }
    } else {
      log.warning("Tried to add a negative timing duration. Was the clock adjusted?")
    }
    count
  }

  /**
   * Add a summarized set of timings.
   */
  def add(timingStat: TimingStat): Long = synchronized {
    if (timingStat.count > 0) {
      // these equations end up using the sum again, and may be lossy. i couldn't find or think of
      // a better way.
      val newMean = (mean * count + timingStat.mean * timingStat.count) / (count + timingStat.count)
      partialVariance = partialVariance + timingStat.partialVariance +
        (mean - newMean) * mean * count +
        (timingStat.mean - newMean) * timingStat.mean * timingStat.count
      mean = newMean
      count += timingStat.count
      maximum = timingStat.maximum max maximum
      minimum = timingStat.minimum min minimum
      histogram.foreach(local => timingStat.histogram.foreach { h => local.merge(h) })
    }
    count
  }

  /**
   * Returns a TimingStat for the measured event.
   * @param reset whether to erase the current history afterwards
   */
  def get(reset: Boolean): TimingStat = synchronized {
    val rv = new TimingStat(count, maximum, minimum, histogram.map(_.clone()), mean, partialVariance)
    if (reset) clear()
    rv

  }
}
