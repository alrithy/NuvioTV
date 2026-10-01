# G11 audit — Watch Party

Evidence for `tasks/G11_WATCH_PARTY.md` (IDs 237–249). Official = accepted `56aaba2` (D050). Official
`dev` is at `5c1d9b0` (observed for G10; landscape posters, strings, hero focus) and has no Watch Party
seam either. Source (SOURCE_MAP pin): AntoninoScardina/NuvioTV branch `watchparty` @
`ff597b12bc7834b07dedda92c88e0d574c0aad81` (GPL-3.0, same license as Nuvio). The whole feature is one
commit there, `58530279d10f3378c237c248ce3ca7bdde5e13cd` ("Add Watch Party: synced playback between
devices"). No other pinned source has Watch Party code. This audit changes no code.

## What official has at `56aaba2`
- **No Watch Party:** no room, no peer transport, no WebView-based networking, no remote control of
  playback. `git grep` for watch party / WebRTC finds nothing in `app/src/main` (also at `5c1d9b0`).
- **Player controls it can drive (REUSE):** `PlayerRuntimeController` already exposes everything a
  sync engine needs: `currentPlaybackPositionMs`, `hasActivePlayIntent`, `seekPlaybackTo`,
  `setPlaybackPaused`, `setPlaybackSpeedInternal` (pitch kept), `currentPlaybackDurationMs`, the
  pause overlay, progress and watch-progress jobs, `isTorrentStream`, `currentStreamUrl` and
  `currentHeaders`, for ExoPlayer and mpv alike.
- **Player route:** `Screen.Player.createRoute` (URL, headers, metadata) opens any HTTP stream.
- **Stream reuse:** `persistSelectedStreamForReuse` saves the last played link and its headers per
  content (`streamLinkCacheDataStore`) when "reuse last link" is on.
- **Fork owners that touch this:** `FeatureId.WATCH_PARTY` exists (unregistered, so OFF); G10's
  Live TV playback registry marks account-bound Live TV URLs; `docs/SECURITY_POLICY.md` already
  requires consent before sharing a resolved URL / headers, redaction, no persistence and encrypted
  transport.

## What the pinned fork adds (Antonino @ ff597b1)
About 1.2k lines of Kotlin plus a bundled SDK.

| Area | Antonino @ ff597b1 |
|---|---|
| Room | `WatchPartyProtocol`: six-character code from a 31-symbol alphabet without look-alikes; room `nuviowatchparty<code>`, password `nuvio-wp-<code>`; `normalizeCode` accepts spaces / lower case |
| Transport | `WebViewWatchPartyTransport`: a hidden WebView with an https base origin (secure context for `crypto.subtle`) runs the bundled VDO.Ninja SDK and `watchparty-bridge.js`; data channel only (no audio / video); a `@JavascriptInterface` bridge posts events to the main thread; leave waits 1.5 s so peers get BYE |
| SDK | `assets/watchparty/vdoninja-sdk.min.js` VDO.Ninja SDK v1.6.1 (MPL-2.0, Steve Seguin), 131 759 bytes, SHA-256 `aef88ddd93f0f8a6a2c020b751029998d2dc984b64cb03d32c69b606fd3b3f12` — byte-identical to the upstream npm release `@vdoninja/sdk@1.6.1` — with `LICENSE-vdoninja-sdk.txt`. Signaling `wss://wss.vdo.ninja` (encrypted with the room password, AES-CBC), STUN Cloudflare / Google, TURN relays `turn-*.vdo.ninja` / `turn.obs.ninja`; peer data over the WebRTC data channel (DTLS) |
| Protocol | one JSON message type: HELLO (name, host), MEDIA (host → guest: URL, headers, metadata, position, playing), STATE (periodic and on change), REQUEST_STATE, CMD (guest → host: play / pause / seek), BYE |
| Sync | host is the authority; guests' play / pause / seek go to the host as CMD and come back as STATE; state every 2 s and on change; local actions detected by polling (500 ms, engine-independent); drift > 3 s → seek (with a learned lead for the seek's own delay, up to 8 s), 0.4–3 s while playing → speed change of up to ±10 % (stops under 0.12 s), settle / suppress windows against echo |
| Shareable | HTTP(S) only; torrent, local proxy (`127.0.0.1` / `localhost`) refused |
| UI | player button (Groups icon) → panel: create room, code shown as `ABC DEF`, participants, leave; badge while in a room; Settings → Watch Party → join screen with a code field; a NavHost effect opens the player on the host's stream (`startFromBeginning`) |
| Strings | en, it |

**Not inherited (security, privacy or ownership):**
- **No consent:** creating a room sends the current URL and *all* headers to every peer that joins,
  and keeps sending each new stream.
- **All headers:** `currentHeaders` can include `Authorization`, `Cookie` and add-on / debrid keys;
  a `user:password@` link becomes an `Authorization` header in the player.
- **Logging:** every WebView console line is written with `Log.d` (the SDK logs signaling detail);
  transport errors are shown and stored verbatim.
- **Code randomness:** `String.random()` (not a secure generator) for the only room secret.
- **Guest persistence:** the guest opens the host's link through the normal player route, so
  "reuse last link" saves the host's URL and headers on the guest device.
- **Account-bound sources:** Live TV channels (Xtream links carry the user's password; Stalker needs
  portal cookies) would be shared like any HTTP stream.
- A process-wide session whose WebView survives the player.
- No flag: always on.

## Decision: one Watch Party owner, consent first, nothing secret leaves (D056)
- **Owner:** `fork/watchparty` (Antonino FILE_PORT, adapted).
  - Protocol, session and sync engine kept as ported (thresholds above), as plain Kotlin behind
    `WatchPartyTransport` and `WatchPartyPlayer` so the engine is JVM-tested with fakes.
  - Transport: the hidden WebView and bridge, unchanged SDK file and license text, the WebView
    created on join and destroyed on leave or when the app leaves the foreground for long.
- **Consent (243):** nothing is shared until the host confirms a dialog that names what goes to the
  people who join: the stream link and the headers needed to play it. Consent lasts for that room;
  leaving the room ends it. A guest is told the link comes from the host before playback starts.
- **Required headers only (244):** a pure `WatchPartySharePolicy` decides:
  - shared: `User-Agent`, `Referer`, `Origin`, `Accept`, `Accept-Language` only;
  - never shared: credentials (`Authorization`, `Cookie`, `Proxy-Authorization`, any `*key*`,
    `*token*`, `*secret*`, `*auth*` header) and a `user:password@` link;
  - a stream that carries one is **not shareable** (the panel says why) rather than shared without
    it, so a stream never half-works for guests;
  - not shareable either: torrents, loopback / local links, non-HTTP(S), Live TV channels (the
    G10 playback registry; account-bound, one connection).
- **Received media (guest):** validated (HTTP(S), header allowlist, size caps on every field),
  memory only; the guest's player is told it is a Watch Party stream, so it does not persist the
  link for reuse and the official reuse path is skipped for it. Watch progress and history follow
  the content id as for any playback.
- **Logs (249):** no WebView console logging; transport errors become fixed codes; nothing logs
  URLs, headers, codes or peer names.
- **Compatibility (240):** the phone side is Antonino's separate NuvioMobile build. To stay
  interoperable the wire format (protocol v1, message fields, `app: nuvio-watchparty`), the code
  alphabet and the room / password derivation are kept exactly; hardening only narrows what this app
  sends and accepts.
- **Room secret:** codes from `SecureRandom` (31^6 ≈ 8.9 × 10^8); the password stays derived from the
  code (the code is the shared secret, as in the source) with signaling encrypted by the SDK and
  peer data over DTLS (245, 246). Rooms exist only while someone is in them.
- **Sync (241, 242, 247, 248):** host authority, CMD / STATE as ported; speed correction uses the
  official `setPlaybackSpeedInternal` (pitch kept); the engine pauses corrections while the player
  buffers or a seek settles.
- **Flag:** `WATCH_PARTY` becomes AUTO with the first code slice; OFF hides the player button and
  the Settings entry and never creates a WebView.
- **License:** the SDK is taken from its upstream release (`@vdoninja/sdk@1.6.1`, not from the
  fork) in Source Code Form (`vdoninja-sdk.js`, which satisfies MPL-2.0's source requirement directly),
  unmodified, MPL-2.0 header kept, license text beside it; `docs/LICENSE_AND_ATTRIBUTION.md` and
  IMPORT_LEDGER name it (file-level MPL, larger work GPL-3.0). Its license grants no right to the
  hosted signaling / STUN / TURN services (VDO.Ninja's terms apply) and no trademark use.

## Feature map

| ID | Feature | Official | Decision |
|---|---|---|---|
| 237 | Watch Party | missing | `fork/watchparty` → G11a / G11b |
| 238 | Six-character room code | missing | secure code + normalisation → G11a |
| 239 | TV ↔ TV | missing | same app on both ends → G11a / G11b |
| 240 | TV ↔ Android phone | missing | wire-compatible with the Nuvio Party phone build (AntoninoScardina/NuvioMobile, outside this repo): same protocol v1, room / password derivation, alphabet and `app` tag → G11a |
| 241 | Play / pause sync | missing | CMD / STATE → G11a |
| 242 | Seek sync | missing | CMD / STATE, seek lead → G11a |
| 243 | Share URL only with permission | missing | host consent dialog per room → G11b (policy G11a) |
| 244 | Only required headers, handled securely | missing | allowlist, credential streams not shareable, guest memory-only → G11a |
| 245 | WebRTC P2P | missing | VDO.Ninja SDK data channel → G11a |
| 246 | Encrypted data channel | missing | password-encrypted signaling + DTLS → G11a |
| 247 | Soft drift correction | missing | speed ±10 % with pitch kept → G11a |
| 248 | Hard seek for large drift | missing | > 3 s → seek with learned lead → G11a |
| 249 | Never log sensitive URLs / headers | missing | no console log, fixed error codes → G11a |

## Slices
- **G11a** (237–242, 244–249): `fork/watchparty` core — protocol, secure code, share
  policy, received-media validation, session / sync engine, WebView transport and bridge, SDK asset
  and license, Hilt module, flag AUTO. Tests: code generation and normalisation, share policy
  (allowlist, credentials, loopback, torrent, Live TV, user-info), received-media validation and
  caps, sync engine with a fake player and transport (play / pause / seek commands, STATE apply,
  soft speed band, hard-seek threshold and lead, settle / suppress, echo), lifecycle (leave clears
  state, BYE, stale transport events ignored), no-log redaction of transport errors.
- **G11b** (243, and the UI of 237–240): player button, consent dialog, panel and badge, join
  screen in Settings, NavHost effect, guest notice and no-reuse wiring; strings en + ar. Tests:
  consent gate (no MEDIA before consent), guest playback marked no-reuse.

Two real devices on a network, a phone, NAT / TURN fallback and drift on weak TVs are device checks
(HV-G11-1..HV-G11-4, G14). No device run is claimed.
