# Nuvio Superfork handoff

<!-- canonical-state:start -->
- Active gate: G1 — Unified Diagnostics & Add-on Health
- Active branch: `feat/unified-diagnostics`
- Status: REVIEW
- Task: `tasks/G1_UNIFIED_DIAGNOSTICS.md`
- Accepted official baseline: `71632b9271e8bce6783e415d64f34cfa4e8b894c`
- Governance: READY
- Owner of these fields: `integration/state.yaml`; regenerate with `state_view.py`.
<!-- canonical-state:end -->

## Current work boundary
G0 DONE (PR #7). Upstream sync to official `71632b9` merged (PR #9, merge `a909827`, D040).
G1 is IN_PROGRESS on `feat/unified-diagnostics`, delivered as sequential PRs from this branch:
- **G1a add-on health (PR #10, merged `d8511d8`):** 281–287, 291 implemented; 288, 290
  verified_official; 289 deferred to G8 (D041). Audit: `docs/audits/G1_DIAGNOSTICS_AUDIT.md`.
- **G1b diagnostics HUD (PR #11, merged `70c99a2`):** 61–81; 63, 70, 72, 75, 78, 80 verified_official.
- **G1c device assessment (this PR):** 14–16 implemented — card in the official advanced settings list.
All G1 rows are now terminal (289 deferred). G1 becomes DONE via a closeout PR after this merges.

## G1a delivered (merged)
In-memory `AddonHealthTracker`, classifier, Addon Manager badge, FeatureRegistry D041 default.

## G1b delivered (merged)
PlaybackHud rows appended to the official stats HUD; reduced HudSample + AudioTrack encoding.

## G1c delivered
- `fork/diagnostics/DeviceAssessment.kt` (pure planner), `DeviceAssessmentApplier.kt` (snapshot +
  official setters + revert), `DeviceAssessmentViewModel.kt`, `DeviceAssessmentSection.kt`.
- `NetworkSettingsScreen` (AdvancedSettingsContent): `device_assessment` card, only while
  UNIFIED_DIAGNOSTICS is not OFF. Strings `assessment_*` in values + values-ar.
- Persistence: DataStore `fork_device_assessment` (schema in IMPORT_LEDGER G1c).
- Tests: DeviceAssessmentTest (7) passes in the local JVM harness; DeviceAssessmentApplierTest (6)
  runs only in the Android suite (CI).

## Exact next action
1. Drive the G1c PR to green (exact-head CI) and merge it (squash).
2. Open `chore/governance-g1-closeout`: record G1 in `completed_gates` (PRs #10, #11, G1c; merge
   SHAs; CI runs), activate G2 (`feat/adaptive-resource-manager`, `tasks/G2_ADAPTIVE_RESOURCE_MANAGER.md`).
3. Before G2: check official dev for new commits touching the resource/buffer owners
   (MemoryBudget, NuvioExoPlayerPerformanceHelper, load control); sync first only if they changed.

## Evidence / limits
Local Android execution is unavailable (no Android SDK); use GitHub CI evidence. Actions
artifacts cannot be downloaded from this agent environment (blob storage egress blocked);
the baseline-audit workflow prints the safe inventory to the job log instead.
No hardware/manual tests run or claimed. MANUAL-PENDING: TV focus of the add-on badge; on-device HUD values (refresh, passthrough, DV) for G1b; D-pad focus/apply/revert on the device assessment card for G1c.

## Ownership / unfinished work
Sequential writer, no lease. Admin actions (default branch, protection) remain in
GITHUB_ADMIN_CHECKLIST; connector access to those settings is not available.
