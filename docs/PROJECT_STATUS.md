# Project status

<!-- canonical-state:start -->
- Active gate: G13 — Experimental AI & MAT
- Active branch: `experimental/media`
- Status: IN_PROGRESS
- Task: `tasks/G13_EXPERIMENTAL.md`
- Accepted official baseline: `5c1d9b0e2669199114a12ade38027da132303eb3`
- Governance: READY
- Owner of these fields: `integration/state.yaml`; regenerate with `state_view.py`.
<!-- canonical-state:end -->

This view's header is generated from state; use `state_view.py` after state changes.

## Work boundary
G0 DONE (PR #7). G1 Unified Diagnostics & Add-on Health DONE (PRs #10, #11, #12; 289 deferred to
G8). G2 Adaptive Resource Manager DONE (PRs #14, #15, #16; 276 deferred, 279 → G7, 280 → G3). G3 Playback Strategy Framework DONE (PR #18). G4 REMUX / Network Performance is code-complete with all slices merged; its TCL C6K checks/A-B are VALIDATION-PENDING and intentionally batched to G14. G5 Audio / DV / HDR / AFR CODE-COMPLETE (PRs #31–#36; device checks VALIDATION-PENDING, batched to G14). G6 Subtitle Intelligence CODE-COMPLETE (PRs #39, #41, #42 after upstream sync #40 to official 56aaba2, D050; SUBTITLE_INTELLIGENCE AUTO, D051; device checks VALIDATION-PENDING, batched to G14). G7 Seek Intelligence CODE-COMPLETE (PRs #44–#46; D052; SEEK_INTELLIGENCE AUTO; 279 closed; device checks VALIDATION-PENDING, batched to G14). G8 Stream Intelligence CODE-COMPLETE (PRs #48–#51; D053; STREAM_INTELLIGENCE AUTO; 289 closed; device checks VALIDATION-PENDING, batched to G14). G9 Skip / Recommendations / Discovery CODE-COMPLETE (audit #53, slices #54–#59, closeout corrections #61; final squash 7552ef8; 51 IDs terminal, 40 implemented + 11 verified_official; D054; DISCOVERY_SKIP_RECOMMENDATIONS AUTO; HV-G9-1..HV-G9-7 MANUAL-PENDING for G14). G10 Live TV CODE-COMPLETE (audit #62, slices #63–#69, final squash 5aba471; 29 IDs implemented; D055; LIVE_TV AUTO, menu off until enabled per profile; HV-G10-1..HV-G10-8 MANUAL-PENDING for G14). G11 Watch Party CODE-COMPLETE (audit #71, slices #72–#73, review corrections #74, final squash 65e1c57; 13 IDs implemented; D056, D057; WATCH_PARTY AUTO; HV-G11-1..HV-G11-4 MANUAL-PENDING for G14). G12 UI Styles CODE-COMPLETE (audit #76, upstream sync #77 to official dev 5c1d9b0 (D059), slices #78–#81, final squash 4d640fc; 19 IDs terminal, 10 implemented + 9 verified_official; D058, D060–D062; UI_STYLES AUTO, official sidebar default; HV-G12-1..HV-G12-5 MANUAL-PENDING for G14). G13 Experimental AI & MAT IN_PROGRESS on experimental/media (Claude, sequential; audit #83 merged, G13a #84 aimedia core + experimental opt-in, D063: AI provider platform 250, 255–261 and MAT 45 ported, AI engines 251–254 deferred until a source ships one; everything default OFF behind a Settings → Experimental opt-in), then G14. Legacy PR #1/#2 stay reference-only.

## CI and baseline
Latest green feature evidence: G13 audit PR #83 exact head
`498da52f25df308342674f8c5178128a5c9e449a`, run https://github.com/alrithy/NuvioTV/actions/runs/36877526195:
2219 tests, 15 known failures, 0 new, 1 skipped; fullDebug APK artifact 11170525995; squash-merged as
`a62ce1d`. G13a PR #84 requires exact-head CI.
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