# Library and Git submodule integration

## Add the repository

From the consuming project's root:

```bash
git submodule add <repository-url> vendor/multiplayer
git submodule update --init --recursive
```

Register the Android library in the consuming project's `settings.gradle.kts`:

```kotlin
include(":video-flow")
project(":video-flow").projectDir = file("vendor/multiplayer/video-flow")
```

Add it to the consuming app or feature module:

```kotlin
dependencies {
    implementation(project(":video-flow"))
}
```

The consuming build must provide Google's Maven repository and use a compatible
Android Gradle Plugin, Kotlin, compile SDK, and Java 17 toolchain. The current
source module uses AGP 9 built-in Kotlin and is verified with AGP 9.3.0. Ensure
the host resolves that plugin, for example in its root build:

```kotlin
plugins {
    id("com.android.library") version "9.3.0" apply false
}
```

The library build declares its AndroidX/Media3 coordinates directly; it does
not depend on the consuming project's version-catalog aliases.

## Create the video layer

The host supplies a `FrameLayout`. It can come from XML, a view hierarchy, or a
Compose `AndroidView`.

```xml
<FrameLayout
    android:id="@+id/video_layer"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

Initialize one engine for that layer and lifecycle:

```kotlin
val flow = VideoFlow.initialize(
    container = findViewById(R.id.video_layer),
    lifecycleOwner = this,
    config = VideoFlowConfig(
        coordinateSpace = CoordinateSpace.Reference(1920, 1080),
        reserveHardwareDecoders = 2,
    ),
)
```

Use `viewLifecycleOwner` instead of `this` when the layer belongs to a Fragment
view.

## Run players

```kotlin
val player1 = flow.run(
    VideoRequest(
        id = "player1",
        label = "Lobby",
        source = VideoSource.Asset("videos/lobby.mp4"),
        placement = VideoPlacement(
            x = 0f,
            y = 0f,
            width = 960f,
            height = 540f,
        ),
        loop = true,
        muted = true,
    ),
)

val player2 = flow.run(
    VideoRequest(
        id = "player2",
        source = VideoSource.FilePath("/absolute/local/path/video.mp4"),
        placement = VideoPlacement(
            x = 960f,
            y = 540f,
            width = 960f,
            height = 540f,
            zIndex = 1,
        ),
    ),
)
```

Content-provider sources are also local:

```kotlin
VideoSource.ContentUri(documentUri)
```

The library intentionally does not request Internet permission or implement
HTTP sources.

## Control a player

`run` returns a stable handle:

```kotlin
player1.pause()
player1.play()
player1.replaceSource(VideoSource.Asset("videos/updated.mp4"))
player1.updatePlacement(VideoPlacement(100f, 100f, 800f, 450f))
player1.retry()
player1.stop()
```

All commands must run on Android's main thread. Player identifiers must be
unique within one engine.

`replaceSource` keeps the existing handle, player view, SurfaceView, placement,
z-order, and focus. It swaps the Media3 item in-place, resets per-source
diagnostics, starts the new local video at zero, and preserves play/pause state
unless `playWhenReady` is supplied. The transition is asynchronous and may
briefly show the last frame or black while the new MP4 reaches its first
decodable frame; it is not a frame-accurate crossfade.

## Observe state

```kotlin
val listener = VideoFlowListener { snapshot ->
    Log.d("VideoFlow", "${snapshot.id}: ${snapshot.state} ${snapshot.metrics}")
}

flow.addListener(listener)
```

`flow.decoderCapacity` exposes decoder names, the advertised maximum, the
reserved instances, and the resulting usable player count.

## Cleanup

The engine automatically releases decoders when its lifecycle stops and
restores registered requests when it starts. Call `release()` only when the
engine must be discarded before lifecycle destruction:

```kotlin
flow.release()
```

## Updating the submodule

```bash
git -C vendor/multiplayer fetch
git -C vendor/multiplayer checkout <tested-tag-or-commit>
git add vendor/multiplayer
git commit -m "Update video-flow submodule"
```

Pin consumers to tested tags or commit hashes. Do not make unrelated host-app
changes directly inside the submodule checkout.
