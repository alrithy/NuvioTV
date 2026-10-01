# G12 development closeout — CODE-COMPLETE

2026-10-01. This records merged work. The G12 source audit (`docs/audits/G12_UI_STYLES_AUDIT.md`) and
D058–D062 remain authoritative. Scope: 19 IDs, 188–205 and 292. Traceability has 10 implemented and 9
verified_official rows, each with evidence, and no planned, in_progress, blocked or deferred rows.

| Slice | PR | IDs | Merge on superfork/integration |
|---|---|---|---|
| Source audit | #76 | 188–205, 292 (plan; 188–192, 195, 201–203 verified_official) | bd1f13e2bfe38e7010c14be3870d0485d14807bf |
| Upstream sync to official `dev` `5c1d9b0` (D059) | #77 | — (merge commit) | c45b78ed61983cf0b326b14bbcbf4a449100fe1c |
| G12a top menu: bar / pill, clock, profile | #78 | 196, 198, 204, 205 | 14a3353b0811b59e0e23292ff9ecd354d616ac74 |
| G12b Glass chrome, blur or flat, Lightweight effects | #79 | 193, 197, 200 | c98dc01d7fa2919db1d17eb878b8a6ae58021caf |
| G12c Cinematic Glass, AGSL liquid glass | #80 | 194, 199 | 783973f24f010c7dc2d1c13a8a13142de658796b |
| G12d optional screensaver | #81 | 292 | 4d640fccdbabe478680690568e9adc2d910ff728 |

Exact-head Full Debug CI. Each slice was squash-merged with its expected head (the sync with a merge
commit, so official stays a parent), and integration was fetched afterwards.

| PR | Head | Run | Tests | Registered failures | New | Skipped | APK artifact |
|---|---|---|---|---|---|---|---|
| #76 | 86a521f44dd60e471d106e595690573ff384a13a | 36846143142 | 2205 | 15 | 0 | 1 | 11153369840 |
| #77 | dbdca31b9d34581e9ed7871b3129a34308ea05fd | 36849295963 | 2205 | 15 | 0 | 1 | 11155351900 |
| #78 | 362dd4582f775026ea167a2988aad0ee32c8f93a | 36855538291 | 2209 | 15 | 0 | 1 | 11159113616 |
| #79 | ec93e48f45b1ed6e52fbd444977458294ef51d36 | 36858515752 | 2212 | 15 | 0 | 1 | 11160717187 |
| #80 | c81156fd2ffb1ce7e7247abdbb09d44339af0c9b | 36863863735 | 2213 | 15 | 0 | 1 | 11162099759 |
| #81 | b4c92e4cbab119f6b75cf8b0ad65379ab7d1e94e | 36865550286 | 2219 | 15 | 0 | 1 | 11163438418 |

The sync's clean baseline replays (run 36848159277) showed official `5c1d9b0` at 1,796 tests,
identical to the `56aaba2` inventory, and the integration merge at 2,205 tests, 15 known, 0 new; the
baseline was re-anchored with debt unchanged (D059). Governance CI, PR Policy, Baseline Change and
State Handoff succeeded on every head. These are the actual CI values, not a claim that every test
passes.

## Review
The Codex review bot had reached its usage limit and left no findings on #76–#81. No review
threads are open. A later review that lands after merge is handled as the G11 corrections were.

## What G12 delivers (D058, D060, D061, D062)
- **Owner:** one `fork/uistyle` owner. Official's Classic / Grid / Modern layouts, both sidebars, the
  hero carousel, hero trailer and full-screen backdrop stay as shipped (verified_official 188–192,
  195, 201–203).
- **Navigation style**, per profile in `fork_ui_style` (official layout preferences never written):
  - Sidebar: official's, and the default.
  - Top bar and Pill: one `TopChromeScaffold` above the content, with official navigation; Up from
    the top row reaches it; Back focuses it, then exits; long-press Back jumps to it.
  - Glass: frosted chrome over official's Modern home and its full-bleed hero, hiding itself on Home
    and revealed by Up from the first row (one hook in `ModernHomeRowsList`).
  - Cinematic Glass: Glass with the full-screen hero backdrop shown without writing the official
    setting (one hook in `ModernHomeContent`); the hero trailer keeps following official's settings.
  - Glass styles fall back to Pill with Classic or Grid (D060: no new `HomeLayout` values).
- **Clock and profile:** a minute-tick clock in the device 12 / 24-hour format, and a profile button
  that opens profile selection like the sidebar profile row.
- **Glass effect by capability:** AGSL liquid glass (Reshaped) on Android 13+ in the
  AdaptiveResources STANDARD tier; Haze blur on other Android 12+ non-LOW_RAM devices; an opaque tint
  otherwise; the per-profile Lightweight effects switch forces the tint. The chrome is never shown
  over the player.
- **Screensaver:** off by default, per profile; dims after 1–30 min without input, never while
  playing or buffering, waits while a dialog has focus; the waking press does nothing else; hero
  trailers do not start under it.
- **Flag:** UI_STYLES is AUTO. OFF shows official's sidebar, hides every new setting, and leaves the
  screensaver off and every hook inert.

## Not done, by design or as a residual risk
**Not imported, by design:**
- `HomeLayout.GLASS` / `CINEMATIC_GLASS` and the `usesModernPipeline` edits across official layout
  code (D060);
- forcing official's hero trailer on in Cinematic Glass (D061);
- NuvioGlass fork identity, Calendar tab and per-card glass; Reshaped's own bar, app-wide flag and
  second RAM probe; Cxsmo's on-by-default screensaver;
- legacy PRs #1 / #2 code (reference only).

See IMPORT_LEDGER G12a–G12d.

**Residual risks:**
- D-pad focus between the top chrome and every root screen, and RTL, on real remotes.
- Blur and AGSL cost on 2 GB and Android 13+ boxes; video surfaces under the glass.
- Remotes that send only key-up or long-press codes, and MediaSession resumes, for the screensaver.

## Checks and device validation
Local closeout checks: project validator, state views, governance regression suite and git diff
--check. Android tests and build evidence come from GitHub CI. During development a standalone JVM
harness exercised the `fork` packages (344 tests at #81, Android seams stubbed). No manual or device
testing is claimed.

HV-G12-1, HV-G12-2, HV-G12-3, HV-G12-4 and HV-G12-5 all remain **MANUAL-PENDING**. They are carried
into validation_pending_gates, the hardware checklist (§4i) and MANUAL_TEST_LOG for G14 under D047.
G12 is CODE-COMPLETE with hardware validation pending; this is not a Stable-release claim.

## Next gate
This governance transition activates G13 Experimental AI & MAT on `experimental/media` (task
`tasks/G13_EXPERIMENTAL.md`, IDs 45 and 250–261), owner Claude (sequential; no lease). Everything in
G13 defaults OFF. The gate after it is G14 Hardening / Release / Upstream. G13 begins only after this
PR merges, with an official-first audit against official and the pinned Fornace AI and ysosrs MAT
sources.
