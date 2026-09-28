# Nuvio Superfork Handoff

## Current state
- Repository: `alrithy/NuvioTV`
- Current official baseline: `NuvioMedia/NuvioTV:dev@fd7973d91dd75d790c5f9b3d68dae652655e92c4`
- Integration branch: `superfork/integration`
- Initial governance PR: #3 — merged as `1edb0190c87f0932c7547c74de4265206e3a93a5`
- Official upstream sync: `3cf04ccdcc20515acb093c28ad9b7c3943a39057`
- Current governance hardening PR: #6 from `chore/governance-hardening`
- Active implementation gate: G0 — Fork Foundation
- Active implementation branch: `chore/fork-foundation-impl`
- Implementation status: READY after PR #6 merges

## Completed preparation
The repository contains:
- authoritative 320-feature scope;
- one-row-per-feature traceability with G0–G14 ownership;
- architecture and durable decision log;
- pinned fork source map + detailed research notes;
- exact gate deliverables and exit criteria;
- Definition of Done;
- agent playbook, failure recovery, branch/concurrency policy;
- upstream sync procedure + immutable sync log;
- security and release policies;
- machine-readable project state;
- governance validator;
- Superfork CI and PR policy;
- import ledger, ADR/task templates and admin checklist.

## Exact next action after PR #6 merges
1. Create `chore/fork-foundation-impl` from the resulting `superfork/integration` HEAD.
2. Work only on G0 from `docs/GATE_SPECS.md`.
3. Implement FeatureId / FeatureMode / FeatureRegistry / SourceAttribution foundation without user-visible behavior changes.
4. Mark affected feature rows `in_progress` while coding, then honest completion statuses.
5. Run:
   - `python3 scripts/superfork/validate_project_state.py`
   - `./gradlew :app:testFullDebugUnitTest --stacktrace`
   - `./gradlew :app:assembleFullDebug --stacktrace`
6. Update state, PROJECT_STATUS, HANDOFF and traceability.
7. Open a PR to `superfork/integration`.

## Next after G0
- G1 — Unified Diagnostics & Add-on Health
- Branch: `feat/unified-diagnostics`

No user-facing fork feature should be imported before G0 is green.
