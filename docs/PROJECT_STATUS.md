# Project Status — Read This First

## One-line status
The Nuvio Superfork operating system is being installed. After this documentation PR is merged into `superfork/integration`, Gate 0 implementation starts on `chore/fork-foundation-impl`.

## Source of truth
Machine-readable state: `integration/state.yaml`.

## Current baseline
- Official upstream: `NuvioMedia/NuvioTV:dev`
- Pinned baseline: `c257a2365ee3386b582dc2974ec235cfe0381f33`
- Integration branch: `superfork/integration`
- Legacy/default fork branch: `dev` — do not build Superfork features directly there.

## Current work
- Gate: G0 — Fork Foundation
- Intended implementation branch: `chore/fork-foundation-impl`
- Status: READY
- Next gate after G0: G1 — Unified Diagnostics
- Next branch: `feat/unified-diagnostics`

## Agent start
Read `AGENTS.md`, run the validator, then follow `docs/AGENT_PLAYBOOK.md`.

## Hard stops
- no Superfork coding on dev
- no direct coding on superfork/integration
- no whole-fork merges
- no silent source-pin updates
- no parallel agents on the same task branch
- no fake hardware/manual test passes

## Coverage
`integration/feature_traceability.csv` maps all 320 feature IDs to a primary implementation gate. CI validates 320/320 coverage.
