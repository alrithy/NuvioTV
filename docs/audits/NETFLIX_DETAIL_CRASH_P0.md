# P0: Detail process crash on the TCL C6K (Build 76)

Status: ROOT CAUSE FOUND AND FIXED on feat/netflix-theme. Pending confirmation on the TCL C6K.

## Symptom (maintainer, real hardware)

Open a title (Dune, Ice Road: Vengeance), move DOWN / sideways through Detail; the app process
terminates and the Google TV launcher appears.

## Reproduction

`DetailRemoteNavigationTvTest` (androidTest) renders the production Detail content
(`MetaDetailsContent`, through the test-only `MetaDetailsContentForTest`) with a movie and a series
that have cast, trailers, More like this, a collection and company rows, and walks it with real
remote key events: stepwise, and as held keys auto-repeating every 33 ms. It runs in the Netflix
Visual matrix (TV emulator, API 31, 1080p and 4K, English and Arabic).

At `6ca40dc` (Build 76 code plus the test), Netflix Visual run 37285880653, job "TV 1080p / ar":
3 of 7 tests failed, every failure the same exception (excerpt from the job log):

```
java.lang.IllegalStateException: Only Sp can convert to Px
    at androidx.compose.ui.unit.DensityWithConverter.toDp-GaN1DYA(AndroidDensity.android.kt:48)
    at com.nuvio.tv.ui.screens.detail.CastSectionKt.CastMemberItem-1yyLQnY(CastSection.kt:456)
    at com.nuvio.tv.ui.screens.detail.CastSectionKt$CastSection$...$itemsIndexed$default$6.invoke(LazyDsl.kt:577)
    ... LazyListMeasure / subcompose ...
```

Failing: `netflixMovieDetailSurvivesStepwiseRemoteWalk`, `netflixMovieDetailSurvivesHeldRemoteBursts`,
`netflixSeriesDetailSurvivesStepwiseRemoteWalk`. Passing: the same title without cast, and both
classic-presentation walks (they did not compose a cast card during the walk).

## Root cause

`bd66917ec` ("Keep room below Thmanyah text so Arabic dots and tails are not cut") moved every
Thmanyah theme style to an **em** line height (`withNuvioDescenderRoom`, 1.55 em).
`CastMemberItem` sized its label area with `nameStyle.lineHeight.toDp()`; `TextUnit.toDp()` only
accepts sp and throws `IllegalStateException` for em. The exception is thrown while the cast row is
composed during LazyColumn measurement, on the main thread, so it is uncaught and the process dies.
In Netflix mode the cast/Details section composes as soon as focus moves down or across the section
tabs, which matches the TV repro. Not a focus, BringIntoView or FocusRequester lifecycle bug: those
paths were exercised by the same walk and did not fail.

The same em/sp mistake existed without crashing in four places that used `lineHeight.value.dp`
(a 1.55 dp spacer instead of a full line): Search discover cards, Grid home cards, Catalog See All
and Folder detail.

## Fix

- `TextStyle.lineHeightDp(density)` in `ui/theme/Type.kt` resolves sp, em and unspecified line
  heights; used by `CastMemberItem` and the four spacer sites.
- Rule: never convert a theme line height with `toDp()`, `toPx()` or `value.dp`.
- `ModernHomeContent` already wraps its conversion in `runCatching` (falls back to `spacing.xl`); left
  unchanged so the approved Home geometry does not move.

## Regression coverage

- `DetailRemoteNavigationTvTest` (device, all four matrix cells).
- `LineHeightDpTest` (JVM): em, sp and unspecified line heights.
