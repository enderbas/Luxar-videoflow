package com.example.multiplayer.media.playback

import com.luxar.videoflow.VideoPlacement

data class DemoVideo(
    val id: String,
    val label: String,
    val assetPath: String,
    val placement: VideoPlacement,
)

val demoVideos = listOf(
    DemoVideo(
        id = "coins",
        label = "Player 1",
        assetPath = "videos/coins_luxar.mp4",
        placement = VideoPlacement(x = 0f, y = 0f, width = 0.495f, height = 0.49f),
    ),
    DemoVideo(
        id = "ortaworld",
        label = "Player 2",
        assetPath = "videos/ortaworld_luxar.mp4",
        placement = VideoPlacement(x = 0.505f, y = 0f, width = 0.495f, height = 0.49f),
    ),
    DemoVideo(
        id = "purple",
        label = "Player 3",
        assetPath = "videos/purple_luxar.mp4",
        placement = VideoPlacement(x = 0f, y = 0.51f, width = 0.495f, height = 0.49f),
    ),
    DemoVideo(
        id = "red-squares",
        label = "Player 4",
        assetPath = "videos/redsquares_luxar.mp4",
        placement = VideoPlacement(x = 0.505f, y = 0.51f, width = 0.495f, height = 0.49f),
    ),
)
