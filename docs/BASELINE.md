# Nuvio Superfork Baseline

## Baseline history
- Initial bootstrap anchor: `c257a2365ee3386b582dc2974ec235cfe0381f33`
- Pre-G0 refresh: `fd7973d91dd75d790c5f9b3d68dae652655e92c4`
- Superfork integration sync commit: `3cf04ccdcc20515acb093c28ad9b7c3943a39057`
- The pre-G0 refresh happened before feature implementation; `fd7973d91dd75d790c5f9b3d68dae652655e92c4` is the current implementation baseline.

## Official upstream
- Repository: NuvioMedia/NuvioTV
- Branch: dev
- Pinned baseline: fd7973d91dd75d790c5f9b3d68dae652655e92c4
- Verified: 2026-09-28
- Commit: Merge pull request #3725 from NuvioMedia/feat/torengine — feat(torrent): replace TorrServer with Nuvio Engine

## Fork branches
- superfork/integration: clean integration baseline
- chore/fork-foundation: Gate 0 work branch
- Existing legacy branches remain untouched.

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
The baseline SHA is immutable. Future upstream syncs must be recorded separately. Never merge a feature fork wholesale. Diff before import and keep official behavior as fallback where practical.