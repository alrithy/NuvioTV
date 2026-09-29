# Nuvio Superfork handoff

<!-- canonical-state:start -->
- Active gate: G0 — Fork Foundation
- Active branch: `chore/fork-foundation-impl`
- Status: REVIEW
- Task: `tasks/G0_FORK_FOUNDATION.md`
- Accepted official baseline: `fd7973d91dd75d790c5f9b3d68dae652655e92c4`
- Governance: READY
- Owner of these fields: `integration/state.yaml`; regenerate with `state_view.py`.
<!-- canonical-state:end -->

## Current work boundary
G0 implementation is complete on `chore/fork-foundation-impl` and awaiting exact-head CI/merge.
Governance hardening is merged via PR #6 (`de1c8bfbc9926ef12ba0f4e5f20fc5ab7301f798`);
PR #5 is closed as superseded (every file it added exists on integration). No external fork
code was imported, so IMPORT_LEDGER is unchanged.

## G0 delivered
Package `app/src/main/java/com/nuvio/tv/fork/foundation/` (new files only, no official code touched):
- `FeatureMode.kt` — OFF / ON / AUTO.
- `FeatureId.kt` — the 14 module-level groups from the task packet, names unchanged;
  `experimental = true` only for AI_MEDIA and MAT_AUDIO.
- `FeatureRegistry.kt` — immutable EnumMap defaults, all OFF; optional constructor
  `overrides` map (defensively copied) is the only seam for later gates. No Context, I/O,
  persistence or UI.
- `SourceAttribution.kt` — provenance value (repository, ref, full pinned SHA, ImportMode,
  optional full-SHA source commits, optional FeatureId). IMPORT_LEDGER stays canonical.
Not created by design: ForkSettingsDataStore, settings UI, DI bindings, runtime activation.

Tests: `app/src/test/java/com/nuvio/tv/fork/foundation/` — FeatureRegistryTest (8) and
SourceAttributionTest (4). Verified locally in a standalone Kotlin 2.3.0/JUnit 4.13.2 JVM
harness (12/12 PASS). The Android full suite and fullDebug APK are verified only by GitHub CI.

Traceability: 1, 2, 311, 313, 314, 315, 317, 319 → implemented with evidence;
318 (Simple/Advanced settings) → deferred with reason/next action.

## Exact next action
1. Check exact-head Superfork CI + PR Policy for the G0 PR; fix any red result.
2. When green, merge to `superfork/integration`, then open a `chore/governance-g0-closeout`
   PR that records G0 in `completed_gates` (PR, merge SHA, CI run), sets G0 DONE and
   activates G1 (`feat/unified-diagnostics`, `tasks/G1_UNIFIED_DIAGNOSTICS.md`).
3. G1 reads the official diagnostics owner first and ports only the missing ysosrs delta.

## Evidence / limits
Accepted official sync: 3cf04ccdcc20515acb093c28ad9b7c3943a39057 (fd7973d baseline).
Observed official dev `71632b9271e8bce6783e415d64f34cfa4e8b894c` is not accepted; G0 does not
touch the subsystems it changes (settings PR #3746 per DECISIONS), so no sync was needed for G0.
Local Android execution is unavailable (no Android SDK); use actual GitHub CI evidence.
No hardware tests run or claimed.

## Ownership / unfinished work
Sequential writer, no lease. Admin actions (default branch, protection) remain in
GITHUB_ADMIN_CHECKLIST; connector access to those settings is not available.
