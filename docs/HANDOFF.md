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
Governance hardening; G0 feature implementation has NOT started. PR #6 is the only
canonical governance PR. Supersession details are in GOVERNANCE_OWNERS.

## Exact next action
After governance is READY on integration:
1. Fetch origin, inspect open PRs/current checks and the active branch.
2. If chore/fork-foundation-impl is behind-only, fast-forward it to current integration.
   If it contains unique work, preserve and inspect it under FAILURE_RECOVERY.
3. Switch to the active branch, run `preflight.py --fetch`, acquire a unique writer lease.
4. Read tasks/G0_FORK_FOUNDATION.md, then implement only FeatureId, FeatureMode,
   FeatureRegistry and SourceAttribution with OFF defaults and foundation tests.
5. Run validator, governance tests, full-suite runner and fullDebug assemble. Record
   exact CI/commit evidence, update affected feature rows and this handoff, release lease.

## Evidence / limits
Accepted official sync: 3cf04ccdcc20515acb093c28ad9b7c3943a39057 (fd7973d baseline).
The G0 branch initially had zero unique commits; it can be advanced without rewriting history.
Independent clean baseline replays and full test identifiers are retained under
integration/evidence and baseline_test_debt.json. Local Android execution is unavailable
(no Android SDK/cache); use actual GitHub CI evidence, not a claimed local build.
No hardware tests run or claimed in this governance task.

## Ownership / unfinished work
No G0 writer is assigned. Acquire a lease before implementation. Coherent governance work
is committed through PRs; never resume an old governance branch as the feature branch.
Admin actions remain in GITHUB_ADMIN_CHECKLIST only. Current upstream observation is
explicitly unaccepted, preserving the latest accepted integration anchor.

## Last verified predecessor
`c96e6bcf3455449199fe9a934d89f03b290d05d4`: Superfork Governance CI and Full Debug CI
succeeded in run 36488329983 (full suite + debug APK). Later governance commits must
pass their own exact-head CI; this predecessor is evidence, not a claim about untested HEAD.

Baseline replay attempt 2 confirms the additional HomeEnrichment case is intermittent,
not fixed: complete official suite returned to 18 failing / 1 skipped. Registry retains
19 explicit entries, with original failure and passing replay evidence. Governance and
Git-safety suite: 31 tests PASS (including writer contention, wrong release, stale branch,
dirty tree, duplicate feature IDs, test identity collisions, missing reports and new failures).
