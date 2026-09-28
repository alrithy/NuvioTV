# Project Status — Read This First

## One-line status
The initial Superfork operating system is merged. Governance hardening is under review in PR #6. After #6 merges, implementation starts at G0 on `chore/fork-foundation-impl`.

## Source of truth
Machine-readable state: `integration/state.yaml`.

## Current baseline
- Official upstream: `NuvioMedia/NuvioTV:dev`
- Current implementation baseline: `fd7973d91dd75d790c5f9b3d68dae652655e92c4`
- Initial bootstrap anchor: `c257a2365ee3386b582dc2974ec235cfe0381f33`
- Integration upstream-sync commit: `3cf04ccdcc20515acb093c28ad9b7c3943a39057`
- Integration branch: `superfork/integration`
- Legacy/default fork branch: `dev` — do not build Superfork features directly there.

## Governance status
- Initial operating-system PR: #3 — merged.
- Governance hardening PR: #6 — review/CI.
- The hardening PR contains no user-facing feature implementation.

## Current implementation work
- Active gate: G0 — Fork Foundation
- Active implementation branch: `chore/fork-foundation-impl`
- Status: READY after governance hardening merges.
- Next gate: G1 — Unified Diagnostics & Add-on Health
- Next branch: `feat/unified-diagnostics`

## Agent start
Read `AGENTS.md`, run the validator, then follow `docs/AGENT_PLAYBOOK.md`.

## Hard stops
- no Superfork coding on dev
- no direct coding on superfork/integration
- no whole-fork merges
- no silent source-pin updates
- no parallel agents on the same task branch
- no fake hardware/manual test passes

## Coverage
`integration/feature_traceability.csv` maps all 320 feature IDs to G0–G14. The governance validator checks 320/320 completeness and text consistency.

## Test health
- Official-based current baseline: `fd7973d...`.
- Complete fullDebug unit suite: 18 known pre-existing failures recorded exactly in `integration/baseline_test_failures.txt`.
- Superfork policy: run the complete suite and fail on any new/unrecorded failure.
- Details: `docs/BASELINE_TEST_DEBT.md`.

## Remaining repository-admin actions
Default-branch migration and branch ruleset/protection are still repository-admin tasks. Until they are completed, the legacy `dev` branch contains explicit redirect instructions and Superfork process rules prohibit feature work there.