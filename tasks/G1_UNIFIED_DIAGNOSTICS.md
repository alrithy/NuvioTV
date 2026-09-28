# Task Packet — G1 Unified Diagnostics & Add-on Health
Feature IDs: 14–16, 61–81, 281–291
Branch: `feat/unified-diagnostics`
Depends on: G0 merged
Sources: current official first; ysosrs pinned SHA for missing deltas.

## Objective
Keep official `PlayerDebugStatsOverlay` as the single UI owner, establish one normalized diagnostics model, and port only missing device-assessment/diagnostics/add-on-health behavior.

## Mandatory audit
Inspect current official diagnostics/models first. Classify every desired behavior as already official, missing delta, unsupported, or blocked. Never create a second HUD.

## Required behavior
Device settings assessment + apply/revert; observable video/audio/network/buffer/passthrough/HDR/display/resource metrics; add-on health states and failure isolation. Unknown values must be unavailable/unsupported, never fabricated.

## Performance & security
No expensive polling/parsing on Compose hot paths. Never expose signed URLs, headers, cookies, tokens or API keys.

## Tests
Model normalization, unavailable states, health classification, retry/isolation, and overhead sanity. Relevant TEST_MATRIX smoke.

## Out of scope
No playback-strategy switching or adaptive resource decision logic yet.
