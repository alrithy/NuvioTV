# Nuvio Superfork Architecture

## Goal
Build a maintainable best-of-Nuvio-forks distribution on top of official dev while reusing proven fork code.

## Principles
- Official-first.
- Reuse-first.
- Adapter-first.
- One owner per concern.
- Feature isolation.
- No duplicate upstream implementations.
- Experimental AI/MAT remain OFF by default.

## Logical layers
Official NuvioTV dev
-> Fork Foundation
   -> Feature Registry / Flags
   -> Source Attribution / Import Ledger
   -> Adaptive Resource Manager
-> Official Player Runtime
   -> Playback Strategy Layer
      -> Official
      -> REMUX / Throughput
      -> Seek Optimized
      -> Low Memory
      -> Auto
-> Subtitle Intelligence
   -> Embedded reference
   -> Release/hash reference
   -> Cue-rhythm alignment
   -> Audio/ASR fallback
   -> Manual fallback
-> Stream Intelligence
   -> Progressive results
   -> Dedup
   -> Cache/quality analysis
   -> Connection fit
   -> Ranking/autoplay
-> Feature Modules
   -> Multi-provider Skip
   -> Calendar / Random / Mystery
   -> Recommendations
   -> Live TV
   -> Watch Party
   -> UI styles
-> Unified Diagnostics
-> Experimental
   -> AI Media Providers
   -> MAT audio

## Package boundary
Keep the existing app Gradle module initially. Add isolation under:
app/src/main/java/com/nuvio/tv/fork/

Suggested packages:
- foundation
- bridge
- resource
- playback
- subtitles
- streams
- skip
- livetv
- discovery
- recommendations
- watchparty
- diagnostics
- ui
- experimental

Do not relocate official/imported classes merely for cosmetic consistency if that causes unnecessary churn.

## Integration rules
Player: extend current PlayerRuntimeController / PlayerMediaSourceFactory; never replace PlayerViewModel wholesale.
Network: official already has ParallelRangeDataSource and StreamSpeedTester; port only missing deltas.
Settings: use official stores; fork-only settings get their own store where appropriate.
Navigation: add destinations through NuvioNavHost and preserve official layouts.
Skip: extend/aggregate behind SkipIntroRepository instead of creating competing systems.
Diagnostics: enrich the existing PlayerDebugStatsOverlay through one normalized model.
