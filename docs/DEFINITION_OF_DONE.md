# Definition of Done

## Feature-level DoD
- Feature ID(s) identified.
- Pinned source commits/files recorded.
- Official equivalent audited.
- No duplicate upstream implementation.
- Import mode and attribution recorded.
- Core fallback/flag preserved where practical.
- Tests added/ported.
- Build succeeds.
- Relevant regression checks pass.
- Sensitive logs/credential handling reviewed.
- IMPORT_LEDGER, state and handoff updated.
- PR lists intentionally excluded source behavior and risks.

## Gate-level DoD
- Every feature assigned to the gate is DONE or explicitly BLOCKED/DEFERRED with reason.
- Automated tests green.
- Manual/hardware cases honestly recorded.
- No unresolved high-severity regression.
- Official fallback works where required.
- Governance validator passes.
- `fullDebug` builds.
- Next gate/branch is explicit.

## Merge criteria
CI green, scoped/reviewable PR, attribution complete, no unrelated churn, and the applicable DoD is satisfied.

## Release-level DoD
Follow `docs/RELEASE_POLICY.md`.
