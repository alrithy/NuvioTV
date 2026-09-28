# Component & Integration Map

Use this before editing core code. Paths are based on the current official baseline and must be re-audited after upstream syncs.

## Player runtime
Primary owner:
- `app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerRuntimeController.kt`
- split `PlayerRuntimeController*.kt` helpers
- `PlayerViewModel.kt`

Rules:
- do not replace PlayerViewModel wholesale;
- add adapters/controllers around the existing runtime;
- preserve official player path/fallback for high-risk changes.

## Media source / transport
Primary:
- `PlayerMediaSourceFactory.kt`
- `ParallelRangeDataSource.kt`
- `core/network/StreamSpeedTester.kt`

Gates: G3/G4.
Rule: current official already owns parallel-range and speed-test behavior. ysosrs/Reshaped are delta sources, not drop-in replacements.

## Playback settings / persistence
Primary:
- `data/local/PlayerSettingsDataStore.kt`
- `ui/screens/settings/PlaybackSettingsViewModel.kt`
- `ui/screens/settings/PlaybackSettingsScreen.kt`
- `ui/screens/settings/NetworkSettingsScreen.kt`

Rules:
- preserve existing settings;
- fork-only settings get isolated keys/store where justified;
- every new setting has an explicit safe default.

## Diagnostics
Primary UI owner:
- `ui/screens/player/PlayerDebugStatsOverlay.kt`

Related:
- last playback diagnostics/settings models
- stream speed tester
- player runtime signals

Gate: G1.
Rule: one normalized model; no competing HUD.

## Stream discovery / selection
Primary:
- `ui/screens/stream/StreamScreenViewModel.kt`
- domain Stream / AddonStreams models
- StreamRepository / AddonRepository
- stream badge/presentation components

Gate: G8.
Rule: one ranking engine must power display and autoplay.

## Skip metadata
Primary:
- `data/repository/SkipIntroRepository.kt`
- player runtime consumers

Gate: G9.
Rule: extend behind one aggregator; keep skip/mute actions distinct.

## Navigation
Primary:
- `ui/navigation/NuvioNavHost.kt`
- navigation Screen definitions

Gates: G9/G10/G12.
Rule: preserve official routes/layouts and D-pad return-focus behavior.

## Layout / Home
Primary:
- `data/local/LayoutPreferenceDataStore.kt`
- Home screens/layout models
- `ModernHomeContent.kt`

Gate: G12, with Calendar/discovery hooks in G9.
Rule: Glass/Pill/top-nav are selectable alternatives, not destructive replacements.

## Network dependency injection
Primary:
- `core/di/NetworkModule.kt`

Rules:
- fixed first-party clients remain strict;
- addon-permissive behavior must not leak into first-party security boundaries;
- new providers get explicit clients/timeouts where needed.

## Target fork-owned package boundary
Prefer new orchestration in:
`app/src/main/java/com/nuvio/tv/fork/`

Subpackages:
- foundation
- bridge
- diagnostics
- resource
- playback
- subtitles
- streams
- skip
- discovery
- recommendations
- livetv
- watchparty
- ui
- experimental

Do not relocate official/imported code merely for aesthetics if it creates churn.

## Gate-to-component summary
- G0: fork/foundation only
- G1: diagnostics + addon health
- G2: fork/resource + call-site policies
- G3: fork/playback strategy + player/media-source adapters
- G4: player/media-source/network delta
- G5: player audio/video/DV/AFR settings/runtime
- G6: fork/subtitles + subtitle repository/player bridge
- G7: seek preview/player/cache paths
- G8: stream VM/repository + fork/streams
- G9: skip/discovery/recommendations/navigation
- G10: fork/livetv + navigation/player bridge
- G11: fork/watchparty + player bridge
- G12: UI/layout/navigation
- G13: fork/experimental only unless an audited bridge is required
- G14: server/security/updater/release/CI/upstream operations
