package com.twitter.ostrich

import com.twitter.xrayspecs.Time
import com.twitter.xrayspecs.TimeConversions._
import org.specs._
import org.specs.mock.JMocker

class TransactionalStatsSpec extends Specification with JMocker {
  "TransactionalStats" should {
    val reporter = mock[StatsReporter]
    val tstats = new TransactionalStats(reporter)

    "log transactionally" in {
      val collection = capturingParam[ThreadUnsafeStatsCollection]
      expect {
        one(reporter).report(collection.capture)
      }

      tstats.transaction { stats =>
        stats.addTiming("test", 2)
        stats.incr("count", 2)
      }

      collection.captured.getCounterStats() mustEqual Map("count" -> 2)
      collection.captured.getTimingStats() mustEqual Map("test" -> new TimingStat(1, 2, 2, None, 2, 0))
    }
  }
}
