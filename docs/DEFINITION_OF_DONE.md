# Definition of Done

This document distinguishes **development progression** from **final release validation**.

For G1–G13, the project may advance to the next development gate when implementation is merged, automated requirements below are satisfied, and any device-only checks are honestly recorded as `MANUAL-PENDING`. This does not turn unexecuted hardware tests into PASS and does not make the project release-certified.

G14 resolves the accumulated MANUAL-PENDING hardware checks before Stable release.

## A. Scope
- [ ] Work belongs to the current roadmap gate.
- [ ] MASTER_FEATURES IDs affected are identified.
- [ ] Corresponding feature_traceability rows are moved from planned/in_progress to an honest completion/block status before gate completion.
- [ ] No unrelated refactor or UI redesign is bundled into the task.
- [ ] Any intentionally deferred scope is recorded with reason.

## B. Source audit for imported behavior
- [ ] Source repo, branch and pinned SHA recorded.
- [ ] Exact source commit(s) and files identified where possible.
- [ ] Current official equivalent inspected.
- [ ] Already-upstream behavior excluded from duplicate import.
- [ ] Import mode selected.
- [ ] License/attribution implications checked.
- [ ] docs/IMPORT_LEDGER.md updated.

## C. Architecture
- [ ] Existing architectural owner is reused rather than duplicated.
- [ ] Core behavior has official fallback/flag where practical.
- [ ] No whole-fork merge occurred.
- [ ] No silent pinned-SHA update occurred.
- [ ] New settings have explicit defaults.
- [ ] Experimental features default OFF.

## D. Code quality
- [ ] Code compiles.
- [ ] New behavior has relevant tests.
- [ ] Fork source tests were ported/adapted when relevant.
- [ ] Error/timeout/fallback paths are handled.
- [ ] Sensitive URLs, headers, credentials and API keys are not logged.
- [ ] Resource usage is bounded for caches, queues, previews and background work.

## E. Regression
- [ ] Relevant automated items in docs/TEST_MATRIX.md executed.
- [ ] Playback-changing work compared against official behavior where feasible in automation.
- [ ] Device-only D-pad/focus/audio/video/performance checks are either executed or explicitly recorded as MANUAL-PENDING for the final hardware campaign.
- [ ] Low-memory behavior considered for expensive features.
- [ ] Known regressions are documented.

## F. Git / CI
- [ ] Task branch is based on intended integration/baseline.
- [ ] Working tree is clean at handoff.
- [ ] Local test/build commands are recorded.
- [ ] Superfork Governance CI and no-new-regressions test guard are green.
- [ ] Any pre-existing full-suite failures are handled only through docs/BASELINE_TEST_DEBT.md; new failures are never waived silently.
- [ ] PR targets superfork/integration.

## G. Handoff
- [ ] integration/state.yaml updated.
- [ ] integration/feature_traceability.csv updated for every affected feature ID.
- [ ] docs/PROJECT_STATUS.md updated.
- [ ] docs/HANDOFF.md updated.
- [ ] Latest commit SHA recorded.
- [ ] Feature IDs touched recorded.
- [ ] Files/modules changed recorded.
- [ ] Tests/build results recorded.
- [ ] Known issues recorded.
- [ ] Exact next action and recommended branch recorded.

If an automated/code requirement fails, use BLOCKED or IN_PROGRESS and do not advance.

A device-only MANUAL-PENDING item is different: record it, add the gate to `validation_pending_gates`, and continue development. Never call that manual item PASS without evidence. G14 cannot complete and Stable cannot ship until all release-blocking MANUAL-PENDING items are resolved.