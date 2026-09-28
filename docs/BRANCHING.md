# Branching & Concurrency Policy

## Permanent branches
- `dev`: legacy/default fork branch; not the Superfork integration target.
- `superfork/integration`: only reviewed/green Superfork work lands here.

## Standard task branches
`chore/fork-foundation-impl`, `feat/unified-diagnostics`, `feat/adaptive-resource-manager`, `feat/playback-strategies`, `feat/remux-network`, `feat/audio-video`, `feat/subtitle-intelligence`, `feat/seek-intelligence`, `feat/stream-intelligence`, `feat/discovery-skip-recommendations`, `feat/live-tv`, `feat/watch-party`, `feat/ui-styles`, `experimental/ai-media`, `experimental/mat-audio`, `chore/release-hardening`.

## Rules
- No direct feature commits to integration.
- No Superfork feature work on dev.
- One active agent per task branch.
- Different agents may work concurrently only on explicitly independent branches.
- Before handing a branch to another agent, commit coherent work and update HANDOFF/state.
- Preserve provenance of imported code.
- Avoid giant cross-gate PRs.
