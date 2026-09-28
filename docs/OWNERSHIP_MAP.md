# Architectural Ownership Map

One concern must have one architectural owner. Adapters may exist; competing engines should not.

| Concern | Owner / integration seam | Superfork extension area | Forbidden outcome |
|---|---|---|---|
| Player lifecycle | PlayerRuntimeController / PlayerViewModel | fork/playback, bridge | second independent player lifecycle |
| Media source / network | PlayerMediaSourceFactory / ParallelRangeDataSource | fork/playback/network strategies | replacing official stack wholesale |
| Resource policy | AdaptiveResourceManager target | fork/resource | per-feature ad-hoc RAM rules |
| Diagnostics | PlayerDebugStatsOverlay + normalized model | fork/diagnostics | multiple Stats HUD models |
| Subtitles | existing subtitle repository/player hooks | fork/subtitles / one SubtitleSyncEngine | multiple competing AutoSync engines |
| Stream ranking | StreamScreenViewModel integration | fork/streams / one ranker | separate autoplay/list rankers |
| Skip metadata | SkipIntroRepository integration | fork/skip / one aggregator | provider-specific skip engines in UI |
| Recommendations | current post-play controller | fork/recommendations | duplicated recommendation state machines |
| Live TV | dedicated Live TV module | fork/livetv | coupling IPTV internals into VOD core |
| Watch Party | PlayerBridge abstraction | fork/watchparty | transport directly owning player internals |
| UI styles | official layout/navigation preferences | fork/ui | deleting official layout choices |
| Feature flags | FeatureRegistry | fork/foundation | scattered unrelated booleans |
| Security/credentials | profile-scoped secure stores / Keystore | fork/security adapters as needed | plaintext secrets or duplicate credential stores |

When an imported fork introduces a second owner for a concern, port its behavior into the owner above instead of keeping the duplicate architecture.
