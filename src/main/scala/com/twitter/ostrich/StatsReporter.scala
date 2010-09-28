package com.twitter.ostrich

trait StatsReporter {
  def report(stats: StatsProvider)
}
