package com.example.multiplayer.ui.home

import kotlin.math.ceil
import kotlin.math.sqrt

object WallLayoutSpec {
    fun columnsFor(activeCount: Int): Int = when {
        activeCount <= 0 -> 0
        activeCount == 1 -> 1
        activeCount <= 4 -> 2
        activeCount <= 9 -> 3
        activeCount <= 16 -> 4
        else -> ceil(sqrt(activeCount.toDouble())).toInt()
    }
}

