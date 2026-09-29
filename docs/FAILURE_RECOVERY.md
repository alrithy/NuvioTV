# Failure recovery — canonical procedure

## Dirty tree / unknown work
Run `git status --short`, `git diff`, `git diff --cached`, and inspect untracked files.
Do not reset, clean, stash blindly or switch over unknown work. Preserve a coherent
checkpoint on a recovery branch (exclude secrets/build outputs); record owner, original
branch/HEAD, files, tests and reason in HANDOFF. Use a separate worktree for unrelated work.
A dirty tree blocks mutation-mode preflight until accounted for.

## Old/wrong branch or stale state
Fetch refs, then read `git show origin/superfork/integration:integration/state.yaml`.
Compare `git rev-list --left-right --count origin/superfork/integration...HEAD`.
Never trust an old local state file over fetched integration.
- Behind only, clean and no unique commits: `git merge --ff-only origin/superfork/integration`.
- Unique commits: inspect `git log origin/superfork/integration..HEAD` and preserve them.
  On the correct task branch, merge integration normally; no force-push.
- Wrong branch with useful commits: checkpoint, create a correct-base branch/worktree,
  cherry-pick only intended commits, then validate and document provenance.
- Missing branch: create it from current integration only after checking remote refs/PRs.

## Conflict / interrupted merge
Inspect `git status` and unmerged paths. Keep current official contracts and documented
local deltas; never choose an entire side blindly. Record each substantive resolution.
If abandoning the attempt, use `git merge --abort` only after preserving any work made
since the merge began. An interrupted agent never grants permission to discard work.

## Optional writer lease
Normal sequential work does not require a lease. If an optional overlap-protection lease
exists, inspect the remote branch, HANDOFF and lease before starting another writer on the
same branch. Do not steal an active lease. A stale/uncertain optional lease does not block
read-only inspection or work on a separate recovery/task branch; resolve ownership before
two writers touch the same task branch.

## CI / test failure
Run the canonical full-suite runner. Compile/dependency/worker errors, missing reports,
new failures, missing tests and newly skipped tests block. Compare clean accepted baseline
for suspected upstream debt; never expand the allowlist automatically. Use BASELINE_TEST_DEBT.
For runtime regressions, use official fallback and bisect coherent commits.

## Upstream/source moved
Keep pins; inspect observed upstream delta, use a dedicated reviewed sync PR and append
UPSTREAM_SYNC_LOG. See UPSTREAM_SYNC for freeze/convergence rules. Do not mix a sync with
feature work or overwrite an official subsystem with an older fork file.

## Security / release incident
Stop affected merge/release, revoke exposed credentials outside git, remove sensitive
logs/artifacts where possible, and record a redacted remediation trail. Revert through a
PR or release rollback without deleting history. Never publish credentials as evidence.