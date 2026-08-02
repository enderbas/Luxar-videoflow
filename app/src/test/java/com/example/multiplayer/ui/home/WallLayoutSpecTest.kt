package com.example.multiplayer.ui.home

import org.junit.Assert.assertEquals
import org.junit.Test

class WallLayoutSpecTest {
    @Test
    fun commonWallSizesUseExpectedColumns() {
        assertEquals(0, WallLayoutSpec.columnsFor(0))
        assertEquals(1, WallLayoutSpec.columnsFor(1))
        assertEquals(2, WallLayoutSpec.columnsFor(4))
        assertEquals(3, WallLayoutSpec.columnsFor(9))
        assertEquals(4, WallLayoutSpec.columnsFor(16))
        assertEquals(5, WallLayoutSpec.columnsFor(17))
    }
}

