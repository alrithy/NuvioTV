# Nuvio Superfork handoff

<!-- canonical-state:start -->
- Active gate: G1 — Unified Diagnostics & Add-on Health
- Active branch: `feat/unified-diagnostics`
- Status: READY
- Task: `tasks/G1_UNIFIED_DIAGNOSTICS.md`
- Accepted official baseline: `71632b9271e8bce6783e415d64f34cfa4e8b894c`
- Governance: READY
- Owner of these fields: `integration/state.yaml`; regenerate with `state_view.py`.
<!-- canonical-state:end -->

## Current work boundary
G0 is DONE (PR #7, merge `791716a`; closeout PR #8, merge `59ed661`).
Upstream sync PR #9 adopts official `71632b9271e8bce6783e415d64f34cfa4e8b894c`
(integration merge `898bc83`, D040). G1 is READY; do not start G1 coding until PR #9 is merged.
`user_visible_behavior_change_allowed` is true for G1 (diagnostics/health UI), behind
FeatureRegistry groups that default OFF unless a documented decision says otherwise.

## Exact next action
1. If PR #9 is still open: it needs exact-head maintainer approval for the reviewed inventory
   removal (D040; Baseline Change policy) — a maintainer must approve the PR at its head SHA or
   comment `BASELINE_DEBT_APPROVED <head sha>`. Agents must not self-approve. Then squash-merge.
2. After PR #9 merges: create `feat/unified-diagnostics` from integration HEAD, run
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
Sync evidence: clean replays in PR #9 (UPSTREAM_SYNC_LOG 2026-09-29); inventory recorded from
the replay job log because this agent environment cannot download Actions artifacts
(blob storage egress blocked). Local Android execution is unavailable (no Android SDK); use
actual GitHub CI evidence. No hardware tests run or claimed.

## Ownership / unfinished work
Sequential writer, no lease. Admin actions (default branch, protection) remain in
GITHUB_ADMIN_CHECKLIST; connector access to those settings is not available.
