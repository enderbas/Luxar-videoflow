package com.luxar.videoflow

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test

class VideoRequestTest {
    private val placement = VideoPlacement(0f, 0f, 1f, 1f)

    @Test
    fun managedViewRemainsTheDefaultOutput() {
        val request = VideoRequest(
            id = "default-output",
            source = VideoSource.Asset("videos/example.mp4"),
            placement = placement,
        )

        assertSame(VideoOutput.ManagedView, request.output)
        assertEquals(1f, request.playbackSpeed)
    }

    @Test
    fun playbackSpeedRejectsValuesOutsideTheSupportedRange() {
        assertThrows(IllegalArgumentException::class.java) {
            VideoRequest(
                id = "too-fast",
                source = VideoSource.Asset("videos/example.mp4"),
                placement = placement,
                playbackSpeed = 5.1f,
            )
        }
    }
}
