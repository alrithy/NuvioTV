# G1 audit — Unified Diagnostics & Add-on Health

Evidence for `tasks/G1_UNIFIED_DIAGNOSTICS.md`. Official = `71632b9` (integration `a909827`).
Source = ysosrs123/NuvioTV-Fork `nuvio-test` @ `45e0984c18460d2a65c5d745999011b4314328eb`.
Classes follow PORTING_PROTOCOL §3. G1 ships as sequential PRs from `feat/unified-diagnostics`:
G1a add-on health, G1b diagnostics model in the official overlay, G1c device assessment.

## Owners found
- Official HUD owner: `ui/screens/player/PlayerDebugStatsOverlay.kt` (cpu, paging, memory,
  buffer, track bitrate, network rx, dropped frames, thermal/throttle).
- Official info panel: `StreamInfoOverlay.kt` + `StreamInfoData` (video codec/size/fps/bitrate,
  audio codec/channels/sample rate, subtitle, engine).
- ysosrs ships a second HUD `PlaybackStatsOverlay.kt` beside the official one → not imported
  (one diagnostics UI owner). Its metrics are ported into the official owner in G1b.
- ysosrs `core/health/*` has no official equivalent. ysosrs has no tests for health/assessment.

## Add-on health (G1a)
| ID | Behavior | Class | Result |
|---|---|---|---|
| 281 | Add-on Health system | ISOLATED_FEATURE | ALGORITHM_PORT: outcome vocabulary + 8 s slow threshold from ysosrs `AddonHealthModel`; in-memory `fork/diagnostics/AddonHealthTracker` (ysosrs persisted per profile; not needed) |
| 282–287 | Healthy / Slow / Timeout / Auth / Manifest / No-streams | missing delta | ysosrs only had HEALTHY/DEGRADED/DOWN; states classified from existing request results |
| 288 | Temporary manifest failure never hides an add-on | ALREADY_OFFICIAL | `AddonRepositoryImpl` keeps cached manifests on failed refresh and emits a placeholder on first-fetch failure |
| 289 | Retry policy | not in source | ysosrs breaker only changes the displayed level; official has no addon retry. Deferred to G8 (D041) |
| 290 | Failing add-on isolated | ALREADY_OFFICIAL | `StreamRepositoryImpl` runs each add-on in its own coroutine with its own catch; covered by `StreamRepositoryAddonHealthTest` |
| 291 | Add-on Health screen | PARTIAL_OVERLAP | Health badge in official Addon Manager cards; no second screen |

Not imported: persistence/DataStore, catalog/meta/subtitle/resolver recording, breaker cooldown,
`addonHealthEnabled` layout setting.

## Diagnostics HUD (G1b)
Owner stays official `PlayerDebugStatsOverlay`; rows are appended after the official rows while
UNIFIED_DIAGNOSTICS is not OFF. Formatting is pure (`fork/diagnostics/PlaybackHud.kt`).
| ID | Behavior | Class | Result |
|---|---|---|---|
| 61 | Unified HUD | PARTIAL_OVERLAP | one HUD: official overlay + G1b rows; ysosrs second HUD not imported |
| 62, 64 | video codec, source fps | ALREADY_OFFICIAL (StreamInfoData) | now also in HUD `video` row from `ExoPlayer.videoFormat` |
| 63 | actual video bitrate | ALREADY_OFFICIAL | official `bitrate` row = file size / duration (real average), tracks fallback |
| 65 | display refresh | missing delta | `display` row from `View.display.refreshRate`; warns when not a multiple of fps |
| 66 | HDR / DV profile | missing delta | `hdr` row: DV profile from codecs (`dvhe.08`), else PQ/HLG/SDR from ColorInfo; HDR10+ not claimed |
| 67, 68 | audio codec, Atmos / DTS:X | missing delta | `audio` row; Atmos only from E-AC3-JOC/`ec+3`, DTS:X from `vnd.dts.uhd;profile=p2`; TrueHD Atmos not signalled so not claimed |
| 69 | real passthrough | missing delta | `output` row from `AnalyticsListener.onAudioTrackInitialized` AudioTrack encoding |
| 70, 72, 75, 78, 80 | throughput, buffer, dropped, RAM/player buffer, thermal | ALREADY_OFFICIAL | official rows kept unchanged |
| 71 | required vs available | missing delta | `need` row: file/track bitrate vs bandwidth estimate, warns under 1.2x headroom |
| 73 | connections / chunk | PARTIAL_OVERLAP | official ParallelRangeDataSource HUD fields extended with connections + chunk size |
| 74, 76 | rebuffers, underruns | PARTIAL_OVERLAP | official counters now displayed (`rebuffer`, `underrun`, `load err`) |
| 77 | audio clock jitter | ALGORITHM_PORT (reduced) | `a-clock` drift ms/s at 1 Hz from player position vs wall clock; ysosrs 20 ms sink sampler not ported (G5 owns the sink) |
| 79 | SoC / CPU | PARTIAL_OVERLAP | official `cpu` + `soc` row (`Build.SOC_MODEL`, API 31+) |
| 81 | current strategy | missing (pre-G3) | `strategy` row = `official`, the only strategy until G3 |

Not imported: ysosrs `PlaybackStatsOverlay`, logcat capture instrumentation, PlaybackByteCounter,
mux-rate estimator, sink jitter sampler, rate-limit/hedge rows (G4 network owner).

## Remaining G1 scope (initial classification, refined in G1b/G1c)
| IDs | Behavior | Initial class |
|---|---|---|
| 61 | Unified HUD | official owner + ysosrs delta; no second HUD |
| 62, 64, 67 | video codec, source fps, audio codec | ALREADY_OFFICIAL in StreamInfoData; not yet in HUD |
| 63, 70, 72, 75, 78, 80 | bitrate, throughput, buffer, dropped, RAM, thermal | ALREADY_OFFICIAL in HUD (verify semantics) |
| 65, 66, 68, 69, 71, 73, 74, 76, 77, 79 | refresh, HDR/DV, Atmos/DTS:X, passthrough, required vs available, connections/chunk, rebuffers, underruns, jitter, SoC | missing delta from ysosrs `PlaybackStatsOverlay` samplers |
| 81 | Current playback strategy | depends on G3 strategy engine → unavailable until G3 |
| 14–16 | Device assessment + apply/revert | ISOLATED_FEATURE (`core/assessment/*`, `DeviceAssessmentScreen`) |
