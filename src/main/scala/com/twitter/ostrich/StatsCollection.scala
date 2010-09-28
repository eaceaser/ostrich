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

import scala.collection.{Map, jcl, mutable, immutable}
import java.util.concurrent.ConcurrentHashMap

/**
 * Concrete StatsProvider that tracks counters and timings.
 */
trait StatsCollection extends StatsProvider {

  def addTiming(name: String, duration: Int): Long = {
    getTiming(name).add(duration)
  }

  def addTiming(name: String, timingStat: TimingStat): Long = {
    getTiming(name).add(timingStat)
  }

  def incr(name: String, count: Int): Long = {
    getCounter(name).value.addAndGet(count)
  }

  /**
   * Find or create a counter with the given name.
   */
  def getCounter(name: String): Counter

  /**
   * Find or create a timing measurement with the given name.
   */
  def getTiming(name: String): Timing

  /**
   * For timing work that doesn't fall into one lexical area, you can specify a specific start and end.
   */
  /*def startTiming(name: String) {
    if (map.contains(name)) {
      log.warning("adding timing for an already timed column")
    }
    timingMap += (name -> System.currentTimeMillis)
  }

  def endTiming(name: String): Unit = timingMap.get(name) match {
    case None => log.error("endTiming called for name that had no start time: %s", name)
    case Some(start) => {
      val startTime = start.asInstanceOf[Long]
      addTiming(name, (System.currentTimeMillis - startTime).toInt)
      timingMap -= name
    }
  } */
}

class ThreadUnsafeStatsCollection(preallocSize: Int) extends StatsCollection {
  def this() = this(1)

  /**
   * Find or create a counter with the given name.
   */
  def getCounter(name: String): Counter = {
    counterMap.get(name) match {
      case Some(counter) => counter
      case None =>
        val counter = new Counter
        counterMap += (name -> counter)
        counter
    }
  }

  def getCounterStats(reset: Boolean): Map[String, Long] = {
    val rv = immutable.HashMap(counterMap.map { case (k, v) => (k, v.value.get) }.toList: _*)
    if (reset) {
      for ((k, v) <- counterMap) {
        v.reset()
      }
    }
    rv
  }

  def getTimingStats(reset: Boolean): Map[String, TimingStat] = {
    val out = new mutable.HashMap[String, TimingStat]

    for ((key, timing) <- timingMap) {
      out += (key -> timing.get(reset))
    }
    out
  }

  def getTiming(name: String): Timing = {
    timingMap.get(name) match {
      case Some(timing) => timing
      case None =>
        val timing = new Timing(false)
        timingMap += (name -> timing)
        timing
    }
  }

  def putVariable(key: String, value: String) {
    variableMap += (key -> value)
  }

  def clearVariable(key: String) {
    variableMap -= key
  }

  def getVariable(key: String) = variableMap.getOrElse(key, "")

  def getVariables(reset: Boolean) = {
    val rv = variableMap.clone.readOnly
    if (reset) {
      variableMap.clear
    }
    rv
  }

  def clearAll() = {
    counterMap.clear()
    timingMap.clear()
    variableMap.clear()
  }

  private val counterMap = new mutable.HashMap[String, Counter]() {
    override def initialSize = preallocSize * 2
  }
  private val timingMap = new mutable.HashMap[String, Timing]() {
    override def initialSize = preallocSize * 2
  }
  private val variableMap = new mutable.HashMap[String, String]() {
    override def initialSize = preallocSize * 2
  }
}

class ThreadSafeStatsCollection extends StatsCollection {
  def getCounterStats(reset: Boolean): Map[String, Long] = {
    val rv = new mutable.HashMap[String, Long]
    for((key, counter) <- jcl.Map(counterMap)) {
      rv += (key -> counter(reset))
    }
    rv
  }

  def getTimingStats(reset: Boolean): Map[String, TimingStat] = {
    val out = new mutable.HashMap[String, TimingStat]
    for ((key, timing) <- jcl.Map(timingMap)) {
      out += (key -> timing.get(reset))
    }
    out
  }

  /**
   * Find or create a counter with the given name.
   */
  def getCounter(name: String): Counter = {
    var counter = counterMap.get(name)
    while (counter == null) {
      counter = counterMap.putIfAbsent(name, new Counter)
      counter = counterMap.get(name)
    }
    counter
  }

  /**
   * Find or create a timing measurement with the given name.
   */
  def getTiming(name: String): Timing = {
    var timing = timingMap.get(name)
    while (timing == null) {
      timing = timingMap.putIfAbsent(name, new Timing)
      timing = timingMap.get(name)
    }
    timing
  }

  def clearAll() {
    counterMap.synchronized { counterMap.clear() }
    timingMap.clear()
    variableMap.synchronized { variableMap.clear() }
  }

  def putVariable(key: String, value: String) = variableMap.synchronized {
    variableMap += (key -> value)
  }

  def clearVariable(key: String) = variableMap.synchronized {
    variableMap -= key
  }

  def getVariable(key: String) = variableMap.synchronized {
    variableMap.getOrElse(key, "")
  }

  def getVariables(reset: Boolean) = variableMap.synchronized { 
    val rv = variableMap.clone.readOnly
    if (reset) {
      variableMap.clear
    }
    rv
  }

  private val counterMap = new ConcurrentHashMap[String, Counter]()
  private val timingMap = new ConcurrentHashMap[String, Timing]()
  private val variableMap = new mutable.HashMap[String, String]()
}
