package com.luxar.videoflow

import org.junit.Assert.assertEquals
import org.junit.Test

class PlacementResolverTest {
    @Test
    fun resolvesNormalizedCoordinates() {
        val result = PlacementResolver.resolve(
            placement = VideoPlacement(x = 0.5f, y = 0.25f, width = 0.25f, height = 0.5f),
            coordinateSpace = CoordinateSpace.Normalized,
            containerWidth = 1920,
            containerHeight = 1080,
        )

        assertEquals(ResolvedPlacement(left = 960, top = 270, width = 480, height = 540), result)
    }

    @Test
    fun scalesReferenceCanvasCoordinates() {
        val result = PlacementResolver.resolve(
            placement = VideoPlacement(x = 960f, y = 540f, width = 960f, height = 540f),
            coordinateSpace = CoordinateSpace.Reference(1920, 1080),
            containerWidth = 1280,
            containerHeight = 720,
        )

        assertEquals(ResolvedPlacement(left = 640, top = 360, width = 640, height = 360), result)
    }
}
