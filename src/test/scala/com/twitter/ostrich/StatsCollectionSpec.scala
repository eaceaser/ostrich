package com.twitter.ostrich

import org.specs._
import com.twitter.xrayspecs.Time
import com.twitter.xrayspecs.TimeConversions._

object StatsCollectionSpec extends Specification {
  "Stats Collection" should {
    val stats = new ThreadUnsafeStatsCollection

    doBefore {
      stats.clearAll()
    }

    "counters" in {
      Stats.incr("widgets", 1)
      Stats.incr("wodgets", 12)
      Stats.incr("wodgets")
      Stats.getCounterStats() must eventually(equalTo(Map("widgets" -> 1, "wodgets" -> 13)))
    }

    "timings" >> {
      "report" in {
        var x = 0
        stats.time("hundred") { for (i <- 0 until 100) x += i }
        val timings = stats.getTimingStats(false)
        timings.keys.toList mustEqual List("hundred")
        timings("hundred").count mustEqual 1
        timings("hundred").minimum mustEqual timings("hundred").average
        timings("hundred").maximum mustEqual timings("hundred").average
      }

      "handle code blocks" in {
        stats.time("test") {
          Time.advance(10.millis)
        }
        val test = stats.getTiming("test")
        test.get(true).average must be_>=(10)
      }

      "reset when asked" in {
        var x = 0
        stats.time("hundred") { for (i <- 0 until 100) x += i }
        stats.getTimingStats(false)("hundred").count mustEqual 1
        stats.time("hundred") { for (i <- 0 until 100) x += i }
        stats.getTimingStats(false)("hundred").count mustEqual 2
        stats.getTimingStats(true)("hundred").count mustEqual 2
        stats.time("hundred") { for (i <- 0 until 100) x += i }
        stats.getTimingStats(false)("hundred").count mustEqual 1
      }

      "timing stats can be added and reflected in Stats.getTimingStats" in {
        var x = 0
        stats.time("hundred") { for (i <- 0 until 100) x += 1 }
        stats.getTimingStats(false).size mustEqual 1

        stats.addTiming("foobar", new TimingStat(1, 0, 0))
        stats.getTimingStats(false).size mustEqual 2
        stats.getTimingStats(true)("foobar").count mustEqual 1
        stats.addTiming("foobar", new TimingStat(3, 0, 0))
        stats.getTimingStats(false)("foobar").count mustEqual 3
      }
    }
  }
}
