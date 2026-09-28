# START HERE — Nuvio Superfork

If you know nothing about this project, start here.

## 1. What is this project?
A maintainable "best of Nuvio forks" integration fork based on official NuvioTV `dev`.

We reuse proven implementations from selected forks instead of rewriting them from scratch, while keeping current official Nuvio as the integration anchor and fallback.

## 2. Where is the project right now?
Read these two files first:
1. `integration/state.yaml` — machine-readable current state.
2. `docs/HANDOFF.md` — human-readable current state and exact next action.

Do not infer progress from old chat history.

## 3. What do I work on?
Only the `current_task` in `integration/state.yaml`.

If that task is complete and all required checks pass, update state/handoff and move to the next roadmap gate. Do not skip gates.

## 4. Which branch?
- Never develop directly on `superfork/integration`.
- Work on the task branch recorded in `integration/state.yaml`.
- One coherent task branch per gate/feature.
- Do not run two agents concurrently on the same task branch unless explicitly coordinated.

## 5. Where does feature code come from?
Read:
- `docs/SOURCE_MAP.md` for pinned repositories/SHAs.
- `docs/FORK_RESEARCH.md` for audited implementation details/commits.
- `docs/DECISIONS.md` for architectural constraints.

Default strategy:
REUSE -> CHERRY-PICK -> FILE_PORT -> DELTA_PORT -> ALGORITHM_PORT -> ADAPTER -> REWRITE LAST.

Never merge an entire fork.

## 6. What is the complete requested scope?
`docs/MASTER_FEATURES.md` contains stable IDs 1–320.

Do not silently drop a feature. If a feature cannot be implemented now, mark it blocked/deferred with reason and evidence.

## 7. How do I know a task is finished?
Read `docs/DEFINITION_OF_DONE.md`.

At minimum:
- scope matches the current gate
- source audit completed
- attribution/import ledger updated
- relevant tests added/ported
- build/tests pass
- official fallback preserved for core changes where practical
- handoff and state updated
- working tree clean
- commit created

## 8. What if official Nuvio changed?
Follow `docs/UPSTREAM_SYNC.md`. Never silently repin the baseline or source SHAs.

## 9. What if the repository is dirty, conflicted, or another agent left work?
Follow `docs/RECOVERY.md`. Preserve work before changing anything.

## 10. What if I am Codex / Claude / another agent?
Same workflow. `AGENTS.md` is authoritative. `CLAUDE.md` only redirects Claude Code into the same process.

## 11. Current command from the user
If the user says only "اشتغل على نوفيو" / "work on Nuvio" / "continue Nuvio":
- do not ask them to restate the project;
- inspect state;
- continue the recorded current task;
- ask only for a genuinely blocking decision that repository evidence cannot resolve.
