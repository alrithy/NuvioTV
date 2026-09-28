# Definition of Done

No gate or feature is complete until every applicable item below is satisfied.

## A. Scope
- [ ] Work belongs to the current roadmap gate.
- [ ] MASTER_FEATURES IDs affected are identified.
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
- [ ] Relevant items in docs/TEST_MATRIX.md executed.
- [ ] Playback-changing work compared against official behavior where feasible.
- [ ] TV D-pad/focus behavior checked for UI changes.
- [ ] Low-memory behavior considered for expensive features.
- [ ] Known regressions are documented.

## F. Git / CI
- [ ] Task branch is based on intended integration/baseline.
- [ ] Working tree is clean at handoff.
- [ ] Local test/build commands are recorded.
- [ ] Superfork CI is green, or a documented infrastructure-only exception exists.
- [ ] PR targets superfork/integration.

## G. Handoff
- [ ] integration/state.yaml updated.
- [ ] docs/HANDOFF.md updated.
- [ ] Latest commit SHA recorded.
- [ ] Feature IDs touched recorded.
- [ ] Files/modules changed recorded.
- [ ] Tests/build results recorded.
- [ ] Known issues recorded.
- [ ] Exact next action and recommended branch recorded.

If any required item fails, status is NOT DONE. Use BLOCKED or IN_PROGRESS, never claim completion.
