# G3 audit — Playback Strategy Framework

Evidence for `tasks/G3_PLAYBACK_STRATEGIES.md`. Official = `71632b9` (integration `78be49a`,
official head observed `7d3cea0`; #3740 edits `AudioSelectionOverlay` and
`PlayerRuntimeControllerPlaybackEvents`, which G3 does not touch, so it stays observed, not synced).
Sources: official + project glue (GATE_SPECS G3). No external code is imported, so there is no
IMPORT_LEDGER entry; D005/D006 define the strategy set.

## Official owners found
- Settings: `PlayerSettings` buffer/network fields: `bufferEngineEnabled` (custom load control master),
  `bufferBudgetManaged`, `bufferSettings.targetBufferSizeMb`, `parallelNetworkEnabled` (parallel
  master), `useParallelConnections` / `parallelConnectionCount`, `vodCacheEnabled` (disk cache), and
  `nuvioPerformanceModeEnabled` (process-wide native engine config).
- Session build: `PlayerRuntimeControllerInitialization.initializePlayer` reads settings once, then
  the ExoPlayer branch builds the load control and configures `PlayerMediaSourceFactory`. Nothing
  re-applies these knobs mid-session (the settings observer only pushes VOD cache size while the
  custom-buffer master is on).
- Official gates kept: parallel reads and the disk cache run only for progressive http(s)
  (not HLS/DASH, torrent or loopback); G2 `MemoryBudget` tiers and `AdaptiveResourcePolicy` caps.

## Design
One owner: `fork/playback`. `PlaybackStrategies.resolve` (pure) maps the stored selection plus
session facts to an effective strategy and reason; `PlaybackStrategies.knobs` (pure) gives
overrides on official settings; `PlaybackStrategySession.plan` applies them to a **copy** used only
for the ExoPlayer session being built. Stored settings are never written, the MPV path and
`nuvioPerformanceModeEnabled` (global engine config) are never changed.

| Strategy | Overrides (null = user's value) |
|---|---|
| Official | none (same `PlayerSettings` instance) |
| REMUX / Throughput | parallel master + parallel on, connections = max(stored, 4) through the G2 policy (≤ 4 on constrained), custom buffer on with managed (device) budget |
| Seek optimized | official disk cache on, custom buffer on with managed budget, parallel off (D006: no parallel REMUX stacked with seek buffering) |
| Low memory | parallel off, custom buffer on, unmanaged, target buffer = min(stored, 50 MB) |
| Auto | low-RAM device → Low memory; progressive http(s) file ≥ 20 GiB or a `remux` filename token → REMUX / Throughput; otherwise Official |

Fallback: feature OFF → Official; MPV engine → Official (`engine`); REMUX / Seek on a stream that is
not progressive http(s) → Official (`stream type`). Low memory applies to any ExoPlayer stream.
Diagnostics: the G1 HUD `strategy` row shows `effective` or `selected → effective (reason)`; the
player logs `PLAYBACK_STRATEGY:` per session.

## Features
| ID | Behavior | Class | Result |
|---|---|---|---|
| 3 | Automatic strategy by device/file/network | missing delta | Auto uses device tier (G2) and file facts (size, filename, stream type); a measured network signal is not available at session build in official, so throughput measurement is left to G4 |
| 27 | Standard playback mode | ALREADY_OFFICIAL | Official strategy = stored official settings, unchanged; default selection |
| 28 | REMUX / Throughput mode | missing delta | official parallel range + managed buffer, capped by G2 |
| 29 | Seek Optimized mode | missing delta | official VOD disk cache + managed buffer, no parallel; Reshaped disk seek buffer stays G4 (ID 25) |
| 30 | Low-RAM mode | missing delta | small buffer, single connection |
| 31 | Auto mode | missing delta | deterministic resolver, tested |

Selection: per-profile DataStore `fork_playback_strategy`, key `selected_strategy` (strategy key;
absent/unknown = Official). Fresh install: nothing stored → Official. Downgrade: older builds ignore
the file. UI: a card in the official advanced (network) settings list, EN + AR strings; hidden when
`PLAYBACK_STRATEGY_ENGINE` is OFF (D043 default AUTO = visible).

Not done here: MPV low-memory demuxer budget (280, no sourced or measured value), measured network
signal for Auto (G4), strategy in playback issue reports (reports keep the stored settings; the HUD
and log carry the strategy).
