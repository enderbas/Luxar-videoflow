package com.example.multiplayer

import android.widget.FrameLayout
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.luxar.videoflow.CoordinateSpace
import com.luxar.videoflow.VideoFlow
import com.luxar.videoflow.VideoFlowConfig
import com.luxar.videoflow.VideoFlowEngine
import com.luxar.videoflow.VideoPlacement
import com.luxar.videoflow.VideoPlayerHandle
import com.luxar.videoflow.VideoPlayerState
import com.luxar.videoflow.VideoRequest
import com.luxar.videoflow.VideoSource
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class VideoSourceReplacementTest {
    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun replacesSourceInPlaceAndReturnsToPlaying() {
        val initialSource = VideoSource.Asset("videos/coins_luxar.mp4")
        val replacementSource = VideoSource.Asset("videos/purple_luxar.mp4")
        val initialPlaying = CountDownLatch(1)
        val replacementPlaying = CountDownLatch(1)
        lateinit var flow: VideoFlowEngine
        lateinit var handle: VideoPlayerHandle
        lateinit var container: FrameLayout

        activityRule.scenario.onActivity { activity ->
            container = FrameLayout(activity)
            activity.setContentView(container)
            flow = VideoFlow.initialize(
                container = container,
                lifecycleOwner = activity,
                config = VideoFlowConfig(
                    coordinateSpace = CoordinateSpace.Normalized,
                    reserveHardwareDecoders = 0,
                ),
            )
            flow.addListener { snapshot ->
                if (snapshot.state == VideoPlayerState.PLAYING) {
                    when (snapshot.source) {
                        initialSource -> initialPlaying.countDown()
                        replacementSource -> replacementPlaying.countDown()
                        else -> Unit
                    }
                }
            }
            handle = flow.run(
                VideoRequest(
                    id = "replaceable",
                    source = initialSource,
                    placement = VideoPlacement(0f, 0f, 1f, 1f),
                ),
            )
        }

        assertTrue("Initial source did not start", initialPlaying.await(15, TimeUnit.SECONDS))

        activityRule.scenario.onActivity {
            handle.replaceSource(replacementSource)
        }

        assertTrue("Replacement source did not start", replacementPlaying.await(15, TimeUnit.SECONDS))

        activityRule.scenario.onActivity {
            assertEquals(1, container.childCount)
            assertEquals(replacementSource, handle.snapshot.source)
            assertEquals(VideoPlayerState.PLAYING, handle.snapshot.state)
            flow.release()
        }
    }
}
