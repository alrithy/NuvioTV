# Regression Test Matrix

Use legal/user-owned or otherwise authorized media.

## Playback corpus
- 1080p H.264 WEB-DL
- 1080p HEVC
- 4K HEVC WEB-DL
- 4K REMUX moderate bitrate
- 4K REMUX 80–100 GB / high bitrate
- HDR10
- Dolby Vision Profile 5
- Dolby Vision Profile 7 where supported
- DD / DD+
- DD+ Atmos
- TrueHD / TrueHD Atmos where supported
- DTS / DTS-HD MA where supported
- malformed/truncated MKV fixture
- non-faststart MP4 fixture
- long seek-heavy file
- subtitle-rich dialogue scene

## Measure for playback-changing PRs
startup time, first frame, throughput, buffer ahead, rebuffering, wasted downloads where measurable, seek latency/correctness, audio selection/passthrough, HDR/DV, dropped frames, RAM peak, crash/ANR.

## Subtitle cases
Arabic aligned; fixed offset; gradual drift; piecewise drift; embedded English reference + Arabic target; no reference; low-confidence fail-closed; valid/invalid custom TTF/OTF.

## TV UI
D-pad focus, long-press duplication, Back behavior, dialog/player return focus, low-memory responsiveness.

## Live TV
M3U, Xtream, Stalker, EPG present/absent, rapid zapping, dead-channel retry, categories/search.

## Watch Party
play/pause, small-drift correction, large-drift seek, join/leave, URL/header redaction, room cleanup.
