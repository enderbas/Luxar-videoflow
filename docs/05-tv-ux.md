# TV user experience

## Principles

- Every operation works with D-pad arrows, center/select, Back, and media keys.
- Focus is always visible.
- Text and indicators are readable from normal TV viewing distance.
- Playback surfaces do not animate, scale, overlap, or clip.
- Video remains primary; controls stay visually quiet.

## Screens

### Playback wall

The default screen contains:

- Compact top status bar
- Global play/pause action
- Library action
- Diagnostics action
- Hardware capacity text
- Fixed video wall

The wall does not scroll. If the library contains more videos than the current
wall can show, selection occurs on the library screen.

### Library

- Ordered list or TV grid of imported videos
- Add, remove, reorder, and assign-to-wall actions
- Import progress and validation results
- Storage usage

### Diagnostics

- Device and Android version
- Decoder name and hardware classification
- Advertised, reserved, initial, and runtime capacities
- Active slot list
- Per-slot state, first-frame time, dropped frames, and last error

## Remote mapping

- Arrow keys: move focus geometrically
- Center/select on a video: select exclusive audio
- Center/select on selected video: keep it selected; do not pause unexpectedly
- Media play/pause: global wall play/pause
- Back: return to wall, then exit normally from the wall
- Menu or settings key when available: open diagnostics

Per-video pause can be added later if required. Version one uses global
play/pause to keep remote behavior predictable.

## Focus style

Focused video tile:

- Bright 3-4 dp border
- Subtle outer glow
- Audio icon when selected
- No scale animation because transforming a SurfaceView can reveal composition
  artifacts

Focused toolbar action:

- TV Material focused colors and border
- Clear text label, not icon-only for uncommon actions

Initial focus returns to the last focused wall tile after closing another
screen. When that item no longer exists, focus moves to the first wall tile.

## Layout

Common arrangements:

- 1: 1x1
- 2: 2x1
- 3-4: 2x2
- 5-6: 3x2
- 7-9: 3x3
- 10-12: 4x3
- 13-16: 4x4

Counts above 16 use a computed near-square grid, but physical TV testing can
introduce a compositor-derived runtime ceiling.

The layout reserves a safe outer margin for TV overscan and system UI. All
tiles keep a 16:9 content area. Unused cells remain empty.

## Tile overlays

Overlays appear only when needed:

- Preparing spinner
- Paused indicator
- Exclusive audio indicator
- Capacity reached message
- Short, user-readable error

Detailed codec errors remain on the diagnostics screen.

## Ambient Mode and burn-in

- Prevent Ambient Mode while at least one tile is actively playing.
- Allow Ambient Mode when globally paused or when the activity stops.
- Do not leave a permanent high-contrast error overlay on screen indefinitely.

## Accessibility

- Meaningful content descriptions for actions and tile states
- High-contrast focus indicator not dependent on color alone
- Focus traversal follows visual geometry
- Minimum TV-readable text sizing
- No operation depends on long press or pointer hover
