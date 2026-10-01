# Nuvio Superfork Baseline

## Baseline history
- Initial bootstrap anchor: `c257a2365ee3386b582dc2974ec235cfe0381f33`
- Pre-G0 refresh: `fd7973d91dd75d790c5f9b3d68dae652655e92c4`
- Superfork integration sync commit: `3cf04ccdcc20515acb093c28ad9b7c3943a39057`
- The pre-G0 refresh happened before feature implementation.
- Post-G0 sync (PR #9, D040): `71632b9271e8bce6783e415d64f34cfa4e8b894c`; integration merge `898bc83abfc8e59cf1664699980942d3bd6718f6`.
- Pre-G6 sync (D050): `56aaba20b7d01746d616a2c8517adcf442950b90`; integration merge `5c24805f82b2bb7092b9acafc00903a32fe5045c`.
- Pre-G12 sync (D059): `5c1d9b0e2669199114a12ade38027da132303eb3`; integration merge `89b46c4dfa4327a0eaaa212e2c05bad3c9c6b766`.
- Pre-G14 sync (D065): `aeb6ee8591c55424256fdd1f35f426618297378d`; integration merge `ed62c7bbb9a6c1065c5f6368048232293f8d235e`. This is the current implementation baseline.

## Official upstream
- Repository: NuvioMedia/NuvioTV
- Branch: dev
- Pinned baseline: aeb6ee8591c55424256fdd1f35f426618297378d
- Verified: 2026-10-01
- Commit: Merge pull request #3796 (fix/mpv-error-diagnostics)

## Fork branches
- superfork/integration: canonical reviewed integration branch.
- chore/governance-hardening: temporary governance-hardening branch for PR #6.
- chore/fork-foundation-impl: active G0 implementation branch after PR #6 merges and is rebased/fast-forwarded to integration.
- chore/fork-foundation: historical bootstrap/governance branch; not an active source of truth.
- Existing legacy claude/* and old feature branches are reference-only unless explicitly harvested through the current porting protocol.

## Verified integration seams
- Navigation: app/src/main/java/com/nuvio/tv/ui/navigation/NuvioNavHost.kt
- Player runtime: app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerRuntimeController.kt
- Player composition: app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerViewModel.kt
- Media source: app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerMediaSourceFactory.kt
- Parallel ranges: app/src/main/java/com/nuvio/tv/ui/screens/player/ParallelRangeDataSource.kt
- Stream selection: app/src/main/java/com/nuvio/tv/ui/screens/stream/StreamScreenViewModel.kt
- Playback settings: app/src/main/java/com/nuvio/tv/ui/screens/settings/PlaybackSettingsViewModel.kt
- Settings store: app/src/main/java/com/nuvio/tv/data/local/PlayerSettingsDataStore.kt
- Network DI: app/src/main/java/com/nuvio/tv/core/di/NetworkModule.kt
- Layout settings: app/src/main/java/com/nuvio/tv/data/local/LayoutPreferenceDataStore.kt
- Skip metadata: app/src/main/java/com/nuvio/tv/data/repository/SkipIntroRepository.kt
- Debug overlay: app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerDebugStatsOverlay.kt
- Speed tester: app/src/main/java/com/nuvio/tv/core/network/StreamSpeedTester.kt

## Rules
Baseline history entries are immutable: never rewrite old sync history. Future official movement must append to docs/UPSTREAM_SYNC_LOG.md and update the current baseline fields through a dedicated upstream-sync PR. Never merge a feature fork wholesale. Diff before import and keep official behavior as fallback where practical.
## Audit observation (historical; superseded by the 2026-09-29 sync)
2026-09-28: official dev = e78de241acb8a6128c29076422377de1206ee8cd, settings PR #3746.
Current accepted pin remains fd7973d; see D037 and state.upstream_observation.
The 3cf04cc integration production diff against fd7973d is only a missing final newline
in ModernHomeContent.kt. Record this precisely; do not claim byte-for-byte parity.
