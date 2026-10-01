# Upstream Sync Log

This file records every official Nuvio upstream movement after the Superfork project was created. Never rewrite old entries.

## 2026-09-28 — Pre-G0 baseline refresh
- Purpose: start feature implementation from the true latest official `dev` available immediately before G0.
- Previous official anchor: `c257a2365ee3386b582dc2974ec235cfe0381f33`
- New official baseline: `fd7973d91dd75d790c5f9b3d68dae652655e92c4`
- Upstream distance: 2 commits.
- Upstream merge: PR #3720 — `fix(home): keep a row's window on its focused card after a refresh`.
- Feature commit in merge: `514771f21221169ed209ca584fb850bee34d9bcf`.
- Changed production file: `app/src/main/java/com/nuvio/tv/ui/screens/home/ModernHomeContent.kt`.
- Superfork integration merge commit: `3cf04ccdcc20515acb093c28ad9b7c3943a39057`.
- Conflicts: none.
- Feature-port code present before sync: none.
- Decision: adopt the newer official state before G0 implementation so fork feature work does not start two commits behind upstream.

## 2026-09-29 — Post-G0 sync before G1
- Purpose: G1 touches settings (device assessment apply/revert, Add-on Health screen) and player diagnostics; official changed both subsystems (D036/D037 trigger).
- Previous official anchor: `fd7973d91dd75d790c5f9b3d68dae652655e92c4`
- New official baseline: `71632b9271e8bce6783e415d64f34cfa4e8b894c`
- Upstream distance: 7 first-parent merges — #3746 settings reorganization, #3749, #3745, #3693 YouTube stream playback, #3752, #3466 random episode/shuffle, `71632b9` debrid episode file selection.
- Superfork integration merge commit: `898bc83abfc8e59cf1664699980942d3bd6718f6` (PR #9). Conflicts: none.
- Feature-port code present before sync: G0 foundation only (new package, no official files touched).
- Tree vs official after merge: G0 foundation files plus the pre-existing missing final newline in ModernHomeContent.kt.
- Re-audit (UPSTREAM_SYNC step 6): changed PlayerRuntimeController{,Metadata,Observers,Streams}, PlayerViewModel, ExternalPlaybackTracker and settings screens; untouched PlayerMediaSourceFactory, ParallelRangeDataSource, StreamSpeedTester, PlayerDebugStatsOverlay, PlayerSettingsDataStore, SkipIntroRepository.
- Clean replays (run 36547502209): official `71632b9` and integration `898bc83` both 1,752+ tests, 18 known failures, 1 skipped, zero new failures, gradle exit 0; HomeEnrichment intermittent case passed.
- Inventory change: `com.nuvio.tv.core.debrid.TorboxFileSelectorTest#selects file by torbox file id first` intentionally renamed/inverted upstream in `71632b9` to `does not treat torrent index as torbox file id`; replacement coverage in the same class (plus two new selector tests). Reviewed under D040.
- Decision: adopt official before G1 coding; baseline debt registry re-anchored with unchanged 19 entries.

## 2026-09-30 — Sync before G6 (official Subtitle AutoSync)
- Purpose: official `dev` merged Subtitle AutoSync (PR #3703, from DavidVamaiotu, the Reshaped AutoSync) — exactly the G6 subsystem. UPSTREAM_SYNC "before beginning a gate that will touch an upstream subsystem that changed materially" and the feature-convergence rule (prefer official ownership) apply; D050.
- Previous official anchor: `71632b9271e8bce6783e415d64f34cfa4e8b894c`
- New official baseline: `56aaba20b7d01746d616a2c8517adcf442950b90`
- Upstream distance: 6 first-parent merges — #3740, #3758 MDBList hero ratings, #3765 completed-playback back-to-details, #3703 Subtitle AutoSync, #3747 input fields, `56aaba2` AutoSync controls-flash fix (55 commits, 90 files, +11,195 / −412).
- Superfork integration merge commit: `5c24805f82b2bb7092b9acafc00903a32fe5045c` (this PR; merged with a merge commit so official stays a parent).
- Conflicts: one, `PlayerRuntimeControllerInitialization.kt` extractor-factory call: kept the fork G5d `injectHdr10Sei` argument and official's `.let { autoSyncExtractorsFactory(it, url, headers) }` AutoSync wrapper (official wraps the fork DolbyVisionExtractorsFactory; both behaviors kept).
- Files changed by both sides (auto-merged, reviewed): `PlayerRuntimeController`, `…Lifecycle`, `…Tracks` (official adds one-line AutoSync hooks next to fork fields/calls), `…Initialization` (above), home/hero/MDBList/add-on manager/post-play and `strings.xml` (G2/G4 fork edits do not overlap official lines).
- Re-audit (UPSTREAM_SYNC step 6): changed PlayerRuntimeController{,AutomaticSubtitleSync,Initialization,Lifecycle,PlaybackEvents,Tracks} and `NuvioNavHost` (completed playback returns to details); untouched PlayerMediaSourceFactory, ParallelRangeDataSource, StreamSpeedTester, PlayerDebugStatsOverlay, PlayerSettingsDataStore, SkipIntroRepository.
- Official AutoSync defaults off (`AutoSyncPreferences` `KEY_ENABLED` false), so the sync changes no default playback timing.
- Test evidence: exact-head Full Debug CI of this PR plus the `[baseline-audit]` clean replays of official `56aaba2` and the integration merge.
- Decision: adopt official before G6 coding; G6 reuses official AutoSync as its single engine (D049 revised, D050).

## 2026-10-01 — Sync before G12 (official home layouts)
- Purpose: official `dev` changed the home layout files G12 touches (global landscape poster mode in `ClassicHomeContent`, `GridHomeContent`, `ModernHomeRows`, `ContentCard`, `GridContentCard`, `LayoutHomeSettings`, `MainActivity`). UPSTREAM_SYNC "before beginning a gate that will touch an upstream subsystem that changed materially" applies; D059.
- Previous official anchor: `56aaba20b7d01746d616a2c8517adcf442950b90`
- New official baseline: `5c1d9b0e2669199114a12ade38027da132303eb3`
- Upstream distance: #3772 Hebrew, #3776 Greek parity, #3778 MDBList ratings in the Grid hero and an Exo libass fix, #3775 global landscape poster mode, #3780, #3784 detail hero trailer controls focus, version bump (22 Kotlin files, +407 / −123, plus el / iw / pl / vi strings).
- Superfork integration merge commit: `89b46c4dfa4327a0eaaa212e2c05bad3c9c6b766` (this PR; merged with a merge commit so official stays a parent).
- Conflicts: none. Files changed by both sides (auto-merged, reviewed): `app/build.gradle.kts` (fork Seekr dependency, official version bump), `MainActivity` (fork dimmer / Live TV hooks, official landscape poster plumbing), `ContentCard` and `SearchDiscoverSection` (fork G2c `directedFor` text direction, official landscape posters; no fork helper import lost), `LayoutHomeSettings` (fork Live TV row, official landscape setting moved), `values/strings.xml`.
- Re-audit (UPSTREAM_SYNC step 6): changed `PlayerLibassCompat` (official Exo libass fix) and `MainActivity`; untouched PlayerRuntimeController, PlayerMediaSourceFactory, ParallelRangeDataSource, StreamSpeedTester, PlayerDebugStatsOverlay, PlayerSettingsDataStore, SkipIntroRepository, navigation.
- Clean replays (Superfork Baseline Audit run 36848159277): official `5c1d9b0` 1,796 tests, 18 failed, 1 skipped, gradle exit 0, outcomes identical test by test to the reviewed `56aaba2` inventory (the 3 stale NuvioExoPlayerPerformanceHelperTest expectations are the same non-debt cases as under D050); integration merge `89b46c4` 2,205 tests, 15 failed (all registered), 0 new, 1 skipped. No test missing or newly skipped; debt and minimum test count unchanged. `integration/evidence/baseline-suite.json` is now the `5c1d9b0` replay.
- Test evidence: exact-head Full Debug CI of this PR.
- Decision: adopt official before G12 coding (D059).

## 2026-10-01 — Sync before G14 code (official player error path)
- Purpose: G14 hardens the release and touches the player error path and logging; official `dev` changed the player error path (libmpv failure causes, startup watchdog). UPSTREAM_SYNC "before beginning a gate that will touch an upstream subsystem that changed materially" applies; D065.
- Previous official anchor: `5c1d9b0e2669199114a12ade38027da132303eb3`
- New official baseline: `aeb6ee8591c55424256fdd1f35f426618297378d`
- Upstream distance: #3794 (MDBList ratings disk cache, stream-screen focus fix, Polish / Hebrew strings, autosync strings moved into `strings.xml`, test constructors), #3796 libmpv failure causes on the error screen (`PlayerRuntimeControllerMpvEvents`, `MpvStartupWatchdogPolicy`, error recovery), #3797 Back after a binge-group skip (11 commits, 26 files, +1,639 / −80).
- Superfork integration merge commit: `ed62c7bbb9a6c1065c5f6368048232293f8d235e` (this PR; merged with a merge commit so official stays a parent).
- Conflicts: none. Files changed by both sides (auto-merged, reviewed): `MdbListRatingsLoader`, `NuvioNavHost`, `NuvioMpvSurfaceView`, `PlayerRuntimeController`, `PlayerRuntimeControllerErrorRecovery`, `PlayerRuntimeControllerLifecycle`, `PlayerRuntimeControllerMpv`, `PlayerRuntimeControllerPlaybackEvents`, `PlayerScreen`, `StreamScreen`, `StreamScreenViewModel`, `values/strings.xml`. No duplicate string resources after the autosync strings moved (values, values-ar, values-iw).
- Re-audit (UPSTREAM_SYNC step 6): the fork's player hooks (G3 strategy, G4 recovery, G5 audio / DV, G6 subtitles, G7 seek, G8 streams, G10 Live TV, G11 Watch Party, G12 screensaver, G13 MAT) sit beside official's new mpv error handling; untouched PlayerMediaSourceFactory, ParallelRangeDataSource, PlayerRuntimeControllerInitialization, PlayerSettingsDataStore.
- Clean replays (Superfork Baseline Audit run 36891605846): official `aeb6ee8` 1,824 tests, 18 failed, 1 skipped, gradle exit 0; all 1,796 tests of the `5c1d9b0` inventory present with identical outcomes, 28 new official tests (24 MpvPlaybackErrorPolicyTest, 4 PlayerBackPressTest) all passing; the 3 stale NuvioExoPlayerPerformanceHelperTest expectations are the same non-debt cases as under D050 / D059. Integration merge `ed62c7b` 2,358 tests, 15 failed (all registered), 0 new, 1 skipped. No test missing or newly skipped; debt unchanged; minimum test count 1,796 → 1,824. `integration/evidence/baseline-suite.json` is now the `aeb6ee8` replay.
- Test evidence: exact-head Full Debug CI of this PR.
- Decision: adopt official before G14 code (D065).
