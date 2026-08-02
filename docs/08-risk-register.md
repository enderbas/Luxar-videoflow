# Risk register

## R-001: Decoder maximum is inaccurate

Impact: advertised maximum is larger than real simultaneous capacity.

Mitigation:

- Reserve two instances.
- Initialize sequentially.
- Catch insufficient-resource errors.
- Lower the session limit without crashing.
- Verify on each supported TV model.

## R-002: Surface compositor limit is lower than decoder limit

Impact: decoders initialize but one or more surfaces remain black or unstable.

Mitigation:

- Use fixed, non-overlapping SurfaceViews.
- Add first-frame and dropped-frame diagnostics.
- Treat repeated surface failures as a runtime wall ceiling.
- Verify each grid size physically.

## R-003: Vendor decoder defects

Impact: codec claims support but fails or corrupts output.

Mitigation:

- Use a strict canonical media profile.
- Capture decoder name and diagnostic information.
- Isolate Media3 unstable APIs.
- Add device-specific workarounds only with reproduced evidence.

## R-004: Excessive memory per player

Impact: process termination or Android 17 memory limiting.

Mitigation:

- Create players lazily.
- Use one-to-three-second local buffers.
- Use an approximately 2 MiB allocation target per player.
- Keep no back buffer.
- Release all players on stop.
- Profile proportional-set size during stress tests.

## R-005: Slow or unreliable storage

Impact: simultaneous local reads cause rebuffering.

Mitigation:

- Copy imports to app-private storage.
- Limit sources to approximately 1 Mbps each.
- Record rebuffer events.
- Avoid direct long-term playback from removable USB storage.

## R-006: TV document picker is absent or difficult

Impact: users cannot conveniently add videos.

Mitigation:

- Verify picker availability on the target TV.
- Provide clear ADB and USB developer workflows.
- Add a TV-specific USB import screen later if product requirements demand it.

## R-007: D-pad focus becomes lost

Impact: application becomes unusable without restarting.

Mitigation:

- Define explicit focus groups and restoration.
- Keep one visible focused element.
- Test every screen using only a standard remote.
- Avoid focusable playback-surface internals.

## R-008: Audio decoder or focus contention

Impact: mixed sound, audio loss, or unnecessary resource usage.

Mitigation:

- Disable audio tracks for all unselected players.
- Maintain a single audio-owner invariant.
- Request audio focus only for that owner.

## R-009: Decoder reclaimed by another application

Impact: active tile fails after another system media component starts.

Mitigation:

- Recognize resource-reclaimed errors.
- Release the terminal codec instance.
- Restore players sequentially when the application regains the foreground.

## R-010: Burn-in or Ambient Mode interference

Impact: paused static UI remains on screen or playback is interrupted.

Mitigation:

- Prevent Ambient Mode only during active playback.
- Allow Ambient Mode while paused and stopped.
- Avoid permanent high-contrast error overlays.

## R-011: 360p appears soft on a 4K TV

Impact: grid tiles appear lower quality at small active counts.

Mitigation:

- Optimize version one for capacity and 1080p 3x3 presentation.
- Measure viewing quality on the target TV.
- Consider a separate 720p contract with its own measured capacity later.
