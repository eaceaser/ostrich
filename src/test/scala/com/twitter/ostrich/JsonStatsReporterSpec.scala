/*
 * Copyright 2010 Twitter, Inc.
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

import scala.collection.immutable
import com.twitter.xrayspecs.Time
import com.twitter.xrayspecs.TimeConversions._
import net.lag.extensions._
import net.lag.logging.{BareFormatter, Level, Logger, StringHandler}
import org.specs._
import org.specs.mock.JMocker


object JsonStatsReporterSpec extends Specification with JMocker {
  "JsonStatsReporter" should {
    val logger = Logger.get("json")
    logger.setLevel(Level.INFO)

    val handler = new StringHandler(BareFormatter)
    logger.addHandler(handler)
    logger.setUseParentHandlers(false)

    var reporter: JsonStatsReporter = null
    val collection = mock[StatsCollection]

    doBefore {
      handler.clear()
      reporter = new JsonStatsReporter(logger)
    }

    "log stats to json" in {
      expect {
        one(collection).toMap willReturn Map("some" -> 1, "stats" -> 25, "variables" -> "hello")
      }
      reporter.report(collection)
      handler.toString mustMatch """"some":1"""
      handler.toString mustMatch """"stats":25"""
      handler.toString mustMatch """"variables":"hello"""
    }
  }
}
