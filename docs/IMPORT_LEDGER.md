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
