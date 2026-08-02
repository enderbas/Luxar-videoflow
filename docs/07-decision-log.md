# Decision log

All entries are accepted for version one.

## D-001: TV-only application

Decision: target Android TV and Google TV with a landscape, remote-first UI.

Consequence: mobile layouts and touch interaction are not implemented.

## D-002: Local app-private playback

Decision: import through a document provider, validate, then copy into
app-private storage.

Consequence: playback is stable and permission-independent, but files occupy
additional storage and are removed when the app is uninstalled.

## D-003: Canonical AVC profile

Decision: accept 640x360, 30 fps, SDR H.264 Main Level 3.1 MP4.

Consequence: decoder selection and capacity are predictable. Other formats
must be converted off-device.

## D-004: Hardware-only video decoding

Decision: use one exact compatible hardware decoder and provide no software
fallback.

Consequence: unsupported devices or media fail visibly instead of consuming
CPU unpredictably.

## D-005: Reserve two advertised instances

Decision: initial known capacity is advertised maximum minus two with a
minimum of one; unknown capacity starts at two.

Consequence: the value remains advisory and runtime allocation can lower it.

## D-006: Sequential player initialization

Decision: initialize one new decoder slot after the previous decoder confirms
initialization.

Consequence: wall startup is staggered but avoids an allocation spike. No
synchronization requirement is violated.

## D-007: SurfaceView rendering

Decision: use a library-owned Media3 PlayerView with SurfaceView.

Consequence: efficient video composition takes priority over rounded clipping,
overlap, scale animation, and transitions.

## D-008: Disable tunneling and frame-rate switching

Decision: use normal MediaCodec surface output, fixed TV display mode, and
30 fps sources.

Consequence: multiple players do not compete for tunneled playback or display
mode changes.

## D-009: Exclusive audio

Decision: audio tracks are disabled except on the selected tile.

Consequence: muted players avoid unnecessary audio decoder work, and the wall
never mixes several soundtracks.

## D-010: Foreground-only playback

Decision: release players when the activity stops; do not add MediaSession or
a playback service in version one.

Consequence: resources are returned promptly and system media integration is
limited to foreground key handling.

## D-011: Fixed playback wall

Decision: playback uses a non-scrolling grid; library management is a separate
screen.

Consequence: every allocated decoder corresponds to visible content and D-pad
navigation remains predictable.

## D-012: Simple application architecture

Decision: one app module, package boundaries, Room, StateFlow, and manual
dependency construction.

Consequence: the project remains easy to inspect. Modularity or a DI framework
can be introduced only when scale justifies it.
