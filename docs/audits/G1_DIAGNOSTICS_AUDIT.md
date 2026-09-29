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

## Remaining G1 scope (initial classification, refined in G1b/G1c)
| IDs | Behavior | Initial class |
|---|---|---|
| 61 | Unified HUD | official owner + ysosrs delta; no second HUD |
| 62, 64, 67 | video codec, source fps, audio codec | ALREADY_OFFICIAL in StreamInfoData; not yet in HUD |
| 63, 70, 72, 75, 78, 80 | bitrate, throughput, buffer, dropped, RAM, thermal | ALREADY_OFFICIAL in HUD (verify semantics) |
| 65, 66, 68, 69, 71, 73, 74, 76, 77, 79 | refresh, HDR/DV, Atmos/DTS:X, passthrough, required vs available, connections/chunk, rebuffers, underruns, jitter, SoC | missing delta from ysosrs `PlaybackStatsOverlay` samplers |
| 81 | Current playback strategy | depends on G3 strategy engine → unavailable until G3 |
| 14–16 | Device assessment + apply/revert | ISOLATED_FEATURE (`core/assessment/*`, `DeviceAssessmentScreen`) |
