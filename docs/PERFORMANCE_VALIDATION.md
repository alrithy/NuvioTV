# Performance Validation Policy

Playback/performance claims require measured evidence, not intuition or fork README claims.

## Core metrics
- startup / first-frame time
- sustained network throughput
- required bitrate vs available throughput
- buffer-ahead
- rebuffer count/duration
- duplicate/wasted byte downloads where measurable
- seek latency/correctness
- dropped frames
- RAM peak / cache footprint
- audio underruns/jitter where available
- CPU/thermal indicators where available

## A/B rule
High-risk playback/network changes must compare the same media/source/device/network conditions against the official strategy or last-green Superfork baseline.

## Evidence
Store measurements in PR notes or a linked manual-test log. Developer claims from source forks are hypotheses until reproduced.

## Tradeoffs
A regression may be accepted only if explicitly documented and justified (for example memory traded for fewer rebuffers). Do not call a change "faster" or "better" without the metric being measured.

## Resource tiers
Performance must be evaluated against the Adaptive Resource Manager tiers once G2 exists; a high-memory optimization must not silently become the low-RAM default.
