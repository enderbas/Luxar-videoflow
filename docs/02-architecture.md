# Architecture

## Shape

The first version uses one Android application module and package-level
separation. Additional Gradle modules would add ceremony without improving the
initial hardware experiment.

    Compose TV screens
            |
    MultiPlayerViewModel
       |             |
       |       VideoRepository
       |          |       |
       |        Room   private files
       |
    PlaybackCoordinator
       |             |
    CodecBudget   PlayerPool
                       |
                  PlayerSlot(s)
                       |
              Media3 ContentFrame
                       |
                  SurfaceView

## Component responsibilities

### AppContainer

Creates application-scoped database, repository, media validator, decoder
discovery, and coordinator factories. No dependency-injection framework is
needed.

### VideoRepository

- Opens document-picker URIs.
- Validates media metadata and decoder compatibility.
- Copies accepted files into filesDir/videos.
- Persists VideoEntity records.
- Deletes an app-private copy only after explicit confirmation.
- Exposes the ordered library as Flow.

### MediaContractValidator

Uses MediaExtractor and MediaFormat metadata to verify container tracks,
video MIME type, profile, level, dimensions, frame rate, and audio format.
It also checks the selected decoder's size and frame-rate support. Validation
does not allocate a long-lived decoder.

### HardwareDecoderDiscovery

- Queries non-secure and non-tunneled AVC decoders.
- Keeps hardware-accelerated, non-software decoders.
- Rejects decoders that do not support the canonical format.
- Selects the first compatible platform-preferred decoder.
- Exposes name, properties, and advertised maximum.

Media3 unstable codec APIs are isolated in this component and the renderers
factory so the rest of the application does not depend on them.

### CodecBudget

Calculates the initial limit, tracks confirmed allocations, and lowers the
runtime limit after a resource-capacity failure. Its state is session-scoped
and is not persisted across firmware updates or process restarts.

### PlayerFactory

Creates identically configured ExoPlayer instances:

- Exact hardware decoder selector
- Tunneling disabled
- SurfaceView output
- Repeat-one mode
- Audio track initially disabled
- Local DefaultDataSource
- One-to-three-second buffer window
- Approximately 2 MiB target allocation
- No back buffer

### PlayerPool

Owns every ExoPlayer. It creates slots lazily and never creates more than the
current CodecBudget limit. It supports assignment, reassignment, release, and
state observation.

### PlaybackCoordinator

Maps wall tile IDs to player slots, applies priority, sequences decoder
initialization, owns exclusive audio selection, stores in-memory positions,
and coordinates lifecycle release and restoration.

### MultiPlayerViewModel

Combines repository, wall selection, diagnostics, and playback coordinator
state into immutable UI models. It contains no ExoPlayer creation code.

## Data model

VideoEntity:

- id: stable generated identifier
- localFilename: generated app-private filename
- displayName: user-visible original name
- orderIndex: library ordering
- durationMs
- width
- height
- frameRate
- fileSize
- importedAt

WallEntry:

- videoId
- wallOrder

Playback positions remain in memory for version one.

## UI state

TileState:

- Waiting
- Preparing
- Playing
- Paused
- CapacityLimited
- InvalidMedia
- PlaybackError

PlayerSlotState:

- Empty
- Preparing
- Ready
- Releasing
- Failed

## Allocation flow

1. Compute requested wall IDs.
2. Clamp the request to the current runtime limit.
3. Keep existing valid assignments to avoid decoder churn.
4. Allocate the focused tile first.
5. Allocate remaining tiles in wall order.
6. Prepare one new slot at a time.
7. Continue after decoder initialization is confirmed.
8. On insufficient resources, release the failed slot and lower the limit.
9. On file failure, mark only that tile and keep the limit.

## Threading

- ExoPlayer and coordinator commands run on the main application looper.
- Import copying, metadata extraction, Room, and deletion use Dispatchers.IO.
- State is exposed through StateFlow.
- UI observes immutable state with lifecycle awareness.

## Lifecycle

Activity ON_START:

- Discover or refresh decoder information.
- Recreate the pool.
- Restore selected wall entries and in-memory positions.
- Allocate current wall slots.

Activity ON_STOP:

- Snapshot positions.
- Disable audio.
- Detach surfaces.
- Release every ExoPlayer and decoder.
- Allow Ambient Mode.

Process death restores the library and wall selection, but playback restarts
from zero in version one.

## Error classification

- Invalid media: import rejection, no pool effect.
- File unavailable or unreadable: tile error, no pool effect.
- Decoder insufficient resource: reduce runtime capacity.
- Decoder reclaimed: release affected slot and retry after foreground recovery.
- Fatal decoder error: isolate tile, record diagnostic information.
- No compatible hardware decoder: block playback and show device diagnostics.
