# Nuvio Superfork — Personal Plugin v1

This repository is both the plugin source and the sole project source of truth.
Root `plugin.json` uses Agent Plugins 1.0.0; skills are discovered from `skills/`.
OpenAI presentation/hooks live in `extensions.com.openai`. No MCP, server,
database, compatibility manifest or second project-state registry is needed.

## Existing system and live files

The plugin extends the existing AGENTS/preflight/validators/playbook; it does not
replace them or alter application architecture. In every session skills read
the **working checkout**, not the installed plugin cache:

| Requested concept | Existing canonical path |
|---|---|
| MASTER_FEATURES | `docs/MASTER_FEATURES.md` (relevant scope/IDs) |
| feature_traceability | `integration/feature_traceability.csv` (affected rows) |
| SOURCE_MAP / pinned SHA | `docs/SOURCE_MAP.md` (relevant sources) |
| AGENT_HANDOFF | `docs/HANDOFF.md` (already owns continuation) |
| Task/branch | `integration/state.yaml` and remote integration state |
| RECOVERY | `docs/RECOVERY.md` redirects to `docs/FAILURE_RECOVERY.md` |

No feature list, source SHA, task progress or handoff content is embedded into
plugin instructions. Read GitHub remote state/PRs/exact-head checks to reconcile
stale local summaries. A fresh agent needs no previous conversation.

| Skill | Invocation / purpose |
|---|---|
| nuvio-orchestrator | `اشتغل على نوفيو` — recover state and continue authorized units |
| nuvio-status | `شيك لي` — read-only Git/GitHub status with evidence |
| nuvio-feature-port | Check official upstream, then pinned source and smallest delta |
| nuvio-ci-recovery | Recover safely under existing policies; diagnose exact-head CI |
| nuvio-handoff | Persist traceability/handoff after each unit and continue |

Arabic phrases are skill-description triggers, not a deterministic command
router. If automatic selection fails, explicitly select the installed skill.
`شيك لي` never authorizes application edits. Development continues between units
under the live task/merge policies. Device-only tests remain MANUAL-PENDING during
feature integration; final hardware certification/release rules still apply.

## Prerequisites

- Current Codex CLI with `codex plugin`/`/plugins` and Agent Plugins support,
  or supported local Codex in the ChatGPT desktop app.
- Git and Python 3.9+ (`python3` on PATH). Hook uses only the standard library.
- Governance validation uses `scripts/superfork/requirements.txt` (PyYAML).
- Access to GitHub through Git and authenticated `gh`, existing GitHub tools,
  or equivalent API access. No GitHub credentials are supplied by this plugin.
- Android SDK/JDK/build dependencies for application gates, or exact-head CI.

## Install locally

Check out the branch/commit containing this plugin in `alrithy/NuvioTV`, rather
than legacy `dev`. Until merged, use the delivery branch shown in the delivery
report. After merging, use `superfork/integration`.

From that checkout:

```bash
python3 -m pip install -r scripts/superfork/requirements.txt
python3 -B scripts/superfork/validate_plugin.py
codex plugin marketplace add "$PWD"
codex plugin add nuvio-superfork@nuvio-personal
```

The checked-in `.agents/plugins/marketplace.json` exposes `./` relative to the
repository root, not relative to `.agents/plugins`. Do not copy project state
to a personal config/skill folder. The host installs a cached bundle; that cache
supplies workflows only. Avoid installing a second copy of the same skills/hooks
under `.agents/skills` or `.codex/hooks.json`.

In the ChatGPT desktop app, restart, choose this local marketplace in Plugins
and install **Nuvio Superfork**. Availability of personal/repo marketplaces
depends on the client/workspace. Local checkout installation does not publish
to the public directory or all ChatGPT accounts.

## First Codex experiment

Open Codex in the working repository:

```bash
codex -C /absolute/path/to/NuvioTV
```

Inspect `/plugins` and confirm Nuvio Superfork is installed/enabled. Open `/hooks`,
review the SessionStart definition and `hooks/session_start.py`, then trust it.
Codex requires this review for non-managed hooks and skips untrusted hooks.
Start a **new session** after install/trust. Then enter:

```text
اشتغل على نوفيو
```

The agent reads checkout state, safely recovers/selects the real active task,
checks upstream and continues authorized work. The plugin delivery branch is
not the application task branch; task selection comes from live remote state.
For a status-only first smoke check, enter `شيك لي` instead. In Codex CLI use
`$nuvio-orchestrator` / `$nuvio-status` explicitly if needed; in ChatGPT select
the installed skill using `@`.

## SessionStart safety and fallback

The hook reads session `cwd` from JSON stdin and resolves its Git root, even
from a subdirectory. It checks origin identity, branch, HEAD, all staged/unstaged/
untracked status, dirty tree, credential-redacted remotes, cached remote task
hints, live checkout handoff and traceability availability/active-gate rows.
It reports missing/conflicting state rather than estimating work.

No fetch/network, switch, reset, clean, stash, lease, file write or recovery occurs.
Git optional locks, fsmonitor and lazy fetching are disabled. `python3 -B` avoids
bytecode writes. Hook output goes to stdout as SessionStart additionalContext;
the host may persist its own event/context logs outside the hook. Cached remote
hints are clearly unverified; live GitHub verification belongs to the skill.

Plugin command hooks are unsupported in cloud-orchestrated ChatGPT Work and the
IDE extension does not support plugins. The skills therefore require the same
manual preflight when hook output is absent. A shell-capable agent can run the
installed script from the working checkout, supplying its current directory:

```bash
python3 -c 'import json,os; print(json.dumps({"cwd": os.getcwd()}))' |
  python3 -B /absolute/path/to/installed/nuvio-superfork/hooks/session_start.py
```

This hook is advisory; it is not a sandbox enforcement mechanism. The repository
validator/preflight validates full YAML; hook task scalars are only hints.

## Test

```bash
python3 -B scripts/superfork/validate_plugin.py
python3 -B -m unittest discover -s scripts/superfork/tests
python3 -B scripts/superfork/validate_project_state.py
python3 -B scripts/superfork/state_view.py --check
git diff --check
```

Plugin tests validate structure/frontmatter and exercise real temporary Git
repositories: clean/dirty/staged/untracked, stale/wrong/detached branches, identity
mismatch, missing files, credential redaction, checkout boundaries, subdirectory
startup and the hook wire contract. They compare files **including Git metadata**
before/after inspection. CI runs these alongside the existing governance suite.
Application work still requires its full DoD automated gates.

Host smoke test: register/install using the commands above, start a new session,
review `/hooks`, verify the five skills appear, run both Arabic prompts and check
their evidence. Do not treat a structure test as proof of autonomous feature
correctness, current GitHub CI or hardware PASS.

## Update

Update plugin files in this GitHub repository through a scoped governance PR,
bump `plugin.json` version, run validation, and commit/push normally. Do not
change source pins/task state just to update the plugin. Pull/fast-forward the
plugin-source checkout only after preserving unknown work.

For the local marketplace, refresh the installed cache by reinstalling:

```bash
codex plugin remove nuvio-superfork@nuvio-personal
codex plugin add nuvio-superfork@nuvio-personal
```

Start a new session; review/trust any changed hook again. For a Git-backed
marketplace, first use `codex plugin marketplace upgrade nuvio-personal`, then
refresh the install. That upgrade command refreshes Git snapshots, not local
working directories. The desktop app also supports refreshing/reinstalling
from its plugin browser after updating the source and restarting.

## Remove

```bash
codex plugin remove nuvio-superfork@nuvio-personal
codex plugin marketplace remove nuvio-personal
```

Or uninstall in the supported Plugins browser. Start a new session. Removal
clears the installed cache/registration; it does not delete repository history,
project handoff or application files. The repo marketplace remains discoverable
while present in the checkout; disable/uninstall its entry in `/plugins` when
needed. No MCP connections or service credentials need removal for v1.

## Format references

Verified against current docs and `codex-cli 0.159.0-alpha.3` during v1 authoring:

- https://developers.openai.com/plugins/build/plugins
- https://agent-plugins.org/schemas/1.0.0/plugin.schema.json
- https://learn.chatgpt.com/docs/hooks
- https://developers.openai.com/codex/plugins

Client availability, hook trust and command syntax may differ in older releases.
Use CLI help and the current official docs when updating the package.
