# Task Packet — G3 Playback Strategy Framework
Feature IDs: 3, 27–31
Branch: `feat/playback-strategies`
Depends on: G2
Sources: official + architecture glue; later deltas from ysosrs/Reshaped.

## Objective
Introduce a strategy layer without changing official default behavior.

Strategies:
- Official
- REMUX / Throughput
- Seek Optimized
- Low Memory
- Auto

## Invariants
Official remains fallback/selectable. Strategy selection is observable in diagnostics. Strategy objects coordinate policies; they must not duplicate the player.

Auto may use device/resource/network/media facts but must be deterministic/testable.

## Tests
Selection matrix, fallback, unsupported conditions, official-default parity, feature flag behavior.

## Out of scope
Do not yet import large ysosrs network delta or Reshaped disk seek implementation; G4/G7 do that behind this framework.
