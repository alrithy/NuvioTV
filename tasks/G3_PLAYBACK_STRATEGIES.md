# Task Packet — G3 Playback Strategy Framework
Feature IDs: 3, 27–31
Branch: `feat/playback-strategies`
Depends on: G2

## Objective
Introduce strategy orchestration without changing official default behavior.

Required strategies: Official, REMUX/Throughput, Seek Optimized, Low Memory, Auto.

## Invariants
Official remains fallback/selectable. Strategy selection is visible to diagnostics. Strategies coordinate policies; they do not duplicate the player. Auto is deterministic/testable.

## Tests
Selection matrix, fallback/unsupported states, official parity, flags.

## Out of scope
No large ysosrs network delta or Reshaped disk-seek port yet; G4/G7 own those implementations.
