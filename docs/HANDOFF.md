# Nuvio Superfork handoff

<!-- canonical-state:start -->
- Active gate: G0 — Fork Foundation
- Active branch: `chore/fork-foundation-impl`
- Status: READY
- Task: `tasks/G0_FORK_FOUNDATION.md`
- Accepted official baseline: `fd7973d91dd75d790c5f9b3d68dae652655e92c4`
- Governance: READY
- Owner of these fields: `integration/state.yaml`; regenerate with `state_view.py`.
<!-- canonical-state:end -->

## Current work boundary
Governance hardening is merged via PR #6 at `de1c8bfbc9926ef12ba0f4e5f20fc5ab7301f798`.
PR #5 is closed as superseded. G0 feature implementation has NOT started.

## Exact next action
1. Fetch origin and verify `chore/fork-foundation-impl` contains current `superfork/integration`.
2. Switch to the active branch and run `python3 scripts/superfork/preflight.py --fetch`.
3. Read `tasks/G0_FORK_FOUNDATION.md`; implement only FeatureId, FeatureMode,
   FeatureRegistry and SourceAttribution with OFF defaults plus foundation tests.
4. Run the required validator/tests/build, then update affected traceability rows and this handoff.
Use the optional writer lease only if another coding session may overlap.

## Evidence / limits
Accepted official sync: 3cf04ccdcc20515acb093c28ad9b7c3943a39057 (fd7973d baseline).
The G0 branch initially had zero unique commits; it can be advanced without rewriting history.
Independent clean baseline replays and full test identifiers are retained under
integration/evidence and baseline_test_debt.json. Local Android execution is unavailable
(no Android SDK/cache); use actual GitHub CI evidence, not a claimed local build.
No hardware tests run or claimed in this governance task.

## Ownership / unfinished work
No G0 writer is assigned. Sequential work may begin after fresh-branch/preflight checks; use an optional lease only for overlapping sessions. Coherent governance work
is committed through PRs; never resume an old governance branch as the feature branch.
Admin actions remain in GITHUB_ADMIN_CHECKLIST only. Current upstream observation is
explicitly unaccepted, preserving the latest accepted integration anchor.

## Governance verification
PR #6 exact head `df0b41491da0974d230392df5768562ebaa9ca15` passed Superfork Governance CI,
the complete no-new-regressions unit-suite guard, and Full Debug APK assembly in run
`36542083884` before squash merge. Integration push CI remains the final repository check
for the merged state.

Baseline replay attempt 2 confirms the additional HomeEnrichment case is intermittent,
not fixed: complete official suite returned to 18 failing / 1 skipped. Registry retains
19 explicit entries, with original failure and passing replay evidence. Governance and
Git-safety suite: 31 tests PASS (including writer contention, wrong release, stale branch,
dirty tree, duplicate feature IDs, test identity collisions, missing reports and new failures).