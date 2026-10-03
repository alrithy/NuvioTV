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
checks all eleven requested screen names and writes dimensions, SHA-256 hashes,
device fingerprint and source commit to `manifest.json`. It does not mark visual
review or TCL performance as passed. Fixture PNGs are review evidence; adopting
them as approved goldens requires visual review of the exact implementation.

## Screen review record

Every row must be reviewed at both resolutions, with English LTR and Arabic RTL.
Review proportions, alignment, row density, typography hierarchy, focus size,
gradients, safe margins and whitespace. Record execution evidence below after
captures are available; a pending row is not a pass.

| Screen | Required review | Execution / visual review |
|---|---|---|
| Home hero | Full width cinematic backdrop, left/start gradient, logo fallback, metadata hierarchy, light focused Play | PENDING |
| Home rows | 16:9 artwork, consistent gap and density, Continue Watching progress, first/last item reachability | PENDING |
| Focused card | Scale does not move the row, selected card draws above neighbors, rapid repeated D-pad movement | PENDING |
| Expanded card | Delayed preview, Play/List/Info reachable, focus returns on dismissal, lightweight tier fallback | PENDING |
| Movie details | Full background treatment, title/logo and metadata, Resume/Beginning semantics, list/trailer actions | PENDING |
| Series details | Season count, next episode, same metadata hierarchy, no invented quality/match badges | PENDING |
| Episode list | Season picker, landscape thumbnails, number/name/duration, synopsis, progress and watched state | PENDING |
| Search | Remote keyboard, provider results, organized dense grid, recent queries, immediate query updates | PENDING |
| Top navigation | Current-generation top menu, Home/Search/Shows/Movies/My Netflix/Profile mapping, Back-to-top-menu, focus restore, RTL; old left rail must not remain in Netflix theme | PENDING |
| Profile selector | Centered large avatar cards, visible focus, profile names, stored theme loaded on selection | PENDING |
| Player controls | Existing production title/buttons/scrub/dialog chrome, focus/seek/Next Episode, HDR/AFR untouched | PENDING |

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
