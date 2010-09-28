package com.twitter.ostrich

import org.specs._

object TimingSpec extends Specification {
  "Timing" should {
    val timing = new Timing

    "empty on get" in {
      timing.add(0)
      timing.get(true) mustEqual new TimingStat(1, 0, 0)
      // the timings list will be empty here:
      timing.get(true) mustEqual new TimingStat(0, 0, 0)
    }

    "basic min/max/average" in {
      timing.add(1)
      timing.add(2)
      timing.add(3)
      timing.get(true) mustEqual new TimingStat(3, 3, 1, Some(Histogram(1, 2, 3)), 2.0, 2.0)
    }

    "average of zero" in {
      timing.add(0)
      timing.get(true) mustEqual new TimingStat(1, 0, 0)
    }

    "ignore negative timings" in {
      timing.add(1)
      timing.add(-1)
      timing.add(Math.MIN_INT)
      timing.get(true) mustEqual new TimingStat(1, 1, 1, Some(Histogram(1)), 1.0, 0.0)
    }

    "boundary timing sizes" in {
      timing.add(Math.MAX_INT)
      timing.add(5)
      val sum = 5.0 + Math.MAX_INT
      val avg = sum / 2.0
      val sumsq = 5.0 * 5.0 + Math.MAX_INT.toDouble * Math.MAX_INT.toDouble
      val partial = sumsq - sum * avg
      val test = Stats.getTiming("test")
      timing.get(true) mustEqual
        new TimingStat(2, Math.MAX_INT, 5, Some(Histogram(5, Math.MAX_INT)), avg, partial)
    }

    "add bundle of timings at once" in {
      val timingStat = new TimingStat(3, 20, 10, Some(Histogram(10, 15, 20)), 15.0, 50.0)
      timing.add(timingStat)
      timing.add(25)
      timing.get(false).count mustEqual 4
      timing.get(false).average mustEqual 17
      timing.get(false).standardDeviation.toInt mustEqual 6
    }

    "add multiple bundles of timings" in {
      val timingStat1 = new TimingStat(2, 25, 15, Some(Histogram(15, 25)), 20.0, 50.0)
      val timingStat2 = new TimingStat(2, 20, 10, Some(Histogram(10, 20)), 15.0, 50.0)
      timing.add(timingStat1)
      timing.add(timingStat2)
      timing.get(false).count mustEqual 4
      timing.get(false).average mustEqual 17
      timing.get(false).standardDeviation.toInt mustEqual 6
    }
  }
}

// TODO: make the tests encapsulated on TimingStat alone.
object TimingStatSpec extends Specification {
  "TimingStat" should {
    val timing = new Timing
    val timingStat = new TimingStat(1, 0, 0)

    doBefore {
      timing.add(timingStat)
    }

    doAfter {
      timing.clear
    }

    "report text in sorted order" in {
      timing.get(false).toString mustEqual
        "(average=0, count=1, maximum=0, minimum=0, " +
        "p25=0, p50=0, p75=0, p90=0, p99=0, p999=0, p9999=0, " +
        "standard_deviation=0)"
    }

    "json contains histogram buckets" in {
      val json = timing.get(false).toJson
      json mustMatch("\"histogram\":\\[")
    }
  }
}
