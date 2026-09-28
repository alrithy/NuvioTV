# Project Status — Read This First

## One-line status
The Superfork project operating system is merged into `superfork/integration`. G0 — Fork Foundation is READY on `chore/fork-foundation-impl`.

## Source of truth
Machine-readable state: `integration/state.yaml`.

## Current baseline
- Official upstream: `NuvioMedia/NuvioTV:dev`
- Pinned baseline: `c257a2365ee3386b582dc2974ec235cfe0381f33`
- Superfork integration: `superfork/integration`
- Operating-system bootstrap commit: `1edb0190c87f0932c7547c74de4265206e3a93a5`
- Legacy/default branch: `dev` — contains safety redirects only for Superfork discovery.

## Current work
- Gate: G0 — Fork Foundation
- Active branch: `chore/fork-foundation-impl`
- Status: READY
- Next gate: G1 — Unified Diagnostics & Add-on Health
- Next branch: `feat/unified-diagnostics`

## Agent start
Read `AGENTS.md`, run the governance validator, then follow `docs/AGENT_PLAYBOOK.md`.

## Hard stops
- no Superfork coding on dev
- no direct coding on superfork/integration
- no whole-fork merges
- no silent source-pin updates
- no parallel agents on the same task branch
- no fake hardware/manual test passes

## Repository note
GitHub Issues are currently disabled in this repository. Do not wait for or search for a task issue. `integration/state.yaml` + `docs/HANDOFF.md` + the active branch are the task source of truth.

## Coverage
`integration/feature_traceability.csv` maps all 320 feature IDs to a primary implementation gate. CI validates 320/320 coverage.
