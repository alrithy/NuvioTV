# Agent procedure — canonical

## Start from any checkout
1. Read AGENTS, inspect status/branch/HEAD; preserve unknown work.
2. Fetch origin and official refs. Read **remote integration** state before trusting local state:
   `git show origin/superfork/integration:integration/state.yaml`.
3. Inspect the active task branch, its open PR if one exists, and exact-head CI. Inspect other branches/PRs only when state, preflight or recovery requires it.
4. Read HANDOFF -> active task packet -> active GATE_SPECS section -> DoD. Load DECISIONS/source/porting/policy docs only when the task touches them.
5. Switch/create the recorded task branch only with clean/preserved work. Follow FAILURE_RECOVERY for behind-only, divergence, conflicts or unknown commits.
6. Run `python3 scripts/superfork/preflight.py --fetch` and the governance validator.
7. For normal sequential work, no lease is required: verify the remote task-branch HEAD immediately before editing/pushing and rely on Git's non-fast-forward protection.
   If two coding sessions might overlap, optionally claim the branch with `claim_task.py claim --owner <unique-agent-session>`; a claim conflict then means another writer owns that branch.

Default-branch/protection admin restrictions do not block authorized development; see
GITHUB_ADMIN_CHECKLIST. The optional lease exists only as a coordination aid for overlapping
sessions; it is not part of the normal sequential Codex/Claude workflow. No writer claim is
needed for read-only inspection.

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
Regenerate headers with `state_view.py`, rerun validator, push normally, then verify the
remote HEAD still equals the commit you pushed. Release an optional lease only if you used one.
Document interrupted/dirty work instead of hiding it. The next sequential writer needs no chat history.

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