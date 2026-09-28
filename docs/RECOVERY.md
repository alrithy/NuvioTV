# Recovery & Failure Procedure

## Dirty working tree
1. Do not reset, clean, checkout-overwrite or discard.
2. Inspect `git status` and `git diff`.
3. Determine whether changes belong to the current task.
4. If ownership is unclear, preserve changes and document them before proceeding.

## Wrong branch
If implementation work is happening on `dev` or `superfork/integration`, stop coding and move to the task branch recorded in integration/state.yaml. Do not move uncommitted changes until they are understood.

## Merge/rebase conflict
1. Stop automatic conflict resolution if behavior is unclear.
2. Compare official/integration, current task, and imported fork source.
3. Preserve current official APIs unless the feature specifically requires a change.
4. Re-run focused tests after conflict resolution.
5. Record non-trivial resolutions in IMPORT_LEDGER/HANDOFF.

## Build fails
Classify the failure:
- deterministic code/test failure: fix before completion;
- missing secret/SDK/local environment: document exact missing requirement; never fake green;
- flaky/external network: retry only with evidence;
- unrelated upstream failure: prove via baseline comparison before an exception.

## Old fork code no longer applies
Do not force old files over current official code. Use DELTA_PORT or ADAPTER. If safe behavior cannot be preserved, mark BLOCKED and record evidence and recommended investigation.

## Suspected regression after another agent
Use Git history, IMPORT_LEDGER and HANDOFF to identify the introducing gate. Do not globally revert unrelated work.

## Lost handoff
Reconstruct from integration/state.yaml, current branch/HEAD, open PR diff, recent commits and IMPORT_LEDGER; then repair HANDOFF before feature work.
