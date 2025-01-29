package net.animetone.userStats

interface StatsStore {
    fun set(statsModel: StatsModel)
    fun find(): StatsModel?
}