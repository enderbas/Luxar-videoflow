# Video Flow for Android TV

This repository provides a reusable Android library for playing multiple
independent local videos with hardware decoders. It also contains a TV demo app
that exercises the same public API consumers use.

The application is intentionally narrow:

- Android TV and Google TV
- Local, app-private MP4 files
- Media3 ExoPlayer
- Hardware-only H.264 video decoding
- Independent looping playback
- One focused audio source
- No network playback, synchronization, DRM, or background playback

## Modules

- `video-flow`: reusable Android library; owns the complete video lifecycle
- `app`: TV demo and device verification host

The library is designed to be included as a Git submodule. See the
[integration guide](docs/09-library-integration.md) for Gradle setup and the
`VideoFlow.initialize` / `run` API.

## Documentation

- [Product requirements](docs/01-product-requirements.md)
- [Architecture](docs/02-architecture.md)
- [Implementation plan](docs/03-implementation-plan.md)
- [Media contract](docs/04-media-contract.md)
- [TV user experience](docs/05-tv-ux.md)
- [Verification plan](docs/06-verification-plan.md)
- [Decision log](docs/07-decision-log.md)
- [Risk register](docs/08-risk-register.md)
- [Library and Git submodule integration](docs/09-library-integration.md)

## Media conversion

The WSL converter is located at
[tools/convert_video.sh](tools/convert_video.sh). It produces the canonical
640x360 H.264/AAC MP4 format described in the media contract.

## Status

The reusable library and four-player vertical slice are running with bundled,
uncompressed 1280x720 H.264 test assets. The demo declares only sources and
normalized rectangles; `video-flow` owns hardware-only codec selection,
decoder budgeting, sequential preparation, muted independent looping,
SurfaceView output, diagnostics, and lifecycle release/recovery.

The bundled assets are temporary test fixtures. The next implementation phase
replaces them with the local media library and format validator.
