# Project status

<!-- canonical-state:start -->
- Active gate: G14 — Hardening Release Upstream
- Active branch: `chore/release-hardening`
- Status: BLOCKED
- Task: `tasks/G14_HARDENING_RELEASE.md`
- Accepted official baseline: `aeb6ee8591c55424256fdd1f35f426618297378d`
- Governance: READY
- Owner of these fields: `integration/state.yaml`; regenerate with `state_view.py`.
<!-- canonical-state:end -->

This view's header is generated from state; use `state_view.py` after state changes.

## Work boundary
G0 DONE (PR #7). G1 Unified Diagnostics & Add-on Health DONE (PRs #10, #11, #12; 289 deferred to
G8). G2 Adaptive Resource Manager DONE (PRs #14, #15, #16; 276 deferred, 279 → G7, 280 → G3). G3 Playback Strategy Framework DONE (PR #18). G4 REMUX / Network Performance is code-complete with all slices merged; its TCL C6K checks/A-B are VALIDATION-PENDING and intentionally batched to G14. G5 Audio / DV / HDR / AFR CODE-COMPLETE (PRs #31–#36; device checks VALIDATION-PENDING, batched to G14). G6 Subtitle Intelligence CODE-COMPLETE (PRs #39, #41, #42 after upstream sync #40 to official 56aaba2, D050; SUBTITLE_INTELLIGENCE AUTO, D051; device checks VALIDATION-PENDING, batched to G14). G7 Seek Intelligence CODE-COMPLETE (PRs #44–#46; D052; SEEK_INTELLIGENCE AUTO; 279 closed; device checks VALIDATION-PENDING, batched to G14). G8 Stream Intelligence CODE-COMPLETE (PRs #48–#51; D053; STREAM_INTELLIGENCE AUTO; 289 closed; device checks VALIDATION-PENDING, batched to G14). G9 Skip / Recommendations / Discovery CODE-COMPLETE (audit #53, slices #54–#59, closeout corrections #61; final squash 7552ef8; 51 IDs terminal, 40 implemented + 11 verified_official; D054; DISCOVERY_SKIP_RECOMMENDATIONS AUTO; HV-G9-1..HV-G9-7 MANUAL-PENDING for G14). G10 Live TV CODE-COMPLETE (audit #62, slices #63–#69, final squash 5aba471; 29 IDs implemented; D055; LIVE_TV AUTO, menu off until enabled per profile; HV-G10-1..HV-G10-8 MANUAL-PENDING for G14). G11 Watch Party CODE-COMPLETE (audit #71, slices #72–#73, review corrections #74, final squash 65e1c57; 13 IDs implemented; D056, D057; WATCH_PARTY AUTO; HV-G11-1..HV-G11-4 MANUAL-PENDING for G14). G12 UI Styles CODE-COMPLETE (audit #76, upstream sync #77 to official dev 5c1d9b0 (D059), slices #78–#81, final squash 4d640fc; 19 IDs terminal, 10 implemented + 9 verified_official; D058, D060–D062; UI_STYLES AUTO, official sidebar default; HV-G12-1..HV-G12-5 MANUAL-PENDING for G14). G13 Experimental AI & MAT CODE-COMPLETE (audit #83, slices #84–#86, final squash 746c82a; 9 IDs experimental (45, 250, 255–261; OFF unless opted in under Settings → Advanced → Experimental), 4 deferred (251–254, no engine in any pinned source); D063; HV-G13-1..HV-G13-3 MANUAL-PENDING for G14). G14 Hardening / Distribution / Upstream BLOCKED on chore/release-hardening (device findings merged as #96, D067: Live TV crash fixed at its root (init before state), Auto Sync choice ranked by SubtitleCandidateRanking, Best-quality list and autoplay on one ranking with device compatibility; HV-G10-1 FAIL, re-tests HV-G14-4..6) (Claude, sequential; audit #88 merged, upstream sync #89 to official dev aeb6ee8 merged (D065), G14a #90 QR server sessions and host-only log URLs merged, G14b #91 fork updater, digest-checked install and tested release merged, G14c #92 read-only upstream watch merged, accounting #93 merged: 318 of 320 rows terminal, 3 and 320 blocked on the hardware campaign and maintainer release actions; G14 BLOCKED; device test build #94 merged (D066, com.nuvio.tv.debug with real login, fixed test key); D064: 293–298, 300–302, 307 verified_official; gaps closed in G14a local-server and log hardening, G14b fork distribution, G14c upstream watch; upstream sync to official dev aeb6ee8 first (D065); then the 320-row accounting and the hardware campaign the maintainer runs). Legacy PR #1/#2 stay reference-only.

## CI and baseline
Latest green feature evidence: G14 device-findings PR #96 exact head
`091db57119c382712f6aeca6abf8c50f4c156a73`, run https://github.com/alrithy/NuvioTV/actions/runs/36963783109:
2391 tests, 15 known failures, 0 new, 1 skipped; Device Smoke 4/4 (run 36963783132); Test Build run 36963783129;
squash-merged as `bd5d61a`. G14 BLOCKED on the hardware campaign.
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