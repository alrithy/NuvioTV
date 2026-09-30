---
name: nuvio-orchestrator
description: "Primary trigger: اشتغل على نوفيو. Also use for work on Nuvio, continue Nuvio, or autonomous Nuvio Superfork development in alrithy/NuvioTV. Reconstruct the task from live repository files and continue coherent work units."
---

# Nuvio orchestration

The GitHub repository `alrithy/NuvioTV` is the sole project authority. No prior
conversation is required. This skill describes how to discover work, never its
current feature list, source pins, progress, architecture, or handoff contents.
User instructions determine scope; a plugin maintenance request does not authorize
unrelated application feature work.

1. Before edits, inspect repository identity (origin fetch/push), branch, HEAD,
   status including untracked/staged files, remotes and cached remote state.
   Use the SessionStart snapshot if available, but refresh Git evidence before
   writing. Without hooks, run the installed plugin's `hooks/session_start.py`
   from the working checkout, or perform the same inspection manually. Never
   read project state from the installed plugin cache.
2. Read the working repository's `AGENTS.md`, `docs/GOVERNANCE_OWNERS.md`,
   `docs/RECOVERY.md` and its canonical target `docs/FAILURE_RECOVERY.md`.
   Fetch origin and read remote `superfork/integration:integration/state.yaml`.
   If starting on legacy `dev`, inspect remote AGENTS/state before switching.
   Inspect/preserve dirty or unknown work first. Recover old/wrong branches,
   divergence and conflicts by the repository procedure; never discard work,
   blindly stash, reset/clean or force-push. Use a separate worktree for an
   explicitly requested independent governance task. A SessionStart hook itself
   must never fetch, switch, repair, claim ownership or update files.
3. Read the current `integration/state.yaml`, `docs/HANDOFF.md` (the existing
   AGENT_HANDOFF), task packet, affected `integration/feature_traceability.csv`
   rows, relevant `docs/MASTER_FEATURES.md` IDs and `docs/SOURCE_MAP.md` entries
   in **every session**, then `docs/AGENT_PLAYBOOK.md`, active gate specification
   and `docs/DEFINITION_OF_DONE.md`. Follow repository ownership: state owns
   branch/task, traceability owns feature evidence, handoff supplies continuation.
   Inspect current GitHub PRs and exact-head checks. Reconcile stale handoff
   claims with Git/GitHub evidence before selecting the next uncompleted unit.
   Missing or conflicting authority is a recovery problem, never a guessed task.
4. Use the recorded active task branch after safe recovery; run the existing
   governance validator and `python3 scripts/superfork/preflight.py --fetch`.
   Recheck remote task HEAD before mutation/push. Follow the repository's writer
   policy; normal sequential sessions need no invented lease requirement.
5. For feature work, follow `nuvio-feature-port`. Check current official upstream
   first, resolve sources through the freshly read SOURCE_MAP and pinned SHA,
   and implement the smallest correct unit with the existing application owners.
6. Run relevant automated gates from the live DoD/task/CI definitions. For code
   changes this includes the canonical full-suite regression guard, build and
   gate-specific tests. Use `nuvio-ci-recovery` for failures; no silent debt waiver.
   Record unavailable local checks and obtain exact-head GitHub CI evidence.
7. Follow the live manual-testing policy. During feature integration, device-only
   checks are `MANUAL-PENDING` with exact checks/evidence recorded. Continue when
   automated/merge requirements are satisfied; pending TV testing alone is not a
   development blocker. Final hardware/release certification still follows DoD.
8. After **each** coherent unit, use `nuvio-handoff` to update affected
   traceability, state and handoff with honest evidence. Re-read the task queue
   and live state, then continue the next authorized unit instead of ending after
   each feature. Advance gates only through the repository's merge/transition
   procedure; unmerged code is not DONE. Stop only for a real blocker, an unsafe
   unresolved decision, completed authorized scope, or user interruption. Leave
   an exact next action when interrupted or limited by the session.

For “شيك لي”, use `nuvio-status` as a read-only status request. Do not start
development merely because status inspection discovers remaining work.
