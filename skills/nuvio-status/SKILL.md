---
name: nuvio-status
description: "Use when the user says شيك لي, check Nuvio, or asks for Nuvio Superfork status. Report actual Git/GitHub, feature_traceability and AGENT_HANDOFF evidence without estimating progress or starting development."
---

# Evidence-based status

1. Inspect current checkout identity, branch, full HEAD, status/dirty tree,
   remotes and cached tracking refs. Read working-repo `AGENTS.md`,
   `docs/GOVERNANCE_OWNERS.md`, `integration/state.yaml`, `docs/HANDOFF.md`
   (AGENT_HANDOFF), `integration/feature_traceability.csv`, relevant
   `docs/MASTER_FEATURES.md` scope and `docs/SOURCE_MAP.md` entries this session.
   No embedded plugin copy or past conversation supplies project state.
2. Read **live GitHub** integration/task branch heads and remote state (GitHub
   connector, API, or `gh api`/`git ls-remote`). Inspect the recorded PR plus
   current open PRs for that task when it has closed. Inspect workflow runs,
   jobs/checks and combined status for the **exact PR/branch SHA**, including
   pending/failing checks. Existing authenticated GitHub tools are optional;
   this plugin ships no MCP. `gh auth status` alone is not proof an API works.
3. Compare local/cached/live heads and state, traceability rows and handoff next
   action. Flag stale/unavailable evidence explicitly. A merged PR is not proof
   the whole gate is complete; a green run for another SHA is not current green.
   Do not fetch (which writes Git metadata), switch branches or repair files in
   this read-only status workflow; leave those actions to authorized development.
4. Report branch + HEAD, clean/dirty, active task/gate, PR state + exact-head CI
   with URLs, affected feature statuses and next actions, real blockers, and
   manual pending checks separately. Use exact counts grouped by the repository's
   status vocabulary if requested, never an estimated completion percentage.
   If GitHub access fails, say remote status is unverified and include the error;
   never substitute a cached last-green claim. Read RECOVERY and its target if
   explaining a recovery action, but do not perform it for “شيك لي”.
