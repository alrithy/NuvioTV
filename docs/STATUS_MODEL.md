# Project Status Model

## Feature status — integration/feature_traceability.csv

### planned
Feature is in scope but implementation/audit has not started.

### in_progress
Active work or source audit has started. There must be a current task branch/handoff explaining the work.

### implemented
Our Superfork contains the required behavior and applicable Definition of Done/tests are satisfied.

### verified_official
No fork port is needed because the current official baseline already satisfies the requested behavior. Evidence must be recorded in IMPORT_LEDGER or the gate audit.

### experimental
Behavior exists only behind an experimental/OFF-by-default path and is not claimed stable.

### blocked
Work cannot currently proceed due to a concrete technical/external blocker. HANDOFF/ledger must state evidence and next action.

### deferred
Intentionally postponed by an explicit project decision, not silently skipped. Reason must be recorded.

## Gate status — integration/state.yaml

### READY
Gate is prepared and may start.

### IN_PROGRESS
The active branch contains ongoing gate work.

### BLOCKED
Gate cannot satisfy exit criteria; exact blocker is documented.

### REVIEW
Implementation is complete enough for PR/CI/manual review, but not yet integrated.

### DONE
Gate has met Definition of Done, applicable feature rows have terminal honest statuses, and work is merged into integration.

## Allowed lifecycle
Typical:
`planned -> in_progress -> implemented`

Alternative:
`planned -> verified_official`
`planned/in_progress -> blocked -> in_progress`
`planned -> deferred`
`planned/in_progress -> experimental`

Do not use DONE/implemented to mean “code exists somewhere.” It means the project’s acceptance criteria were met.

## Manual-validation batching

For G1–G13, a gate whose implementation is merged and whose automated tests/CI are green may stop being the active development gate even when device-only checks remain `MANUAL-PENDING`.

Those gates are tracked in `integration/state.yaml: validation_pending_gates`. This is **not** a claim that the gate is fully validated or release-DONE. It means development may proceed while hardware evidence is intentionally batched.

All remaining MANUAL-PENDING hardware checks are resolved in the G14 final hardware certification before any Stable release claim. A FAIL there reopens the affected feature/gate for correction.

## Gate completion invariant
A gate cannot be DONE while one of its feature rows is unexplained `planned` or `in_progress`.

A gate may close with `blocked` or `deferred` rows only if the decision is explicit, visible and acceptable under that gate’s exit criteria. G14 must account for all 320 rows before release claims.