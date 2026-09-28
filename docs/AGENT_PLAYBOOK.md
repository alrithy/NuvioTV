# Agent procedure — canonical

## Start from any checkout
1. Read AGENTS, inspect status/branch/HEAD; preserve unknown work.
2. Fetch origin and official refs. Read **remote integration** state before trusting local state:
   `git show origin/superfork/integration:integration/state.yaml`.
3. Inspect all branches, open PRs and CI using GitHub tools (or `gh pr list`, `gh run list`).
   Compare exact PR head and integration HEAD, not an old successful run.
4. Read state -> generated PROJECT_STATUS/HANDOFF -> active task packet -> GATE_SPECS ->
   DoD -> DECISIONS -> relevant SOURCE_MAP/FORK_RESEARCH/COMPONENT_MAP/PORTING_PROTOCOL.
5. Switch/create the recorded task branch only with clean/preserved work. Follow
   FAILURE_RECOVERY for behind-only, divergence, conflicts or unknown commits.
6. Run `python3 scripts/superfork/preflight.py --fetch` and the governance validator.
7. Claim the single-writer lease using `claim_task.py claim --owner <unique-agent-session>`.
   Record the returned lease SHA and owner in HANDOFF/state, set IN_PROGRESS, commit and push.
   A claim conflict is a real ownership blocker; do not steal another writer's work.

Default-branch/protection admin restrictions do not block authorized development; see
GITHUB_ADMIN_CHECKLIST. If git write credentials are unavailable but the connector can
write: create the same unique lease commit then **create** its new ref atomically through
GitHub (never update an existing lock ref). Preserve the returned lease SHA. No writer
claim is needed for read-only inspection. A governance maintenance session uses its own
explicitly authorized branch, with the same one-writer rule.

## Implement only the active task
Identify feature IDs; use pinned source repo/branch/SHA; inspect source commits/files/tests;
diff current accepted official behavior and note any relevant newer upstream change.
Classify already-official, missing delta, obsolete or blocked. Use PORTING_PROTOCOL and
record IMPORT_LEDGER before imports. No fork import in G0. No later gate because it looks easier.
Update only the affected feature rows to in_progress. Preserve official fallback, single
owners and Arabic/generalized subtitle requirements. Feature flags follow FEATURE_FLAG_POLICY.

## Validate
```bash
python3 scripts/superfork/validate_project_state.py
python3 -m unittest discover -s scripts/superfork/tests
python3 scripts/superfork/run_full_unit_suite.py
./gradlew :app:assembleFullDebug --stacktrace
git diff --check
```
Add gate-specific TEST_MATRIX cases. Report PASS/FAIL/MANUAL-PENDING honestly. Full Debug
CI success means build + no new regressions, not that known baseline debt passed.
Any new failure, incomplete suite or newly skipped baseline test blocks.

## Handoff or agent switch
Commit coherent work. Update state/traceability, IMPORT_LEDGER when importing, and HANDOFF:
last verified commit (an ancestor is valid; never invent a self-referential HEAD), commands,
CI URLs/results, changed modules/feature IDs, blockers and exact next command.
Regenerate headers with `state_view.py`, rerun validator, push normally, verify clean tree
and remote HEAD, then release the exact lease SHA. Document interrupted/dirty work instead
of hiding it. The next writer claims a new lease and needs no chat history.

## PR / gate lifecycle
Open PR against superfork/integration. Keep the active gate unchanged while it is REVIEW.
Merge only on exact-head required checks + applicable DoD. After merge, a small governance
transition PR records completed gate PR/merge/evidence, selects the next queue row and
creates/fast-forwards its branch after the transition merges. Never record DONE prematurely.

## Upstream movement or blocker
Pins stay fixed until a dedicated sync PR is accepted. Use UPSTREAM_SYNC and append its log.
A moved HEAD is an observation, not permission to import it. Mark blocked/deferred only
with evidence, reason and next step. Ask the user only when repository/GitHub evidence
cannot resolve the target, authority, ownership or external dependency.
