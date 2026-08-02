# Verification plan

## Test layers

### Unit tests

CodecBudget:

- Advertised maximum 8 produces initial limit 6.
- Advertised maximum 2 still permits one player.
- Advertised maximum 1 still permits one player.
- Unknown maximum produces limit 2.
- Runtime failure lowers but never raises the active limit.

Allocation policy:

- Focused tile receives first priority.
- Existing assignments are retained.
- Pool never exceeds runtime capacity.
- Removing a tile releases its assignment.
- File error does not reduce capacity.
- Resource error reduces capacity.

State:

- Exclusive audio always has zero or one owner.
- Global pause affects all assigned players.
- Lifecycle stop transitions all slots toward release.

Media validation:

- Canonical file accepted.
- HEVC rejected.
- AVC with wrong dimensions rejected.
- More than 30 fps rejected.
- HDR profile rejected.
- Invalid audio rejected.
- No-audio file accepted.
- Truncated and corrupt files rejected.

### Instrumentation tests

- Room import, restart, reorder, and delete
- App-private copy and storage-space failure
- Activity stop/start
- Surface detach and reattach
- D-pad traversal and focus restoration
- Global media-key behavior
- Exclusive audio selection

### Physical TV tests

An emulator cannot validate the hardware decoder or compositor. Run on every
supported TV model:

1. Record firmware, Android version, display mode, decoder name, and advertised
   capacity.
2. Import at least the expected maximum plus two canonical videos.
3. Increase active count one slot at a time.
4. Record successful decoder initialization and first frame.
5. Observe any capacity downgrade.
6. Run the stable count for 30 minutes.
7. Record memory, thermal behavior, dropped frames, and playback errors.
8. Repeat after standby/resume.
9. Repeat after opening and closing another media application.

## Required media fixtures

- Landscape canonical MP4 with AAC
- Portrait source converted with black side padding
- Canonical MP4 without audio
- Short one-second clip
- Long thirty-minute clip
- Corrupt MP4
- HEVC MP4
- AVC 1920x1080 MP4
- AVC 60 fps MP4
- HDR source

Fixtures should be visually distinct and numbered so incorrect player-to-tile
assignment is obvious.

## Stress scenarios

- Start the full wall from a cold launch.
- Rapid global pause/play.
- Move focus continuously across all tiles.
- Change exclusive audio repeatedly.
- Open diagnostics while every slot plays.
- Background and foreground five times.
- Enter TV standby and resume.
- Remove a video assigned to an active slot.
- Fill app storage during import.
- Force a decoder resource conflict with another media app when possible.

## Acceptance observations

- Decoder initialization logs contain only the selected hardware video decoder.
- All requested slots up to the learned limit render first frames.
- A slot above real capacity becomes CapacityLimited without an app crash.
- Playback remains responsive to the remote.
- Audio never comes from two tiles simultaneously.
- Backgrounding releases decoder instances.
- Memory reaches a plateau rather than growing for the entire stress run.
- No ANR, fatal exception, or persistent black surface occurs.

Dropped-frame and memory numbers are recorded per TV before setting hard
thresholds. Vendor hardware differs too much for a meaningful universal number.

## Developer checks

Before each physical build:

- Unit tests
- Android lint
- Debug compilation
- Instrumentation tests when a device is attached
- Git diff whitespace check

For release:

- Release APK or App Bundle build
- TV launcher visibility
- Banner and icon verification
- D-pad-only walkthrough
- No INTERNET permission
- No broad external-storage permission
