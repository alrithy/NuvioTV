# Project status

<!-- canonical-state:start -->
- Active gate: G1 — Unified Diagnostics & Add-on Health
- Active branch: `feat/unified-diagnostics`
- Status: REVIEW
- Task: `tasks/G1_UNIFIED_DIAGNOSTICS.md`
- Accepted official baseline: `71632b9271e8bce6783e415d64f34cfa4e8b894c`
- Governance: READY
- Owner of these fields: `integration/state.yaml`; regenerate with `state_view.py`.
<!-- canonical-state:end -->

This view's header is generated from state; use `state_view.py` after state changes.

## Work boundary
G0 DONE (PR #7). Official baseline `71632b9` accepted (PR #9). G1 REVIEW: G1a add-on
health (PR #10) and G1b diagnostics HUD (PR #11) merged; G1c device assessment in review; closeout
PR follows. Legacy PR #1/#2 stay reference-only.

## CI and baseline
Full-suite evidence and remaining debt: BASELINE_TEST_DEBT and integration/evidence.
A no-new-regressions PASS is not an all-tests-pass claim. Reviewed inventory: clean official
`71632b9` replay, 1,752 tests, 18 known failures + 1 intermittent registered, 1 skipped.

## Upstream
Accepted official baseline: `71632b9271e8bce6783e415d64f34cfa4e8b894c` (UPSTREAM_SYNC_LOG
2026-09-29). Future movement uses dedicated reviewed sync PRs; never silently update pins.

## Administration
GitHub connector branch-protection read returned 403; admin writes are not exposed.
Default branch and protection are manual actions in GITHUB_ADMIN_CHECKLIST. Existing
dev AGENTS/CLAUDE redirects were verified. They do not block authorized task development.