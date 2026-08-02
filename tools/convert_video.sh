#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'EOF'
Usage: convert_video.sh [--force] INPUT OUTPUT.mp4

Convert a local SDR video to the multiplayer grid profile:
  H.264 Main, yuv420p, 640x360, <=30 fps, constrained CRF
  AAC-LC stereo 48 kHz when the input contains audio

Run this script inside WSL. Windows files are available below /mnt/<drive>/.
EOF
}

overwrite=()
if [[ "${1:-}" == "--force" ]]; then
  overwrite=(-y)
  shift
fi

if [[ $# -ne 2 ]]; then
  usage >&2
  exit 2
fi

input=$1
output=$2

if [[ ! -f "$input" ]]; then
  printf 'Input does not exist: %s\n' "$input" >&2
  exit 1
fi

if [[ "${output,,}" != *.mp4 ]]; then
  printf 'Output must use the .mp4 extension: %s\n' "$output" >&2
  exit 1
fi

if [[ -e "$output" && ${#overwrite[@]} -eq 0 ]]; then
  printf 'Output already exists (pass --force to replace it): %s\n' "$output" >&2
  exit 1
fi

mkdir -p "$(dirname "$output")"

ffmpeg "${overwrite[@]}" -hide_banner -i "$input" \
  -map 0:v:0 -map '0:a:0?' -map_metadata -1 -map_chapters -1 \
  -vf "scale=640:360:force_original_aspect_ratio=decrease:force_divisible_by=2:reset_sar=1,pad=640:360:(ow-iw)/2:(oh-ih)/2:black,fps=30,format=yuv420p" \
  -c:v libx264 -preset medium -profile:v main -level:v 3.1 \
  -crf 23 -maxrate 1000k -bufsize 2000k -g 60 -keyint_min 30 \
  -c:a aac -profile:a aac_low -b:a 128k -ar 48000 -ac 2 \
  -movflags +faststart -fps_mode cfr \
  "$output"

printf 'Created %s\n' "$output"
