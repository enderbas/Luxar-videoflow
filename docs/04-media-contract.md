# Media contract

## Canonical grid profile

Video:

- Container: ISO base media MP4
- Codec: H.264/AVC
- Profile: Main
- Level: 3.1
- Dimensions: exactly 640x360
- Sample aspect ratio: 1:1
- Pixel format: yuv420p, 8-bit
- Frame rate: constant 30 fps
- Rate control: CRF 23 with 1 Mbps maximum and 2 Mbps VBV buffer
- GOP: maximum 60 frames
- HDR: not allowed

Audio:

- Optional
- AAC-LC
- 48 kHz
- Stereo
- 128 kbps

Other tracks:

- Subtitles, chapters, attachments, and additional audio tracks are ignored.

## Conversion

From PowerShell:

    wsl bash /mnt/c/Users/engin/Documents/multiplayer/tools/convert_video.sh \
      /mnt/c/path/to/input.mov \
      /mnt/c/path/to/output.mp4

Pass --force before the input to replace an existing output.

The converter:

- Preserves source aspect ratio.
- Pads to 640x360 with black.
- Converts frame rate to 30 fps.
- Converts the first audio stream when present.
- Produces no audio stream when the source has none.
- Removes source metadata and chapters.
- Writes the MP4 metadata atom first. This is harmless for local playback and
  keeps files generally portable.

## Import validation

The Android importer must check:

1. File can be opened and has exactly one usable video selection.
2. Video sample MIME type is video/avc.
3. Width and height are 640 and 360 after rotation metadata.
4. Frame rate is present and no greater than 30.
5. AVC profile and level do not exceed Main Level 3.1.
6. Selected hardware decoder supports size and rate.
7. Audio is absent or AAC with supported channel and sample-rate values.
8. Duration is positive and samples can be read.
9. File size fits available app-private storage.

Pixel-format and malformed-bitstream issues not exposed by container metadata
may still appear during decoder initialization. Those are classified as
per-file failures, not capacity failures.

## FFprobe verification

Example WSL command:

    ffprobe -v error \
      -show_entries stream=index,codec_name,profile,pix_fmt,width,height,r_frame_rate,sample_rate,channels \
      -show_entries format=format_name,duration,size,bit_rate \
      -of json output.mp4

Expected video fields include:

- codec_name: h264
- profile: Main
- width: 640
- height: 360
- pix_fmt: yuv420p
- r_frame_rate: 30/1

Expected audio fields, when present:

- codec_name: aac
- profile: LC
- sample_rate: 48000
- channels: 2

## Future profiles

A 1280x720 profile may be considered for 4K televisions only after measuring
the target hardware. It must have a separate decoder budget because resolution
changes concurrent codec throughput. Mixing profiles in one wall is out of
scope for version one.
