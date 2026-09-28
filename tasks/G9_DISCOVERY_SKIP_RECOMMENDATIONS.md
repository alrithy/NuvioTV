# Task Packet — G9 Skip / Recommendations / Discovery
Feature IDs: 117–146, 169–187, 206–207
Branch: `feat/discovery-skip-recommendations`
Depends on: G8
Primary source: Cxsmo + current official.

## Scope
One multi-provider skip aggregator; recap/credits/preview/content-warning/mute actions; provider isolation/timeouts/evidence/confidence; ID normalization; Calendar; Random/Mystery; recommendation providers/pagination/lazy metadata/trailer fallback; App Dimmer.

## Rules
Keep skip vs mute actions distinct. One aggregator behind official skip integration seam. Spoiler-safe states must not reveal hidden episode metadata.

## Tests
provider timeout/isolation, evidence merge, action conflict, Random/Mystery scope/unwatched fallback, Calendar watched updates, recommendation provider fallback.
