# G2 audit — Adaptive Resource Manager

Evidence for `tasks/G2_ADAPTIVE_RESOURCE_MANAGER.md`. Official = `71632b9` (integration `3273f5a`,
official head observed `7d3cea0`, which touches none of the files below).
Source = hackerslash/NuvioTV-Lite `dev` @ `2afdcd05d45e27afd48fb83ef9db6c286216a44c` (GPL-3.0,
LICENSE identical to official). Lite is based on an older official tree (it lacks Simkl,
episode shuffle, the G1 health tracker and official's prefetch-depth budget), so files are never
copied whole: only the resource delta is ported. Classes follow PORTING_PROTOCOL §3.

G2 ships as sequential PRs from `feat/adaptive-resource-manager`:
G2a tier core + fan-out/image/post-play policies, G2b playback allocation safety
(buffer/parallel/chunk, 266–268, plus the stale `NuvioExoPlayerPerformanceHelperTest` debt),
G2c bounded caches, stream-list/bidi work and the MPV cache (274–280).

## Owners found
- Official physical RAM: `NuvioExoPlayerPerformanceHelper.getDevicePhysicalRamBytes`
  (ActivityManager, `/proc/meminfo` fallback) and `NuvioApplication.newImageLoader`
  (ActivityManager `totalMem` for the cache share). Nothing official classifies a device tier
  from it; `MemoryBudget.isLowRamTier` is heap-based (`maxHeapMb < 512`), which `largeHeap`
  defeats on 2 GB boxes.
- Lite `core/device/DeviceMemoryTier`: two cuts (≤1600 MB low-RAM comfort, ≤2560 MB constrained
  safety), `isLowRamDevice` wins, unknown RAM counts as low-RAM, `dropsOptionalWork` is
  `liteMode || isLowRam`. Lite caps strong devices at 8 stream fetches.

## Tier core + consumers (G2a)
| ID | Behavior | Class | Result |
|---|---|---|---|
| 262 | Adaptive Resource Manager | ISOLATED_FEATURE | `fork/resource/AdaptiveResources`: one `AdaptiveResourcePolicy` installed in `NuvioApplication.onCreate` before Hilt injection; `ADAPTIVE_RESOURCE_MANAGER` AUTO (D042); OFF or not installed = official values |
| 263 | Detect physical RAM | ALREADY_OFFICIAL | official ActivityManager `totalMem` path (helper + image loader); the installer reads the same source plus `isLowRamDevice` |
| 264 | Low-RAM tier | missing delta | ALGORITHM_PORT of Lite cut ≤1600 MB, `isLowRamDevice`, unknown/0 RAM → LOW_RAM |
| 265 | Constrained tier | missing delta | ALGORITHM_PORT of Lite cut ≤2560 MB; LOW_RAM implies constrained |
| 269 | Bound add-on request concurrency | missing delta | stream + subtitle add-on fetches: 3 low-RAM, 6 constrained, **unbounded on standard** (official; Lite's 8 not imported). Health latency still starts after the permit |
| 270 | Smaller poster cache on weak devices | PARTIAL_OVERLAP | official 0.15/0.20/0.25 kept; low-RAM clamps to 0.08 (never raises) |
| 271 | RGB565 only when required | PARTIAL_OVERLAP | official user toggle kept; forced on low-RAM only |
| 272 | Reduce animated posters | missing delta | animated decoders not registered on low-RAM (stills); SWR revalidation dropped on low-RAM; decode parallelism 4 → 2 on low-RAM |
| 273 | Reduce post-play prefetch | missing delta | low-RAM resolves only the card on screen; official on-demand resolution (`awaitCandidateResolution`) pages the rest in |

Also from Lite: home catalog load concurrency 3 → 2 on low-RAM only (eager row count unchanged).

Not imported: `liteMode` edition switches (Sentry, TV channel sync, disk cache 100 MB, hidden
RGB565 row), `MemoryDiagnostics`, Lite's reverts of official features (Simkl, shuffle,
custom-poster screens, health tracker), Lite's `MAX_CATALOG_LOAD_CONCURRENCY = 4`, and the
cap of 8 fetches on strong devices.

Strong-device invariant: on `MemoryTier.STANDARD` every policy value equals the official input
(`AdaptiveResourcesTest.standardTierKeepsEveryOfficialValue`), and low-RAM cuts are `min()`
clamps that never raise an official value.
