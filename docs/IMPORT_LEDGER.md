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

### G5d3 — Display output state in the HUD (53 part, 60); 55 verified official
- Roadmap gate: G5
- Source repository: none (local ADAPTER over the Android Display API; the ysosrs and Reshaped deltas for these IDs are not imported)
- Source branch: n/a
- Pinned source SHA: n/a
- Source commit(s): n/a
- Source file(s): n/a
- Import mode: ADAPTER (G1 HUD rows)
- Current official equivalent: HUD `display` row shows the refresh rate only; official True-black letterbox toggle (55)
- What already existed upstream: `LetterboxRenderPolicy` / `PlayerWindowBackdrop`, `transparentLetterbox` setting (default off)
- What was imported: nothing; added the display mode size to the `display` row and a `tv hdr` row (Display HDR types)
- What was intentionally not imported: Reshaped `LetterboxRenderPolicy.defaultTransparentLetterbox` (turns true-black on for every non-Amazon device, D048), ysosrs deletion of `LetterboxRenderPolicy` (removal not inherited)
- Local adaptations: `supportedHdrTypesOf` reads `Display.Mode.supportedHdrTypes` on API 34+, `Display.getHdrCapabilities` before; the current panel HDR mode is not exposed by Android and is not claimed
- Feature flag / fallback: HUD rows appear only in the official stats overlay; unreadable values leave the row out
- Tests ported/added: PlaybackHudTest +1
- License / attribution notes: n/a (local)
- Resulting local commit: recorded in HANDOFF after merge
- Known risks / follow-up: some TVs report HDR types for the panel rather than the HDMI source mode; device check MANUAL-PENDING (G14 campaign, HV-G5-8)

### G5e — Track-format AFR fallback and settle hold (58, 59); 56 verified official
- Roadmap gate: G5
- Source repository: ysosrs123/NuvioTV-Fork
- Source branch: nuvio-test
- Pinned source SHA: 45e0984c18460d2a65c5d745999011b4314328eb
- Source commit(s): tree at the pinned SHA (file-level diff)
- Source file(s): ui/screens/player/PlayerRuntimeControllerAfrTrack.kt (`maybeRunTrackFormatAfr`, `resumePlaybackAfterTrackAfrIfHeld`, deadline / settle constants), core/player/FrameRateUtils.kt (`MIN_AFR_SWITCH_FPS` floor)
- Import mode: ALGORITHM_PORT (decision, hold/settle/release) + DELTA_PORT (start-site gates, tracks / preflight hooks, floor)
- Current official equivalent: ExoPlayer AFR switches only from the probing preflight (async to prepare); a failed probe means no switch; MPV has a settle delay
- What already existed upstream: preflight probe + in-memory cache, `refineFrameRateForDisplay`, frame-rate-first mode selection, track frame rate recorded in UI state
- What was imported: fallback switch from the track's reported rate when the preflight found none, start held through the switch + 2 s settle with an 8 s absolute deadline, generation guard across stream changes, detection cached for the next play; < 20 fps never switches the panel
- What was intentionally not imported: ysosrs cache-only ExoPlayer preflight (replaces official probing for every AFR user), C-2 prewarm-head seed (`MatroskaAfrProbe`), disk persistence of the fps cache, host-less cache key, audio quiesce during the switch (Shield-specific), ysosrs `selectModesForVideoResolution` rewrite (official has a newer selector)
- Local adaptations: pure `fork/video/TrackAfrPolicy` decision; deferral to the end of a running preflight (`onAfrPreflightFinished`) instead of the ysosrs cache-only flow; the start sites in `initializePlayer` (READY actions and first frame) stand down while `afrTrackSwitchInFlight`, and the release re-applies the start unless paused or opened paused
- Feature flag / fallback: FeatureId.AUDIO_DV_AFR OFF = official (no fallback switch, no floor); runs only with AFR on and the ExoPlayer engine, and only when the preflight had no detection (D048: a user-chosen path's failure case)
- Tests ported/added: TrackAfrPolicyTest (3), FrameRateUtilsForkTest (3)
- License / attribution notes: GPL-3.0, identical LICENSE; attributed in KDoc
- Resulting local commit: recorded in HANDOFF after merge
- Known risks / follow-up: startup hold timing is device-dependent (HDMI mode-switch latency); device checks MANUAL-PENDING (G14 campaign, HV-G5-9, HV-G5-10)

### G6a — Stream-provided subtitle reference for official AutoSync (84); Arabic AutoSync strings (94)
- Roadmap gate: G6
- Source repository: local (REWRITE on official AutoSync; no fork code imported)
- Source branch: n/a
- Pinned source SHA: n/a (official baseline `56aaba2`)
- Source commit(s): n/a
- Source file(s): n/a
- Import mode: ADAPTER (fallback reference input to official `AutomaticSubtitleSync.findTimelineRetime`) + REWRITE (Arabic strings)
- Current official equivalent: AutoSync V2 references embedded subtitles only; with none it stops with "no embedded subtitles to compare with"; AutoSync strings are English only
- What already existed upstream: the whole AutoSync engine, cue parser, download with size cap and 429 retry, reference size/span floors, confidence gates, failure toasts
- What was imported: nothing; added a `fallbackReferences` input used only when no embedded reference was found; the playing stream's own subtitles (`isStreamProvided`, same release) other than the target become reference tracks held to official's floors (8 cues, 45 s span); at most 2; `values-ar/autosync_strings.xml`
- What was intentionally not imported: a second engine (VibeSubtitle aligner, PR #38); add-on subtitles as references (their release is unknown); hash-matched references (85 deferred: no per-subtitle flag in the protocol)
- Local adaptations: pure `fork/subtitles/StreamSubtitleReference` selection; `PlayerRuntimeControllerStreamSubtitleReference` maps `streamSubtitles`; each reference is downloaded with its own headers only (stream request headers are never added); one count-only log line
- Feature flag / fallback: FeatureId.SUBTITLE_INTELLIGENCE (AUTO, D051) OFF = official AutoSync exactly; runs only while the user's Auto Sync setting is on and only on its no-embedded-reference path
- Tests ported/added: StreamSubtitleReferenceTest (6), FeatureRegistryTest updated
- License / attribution notes: n/a (local)
- Resulting local commit: recorded in HANDOFF after merge
- Known risks / follow-up: a stream that ships subtitles from another release would give a wrong reference (official confidence gates still apply); if upstream later adds Arabic AutoSync strings, the duplicate resource is resolved in that sync PR; device check MANUAL-PENDING (G14 campaign, HV-G6-1, HV-G6-2)

### G6b1 — Custom subtitle fonts: store, QR/LAN upload, HTTPS download, validation, fallback (100–104)
- Roadmap gate: G6
- Source repository: DavidVamaiotu/NuvioTV-Reshaped
- Source branch: subtitle-autosync
- Pinned source SHA: 0ccf049d2789600835f3f7a75423e9149ea416ba
- Source commit(s): tree at the pinned SHA (file-level diff)
- Source file(s): reshaped/subtitlefont/SubtitleFontStore.kt, reshaped/subtitlefont/SubtitleFontUploadServer.kt, ui/screens/settings/SubtitleFontSettingsItems.kt, res/values/subtitle_font_strings.xml; hooks in NuvioApplication, PlayerScreen, NuvioMpvSurfaceView
- Import mode: FILE_PORT (store, upload server, dialog, strings) + DELTA_PORT (four one-line hooks)
- Current official equivalent: none; ExoPlayer uses Typeface.DEFAULT(_BOLD), mpv `sub-font=Roboto`; official already ships NanoHTTPD, ZXing (`QrCodeGenerator`) and `DeviceIpAddress`
- What already existed upstream: QR config-server pattern (`core/server/*ConfigServer`), `NuvioDialog`, `SettingsActionRow`, subtitle style settings
- What was imported: one validated .ttf/.otf in app storage (20 MB cap, sfnt signature, Android load, family name), phone/computer upload page behind a 128-bit per-session path token and an Origin check, running only while the dialog is open and the app is in the foreground; file picker; URL import; reset; Exo typeface and libass `sub-fonts-dir`/`sub-font`; a font that stops loading is deleted so playback falls back to the official font
- What was intentionally not imported: Reshaped `LanAddress` (official `DeviceIpAddress` reused), the LazyList settings screen (`NuvioReshapedSettingsContent`), Reshaped bundled UI fonts (`res/font/*.ttf`, app fonts, not subtitle fonts), cleartext `http://` downloads
- Local adaptations: HTTPS-only URL import with `followSslRedirects(false)` and a final-URL HTTPS check; logs carry host and status only, never exception text or the path/query; pure `SubtitleFontFile` (signature, name table with a record bounds check) and `SubtitleFontImportPolicy` (token, origin, length, URL rules); upload page follows the app language and direction (Arabic RTL); row placed in the official subtitle style section
- Feature flag / fallback: FeatureId.SUBTITLE_INTELLIGENCE (AUTO, D051) OFF hides the row and the store returns no font (official font); no imported font = official font
- Tests ported/added: SubtitleFontFileTest (6), SubtitleFontImportPolicyTest (5)
- License / attribution notes: GPL-3.0 (Reshaped is a GPL-3.0 fork of official); attributed in KDoc
- Resulting local commit: recorded in HANDOFF after merge
- Known risks / follow-up: libass matches by family name, so a font whose name table disagrees with its internal family may fall back in mpv; LAN upload is plain HTTP on the local network, protected by the token shown only on the TV; device checks MANUAL-PENDING (G14 campaign, HV-G6-3, HV-G6-4)

### G6b2 — Arabic cinema subtitle preset (105)
- Roadmap gate: G6
- Source repository: local (REWRITE; no pinned source has a preset)
- Source branch: n/a
- Pinned source SHA: n/a
- Source commit(s): n/a
- Source file(s): n/a
- Import mode: REWRITE
- Current official equivalent: individual subtitle style settings (size, offset, bold, colors, outline); no presets
- What already existed upstream: every setter the preset writes
- What was imported: nothing; `fork/subtitles/ArabicCinemaPreset` (130 %, bold, white text, no box, black outline 3, offset 8) and a settings row that applies it once through the official setters and shows "Applied" while the values match
- What was intentionally not imported: an automatic preset for Arabic subtitles (would change official output without a user choice, D048/D051)
- Local adaptations: n/a
- Feature flag / fallback: FeatureId.SUBTITLE_INTELLIGENCE OFF hides the row; applying is a user action and every value stays editable
- Tests ported/added: ArabicCinemaPresetTest (3)
- License / attribution notes: n/a (local)
- Resulting local commit: recorded in HANDOFF after merge
- Known risks / follow-up: preset values are a design choice pending the G14 viewing check (HV-G6-5)

### G7a — Hybrid seek preview engine: local keyframes, bounded Seekr fallback, Preview Sync (107–110, 112, 115, 116, 279)
- Roadmap gate: G7
- Source repository: DavidVamaiotu/NuvioTV-Reshaped (engine); Cxsmo-ai/NuvioTV-Custom (key store)
- Source branch: subtitle-autosync; main
- Pinned source SHA: 0ccf049d2789600835f3f7a75423e9149ea416ba; 3e0d0fad60a2721adec133b88640b49c0183883f
- Source commit(s): trees at the pinned SHAs (file-level diff)
- Source file(s): Reshaped `ui/screens/player/seekpreview/*` (SeekPreviewTrack, SeekPreviewState, SeekPreviewThumbnailHost, SeekPreviewSyncOverlay, SeekPreviewSyncConfig, SeekPreviewCueGrid, SeekPreviewPlayerHooks, BoundedSeekrTrack, SeekrContentMapping, local/VideoKeyframeTap, local/KeyframeThumbnailDecoder, local/LocalPreviewTrack, local/LocalPreviewSources, local/LocalSeekPreviewSettings) + tests SeekPreviewCueStepperTest, SeekrContentMappingTest + PlayerScreen/ViewModel/PlaybackEvents/Lifecycle hook hunks; Cxsmo `data/local/SeekrCredentialsStore.kt`
- Import mode: FILE_PORT (engine, UI, tests, key store) + DELTA_PORT (player hooks)
- Current official equivalent: none (scrubber without previews)
- What already existed upstream: extractor-factory seam, scrubber composables (`ProgressBar`, `SeekOverlay`), grid-less D-pad scrubbing, NanoHTTPD-free settings rows
- What was imported: thumbnails from the keyframes playback already downloads (extractor tap, one software decoder, decoded only while paused/scrubbing, spool while playing, per-title disk cache), keyframe-exact commit, memory-bounded Seekr sprites for slots without a local frame, hybrid track, cue ticks and grid-locked scrubbing, manual Preview Sync; Seekr key encrypted per profile with an Android Keystore key
- What was intentionally not imported: Reshaped plaintext `SeekrKeyPreferences` and the built-in `BuildConfig.SEEKR_API_KEY`; Reshaped phone key-send page; Cxsmo live-player surface capture and unbounded SDK host (calibration comes in G7b on local frames); Reshaped audio-sync tap chained in the same factory
- Local adaptations: package `fork/seek`; limits from `AdaptiveResources.seekPreviewBudget` (48/32/16 decoded, 200/100/50 MB cache, 96/48/24 MB spool by tier; no decode above 1080p under 3 GB; low-RAM opt-in) instead of fixed constants; keyframe tap chained after the DV factory and before AutoSync's; logs carry exception class only; key checked with Seekr (`X-API-Key`, no redirects) before saving; Seekr SDK `tv.seekr:seekr-android:0.2.0` (Apache-2.0)
- Feature flag / fallback: FeatureId.SEEK_INTELLIGENCE (AUTO, D052) OFF = no tap, no tracks, rows hidden, official scrubber; no key = local previews only; unsupported codec/size = no local frames; MPV = official scrubber
- Tests ported/added: SeekPreviewCueStepperTest (22, ported), SeekrContentMappingTest (ported), SeekPreviewBudgetTest (5), SeekrKeyValidatorTest (2), FeatureRegistryTest updated
- License / attribution notes: GPL-3.0 forks of official; Seekr SDK Apache-2.0 (compatible); attributed in KDoc
- Resulting local commit: recorded in HANDOFF after merge
- Known risks / follow-up: software decoders vary by device (a codec without one gets no local frames); Dolby Vision streams may expose a DV mime with no software decoder; device checks MANUAL-PENDING (G14 campaign, HV-G7-1..HV-G7-4)

### G7b — Automatic Seekr calibration against local keyframe frames (111, 113, 114)
- Roadmap gate: G7
- Source repository: Cxsmo-ai/NuvioTV-Custom
- Source branch: main
- Pinned source SHA: 3e0d0fad60a2721adec133b88640b49c0183883f
- Source commit(s): tree at the pinned SHA (file-level diff)
- Source file(s): ui/screens/player/SeekrFrameCalibration.kt (`SeekrFrameCalibrator.estimate`, `perceptualSimilarity`, `sampleLuma`, constants)
- Import mode: ALGORITHM_PORT
- Current official equivalent: none
- What already existed upstream: nothing; G7a (#45) provides the local keyframe thumbnails and the bounded Seekr track
- What was imported: 32×18 luma-grid normalized cross-correlation, weak (< 0.56) and ambiguous (margin < 0.035) match rejection, median + MAD inlier band (max(1.25 s, 3·MAD)), confidence = mean inlier similarity × inlier share
- What was intentionally not imported: `ExoSeekrFrameCapture` (seeks and mutes the live player behind a cover to copy its surface) and the three fixed source anchors at 20/50/80 %
- Local adaptations: reference frames are real keyframes the local track already decoded (no seeking, no extra network); each anchor compares one local frame with the Seekr thumbnails at candidate offsets (a cue-interval grid around 0 and the duration-gap hint, ±240 s), one candidate per distinct cue so tiles cannot tie; applied only when ≥ 3 anchors agree with confidence ≥ 0.5 and the result is ≥ 1 s; runs only while paused or scrubbing, at most 5 attempts per track; never overrides a manual Preview Sync value; a new track resets both
- Feature flag / fallback: FeatureId.SEEK_INTELLIGENCE OFF = no Seekr track, no calibration; rejected or weak result = offset unchanged (0 or the manual value)
- Tests ported/added: SeekrCalibrationTest (10)
- License / attribution notes: GPL-3.0 (Cxsmo is a GPL-3.0 fork of official); attributed in KDoc
- Resulting local commit: recorded in HANDOFF after merge
- Known risks / follow-up: titles whose watched part is mostly dark or static give weak matches and stay uncalibrated (by design); device check MANUAL-PENDING (G14 campaign, HV-G7-5)

### G8a — Progressive AIOStreams with fallback; bounded add-on retry (147, 149, 150, 151, 289)
- Roadmap gate: G8
- Source repository: Cxsmo-ai/NuvioTV-Custom
- Source branch: main
- Pinned source SHA: 3e0d0fad60a2721adec133b88640b49c0183883f
- Source commit(s): tree at the pinned SHA (file-level diff)
- Source file(s): data/repository/StreamRepositoryImpl.kt (`fetchProgressiveStreams`, `isProgressiveAioStreamsUrl`, the snapshot-replace emission), data/remote/dto `ProgressiveStreamEnvelopeDto`
- Import mode: FILE_PORT (progressive endpoint, URL rules, DTO); retry policy local (no source has one: ysosrs breaker is display-only, official `safeApiCall` has none)
- Current official equivalent: per-add-on accumulated emission (148); no progressive endpoint, no retry
- What already existed upstream: official `getStreamsFromAllAddons` loop, `mergeStreams` dedup, Direct Debrid presentation, add-on health (G1)
- What was imported: `client=nuvio-progressive` opt-in detection, `stream-progressive/<type>/<id>.ndjson` URL keeping the add-on's query, line-by-line NDJSON read with keep-alive/comment lines skipped, cumulative snapshots that replace the add-on's group, 180 s call budget, fallback to the ordinary JSON endpoint
- What was intentionally not imported: Cxsmo's rewrite of the add-on loop and stream screen, `R1_SPLIT` timing logs, prefetch/sweep engines, logging of full add-on URLs
- Local adaptations: runs inside official's add-on loop (`forkAwareStreamsFromAddon`), so health, inline-meta fallback and presentation stay official; snapshots pass official dedup and presentation before replacing the group; reads through official's `addonPermissive` client; leaving the screen cancels the blocking read; logs carry the host only; one retry after 750 ms for a 5xx/408/timeout whose first attempt took <= 8 s, never another 4xx, never twice
- Feature flag / fallback: FeatureId.STREAM_INTELLIGENCE (AUTO, D053) OFF = official request only, no progressive endpoint, no retry; any progressive failure = official request unchanged
- Tests ported/added: ProgressiveAioStreamsRulesTest (4), AddonStreamRetryTest (3), StreamRepositoryProgressiveTest (4)
- License / attribution notes: GPL-3.0 (Cxsmo is a GPL-3.0 fork of official); attributed in KDoc
- Resulting local commit: recorded in HANDOFF after merge
- Known risks / follow-up: needs an AIOStreams build that serves the Nuvio progressive endpoint; device checks MANUAL-PENDING (G14 campaign, HV-G8-1..HV-G8-3)

### G8b — One stream ranker, Best-quality autoplay mode and list order (155, 156, 158–164, 166, 168)
- Roadmap gate: G8
- Source repository: Cxsmo-ai/NuvioTV-Custom
- Source branch: main
- Pinned source SHA: 3e0d0fad60a2721adec133b88640b49c0183883f
- Source commit(s): tree at the pinned SHA (file-level diff)
- Source file(s): core/player/StreamQualityRank.kt (ranking chain, stable sort, "user list sort ignored" rule), core/debrid/TrashReleaseGroups.kt (TRaSH tier data)
- Import mode: ALGORITHM_PORT (ranking chain); FILE_PORT of the TRaSH data, regrouped by tier
- Current official equivalent: `DirectDebridStreamFilter` facts and sort criteria for the cached Direct Debrid list only; autoplay MANUAL / FIRST_STREAM / REGEX_MATCH
- What already existed upstream: official facts extraction (resolution, quality, HDR/DV, audio, channels, codec, release group, size), official autoplay candidate scoping and playability rules
- What was imported: fixed best-first chain over official facts, release-group quality tiers (TRaSH Remux / UHD / HD BluRay / WEB tiers; LQ, Bad Dual and Generated Dynamic HDR groups last), stable ties, fixed orders instead of the user's list sort criteria
- What was intentionally not imported: Cxsmo's changes to official defaults (maxResults 8, required resolutions, excluded AV1 / CAM / 3D, reordered audio and codec defaults, TRaSH exclusions in DebridStreamPreferences), its `factsFor` / exclusion-filter rewrite of `DirectDebridStreamFilter`, dropping excluded streams, `R1_SPLIT` timing logs, container score
- Local adaptations: cache tier first (156/157), DV ranked by the display's HDR capabilities (162), lossless-first audio (163), size as bitrate within one title (164), G1 add-on health as the last tiebreak (166); pure `StreamRankRules` with a per-load cached `StreamRanker.Session`; `StreamAutoPlayMode.BEST_QUALITY` in the official selector (same candidates and playability as the official modes; stream screen and next episode); per-profile "Sort streams by quality" list order in `fork_streams`, default off (167)
- Feature flag / fallback: FeatureId.STREAM_INTELLIGENCE OFF = no Best-quality option or list toggle, a stored BEST_QUALITY behaves as FIRST_STREAM, lists keep official order
- Tests ported/added: StreamRankRulesTest (8), StreamAutoPlayBestQualityTest (6)
- License / attribution notes: GPL-3.0 (Cxsmo is a GPL-3.0 fork of official); TRaSH Guides data MIT (Copyright (c) 2021 TRaSH); attributed in KDoc
- Resulting local commit: recorded in HANDOFF after merge
- Known risks / follow-up: ranking depends on official's text-based facts (unparsed names rank as unknown, never dropped); device checks MANUAL-PENDING (G14 campaign, HV-G8-4..HV-G8-6)

### G8c — Connection fit (165)
- Roadmap gate: G8
- Source repository: DavidVamaiotu/NuvioTV-Reshaped
- Source branch: subtitle-autosync
- Pinned source SHA: 0ccf049d2789600835f3f7a75423e9149ea416ba
- Source commit(s): tree at the pinned SHA (file-level diff)
- Source file(s): core/connection/StreamConnectionFit.kt, core/connection/ConnectionSpeed.kt (`ConnectionSpeedEstimator`, `DefaultNetworkObserver`, `PlaybackThroughputSampler`, `isInternetPlaybackSource`), core/connection/PlaybackThroughput.kt; the mpv `sampleThroughput` and Exo tick call sites
- Import mode: FILE_PORT
- Current official equivalent: none (official `StreamSpeedTester` is a manual diagnostic in network settings)
- What already existed upstream: official progress loop, HTTP data source factories, mpv property access
- What was imported: passive throughput learning per network kind (warm-up skip, 3–10 s windows, network-change drop, internet sources only), best of the last 3 samples with at least 2 and at most 14 days old, average bitrate from size ÷ runtime with plausibility bounds, demotion above connection ÷ 1.5, unknown bitrate kept in place, captured once per load
- What was intentionally not imported: Reshaped's separate "match streams to connection" setting (default on) and its per-group list partition, its settings status rows, `PlaybackConnectionEvents` network logging, the kotlinx-serialization sample format, Live TV hooks, its `StreamSpeedTester` rewrite
- Local adaptations: connection fit is a ranker key right after the cache tier (a heavy stream drops within its tier; an uncached stream never outranks a cached one), so it applies only with Best-quality autoplay or the opt-in ranked list order; pure `ConnectionFitRules` + Android `ConnectionSpeed.kt`; samples stored device-wide as `KIND:mbps:time` in `fork_connection_speed` (no URLs or hosts); byte counting wraps the progressive upstream and HLS/DASH factories once; next-episode autoplay uses the current episode's duration as runtime
- Feature flag / fallback: FeatureId.STREAM_INTELLIGENCE OFF = no byte counting, no sampling, no fit; no estimate yet or unknown runtime/size = order unchanged
- Tests ported/added: ConnectionFitRulesTest (9, new, over the ported rules and sampler), StreamConnectionFitRankingTest (2)
- License / attribution notes: GPL-3.0 (Reshaped is a GPL-3.0 fork of official); attributed in KDoc
- Resulting local commit: recorded in HANDOFF after merge
- Known risks / follow-up: the estimate needs two qualifying playbacks per network before it acts; device check MANUAL-PENDING (G14 campaign, HV-G8-7)

### G9a — Skip aggregator core on the official seam (117, 124–126, 131–135)
- Roadmap gate: G9
- Source repository: Cxsmo-ai/NuvioTV-Custom
- Source branch: main
- Pinned source SHA: 3e0d0fad60a2721adec133b88640b49c0183883f
- Source commit(s): tree at the pinned SHA (file-level diff)
- Source file(s): data/repository/SkipIntroRepository.kt (`fetchFromSkipMe`, `fetchFromTheIntroDb`, `fetchFromPublicMetaDb`, `mergeSkipIntervals`, `SkipMetadataParser.parseSkipMe` / `parseTheIntroDb` / `parsePublicMetaDb*` / `timeSeconds` / `parseClock`), data/local/SkipProviderCredentialsStore.kt
- Import mode: FILE_PORT (fetchers, parsers moved from org.json to Moshi, credential cipher); ALGORITHM_PORT (evidence merge)
- Current official equivalent: `SkipIntroRepository` with IntroDB, AniSkip and Anime-Skip in parallel and a one-per-category priority merge
- What already existed upstream: the official repository, player skip button, auto-skip categories, movie TMDB/MAL/Kitsu → IMDb resolution, post-credits guard
- What was imported: SkipMe.db / TheIntroDB / PublicMetaDB fetchers and parsers with Cxsmo confidences, 6 s per-provider timeout, 2 MiB response cap, confidence-weighted evidence merge, Keystore-encrypted per-profile provider keys
- What was intentionally not imported: Cxsmo's rewrite that drops official AniSkip / Anime-Skip and the Simkl anime mapping; its IntroDB app key; MovieHavenDB / VideoSkip / NotScare and the preview / content-warning categories (G9b); SkipMe retry loop; Cxsmo's `"?$$query"` / `"Bearer $$apiKey"` strings (sent a literal `$`; fixed)
- Local adaptations: fork providers run beside the unchanged official providers from inside official `SkipIntroRepository` and only when the user switched one on; official results keep their priority by confidence; one segment per official category; official post-credits guard reapplied after merging; with a fork provider on, official requests are also cancelled after 6 s; series under a non-IMDb id use the cached meta IMDb id; the Seekr key store now shares the same `KeystoreCipher` (same alias and format, stored keys unchanged); only `tt\d+` ids are sent; logs name the provider and status only
- Feature flag / fallback: FeatureId.DISCOVERY_SKIP_RECOMMENDATIONS (AUTO, D054) OFF = official skip exactly and no provider rows; no provider on = official path byte for byte
- Tests ported/added: SkipEvidenceMergeTest (7), SkipIntroForkProvidersTest (6), FeatureRegistryTest updated
- License / attribution notes: GPL-3.0 (Cxsmo is a GPL-3.0 fork of official); attributed in KDoc
- Resulting local commit: recorded in HANDOFF after merge
- Known risks / follow-up: provider response formats are ported from Cxsmo, not from provider docs; SkipMe answers only when the runtime is known at lookup time; device checks MANUAL-PENDING (G14 campaign, HV-G9-1, HV-G9-2)

### G9b — Previews, content warnings and mute segments (120–122, 127–129)
- Roadmap gate: G9
- Source repository: Cxsmo-ai/NuvioTV-Custom
- Source branch: main
- Pinned source SHA: 3e0d0fad60a2721adec133b88640b49c0183883f
- Source commit(s): tree at the pinned SHA (file-level diff)
- Source file(s): data/repository/SkipIntroRepository.kt (`fetchFromMovieHavenDb`, `fetchFromVideoSkip`, `fetchFromNotScare`, `slugifyNotScareTitle`, `SkipMetadataParser.parseMovieHaven` / `parseVideoSkip` / `parseNotScarePage` / `parseTimestamp` / `visibleHtmlText` / `decodeHtmlEntities` / `mapCategory`), data/local/PlayerSettingsDataStore.kt (`AutoSkipSegmentType` preview and content categories, as data only)
- Import mode: FILE_PORT (fetchers, parsers; MovieHavenDB moved from org.json to Moshi)
- Current official equivalent: none (official skip knows intro, recap, outro, movie credits, post-credits)
- What already existed upstream: official skip button, its labels and auto-skip categories; G9a aggregator
- What was imported: MovieHavenDB / VideoSkip / NotScare fetchers and parsers with Cxsmo confidences and caps, preview and content-warning categories, skip / mute / warn actions
- What was intentionally not imported: Cxsmo's `custom` category, auto-skip for the new categories, changes to official `AutoSkipSegmentType`, Cxsmo's severity field; Cxsmo never applied `mute` in its player
- Local adaptations: every new provider and category is off by default; content segments all stay (not one per category) and only offer the skip button; a mute segment is applied locally (ExoPlayer volume / mpv `mute`, separate from mpv volume used by amplification), never seeks and never shows the button; external players only receive plain official skip segments; official `SkipInterval` gains a defaulted `action`
- Feature flag / fallback: FeatureId.DISCOVERY_SKIP_RECOMMENDATIONS OFF = official skip exactly; no category on = no new segments
- Tests ported/added: SkipTextParsersTest (4), SkipEvidenceMergeTest (+3), SkipIntroForkProvidersTest (+2, 1 updated), SkipMuteRulesTest (3)
- License / attribution notes: GPL-3.0 (Cxsmo is a GPL-3.0 fork of official); attributed in KDoc
- Resulting local commit: recorded in HANDOFF after merge
- Known risks / follow-up: videoskip.herokuapp.com and notscare.me are blocked by this environment's proxy and the MovieHavenDB path returned 404 for a sampled id, so none of the three endpoints is verified here; they fail closed (no segments); device check MANUAL-PENDING (G14 campaign, HV-G9-3)

### G9c — Post-play sources, AUTO chain, paging and trailer fallback (136, 140–144, 146)
- Roadmap gate: G9
- Source repository: Cxsmo-ai/NuvioTV-Custom
- Source branch: main
- Pinned source SHA: 3e0d0fad60a2721adec133b88640b49c0183883f
- Source commit(s): tree at the pinned SHA (file-level diff)
- Source file(s): ui/screens/player/PostPlayRecommendationController.kt (`loadCandidates` source chain, `loadKuratoCandidates` / `loadBingeCatCandidates` / `fetch*CatalogPage`, `findKuratoAiCatalog` / `findBingeCatAiCatalog`, `buildKuratoRecommendationQuery` / `buildBingeCatRecommendationQuery`, `loadMdbListCandidates`, `resolveAddonTrailerSource` / `normalizeAddonTrailerUrl`, `RECOMMENDATION_PREFETCH_BEHIND/AHEAD`), data/local/PlayerSettingsDataStore.kt (`PostPlayRecommendationSource`)
- Import mode: FILE_PORT (catalog matching, prompts, trailer URL rules) + DELTA_PORT (source chain on the official controller) + REUSE (official MDBList watchlist sync, `CatalogRepository`, `TrailerService.getTrailerPlaybackSourceFromYouTubeUrl`)
- Current official equivalent: `PostPlayRecommendationController` with one More like this source (Trakt / TMDB / Simkl, TMDB fallback), 4 cards, one trailer lookup
- What already existed upstream: controller, timing, lazy per-card resolution, watched / unreleased filters, MDBList watchlist sync (`MdbListTrackingLibraryProvider`)
- What was imported: Kurato AI / BingeCat AI add-on catalog sources with Cxsmo prompts and catalog matching, MDBList watchlist as a source, AUTO chain order, longer lists with a prefetch window, add-on YouTube trailers
- What was intentionally not imported: Cxsmo's removal of official timing (`PostPlayRecommendationTiming` kept), Cxsmo's Simkl library-as-source (official `SimklRelatedService` kept), Cxsmo's own MDBList watchlist client (official sync reused), unbounded catalog paging, Cxsmo's extra meta fetch for trailers
- Local adaptations: source setting is per profile and defaults to official's chain (4 cards, identical behaviour); every fork source falls back to official's chain; catalogs stop after 3 pages or 60 items; fork sources show up to 20 cards; official lists (≤ 4) still resolve in full; add-on trailers are tried only after official's lookup finds none
- Feature flag / fallback: FeatureId.DISCOVERY_SKIP_RECOMMENDATIONS OFF = official post-play exactly (setting hidden, source official)
- Tests ported/added: PostPlaySourcesTest (6), PostPlayForkCatalogTest (3), AdaptiveResourcesTest (+1, 2 updated)
- License / attribution notes: GPL-3.0 (Cxsmo is a GPL-3.0 fork of official); attributed in KDoc
- Resulting local commit: recorded in HANDOFF after merge
- Known risks / follow-up: Kurato AI / BingeCat AI catalog ids and prompts are Cxsmo's, not verified against the add-ons from this environment; device check MANUAL-PENDING (G14 campaign, HV-G9-4)

### G9d — Shuffle season scope, all-watched fallback and Mystery mode (171, 173–177)
- Roadmap gate: G9
- Source repository: Cxsmo-ai/NuvioTV-Custom
- Source branch: main
- Pinned source SHA: 3e0d0fad60a2721adec133b88640b49c0183883f
- Source commit(s): tree at the pinned SHA (file-level diff)
- Source file(s): ui/screens/detail/RandomEpisodeDialog.kt (season pool, unwatched-only falling back to the whole pool, `mysteryMode`), ui/screens/stream/StreamScreenViewModel.kt / StreamScreen.kt (`mysteryMode` forcing the direct auto-play flow, header label), ui/screens/player/PlayerScreen.kt (`mysteryMode` labels)
- Import mode: DELTA_PORT onto official `EpisodeShuffle` / `EpisodeShuffleStore` / `EpisodeShuffleDialog` (behaviour from Cxsmo, structure official's)
- Current official equivalent: per-show episode shuffle (unwatched-only or all episodes) across detail, home and next episode, with an artwork-hidden preview option; an exhausted unwatched pool is empty by design
- What already existed upstream: shuffle picking, history, surfaces, "caught up" message and the manual include-watched path
- What was imported: season scope, all-watched fallback, Mystery mode (hide number, title, still, overview until playback), no stream list for a Mystery pick
- What was intentionally not imported: Cxsmo's separate one-shot `RandomEpisodeDialog` (official's persistent shuffle kept), the `mysteryMode` navigation arguments (the pick is recognised through official `EpisodeShuffle`), Cxsmo's automatic fallback (opt-in here: official's empty pool is its tested default), hiding metadata after playback starts
- Local adaptations: settings stored per show in official's `episode_shuffle` store (`season:`, `mystery:`, `fallback:` keys); the stream screen treats an episode as a Mystery pick only when a shuffle surface or the shuffle dialog picked it and the show's Mystery mode is on; Manual auto-play mode uses the G8b best-quality pick for it unless the user chose to pick by hand; the detail screen no longer pulls season or episode focus to a Mystery pick
- Feature flag / fallback: FeatureId.DISCOVERY_SKIP_RECOMMENDATIONS OFF = official shuffle exactly (stored fork keys ignored, options hidden)
- Tests ported/added: ShuffleRulesTest (3), EpisodeShuffleForkTest (5), EpisodeShuffleStoreTest (+1)
- License / attribution notes: GPL-3.0 (Cxsmo is a GPL-3.0 fork of official); attributed in KDoc
- Resulting local commit: recorded in HANDOFF after merge
- Known risks / follow-up: closeout review correction adds saved route context and neutral manual/failure source cards (CI pending); device check MANUAL-PENDING (G14 campaign, HV-G9-5)

### G9e — Calendar (178–187)
- Roadmap gate: G9
- Source repository: Cxsmo-ai/NuvioTV-Custom
- Source branch: main
- Pinned source SHA: 3e0d0fad60a2721adec133b88640b49c0183883f
- Source commit(s): tree at the pinned SHA (file-level diff)
- Source file(s): data/repository/CalendarRepositoryImpl.kt, domain/model/CalendarModels.kt, domain/repository/CalendarRepository.kt, ui/screens/calendar/CalendarViewModel.kt, ui/screens/calendar/CalendarScreen.kt, the `Screen.Calendar` route, NavHost entry and drawer item
- Import mode: FILE_PORT (repository, models, view model, screen) on official repositories (REUSE: WatchProgressRepository, LibraryRepository, TrackingProgressProviderRegistry, MetaRepository, parseEpisodeReleaseLocalDate)
- Current official equivalent: none (official has no calendar)
- What already existed upstream: every data source (progress, watched items, library, trackers, metadata add-ons) and the release-date parser
- What was imported: series discovery from progress / watched / library / trackers, the 30-day-back / 90-day-ahead window, day grouping, filters, Cxsmo's spoiler rule, watched-state-only refresh, the screen and its drawer entry
- What was intentionally not imported: Cxsmo's top-navigation insets (official has no top bar), its hard-coded English date labels (string resources, English and Arabic), start-up loading at app launch
- Local adaptations: pure rules in `fork/discovery/CalendarModels.kt`; at most the 80 most recently active series; add-on concurrency from `AdaptiveResources` (6 on standard devices); 15 s per series; loading starts on the first Calendar visit; logs carry counts and exception class names only; a spoiler's still is never requested
- Feature flag / fallback: FeatureId.DISCOVERY_SKIP_RECOMMENDATIONS OFF = no drawer entry, nothing loads
- Tests ported/added: CalendarRulesTest (5)
- License / attribution notes: GPL-3.0 (Cxsmo is a GPL-3.0 fork of official); attributed in KDoc
- Resulting local commit: recorded in HANDOFF after merge
- Known risks / follow-up: users with more than 80 active series see the 80 most recent; air dates are only as good as the metadata add-ons; device check MANUAL-PENDING (G14 campaign, HV-G9-6)

### G9f — App Dimmer (206, 207)
- Roadmap gate: G9
- Source repository: Cxsmo-ai/NuvioTV-Custom
- Source branch: main
- Pinned source SHA: 3e0d0fad60a2721adec133b88640b49c0183883f
- Source commit(s): tree at the pinned SHA (file-level diff)
- Source file(s): ui/components/AppDimmerOverlay.kt, MainActivity.kt (`LocalAppDimPercent`, overlay above the content), ui/components/NuvioDialog.kt (overlay inside dialogs), ui/screens/settings/ThemeSettingsScreen.kt (slider), ui/screens/player/PlayerScreen.kt (control button and `AppDimmerDialog`)
- Import mode: FILE_PORT (overlay) + DELTA_PORT (MainActivity, NuvioDialog, Appearance settings, player control)
- Current official equivalent: none (official dims only its screensaver)
- What already existed upstream: settings slider and single-choice dialog components, player control buttons
- What was imported: the overlay, its placement above all content and in dialog windows, the settings slider and a player control
- What was intentionally not imported: Cxsmo's storage in official `ThemeDataStore` (a fork store `fork_app_dimmer` keeps the official store untouched), Cxsmo's hard-coded "Off" label, its custom player dialog (official single-choice dialog with presets instead)
- Local adaptations: 10 % steps up to 90 %; per profile; the player picker state lives on `PlayerViewModel` so auto-hiding controls never close it
- Feature flag / fallback: FeatureId.DISCOVERY_SKIP_RECOMMENDATIONS OFF = no dimming, no settings row, no player button
- Tests ported/added: AppDimmerRulesTest (2)
- License / attribution notes: GPL-3.0 (Cxsmo is a GPL-3.0 fork of official); attributed in KDoc
- Resulting local commit: recorded in HANDOFF after merge
- Known risks / follow-up: closeout review correction adds the same draw layer to raw dialog windows through AppDimmedDialog (CI pending); device check MANUAL-PENDING (G14 campaign, HV-G9-7)


### G9 closeout review corrections — existing seams only
- Gate / IDs: G9; 125, 126, 131, 135, 174–177, 206, 207.
- Source: existing G9a/G9d/G9f ledger entries at unchanged Cxsmo pin 3e0d0fad60a2721adec133b88640b49c0183883f; no new external import.
- Mode: DELTA_PORT / local correctness fixes; retain official skip, shuffle, dialogs and playback owners.
- Delta: cancellation-aware bounded skip HTTP (official OkHttpMdbListEngine callback pattern reused), per-profile/non-secret credential cache revision, SavedStateHandle Mystery route context, neutral source cards preserving original playback objects, input-transparent draw layer in raw dialog/popup windows.
- Tests: SkipHttpCallTest (stalled headers/body, response cap, six-second provider isolation); SkipProviderDeadlineTest (concurrent deadlines, official-only fallback); SkipProviderSettingsTest (rotation/recreation/profile cache isolation); MysteryStreamContextTest (restoration, OFF, disable, neutral presentation). Existing dimmer rules retained.
- Storage: optional credential_revision string defaults empty for existing prefs; encrypted key write and revision change are atomic. No official keys renamed/deleted. Route context persists through Android saved state only.
- Fallback: DISCOVERY_SKIP_RECOMMENDATIONS OFF keeps official behavior; stored optional state retained. No new endpoints/dependencies/permissions; keys are never in cache keys, UI or logs.
- License: existing GPL-3.0 attribution retained; corrections are local glue, not a new fork source.
- Resulting commit / CI: recorded in correction PR/HANDOFF before closeout; PR #61, awaiting exact-head CI.
- Hardware: HV-G9-1..HV-G9-7 stay MANUAL-PENDING for G14, including raw-dialog coverage and restored/manual Mystery paths.
- Follow-up correction (review of #61): with a fork provider active, the official Simkl id and episode-mapping lookups behind AniSkip / Anime-Skip now start at once and share the six-second deadline, each lookup keeping what it found; official `SimklIdResolver` GETs and its redirect probe use the same cancellation-aware call (`readSkipBody` / `readHeaderCancellable`), so the deadline stops them. Test: SkipIntroSimklDeadlineTest (2). Official-only path unchanged in order and results.

### G10a — Live TV sources, readers, encrypted storage and repository (208–212, 233)
- Roadmap gate: G10
- Source repository: DavidVamaiotu/NuvioTV-Reshaped
- Source branch: main
- Pinned source SHA: 0ccf049d2789600835f3f7a75423e9149ea416ba
- Source commit(s): tree at the pinned SHA (file-level diff)
- Source file(s): reshaped/livetv/LiveTvModels.kt, LiveTvPlaylistParser.kt, LiveTvProviders.kt, LiveTvHttp.kt, LiveTvStorage.kt (source records), LiveTvRepository.kt (sources and loads); app/src/test/.../LiveTvPlaylistParserTest.kt (parser cases)
- Import mode: FILE_PORT (models, M3U parser, Xtream / Stalker readers, per-source load jobs); ADAPTER (storage, HTTP, repository lifecycle)
- Current official equivalent: none (official has live playback plumbing only: type `channel`, live latch, live-window retry)
- What already existed upstream: `LivePlaybackUiPolicy`, `PlayerRuntimeControllerErrorRecovery`, `KeystoreCipher` (G9a), `ProfileDataStoreFactory`
- What was imported: line-streamed M3U parser (EXTINF attributes, EXTVLCOPT / EXTHTTP / Kodi headers, duplicates and category rows dropped, HLS or direct link as one channel); Xtream categories / live streams with the panel's own links and TS-unless-HLS-only; Stalker handshake, profile, per-portal session renewed once, all-channels or four-at-a-time ordered pages with one retry and an incomplete flag, per-play links, logo paths; multiple sources with per-source jobs (two at once), same-identity replace, a failing source keeping its channels, idle release after 5 min
- What was intentionally not imported: plaintext SharedPreferences for sources, passwords and MACs; favorites / last channel by stream URL; `Log.w` with raw exceptions; the process-wide `object` with an `Int` profile; `android.util.JsonReader` / `org.json`; legacy single-source migration (no earlier Nuvio data); EPG, organisation, screens, preview, player and QR setup (G10b–G10g)
- Local adaptations: Hilt singleton bound to `ProfileManager.activeProfileId`; the whole source list AES-GCM encrypted per profile with `KeystoreCipher` in the `fork_live_tv` DataStore, imported playlists app-private per profile; channel identity is a hash of source, category and name (`liveTvChannelKey`), never the link; Moshi streaming readers; own OkHttp client (the app client logs URLs in debug builds) with call cancellation on coroutine cancellation and a 1 MB cap for API text; logs name the host only; Stalker Cookie / Authorization attached only to streams on the portal host; `{"js":[...]}` answers read as data (Reshaped skipped them, losing Stalker genres); 100 000-channel cap per source; sources and channels published in one state update; secrets kept out of `toString`
- Feature flag / fallback: FeatureId.LIVE_TV (AUTO, D055); nothing loads until a Live TV surface asks (G10b adds it behind a menu entry that is off by default); OFF = nothing loads
- Tests ported/added: LiveTvPlaylistParserTest (7), LiveTvProvidersTest (8), LiveTvSourceCodecTest (2), LiveTvRepositoryTest (8), FeatureRegistryTest updated
- License / attribution notes: GPL-3.0 (Reshaped is a GPL-3.0 fork of official); attributed in KDoc
- Resulting local commit: recorded in HANDOFF after merge
- Known risks / follow-up: provider formats are ported from Reshaped, not from provider documentation; real providers and the portal-host rule for Stalker headers need device coverage (G14, with the G10b screen)

### G10b — Live TV screen, menu entry, organisation and playback as `channel` (208, 219–225)
- Roadmap gate: G10
- Source repository: DavidVamaiotu/NuvioTV-Reshaped
- Source branch: main
- Pinned source SHA: 0ccf049d2789600835f3f7a75423e9149ea416ba
- Source commit(s): tree at the pinned SHA (file-level diff)
- Source file(s): ui/reshaped/livetv/LiveTvScreen.kt, LiveTvScreenModel.kt, LiveTvComponents.kt, LiveTvSourceDialog.kt, LiveTvCategoryDialog.kt, LiveTvNavigation.kt; reshaped/livetv/LiveTvRepository.kt (favorites, hiding, order, names, last channel), LiveTvStorage.kt (the same choices); ui/screens/settings/LiveTvSettingsItems.kt; res/values/live_tv_strings.xml
- Import mode: FILE_PORT (screen, components, dialogs, strings); ALGORITHM_PORT (category order, shown channels, filters); ADAPTER (navigation, settings, player route)
- Current official equivalent: none; the player route and `LivePlaybackUiPolicy` (type `channel`: live timeline, no progress) are REUSED
- What already existed upstream: `Screen.Player.createRoute`, the drawer (`DrawerItem`, `rootRoutes`), `SettingsToggleRow`, `NuvioDialog`, theme tokens
- What was imported: category column that follows focus, All / Favorites / per-source filters, remote-safe search and text fields, channel rows with logos decoded at drawn size, last-channel row, source dialog (M3U / Xtream / Stalker, two-press remove, per-source errors), category dialog (show / hide / hide all, move with hold-OK + ▲▼, A–Z, rename, per-channel hiding), favorites by hold-OK, off-main-thread filtering with the filtered list kept across the player round trip, focus restore on return
- What was intentionally not imported: favorites / hidden / last channel keyed by stream URL (now `liveTvChannelKey`); the process-wide `LiveTvPreferences` object (now a per-profile DataStore value); the recent-channel stand-in built from a stored link (the last channel now resolves only while a source still lists it); the preview, guide, programme lines and player overlay (G10c, G10e, G10f); the QR phone setup column (G10g); Reshaped's pill-navigation hooks
- Local adaptations: the menu switch lives in Layout → sidebar settings, per profile, off by default, hidden while LIVE_TV is OFF; turning it off while on the screen goes to Home; the choices are one JSON value per profile (channel keys as hex, category names) beside the encrypted sources; choice changes apply to the state at once (a rename field reads them back) and are saved in order on one writer; the zapping list is refiltered off the caller's thread; passwords typed in the source dialog are not kept in saved instance state; the player route carries type `channel` and the source's headers (Stalker session headers only for portal-host streams, G10a)
- Feature flag / fallback: FeatureId.LIVE_TV (AUTO, D055); menu entry off until the user enables it; OFF removes the setting, the entry and every load
- Tests ported/added: LiveTvOrganisationTest (7: order, filters, stale filters, moves, library codec without links, choices published / saved per profile, menu switch and OFF); LiveTvRepositoryTest fake extended
- License / attribution notes: GPL-3.0 (Reshaped is a GPL-3.0 fork of official); attributed in KDoc
- Resulting local commit: recorded in HANDOFF after merge
- Known risks / follow-up: TV focus and long-press behavior need device checks (HV-G10-1..HV-G10-3, G14); logos from the guide join in G10c

### G10c — Live TV guide: now, next, progress, time left, guide logos (214–218, 225)
- Roadmap gate: G10
- Source repository: DavidVamaiotu/NuvioTV-Reshaped
- Source branch: main
- Pinned source SHA: 0ccf049d2789600835f3f7a75423e9149ea416ba
- Source commit(s): tree at the pinned SHA (file-level diff)
- Source file(s): reshaped/livetv/LiveTvEpg.kt, LiveTvGuideCache.kt, LiveTvHttp.kt (`download`, `readFile`), LiveTvRepository.kt (guide region); ui/reshaped/livetv/LiveTvComponents.kt (minute clock, progress bar, time left), LiveTvScreen.kt (programme lines); app/src/test/.../LiveTvPlaylistParserTest.kt (guide cases)
- Import mode: FILE_PORT (XMLTV reader, schedule builder, name keys, guide cache, guide lifecycle, UI pieces)
- Current official equivalent: none
- What already existed upstream: AdaptiveResources tiers (G2, D042)
- What was imported: XMLTV pull reading with flat memory, id-then-name matching (country tags / quality words ignored), bounded past / ahead window, repeated titles shared, kept-programme cache keyed by guides / channels / window, download to gzip (fast compression) with the old copy kept until complete, 10 h re-download, re-read when a cut channel runs out, 30 min retry, each guide published as soon as read, now / next / progress / time left, guide logos for channels without one
- What was intentionally not imported: `LiveTvDevice` (its low-memory line, under 2.5 GB, is AdaptiveResources' constrained tier, so that owner decides the window); the full-screen guide grid and the Now/Next card (G10e)
- Local adaptations: the pull parser and file access go through `LiveTvGuideFiles` (Android's `Xml.newPullParser()` in `LiveTvGuideStore`, fakes in tests); downloads use Live TV's own client with call cancellation and a 120 s read timeout; guide failures are logged by host only (Xtream's guide URL carries the login); guide files are named by a hash of the URL, never the URL
- Feature flag / fallback: FeatureId.LIVE_TV (AUTO, D055); no guide link means no guide work at all; the guide only runs while Live TV is shown
- Tests ported/added: LiveTvEpgTest (8: Reshaped's read-again, name-key and id/name/window cases; timestamps with and without offsets; programme on now; window per tier; kept-programme cache validity; repository publishing now / next / guide logos with a fake guide seam; a failing download leaves the list without programmes)
- Review correction: a child cancellation monitor closes pending/stalled OkHttp calls immediately; unique partial files prevent old-generation cleanup from deleting a new download; failed rename preserves the old guide. Cancelled sockets preserve CancellationException via ensureActive. LiveTvHttpTest adds real HTTP cancellation before headers, during body read, and gzip replacement (3 cases).
- License / attribution notes: GPL-3.0 (Reshaped is a GPL-3.0 fork of official); attributed in KDoc
- Resulting local commit: recorded in HANDOFF after merge
- Known risks / follow-up: the XML reader itself runs on Android's parser, so real guides are a device check (HV-G10-4); guide memory on 1–2 GB boxes is MANUAL-PENDING (G14)

#### G10c review hardening (PR #65)
- Import mode: WRITE adapters/corrections around the attributed FILE_PORT; source pin unchanged.
- Per-source guide URL/channel association and source-scoped result keys retain raw XMLTV-id-first matching; duplicate-id variants keep all names and any missing-logo requirement.
- Partial/invalid-tail reads carry `complete=false`: retain parsed programmes, skip complete-cache writes, re-download at the failure retry. Guide workers use visibility `collectLatest` so the last collector leaving cancels network/parse IO immediately; source completion refreshes preserved guides even when every reload fails.
- SHA-256 URL filenames and digest cache v2 include guide-to-source-key mapping (old cache is invalidated once). URLs/credentials remain absent from logs and filenames.
- AdaptiveResources supplies compressed/disk and inflated byte caps (64/256 MiB constrained, 128/512 MiB standard) and a 5-minute total download timeout; 120-second idle timeout remains. Streaming limits reject chunked oversize or gzip expansion before old-file replacement. No app dependency added.
- Added/adapted tests: duplicate IDs/aliases/logos; source namespaces and per-guide requests; hash-collision pair Aa/BB; parser incomplete-tail signal; immediate cancellation when invisible; partial display without complete caching and 30-minute re-download/recovery; failed-source Refresh; chunked/expanded oversize preserving old file; total call timeout; resource-tier budgets. Local standalone JVM harness: 85 tests PASS, Android seams stubbed; exact-head Android/fullDebug CI required.
- Feature 216 stays in_progress: next-programme API exists but display is G10e. HV-G10-4 remains MANUAL-PENDING for G14.

### G10d — Live-only playback rules and live-aware AFR (233–236)
- Roadmap gate: G10
- Source repository: DavidVamaiotu/NuvioTV-Reshaped
- Source branch: main
- Pinned source SHA: 0ccf049d2789600835f3f7a75423e9149ea416ba
- Source commit(s): tree at the pinned SHA (file-level diff)
- Source file(s): reshaped/livetv/LiveTvPlaybackRegistry.kt, LiveTvLoadErrors.kt, LiveTvTsFlags.kt, LiveEdgeRecovery.kt; ui/reshaped/livetv/LiveTvFrameRate.kt; the hooks in PlayerMediaSourceFactory, PlayerRuntimeControllerInitialization, PlayerRuntimeControllerAfrPreflight, PlayerRuntimeControllerTracks, PlayerRuntimeControllerAutomaticSubtitleSync and PlaybackThroughput
- Import mode: FILE_PORT (registry, retry schedule, TS flag, live-edge rejoin, frame meter); ALGORITHM_PORT (median frame rate); ADAPTER (live branch on the G5e AFR owner)
- Current official equivalent: `LivePlaybackUiPolicy`, BEHIND_LIVE_WINDOW / HLS probe-and-reinit in `PlayerRuntimeControllerErrorRecovery`, G4 dead-source failover, the AFR preflight
- What already existed upstream: all of the above stay in place and still run for Live TV once the live-only step gives up
- What was imported: the playback registry (memory only, latest 16 URLs, list entry by channel key); HTTP refusals retried at 0.7 / 1.4 / 2.1 s for Live TV loads; FLAG_ALLOW_NON_IDR_KEYFRAMES for Live TV TS; live-edge rejoin (seekToDefaultPosition + prepare, three per minute); no VOD disk cache, speed learning, seek preview or AutoSync for Live TV; AFR preflight skipped for Live TV; frame rate measured from 48 frame timestamps (median gap) for tracks that report none
- What was intentionally not imported: Reshaped's separate AFR switcher (`LiveTvFrameRateMatch` / `matchDisplayToLiveTrack`): the decision stays with G5e `TrackAfrPolicy`, which gains `liveTv` and `RUN_LIVE` (switch once without a hold when the rate came after playback started); `keepPlayerForLiveTvZap` (G10e, with zapping); Reshaped's `SeekReadAhead` hook (that component does not exist here)
- Local adaptations: decisions are plain functions (`LiveTvPlaybackRules`) with the media3 glue in `LiveTvPlaybackHooks` and the controller glue in `PlayerRuntimeControllerLiveTv`; official files get one-line, registry-gated hooks; the frame meter is per stream generation (weak map), so no official field is added; the live display switch is not cached (the rate belongs to what is on now); the registry keys URLs to channel keys, not list links
- Feature flag / fallback: FeatureId.LIVE_TV (AUTO, D055); a URL Live TV did not register takes every official path unchanged; OFF means nothing is ever registered
- Tests ported/added: LiveTvPlaybackTest (5: registry scope and bound, retry schedule, rejoin limit, median frame rate with dropped frames / jumps / NTSC / implausible rates), TrackAfrPolicyTest (+1: live branch)
- License / attribution notes: GPL-3.0 (Reshaped is a GPL-3.0 fork of official); attributed in KDoc
- Resulting local commit: recorded in HANDOFF after merge
- Known risks / follow-up: display switching and TS start-up need real channels and a display (HV-G10-5, G14); a mid-stream mode switch blanks the picture briefly, accepted for live only

### G10e — Live TV inside the player: zapping, channel list, categories, Now/Next card (216, 228–232)
- Roadmap gate: G10
- Source repository: DavidVamaiotu/NuvioTV-Reshaped
- Source branch: main
- Pinned source SHA: 0ccf049d2789600835f3f7a75423e9149ea416ba
- Source commit(s): tree at the pinned SHA (file-level diff)
- Source file(s): ui/reshaped/livetv/LiveTvPlayerOverlay.kt (LiveTvPlayerState, banner, info card, channel panel, folder column), ui/reshaped/livetv/LiveTvStreamDetails.kt, the zap hooks in PlayerScreen and PlayerRuntimeControllerStreams (`keepPlayerForLiveTvZap`), LiveTvRepository zap list
- Import mode: FILE_PORT (player state, overlay composables, stream details, keep-player zap); ALGORITHM_PORT (wrap-around neighbour); ADAPTER (Hilt entry point instead of a player view-model field)
- Current official equivalent: none (official has no channel concept in the player); official `switchToSourceStream` reuses an existing ExoPlayer when one is kept
- What already existed upstream: the player key handler, controls, stream-info data (`buildStreamInfoData`), source switching
- What was imported: ▲▼ on the bare picture and CH+/CH- switch channel inside the list it was picked from (wrapping, 350 ms settle so quick presses land once), the ExoPlayer kept across Live TV zaps, a banner after each switch, ◀ channel list with now / progress / time left and its categories column, OK Now/Next card with picture and sound details and the next programme (216), ▶ opens the controls; strings en + ar
- What was intentionally not imported: Reshaped's full-screen guide grid (no feature in scope; the guide shows on every row) and its `▶ Guide` hint; any URL-keyed state (zap list and current channel are channel keys)
- Local adaptations: the state lives beside the official player through `rememberLiveTvPlayer` (Hilt `LiveTvPlayerEntryPoint`, no new view model); every key returns false unless the playing URL is in the Live TV playback registry, so VOD keys, panels and dialogs are unchanged; overlay collects the repository only while a channel plays; four one-line hooks in PlayerScreen and one in PlayerRuntimeControllerStreams
- Feature flag / fallback: FeatureId.LIVE_TV (AUTO, D055); with nothing registered the overlay and keys are inert; mpv engine and non-Live-TV switches release the player as before
- Tests ported/added: LiveTvOrganisationTest (+2: neighbour wraps both ways, falls back to the first channel; empty list)
- License / attribution notes: GPL-3.0 (Reshaped is a GPL-3.0 fork of official); attributed in KDoc
- Resulting local commit: recorded in HANDOFF after merge
- Known risks / follow-up: key routing, focus and zap timing need a remote and real channels (HV-G10-6, G14)

### G10f — Live TV channel preview, sized by AdaptiveResources (226, 227)
- Roadmap gate: G10
- Source repository: DavidVamaiotu/NuvioTV-Reshaped
- Source branch: main
- Pinned source SHA: 0ccf049d2789600835f3f7a75423e9149ea416ba
- Source commit(s): tree at the pinned SHA (file-level diff)
- Source file(s): ui/reshaped/livetv/LiveTvPreview.kt (LiveTvPreviewPlayer, LiveTvPreviewPanel, rememberLiveTvPreviewPlayer), the preview parts of ui/reshaped/livetv/LiveTvScreen.kt, reshaped/livetv/LiveTvPreferences.kt (previews, preview sound), reshaped/livetv/LiveTvDevice.kt (low-memory line), ui/screens/settings/LiveTvSettingsItems.kt (preview rows)
- Import mode: FILE_PORT (preview player, panel, screen wiring, settings rows); ADAPTER (settings per profile in the Live TV DataStore beside the menu switch; size, buffers and default from AdaptiveResources)
- Current official equivalent: none (official has no channel list)
- What already existed upstream: `PlayerMediaSourceFactory.normalizePlaybackRequest` (reused so a user:password@ link becomes a header as in the player)
- What was imported: one preview ExoPlayer for the focused channel after 400 ms (first channel at once), lowest adaptive bitrate, capped size, a few seconds of buffer, HLS retry for links without .m3u8, sound faded in only when on, a mute button under the picture, released on play / leaving / background; per-profile Channel previews and Preview sound settings; strings en + ar
- What was intentionally not imported: Reshaped's `LiveTvDevice` second memory check (AdaptiveResources owns the tier: `liveTvPreviewBudget`, constrained = Reshaped's under-2.5 GB line) and its app-wide SharedPreferences (settings are per profile); the Guide button under the picture (no guide grid, G10e)
- Local adaptations: previews default off on low-RAM boxes (D055 audit); the preview uses Live TV's own OkHttp client (no logging interceptor) with no disk cache; Stalker channels are not previewed (links made per play); nothing logs URLs or errors
- Feature flag / fallback: FeatureId.LIVE_TV (AUTO, D055); LIVE_TV OFF resolves previews off; previews off shows the list as before
- Tests ported/added: LiveTvPreviewTest (3: device default until chosen, per profile, sound; LIVE_TV off; Stalker and HLS rules), AdaptiveResourcesTest (+1: preview budget per tier)
- License / attribution notes: GPL-3.0 (Reshaped is a GPL-3.0 fork of official); attributed in KDoc
- Resulting local commit: recorded in HANDOFF after merge
- Known risks / follow-up: a second decoder beside the UI on 1-2 GB boxes and one-connection accounts need real devices (HV-G10-7, G14)

### G10g — Live TV set up from a phone via QR (213)
- Roadmap gate: G10
- Source repository: DavidVamaiotu/NuvioTV-Reshaped
- Source branch: main
- Pinned source SHA: 0ccf049d2789600835f3f7a75423e9149ea416ba
- Source commit(s): tree at the pinned SHA (file-level diff)
- Source file(s): reshaped/livetv/LiveTvSetupServer.kt (server and phone page), the QR column and server lifecycle of ui/reshaped/livetv/LiveTvSourceDialog.kt, `LiveTvRepository.importPlaylist`
- Import mode: FILE_PORT (server, page, dialog column, playlist import); ALGORITHM_PORT (token, Origin and size checks as `LiveTvSetupPolicy` functions)
- Current official equivalent: the add-on and G6b font LAN pages (`core/server` `DeviceIpAddress`, `QrCodeGenerator`, NanoHTTPD) — reused, not duplicated
- What already existed upstream: NanoHTTPD, `DeviceIpAddress`, `QrCodeGenerator`; G10a `LiveTvSourceStore.savePlaylist` (bounded, temp file then rename)
- What was imported: a per-session 128-bit token path page (M3U link or file, Xtream, Stalker) beside the add-source form, Origin check, 16 KiB form and 64 MiB playlist caps checked before reading, bounded body reads, file name stripped of folders, the playlist saved under its source and loaded like a new one; page language and direction follow the app; strings en + ar
- What was intentionally not imported: Reshaped's org.json form parsing (Moshi's streaming reader keeps it testable on the JVM) and its singleton repository calls (the dialog hands the profile-bound repository to the server)
- Local adaptations: the server runs only while the sources dialog is open and the app is in the foreground (a restart is a new token); credentials arrive only in the POST body and go to the encrypted per-profile store; nothing is logged; JSON responses never echo input
- Feature flag / fallback: FeatureId.LIVE_TV (AUTO, D055); no network or no free port shows the reason instead of a QR code and the form still works
- Tests ported/added: LiveTvSetupPolicyTest (5: token and Origin, size caps, form parsing and validation, file names, repository import of a file and refusal of an oversized one)
- License / attribution notes: GPL-3.0 (Reshaped is a GPL-3.0 fork of official); attributed in KDoc
- Resulting local commit: recorded in HANDOFF after merge
- Known risks / follow-up: phone browsers, LAN isolation on guest Wi-Fi and large files need real devices (HV-G10-8, G14)

### G11a — Watch Party core: protocol, sync engine, share policy, VDO.Ninja transport (237–249, UI in G11b)
- Roadmap gate: G11
- Source repository: AntoninoScardina/NuvioTV
- Source branch: watchparty
- Pinned source SHA: ff597b12bc7834b07dedda92c88e0d574c0aad81
- Source commit(s): 58530279d10f3378c237c248ce3ca7bdde5e13cd (the whole feature)
- Source file(s): watchparty/WatchPartyProtocol.kt, WatchPartySession.kt, WatchPartyTransport.kt, WebViewWatchPartyTransport.kt, WatchPartyModule.kt; assets/watchparty/watchparty-bridge.js
- Third-party file: VDO.Ninja SDK v1.6.1 by Steve Seguin (MPL-2.0) from its upstream npm release `@vdoninja/sdk@1.6.1` (github.com/steveseguin/ninjasdk), Source Code Form `vdoninja-sdk.js` (SHA-256 88f623ac8b6dcf2ab66b4bddc70b66f1a2d7d04198f143a6efe1b0d4c89c440c), unmodified, with LICENSE-vdoninja-sdk.txt; the fork's minified copy is byte-identical to the same release
- Import mode: FILE_PORT (protocol, session / sync engine, transport, bridge, module); local WRITE (WatchPartySharePolicy)
- Current official equivalent: none (no Watch Party in official 56aaba2 or dev 5c1d9b0); the player controls it drives exist and are reused in G11b
- What already existed upstream: `PlayerRuntimeController` position / play intent / seek / pause / pitch-kept speed / duration
- What was imported: protocol v1 (unchanged wire format, alphabet, room and password derivation, for the phone build), host-authority sync with CMD / STATE every 2 s and on change, polling detection of local actions, soft speed correction (±10 %) and hard seek with a learned lead, settle / suppress windows, the hidden WebView data-channel transport and bridge
- What was intentionally not imported: unconditional sharing of the open link with every header; WebView console logging and raw error text; `String.random()` codes; the fork's nightly-release workflow, README and screenshots
- Local adaptations: room creation requires the host's consent and MEDIA is never sent without it; WatchPartySharePolicy (header allowlist; credential headers, user-info links, torrents, loopback and Live TV not shareable; received media validated and bounded); SecureRandom codes; fixed error codes; no console logging, no file / content access and no navigation in the WebView; scope and clock injectable for tests; WATCH_PARTY AUTO (D056)
- Feature flag / fallback: FeatureId.WATCH_PARTY (AUTO); no entry point exists until G11b, and OFF will hide every entry and never create a WebView
- Tests ported/added: WatchPartySharePolicyTest (5: codes and room derivation, header allowlist and credentials, unshareable sources, received media, no-print / names), WatchPartySessionTest (6: consent gate, received media, soft drift and hard seek, host follows CMD and guests follow pause, guest CMD, lifecycle and stale transport); FeatureRegistryTest updated (D056)
- License / attribution notes: Antonino port GPL-3.0 (fork of official); SDK MPL-2.0, file-level, recorded in LICENSE_AND_ATTRIBUTION; the SDK license grants no right to VDO.Ninja's hosted signaling / STUN / TURN (their terms apply) and no trademark use
- Resulting local commit: recorded in HANDOFF after merge
- Known risks / follow-up: availability and terms of the hosted signaling / TURN service; WebView WebRTC support on TV boxes; NAT traversal and phone interop are device checks (G14)

### G11b — Watch Party UI: player button, consent, room panel, join, guest playback (237–244 UI)
- Roadmap gate: G11
- Source repository: AntoninoScardina/NuvioTV
- Source branch: watchparty
- Pinned source SHA: ff597b12bc7834b07dedda92c88e0d574c0aad81
- Source commit(s): 58530279d10f3378c237c248ce3ca7bdde5e13cd
- Source file(s): ui/screens/player/PlayerWatchParty.kt (player adapter, binding, panel, badge), ui/screens/watchparty/WatchPartyNavigationEffect.kt, WatchPartyJoinScreen.kt, the PlayerScreen / NuvioNavHost / Settings hooks
- Import mode: FILE_PORT (player adapter, binding loop, panel, badge, navigation effect); ADAPTER (join as a Playback settings row with a code dialog instead of a new settings category and route); local WRITE (consent step, unshareable reasons, received-stream registry)
- Current official equivalent: none; reuses `PlayerOverlayScaffold`, `DialogButton`, `ControlButton`, `ForkTextEntryDialog`, `Screen.Player.createRoute`
- What already existed upstream: the player controls and route
- What was imported: Watch Party button in the player controls, panel (create, code, participants, leave), badge, guest navigation to the host's stream, joining by code; strings en + ar
- What was intentionally not imported: a new Settings category and route (a Playback settings row instead); Italian strings
- Local adaptations: two-step consent naming what joiners receive; unshareable reasons (torrent / local, Live TV, credentials) from WatchPartySharePolicy and G10's playback registry; a guest's received link is registered in memory (`WatchPartyReceivedStreams`) and `streamCacheKey` returns null for it, so it is never saved for reuse; panel state as a one-line `PlayerViewModel` flow (as G9f's dimmer); everything hidden while WATCH_PARTY is OFF
- Feature flag / fallback: FeatureId.WATCH_PARTY (AUTO, D056); OFF: no button, no settings row, no navigation effect, no WebView
- Tests ported/added: WatchPartySharePolicyTest +1 (received links remembered in memory, bounded); consent gate already in WatchPartySessionTest
- License / attribution notes: GPL-3.0 (Antonino fork of official); attributed in KDoc
- Resulting local commit: recorded in HANDOFF after merge
- Known risks / follow-up: two-device sync, phone interop, NAT / TURN and logs need real devices (HV-G11-1..HV-G11-4, G14)

### G11 review corrections — Watch Party after the #71–#73 reviews (237–249)
- Roadmap gate: G11
- Source repository: AntoninoScardina/NuvioTV (unchanged pin ff597b12bc7834b07dedda92c88e0d574c0aad81); compatibility reference AntoninoScardina/NuvioMobile `watchparty` @ ff7a16b19e7777cfce9254792cef9a141651dfe4 (tag v2026.929.8; read only, nothing imported)
- Source commit(s): none new
- Source file(s): none new; changes are local to `fork/watchparty`, `PlayerWatchParty.kt`, `WatchPartyUi.kt`, the bridge script and two one-line hooks
- Import mode: local WRITE (corrections of G11a / G11b code)
- Current official equivalent: none
- What already existed upstream: `PlayerNavigationArgs` reads the route from `SavedStateHandle`; `setLastPlaybackDiagnostics` saves the last link and headers; `PlayerEvent.OnPlayPause` bookkeeping
- What was imported: nothing
- What was intentionally not imported: no revalidation of HTTP redirects inside the player's data source (a public link that redirects into the guest's network is a known residual risk, recorded in the closeout)
- Local adaptations: the guest's player route carries a one-time in-memory ticket instead of the link and headers (`PlayerNavigationArgs` hook redeems it; leaving drops unredeemed tickets; a recreated process opens nothing); the last-playback diagnostics never save a received link (`setLastPlaybackDiagnostics` hook); loopback, private, carrier-grade NAT, link-local, unique-local, multicast and reserved addresses, LAN-only names and non-dotted numeric hosts are never shared or opened, and a guest opens a name only when every address it resolves to is public; a guest follows one host (the first to speak as host) until it leaves, and the host leaving clears its state and corrections; the host's speed travels as an optional `rate` field (omitted at 1×, ignored by older builds) and guests correct around it, getting their own speed back afterwards; terminal signaling loss (`reconnectFailed`, a non-retried disconnect) ends the room with the connection error; remote play / pause does the play / pause button's mpv bookkeeping (progress, watch progress, scrobbles); a stream that becomes unshareable is detached from the room (the panel says so); streams without a known duration attach once loaded; a guest whose player cannot open the host's link, or whose host sent a refused link, sees why (IP-locked / local sources)
- Feature flag / fallback: FeatureId.WATCH_PARTY (AUTO, D056) unchanged
- Tests ported/added: WatchPartySharePolicyTest +3 (non-public hosts, DNS answers, tickets), WatchPartySessionTest +4 (host pinning, host leaving, speed follow and restore with v1 wire at 1×, unsupported stream)
- License / attribution notes: GPL-3.0, unchanged; the SDK file is unchanged
- Resulting local commit: recorded in HANDOFF after merge
- Known risks / follow-up: redirect and DNS-rebinding paths inside the player; the controller-level pause bookkeeping and Android saved-state behaviour compile only in CI and need device checks (HV-G11-3)

### G12a — UI styles: top menu (bar / pill), clock and profile access (196, 198, 204, 205)
- Roadmap gate: G12
- Source repository: xnucade/NuvioGlass (`dev` @ 84098b7de222fb599e30d12f7a6f5b9b1e347fee, commit 09ade4547a071f2182c0d4df762cf695bb85a4b5); Cxsmo-ai/NuvioTV-Custom (`main` @ 3e0d0fad60a2721adec133b88640b49c0183883f, `TopNavigation.kt`); DavidVamaiotu/NuvioTV-Reshaped (`subtitle-autosync` @ 0ccf049d2789600835f3f7a75423e9149ea416ba, `ui/reshaped/pillnav`)
- Source file(s): NuvioGlass `GlassScaffold.kt`, `ui/components/glass/GlassNavPill.kt`, `GlassClockPill.kt`; Cxsmo `TopNavigation.kt` (profile access); Reshaped `PillNavScaffold.kt` / `PillNavigationBar.kt` (pill menu replacing the sidebar)
- Import mode: FILE_PORT (NuvioGlass scaffold, sliding-indicator menu, minute-tick clock); ALGORITHM_PORT (Cxsmo top-bar profile access, Reshaped pill look) on the same scaffold; local WRITE (UiStyleRules, UiStyleSettings)
- Current official equivalent: official legacy and modern sidebars (kept as the default); `NuvioTopBar` is unused
- What already existed upstream: `DrawerItem`, `navigateToDrawerRoute`, `LocalContentFocusRequester`, the sidebar profile row and `ProfileAvatarCircle`
- What was imported: a top menu with one indicator sliding between destinations, a clock that redraws on the minute, a profile button, Back / long-press Back that behave like the sidebar's
- What was intentionally not imported: NuvioGlass Calendar tab and fork identity; Reshaped app-wide SharedPreferences flag and its AGSL lens (liquid glass lands with the overlaid Glass chrome, G12b); a second or third top bar
- Local adaptations: one `TopChromeScaffold` with BAR and PILL looks instead of three bars; the menu sits above the content (no auto-hide over a hero), so focus traversal reaches it with Up from the top row; per-profile choice in `fork_ui_style` (Navigation style: Sidebar default / Top bar / Pill; clock switch); official navigation through `navigateToDrawerRoute` (made internal) and raw icons through the settings design system's internal `rememberRawSvgPainter`; RTL-safe indicator (absolute positions); UI_STYLES AUTO (D058)
- Feature flag / fallback: FeatureId.UI_STYLES (AUTO); OFF hides the setting and always shows official's sidebar; an unknown stored style is the sidebar
- Tests ported/added: UiStyleRulesTest (4: default and flag-off fallback, style cycle, Back contract, clock format and minute tick); FeatureRegistryTest updated (UI_STYLES AUTO; the isolation example now uses MAT_AUDIO)
- License / attribution notes: GPL-3.0 forks of official; attributed in KDoc
- Resulting local commit: recorded in HANDOFF after merge
- Known risks / follow-up: D-pad focus between menu and every root screen, RTL and the clock on real TVs are device checks (HV-G12-1, HV-G12-2)

### G12b — UI styles: Glass chrome, blur or flat, lightweight effects (193, 197, 200)
- Roadmap gate: G12
- Source repository: xnucade/NuvioGlass (`dev` @ 84098b7de222fb599e30d12f7a6f5b9b1e347fee, commit 09ade4547a071f2182c0d4df762cf695bb85a4b5)
- Source file(s): `GlassScaffold.kt`, `ui/components/glass/GlassSurface.kt` (incl. `LocalGlassChromeReveal`), `ui/theme/GlassTokens.kt`, the `ModernHomeRowsList.kt` first-row Up hook
- Import mode: FILE_PORT (scaffold, surface, tokens, reveal hook); local WRITE (effect policy in UiStyleRules, Lightweight effects setting)
- Current official equivalent: Modern home with full-bleed hero and the modern sidebar's Haze blur (API gate only)
- What already existed upstream: Haze 1.7.2, `hideBuiltInHeaders`, `navigateToDrawerRoute`, `LocalContentFocusRequester`, the Modern rows key handler
- What was imported: chrome kept in composition and moved off-screen with a graphicsLayer so focus traversal still reaches it only from the top row; auto-hide on Home (3.5 s on arrival, 0.25 s once used); Up from the first row reveals and focuses it; content dimmed while it has focus; scrim; glass surface (blur 24 dp at input scale 0.66, tint 0.28 live / 0.82 flat, bevelled hairline)
- What was intentionally not imported: `HomeLayout.GLASS` and the `usesModernPipeline` edits across official layout code (D060: Glass is a navigation style instead); NuvioGlass fork identity, Calendar tab, glass badges and per-card glass; the API-only blur gate
- Local adaptations: Glass is a per-profile Navigation style (Pill with Classic / Grid); the menu, clock and profile button are the G12a parts on glass pills; blur or flat chosen by `UiStyleRules.glassEffect` (API 31, AdaptiveResources tier, Lightweight effects); Back follows the shared top-menu contract (first Back focuses the chrome, then exits) instead of NuvioGlass's hide-on-Back; the long-press Back handler is shared with the G12a scaffold
- Feature flag / fallback: FeatureId.UI_STYLES (AUTO); OFF shows official's sidebar and hides the settings; the reveal hook returns false outside Glass, so the Modern rows behave as official
- Tests ported/added: UiStyleRulesTest (+3: Glass falls back to Pill off Modern and to Sidebar with the flag off; blur only on API 31+ with a known non-LOW_RAM tier and Lightweight effects off; hide delays)
- License / attribution notes: GPL-3.0 fork of official; attributed in KDoc
- Resulting local commit: recorded in HANDOFF after merge
- Known risks / follow-up: blur cost on 2 GB boxes and over the hero trailer, D-pad reveal and RTL on real TVs are device checks (HV-G12-3); Cinematic Glass (194) and liquid glass (199) next

### G12c — UI styles: Cinematic Glass and AGSL liquid glass (194, 199)
- Roadmap gate: G12
- Source repository: DavidVamaiotu/NuvioTV-Reshaped (`subtitle-autosync` @ 0ccf049d2789600835f3f7a75423e9149ea416ba, `ui/reshaped/pillnav`); xnucade/NuvioGlass (`dev` @ 84098b7de222fb599e30d12f7a6f5b9b1e347fee) for the Glass chrome it draws on; Cinema View ideas from legacy PR #1 (reference only, no code)
- Source file(s): Reshaped `PillGlassShader.kt` (AGSL, verbatim), `PillGlassBackdrop.kt` (backdrop recorder), the `LiquidPillGlass` draw in `PillNavigationBar.kt`
- Import mode: FILE_PORT (shader, recorder, lens draw); local WRITE (Cinematic Glass style, LIQUID effect level, the `LocalCinematicGlass` hook)
- Current official equivalent: Modern full-screen hero backdrop setting (`modern_hero_full_screen_backdrop`) and hero trailer; Haze blur on the modern sidebar
- What already existed upstream: Compose `GraphicsLayer` recording, `RenderEffect.createRuntimeShaderEffect` (Android 13), the Modern hero's full-screen mode
- What was imported: the capsule lens (circular rim profile, ordered dispersion, vibrancy, specular rim lit from the top left), a display-list backdrop recorded once per content frame and replayed under each pill, redraw every frame for 1.5 s after a key press and once a second when idle
- What was intentionally not imported: Reshaped's own pill bar and app-wide SharedPreferences flag; its second RAM probe (`pillLensSupported`); the sliding item lens (the Glass menu keeps its indicator); forcing official's hero trailer on; legacy PR #1 code
- Local adaptations: LIQUID is a third `GlassEffect` level chosen by `UiStyleRules.glassEffect` (API 33 + AdaptiveResources STANDARD tier + Lightweight effects off); the lens is a Modifier under both Glass pills; the recorder runs only while the chrome is shown and is cleared when it hides; Cinematic Glass is a Navigation style that shows the full-screen hero backdrop through a CompositionLocal without writing the official preference (D061)
- Feature flag / fallback: FeatureId.UI_STYLES (AUTO); OFF shows official's sidebar; LocalCinematicGlass is false outside Cinematic Glass, so the Modern home follows its own setting
- Tests ported/added: UiStyleRulesTest (+1: liquid glass only on API 33+ STANDARD tier without Lightweight effects; Cinematic Glass cycle, fallback to Pill off Modern and to Sidebar with the flag off, isGlass)
- License / attribution notes: GPL-3.0 fork of official; the shader keeps Reshaped's notes and is attributed in KDoc
- Resulting local commit: recorded in HANDOFF after merge
- Known risks / follow-up: AGSL cost and visual quality on real Android 13+ boxes, video holes under the lens, the full-screen backdrop on 1080p vs 4K TVs are device checks (HV-G12-4)

### G12d — UI styles: optional screensaver (292)
- Roadmap gate: G12
- Source repository: Cxsmo-ai/NuvioTV-Custom (`main` @ 3e0d0fad60a2721adec133b88640b49c0183883f)
- Source file(s): `core/player/ScreensaverController.kt`, `ui/components/ScreensaverOverlay.kt`, the ticker / `dispatchKeyEvent` / `onWindowFocusChanged` hooks in `MainActivity.kt`, the playback-active collector in `PlayerViewModel.kt`, `TrailerPlayerPool.setScreensaverSuppressed`, the Appearance settings rows
- Import mode: ALGORITHM_PORT (controller as a pure `ScreensaverMachine` with an injected clock, key swallowing moved into it); FILE_PORT (overlay)
- Current official equivalent: none (the App Dimmer from G9f is a constant level, not an idle screensaver)
- What already existed upstream: `TrailerPlayerPool`, the player's `isPlaying` / `isBuffering` state, the activity's `dispatchKeyEvent`
- What was imported: idle check once a second while started; dims after the timeout with no input; blocked while playing or buffering, woken by resume, clock restarted by pause; waits while a dialog window has focus; the waking press (down, repeats, up) is swallowed; trailers stopped on engage and not started while it shows; 1.4 s fade in, 0.25 s fade out
- What was intentionally not imported: Cxsmo's on-by-default; its ThemeDataStore keys (the settings live per profile in `fork_ui_style`); the suppression flag inside the trailer pool (the pool asks the machine)
- Local adaptations: off by default, per profile, hidden while UI_STYLES is OFF; options start after 1 / 2 / 5 / 10 / 15 / 30 min and darkness 50 / 70 / 85 % with unknown stored values falling back to 5 min / 70 %; the overlay is drawn above the App Dimmer; one `Screensaver` instance shared by the hooks (D062)
- Feature flag / fallback: FeatureId.UI_STYLES (AUTO); OFF or the profile switch off means the overlay never shows and every key passes through unchanged
- Tests ported/added: ScreensaverMachineTest (6: timeout, input restarts the clock, waking press swallowed to its release, never during playback and pause restarts the clock, waits while a dialog has focus, options and fallbacks)
- License / attribution notes: GPL-3.0 fork of official; attributed in KDoc
- Resulting local commit: recorded in HANDOFF after merge
- Known risks / follow-up: remotes that send only key-up or long-press codes, MediaSession resumes from the system, and OLED retention are device checks (HV-G12-5)

### G13a — AI media provider host core and the experimental opt-in (255, 256, 259–261; 250, 257 core)
- Roadmap gate: G13
- Source repository: Fornace/nuvio-ai (`dev` @ 518af7120c82d014c02f7717f4c9c6e396eb215c)
- Source file(s): `app/src/main/java/com/nuvio/tv/core/media/provider/host/` `ExternalProviderContract.kt`, `HostCrypto.kt`, `ProviderArtifactVerifier.kt`, `ProviderContractValidator.kt`, `ProviderPackageScanner.kt`, `ProviderRegistryModels.kt`, `VendorCatalogModels.kt`; `security/` `CipherTextStore.kt`, `InstallIdentity.kt`, `KeystoreBridge.kt`, `ProfileGenerationStore.kt`, `ProviderCredentialVault.kt`, `ProviderSecret.kt`; their tests
- Import mode: FILE_PORT (package renamed to `fork/aimedia`; `AndroidKeystoreBridge` split from the bridge interface so the pure code is JVM-testable); local WRITE (ExperimentalOptIn, ExperimentalOptInStore, the registry seam)
- Current official equivalent: none
- What already existed upstream: `kotlinx.serialization`, the feature registry's override seam (unused until now)
- What was imported: registry and vendor-catalog models with their safety checks, constant-time SHA-256 artifact check, exact signer-set contract validation, AAD-bound per-profile credential vault, install identity and profile generations
- What was intentionally not imported: the capability grants, transform-provider registry, fake subtitle provider and cue / dub player pieces (engine-time; 251–254 deferred, D063); Android clients, installer and UI (G13b); Fornace fork identity
- Local adaptations: one `fork/aimedia` owner; nothing is constructed at runtime yet; experimental groups turn on only through `ExperimentalOptIn` (device-wide store read in `NuvioApplication.onCreate` before any registry read; only experimental group names are accepted)
- Feature flag / fallback: FeatureId.AI_MEDIA (experimental, OFF unless opted in); the opt-in default is empty, so every registry read is unchanged
- Tests ported/added: ProviderArtifactVerifierTest, ProviderContractValidatorTest, ProviderRegistryModelsTest, VendorCatalogModelsTest, ProviderVersionComparisonTest, ProviderCredentialVaultTest (including a bytecode scan that the security classes never reference android.util.Log), InstallIdentityTest, ProfileGenerationStoreTest (ported); ExperimentalOptInTest (4, new)
- License / attribution notes: GPL-3.0 fork of official; each ported file names its source path
- Resulting local commit: recorded in HANDOFF after merge
- Known risks / follow-up: the Keystore bridge and the opt-in store run only on Android (CI compiles them; device checks HV-G13-1)
