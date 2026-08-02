# Implementation plan

Implementation is divided into vertical phases. Each phase ends with runnable
or testable behavior.

## Phase 0: Project scaffold and reusable library boundary

Deliverables:

- Gradle Kotlin DSL Android project
- One app module
- Kotlin, Compose, Compose for TV, Media3, Room, and test dependencies
- TV-only manifest declarations
- Landscape main activity
- TV launcher icon and 320x180 banner placeholder
- App theme and empty D-pad-focusable home screen

Exit criteria:

- Debug APK installs and appears in the Android TV launcher.
- D-pad focus is visible.
- Unit and instrumentation test tasks run.

## Phase 1: Media library and validation

Deliverables:

- Room database and VideoEntity
- VideoRepository
- Document-picker import flow
- MediaContractValidator
- Copy into app-private video storage
- Import progress, validation error, delete, and reorder UI
- Empty-state guidance containing the FFmpeg profile

Exit criteria:

- Valid converter output imports and survives restart.
- Invalid AVC, HEVC, HDR, wrong resolution, and corrupt files are rejected.
- No internet or broad storage permission exists.

## Phase 2: Decoder discovery and diagnostics

Deliverables:

- HardwareDecoderDiscovery
- Exact AVC decoder selection
- CodecBudget calculation
- Device diagnostics screen
- Unit tests for known, small, and unknown maximum values

Exit criteria:

- Target TV reports a decoder name and advertised capacity.
- Software decoders are visibly excluded.
- A device without a compatible decoder gets a clear blocking message.

## Phase 3: Single player vertical slice

Deliverables:

- PlayerFactory
- Local MediaItem construction
- Library-owned PlayerView with SurfaceView
- Repeat-one playback
- Short local buffer policy
- Lifecycle release and recreation
- Playback diagnostics listener

Exit criteria:

- One imported video loops using the chosen hardware decoder.
- Backgrounding releases the decoder.
- Foregrounding resumes safely.

## Phase 4: Player pool

Deliverables:

- PlayerSlot and PlayerPool
- PlaybackCoordinator
- Sequential decoder initialization
- Runtime capacity downgrade
- Per-tile failure isolation
- Position snapshot on reassignment

Exit criteria:

- Multiple videos play independently.
- Pool never exceeds its current limit.
- Simulated capacity failure lowers the limit without a crash.
- No player is owned by a composable.

## Phase 5: TV playback wall

Deliverables:

- Fixed wall layouts for common counts
- D-pad focus border and glow without surface scaling
- Exclusive focused audio
- Global play/pause
- Capacity placeholders
- Active-count and capacity indicator
- Keep-awake and Ambient Mode behavior

Exit criteria:

- The entire wall is usable with only a standard TV remote.
- Focus never disappears.
- Exactly one tile can produce audio.
- The playback wall does not scroll.

## Phase 6: Device hardening

Deliverables:

- Resource-exhaustion and reclaimed-decoder handling
- Standby, HDMI interruption, and activity recreation verification
- Memory and dropped-frame instrumentation
- Vendor diagnostic logging
- Thirty-minute stress test
- Release build configuration

Exit criteria:

- Target TV completes the verification matrix.
- Successful hardware count and stable runtime limit are documented.
- No fatal decoder error, ANR, or monotonic memory leak occurs.

## Implementation order within each phase

1. Define immutable models and interfaces.
2. Write unit tests for deterministic policy.
3. Implement the Android integration.
4. Add UI state and controls.
5. Run local checks.
6. Run on emulator for navigation behavior.
7. Run on the physical TV for codec behavior.

An emulator is valid for UI and policy tests but never proves hardware decoder
capacity.

## Initial package layout

    app/src/main/java/<namespace>/
      app/
      data/
        db/
        import/
      media/
        contract/
        decoder/
        playback/
      ui/
        library/
        wall/
        diagnostics/
        theme/

## Dependency policy

- Use stable AndroidX releases.
- Compile with API 37 because the selected Compose release requires it; keep
  target API 36 for the first release.
- Keep all Media3 artifacts on one version.
- Do not add a networking library.
- Do not add Hilt, WorkManager, image loading, or navigation until a concrete
  requirement justifies them.
- Prefer platform and AndroidX APIs over custom infrastructure.
