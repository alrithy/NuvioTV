# Task Packet — G7 Seek Intelligence
Feature IDs: 107–116
Branch: `feat/seek-intelligence`
Depends on: G6
Sources: Reshaped + Cxsmo.

## Objective
One hybrid preview path: local real keyframes -> calibrated Seekr -> normal seek fallback.

## Scope
Real keyframe timestamps, Seekr calibration/manual offset/weak-match rejection, bounded memory/disk, disk seek buffer under Seek Optimized strategy.

## Rules
Avoid extra network fetch when existing media/keyframes suffice. AdaptiveResourceManager owns memory/cache policy.

## Tests
Preview vs actual seek alignment, confidence rejection, long-file bounds, cleanup, weak-device behavior.
