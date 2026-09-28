# Engineering Runbook

## Session start
1. Run `git status --short --branch`.
2. Run `git log --oneline -n 10`.
3. Read `docs/START_HERE.md`.
4. Read `integration/state.yaml` and `docs/HANDOFF.md`.
5. Verify the current branch equals the recorded task branch.
6. Verify no unexplained uncommitted changes.
7. Read the current gate in `docs/ROADMAP.md` and `docs/GATE_CHECKLISTS.md`.
8. Read relevant decisions/source research before coding.

## Before importing a fork feature
1. Identify MASTER_FEATURES ID(s).
2. Locate source in SOURCE_MAP.
3. Inspect pinned source SHA, exact commits and files.
4. Diff current official equivalent.
5. Decide import mode.
6. Record the source plan in IMPORT_LEDGER.
7. Import the smallest coherent behavior and its tests.

## During implementation
- Commit logical checkpoints.
- Avoid unrelated formatting/refactors.
- Keep fallback paths.
- Do not edit integration directly.
- Do not change source pins without an explicit recorded decision.
- Keep caches/queues/concurrency bounded.
- Treat Android TV focus/D-pad behavior as a first-class regression surface.

## Before handoff
1. Run focused tests.
2. Run required build.
3. Run `python3 scripts/superfork_guard.py`.
4. Run `git diff --check`.
5. Review `git status`.
6. Update IMPORT_LEDGER if code was imported.
7. Update integration/state.yaml and HANDOFF.
8. Commit.
9. Ensure working tree is clean.
10. Push task branch / update PR.

## Failure rule
Never hide a failure. Record exact command, error, classification, and next action. Use docs/RECOVERY.md.
