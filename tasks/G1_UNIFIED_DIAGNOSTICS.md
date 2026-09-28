# Task Packet — G1 Unified Diagnostics & Add-on Health
Feature IDs: 14–16, 61–81, 281–291
Branch: `feat/unified-diagnostics`
Depends on: G0 merged

## Objective
Keep official `PlayerDebugStatsOverlay` as the single UI owner, establish one normalized diagnostics model, and port only missing ysosrs device-assessment/diagnostic/add-on-health deltas.

## Source
Official current baseline first. Delta source: ysosrs pinned in SOURCE_MAP.

## Mandatory audit
- inspect current PlayerDebugStatsOverlay and related models;
- identify which desired metrics already exist;
- inspect ysosrs pinned code/commits;
- classify each feature ID as already official / missing delta / unsupported;
- never create a second competing HUD.

## Required behavior
Device settings assessment + apply/revert; video/audio/network/buffer/passthrough/HDR/frame/display/resource metrics where actually observable; add-on health states and failure isolation.

Metrics that cannot be measured honestly must show unavailable/unsupported rather than fabricated values.

## Performance
No expensive polling or parsing on Compose hot paths. Sampling must be bounded and lifecycle-aware.

## Security
Never expose signed URLs, Authorization/Cookie headers, API keys, or secret query parameters in diagnostics.

## Tests
Model normalization, unavailable states, health-state classification, retry/isolation logic, and overhead sanity. Relevant TEST_MATRIX playback smoke.

## Out of scope
Do not add playback strategy switching or resource-tier policy yet; expose hooks/data only if G2/G3 will own decisions.
