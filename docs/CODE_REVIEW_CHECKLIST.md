# Code Review Checklist

Use for every non-trivial feature/port PR.

## Scope / provenance
- [ ] Gate and feature IDs match the diff.
- [ ] Pinned source SHA/commits/files are documented.
- [ ] Current official equivalent was inspected.
- [ ] No whole-fork or stale-file overwrite.
- [ ] Explicitly excluded source behavior is listed.
- [ ] License notices are preserved.

## Architecture
- [ ] Existing subsystem owner remains singular.
- [ ] No duplicate ranker/subtitle engine/diagnostics HUD/skip engine/resource manager.
- [ ] Official fallback remains for high-risk core paths where practical.
- [ ] Experimental paths remain OFF by default.
- [ ] No hidden cross-gate work.

## Kotlin / concurrency / lifecycle
- [ ] Coroutine scope/cancellation follows lifecycle.
- [ ] No unbounded jobs/retries/flows.
- [ ] Timeouts exist for remote providers where appropriate.
- [ ] No race-prone duplicate player/network ownership.
- [ ] Cleanup occurs for sessions/temp files/WebViews/listeners.

## Android TV / Compose
- [ ] D-pad entry/exit/focus restoration is deterministic.
- [ ] No focus traps or duplicate click handling.
- [ ] Hot recomposition paths avoid expensive parsing/I/O.
- [ ] RTL/Bidi behavior considered for UI/text changes.
- [ ] Weak-device visual fallback exists when effects are expensive.

## Playback / network
- [ ] No duplicate download/retry loops.
- [ ] 429/503 and cancellation semantics are intentional.
- [ ] HLS/DASH/live/direct-file differences are considered.
- [ ] Range requests/headers do not leak credentials cross-host.
- [ ] Same-file A/B evidence exists for performance claims.

## Persistence / security
- [ ] New settings have safe defaults.
- [ ] Migration/collision risk reviewed.
- [ ] Secrets are Keystore-backed/profile-scoped where required.
- [ ] Signed URLs/auth headers/API keys are not logged.
- [ ] New permissions/dependencies are justified.

## Quality
- [ ] Feature tests ported/added.
- [ ] Full suite no-new-regressions guard passes.
- [ ] Relevant TEST_MATRIX scenarios recorded.
- [ ] Hardware/manual results use PASS/FAIL/MANUAL-PENDING honestly.
- [ ] State, traceability, ledger and handoff are updated.
