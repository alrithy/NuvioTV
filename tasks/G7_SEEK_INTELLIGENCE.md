# Task Packet — G7 Seek Intelligence
Feature IDs: 107–116
Branch: `feat/seek-intelligence`
Depends on: G6
Sources: Reshaped + Cxsmo.

## Objective
One hybrid preview path: local real keyframes -> calibrated Seekr -> normal seek fallback.

## Scope
Real keyframe timestamps, Seekr calibration/manual offset/weak-match rejection, bounded preview memory/disk. Reuse G4 disk seek buffer (ID 25); do not reassign its primary gate.

## Rules
Avoid extra network fetch when existing media/keyframes suffice. AdaptiveResourceManager owns memory/cache policy.

## Tests
Preview vs actual seek alignment, confidence rejection, long-file bounds, cleanup, weak-device behavior.
