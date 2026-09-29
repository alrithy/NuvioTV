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

## Playback allocation safety (G2b)
Official owners: `MemoryBudget` (heap ratio 0.65/0.85, 210 MB reserve, 16 MB chunk cap on its
low tier, DV7 back-buffer and conversion ratio), `PlayerMediaSourceFactory` (session connections,
`prefetchDepthChunks` floored at 2 chunks per connection), `ParallelRangeDataSource` session chunk
cap, native safe limits in `NuvioExoPlayerPerformanceHelper` (unknown 200, <1.15 GB 100,
<2.3 GB 200). Official's low tier is heap-based (`maxHeapMb < 512`), so a 2 GB box with
`largeHeap` (512 MB) is treated as high-RAM: 0.85 ratio, 128 MB chunks.

| ID | Behavior | Class | Result |
|---|---|---|---|
| 266 | Buffer size by RAM tier | PARTIAL_OVERLAP | official heap tier kept; `MemoryBudget.isLowRamTier` also true when constrained (only adds safety); Java-heap budget ceiling 250 MB on constrained (Lite `LOW_RAM_BUFFER_CEILING_MB`); floor unchanged |
| 267 | Parallel connections by RAM tier | missing delta | session connections capped at 4 (official non-performance maximum) on constrained; performance mode's 16 is kept on standard. Stored setting and UI unchanged |
| 268 | Chunk size by RAM tier | ALREADY_OFFICIAL mechanism, extended | official 16 MB `tierMaxChunkMb` now also applies to constrained devices through `isLowRamTier` |

Not imported: Lite's `bufferCount = connections * 2` (official moved to `+ 2` and a prefetch-depth
budget), removal of official `prefetchDepthChunks` / large-target-buffer override, Lite's
budget-fit chunk clamp in performance mode (official tier cap is kept as the single chunk
ceiling), and any change to stored settings.

Baseline debt in this subsystem: `NuvioExoPlayerPerformanceHelperTest` default/1 GB/2 GB cases
expected 250/150/250; official `2a22a6f22` ("lower the native memory tiers and seed the target
below them") changed the values to 200/100/200 on purpose without updating the test. The
expectations are corrected; the three debt entries are removed only after a full-suite report
shows them passing and a confirming run (BASELINE_TEST_DEBT).

## Caches, list work and remaining rows (G2c)
Lite bounds 20 in-memory caches with `lruCacheMap` for every device. Official uses unbounded
`ConcurrentHashMap`/`mutableMapOf` with TTL-only expiry. Every official use of the 18 caches below
is get/put/remove/clear (no iteration, no `compute*`), so an LRU map is a drop-in replacement.

| ID | Behavior | Class | Result |
|---|---|---|---|
| 274 | Bounded metadata caches | missing delta | `AdaptiveResourcePolicy.boundedCache(official, n)`: official map on standard, LRU on constrained with Lite's sizes. TMDB metadata ×8 (48), TMDB id maps ×2 (128), Simkl ids/episodes/anime seasons (48), parental guide (48), Trakt related (48), SWR `revalidatedAt` (512) |
| 275 | Bounded rating cache | missing delta | same helper: MDBList ratings (512, under the loader's lock), IMDb episode ratings (32) |
| 276 | Bounded offline sync queue | not in source | no offline sync queue exists in official `71632b9` or Lite `2afdcd05` (only in-flight maps and a Trakt rate-limit window); nothing to bound. Deferred; re-audit when a queue is introduced |
| 277 | Fewer stream-list recompositions | missing delta | DELTA_PORT Lite `c760295eb`: player source side panel observes the last visible row through `snapshotFlow` instead of reading it in composition (same pagination trigger) |
| 278 | Less bidi/RTL work | missing delta | DELTA_PORT Lite `d491bd09e`/`c760295eb`: remembered `directedFor` / `rememberContentTextDirection` in official `TextDirectionUtils` (official emoji-aware scan unchanged); applied on stream rows, source chip, content card, hero description, search dropdown |
| 279 | Memory-safe Seekr | out of scope | task packet: no Seekr implementation in G2; deferred to G7 |
| 280 | Low-RAM MPV cache | not in source | Lite @ `2afdcd05` keeps official's 64 MB forward + 64 MB back demuxer cache; no measured low-RAM value to port. Deferred to G3 (Low Memory strategy owns engine buffers) |

Not imported: `SkipIntroRepository` caches (owned by the single Skip aggregator gate), TMDB
collection image semaphore (fan-out outside 269), Lite's older emoji-unaware direction scan, and
the remaining official `copy(textDirection = …)` sites outside the list/focus hot paths.
Strong-device invariant holds: `boundedCache` returns the official map instance on standard
(`AdaptiveResourcesTest.boundedCacheKeepsTheOfficialMapOnStandardDevices`); 277/278 are
output-identical (same direction, same pagination threshold).
