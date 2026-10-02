# Netflix theme verification

The theme reuses Nuvio's content, navigation and playback owners. No Netflix logos,
artwork, font files, internal code or fixed Netflix catalog data are included.
The target is the observable TV presentation and interaction described in the
request. There is no numeric fidelity claim: a 99.99% comparison requires a
specified reference build, matching content, viewport and frame capture.

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
| Navigation | Minimal collapsed rail, expanded labels, first content entry, Back/reveal/focus restore, RTL | PENDING |
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

Local environment: Java 21, Android SDK setup in progress, no `/dev/kvm`; software
Android TV emulation is being assessed. No screenshots or hardware passes are
claimed before execution. Exact commands, CI links, APK artifact and remaining
visual differences will be added after the implementation and tests are stable.
