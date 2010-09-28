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
import java.util.Date
import scala.collection.immutable
import com.twitter.xrayspecs.Time
import com.twitter.xrayspecs.TimeConversions._
import net.lag.extensions._
import net.lag.logging.{GenericFormatter, Level, Logger, StringHandler}
import org.specs._
import org.specs.mock.JMocker


class W3CReporterSpec extends Specification with JMocker {
  noDetailedDiffs()

  "W3CReporter" should {
    val logger = Logger.get("w3c")
    logger.setLevel(Level.INFO)

    val handler = new StringHandler(new GenericFormatter("%2$s: "))
    logger.addHandler(handler)
    logger.setUseParentHandlers(false)

    var reporter: W3CReporter = null
    val collection = mock[StatsCollection]

    def expectedHeader(crc: Long) = "w3c: #Version: 1.0" :: "w3c: #Date: 03-Aug-2009 19:23:04" :: ("w3c: #CRC: " + crc) :: Nil

    doBefore {
      handler.clear()
      Time.now = Time.at("2009-08-03 19:23:04 +0000")
      reporter = new W3CReporter(logger, Seq(), true, false)
    }

    "log basic stats" in {
      expect {
        one(collection).toMap willReturn Map("cats" -> 10, "dogs" -> 9)
      }

      reporter.setColumns(Seq("cats", "dogs"))
      reporter.report(collection)
      handler.toString.split("\n").toList mustEqual
        expectedHeader(948200938) ::: "w3c: #Fields: cats dogs" :: "w3c: 10 9" :: Nil
    }

    "convert values appropriately" in {
      expect {
        one(collection).toMap willReturn Map("date" -> new Date(0), "size" -> (1L << 32), "address" -> InetAddress.getByName("127.0.0.1"), "x" -> new Object)
      }

      reporter.setColumns(Seq("address", "date", "size", "x"))
      reporter.report(collection)
      handler.toString.split("\n").last mustEqual "w3c: 127.0.0.1 01-Jan-1970_00:00:00 4294967296 -"
    }

    "not repeat the header too often" in {
      expect {
        one(collection).toMap willReturn Map("a" -> 1)
        one(collection).toMap willReturn Map("a" -> 2)
        one(collection).toMap willReturn Map("a" -> 3)
      }
      reporter.setColumns(Seq("a"))
      reporter.report(collection)
      reporter.report(collection)
      reporter.nextHeaderDumpAt = Time.now
      reporter.report(collection)
      handler.toString.split("\n").toList mustEqual
        expectedHeader(276919822) :::
        "w3c: #Fields: a" ::
        "w3c: 1" ::
        "w3c: 2" ::
        expectedHeader(276919822) :::
        "w3c: #Fields: a" ::
        "w3c: 3" :: Nil
    }

    "repeat the header when the fields change" in {
      expect {
        one(collection).toMap willReturn Map("a" -> 1)
        one(collection).toMap willReturn Map("a" -> 2)
        one(collection).toMap willReturn Map("a" -> 3, "b" ->1)
      }
      reporter.setColumns(Seq("a"))
      reporter.report(collection)
      reporter.report(collection)
      reporter.setColumns(Seq("a", "b"))
      reporter.report(collection)
      handler.toString.split("\n").toList mustEqual
        expectedHeader(276919822) :::
        "w3c: #Fields: a" ::
        "w3c: 1" ::
        "w3c: 2" ::
        expectedHeader(1342496559) :::
        "w3c: #Fields: a b" ::
        "w3c: 3 1" :: Nil
    }

    "per line crc printing"  >> {
      val crcReporter = new W3CReporter(logger, Seq("a", "b"), true, true)

      "should print" in {
        expect {
          one(collection).toMap willReturn Map("a" -> 3, "b" -> 1)
        }
        crcReporter.report(collection)
        handler.toString.split("\n").toList mustEqual
          expectedHeader(1342496559) :::
          "w3c: #Fields: a b" ::
          "w3c: 1342496559 3 1" :: Nil
      }

      "changes appropriately when column headers change" in {
        expect {
          one(collection).toMap willReturn Map("a" -> 1)
          one(collection).toMap willReturn Map("a" -> 3, "b" -> 1)
        }
        crcReporter.setColumns(Seq("a"))
        crcReporter.report(collection)
        crcReporter.setColumns(Seq("a", "b"))
        crcReporter.report(collection)
        handler.toString.split("\n").toList mustEqual
          expectedHeader(276919822) :::
          "w3c: #Fields: a" ::
          "w3c: 276919822 1" ::
          expectedHeader(1342496559) :::
          "w3c: #Fields: a b" ::
          "w3c: 1342496559 3 1" :: Nil
      }
    }
  }
}
