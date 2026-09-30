# G9 audit — Skip / Recommendations / Discovery

Evidence for `tasks/G9_DISCOVERY_SKIP_RECOMMENDATIONS.md` (IDs 117–146, 169–187, 206–207). Official =
accepted `56aaba2` (D050). Official `dev` has since moved to `7f32b3c` (version bump, Greek strings,
Grid Home hero ratings, Exo libass fix), which touches no G9 seam: observed, not accepted. Source
(SOURCE_MAP pin): Cxsmo-ai/NuvioTV-Custom `main` @ `3e0d0fad60a2721adec133b88640b49c0183883f`.
Reshaped has no G9 code. This audit changes no code.

## What official has at `56aaba2`
- **Skip:** `data/repository/SkipIntroRepository` fetches IntroDB (intro, recap, outro; movie
  credits and post-credits with a guard so skipping credits never skips the scene), AniSkip and
  Anime-Skip **in parallel** (`async`) and keeps one interval per category by provider priority
  (IntroDB, Anime-Skip, AniSkip). Movies resolve TMDB / MAL / Kitsu ids to IMDb; series need an
  IMDb id (anime entries are mapped through Simkl). No per-provider timeout, no confidence, no
  preview / content-warning / mute segments; the Anime-Skip client id is stored in plain DataStore.
  Player: `SkipIntroButton`, `autoSkipSegmentTypes` (intro, recap, outro, movie credits).
- **Post-play:** `PostPlayRecommendationController` with one configurable "more like this" source
  (TMDB, Trakt or Simkl; TMDB when the chosen tracker is not signed in), 10 s budget, watched and
  unreleased filtering, at most 4 cards, each resolved **lazily** when shown
  (`startCandidateResolution`), one trailer source (`TrailerService`), MDBList ratings on cards.
  `PostPlayRecommendationTiming` owns when the overlay appears.
- **Random episodes:** `EpisodeShuffle` + `RandomEpisodePicker` + `EpisodeShufflePlayback` on the
  detail, home and playback surfaces: random across all seasons, unwatched-only unless "include
  watched" is on, no immediate repeats. When every episode is watched and "include watched" is off,
  the picker returns nothing (the UI shows "change selection").
- **No** calendar, Mystery mode, season-scoped random or app dimmer.

## What the pinned fork adds (Cxsmo)
| Area | Cxsmo @ 3e0d0fa |
|---|---|
| Skip aggregator | `SkipIntroRepository` rewrite: SkipMe.db, IntroDB (app API key), TheIntroDB, PublicMetaDB, MovieHavenDB, VideoSkip, NotScare (public page); each provider in its own `async` with a 6 s `withTimeoutOrNull`; `SkipEvidence` per report; `mergeSkipIntervals` groups same type + action overlapping within 2 s, confidence-weighted start/end, multi-provider confidence bonus, evidence kept; 6 h cache; response size caps; categories preview, jumpscare, nudity, sex, gore, violence, profanity, custom; `action` skip vs mute parsed (MovieHavenDB `mute`, VideoSkip audio labels) |
| Credentials | `SkipProviderCredentialsStore`: PublicMetaDB / IntroDB app / TheIntroDB keys, AES-GCM with an Android Keystore key, per-profile DataStore |
| Post-play | source choice AUTO / Trakt / TMDB / Kurato AI / BingeCat AI / Simkl / MDBList; Kurato and BingeCat are catalogs of add-ons the user installed (no new endpoints); catalog paging; post-play trailer playback through `TrailerPlayerPool` |
| Random / Mystery | `RandomEpisodeDialog`: all seasons or current season, unwatched only with fallback to the whole pool, Mystery mode flag passed to playback |
| Calendar | `CalendarRepositoryImpl` + `CalendarScreen`: library and tracked shows, metadata add-on air dates, Nuvio Sync / tracker watched-state refresh (flags only, no metadata refetch), spoiler-hidden flag for an unwatched episode whose previous episode is unwatched |
| Dimmer | `AppDimmerOverlay`: black overlay 0–95 %, drawn above everything, no input capture |

**Not inherited (fork removals / rewrites of official):** Cxsmo's skip rewrite drops official
**AniSkip and Anime-Skip** (and the Simkl anime mapping) and replaces the priority merge; it deletes
official `EpisodeShufflePlayback` and `PostPlayRecommendationTiming`. Official keeps all of them. The
mute action is parsed by Cxsmo but never applied by its player.

## Decision: one skip aggregator on the official seam (D054)
- **Skip:** official `SkipIntroRepository` stays the only entry point and keeps IntroDB, AniSkip and
  Anime-Skip. Additional providers (Cxsmo parsers and fetchers, FILE_PORT) live in `fork/skip` and run
  beside them, each in its own coroutine with its own timeout; results merge by Cxsmo's evidence merge
  (ALGORITHM_PORT) so overlapping reports combine with confidence weighting and a failing or slow
  provider never delays the others. New providers and the new categories are **opt-in** per provider /
  category; the defaults are exactly official's providers and categories (D048 principle).
- **Skip vs mute stay distinct:** a mute segment never seeks; it lowers the player volume for its span
  and restores it (local; no source applies it). Content-warning segments are off by default.
- **Credentials:** provider keys use Cxsmo's Keystore-encrypted per-profile store (FILE_PORT); keys are
  never logged.
- **Post-play:** official controller and timing stay; the fork adds sources (MDBList, Kurato AI and
  BingeCat AI add-on catalogs, AUTO chain), paging beyond 4 cards and a trailer fallback chain behind
  the official source setting.
- **Random / Mystery:** built on official `EpisodeShuffle` / `RandomEpisodePicker`: add a current-season
  scope, the all-watched fallback and Mystery mode (title, number, image and overview hidden until
  playback starts), never revealing Mystery metadata in any surface.
- **Calendar and dimmer:** Cxsmo FILE_PORT behind the fork flag.
- `DISCOVERY_SKIP_RECOMMENDATIONS` becomes AUTO with the first code slice; OFF = official exactly.

## Features
| ID | Behavior | Class | Result / plan |
|---|---|---|---|
| 117 | Multi-provider skip engine | PARTIAL_OVERLAP | official 3 providers, priority merge; extend on the official seam → G9a |
| 118 | Skip recap | ALREADY_OFFICIAL | IntroDB / AniSkip recap |
| 119 | Skip credits / outro | ALREADY_OFFICIAL | outro, movie credits, post-credits guard |
| 120 | Skip preview | missing delta | Cxsmo preview category → G9b |
| 121 | Content-warning segments | missing delta | Cxsmo categories (jumpscare, nudity, …), opt-in → G9b |
| 122 | Mute segments | missing delta | Cxsmo parses `mute`, never applies; local volume mute → G9b |
| 123 | IntroDB | ALREADY_OFFICIAL | official provider (optional app key → G9a credentials) |
| 124 | SkipMe.db | missing delta | Cxsmo fetcher + parser → G9a |
| 125 | TheIntroDB | missing delta | → G9a |
| 126 | PublicMetaDB | missing delta | → G9a |
| 127 | MovieHavenDB | missing delta | → G9b (movie content / mute data) |
| 128 | VideoSkip | missing delta | → G9b |
| 129 | NotScare public page | missing delta | → G9b (jump scares) |
| 130 | Parallel provider requests | ALREADY_OFFICIAL | official `async` per provider |
| 131 | Independent timeout per provider | missing delta | 6 s per provider → G9a |
| 132 | Merge overlapping evidence | PARTIAL_OVERLAP | official one-per-category priority; Cxsmo evidence merge → G9a |
| 133 | Confidence-weighted timing | missing delta | → G9a |
| 134 | TMDB → IMDb normalization | PARTIAL_OVERLAP | official for movies; series too → G9a |
| 135 | Encrypt provider credentials per profile | missing delta | Cxsmo Keystore store → G9a |
| 136 | Configurable post-play engine | PARTIAL_OVERLAP | official one source setting; more sources → G9c |
| 137 | Trakt recommendations | ALREADY_OFFICIAL | `TraktRelatedService` |
| 138 | TMDB recommendations | ALREADY_OFFICIAL | `fetchMoreLikeThis` |
| 139 | Simkl recommendations | ALREADY_OFFICIAL | `SimklRelatedService` |
| 140 | MDBList recommendations | missing delta | → G9c |
| 141 | Kurato AI recommendations | missing delta | installed add-on catalog → G9c |
| 142 | BingeCat AI recommendations | missing delta | installed add-on catalog → G9c |
| 143 | Auto recommendation-provider mode | PARTIAL_OVERLAP | official falls back to TMDB; AUTO chain → G9c |
| 144 | Full pagination | missing delta | official max 4 → G9c |
| 145 | Lazy metadata loading | ALREADY_OFFICIAL | per-card `startCandidateResolution` |
| 146 | Multi-source trailer fallback | PARTIAL_OVERLAP | official one trailer source → G9c |
| 169 | Random Episode picker | ALREADY_OFFICIAL | `EpisodeShuffle` / `RandomEpisodePicker` |
| 170 | Random across all seasons | ALREADY_OFFICIAL | picker pool = all episodes |
| 171 | Random within current season | missing delta | → G9d |
| 172 | Unwatched-only mode | ALREADY_OFFICIAL | "include watched" off by default |
| 173 | Fallback after all episodes watched | missing delta | official returns nothing → G9d |
| 174 | Mystery Episode mode | missing delta | → G9d |
| 175 | Hide episode title in Mystery Mode | missing delta | → G9d |
| 176 | Hide episode number | missing delta | → G9d |
| 177 | Hide image / overview | missing delta | → G9d |
| 178 | Native in-app Calendar | missing delta | Cxsmo FILE_PORT → G9e |
| 179 | Upcoming episodes | missing delta | → G9e |
| 180 | Recently released episodes | missing delta | → G9e |
| 181 | Combine Library + metadata-add-on dates | missing delta | → G9e |
| 182 | Nuvio Sync integration | missing delta | watched state via official repositories → G9e |
| 183 | Trakt integration | missing delta | → G9e |
| 184 | Simkl integration | missing delta | → G9e |
| 185 | MDBList integration | missing delta | → G9e |
| 186 | Real-time watched-state updates | missing delta | → G9e |
| 187 | Spoiler-safe Calendar | missing delta | → G9e |
| 206 | App Dimmer | missing delta | Cxsmo overlay → G9f |
| 207 | Player-accessible dimmer control | missing delta | → G9f |

## Slice plan
- G9a (117, 124–126, 131–135): aggregator core on the official seam; SkipMe / TheIntroDB / PublicMetaDB;
  per-provider timeout; evidence merge; series normalization; encrypted credentials. Tests: timeout
  isolation, merge / confidence, official providers unchanged by default.
- G9b (120–122, 127–129): preview and content-warning categories, MovieHavenDB / VideoSkip / NotScare,
  mute action. Tests: skip vs mute kept apart, action conflict, opt-in defaults.
- G9c (136, 140–144, 146): post-play sources, AUTO chain, paging, trailer fallback. Tests: fallback order,
  paging, official timing untouched.
- G9d (171, 173–177): season scope, all-watched fallback, Mystery mode. Tests: scope, fallback, no
  Mystery metadata leak.
- G9e (178–187): Calendar. Tests: date merge, watched updates, spoiler flags.
- G9f (206, 207): App dimmer and player control.
- 118, 119, 123, 130, 137–139, 145, 169, 170, 172 verified official.

Every slice keeps device checks MANUAL-PENDING in `docs/HARDWARE_VALIDATION_TCL_C6K.md` (G14, D047).
