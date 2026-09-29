# Import Ledger

Every imported feature must add an entry before its PR is considered complete.

## Template

### <feature-id / feature-name>
- Roadmap gate:
- Source repository:
- Source branch:
- Pinned source SHA:
- Source commit(s):
- Source file(s):
- Import mode: CHERRY_PICK / FILE_PORT / DELTA_PORT / ALGORITHM_PORT / LOGIC_PORT / ADAPTER
- Current official equivalent:
- What already existed upstream:
- What was imported:
- What was intentionally not imported:
- Local adaptations:
- Feature flag / fallback:
- Tests ported/added:
- License / attribution notes:
- Resulting local commit:
- Known risks / follow-up:

## Entries

### G1a — Add-on health (281–291)
- Roadmap gate: G1
- Source repository: ysosrs123/NuvioTV-Fork
- Source branch: nuvio-test
- Pinned source SHA: 45e0984c18460d2a65c5d745999011b4314328eb
- Source commit(s): tree at the pinned SHA (shallow history; file-level diff used)
- Source file(s): app/src/main/java/com/nuvio/tv/core/health/AddonHealthModel.kt, AddonHealthStore.kt; recording hooks in StreamRepositoryImpl/AddonManagerScreen
- Import mode: ALGORITHM_PORT
- Current official equivalent: none for health; AddonRepositoryImpl manifest cache/placeholder (288) and StreamRepositoryImpl per-add-on isolation (290)
- What already existed upstream: 288, 290 (see docs/audits/G1_DIAGNOSTICS_AUDIT.md)
- What was imported: outcome mapping (success / empty / timeout-error) and the 8 s slow threshold
- What was intentionally not imported: profile DataStore persistence, rolling window + breaker cooldown, catalog/meta/subtitle/resolver recording, `addonHealthEnabled` setting, ysosrs UI
- Local adaptations: specific states (Healthy, Slow, Timeout, Auth error, Manifest error, No streams, Request error) classified from NetworkResult; in-memory `fork/diagnostics/AddonHealthTracker`; badge in official Addon Manager cards; EN + AR strings
- Feature flag / fallback: FeatureId.UNIFIED_DIAGNOSTICS (default AUTO, D041); OFF records nothing
- Tests ported/added: AddonHealthTest (10), StreamRepositoryAddonHealthTest (2), FeatureRegistryTest updated for D041
- License / attribution notes: GPL-3.0, identical LICENSE file to official; algorithm source attributed in AddonHealth.kt KDoc
- Resulting local commit: recorded in HANDOFF after merge
- Known risks / follow-up: 289 deferred to G8 (D041); health is per process (resets on restart)

### G1b — Diagnostics HUD rows (61–81)
- Roadmap gate: G1
- Source repository: ysosrs123/NuvioTV-Fork
- Source branch: nuvio-test
- Pinned source SHA: 45e0984c18460d2a65c5d745999011b4314328eb
- Source commit(s): tree at the pinned SHA (file-level diff)
- Source file(s): ui/screens/player/PlaybackStatsOverlay.kt, PlayerViewModel.samplePlaybackStats, PlayerPlaybackAnalyticsDiagnostics.kt (HudSample), PlaybackSpeedAwareAudioSink.kt (jitter)
- Import mode: DELTA_PORT (diagnostics HudSample) + ALGORITHM_PORT (row selection, drift plausibility bound)
- Current official equivalent: PlayerDebugStatsOverlay (owner), StreamInfoData, PlayerPlaybackAnalyticsDiagnostics, ParallelRangeDataSource HUD fields
- What already existed upstream: 63, 70, 72, 75, 78, 80 rows; decoder/underrun/load/rebuffer counters
- What was imported: reduced HudSample accessor; metric set for video/HDR/DV/display/audio/output/need/conn/stall counters
- What was intentionally not imported: second HUD, logcat instrumentation, PlaybackByteCounter, mux-rate estimator, 20 ms sink jitter sampler, rate-limit/hedge rows
- Local adaptations: pure `fork/diagnostics/PlaybackHud.kt` + `ClockDriftMeter`; audio output encoding from `onAudioTrackInitialized`; connection/chunk HUD fields in official ParallelRangeDataSource; rows appended to the official overlay only when UNIFIED_DIAGNOSTICS is not OFF
- Feature flag / fallback: FeatureId.UNIFIED_DIAGNOSTICS (AUTO, D041); the overlay itself stays behind the official stats-HUD setting
- Tests ported/added: PlaybackHudTest (11)
- License / attribution notes: GPL-3.0, identical LICENSE; source attributed in PlaybackHud.kt KDoc
- Resulting local commit: recorded in HANDOFF after merge
- Known risks / follow-up: 77 is 1 Hz drift, not sub-frame jitter; HDR10+ and TrueHD-Atmos are not signalled by media3 Format and are not claimed; on-device values MANUAL-PENDING

### G1c — Device assessment + apply/revert (14–16)
- Roadmap gate: G1
- Source repository: ysosrs123/NuvioTV-Fork
- Source branch: nuvio-test
- Pinned source SHA: 45e0984c18460d2a65c5d745999011b4314328eb
- Source commit(s): tree at the pinned SHA (file-level diff)
- Source file(s): core/assessment/AssessmentModels.kt, DeviceAssessmentApplier.kt, DeviceAssessmentEngine.kt, ui/screens/settings/DeviceAssessmentScreen.kt
- Import mode: ALGORITHM_PORT (tier model, apply order via official setters, snapshot-then-write, revert-last-apply)
- Current official equivalent: NetworkSettingsScreen stream test, MemoryBudget, NuvioExoPlayerPerformanceHelper, DisplayCapabilities, PlayerSettingsDataStore setters
- What already existed upstream: the measurement (stream test) and every setting assessed
- What was imported: MEASURED/CALCULATED/VERIFY tiers; plan with null = untouched; snapshot before first write; revert restores and clears
- What was intentionally not imported: StreamSweepEngine, intent profiles, DV/HDR10+/VOD/performance-mode/passthrough/MAT rows, ysosrs screen
- Local adaptations: pure `fork/diagnostics/DeviceAssessment.kt`; `DeviceAssessmentApplier` (typed DataStore keys, not org.json); `DeviceAssessmentViewModel` two-pass buffer cap; `DeviceAssessmentSection` card in the official advanced settings list; EN + AR strings
- Persistence: new profile-scoped DataStore file `fork_device_assessment` (via ProfileDataStoreFactory, `_p<id>` suffix for non-primary profiles). Keys: `snapshot_use_parallel_connections` (bool), `snapshot_parallel_connection_count` (int), `snapshot_target_buffer_size_mb` (int), `snapshot_frame_rate_matching_mode` (string, FrameRateMatchingMode name; presence = revert available). No official key is renamed or reused. Fresh install: no file, no revert offered. Corrupt mode value: snapshot discarded, no setting written. Downgrade: older builds ignore the file.
- Feature flag / fallback: FeatureId.UNIFIED_DIAGNOSTICS (AUTO, D041); settings change only on the explicit Apply action
- Tests ported/added: DeviceAssessmentTest (7), DeviceAssessmentApplierTest (6)
- License / attribution notes: GPL-3.0, identical LICENSE; source attributed in KDoc
- Resulting local commit: recorded in HANDOFF after merge
- Known risks / follow-up: TV D-pad focus through the new card MANUAL-PENDING; recommendations limited to the three rows above

### G2a — Resource tiers + fan-out/image/post-play policies (262–265, 269–273)
- Roadmap gate: G2
- Source repository: hackerslash/NuvioTV-Lite
- Source branch: dev
- Pinned source SHA: 2afdcd05d45e27afd48fb83ef9db6c286216a44c
- Source commit(s): tree at the pinned SHA (file-level diff against official 71632b9)
- Source file(s): core/device/DeviceMemoryTier.kt, NuvioApplication.kt (image loader), StreamRepositoryImpl.kt, SubtitleRepositoryImpl.kt, HomeViewModel.kt, PostPlayRecommendationState.kt (postPlayPrefetchIndices), PostPlayRecommendationController.kt, DeviceMemoryTierTest.kt
- Import mode: ALGORITHM_PORT (tier cuts, unknown-as-low-RAM, fetch permits, prefetch indices) + DELTA_PORT (image loader and consumer call sites)
- Current official equivalent: NuvioExoPlayerPerformanceHelper.getDevicePhysicalRamBytes, NuvioApplication.newImageLoader RAM-scaled cache share, MemoryBudget heap tier
- What already existed upstream: physical RAM read (263), RAM-scaled poster cache, RGB565 user toggle
- What was imported: two-cut tier; add-on fetch permits 3/6; poster cache 0.08, forced RGB565, no animated decoders, no SWR revalidation, decode parallelism 2 on low-RAM; single-card post-play prefetch; catalog concurrency 2 on low-RAM
- What was intentionally not imported: liteMode edition switches, MemoryDiagnostics, Lite reverts of official features, cap of 8 fetches on strong devices, Lite catalog concurrency 4, 100 MB disk cache
- Local adaptations: pure `fork/resource/AdaptiveResources.kt` (MemoryTier, AdaptiveResourcePolicy, AdaptiveResources holder); Android `AdaptiveResourcesInstaller.kt`; standard tier = official values; clamps never raise official values; OFF/not-installed = OFFICIAL policy
- Feature flag / fallback: FeatureId.ADAPTIVE_RESOURCE_MANAGER (AUTO, D042); OFF restores official values
- Tests ported/added: AdaptiveResourcesTest (13; Lite DeviceMemoryTierTest cases re-expressed on MemoryTier)
- License / attribution notes: GPL-3.0, identical LICENSE; source attributed here and in the audit
- Resulting local commit: recorded in HANDOFF after merge
- Known risks / follow-up: on-device memory and scrolling behavior on 1–2 GB boxes MANUAL-PENDING; the official RGB565 toggle still shows the user value while low-RAM forces it on

### G2b — Playback allocation safety by tier (266–268)
- Roadmap gate: G2
- Source repository: hackerslash/NuvioTV-Lite
- Source branch: dev
- Pinned source SHA: 2afdcd05d45e27afd48fb83ef9db6c286216a44c
- Source commit(s): tree at the pinned SHA (file-level diff against official 71632b9)
- Source file(s): ui/screens/settings/MemoryBudget.kt (isConstrainedTier, LOW_RAM_BUFFER_CEILING_MB, clampParallel), MemoryBudgetTest.kt
- Import mode: ALGORITHM_PORT (constrained tier drives allocation safety, 250 MB ceiling, 4-connection clamp)
- Current official equivalent: MemoryBudget heap tier and budget, tierMaxChunkMb, PlayerMediaSourceFactory session connections, NuvioExoPlayerPerformanceHelper native limits
- What already existed upstream: heap-tier ratio/reserve/floor, 16 MB low-tier chunk cap, native safe/warning limits, prefetch-depth budget
- What was imported: constrained devices join official's low tier; 250 MB heap budget ceiling and 4 session connections on constrained
- What was intentionally not imported: Lite bufferCount formula, removal of official prefetchDepthChunks and large-target-buffer override, Lite perf-mode budget-fit chunk clamp, settings/UI changes
- Local adaptations: `AdaptiveResourcePolicy.heapBufferBudgetMb` / `parallelConnections` (identity on standard); two call sites in official MemoryBudget and one in PlayerMediaSourceFactory
- Feature flag / fallback: FeatureId.ADAPTIVE_RESOURCE_MANAGER (AUTO, D042); OFF restores official heap-only tiering
- Tests ported/added: AdaptiveResourcesTest (+2, Lite MemoryBudgetTest ceiling/clamp cases re-expressed on the policy); corrected NuvioExoPlayerPerformanceHelperTest expectations (official 2a22a6f22)
- License / attribution notes: GPL-3.0, identical LICENSE
- Resulting local commit: recorded in HANDOFF after merge
- Known risks / follow-up: startup/seek behavior on 2 GB boxes with the lower budget MANUAL-PENDING

### G2c — Bounded caches, stream-list and bidi work (274, 275, 277, 278)
- Roadmap gate: G2
- Source repository: hackerslash/NuvioTV-Lite
- Source branch: dev
- Pinned source SHA: 2afdcd05d45e27afd48fb83ef9db6c286216a44c
- Source commit(s): c760295eb (stream lists / snapshotFlow), d491bd09e (bidi styling), tree at the pin for cache sizes
- Source file(s): core/util/LruCacheMap.kt + LruCacheMapTest.kt; TmdbMetadataService, TmdbService, SimklIdResolver, ParentalGuideRepository, TraktRelatedService, StaleWhileRevalidateCacheStrategy, MdbListRatingsLoader, ImdbEpisodeRatingsRepository (sizes); ui/util/TextDirectionUtils.kt (directedFor); StreamComponents, StreamSourcesSidePanel, ContentCard, HeroCarousel, SourceStatusFilterChip, SearchDiscoverSection
- Import mode: FILE_PORT (LruCacheMap → fork/resource), DELTA_PORT (cache call sites, directedFor helpers, snapshotFlow pagination)
- Current official equivalent: unbounded ConcurrentHashMap/mutableMapOf caches with TTL; official emoji-aware contentTextDirection
- What already existed upstream: TTL expiry, in-flight de-duplication, direction detection
- What was imported: LRU map; per-cache sizes; remembered direction/style helpers; snapshotFlow pagination in the player source panel
- What was intentionally not imported: bounding on standard devices, SkipIntroRepository caches, TMDB collection image semaphore, Lite's older direction scan, remaining non-hot bidi sites
- Local adaptations: `AdaptiveResourcePolicy.boundedCache(official, n)` keeps the official map on standard; `lruCacheMap` rejects non-positive sizes
- Feature flag / fallback: FeatureId.ADAPTIVE_RESOURCE_MANAGER (AUTO, D042) for caches; 277/278 are output-identical Compose optimizations
- Tests ported/added: LruCacheMapTest (4, 2 ported), AdaptiveResourcesTest (+2)
- License / attribution notes: GPL-3.0, identical LICENSE; attributed in LruCacheMap.kt KDoc
- Resulting local commit: recorded in HANDOFF after merge
- Known risks / follow-up: focus-move smoothness on the source panel and RTL rendering MANUAL-PENDING; 276 deferred (no queue), 279 → G7, 280 → G3
