package com.twitter.ostrich

import com.twitter.json.Json
import net.lag.logging.Logger

class JsonStatsReporter(val logger: Logger) extends StatsReporter {
  def report(stats: StatsProvider) {
    logger.info(Json.build(stats.toMap).toString)
  }
}
