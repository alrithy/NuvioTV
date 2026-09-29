# Project status

<!-- canonical-state:start -->
- Active gate: G1 — Unified Diagnostics & Add-on Health
- Active branch: `feat/unified-diagnostics`
- Status: READY
- Task: `tasks/G1_UNIFIED_DIAGNOSTICS.md`
- Accepted official baseline: `fd7973d91dd75d790c5f9b3d68dae652655e92c4`
- Governance: READY
- Owner of these fields: `integration/state.yaml`; regenerate with `state_view.py`.
<!-- canonical-state:end -->

This view's header is generated from state; use `state_view.py` after state changes.

## Work boundary
G0 Fork Foundation is DONE (PR #7, merge `791716a`). G1 is READY; an upstream sync to
official `71632b9` is required first because G1 touches settings and player diagnostics
that changed upstream. No external fork feature has been imported yet. Legacy PR #1/#2
stay reference-only.

## CI and baseline
Full-suite evidence and remaining debt: BASELINE_TEST_DEBT and integration/evidence.
A no-new-regressions PASS is not an all-tests-pass claim. The governance PR exact head
passed the complete regression guard and Full Debug build before merge; integration push
CI remains authoritative for the current merged HEAD.

## Upstream
The accepted baseline and latest observed upstream HEAD are separate state fields.
Current observed official dev is `71632b9271e8bce6783e415d64f34cfa4e8b894c`; it is **not accepted** by observation alone.
The governance scope keeps the accepted baseline at `fd7973d91dd75d790c5f9b3d68dae652655e92c4`.
Use a dedicated reviewed upstream-sync PR before adoption. Never silently update source pins.

## Administration
GitHub connector branch-protection read returned 403; admin writes are not exposed.
Default branch and protection are manual actions in GITHUB_ADMIN_CHECKLIST. Existing
dev AGENTS/CLAUDE redirects were verified. They do not block authorized task development.