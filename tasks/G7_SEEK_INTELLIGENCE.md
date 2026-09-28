# Task Packet — G7 Seek Intelligence
Feature IDs: 107–116
Branch: `feat/seek-intelligence`
Depends on: G6
Sources: Reshaped + Cxsmo pinned SHAs.

## Objective
One hybrid preview path:
1. local real keyframes when available;
2. calibrated Seekr fallback;
3. normal seek bar fallback.

## Scope
Real keyframe timestamps, Seekr calibration, manual offset, weak-match rejection, bounded memory/disk, disk seek buffer under Seek Optimized strategy.

## Rules
Do not create an extra network fetch when local preview can use existing media/keyframes. ResourceManager controls expensive memory/cache behavior.

## Tests
Preview/actual seek alignment, calibration confidence, long-file memory bounds, cleanup, weak-device behavior.
