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
checks all 26 required screen names, rejects two required states that render identical
pixels (the old Home hero/rows captures were hash-identical), and writes dimensions,
SHA-256 hashes, device fingerprint and source commit to `manifest.json`. Failing tests are
printed with their stack into the CI log, and small JPEG previews of every capture are printed
as `NETFLIX_PREVIEW <name> <base64>` lines, so the evidence can be read even where artifact
storage is unreachable. Nothing here marks visual review or TCL performance as passed.

## Screen review matrix (26 surfaces × 1080p/4K × EN/AR)

Each capture is produced by the test that drives that exact state; a PNG existing is not a pass.
`Automated` is the instrumentation result for the state; `Visual` is the human review (rubric in
the parity audit §27) and stays PENDING until the maintainer reviews the images.

| # | Capture | State it proves (test) | Automated | Visual |
|---|---|---|---|---|
| 1 | 01-home-hero | Focused title decision surface, Play focused (homeHeroShowsMetadata…) | see run | PENDING |
| 2 | 02-home-rows | Focus moved from hero into a row card; distinct from 01 (homeRowsKeepContentReachable…) | see run | PENDING |
| 3 | 03-focused-card | 16:9 card focus: scale + elevation + thin outline, no reflow (focusedLandscapeCard…) | see run | PENDING |
| 4 | 04-expanded-card | Expanded card with Play/My List/Info (expandedPreviewExposes…; 04b in Home) | see run | PENDING |
| 5 | 05-movie-details | Movie details, no invented quality/match badges (movieDetailsRetain…) | see run | PENDING |
| 6 | 06-series-details | Series details, season/next episode context (seriesDetailsShow…) | see run | PENDING |
| 7 | 07-episodes | Season tabs, landscape thumbnails, progress/watched (episodesShowSeason…) | see run | PENDING |
| 8 | 08-search | Remote keyboard, recent searches, result grid (searchKeyboardUpdates…; 08b deep grid; 08c async restore) | see run | PENDING |
| 9 | 09-top-nav-home | Top navigation, Home selected and focused (topNavigationReaches…) | see run | PENDING |
| 10 | 10-top-nav-search | One D-pad step Home → Search (topNavigationReaches…) | see run | PENDING |
| 11 | 11-top-nav-my-netflix | My Netflix tab focused; Settings trailing (topNavigationReaches…) | see run | PENDING |
| 12 | 12-my-netflix-hub | Hub with Continue Watching / My List / Recently Watched (myNetflixHubAggregates…) | see run | PENDING |
| 13 | 13-profiles | Large profile cards, Add Profile de-emphasized (profilesOfferLarge…) | see run | PENDING |
| 14 | 14-player-controls | Title/episode, scrubber, elapsed/duration, play/next/audio-subtitles (playerChrome…) | see run | PENDING |
| 15 | 15-resume-actions | Resume and Start from beginning as separate actions (detailResumeAndBeginning…) | see run | PENDING |
| 16 | 16-confirmation-dialog | Netflix dark dialog, Cancel focused, Back dismisses (aConfirmationDialog…) | see run | PENDING |
| 17 | 17-empty-state | Empty My Netflix with a real Search action (emptyMyNetflixOffers…) | see run | PENDING |
| 18 | 18-network-error | Error with focused Retry, no raw URL (errorStateOffers…) | see run | PENDING |
| 19 | 19-playback-loading | Backdrop, title, real progress only (playbackLoadingUses…) | see run | PENDING |
| 20 | 20-contextual-callout | Callout from real progress only; no rank/award/match text (contextualCallout…) | see run | PENDING |
| 21 | 21-low-memory-fallback | LOW_RAM policy: static artwork, no video, no motion (lowMemoryTier…) | see run | PENDING |
| 22 | 22-missing-logo-fallback | Failed logo → title text; missing art → dark card (missingLogoAndArtwork…) | see run | PENDING |
| 23 | 23-very-long-title | Long title ellipsizes, actions stay on screen (veryLongTitles…) | see run | PENDING |
| 24 | 24-arabic-mixed-bidi | Arabic title + Latin tokens + isolated S1 E2 (arabicTitleWithLatin…) | see run | PENDING |
| 25 | 25-search-error-keyboard | Search error while the keyboard keeps focus (savedSearchFocusWaits…) | see run | PENDING |
| 26 | 26-details-return-focus | Saved Home focus restores the exact row/card (returningFromDetails…) | see run | PENDING |

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

### Audited capture
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

### Run 37088795947 (Superfork Netflix Visual #10, head 79fa95c)
- Build: PASS. 1080p EN: 26 tests, 4 failures; 26 screenshots, no identical required states.
- Now passing (were failing in #3/#7): playerChrome…, focusedLandscapeCard…, homeDwellPreview…; Home hero and rows captures now differ.
- Still failing: deeplyScrolledSearch… and savedSearchFocus… (programmatic restoration into the lazy result grid never takes focus),
  returningFromDetailsRestoresTheExactRowAndCard (saved Home focus not re-applied in the harness), and
  myNetflixHub… (test looked up a tag in the merged tree; fixed in the next head).
- Player capture is now a real controls overlay (title, S1 E2 · episode, 30:00 / 1:52:00, scrubber, controls).
- Visual review: PENDING (maintainer). TCL C6K: MANUAL-PENDING.
