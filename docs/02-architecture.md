# Architecture

## Repository shape

The repository contains a reusable library and a verification application:

    Host activity or fragment
              |
      VideoFlow public API
              |
       VideoFlowEngine
        |     |      |
    placement |  lifecycle
              |
       decoder capacity
              |
        player slots
              |
     Media3 ExoPlayer(s)
              |
     PlayerView / SurfaceView

The `app` module is intentionally a thin host. It supplies four asset sources
and four rectangles to `video-flow`; it does not create or control ExoPlayer
directly. This ensures the demo tests the exact path used by submodule
consumers.

## Public boundary

The library package is `com.luxar.videoflow`. Its public boundary contains:

- `VideoFlow.initialize`: creates a screen-scoped engine.
- `VideoFlowEngine.run`: registers and starts one video request.
- `VideoPlayerHandle`: play, pause, retry, move, and stop operations.
- `VideoSource`: host asset, absolute file path, or `content://` URI.
- `VideoPlacement`: x, y, width, height, and z-index.
- `CoordinateSpace`: normalized or reference-canvas coordinates.
- `VideoPlayerSnapshot`: state, decoder metrics, and error information.
- `DecoderCapacity`: decoder names and the usable player budget.

No Media3 type is exposed by the public API. This keeps host projects insulated
from ExoPlayer configuration and unstable codec APIs.

## Engine ownership

One engine belongs to one visible host container and lifecycle. It owns:

- all player instances and rendering views;
- hardware-only decoder filtering;
- the decoder budget (`getMaxSupportedInstances() - reserve`);
- sequential decoder initialization;
- runtime capacity backoff after insufficient-resource failures;
- source URI construction and local file access;
- looping, audio track policy, and buffer configuration;
- live FPS, bitrate, resolution, decoder, buffer, and dropped-frame metrics;
- coordinate scaling and view placement;
- foreground release and restoration.

The engine must be called on Android's main thread. It validates this at its
public command boundary.

## Coordinates

Normalized coordinates use the container as a 1×1 canvas. A request at
`(0.5, 0.0)` with size `(0.5, 0.5)` occupies the upper-right quarter.

Reference coordinates scale a fixed design canvas to the real container. For
example, coordinates from a 1920×1080 design can be used unchanged on a
1280×720 TV surface.

PlayerView preserves the video aspect ratio inside the assigned rectangle.
The rectangle itself scales independently on x and y when the container aspect
ratio differs from the reference canvas.

## Decoder allocation

1. Query Media3's ordered AVC decoder list.
2. Keep hardware-accelerated, non-software decoders only.
3. Read the first platform-preferred decoder's advertised maximum.
4. Subtract the configured reserve, which defaults to two instances.
5. Queue requests beyond the resulting runtime limit.
6. Prepare accepted players one at a time.
7. If the codec reports insufficient resources, lower the runtime limit to the
   number of allocations that actually succeeded.

An unknown advertised maximum uses a conservative configurable fallback.

## Rendering

Version one uses SurfaceView through Media3 PlayerView. This minimizes GPU
composition cost and is the preferred path for many simultaneous TV videos.
Rectangles should normally not overlap. z-index controls Android child order,
but reliable overlapping video composition will require a future TextureView
rendering option and a separate performance budget.

## Lifecycle

On `ON_STOP`, the engine releases every ExoPlayer and hardware decoder but
retains requests and placements. On `ON_START`, it rebuilds them sequentially.
On `ON_DESTROY` or explicit `release()`, it also removes views, requests, and
listeners.

Fragments should pass `viewLifecycleOwner`; activities can pass themselves.

## Errors and observability

Each request is isolated. A file or decode failure affects only its slot.
Listeners receive immutable snapshots with the current state, error code, and
metrics. When diagnostics are enabled, the library renders the same information
inside the player rectangle.
