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


/*
object W3CStatsSpec extends Specification {
  "w3c Stats" should {
    val logger = Logger.get("w3c")
    logger.setLevel(Level.INFO)
    val handler = new StringHandler(BareFormatter)
    logger.addHandler(handler)
    logger.setUseParentHandlers(false)

    val w3c = new W3CStats(logger, Array("backend-response-time", "backend-response-method", "request-uri", "backend-response-time_ns", "unsupplied-field", "finish_timestamp", "widgets", "wodgets"))

    def getFirstLine() = handler.toString.split("\n").toList.filter(!_.startsWith("#")).first

    doBefore {
      Stats.clearAll()
      handler.clear()
    }

    "log and check some timings" in {
      w3c.transaction { w3c =>
        val response: Int = w3c.time[Int]("backend-response-time") {
          w3c.log("backend-response-method", "GET")
          w3c.log("request-uri", "/home")
          1 + 1
        }
        response mustEqual 2

        w3c.log("finish_timestamp", new Date(0))

        val response2: Int = w3c.timeNanos[Int]("backend-response-time_ns") {
          1 + 2
        }
        response2 mustEqual 3
      }
      val logline = getFirstLine()
      logline mustNot beNull

      val entries: Array[String] = logline.split(" ")
      entries(0).toInt must be_>=(0)
      entries(1) mustEqual "GET"
      entries(2) mustEqual "/home"
      entries(3).toInt must be_>=(10)  //must take at least 10 ns!
      entries(4) mustEqual "-"
      entries(5) mustEqual "01-Jan-1970_00:00:00"
    }

    "handle a transaction" in {
      w3c.transaction { w3c =>
        w3c.log("widgets", 8)
        w3c.log("wodgets", 3)
      }
      val logline = getFirstLine()
      logline.replaceAll(" ?- ?", "") mustEqual "8 3"
      false
    }

    "sum multiple counts within a transaction" in {
      w3c.transaction { w3c =>
        w3c.log("widgets", 8)
        w3c.log("widgets", 8)
      }
      val logline = getFirstLine()
      logline.replaceAll(" ?- ?", "") mustEqual "16"
      false
    }

    "concat multiple string values within a transaction" in {
      w3c.transaction { w3c =>
        w3c.log("widgets", "hello")
        w3c.log("widgets", "kitty")
      }
      val logline = getFirstLine()
      logline.replaceAll(" ?- ?", "") mustEqual "hello,kitty"
      false
    }
  }
} */
