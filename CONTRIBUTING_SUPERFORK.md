# Contributing to the Nuvio Superfork

This policy applies only to Superfork branches rooted in `superfork/integration`.

The upstream `CONTRIBUTING.md` is preserved unchanged for compatibility with Nuvio upstream and the legacy `dev` branch. Its temporary restriction against feature work does **not** govern the Superfork roadmap.

## Superfork contribution model
- Work only from a documented gate/task branch.
- Follow `AGENTS.md`, `integration/state.yaml`, `docs/GATE_SPECS.md`, and `docs/DEFINITION_OF_DONE.md`.
- Feature work is allowed when it is part of the documented 320-feature scope and current roadmap gate.
- Reuse proven fork implementations first; never merge a whole fork.
- Preserve official fallback for high-risk core paths.
- Record source provenance and licensing for imported code.
- Keep PRs coherent and gate-scoped.
- Do not advance a gate on failed CI or undocumented manual-test gaps.

## Pull requests to superfork/integration
Use the Superfork PR template and satisfy:
- Superfork PR Policy
- Superfork Governance CI
- Superfork Full Debug CI
- gate-specific tests / manual status

## Pull requests to dev
The original upstream `CONTRIBUTING.md` and upstream PR policy apply.

## If policies conflict
For Superfork task/integration branches:
1. explicit user instruction
2. AGENTS.md
3. CONTRIBUTING_SUPERFORK.md
4. docs/DECISIONS.md / docs/GATE_SPECS.md
5. upstream CONTRIBUTING.md only where it does not conflict

For `dev`, upstream CONTRIBUTING.md remains authoritative.
