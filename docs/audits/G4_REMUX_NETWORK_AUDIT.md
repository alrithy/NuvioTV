# G4 audit — REMUX / Network Performance

Evidence for `tasks/G4_REMUX_NETWORK.md`. Official = `71632b9` (integration `c004af8`; official
head `7d3cea0`). Sources: ysosrs123/NuvioTV-Fork `nuvio-test` @ `45e0984c18460d2a65c5d745999011b4314328eb`,
DavidVamaiotu/NuvioTV-Reshaped `subtitle-autosync` @ `0ccf049d2789600835f3f7a75423e9149ea416ba`
(SOURCE_MAP pins). This is the mandatory audit only; no G4 code is changed yet.

## File-level comparison
| File | Official | ysosrs | Reshaped |
|---|---|---|---|
| `ParallelRangeDataSource.kt` | 1836 lines | 2404 (+661/−93 vs official, same function set) | 1401 (older session model: playhead window, tail chunks, pinned side chunks) |
| `PlayerMediaSourceFactory.kt` | — | 1015 | 1211 |
| `PlayerRuntimeControllerErrorRecovery.kt` | — | +311/−27 vs official | — |

Official already carries most of the ysosrs network work: companion-level chunk sessions reused
across instances, warm-session reopen, bootstrap window, rate-limit (429/503) backoff with jitter
and depth cap, body-rate stall watchdog with hedged restart (CDN 256 KB/s, Usenet 128 KB/s), prefetch
depth from the native budget, native (off-heap) allocator in performance mode, and the stream test
on the last played URL. The ysosrs delta is refinements inside those functions (S1 bounded reopen
GET, S1b 256 KB bootstrap window, nt5 reader-blocked chunk escalation, nt7 in-flight downloads
surviving instance teardown, zero-progress read guard, two-window Usenet stall rule), each with
device measurements in its comments, plus player-level dead-source failover and startup failover.

## Features
| ID | Behavior | Class | Result / plan |
|---|---|---|---|
| 4 | Parallel range downloading | ALREADY_OFFICIAL | official `ParallelRangeDataSource`; selected per session by the G3 REMUX strategy |
| 5 | Adaptive connection count | not in source | neither ysosrs nor Reshaped adapts the count at runtime (Reshaped `StreamConnectionFit` ranks sources, G8); G3 Auto + G2 policy choose it per session. Candidate: G4 slice after A/B data |
| 6 | Adaptive chunk size | not in source | as 5; official tier cap + budget-fit chunk (G2b) only |
| 7 | Deep buffering for 4K REMUX | PARTIAL_OVERLAP | official performance mode (400 MB native target) + managed budget; G3 REMUX uses it |
| 8 | Avoid re-downloading fetched data | PARTIAL_OVERLAP | official sessions retain completed chunks; ysosrs nt7 also keeps in-flight downloads alive across instances → G4b |
| 9 | CDN throttle recovery | PARTIAL_OVERLAP | official rate-limit backoff + depth cap; ysosrs refinements → G4b |
| 10 | HTTP 429/503 handling | ALREADY_OFFICIAL | official `downloadChunkWithRateLimitBackoff` (3 retries, 500 ms base, jitter, 3 s cap) |
| 11 | Stalled connection reopen | ALREADY_OFFICIAL (+ delta) | official `StalledChunkException` hedged restart; ysosrs nt5 reader-blocked escalation for hung requests → G4b |
| 12 | Off-heap/network buffers | ALREADY_OFFICIAL | official native allocator (`DefaultAllocatorNative`) in performance mode |
| 13 | Speed test on the actual stream | ALREADY_OFFICIAL | official `StreamSpeedTester` runs baseline / 1 / 4 / 8-connection tests on the last played URL |
| 17 | Pre-resolve stream | not found | no pre-resolve in either pinned source (ysosrs `StreamPrefetchCache` is stream-list prefetch, G8) |
| 18 | Warm connection before playback | PARTIAL_OVERLAP | official warm-session reopen and shared connection pool; no pre-playback warm-up in sources |
| 19 | Source failover mid-playback | missing delta | ysosrs `attemptDeadSourceFailover` / `advanceToNextLiveSource` (max 3, total recovery budget 5) → G4a |
| 20 | Startup watchdog | missing delta | ysosrs startup watchdog + `attemptStartupExhaustedSourceFailover` → G4a |
| 21–23 | Malformed / truncated MKV, nested SeekHead | audit pending | official and ysosrs both carry forked `dvmkv` extractors; per-file diff needed with fixtures → G4c |
| 24 | Non-faststart MP4 seek | PARTIAL_OVERLAP | official MP4 scatter-read session mode (see `MemoryBudget` notes); ysosrs S1 excludes MP4 → G4c |
| 25 | Disk seek buffer | missing delta | Reshaped seek buffer; official has the VOD disk cache (used by G3 Seek optimized). Must stay in Seek optimized, never stacked with parallel REMUX (D006) → G4d |
| 26 | Adaptive MPV demux cache | not in source | both sources keep official 64 MB + 64 MB (same as 280) |

## Slice plan
- G4a: player-level dead-source failover + startup watchdog (19, 20); unit-testable decision logic.
- G4b: ysosrs `ParallelRangeDataSource` refinements (8, 9, 11), opt-in through the G3 REMUX strategy.
- G4c: container recovery (21–24) with malformed/non-faststart fixtures.
- G4d: Reshaped disk seek buffer (25) inside Seek optimized only.

## Blocker for the G4 exit
GATE_SPECS G4 requires same-file/source A/B (startup, throughput, rebuffer, waste, RAM, seek) on
real devices. This environment has no Android device or SDK, so every G4 slice can be built and
unit-tested in CI but its exit evidence is MANUAL-PENDING until someone runs the A/B.

## G4a result
Ported (see IMPORT_LEDGER G4a): dead-source failover for 404/410 and non-media bodies, mid-play
malformed/IO failover after official same-URL retries, startup-exhausted failover, 3-failover cap,
startup watchdog. Not ported: ysosrs' removal of official VC-1 guards, total cross-ladder recovery
budget, source-panel greying (UI), mime-override re-init.

## G4b result
Correction to the table above: the ysosrs `ParallelRangeDataSource` delta is comments only. With
comments stripped the two files differ in five lines, all official additions (HUD connection and
chunk fields), so 8, 9 and 11 are verified_official. Correction to 18: ysosrs does warm before
playback (`PlayerPlaybackNetworking.prewarmPlaybackConnection`), and official already consumes the
windows it produces (`PrefetchWindowStore`). Ported (IMPORT_LEDGER G4b): head + tail window warm at
press, only when the press resolves to REMUX / Throughput. Not ported: second-socket head
fallback, focus-time warm (G8), POOL_ID logging.

## G4c result
Per-file dvmkv diff (official vs ysosrs; Reshaped equals official): ysosrs differs only in
`MatroskaExtractor`, `DefaultEbmlReader` and `VarintReader`. Official is newer for truncated tails
(22) and nested SeekHead (23); ysosrs removes both, so those removals are not inherited. The ysosrs
delta is mid-stream resync to the next Cluster after malformed cluster data (21), ported with
official truncated-tail handling checked first. 24: neither source has MP4 extractor changes; the
ysosrs delta is "MP4 session mode" in `PlayerMediaSourceFactory` (single-connection 8 MiB chunk
session for progressive MP4 when parallel is off, so scatter reads survive data-source recreation on
seek). It changes network behavior, so per D044 it belongs to a strategy: moved to G4d with the disk
seek buffer, under Seek optimized.
