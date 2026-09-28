# Agent Playbook

## A. Session start — mandatory
Run:
```bash
git status --short
git branch --show-current
git rev-parse HEAD
python3 scripts/superfork/validate_project_state.py
```

Read: state -> PROJECT_STATUS -> HANDOFF -> active GATE_SPECS -> PRODUCT_REQUIREMENTS/NON_GOALS -> DECISIONS -> relevant COMPONENT_MAP/FORK_RESEARCH -> PORTING_PROTOCOL when importing -> TEST_STRATEGY/TEST_MATRIX -> DEFINITION_OF_DONE -> STATUS_MODEL. Read SECURITY/DEPENDENCY/DATA_MIGRATION policies when the task touches those boundaries.

## B. Branch safety
- On `dev`: stop coding; switch/create the active task branch from `superfork/integration`.
- On `superfork/integration`: stop coding; create/switch to the active task branch.
- If branch differs from `state.yaml: active_branch`, inspect unfinished work before any switch/reset.
- Never force-push unless explicitly instructed.
- Never run two agents concurrently on the same task branch.

## C. Before importing a fork feature
1. Identify MASTER_FEATURES IDs.
2. Confirm gate/source in feature_traceability.csv.
3. Confirm pinned SHA in SOURCE_MAP.
4. Read relevant FORK_RESEARCH section.
5. Diff against current official.
6. Choose the smallest import mode.
7. Record provenance in IMPORT_LEDGER.
8. Set affected traceability rows to `in_progress` when implementation begins.
9. Port behavior + tests together.

## D. While coding
Keep official fallback for core paths, one owner per concern, and diff scoped to the active gate. Preserve A/B path for high-risk playback changes until validated.

## E. Validation
Minimum:
```bash
python3 scripts/superfork/validate_project_state.py
./gradlew :app:testFullDebugUnitTest --stacktrace || true
python3 scripts/superfork/check_baseline_test_failures.py app/build/test-results/testFullDebugUnitTest integration/baseline_test_failures.txt
./gradlew :app:assembleFullDebug --stacktrace
```
Run gate-specific checks too. Hardware tests are PASS/FAIL/MANUAL-PENDING; never invent results.

## F. Handoff
Commit coherent work, update IMPORT_LEDGER when applicable, update affected feature_traceability statuses, state, PROJECT_STATUS and HANDOFF, rerun validator, leave clean tree or document why not, and record exact next action.

## G. Recovery
Never discard unknown changes first. Inspect status/diff/history. Git history wins factual disputes; reconcile docs before continuing.

## H. Source moved
Use pinned SHA. Updating a pin is a separate explicit decision.

## I. Blocked
Mark blocked with exact reason and next action. Do not silently skip a feature or advance the gate.