# G8 audit — Stream Intelligence

Evidence for `tasks/G8_STREAM_INTELLIGENCE.md` (IDs 147–168) and G1's deferred 289 (add-on retry
policy). Official = accepted `56aaba2` (D050). Official `dev` has since moved to `9bf4ed1` ("bump
version": `app/build.gradle.kts` version fields only), which touches no G8 seam: observed, not
accepted. Sources (SOURCE_MAP pins): DavidVamaiotu/NuvioTV-Reshaped `subtitle-autosync` @
`0ccf049d2789600835f3f7a75423e9149ea416ba` (`core/connection/*`), Cxsmo-ai/NuvioTV-Custom `main` @
`3e0d0fad60a2721adec133b88640b49c0183883f` (progressive AIOStreams, `StreamQualityRank`). This audit
changes no code.

## What official has at `56aaba2`
- **Incremental results:** `StreamRepositoryImpl.getStreamsFromAllAddons` emits the accumulated
  list as each add-on answers, and the stream screen renders it while slower add-ons continue.
- **Dedup:** add-on results merge by `Stream.dedupKey()`; the Direct Debrid list dedups by
  info hash / file index / filename.
- **Ranking engine (Direct Debrid only):** `DirectDebridStreamFilter` extracts facts (resolution,
  quality BluRay REMUX > BluRay > WEB-DL > …, release group (required/excluded lists, alphabetical sort key), HDR/DV visual tags, audio tags,
  channels, encode, size, language) and applies the user's `DebridStreamPreferences`
  (sort criteria, preferred/required/excluded lists, limits). It runs on the Direct Debrid list,
  which holds **cached** debrid streams only; ordinary add-on lists keep add-on order.
- **Autoplay:** `StreamAutoPlaySelector` modes MANUAL / FIRST_STREAM / REGEX_MATCH, source and
  add-on scoping, binge group; no quality-based pick.
- **Scrape timeout:** `streamAutoPlayTimeoutSeconds`: 0 = instant (first add-on response),
  1–30 s = bounded wait, unlimited = wait for every add-on with a 60 s hard timeout.
- **Add-on health (fork G1):** `fork/diagnostics/AddonHealthTracker` records per-add-on outcomes
  (display only).
- **No** progressive AIOStreams endpoint, bitrate estimate, connection fit or add-on retry.

## What the pinned forks add
| Area | Reshaped | Cxsmo |
|---|---|---|
| Progressive AIOStreams | none | `StreamRepositoryImpl.fetchProgressiveStreams` + `ProgressiveStreamEnvelopeDto`: opt-in `/nuvio-progressive/` NDJSON endpoint, cumulative snapshots replace the add-on's group, 180 s budget, falls back to the ordinary JSON endpoint when unavailable |
| Ranking for all sources | none | `StreamQualityRank`: official `DirectDebridStreamFilter` facts for every source, exclusion lists first (falls back to the unfiltered pool if that empties it), fixed chain resolution → quality → release-group ladder (TRaSH tiers) → visual → audio → channels → encode → size → container; stable (ties keep add-on order); user list sort order deliberately ignored for autoplay |
| Autoplay by quality | none | `StreamAutoPlaySelector` hook to `StreamQualityRank` |
| Connection fit | `StreamConnectionFit` + `ConnectionSpeedEstimator` + `PlaybackThroughput`: average bitrate from size ÷ runtime (implausible values ignored), streams above connection ÷ 1.5 move to the bottom in stable order, captured once per load so the list never reshuffles; unknown bitrate stays in place | none |
| Prefetch / sweep | none | `StreamPrefetchCache`, `PrefetchSelectionSupplier`, `StreamSweepEngine` (911 lines, background source sweeps) — outside the G8 packet |

Not inherited: Cxsmo's `R1_SPLIT` timing logs on every ranking call, its removal of official
`PlaybackAvailability` / YouTube resolver / sidecar subtitles, its stream-screen redesign
(`StreamComponents`, `StreamSourcesSidePanel` churn), and Reshaped's rewrite of official
`StreamSpeedTester`. Official is kept wherever a fork removed or rewrote it.

## Decision: one ranking engine (D053)
One ranker in `fork/streams`, built on **official** `DirectDebridStreamFilter` facts and
`DebridStreamPreferences` (REUSE; no second fact parser), used by both the stream lists and
autoplay. Pipeline, as the packet orders it: progressive collection → official dedup → official
facts → ranking → connection fit → autoplay.
- Ranking (ALGORITHM_PORT of Cxsmo `StreamQualityRank`): cached debrid first; uncached stays
  visible below; resolution → quality (REMUX first, WEB-DL fallback) → release group → HDR/DV →
  lossless audio → channels → encode → bitrate (size ÷ runtime) → source reliability (G1 health),
  stable so add-on order survives every tie. User-set sort criteria keep driving the visible
  Direct Debrid list as in official.
- Connection fit (FILE_PORT of Reshaped `StreamConnectionFit`): demotes streams the measured
  connection cannot sustain, in stable order, captured once per load.
- Autoplay: a new official-style mode **Best quality** next to First stream / Regex; it takes
  the ranker's first stream from the same candidates the official modes see. Existing modes and
  the default are unchanged (D048 principle: a user choice).
- Lists: ranked order is a user choice (a "Best quality" list order next to official's), default
  official add-on order (167).
- Progressive AIOStreams: capability-detected per add-on URL, snapshots replace that add-on's
  group through official dedup; any failure falls back to the official request unchanged.
- 289: one bounded retry for a timed-out or 5xx add-on stream request within the active scrape
  timeout, never for 4xx, never beyond the hard timeout; owned by the stream layer.
- STREAM_INTELLIGENCE becomes AUTO with the first code slice; OFF = official lists, autoplay and
  requests exactly.

## Features
| ID | Behavior | Class | Result / plan |
|---|---|---|---|
| 147 | Progressive AIOStreams results | missing delta | Cxsmo progressive endpoint → G8a |
| 148 | Show results while slower add-ons continue | ALREADY_OFFICIAL | per-add-on accumulated emission |
| 149 | Cumulative NDJSON snapshots | missing delta | snapshot replaces the add-on group → G8a |
| 150 | Deduplicate progressive results | PARTIAL_OVERLAP | official `dedupKey` merge; applied to snapshots → G8a |
| 151 | Fall back to normal AIOStreams endpoint | missing delta | capability detect + official request on any failure → G8a |
| 152 | Instant scrape timeout mode | ALREADY_OFFICIAL | autoplay timeout 0 |
| 153 | Bounded scrape timeout mode | ALREADY_OFFICIAL | autoplay timeout 1–30 s |
| 154 | Unlimited scrape mode with safeguards | ALREADY_OFFICIAL | unlimited with the 60 s hard timeout |
| 155 | Improved stream ranking | PARTIAL_OVERLAP | official ranking covers Direct Debrid only; one engine for all sources → G8b |
| 156 | Real-Debrid cached first | PARTIAL_OVERLAP | official Direct Debrid list is cached-only; ranker orders cached first across sources → G8b |
| 157 | Keep uncached results visible | ALREADY_OFFICIAL | official keeps uncached add-on streams in their lists; the ranker never drops them → verified in G8b tests |
| 158 | 4K above 1080p when configured | PARTIAL_OVERLAP | official resolution criterion (Direct Debrid); ranker for all sources → G8b |
| 159 | REMUX preference | PARTIAL_OVERLAP | official quality order (REMUX > BluRay > WEB-DL); ranker → G8b |
| 160 | Release-group quality ranking | PARTIAL_OVERLAP | official has required/excluded release groups and an alphabetical sort key, no quality ladder; Cxsmo tier ladder → G8b |
| 161 | Codec ranking | PARTIAL_OVERLAP | official encode key; ranker → G8b |
| 162 | HDR / DV ranking | PARTIAL_OVERLAP | official visual tags; ranker (DV only when the display supports it, G5 display caps) → G8b |
| 163 | Lossless-audio ranking | PARTIAL_OVERLAP | official audio tags; ranker → G8b |
| 164 | Bitrate ranking | missing delta | size ÷ runtime (Reshaped `averageBitrateMbps`) → G8b |
| 165 | Connection-fit ranking | missing delta | Reshaped `StreamConnectionFit` → G8c |
| 166 | Source-reliability ranking | missing delta | G1 `AddonHealthTracker` as the last tiebreak → G8b |
| 167 | Preserve add-on order where possible | ALREADY_OFFICIAL | official default order; ranker is stable and opt-in |
| 168 | Autoplay best stream with the same engine | missing delta | "Best quality" autoplay mode → G8b |
| 289 | Add-on retry policy (G1) | missing delta | one bounded retry inside the scrape timeout → G8a |

## Slice plan
- G8a (147, 149, 150, 151, 289): progressive AIOStreams with clean fallback; bounded add-on
  retry. Tests: snapshot replace, dedup, fallback on 404/parse error/timeout, retry bounds.
- G8b (155, 156, 158–164, 166, 168): the ranker over official facts, "Best quality" autoplay mode
  and list order. Tests: deterministic order, cached-first, missing metadata, stability,
  autoplay/list parity.
- G8c (165): connection fit with a measured connection estimate. Tests: demotion, unknown bitrate
  stays, stable order, no reshuffle within a load.
- 148, 152–154, 157, 167 verified official.

Every slice keeps device checks MANUAL-PENDING in `docs/HARDWARE_VALIDATION_TCL_C6K.md` (G14, D047).
