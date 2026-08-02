# MultiPlayer TV

MultiPlayer TV is a TV-only Android application for playing multiple independent
local MP4 videos on one screen with hardware H.264 decoders.

The application is intentionally narrow:

- Android TV and Google TV
- Local, app-private MP4 files
- Media3 ExoPlayer
- Hardware-only H.264 video decoding
- Independent looping playback
- One focused audio source
- No network playback, synchronization, DRM, or background playback

## Documentation

- [Product requirements](docs/01-product-requirements.md)
- [Architecture](docs/02-architecture.md)
- [Implementation plan](docs/03-implementation-plan.md)
- [Media contract](docs/04-media-contract.md)
- [TV user experience](docs/05-tv-ux.md)
- [Verification plan](docs/06-verification-plan.md)
- [Decision log](docs/07-decision-log.md)
- [Risk register](docs/08-risk-register.md)

## Media conversion

The WSL converter is located at
[tools/convert_video.sh](tools/convert_video.sh). It produces the canonical
640x360 H.264/AAC MP4 format described in the media contract.

## Status

Phase 0 is complete. A four-player vertical slice is also running on the Android
TV emulator using four bundled, uncompressed 1280x720 H.264 test assets. Players
use hardware-only codec selection, sequential preparation, muted audio,
independent looping, SurfaceView output, short local buffers, and lifecycle
release.

The bundled assets are temporary test fixtures. The next implementation phase
replaces them with the local media library and format validator.
