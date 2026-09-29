# Nuvio Superfork handoff

<!-- canonical-state:start -->
- Active gate: G1 — Unified Diagnostics & Add-on Health
- Active branch: `feat/unified-diagnostics`
- Status: READY
- Task: `tasks/G1_UNIFIED_DIAGNOSTICS.md`
- Accepted official baseline: `fd7973d91dd75d790c5f9b3d68dae652655e92c4`
- Governance: READY
- Owner of these fields: `integration/state.yaml`; regenerate with `state_view.py`.
<!-- canonical-state:end -->

## Current work boundary
G0 is DONE: PR #7 squash-merged as `791716a86f1f363832f48e4ab950285f33a74ef1` after exact-head
CI run `36545736693` (governance PASS, full-suite guard `new_failures: []`, fullDebug APK PASS).
G1 is READY but must NOT start coding until the upstream sync below is merged.
`user_visible_behavior_change_allowed` is true for G1 (diagnostics/health UI), still behind
FeatureRegistry groups that default OFF unless a documented decision says otherwise.

## Exact next action
1. Upstream sync first (UPSTREAM_SYNC.md, D036/D037): official dev moved
   `fd7973d..71632b9` (7 first-parent merges incl. settings reorganization #3746 and
   PlayerRuntimeController/ViewModel changes). G1 touches settings (device assessment
   apply/revert, Add-on Health screen) and player diagnostics, so adopt it via a dedicated
   `chore/upstream-sync-2026-09-29` PR. A local dry-run merge onto integration had no conflicts.
   The sync PR must also update UPSTREAM_SYNC_LOG, SOURCE_MAP, BASELINE and
   baseline_test_debt.json (check_pr_scope requirement) with clean-baseline replay evidence.
2. After the sync merges: create `feat/unified-diagnostics` from integration HEAD, run
   `python3 scripts/superfork/preflight.py --fetch`, read `tasks/G1_UNIFIED_DIAGNOSTICS.md`.
3. G1 audit order: official `PlayerDebugStatsOverlay` and addon repository first; classify
   each of features 14–16, 61–81, 281–291 as already official / missing delta / unsupported /
   blocked; port only the missing ysosrs delta at the pinned SHA in SOURCE_MAP.

## G0 delivered (for reference)
`app/src/main/java/com/nuvio/tv/fork/foundation/`: FeatureMode (OFF/ON/AUTO), FeatureId
(14 module groups; AI_MEDIA and MAT_AUDIO experimental), FeatureRegistry (immutable, all OFF,
constructor `overrides` seam), SourceAttribution + ImportMode. Tests in the matching test
package (12). No persistence/UI/DI. Feature 318 deferred with reason in traceability.

## Evidence / limits
Accepted official baseline remains `fd7973d91dd75d790c5f9b3d68dae652655e92c4` until the sync PR merges.
Local Android execution is unavailable (no Android SDK); use actual GitHub CI evidence.
No hardware tests run or claimed.

## Ownership / unfinished work
Sequential writer, no lease. Admin actions (default branch, protection) remain in
GITHUB_ADMIN_CHECKLIST; connector access to those settings is not available.
