# Branching & Ownership

## Long-lived branches
- `dev`: legacy fork/default branch; not the Superfork integration target.
- `superfork/integration`: integration branch; PRs only by process.
- old `claude/*` branches: historical; not current state unless explicitly referenced.

## Recommended task branches
- chore/fork-foundation
- feat/unified-diagnostics
- feat/adaptive-resource-manager
- feat/playback-strategies
- feat/remux-performance
- feat/audio-dv-afr
- feat/subtitle-intelligence
- feat/seek-intelligence
- feat/stream-intelligence
- feat/discovery-skip-recommendations
- feat/live-tv
- feat/watch-party
- feat/ui-styles
- feat/experimental-media
- chore/upstream-sync-YYYY-MM-DD

## Ownership
Exactly one active writer/agent per task branch by default.

When switching agents:
1. commit completed work;
2. update HANDOFF and state;
3. leave a clean tree;
4. the new agent reads repository state before editing.

Never let Agent B start from chat instructions while Agent A has uncommitted changes.
