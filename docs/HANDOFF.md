# Nuvio Superfork Handoff

## Current state
- Repository: alrithy/NuvioTV
- Official baseline: NuvioMedia/NuvioTV dev @ c257a2365ee3386b582dc2974ec235cfe0381f33
- Integration branch: superfork/integration
- Operating-system bootstrap: MERGED
- Bootstrap commit: 1edb0190c87f0932c7547c74de4265206e3a93a5
- Active implementation branch: chore/fork-foundation-impl
- Active roadmap gate: G0 — Fork Foundation
- Status: READY TO IMPLEMENT
- GitHub Issues: disabled; do not rely on issues for task discovery

## Environment completed
The repository contains:
- 320-feature authoritative scope
- full architecture and decision log
- fork research + pinned source SHAs
- 320/320 feature traceability
- G0–G14 gate specifications
- Definition of Done
- agent playbook + recovery runbook
- branching/concurrency/upstream policies
- security + release policies
- machine-readable state
- governance validator
- Superfork CI
- PR/issue/task templates
- default-branch safety redirects on dev

## Exact next action
On `chore/fork-foundation-impl`:
1. implement G0 foundation code from `docs/GATE_SPECS.md`;
2. create `com.nuvio.tv.fork.foundation`;
3. implement FeatureId, FeatureMode and FeatureRegistry with official-safe defaults;
4. add SourceAttribution utility;
5. add ForkSettingsDataStore only if actually required;
6. add tests;
7. run validator + fullDebug unit tests + assemble;
8. update state/handoff;
9. open PR to `superfork/integration`.

## Next after G0
- Gate: G1 — Unified Diagnostics & Add-on Health
- Branch: feat/unified-diagnostics

No user-facing fork feature should be imported before G0 is green.
