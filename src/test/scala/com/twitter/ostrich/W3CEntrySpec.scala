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

import net.lag.extensions._
import net.lag.logging.{BareFormatter, Level, Logger, StringHandler}
import org.specs._
import scala.collection.immutable
import java.text.SimpleDateFormat
import java.util.Date


object W3CEntrySpec extends Specification {
  "w3c entries" should {
    val logger = Logger.get("w3c")
    logger.setLevel(Level.INFO)
    val handler = new StringHandler(BareFormatter)
    logger.addHandler(handler)
    logger.setUseParentHandlers(false)

    val w3c = new W3CEntry(logger, Array("backend-response-time", "backend-response-method", "request-uri", "backend-response-time_ns", "unsupplied-field", "finish_timestamp", "widgets", "wodgets"))

    def getFirstLine() = handler.toString.split("\n").toList.filter(!_.startsWith("#")).first

    doBefore {
      Stats.clearAll()
      handler.clear()
    }

    "starts life with an empty map" in {
      w3c.map.size mustEqual 0
    }

    "log and check a single timing" in {
      w3c.addTiming("backend-response-time", 57)
      w3c.flush
      handler.toString() must beMatching("57")
      handler.clear()
    }

    "flushing ensures that the entry is gone" in {
      w3c.addTiming("backend-response-time", 57)
      w3c.flush
      handler.clear()

      w3c.flush
      getFirstLine() mustNot beMatching("57")
    }

    "incr works with positive and negative numbers" in {
      w3c.incr("wodgets", 1)
      w3c.incr("wodgets")

      w3c.incr("widgets", 1)
      w3c.incr("widgets", -1)
      w3c.flush

      getFirstLine() must endWith("0 2")
    }

    "works with Strings" in {
      w3c.log("backend-response-time", "57")
      w3c.flush
      getFirstLine() must beMatching("57")
    }

    "rejects a column that isn't registered" in {
      w3c.incr("whatwhatlol", 100)
      getFirstLine() mustNot beMatching("100")
    }

    "start and end Timing" in {
      "flushing before ending a timing means it doesn't get logged" in {
        w3c.startTiming("backend-response-time")
        w3c.flush
        getFirstLine() must startWith("- ")
      }

      "end" in {
        w3c.startTiming("backend-response-time")
        w3c.endTiming("backend-response-time")
        w3c.flush
        getFirstLine() mustNot startWith("- ")
      }
    }
  }
}
