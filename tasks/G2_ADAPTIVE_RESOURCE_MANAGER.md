# Task Packet — G2 Adaptive Resource Manager
Feature IDs: 262–280
Branch: `feat/adaptive-resource-manager`
Depends on: G1
Source: hackerslash Lite pinned SHA, plus current official behavior.

## Objective
Create one fork-owned resource policy used by later subsystems. Do not create a permanently stripped Lite build.

## Required behavior
Physical RAM detection; constrained/low-RAM tiers; bounded buffer/chunk/concurrency/cache recommendations; poster/animation/prefetch policy; bounded metadata/rating/offline queues; memory-safe preview policies.

## Rules
Strong devices must not be unnecessarily capped. Separate comfort/performance tuning from hard allocation-safety limits. Policy should be deterministic and testable without UI.

## Integration
Prefer `com.nuvio.tv.fork.resource`. Later gates consume policy; do not prematurely rewrite every call site.

## Tests
Tier boundaries, unknown/zero RAM fallback, strong-device behavior, cap invariants, deterministic outputs.

## Out of scope
No ysosrs throughput algorithm yet; no Seekr implementation yet; no Live TV preview implementation yet.
