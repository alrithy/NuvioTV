# Project status

<!-- canonical-state:start -->
- Active gate: G6 — Subtitle Intelligence
- Active branch: `feat/subtitle-intelligence`
- Status: IN_PROGRESS
- Task: `tasks/G6_SUBTITLE_INTELLIGENCE.md`
- Accepted official baseline: `56aaba20b7d01746d616a2c8517adcf442950b90`
- Governance: READY
- Owner of these fields: `integration/state.yaml`; regenerate with `state_view.py`.
<!-- canonical-state:end -->

This view's header is generated from state; use `state_view.py` after state changes.

## Work boundary
G0 DONE (PR #7). G1 Unified Diagnostics & Add-on Health DONE (PRs #10, #11, #12; 289 deferred to
G8). G2 Adaptive Resource Manager DONE (PRs #14, #15, #16; 276 deferred, 279 → G7, 280 → G3). G3 Playback Strategy Framework DONE (PR #18). G4 REMUX / Network Performance is code-complete with all slices merged; its TCL C6K checks/A-B are VALIDATION-PENDING and intentionally batched to G14. G5 Audio / DV / HDR / AFR CODE-COMPLETE (PRs #31–#36; device checks VALIDATION-PENDING, batched to G14). G6 Subtitle Intelligence IN_PROGRESS (pre-G6 sync PR #40 to official 56aaba2 with official AutoSync, D050; audit PR #39; G6a stream-subtitle reference + Arabic AutoSync strings (PR #41); G6b custom subtitle fonts + Arabic cinema preset; SUBTITLE_INTELLIGENCE AUTO, D051). Legacy PR #1/#2 stay reference-only.

## CI and baseline
Full-suite evidence and remaining debt: BASELINE_TEST_DEBT and integration/evidence.
A no-new-regressions PASS is not an all-tests-pass claim. Reviewed inventory: clean official
`56aaba2` replay (D050), 1,796 tests, 18 failed (15 registered + official's 3 stale RAM-tier tests
that G2b corrected on integration), 1 skipped. Registry: 15 reproduced + 1 intermittent.

## Upstream
Accepted official baseline: `56aaba20b7d01746d616a2c8517adcf442950b90` (UPSTREAM_SYNC_LOG
2026-09-30, D050; includes official Subtitle AutoSync #3703). Future movement uses dedicated
reviewed sync PRs; never silently update pins.

## Administration
GitHub connector branch-protection read returned 403; admin writes are not exposed.
Default branch and protection are manual actions in GITHUB_ADMIN_CHECKLIST. Existing
dev AGENTS/CLAUDE redirects were verified. They do not block authorized task development.