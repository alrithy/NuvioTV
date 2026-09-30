# G10 audit — Live TV

Evidence for `tasks/G10_LIVE_TV.md` (IDs 208–236). Official = accepted `56aaba2` (D050). Official `dev`
has since moved to `5c1d9b0` (global landscape poster mode, Hebrew / Greek / Vietnamese / Polish strings,
detail hero trailer focus, Grid Home MDBList ratings, Exo libass fix). It touches no Live TV seam; the
only shared file is `MainActivity`, where it adds two layout preferences next to the drawer: observed,
not accepted. Source (SOURCE_MAP pin): DavidVamaiotu/NuvioTV-Reshaped @
`0ccf049d2789600835f3f7a75423e9149ea416ba` (GPL-3.0, same license as Nuvio). Cxsmo has no Live TV code.
This audit changes no code.

## What official has at `56aaba2`
- **No Live TV module:** no M3U, Xtream or Stalker source, no channel list, no EPG, no zapping.
- **Live playback plumbing (REUSE):** `LivePlaybackUiPolicy` treats Stremio type `channel` (and a player
  that reports a live window, latched) as live: the live timeline, no progress saving and
  `LivePlaybackWatchClock` for watch time. `PlayerRuntimeControllerErrorRecovery` retries
  `ERROR_CODE_BEHIND_LIVE_WINDOW` and HLS failures, and G4's dead-source failover
  (`advanceToNextLiveSource`) moves to the next add-on stream. The Exo extractor factory sets
  `FLAG_ENABLE_HDMV_DTS_AUDIO_STREAMS` and a 1500-packet TS timestamp search for every stream.
- **Frame rate:** the official AFR preflight downloads the start of the stream over extra connections
  before playback (up to 18 s); G5e's `TrackAfrPolicy` (feature 58) is the fork's single fallback owner:
  it matches from the track format, holding the start, and refuses a mid-playback switch (`TOO_LATE`).
- **LAN setup pages:** NanoHTTPD servers behind a per-session token for add-ons, repositories, posters
  and G6b's subtitle fonts (`fork/subtitles/SubtitleFontUploadServer`).
- **Credentials:** `fork/foundation/KeystoreCipher` (AES-GCM, non-exportable Android Keystore key), used
  by G7a (Seekr) and G9a (skip providers).
- **Networking:** the app's `OkHttpClient` logs requests at `BASIC` in debug builds, which prints full
  URLs.

## What the pinned fork adds (Reshaped)
About 7.4k lines in `reshaped/livetv` (data) and `ui/reshaped/livetv` (screens and player), plus
small hooks into the player, navigation and menu.

| Area | Reshaped @ 0ccf049 |
|---|---|
| Sources | `LiveTvSource` list (M3U link or imported file, Xtream, Stalker); same-identity source replaces the saved one; per-source load job, two sources at once, a failing source keeps its last channels (`sourceErrors`) |
| M3U | `parseM3uPlaylist`: line-streamed (no whole-file string), `#EXTM3U url-tvg`, `#EXTINF` attributes (the name may contain commas), `#EXTVLCOPT` / `#EXTHTTP` / Kodi `url\|header` headers, duplicate links and `#### Category ####` rows dropped, an HLS media playlist or a direct video link becomes one channel |
| Xtream | streaming JSON reader for categories and live streams; the panel's own `/live/…` link (never `direct_source`); TS unless the login reply allows only HLS; guide at `xmltv.php` |
| Stalker | handshake + profile, one session per portal (renewed once on failure), `get_all_channels` or ordered pages (four at a time, retried once; incomplete lists flagged), per-play `create_link`, portal logo paths |
| EPG | XMLTV pull parser over a gzip file on disk (100+ MB guides stay flat in memory); a window of past / upcoming programmes per channel (smaller on low-memory TVs); matched by `tvg-id`, else by normalised name; kept-guide cache; re-download every 10 h or on Refresh; failures retried after 30 min |
| Organisation | favorites, categories (rename, reorder, hide, hide all), hidden channels (64-bit key of source + category + name in a small file), search, logos (playlist, else the guide's) |
| Screen | category column, channel rows with now / next / progress / time left, recent channel, empty state, source dialog, category dialog, guide |
| Preview | a second, muted-or-not ExoPlayer for the focused channel after 400 ms; 720p cap on low-memory TVs, else 1080p |
| Player | overlay with banner, Now/Next card on OK (resolution / fps / codec / audio), channel panel and category column, D-pad Up/Down and CH+/CH− zapping inside the list the channel came from; a zap keeps the ExoPlayer (`keepPlayerForLiveTvZap`) |
| Playback | `LiveTvPlaybackRegistry` (the URLs Live TV sent to the player); for those only: MPEG-TS `FLAG_ALLOW_NON_IDR_KEYFRAMES`, HTTP refusals retried at 0.7 / 1.4 / 2.1 s, live-edge rejoin (three per minute), no disk cache, no AutoSync, no connection-speed learning, AFR preflight skipped and the rate taken from the track or measured from 48 frame timestamps |
| QR setup | `LiveTvSetupServer` (NanoHTTPD, random token path, Origin check, 16 KB form / 64 MB playlist caps) with a phone page for M3U link or file, Xtream and Stalker |
| Settings | Live TV menu entry (off by default), previews and preview sound (on) |

**Not inherited (security or ownership):**
- Xtream and Stalker passwords and M3U links (which often carry `username` / `password`) stored in
  plain `SharedPreferences`.
- Favorites and the last channel stored by stream URL. Xtream URLs contain the password.
- `Log.w(…, error)` with raw exceptions. An OkHttp exception text can hold the full request URL.
- A process-wide `object` repository keyed by an `Int` profile id.
- Its own AFR switcher beside G5e.
- A low-memory check separate from `AdaptiveResources`.

## Decision: one Live TV owner, isolated from VOD (D055)
- **Owner:** `fork/livetv` (Reshaped FILE_PORT, adapted).
  - A Hilt singleton repository, bound to the active profile.
  - Parsers and providers kept as ported: streaming M3U, Xtream and Stalker readers, and the XMLTV
    pull parser.
- **Storage:** per-profile `ProfileDataStoreFactory` store.
  - Each source record (the URL, users, passwords and MAC) is encrypted with `KeystoreCipher`.
  - Favorites, hidden channels, the category order and the last channel are keyed by non-secret
    hashes (source id + category + name), never by stream URL.
  - Imported playlists and guides are app-private files.
  - Credentials are never logged or shown back.
- **Networking:**
  - Live TV's own `OkHttpClient`, with no logging interceptor, because Xtream URLs carry credentials.
  - Logs name the source type and host only, never the URL or exception text.
  - Stalker's `Cookie` / `Authorization` go only to the portal host, never to another stream host.
- **Playback on the official player:**
  - A channel opens through the official player route as Stremio type `channel` (REUSE of
    `LivePlaybackUiPolicy`: live timeline, no progress).
  - Every live-only rule is gated on the Live TV playback registry, so add-on and VOD streams keep
    official behavior exactly. The live-only rules are:
    - non-IDR TS start;
    - HTTP-refusal retries;
    - live-edge rejoin;
    - player reuse on zap;
    - no disk cache;
    - no AutoSync;
    - no seek preview;
    - no speed learning.
- **AFR:** one owner (G5e `TrackAfrPolicy`), which gains a live branch.
  - For a Live TV channel, the official preflight probe is skipped, because it would open a second
    connection on one-connection accounts.
  - The rate comes from the track format, or is measured from frame timestamps.
  - A live channel accepts one early mid-playback switch, because a live stream has no start to hold.
- **Resources:** the preview budget comes from `AdaptiveResources`: on or off, maximum size and
  default.
  - Low-RAM devices default to no preview.
  - Low-RAM and constrained devices are capped at 720p.
  - One preview player at a time.
  - Guide windows follow the same tier.
- **Flag:** `LIVE_TV` becomes AUTO with the first code slice.
  - The Live TV menu entry stays off until the user turns it on (Reshaped's default), so nothing
    changes for users who never enable it.
  - OFF removes the entry and every live-only hook.

## Features
| ID | Behavior | Class | Result / plan |
|---|---|---|---|
| 208 | Live TV module | missing delta | `fork/livetv` owner, screen, menu entry → G10a / G10b |
| 209 | M3U playlists | missing delta | streamed parser, link or imported file → G10a |
| 210 | Xtream Codes | missing delta | streaming reader, panel links → G10a |
| 211 | Stalker Portal | missing delta | session, pages, per-play links → G10a |
| 212 | Multiple IPTV sources | missing delta | per-source jobs, failure isolation → G10a |
| 213 | Add source from phone via QR | missing delta | token-path LAN page (G6b server pattern) → G10g |
| 214 | EPG | missing delta | XMLTV pull parser, bounded window, cache → G10c |
| 215 | Now Playing | missing delta | → G10c |
| 216 | Next Program | missing delta | → G10c |
| 217 | Program progress bar | missing delta | → G10c |
| 218 | Time remaining | missing delta | → G10c |
| 219 | Favorites | missing delta | hashed keys → G10b |
| 220 | Categories | missing delta | → G10b |
| 221 | Reorder categories | missing delta | → G10b |
| 222 | Hide categories | missing delta | → G10b |
| 223 | Hide channels | missing delta | → G10b |
| 224 | Channel search | missing delta | remote-safe search → G10b |
| 225 | Channel logos | missing delta | playlist logo (G10b), guide logo fallback (G10c) |
| 226 | Channel preview | missing delta | one preview player → G10f |
| 227 | Low-resolution preview on constrained devices | missing delta | `AdaptiveResources` preview budget → G10f |
| 228 | Zap Up/Down | missing delta | → G10e |
| 229 | CH+/CH− | missing delta | → G10e |
| 230 | Channel list inside player | missing delta | → G10e |
| 231 | Category panel inside player | missing delta | → G10e |
| 232 | Now/Next card on OK | missing delta | → G10e |
| 233 | Improved Xtream TS/HLS handling | missing delta | allowed formats, panel links (G10a); playback rules (G10d) |
| 234 | Channel-start retry | PARTIAL_OVERLAP | official live-window retry and G4 failover; Live-TV-only HTTP refusal retries and live-edge rejoin → G10d |
| 235 | Faster MPEG-TS startup from first I-frame | missing delta | non-IDR keyframes for Live TV only → G10d |
| 236 | Live-TV-aware AFR | PARTIAL_OVERLAP | official preflight + G5e fallback; live branch on the G5e owner → G10d |

No ID is verified official: official has live-playback plumbing but no Live TV feature.

## Slice plan
- **G10a** (208 data, 209–212, 233 sources): models, M3U / Xtream / Stalker readers, encrypted
  per-profile storage, and the repository with per-source jobs and failure isolation. The feature flag
  is registered but not yet shown. Tests:
  - parser fixtures (attributes, headers, category rows, HLS / direct links);
  - Xtream / Stalker readers on recorded JSON;
  - one failing source leaves the others;
  - credentials only ciphertext at rest; hashed keys.
- **G10b** (208 UI, 219–225): Live TV screen, source dialog, menu entry (off by default), and playback
  through the official route as `channel`. Also favorites, categories (rename, reorder, hide), hidden
  channels, search and logos. Tests: filter / order / hide rules; route carries type `channel`.
- **G10c** (214–218, 225 guide logos): XMLTV guide, bounded window from `AdaptiveResources`, cache,
  now / next / progress / time left. Tests:
  - EPG present or absent;
  - name / id matching;
  - window bounds;
  - timestamp parsing.
- **G10d** (233–236 playback): registry-gated live rules (non-IDR TS start, HTTP-refusal retries,
  live-edge rejoin, no disk cache / AutoSync / seek preview / speed learning) and the live branch in
  G5e AFR with measured frame rate. Tests:
  - VOD URLs keep official policies;
  - retry schedule;
  - rejoin limit;
  - AFR live decisions;
  - frame-rate median.
- **G10e** (228–232): in-player overlay with banner, Now/Next card on OK, channel panel and categories,
  D-pad and CH± zapping in the list the channel came from, player reuse on zap. Tests: neighbour /
  wrap, zap list follows the picked folder, key mapping.
- **G10f** (226, 227): channel preview on one player, from the `AdaptiveResources` preview budget.
  Tests: budget per tier, one player at a time.
- **G10g** (213): QR / LAN setup page (token path, Origin check, size caps; credentials only in the POST
  body). Tests: token, Origin, size limits, invalid input.

Real providers, TV focus / zapping and AFR on a display are device checks. They stay MANUAL-PENDING in
`docs/HARDWARE_VALIDATION_TCL_C6K.md` for G14 (D047); no device run is claimed.
