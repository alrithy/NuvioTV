# Task Packet — G9 Skip / Recommendations / Discovery
Feature IDs: 117–146, 169–187, 206–207
Branch: `feat/discovery-skip-recommendations`
Depends on: G8
Primary source: Cxsmo + current official.

## Scope
One skip aggregator; recap/credits/preview/content-warning/mute; provider isolation/timeouts/evidence/confidence; ID normalization; Calendar; Random/Mystery; post-play providers/pagination/lazy metadata/trailer fallback; App Dimmer.

## Rules
Keep skip vs mute distinct. Extend behind official skip seam. Spoiler-safe flows must not reveal Mystery metadata.

## Tests
Timeout/isolation, evidence merge/action conflict, Random/Mystery scope/unwatched fallback, Calendar watched updates, recommendation fallbacks.
