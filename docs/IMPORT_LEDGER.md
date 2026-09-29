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

### G4a — Dead-source failover + startup watchdog (19, 20)
- Roadmap gate: G4
- Source repository: ysosrs123/NuvioTV-Fork
- Source branch: nuvio-test
- Pinned source SHA: 45e0984c18460d2a65c5d745999011b4314328eb
- Source commit(s): tree at the pinned SHA (file-level diff)
- Source file(s): ui/screens/player/PlayerRuntimeControllerErrorRecovery.kt (isDeadSourceHttpError, isDeadSourcePlaybackError, advanceToNextLiveSource, attemptStartupExhaustedSourceFailover), PlayerRuntimeControllerObservers.kt (scheduleStartupWatchdog, retractStartupTimeoutErrorAfterFirstFrame), PlayerRuntimeControllerInitialization.kt (hook sites), strings
- Import mode: ALGORITHM_PORT (dead classification, next-live-source selection, watchdog extend/fire and reason) + DELTA_PORT (hook sites, strings)
- Current official equivalent: onPlayerError ladders (NPE, MediaPeriodHolder, 416, parsing probe, engine failover, auto-retry), first-frame and stall watchdogs
- What already existed upstream: every same-URL recovery ladder; nothing advanced to another source; no pre-READY startup watchdog
- What was imported: 404/410 and non-media-body failover, mid-play malformed/IO failover, startup-exhausted failover, 3-failover cap, dead-URL set, startup watchdog 20 s / 60 s ceiling with buffered-ahead extension and four honest reasons, late-frame retraction; EN strings (AR written here)
- What was intentionally not imported: ysosrs removal of official VC-1 guards (official is newer), mime-override clear re-init, total auto-recovery budget across all ladders, dead-source greying in the source panel, TtffTrace markers, denied-audio FFmpeg fallback (G5)
- Local adaptations: pure `fork/recovery/PlaybackRecovery.kt` (DeadSourcePolicy, StartupWatchdogPolicy); controller code in new `PlayerRuntimeControllerSourceFailover.kt`; mid-play failover runs after official auto-retry (ysosrs ran it before); failover count resets at first frame; host-only logging
- Feature flag / fallback: FeatureId.REMUX_PERFORMANCE (AUTO, D044); OFF restores official error handling
- Tests ported/added: PlaybackRecoveryTest (7)
- License / attribution notes: GPL-3.0, identical LICENSE; attributed in KDoc
- Resulting local commit: recorded in HANDOFF after merge
- Known risks / follow-up: hardware A/B (dead link, stuck startup) MANUAL-PENDING on TCL C6K

### G4b — Press-time connection warm-up (18); 8, 9, 11 verified official
- Roadmap gate: G4
- Source repository: ysosrs123/NuvioTV-Fork
- Source branch: nuvio-test
- Pinned source SHA: 45e0984c18460d2a65c5d745999011b4314328eb
- Source commit(s): tree at the pinned SHA (file-level diff)
- Source file(s): ui/screens/player/PlayerPlaybackNetworking.kt (prewarmPlaybackConnection, enqueueConcurrentSuffixTail, enqueueTailPrewarm, parsePrewarmContentRange*, prewarmHttpClient), ui/screens/stream/StreamScreen.kt (press sites)
- Import mode: ALGORITHM_PORT (dedup, window validation, Content-Range parsing) + DELTA_PORT (OkHttp warm requests, press hook)
- Current official equivalent: `PrefetchWindowStore` and its consumer in `ParallelRangeDataSource` (head consume, tail peek), shared connection pool; nothing produced entries
- What already existed upstream: the store, the consumer, the pool; ysosrs `ParallelRangeDataSource` code equals official apart from comments (official adds two HUD fields), so 8, 9, 11 are official
- What was imported: head window warm (bytes 0-262143) stored as the bootstrap entry, concurrent suffix-range tail (4 MiB) with exact-window validation, head-triggered fallback tail, 60 s same-URL dedup, pool-sharing client built like the parallel path's
- What was intentionally not imported: duplicate head warm on a failed/range-hostile head (second-socket fallback), focus-time warm (`PrefetchSelectionSupplier`, stream prefetch, G8), POOL_ID diagnostics logging, AFR preflight head peek
- Local adaptations: runs only when the press resolves to the REMUX / Throughput strategy (G3 `decide`, new) and REMUX_PERFORMANCE is not OFF; torrents and non-http URLs skipped; no tail window on the low-RAM tier (stored windows are heap, 5 min TTL); host-only logging; one hook in `onInternalPlayerLaunching`
- Feature flag / fallback: FeatureId.REMUX_PERFORMANCE (AUTO, D044) + PLAYBACK_STRATEGY_ENGINE selection (default Official, D043); Official/Seek/Low memory send no extra requests
- Tests ported/added: ConnectionPrewarmPolicyTest (7)
- License / attribution notes: GPL-3.0, identical LICENSE; attributed in KDoc
- Resulting local commit: recorded in HANDOFF after merge
- Known risks / follow-up: extra 256 KiB + 4 MiB per REMUX press; startup gain MANUAL-PENDING (TCL C6K A/B)

### G4c — Matroska mid-stream resync (21); 22, 23 verified official
- Roadmap gate: G4
- Source repository: ysosrs123/NuvioTV-Fork
- Source branch: nuvio-test
- Pinned source SHA: 45e0984c18460d2a65c5d745999011b4314328eb
- Source commit(s): tree at the pinned SHA (file-level diff)
- Source file(s): core/player/dvmkv/MatroskaExtractor.java (resyncToNextCluster, MAX_RESYNC_ATTEMPTS, MAX_RESYNC_SCAN_BYTES, RESYNC_BLOCK_BYTES, resetParsingState, read() catch)
- Import mode: ALGORITHM_PORT (Cluster ID block scan, budget) + DELTA_PORT (catch site, reset extraction)
- Current official equivalent: vendored dvmkv MatroskaExtractor with truncated-tail end-of-input (22), EBML-reader level-1 resync on zero padding between elements, nested SeekHead following capped at 4 (23)
- What already existed upstream: 22 and 23 (ysosrs is older there and lacks both); a corrupted element inside cluster data still failed playback with ParserException 3001
- What was imported: after the seek map, a ParserException in cluster data resets parsing state (the official seek() body) and block-scans forward to the next Cluster ID, 8 attempts per extractor, 64 MiB scan span, 64 KiB blocks
- What was intentionally not imported: ysosrs removal of official truncated-tail handling (peekFullyOrEnd, finishReadAtEndOfInput, shouldTreatEbmlErrorAsEndOfInput), removal of nested SeekHead following, VarintReader IllegalStateException regression, MKV DefaultDuration frame rate (nt2, AFR, G5), DolbyVisionExtractorsFactory HDR/DV deltas (G5)
- Local adaptations: catch narrowed to ParserException (AR-008: ysosrs also caught IllegalStateException because its VarintReader threw ISE for a bad length mask; official throws ParserException there, and the remaining ISEs under reader.read are internal invariants); official truncated-tail handling keeps priority (checked first, so unknown-length streams and holes in the last max(8 MiB, 2%) still end the stream as official does); scan helper and flag in `fork/recovery/MkvResync.kt`; flag read from registry defaults like `AdaptiveResources.install`
- Feature flag / fallback: FeatureId.REMUX_PERFORMANCE (AUTO, D044); OFF restores official extractor behavior; budget exhaustion surfaces the error and G4a failover moves to the next source
- Tests ported/added: MatroskaMalformedResyncTest (5: resync gate, Cluster scan across a zero-filled hole and a block boundary, end of input). Full-extractor EBML fixtures ran only in a local JVM harness on the vendored media3 jars (the unit-test android.jar stubs SparseArray): zero-filled SimpleBlock recovered, 8 vs 9 damaged clusters, tail hole kept official, pre-seek-map corruption not resynced; the recovery fixtures fail with ParserException when resync is disabled
- License / attribution notes: GPL-3.0 (app) + Apache-2.0 (vendored media3 extractor header kept); attributed in comments and KDoc
- Resulting local commit: recorded in HANDOFF after merge
- Known risks / follow-up: a skipped hole drops up to one cluster of samples (brief visual/audio skip); real Usenet zero-fill files MANUAL-PENDING (TCL C6K)

### G4d — Seek optimized: MP4 session mode (24) and disk read-ahead ring (25)
- Roadmap gate: G4
- Source repository: ysosrs123/NuvioTV-Fork (24); DavidVamaiotu/NuvioTV-Reshaped (25)
- Source branch: nuvio-test (24); subtitle-autosync (25)
- Pinned source SHA: 45e0984c18460d2a65c5d745999011b4314328eb (24); 0ccf049d2789600835f3f7a75423e9149ea416ba (25)
- Source commit(s): trees at the pinned SHAs (file-level diff)
- Source file(s): ysosrs ui/screens/player/PlayerMediaSourceFactory.kt (resolveChunkSessionShape mp4SessionMode, MP4_SESSION_CHUNK_BYTES, allowContinuationReopen = !mp4SessionMode); Reshaped ui/screens/player/seekbuffer/SeekReadAhead.kt (whole file), PlayerMediaSourceFactory / PlayerRuntimeControllerLifecycle / PlayerRuntimeControllerMpv hooks
- Import mode: DELTA_PORT (24, factory branch) + FILE_PORT (25, SeekReadAhead) + ADAPTER (strategy gate, tier size)
- Current official equivalent: parallel `ParallelRangeDataSource` sessions (opt-in), VOD disk cache (keeps played data); no MP4-specific session and no read-ahead ring
- What already existed upstream: `ParallelRangeDataSource.Factory(allowContinuationReopen)`, the VOD cache, the G3 Seek optimized strategy (single connection + VOD cache)
- What was imported: MP4 session mode (1 connection, 8 MiB chunks, prefetch depth 2, whole-chunk retention) for progressive MP4; SeekReadAhead ring (one connection, pread/pwrite ring file, relocation on out-of-ring reads, one-connection-per-link handling, disk-failure fallback to direct reads, unbounded-stream handoff) for other progressive files; release on player release and on the MPV switch
- What was intentionally not imported: Reshaped SeekBufferSettings screen and SharedPreferences store, MPV demuxer cache sizing from that setting (26, deferred), seek-bar buffered-position extension and throughput-sampler hook (touch PlaybackEvents, which official #3740 changes), Live TV registry (Reshaped-only feature), ysosrs `shouldAllowBackgroundPrefetch = { true }` change (official startup gating kept), ysosrs nt13 prestartChunk0 wiring (G4b warm-up covers startup)
- Local adaptations: `fork/playback/SeekOptimizedMedia.kt` decides the mode (never both, never with parallel) and the tier size; ring cleanup of an earlier run's files on first use in the process instead of an Application hook; a non-read-ahead playback releases any live ring
- Feature flag / fallback: engaged only when the effective G3 strategy is Seek optimized and REMUX_PERFORMANCE is not OFF (D044, D045); Official/REMUX/Low memory/Auto-default never touch it
- Tests ported/added: SeekOptimizedMediaTest (5)
- License / attribution notes: GPL-3.0, identical LICENSE in both sources; attributed in KDoc
- Resulting local commit: recorded in HANDOFF after merge
- Known risks / follow-up: ring concurrency is Reshaped's (untested on JVM: android.system.Os pread/pwrite); disk wear and storage use on the TV; seek latency and rebuffer A/B MANUAL-PENDING (TCL C6K)

### G5a — Soft clipping for boosted volume (48)
- Roadmap gate: G5
- Source repository: DavidVamaiotu/NuvioTV-Reshaped
- Source branch: subtitle-autosync
- Pinned source SHA: 0ccf049d2789600835f3f7a75423e9149ea416ba
- Source commit(s): tree at the pinned SHA (file-level diff)
- Source file(s): ui/reshaped/volumeboost/VolumeBoostSoftClip.kt (softClipBoosted), ui/screens/player/GainAudioProcessor.kt (two call sites)
- Import mode: ALGORITHM_PORT (tanh knee at 0.8) + DELTA_PORT (hook in the official gain processor)
- Current official equivalent: `GainAudioProcessor` amplification 0-10 dB with a hard clamp at full scale
- What already existed upstream: the boost itself (47, verified official)
- What was imported: soft clip of amplified PCM16 and float samples above the knee; samples under the knee unchanged
- What was intentionally not imported: Reshaped `VolumeBoostBar` UI (official already has the amplification setting), Reshaped settings screen
- Local adaptations: pure `fork/audio/SoftClip.kt`; flag read from registry defaults; the official clamp stays as a final guard
- Feature flag / fallback: FeatureId.AUDIO_DV_AFR (AUTO, D048); OFF restores the official hard clamp; only reached when amplification > 0 dB
- Tests ported/added: SoftClipTest (4)
- License / attribution notes: GPL-3.0, identical LICENSE; attributed in KDoc
- Resulting local commit: recorded in HANDOFF after merge
- Known risks / follow-up: audible result MANUAL-PENDING (G14 campaign, HV-G5-1)

### G5b — Default to the best lossless audio track (37)
- Roadmap gate: G5
- Source repository: ysosrs123/NuvioTV-Fork
- Source branch: nuvio-test
- Pinned source SHA: 45e0984c18460d2a65c5d745999011b4314328eb
- Source commit(s): tree at the pinned SHA (file-level diff)
- Source file(s): ui/screens/player/LosslessAudioTrackDefault.kt (tiering, commentary filter, language preference, `applyLosslessAudioDefaultIfUnset`), PlayerRuntimeController / PlayerRuntimeControllerStreams (per-stream flags and resets), PlayerRuntimeControllerTracks / PlayerRuntimeControllerMpv (call sites)
- Import mode: ALGORITHM_PORT (pick) + DELTA_PORT (hooks after the official restore pass)
- Current official equivalent: audio picked by preferred language and container default/forced flags only
- What already existed upstream: remembered / persisted track preference restore, engine-switch carry-over, `selectAudioTrack`
- What was imported: lossless tiers TrueHD > DTS-HD MA > FLAC > PCM (ties by channel count), commentary-like names skipped, preferred languages first; applied at most once per stream and never over a user pick, a remembered or persisted preference, or an engine-switch carry-over
- What was intentionally not imported: ysosrs's always-on default (here behind a setting, D048); track names in the trace log (codec/language/channels only)
- Local adaptations: pure `fork/audio/LosslessAudioDefault.kt`; profile-scoped `fork/audio/AudioOutputPreferences` DataStore with a "Prefer lossless audio" toggle in Playback → Audio (EN/AR); selection is never written back as a preference
- Feature flag / fallback: setting default off (D048: changes default track selection for every playback); hidden and inert when FeatureId.AUDIO_DV_AFR is OFF
- Tests ported/added: LosslessAudioDefaultTest (6)
- License / attribution notes: GPL-3.0, identical LICENSE; attributed in KDoc
- Resulting local commit: recorded in HANDOFF after merge
- Known risks / follow-up: a sink without TrueHD/DTS-HD passthrough decodes the lossless track in software (G5c adds capability awareness); device check MANUAL-PENDING (G14 campaign, HV-G5-2)

### G5c — Per-format passthrough controls (38)
- Roadmap gate: G5
- Source repository: ysosrs123/NuvioTV-Fork
- Source branch: nuvio-test
- Pinned source SHA: 45e0984c18460d2a65c5d745999011b4314328eb
- Source commit(s): tree at the pinned SHA (file-level diff)
- Source file(s): core/player/AudioPassthroughPolicy.kt (whole file, user switches), ui/screens/player/PlaybackSpeedAwareAudioSink.kt (`shouldRejectDirectPlayback` deny, `isPolicyDeniedPassthrough`), PlaybackSpeedAwareAudioRenderer.kt (`getDecoderInfos` empty for denied formats), PlayerRuntimeControllerInitialization.kt (policy build and factory param)
- Import mode: FILE_PORT (policy) + DELTA_PORT (sink / renderer / factory hooks)
- Current official equivalent: one "force optical passthrough" switch; otherwise the platform capability report decides
- What already existed upstream: `PlaybackSpeedAwareAudioSink` force-PCM chokepoint (speed, Bluetooth, recovery), FFmpeg audio renderer
- What was imported: five "receiver decodes this" switches; a denied format is refused by the sink and routed to the FFmpeg decoder; formats FFmpeg cannot decode (AC-4, DTS Express, DTS:X P2) are never denied
- What was intentionally not imported: learned per-route rejection groups (F3, `audioRejectionsConfirmed`), AC-3 transcode of denied formats (F5 `DeniedTranscodePlanner`), error-recovery FFmpeg fallback hook, Kodi label wording
- Local adaptations: switches in the profile-scoped `fork_audio_output` store (not the official PlayerSettings); snapshot per playback in the player build; policy made inert while force-optical is active (the vendored FFmpeg renderer then expects AC-3 to pass through, so denying it would leave no renderer), with decoder priority OFF, or when FFmpeg is unavailable; one host-free log line when a format is denied
- Feature flag / fallback: every switch defaults on (= official, D048); ALLOW_ALL when FeatureId.AUDIO_DV_AFR is OFF (rows hidden)
- Tests ported/added: AudioPassthroughPolicyTest (4), BluetoothAudioRoutePolicyTest +2 (sink deny / default)
- License / attribution notes: GPL-3.0, identical LICENSE; attributed in KDoc
- Resulting local commit: recorded in HANDOFF after merge
- Known risks / follow-up: FFmpeg decode cost of TrueHD/DTS-HD on the TV CPU; device check MANUAL-PENDING (G14 campaign, HV-G5-3)

### G5c — Audio output diagnostics (39–42)
- Roadmap gate: G5
- Source repository: ysosrs123/NuvioTV-Fork
- Source branch: nuvio-test
- Pinned source SHA: 45e0984c18460d2a65c5d745999011b4314328eb
- Source commit(s): tree at the pinned SHA (file-level diff)
- Source file(s): ui/screens/player/AudioCapabilityReport.kt (direct-support probe, surround mode, per-encoding HDMI PCM channels), its capture in the sink build
- Import mode: ALGORITHM_PORT (probe) + ADAPTER (G1 HUD rows)
- Current official equivalent: `AudioCapabilities` / `AudioOutputRouteDetector` decide output; the G1 HUD shows source codec/channels and the output encoding
- What already existed upstream: the official stats overlay; G1b HUD rows `audio` / `output`
- What was imported: platform claim snapshot (formats accepted directly, surround mode, max PCM channels) shown as the HUD `chain` row; output PCM channel count; decode reason when a G5c receiver switch is off; stream-reported audio bitrate
- What was intentionally not imported: ysosrs negotiated-encodings line and Device Assessment per-format rows (tied to its F2/F3 learning), `AudioTrackRejectionLog` (F3 learning input), diagnostics page text export
- Local adaptations: pure `AudioChainSnapshot` + formatting in `fork/diagnostics/PlaybackHud.kt` (G1 owner); Android probe in `fork/diagnostics/AudioChainProbe.kt`, captured only while FeatureId.AUDIO_DV_AFR is not OFF
- Feature flag / fallback: HUD rows appear only in the official stats overlay; a failed or pre-API-29 probe leaves the row out
- Tests ported/added: PlaybackHudTest +3
- License / attribution notes: GPL-3.0, identical LICENSE; attributed in KDoc
- Resulting local commit: recorded in HANDOFF after merge
- Known risks / follow-up: `isDirectPlaybackSupported` reflects vendor audio-policy profiles, which some TVs over-report (the reason 38 exists); device check MANUAL-PENDING (G14 campaign, HV-G5-4)

### G5d1 — Dolby Vision conversion correctness (52, part)
- Roadmap gate: G5
- Source repository: ysosrs123/NuvioTV-Fork
- Source branch: nuvio-test
- Pinned source SHA: 45e0984c18460d2a65c5d745999011b4314328eb
- Source commit(s): tree at the pinned SHA (file-level diff)
- Source file(s): app/src/main/cpp/dovi_bridge.cpp (`map_conversion_mode` case 5, `noteRpuDropOnFailure`, drop on failed conversion in both NAL loops), core/player/DolbyVisionMatroskaTransformer.kt (F5 drop + abandon threshold)
- Import mode: DELTA_PORT
- Current official equivalent: a failed RPU conversion is forwarded raw with DV 8.1 signalling; preserve-mapping sends native mode 4
- What already existed upstream: single conversion site for BlockAdditional RPUs (ysosrs F4 is already official); mode-2 → mode-1 per-RPU fallback
- What was imported: drop a failed RPU (base layer continues as HDR10) with throttled counting and a 60-failure abandon threshold (MKV); the same drop on the native MP4/TS sample path behind a JNI switch; preserve-mapping uses mode 2 (standard 8.1)
- What was intentionally not imported: ysosrs `DolbyVisionConversionStats` drop counters for its diagnostics page, the header-include switch (`libdovi/rpu_parser.h`), EL-type and RPU metadata readers (G5d2)
- Local adaptations: the preserve-mapping fix lives in `DolbyVisionConversionConfig.conversionMode` (Kotlin) instead of the native mapping, so the native mapping stays official; verified against dolby_vision 3.3.2 source (`ConversionMode::from`: 2|3 → To81, 4 → To84, used by `dovi_convert_rpu_with_mode`)
- Feature flag / fallback: `DolbyVisionConversionConfig.forkDvFixes` and the native switch follow FeatureId.AUDIO_DV_AFR; OFF = official forwarding and mode mapping. Only failure paths and the experimental preserve-mapping option change (D048)
- Tests ported/added: DolbyVisionConversionConfigForkTest (3), DolbyVisionMatroskaTransformerForkTest (3)
- License / attribution notes: GPL-3.0, identical LICENSE; libdovi (MIT) unchanged; attributed in comments
- Resulting local commit: recorded in HANDOFF after merge
- Known risks / follow-up: with preserve mapping on, output now equals standard 8.1 (the preserve-mapping curve is unreachable through the bundled C API); device check MANUAL-PENDING (G14 campaign, HV-G5-5)

### G5d2 — DV stream metadata, EL type and HDR10 SEI on the strip path (52, 53 part, 54)
- Roadmap gate: G5
- Source repository: ysosrs123/NuvioTV-Fork
- Source branch: nuvio-test
- Pinned source SHA: 45e0984c18460d2a65c5d745999011b4314328eb
- Source commit(s): tree at the pinned SHA (file-level diff)
- Source file(s): app/src/main/cpp/dovi_bridge.cpp (`nativeDetectRpuElType`, `nativeGetRpuStaticMetadata`, libdovi header include), core/player/DoviBridge.kt (`RpuStaticMetadata`, readers), core/player/Hdr10SeiInjector.kt (whole file), core/player/DolbyVisionMatroskaTransformer.kt (EL probe, metadata probe, `injectHdr10SeiIfEnabled`)
- Import mode: FILE_PORT (injector, native readers) + DELTA_PORT (transformer / factory hooks) + ADAPTER (HUD `dv` row, fork setting)
- Current official equivalent: none (no RPU metadata read, no SEI authoring); official `hdr` HUD row shows the stream format
- What already existed upstream: libdovi bridge, RPU strip, HDR10+ SEI strip, single-track EL strip
- What was imported: EL type and static-metadata readers; MDCV + CLLI authoring with emulation prevention; injection before the first slice for DV8 on the MKV strip path when absent
- What was intentionally not imported: Annex-B injection and the MP4/TS metadata probe (no strip-path injection there in ysosrs either), `DolbyVisionConversionStats` diagnostics page, startup self-test (became unit tests), the bridge startup exercise call
- Local adaptations: pure injector and metadata types in `fork/video` (length-delimited only); HUD `dv` row fed by `fork/video/DvStreamInfo` (reset per playback); switch in the G5 profile store (`hdr10_sei_on_dv_strip`) with a row in the official Dolby Vision / HDR section (EN/AR)
- Feature flag / fallback: injection needs the switch (default off, D048) and FeatureId.AUDIO_DV_AFR not OFF; probes run only with the group on and the native bridge available; any read failure leaves the row empty
- Tests ported/added: Hdr10SeiInjectorTest (6, from ysosrs selfTest), PlaybackHudTest +1
- License / attribution notes: GPL-3.0, identical LICENSE; libdovi (MIT) credited in KDoc
- Resulting local commit: recorded in HANDOFF after merge
- Known risks / follow-up: primaries assumed BT.2020/D65 (RPU does not carry them); device checks MANUAL-PENDING (G14 campaign, HV-G5-6, HV-G5-7)
