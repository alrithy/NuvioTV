# Task Packet — G2 Adaptive Resource Manager
Feature IDs: 262–280
Branch: `feat/adaptive-resource-manager`
Depends on: G1
Source: hackerslash Lite pinned SHA + current official.

## Objective
Create one resource policy for later subsystems; never create a permanently stripped Lite product.

## Required behavior
Physical RAM detection; constrained/low-RAM tiers; bounded buffer/chunk/concurrency/cache recommendations; poster/animation/prefetch policies; bounded metadata/rating/offline queues; memory-safe preview policies.

## Rules
Strong devices must not be unnecessarily capped. Separate performance comfort limits from hard allocation safety. Keep policy deterministic/testable.

## Tests
Tier boundaries, unknown/zero RAM, strong-device behavior, caps/invariants.

## Out of scope
No ysosrs throughput algorithm, Seekr implementation or Live TV previews yet.
