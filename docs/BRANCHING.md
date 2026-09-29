# Branch lifecycle and ownership

`superfork/integration` accepts GitHub PR merges only. `dev` is legacy/reference.
The only task branch selector is `integration/state.yaml`; future names/dependencies
come from `integration/task_queue.csv`. Never infer work from an old branch name.

Before editing, run `preflight.py --fetch`, inspect current remote PRs/CI, and acquire
a unique writer lease with `claim_task.py claim --owner <agent-session-id>`.
Exactly one writer owns a task branch. Read-only review may occur separately.
The lease is an atomic remote ref under `agent-locks/`; it is not the task branch.

When handing off: commit coherent work, record tests/state/HANDOFF and exact next step,
push normally, verify clean tree and remote HEAD, then release the lease using its exact
SHA. Another agent must acquire a new lease. Time alone does not prove a lease is abandoned.

After a gate PR merges, use a governance-only transition PR to advance state to the next
queue row, record the completed gate PR/merge/evidence, then create the new branch from
that integration HEAD. Gate PRs keep their own gate active through review.
Never mark DONE before the gate PR is merged and DoD has evidence.

If integration advanced: behind-only/no unique work => fast-forward; real divergent
work => preserve it, merge current integration into the task branch, resolve minimally,
rerun CI. Rebase is allowed on private/unpublished work. Do not rewrite published work
without explicit authorization. Recovery commands: FAILURE_RECOVERY.