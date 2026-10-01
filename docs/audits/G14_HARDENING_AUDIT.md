# G14 audit — Hardening, distribution and upstream

Evidence for `tasks/G14_HARDENING_RELEASE.md` (IDs 293–310, 312, 316 and 320) and for the
consolidated hardware certification. Official is the accepted baseline `5c1d9b0` (D059). Official
`dev` is now at `aeb6ee8`: eleven commits since `5c1d9b0`:
- libmpv failure causes on the error screen (`PlayerRuntimeControllerMpvEvents`,
  `MpvStartupWatchdogPolicy`, error recovery);
- Back after a binge-group skip no longer opens next-episode streams;
- stream-screen focus;
- an MDBList ratings disk cache;
- Polish / Hebrew strings, with the autosync strings moved into `strings.xml`.

They merge into integration with no conflicts. G14 is the release gate and changes the player error
path and logging, so an upstream sync PR precedes G14 code (the G12 pattern, D065). This audit
changes no code.

No fork source is pinned for G14 (source group `official+project`): every item is either official
behaviour that the fork keeps, or fork hardening of official code.

## What official already has at `5c1d9b0`
- **Self-hosted servers (293–298):**
  - `ServerDiscoveryService` fetches `<host>/.well-known/nuvio`:
    - HTTPS by default; an HTTPS → HTTP downgrade is refused;
    - no credentials or query in the URL;
    - a 64 KiB cap;
    - version, service, `self_hosted`, a publishable key and at least one auth capability are required;
    - the official backend is refused as "custom".
  - `ServerConnectionDialogs` shows a trust confirmation (server details and a warning) before
    switching, and a "Use official server" confirmation.
  - `ServerConfigurationStore.useOfficial()` drives the return path. `SupabaseModule` builds the
    client from the stored configuration (backend URL and publishable key = custom Supabase).
- **Credentials (299, 300):**
  - Android Keystore protects the profile lock (`ProtectedProfilePreferences`) and the Simkl tokens
    (`AndroidSimklAuthStorage`).
  - Trakt, debrid and MDBList tokens live in app-private DataStores, unencrypted at rest; the
    manifest has `allowBackup="false"`.
  - `ProfileManager.deleteProfile` runs every `ProfileScopedCredentialStore` in the multibinding set
    and clears the profile's DataStores.
  - Fork stores (Live TV sources, skip-provider keys, Seekr key) live in profile DataStores and
    encrypt with `KeystoreCipher`; the G13 AI vault joins the credential-store set.
- **Subtitle hosts (301, 302):**
  - AutoSync runs on the device. There is no subtitle-sync service: the stream's headers go only to
    the stream's own URL (embedded timeline loader), and subtitle downloads send only the subtitle's
    own headers (`AutoSyncSubtitleHttp`).
  - Sidecar subtitles in `MediaItem` go through `SubtitleRoutingDataSourceFactory`, so they never
    inherit the stream's data source, headers or cookies.
- **Local QR servers (303, 304) — gap:**
  - Official's five NanoHTTPD configuration servers serve fixed paths (`/`, `/api/...`) to anyone on
    the LAN, with no session token and no Origin or CSRF check: `AddonConfigServer`,
    `RepositoryConfigServer`, `CustomPosterConfigServer`, `DebridFormatterConfigServer`,
    `StreamBadgeConfigServer`.
  - `/api/addons` and `/api/state` return the installed add-on URLs, which can carry configuration
    keys. A cross-site page that knows the TV's address can POST changes; they still need on-TV
    confirmation.
  - The fork's own servers (`SubtitleFontUploadServer`, `LiveTvSetupServer`) already put a random
    per-session token in every path and refuse a foreign `Origin`.
- **Logging (305) — gap:**
  - `core/logging/urlForLog()` is an identity function, so every call site that "redacts" through it
    logs full URLs.
  - Official also logs add-on manifest and stream request URLs directly (`StreamRepositoryImpl`,
    `AddonRepositoryImpl`, `MetaRepositoryImpl`, `AutoSyncSidecarBridge`, `FrameRateUtils`, the
    torrent startup). Add-on URLs carry configuration and keys.
  - Sentry is compiled in but disabled without a DSN (the fork ships none). It already drops the
    request and the user from events.
- **Release (306–310, 312):**
  - ABI splits: armeabi-v7a, arm64-v8a, x86, x86_64, plus universal. `AbiSelector` picks the best
    asset for the device.
  - `UpdateChannel` (stable / beta, defaulting from the version), `ReleaseSelector` and
    `UpdateViewModel` / `ApkDownloader` / `ApkInstaller` in the `full` flavour.
  - The updater points at `BuildConfig.GITHUB_OWNER = "NuvioMedia"`. **The fork's updater offers
    official's releases**, which would replace the fork (or fail against its signer).
  - The downloaded APK is never checked against a digest. GitHub's release API reports a SHA-256
    `digest` per asset, and the DTO drops it.
  - `android-release.yml` (manual dispatch) runs only the updater tests before `assembleFullRelease`,
    publishes no checksum file, and signs with repository secrets (`NUVIO_RELEASE_*`) that only the
    maintainer can configure for the fork.
- **Upstream (316):** the fork's sync process is manual (UPSTREAM_SYNC, `upstream_observation`);
  no workflow watches official `dev`.

## Decision: keep official, close the gaps in one owner each (D064)
- **verified_official** (kept as shipped, no fork code):
  - 293–298 self-hosted server discovery, switching, custom Supabase, trust confirmation and return
    to official;
  - 300 profile-scoped secrets (credential-store set plus per-profile DataStores);
  - 301 no subtitle-sync service receives stream credentials (AutoSync is on-device);
  - 302 sidecar subtitles never inherit the stream's headers or cookies;
  - 307 ABI-specific APKs and on-device ABI selection.
- **G14a — local-server and log hardening** (`fork/security`, one owner):
  - **`LocalServerGuard`** for official's five servers. The QR URL carries a random per-session key
    (`/?k=<key>`). The first page load exchanges it for an `HttpOnly; SameSite=Strict` session
    cookie, and every other request must carry that cookie. A foreign `Origin` (or `Referer` when
    `Origin` is absent) is refused on every state-changing request.
  - The servers' 3,000 lines of official HTML and JS stay untouched (same-origin `fetch` sends the
    cookie). Each server gets a one-line guard call in `serve()` and its QR URL builder appends the key.
  - **Log redaction.** `urlForLog()` becomes scheme + host. The official sites above that log raw
    add-on or stream URLs route through it.
  - 299 is closed with the residual stated: Keystore covers official's profile lock and Simkl tokens
    and every fork-owned secret. The fork does not rewrite official's Trakt, debrid and MDBList
    DataStores: they are app-private, `allowBackup` is false, and a rewrite would conflict with every
    upstream sync. Encrypting them is proposed upstream instead.
  - Covers 299, 303, 304, 305. JVM-tested guard and redaction.
- **G14b — fork distribution** (`fork/distribution`, one owner, `full` flavour only):
  - The updater reads the fork repository from build config (`FORK_UPDATE_OWNER` / `_REPO`,
    defaulting to this repository), never official's.
  - **Digest check.** The asset DTO keeps GitHub's `digest`; the downloaded APK must match its
    SHA-256 before `ApkInstaller` runs. A missing or mismatched digest deletes the file and refuses.
  - **Channels.** Stable / beta keep official's `UpdateChannel` and `ReleaseSelector` over the
    fork's releases (pre-release = beta).
  - **Release workflow.** It runs the full unit suite, the governance validator and the
    known-failure guard before building, then publishes `SHA256SUMS.txt` beside the APKs.
  - Covers 306, 308, 309, 310, 312.
  - **Repository admin actions, recorded in GITHUB_ADMIN_CHECKLIST:** the release signing secrets
    and the first fork release. Only the maintainer can do these.
  - **Open product choice, asked when G14b starts:** keep `com.nuvio.tv` (replaces official, data
    continuity, needs uninstalling official because the signers differ) or a fork application id
    (installs beside official).
- **G14c — upstream watch:**
  - A scheduled workflow (daily plus manual) compares official `dev` with
    `current_official_baseline_sha`.
  - It lists the commits and the fork seams they touch in the job summary, and annotates when a sync
    is due.
  - It is read-only (`contents: read`), with no secrets and no automatic merges.
  - Covers 316.
- **320 (the end goal)** is closed last, with the 320-row accounting:
  - every row is terminal with evidence;
  - the IMPORT_LEDGER / license / dependency / sensitive-log audits are done;
  - TEST_MATRIX;
  - the hardware campaign below.

## Hardware certification (D047 final rule)
- **Accumulated:** 72 MANUAL-PENDING cases, HV-G1-1 … HV-G13-3:
  - G1: 3, G2: 3, G3: 3, G4: 9, G5: 10, G6: 5, G7: 5;
  - G8: 7, G9: 7, G10: 8, G11: 4, G12: 5, G13: 3.
- **Also required:** the G4 same-file A/B (`docs/HARDWARE_VALIDATION_TCL_C6K.md` §5).
- **Procedure:** one planned campaign on the TCL C6K plus the boxes each section names (2 GB,
  Android 11, Android 13+, an eARC receiver, two TVs and a phone for Watch Party).
  - Results are recorded only as real PASS / FAIL with attached evidence.
  - A FAIL reopens the feature for a focused fix and a re-test.
- **Who runs it:** only the maintainer, with the devices. Claude prepares the run sheet and records
  results; it never infers PASS from CI.
- **Stable is forbidden** while a release-blocking case is MANUAL-PENDING. After the code slices,
  G14 is therefore expected to sit BLOCKED on this campaign, with that as the only blocker.

## Feature map
| ID | Feature | Official | Decision |
|---|---|---|---|
| 293 | Self-hosted Nuvio server | `ServerDiscoveryService`, server dialogs | verified_official |
| 294 | `/.well-known/nuvio` discovery | `ServerDiscoveryPolicy` | verified_official |
| 295 | Custom backend switching | `ServerConnectionViewModel`, `ServerConfigurationStore` | verified_official |
| 296 | Custom Supabase configuration | discovery document → `SupabaseModule` | verified_official |
| 297 | Server trust confirmation | `ServerTrustConfirmationDialog` | verified_official |
| 298 | Easy return to official | "Use official server" + `useOfficial()` | verified_official |
| 299 | Keystore for sensitive credentials | profile lock, Simkl | G14a (fork secrets audited; official DataStores residual) |
| 300 | Profile-scoped secrets | credential-store set, per-profile DataStores | verified_official |
| 301 | No stream credentials to subtitle-sync services | on-device AutoSync | verified_official |
| 302 | No Cookie / Authorization to unrelated subtitle hosts | `SubtitleRoutingDataSourceFactory` | verified_official |
| 303 | QR local-server session tokens | none on official's servers | G14a |
| 304 | Cross-origin protection for QR pages | none on official's servers | G14a |
| 305 | No API keys / sensitive URLs in logs | `urlForLog` is identity | G14a |
| 306 | SHA-256 checks for release APKs | none | G14b |
| 307 | ABI-specific APKs | splits + `AbiSelector` | verified_official |
| 308 | In-app updater for the fork | updater points at official | G14b |
| 309 | Stable channel | `UpdateChannel.STABLE` (official releases) | G14b |
| 310 | Beta channel | `UpdateChannel.BETA` (official releases) | G14b |
| 312 | Tests before APK release | updater tests only | G14b |
| 316 | Automated upstream sync checks | none | G14c |
| 320 | One Nuvio distribution (end goal) | — | final accounting after the campaign |

## Slices
- **Sync** (D065): official `dev` `aeb6ee8` into integration, with clean `[baseline-audit]` replays.
- **G14a** (299, 303–305): `LocalServerGuard` on official's five servers; host-only `urlForLog` and
  routing of the raw-URL log sites.
- **G14b** (306, 308–310, 312): the fork updater source, digest-checked install, release workflow
  tests and checksums.
- **G14c** (316): the upstream watch workflow.
- **Closeout:** the 320-row accounting (320), the hardware run sheet, and G14 BLOCKED on the
  campaign.

No device run is claimed. Repository settings (signing secrets, branch protection, default branch)
are maintainer actions.
