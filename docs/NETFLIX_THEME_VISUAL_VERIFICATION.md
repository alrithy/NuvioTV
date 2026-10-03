# Netflix theme verification

The theme reuses Nuvio's content, navigation and playback owners. No Netflix logos,
artwork, font files, internal code or fixed Netflix catalog data are included.
The target is the observable CURRENT Netflix TV generation, constrained by Nuvio's own data and owners.
The mandatory current-reference audit is docs/NETFLIX_TV_2026_PARITY_AUDIT.md. That audit uses the
current Netflix Help Center, Tudum and About Netflix documentation and supersedes any assumption that
the older left-sidebar Netflix UI is the target. There is no numeric 99.99% fidelity claim: an exact
fidelity measurement would require a specified Netflix build, matching content, viewport and frame capture.

## Reproducible screenshot capture

`NetflixThemeTvTest` renders actual production Compose components using offline
fixtures. This removes live add-on/TMDB timing from screenshots and exercises
D-pad activation and focus. These are component/section screenshots; they are
not evidence of real backend, stream selection or device playback performance.
The existing Compose instrumentation dependencies are reused; no golden framework
or new rendering dependency is needed.

Build and install the app and instrumentation APK with the normal full flavor,
the configured temporary signing key and `-Pdebuggable=true`. Then run:

```bash
python3 scripts/superfork/capture_netflix_theme.py \
  --serial emulator-5554 --resolution 1080p --output build/netflix-visual/1080p
python3 scripts/superfork/capture_netflix_theme.py \
  --serial emulator-5554 --resolution 4k --output build/netflix-visual/4k
```

The script sets 1920×1080/320 dpi or 3840×2160/640 dpi, yielding the same 960×540
dp TV canvas. It selects a device explicitly, runs instrumentation, collects PNGs,
checks all 42 required screen names, rejects two required states that render identical
pixels (the old Home hero/rows captures were hash-identical), and writes dimensions,
SHA-256 hashes, device fingerprint and source commit to `manifest.json`. Failing tests are
printed with their stack into the CI log, and small JPEG previews of every capture are printed
as `NETFLIX_PREVIEW <name> <base64>` lines, so the evidence can be read even where artifact
storage is unreachable. Nothing here marks visual review or TCL performance as passed.

## Screen review matrix (42 surfaces × 1080p/4K × EN/AR)

Each capture is produced by the test that drives that exact state; a PNG existing is not a pass.
`32-top10-row` is intentionally absent (no factual ranking source). Visual review is the maintainer's
and stays PENDING until the images are reviewed. Element-level measurements live in
docs/NETFLIX_REFERENCE_FIDELITY.md.

| Capture | State | Visual |
|---|---|---|
| 01-home-hero | Hero card, Play focused | PENDING |
| 02-home-rows | Focus moved from hero into the first row | PENDING |
| 03-focused-card | Focused poster expanded inline (alias of 04/30/34) | PENDING |
| 04-expanded-card | Same scene: the inline landscape card is the expanded card | PENDING |
| 05-movie-details | Movie details (no reference; Round 2 presentation) | PENDING |
| 06-series-details | Series details (no reference) | PENDING |
| 07-episodes | Episodes (no reference) | PENDING |
| 08-search | Search: keyboard + portrait results | PENDING |
| 09-top-nav-home | Top nav, Home selected pill + focus | PENDING |
| 10-top-nav-search | Home → Search in one step | PENDING |
| 11-top-nav-my-netflix | My Netflix tab focused; no Settings tab | PENDING |
| 12-my-netflix-hub | My Netflix hub from real profile data | PENDING |
| 13-profiles | Profiles (no reference) | PENDING |
| 14-player-controls | Player chrome (no reference) | PENDING |
| 15-resume-actions | Resume / Start over separate | PENDING |
| 16-confirmation-dialog | Dialog, Back dismisses | PENDING |
| 17-empty-state | Empty My Netflix with real Search action | PENDING |
| 18-network-error | Error with Retry | PENDING |
| 19-playback-loading | Playback loading (no reference) | PENDING |
| 20-contextual-callout | Factual callout only | PENDING |
| 21-low-memory-fallback | LOW_RAM: static art, no motion | PENDING |
| 22-missing-logo-fallback | Missing logo → text title | PENDING |
| 23-very-long-title | Long title ellipsizes | PENDING |
| 24-arabic-mixed-bidi | Arabic + isolated Latin tokens | PENDING |
| 25-search-error-keyboard | Search error keeps keyboard focus | PENDING |
| 26-details-return-focus | Home restores row and card | PENDING |
| 27-home-hero-full-reference-state | Hero top state (alias of 01) | PENDING |
| 28-home-category-shortcuts | Category tiles focused | PENDING |
| 29-browse-row-portrait-idle | Idle portrait row | PENDING |
| 30-browse-row-focused-landscape | Alias of 03 | PENDING |
| 31-browse-row-focus-moved-next-item | Previous collapsed, next expanded | PENDING |
| 33-search-portrait-results | Alias of 08 | PENDING |
| 34-arabic-browse-expanded | Alias of 03 | PENDING |
| 35-arabic-search-keyboard-right | Alias of 08 | PENDING |
| 36-home-full-production-top | Production top bar + production Home: nav, hero, categories, first row | PENDING |
| 37-home-full-production-scrolled | Production scaffold browse state: expanded card, facts, neighbours, next row | PENDING |
| 38-comfort-zone-middle | Middle poster expands in place, no scroll | PENDING |
| 39-comfort-zone-near-edge | Near the edge: scrolls only the overflow | PENDING |
| 40-comfort-zone-rtl-last | Last item reachable and fully visible (RTL in the AR run) | PENDING |
| 41-category-strip-final | Category tile geometry | PENDING |
| 42-search-full-production | Search under the production top bar | PENDING |
| 43-search-full-production-results | Focus moved into results | PENDING |

## Automated and hardware boundaries

Required automated checks remain the project validator, governance regression
suite, `python3 scripts/superfork/run_full_unit_suite.py`, `:app:assembleFullDebug`,
the instrumentation theme suite, full Superfork CI and signed Superfork Test Build.
The baseline registry permits exact pre-existing unit failures; report the full
inventory and new-failure count rather than saying every unit assertion passed.

TCL C6K checks remain separate: 4K/1080p D-pad feel, sustained frame time, image
memory, low-RAM fallback, HDR/DV/Atmos output, AFR, subtitle and stream selection
regressions. A software emulator cannot certify 60 fps or television output modes.
Do not merge this theme while the requested visual verification remains incomplete.

## Current evidence

Final fidelity round: see the latest "Superfork Netflix Visual" run on the PR head and the run IDs in
the PR body. Earlier rounds are recorded below as history; their matrix descriptions (16:9 cards, no
reflow, floating preview, Settings trailing, 26 surfaces) are superseded.

## HISTORY — evidence from earlier rounds

#### Audited capture
- Source commit: `6a8389a1e0fdc8fca140007fba68d17357ab597f`
- Workflow: Superfork Netflix Visual #3
- Run: https://github.com/alrithy/NuvioTV/actions/runs/37076381309
- Result: FAILURE
- Build TV visual fixtures: PASS
- All four matrix jobs produced artifacts/screenshots, but instrumentation failed.
- 17 screenshots were collected per locale with no required screen missing and no unexpected 1080p dimensions.
- Visual review remains PENDING.
- Netflix reference comparison is now documented in `docs/NETFLIX_TV_2026_PARITY_AUDIT.md`.
- Hardware performance remains MANUAL-PENDING.

Artifacts from run 37076381309:
- 1080p EN: `netflix-visual-1080p-en-37076381309`, artifact 11256499843
- 1080p AR: `netflix-visual-1080p-ar-37076381309`, artifact 11257610068
- 4K EN: `netflix-visual-4k-en-37076381309`, artifact 11256794577
- 4K AR: `netflix-visual-4k-ar-37076381309`, artifact 11257222209
- fixture APKs: `netflix-visual-apks-37076381309-1`, artifact 11257027139

The 1080p manifests report:
- EN: 19 instrumentation tests, 5 failures
- AR: 19 instrumentation tests, 5 failures
- same five failing interactions in both locales:
  1. `playerChromeUsesTheExistingSeekCallbacksAndLogicalEpisodeTitle`
  2. `deeplyScrolledSearchReturnsToAComposedResultAfterBack`
  3. `savedSearchFocusWaitsForResultsAndKeepsTheKeyboardAvailableDuringAnError`
  4. `homeDwellPreviewRestoresItsAnchorAndAllowsRepeatedRemoteNavigation`
  5. `focusedLandscapeCardMovesWithoutChangingItsLayoutBounds`

Do not skip or quarantine these failures to make the visual job green. Resolve the implementation or
prove and fix a deterministic test defect. The player capture from this run is not acceptable visual
evidence because the player test failed and the English player image was effectively blank/white.

Additional audit observations:
- Home hero and Home rows captures are hash-identical in both EN and AR; the capture must be changed so
  the intended row state is visibly demonstrated.
- The captured left collapsed/expanded navigation is now a known architectural mismatch with current
  Netflix TV and must be replaced for AppTheme.NETFLIX with the current top-navigation presentation.
- Movie/series details and Episodes are strong foundations and should be refined rather than rewritten.
- Search is structurally useful, but its focus restoration/error-state tests must be fixed.
- Arabic RTL is a real mirrored implementation, but mixed LTR tokens and season/episode metadata require
  explicit bidi verification.

No TCL C6K performance, remote-feel or TV output certification is claimed from emulator evidence.

#### Run 37088795947 (Superfork Netflix Visual #10, head 79fa95c)
- Build: PASS. 1080p EN: 26 tests, 4 failures; 26 screenshots, no identical required states.
- Now passing (were failing in #3/#7): playerChrome…, focusedLandscapeCard…, homeDwellPreview…; Home hero and rows captures now differ.
- Still failing: deeplyScrolledSearch… and savedSearchFocus… (programmatic restoration into the lazy result grid never takes focus),
  returningFromDetailsRestoresTheExactRowAndCard (saved Home focus not re-applied in the harness), and
  myNetflixHub… (test looked up a tag in the merged tree; fixed in the next head).
- Player capture is now a real controls overlay (title, S1 E2 · episode, 30:00 / 1:52:00, scrubber, controls).
- Visual review: PENDING (maintainer). TCL C6K: MANUAL-PENDING.

#### Run 37093075199 (head f75d31a) — automated matrix GREEN
- Superfork Netflix Visual: Build PASS; 1080p EN, 1080p AR, 4K EN, 4K AR all PASS (26/26 tests each, 0 missing
  required screens, no identical required states). The PR-triggered run 37093077773 on the same head is also 4/4 PASS.
- Same head: Superfork Full Debug CI PASS, Device Smoke PASS, Governance/PR Policy/State Handoff/Baseline Change PASS.
- Artifacts: netflix-visual-1080p-en-37093075199 (11263268766), netflix-visual-1080p-ar-37093075199 (11262633396),
  netflix-visual-4k-en-37093075199 (11263673296), netflix-visual-4k-ar-37093075199 (11262862755),
  APKs netflix-visual-apks-37093075199-1 (11263333269).
- Final root causes of the remaining failures: Search restoration did move focus (logged moved=true); the tests asserted
  focus on GridContentCard's tagged wrapper instead of its Card. Return-to-card fixture omitted the vertical scroll index
  production saves. Hub test asserted an off-screen lazy section and used a header action after it scrolled away. The
  emulator occasionally returned an all-white surface; capture now retries and refuses to save a uniform frame.
- Not yet produced: signed Superfork Test Build (runs on a PR titled `[test-build]`, deferred until the maintainer
  approves the screenshots). Visual review: PENDING. TCL C6K: MANUAL-PENDING.

#### Visual Round 2 — head ea68c79 (automated matrix GREEN; visual review PENDING)
- Netflix Visual push run 37095465484 and PR run 37095461937: Build PASS; 1080p EN/AR and 4K EN/AR all PASS,
  26/26 instrumentation each, 0 missing required screens, no identical or blank required states.
- Same head: Superfork Full Debug CI PASS (37095465511), Device Smoke PASS (37095465455), Governance/PR Policy/
  State Handoff/Baseline Change PASS.
- Round 2 artifacts: 1080p EN 11264422090, 1080p AR 11264471894, 4K EN 11264795594, 4K AR 11263882810,
  APKs 11264127015.
- Changes: Home rows band 45% + larger type/cards; top-nav translucent focus pill and selected underline; inline
  callout; art-backed My Netflix header with quiet actions; shared Netflix empty/error panel; TV-sized dialogs;
  player/details/episodes/profiles typography and focus polish. Low-memory contract unchanged (no new animation,
  bounded header-art decode, no video).
- Visual review: PENDING (maintainer). Signed Test Build: not produced (on hold until screenshot approval).
  TCL C6K: MANUAL-PENDING.

#### Visual Round 3 — maintainer reference correction, head 23f3d85 (automated GREEN; visual review PENDING)
Reference: parity audit §0 (2026-10-03). No Netflix recording, frame or asset is committed.
- Netflix Visual: PR run 37110129569 and push run 37110126396 — Build PASS; 1080p EN, 1080p AR, 4K EN, 4K AR all PASS,
  32/32 instrumentation each, 0 missing required screens (34 names incl. aliases), no unintended identical states.
- Same head: Full Debug CI PASS (37110129528), Device Smoke PASS (37110129543), Governance / PR Policy / State Handoff /
  Baseline Change PASS.
- Artifacts (run 37110129569): 1080p EN 11269532491, 1080p AR 11269707046, 4K EN 11269273166, 4K AR 11269861952,
  APKs 11269567015.
- New surfaces: 27 hero reference state, 28 category shortcuts, 29 portrait idle row, 30 focused landscape (inline),
  31 focus moved to next item, 33 Search portrait results, 34 Arabic expanded row, 35 Arabic Search keyboard right.
  32 Top 10 is intentionally absent: Nuvio has no factual ranking source.
- Category strip source: the profile's own add-on catalogs on Home (real data), opened via the existing See all owner.
  No genre list exists on Home; none is invented.
- Visual review: PENDING (maintainer). Signed Test Build: not produced. TCL C6K: MANUAL-PENDING.
