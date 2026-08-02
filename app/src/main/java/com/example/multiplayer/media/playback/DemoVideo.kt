package com.example.multiplayer.media.playback

data class DemoVideo(
    val id: String,
    val label: String,
    val assetPath: String,
)

val demoVideos = listOf(
    DemoVideo(
        id = "coins",
        label = "Coins",
        assetPath = "videos/coins_luxar.mp4",
    ),
    DemoVideo(
        id = "ortaworld",
        label = "Orta World",
        assetPath = "videos/ortaworld_luxar.mp4",
    ),
    DemoVideo(
        id = "purple",
        label = "Purple",
        assetPath = "videos/purple_luxar.mp4",
    ),
    DemoVideo(
        id = "red-squares",
        label = "Red Squares",
        assetPath = "videos/redsquares_luxar.mp4",
    ),
)

