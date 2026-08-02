# Product requirements

## Purpose

Display as many independent local videos as a TV can safely decode and compose
at once, while reserving two advertised H.264 decoder instances for the system.

## Target

- TV-only application for Android TV and Google TV
- Landscape orientation
- Minimum Android API 23
- Compile API 37 and target API 36
- Remote-control and D-pad operation
- Kotlin and Jetpack Compose

The package namespace is provisional until an owner domain is selected.

## Functional requirements

### Video library

- Import one or more MP4 files using a TV-accessible document picker.
- Validate each file before accepting it.
- Copy accepted files into app-private storage.
- Preserve the original user file.
- Persist display name, ordering, and media metadata.
- Allow removal and reordering from the library screen.
- Never require internet or broad storage permissions.

### Playback wall

- Show a fixed, non-scrolling grid of selected videos.
- Choose a grid shape appropriate for the active count.
- Start eligible videos automatically and loop them independently.
- Do not synchronize positions or start times.
- Render only videos assigned to visible wall tiles.
- Show an explicit placeholder when hardware capacity is reached.
- Support global play and pause.
- Keep the screen awake and prevent Ambient Mode only while playing.

### Audio

- Begin with every audio track disabled.
- Give audio exclusively to the selected tile.
- Disable the previous tile's audio track when selection changes.
- Handle audio focus and audio-becoming-noisy for the selected player.

### Diagnostics

Show:

- Selected decoder name
- Whether it is hardware accelerated
- Advertised maximum instances
- Reserved instance count
- Initial and runtime player limits
- Active player count
- Per-player decoder, resolution, state, dropped frames, and error

## Decoder capacity

For the selected hardware H.264 decoder:

    known maximum: max(1, advertised maximum - 2)
    unknown maximum: 2
    active count: min(runtime limit, requested wall tiles)

The advertised maximum is an initial ceiling, not a guarantee. Decoder
allocation is staggered. Resource exhaustion lowers the runtime limit for the
current foreground session.

## Non-functional requirements

- Never select a software video decoder.
- Release all players and decoder resources when the activity stops.
- Keep player memory bounded as the active count increases.
- Remain operable entirely with a D-pad remote.
- Provide visible focus at all times.
- Fail one tile without stopping unrelated tiles.
- Avoid a crash or ANR during decoder exhaustion.
- Log enough information to reproduce vendor codec failures.

## Media requirements

- MP4 container
- H.264/AVC Main profile, Level 3.1 or lower
- 640x360
- 8-bit YUV 4:2:0
- 30 fps maximum
- Approximately 1 Mbps maximum video rate
- Optional AAC-LC stereo audio at 48 kHz
- SDR only

## Out of scope for version one

- Phones and tablets
- Network playback
- HLS, DASH, RTSP, or WebRTC
- Playback synchronization
- Software video decoding
- On-device transcoding
- Background playback or a foreground media service
- MediaSession integration
- Fullscreen playback
- HDR, HEVC, AV1, DRM, subtitles, and multiple audio tracks
- Animated, clipped, overlapping, or rounded video surfaces

## Release acceptance

Version one is accepted when:

1. A conforming local MP4 can be imported and persists across app restarts.
2. Every active tile uses the chosen hardware H.264 decoder.
3. The wall reaches the safe device limit or degrades without crashing.
4. D-pad focus, selection, audio, global play/pause, and Back all work.
5. Players are released in the background and rebuilt in the foreground.
6. A 30-minute multi-player run shows no monotonic memory leak or fatal error.
