# Architecture & Product Decision Log

This file records the decisions that must survive across coding agents and chat sessions.

## D001 — Base
Use official `NuvioMedia/NuvioTV:dev` as the base. Current pinned baseline is:
`fd7973d91dd75d790c5f9b3d68dae652655e92c4`.

Initial governance work began at `c257a2365ee3386b582dc2974ec235cfe0381f33`, then the baseline was refreshed before G0 to `fd7973d91dd75d790c5f9b3d68dae652655e92c4`. See `docs/UPSTREAM_SYNC_LOG.md`.

Reason: forks overlap, diverge and are based on different upstream points. The official dev branch is the integration anchor.

## D002 — Reuse proven fork code first
We are intentionally building a "best of Nuvio forks" integration project.

Order of preference:
REUSE -> CHERRY-PICK -> FILE_PORT -> DELTA_PORT -> ALGORITHM_PORT -> ADAPTER -> REWRITE LAST.

Do not rewrite a working feature merely for stylistic consistency.

## D003 — Never merge a whole fork
Import coherent features/commits/files, not an entire fork branch. Whole-fork merges make future upstream sync and conflict attribution unmanageable.

## D004 — Current official code wins when functionality has converged upstream
Before importing any fork feature, diff against the current official implementation. If official already contains the capability, import only the missing delta.

Known examples already present in official at baseline:
- ParallelRangeDataSource
- StreamSpeedTester
- libdovi / DV-related work
- PlayerDebugStatsOverlay
- MDBList support
- custom server / discovery infrastructure

## D005 — Preserve official fallback
For core playback/network/audio paths, official behavior remains available as fallback or selectable strategy wherever practical.

## D006 — Playback architecture
Do not blindly stack Reshaped disk seek-buffer behavior and ysosrs parallel REMUX behavior.

Expose distinct strategies:
- Official
- REMUX / Throughput
- Seek Optimized
- Low Memory
- Auto

Auto may select based on RAM, bitrate, file size, network and device capability.

## D007 — Subtitle architecture
Do not run multiple independent AutoSync systems.

Build one Subtitle Intelligence pipeline:
1. embedded reference
2. hash/release-matched reference
3. cue-rhythm alignment
4. audio/ASR fallback
5. original timing + manual offset

Generalize VibeSubtitle ideas beyond Spanish, including Arabic.

## D008 — Seek architecture
Hybrid order:
1. local exact/keyframe preview
2. calibrated Seekr fallback
3. normal seek bar fallback

Bound memory/disk usage through the resource manager.

## D009 — Skip architecture
One multi-provider aggregator behind the existing official skip-repository integration point. Keep conflicting actions (skip vs mute) distinct.

## D010 — Adaptive resource manager
Do not create a permanently feature-deleted Lite edition.

Harvest Lite's RAM/device-tier logic into one Adaptive Resource Manager used by playback, seek previews, poster loading, Live TV previews and other expensive subsystems.

## D011 — Diagnostics
One normalized diagnostics model and one Stats for Nerds UI owner. Extend official `PlayerDebugStatsOverlay`; do not create competing overlays.

## D012 — Stream ranking
The ranker should consider:
availability/cache -> resolution -> release-group quality -> source reliability -> codec -> HDR/DV -> audio -> bitrate -> connection fit -> file size,
while preserving addon order when practical.

Product defaults:
- cached Real-Debrid first
- uncached still visible
- 4K > 1080p in quality-first mode
- REMUX preferred
- WEB-DL fallback
- DV/HDR preferred when supported
- TrueHD Atmos/lossless preferred when supported

## D013 — Progressive AIOStreams
Capability-detect a progressive endpoint. If unsupported, fall back cleanly to the normal manifest/stream path. Do not require a custom AIOStreams server for basic playback.

## D014 — Live TV
Primary source is Reshaped. Target scope includes M3U, Xtream, Stalker, multiple sources, QR setup, EPG, categories, favorites, hidden/reordered channels, search, preview, zapping, Now/Next and live-aware retries/AFR.

## D015 — Watch Party
Primary source is AntoninoScardina/NuvioTV.

Security additions required:
- explicit permission before sharing resolved URL/headers
- never persist sensitive URLs/headers
- redact logs
- clean room/session lifecycle
- keep encrypted transport semantics

## D016 — UI
Keep official layouts. Glass, Pill, top-nav and other fork UI ideas become selectable styles/layouts, not replacements that remove official choices.

## D017 — AI
Fornace AI-provider work is Experimental and OFF by default. Keep provider APK separation, BYOK and verification model.

## D018 — MAT
Kodi-style MAT/IEC61937 work remains Experimental and OFF by default until validated on target hardware.

## D019 — Do not import reverted Smart Vibrance as stable
Cxsmo Smart Vibrance experiments were explicitly reverted. Do not include in stable core. Only revisit as a new experimental project with fresh validation.

## D020 — Do not inherit ysosrs feature removals
Do not adopt fork-specific removals such as Modern-only UI, removal of updater, or removal of IAMF/MPEG-H merely because they exist in that fork.

## D021 — Do not re-port obsolete Enhanced self-host implementation
Current official dev already contains custom-server infrastructure. Use official implementation.

## D022 — "Tested in a fork" is not the same as integration-tested
We trust proven source implementations enough to reuse them, but every imported feature must be regression-tested in the combined app.

## D023 — Source pinning
All fork sources are pinned to repo + branch + SHA in `docs/SOURCE_MAP.md`. Agents should not silently pull a moving branch head during a port. If a newer source is intentionally adopted, update SOURCE_MAP and record the decision.

## D024 — Multi-agent workflow
GitHub is the source of truth. Codex, Claude Code, Gemini or another agent may work on the project. No agent may depend on hidden chat context. Read AGENTS.md and docs/HANDOFF.md first.

## D025 — Branch ownership
Never develop directly on `superfork/integration`.
One active task/feature branch per coherent work item.
Exactly one writer may edit a task branch. Parallel read-only review is allowed; another writer needs a separately assigned branch and scope.

## D026 — Handoff
Before changing agents, commit completed work and update `docs/HANDOFF.md` with branch, SHA, tests, unresolved issues and exact next task.

## D027 — Stability
The goal is to include the full desired feature set over time, but stability wins over enabling everything simultaneously. Experimental/high-risk systems may remain disabled until validated.

## D028 — Licensing / attribution
Preserve GPL-3.0 obligations and source attribution for imported code. Record source repo, SHA/commits and imported files in the import ledger. Preserve third-party notices such as VDO.Ninja/MPL obligations where applicable.

## D029 — User-facing simplicity
Expose simple defaults for normal users and advanced controls for expert users. Internal complexity should not force every user to tune dozens of options.

## D030 — Zero-prompt agent behavior
When the user says only "اشتغل على نوفيو" / "work on Nuvio" / "continue Nuvio", the coding agent should inspect repository state and continue the current/next documented gate rather than asking the user to restate the project.

## D031 — Full-suite baseline debt uses a no-new-regressions guard
The official-based baseline currently exposes 18 failures when the entire `:app:testFullDebugUnitTest` suite is run, even though official PR CI is green because it gates a narrower updater-focused subset plus build.

Decision:
- run the full unit suite on every Superfork PR;
- record the 18 known failures in `integration/baseline_test_debt.json`;
- fail CI on any unrecorded/new failure;
- allow known failures to remain temporarily;
- remove debt entries once fixed.

This is documented in `docs/BASELINE_TEST_DEBT.md`.

## D032 — Pre-Superfork PRs are reference sources, not merge candidates
Legacy PR #1 (Cinema View) and PR #2 (Prototype Hub) target old `dev` and predate the current architecture.

Decision:
- do not merge them directly into `superfork/integration`;
- optionally harvest selected G12 ideas through the normal current-baseline porting protocol.

## D033 — Dependency and permission expansion require explicit review
Imported fork code may not silently add binary artifacts, SDKs, Android permissions, telemetry, or globally relaxed network security.

Use `docs/DEPENDENCY_POLICY.md`. New permissions/dependencies must be justified by feature IDs/gate, audited for license/security, and scoped to the narrowest trust boundary.

## D034 — Persisted settings require backward-compatible migration discipline
Fork features must not casually overwrite/rename official settings or leak profile data. New settings use safe explicit defaults; secrets remain Keystore-backed where required.

Use `docs/DATA_MIGRATION_POLICY.md`.

## D035 — Product priority stack resolves trade-offs
When two technically valid implementations conflict, use `docs/PRODUCT_REQUIREMENTS.md`:
P0 stability/security/upstream/data integrity, then P1 playback/subtitle/remote reliability, followed by capability expansion, experience/social, then experimental systems.

Durable exceptions require a new decision record.

## D036 — Freeze the implementation baseline within a gate
Before G0 starts, take one final reviewed refresh from official `dev`. Once a gate is active, do not continuously chase upstream inside that feature branch.

Official updates use dedicated upstream-sync PRs, normally at gate boundaries, when the affected subsystem materially changed, or for critical fixes. This preserves reproducibility while keeping long-term drift controlled.

## D037 — Governance closure keeps accepted and observed upstream distinct
During the 2026-09-28 audit official dev moved to e78de241acb8a6128c29076422377de1206ee8cd
(settings PR #3746, 65 changed files including build/tests). This governance-only task
retains the latest accepted fd7973d sync; no runtime merge is hidden inside governance.
The final reviewed pre-G0 anchor remains fd7973d. G0 foundation does not touch settings;
review the observed upstream delta through a dedicated sync before a settings-touching gate.
D036 means latest **reviewed/accepted** official anchor, not an uncontrolled moving HEAD.

## D038 — Fail-closed baseline debt and singular governance owners
Use GOVERNANCE_OWNERS and BASELINE_TEST_DEBT. Exact fully qualified IDs, fresh complete
execution, inventory checks and explicit reviewed baseline changes replace simple-name
allowlists/count locks/catch-alls. One writer lease guards each task branch. No G0 feature
is marked implemented by this governance-only review.

## D039 — Explicit initial classification of the 19th baseline failure
Clean official replay 36488330126 / job 109150571733 executed 1,611 tests and failed 19.
The new guard correctly rejected HomeEnrichmentRepositoryBoundaryTest#`a real repository
transport failure is retried and then resolves`; clean integration replay had 18 failures
and this test passed. Test/build inputs are identical. Its source uses wall-clock delays,
Unconfined coroutines and a 5-second request wait; scheduling sensitivity is a hypothesis,
not a proven root cause. This failure is observed before any Superfork feature code.

During the explicitly requested baseline audit, register its fully qualified ID as
intermittent baseline debt with both results preserved. This is a reviewed, explicit
initial registration of 19 entries (18 reproduced + 1 intermittent), not automatic growth.
Repeat official replay once to characterize it; do not discard the failing evidence or
use reruns to claim the test is fixed. Future additions require exact-head maintainer review.

## D040 — Adopt official 71632b9 before G1 and review one renamed baseline test
Official dev moved `fd7973d..71632b9` with settings reorganization (#3746) and player
controller changes that G1 depends on, so D036/D037 require a reviewed sync before G1 coding.
Merged via PR #9 without conflicts or fork changes.

Clean replays of official `71632b9` and the integration merge found zero new failures and the
same 18 reproduced debts (HomeEnrichment intermittent case passed; it stays registered as
intermittent until the removal rule is met). One reviewed-inventory case disappeared:
`com.nuvio.tv.core.debrid.TorboxFileSelectorTest#selects file by torbox file id first`. Upstream commit `71632b9` deliberately replaced it with
`does not treat torrent index as torbox file id` (same fixture, inverted expectation: a torrent
index must not be treated as a TorBox file id) plus two new selector tests. This is an official
behavior correction with replacement coverage, not a lost regression test. The reviewed
inventory is replaced with the clean official replay; the suite floor rises 1,611 → 1,752.
Debt entries keep their original registration SHA/date. Exact-head maintainer approval is
required by the Baseline Change policy for this inventory removal.

## D041 — Passive diagnostics run by default; add-on retry belongs to stream intelligence
`UNIFIED_DIAGNOSTICS` defaults to `AUTO` in `FeatureRegistry.DECIDED_DEFAULTS`. G1 diagnostics
are passive and read-only: they observe outcomes of requests the app already makes, keep state
in memory, add no network traffic, persistence or logging of URLs/headers, and only add status
text to existing official screens. Every other group stays `OFF`; experimental groups can never
receive a decided default (FeatureRegistryTest).

Feature 289 (add-on retry policy) is deferred to G8. The pinned ysosrs source has no retry: its
breaker only changes the displayed level, and official `safeApiCall` returns the first error.
Retrying add-on requests changes network behavior and belongs to the stream/failover owner, not
to a diagnostics gate.

## D042 — Adaptive resources run by default and only tighten limits on weak devices
`ADAPTIVE_RESOURCE_MANAGER` defaults to `AUTO` in `FeatureRegistry.DECIDED_DEFAULTS`. The tier is
physical RAM (Lite cuts: ≤1600 MB low-RAM, ≤2560 MB constrained; `isLowRamDevice` and unreadable
RAM count as low-RAM). On the standard tier every value is the official one, so strong devices
are never capped; weak-device cuts are `min()` clamps that never raise an official value. Comfort
cuts (poster cache share, animated posters, revalidation, decode parallelism, post-play prefetch,
catalog fan-out) key on low-RAM; allocation-safety limits key on constrained. `OFF` restores
official values everywhere. Lite edition switches (`liteMode`) and Lite's cap of 8 fetches on
strong devices are not imported. Playback budgets change in G2b: constrained devices join official's low tier, with a 250 MB
Java-heap buffer ceiling and at most 4 session connections; stored settings are not rewritten.

## D043 — Playback strategies are selectable; Official stays the default
`PLAYBACK_STRATEGY_ENGINE` defaults to `AUTO` in `FeatureRegistry.DECIDED_DEFAULTS`, which only makes
the strategy selector visible. The stored per-profile selection defaults to Official, so official
playback is unchanged until a user picks another strategy. Strategies are session-only overrides of
official buffer/network settings (never written back, never the MPV path or the global performance
engine); unsupported combinations fall back to Official with a reason shown in the HUD. Auto uses
device tier and file facts; a measured network signal is left to G4.

## D044 — G4 playback recovery runs by default; network changes stay opt-in
`REMUX_PERFORMANCE` defaults to `AUTO` in `FeatureRegistry.DECIDED_DEFAULTS` for the G4 recovery
paths: dead-source failover (HTTP 404/410, non-media body, and mid-play malformed/IO errors after
official same-URL retries) and the startup watchdog. Both replace a dead-end error screen or an
endless spinner; failover keeps the user's source order, never revisits a dead URL and is capped at
3 per chain. Deliberately not dead: HTTP 429 and timeouts. G4 changes to network transfer behavior
(ParallelRangeDataSource refinements, disk seek buffer) apply only when a G3 strategy selects them.
OFF restores official error handling. G4 is not DONE until the same-file A/B on the TCL C6K runs.

## D045 — Seek optimized: one seek mechanism per stream, sized by tier
The Seek optimized strategy (D006) adds, only off the parallel path and only for remote progressive
http(s) streams: ysosrs MP4 session mode (single connection, 8 MiB chunks) for MP4, and the
Reshaped disk read-ahead ring for everything else. Never both on one stream (a ring thrashes on MP4
scatter reads) and never with parallel REMUX. Ring size comes from the G2 tier: 512 MB standard,
256 MB constrained, off on low-RAM (Reshaped shipped it off by default because full-speed disk
writes are heavy on weak TVs); it also keeps half of free storage and a 1 GiB reserve. The
official VOD disk cache stays as the strategy already sets it (it keeps what was played; the ring
holds what comes next). No separate settings screen: the strategy selector is the only switch.

## D046 — REMUX_PERFORMANCE stays AUTO before the hardware A/B (AR-003)
Reviewed against the stability-first alternative (OFF until the TCL C6K A/B passes). Kept `AUTO`:
every default-on G4 path engages only where official already failed or hung. Dead-source failover
runs after official retries and shows the error when it cannot help; the startup watchdog never
stops the player and retracts its error on a late first frame; MKV resync runs only on malformed
cluster data that official turns into a playback error, with official truncated-tail handling
first. The network-transfer changes (G4b warm-up, G4d MP4 session and read-ahead) stay behind the
strategy selection, whose default is Official (D043). If the A/B shows a regression in any
default-on path, that path's default goes to OFF in a follow-up decision.

## D047 — Hardware validation is batched into G14
Record of the user decision of 2026-09-29, implemented by `9c627ca` (`validation_policy` in
`integration/state.yaml`, AGENTS.md "Manual hardware validation", DEFINITION_OF_DONE, STATUS_MODEL,
GATE_SPECS, G14 task packet). Development of G1-G13 no longer stops for device-only checks: a gate
advances when its code is merged and automated CI/DoD is green, and each device check stays
MANUAL-PENDING with its exact expected evidence in `docs/HARDWARE_VALIDATION_TCL_C6K.md`. All of
them run once in the G14 hardware-certification campaign before any Stable release claim; a FAIL
there reopens the owning gate's work, and default-on paths follow the D046 rollback rule. No PASS
is ever recorded without a real run.

## D048 — AUDIO_DV_AFR runs by default; each slice keeps official output unless limited
`AUDIO_DV_AFR` defaults to `AUTO` in `FeatureRegistry.DECIDED_DEFAULTS` so G5 behavior reaches
users without a hidden switch. Each G5 slice must keep official audio/video output unless its change
is limited to a user-chosen path (for example soft clipping only while amplification is above 0 dB),
a failure/fallback path, or a per-format control whose default equals official behavior. Anything
that changes output for every playback (for example claiming a new passthrough format) ships behind
an explicit setting that defaults to official. OFF restores official G5 behavior; device evidence is
batched into G14 (D047).

## D049 — G6 has one subtitle sync engine, behind an AutoSync setting (superseded by D050)
The only automatic subtitle timing engine is an ALGORITHM_PORT of VibeSubtitle `SubtitleCueAligner`
(language-independent cue-rhythm anchors → offset, clock scale, piecewise segments, confidence;
null = fail closed) in `fork/subtitles`, fed by one reference pipeline: embedded reference →
hash / same-release add-on reference → alignment → original timing + manual offset. Reshaped
`autosync/*` is harvested only as bounded pieces ported into that engine, never run beside it.
Automatic retiming changes what every playback shows, so it runs only while a per-profile AutoSync
setting is on (default off = official timing); FeatureId.SUBTITLE_INTELLIGENCE becomes AUTO with
the first G6 code slice so the setting is visible, and OFF hides it. The official manual cue pick
and delay always take priority. Audio/ASR sync (92, 93) is deferred (audit).
Superseded by D050 before any engine code: official AutoSync is the engine; the reference
pipeline order above survives only as G6a's no-embedded-reference fallback inside official AutoSync.

## D050 — Adopt official 56aaba2 before G6: official Subtitle AutoSync becomes the G6 engine
Official `dev` moved `71632b9..56aaba2` and merged Subtitle AutoSync (PR #3703, the Reshaped
AutoSync by its author) — the subsystem G6 was about to build. UPSTREAM_SYNC's gate-start rule and
feature-convergence rule (prefer official ownership, migrate only our unique delta) require a
reviewed sync first. Merged with one resolved conflict (extractor factory: fork DV factory wrapped
by the official AutoSync factory). Official AutoSync defaults off. This revises D049: the single
G6 sync engine is **official AutoSync** (REUSE); no fork `fork/subtitles` engine is built and the
VibeSubtitle aligner / PR #38 engine are not imported, so there is never a second engine. G6
continues with only the gaps official still lacks (re-audited on this baseline).

## D051 — SUBTITLE_INTELLIGENCE runs by default; it only extends official AutoSync
G6 adds to official AutoSync (D050), which itself stays off until the user turns on Auto Sync
Subtitles. Each G6 addition is limited to a failure path of that user choice (G6a: the file has no
embedded subtitle reference, where official gives up) or to presentation that defaults to official
(G6b fonts/preset), so SUBTITLE_INTELLIGENCE is AUTO in `FeatureRegistry.DECIDED_DEFAULTS`; OFF
restores official AutoSync exactly. Device evidence is batched into G14 (D047).

## D052 — G7 has one seek-preview engine: local keyframes, then calibrated Seekr, then normal seek
The only seek-preview pipeline is one `SeekPreviewTrack` chain in `fork/seek`: thumbnails from the
keyframes playback already downloads (Reshaped `seekpreview/local`, FILE_PORT; software decoder
only, no extra requests), Seekr sprite thumbnails only for slots without a local frame (Reshaped
memory-bounded track, closing G2's 279), and otherwise the official scrubber. Seekr calibration is
Cxsmo's estimator (ALGORITHM_PORT) run on the local keyframe thumbnails, never by seeking the live
player. The Seekr key is the user's own, AES-GCM encrypted per profile with an Android Keystore key
(Cxsmo store); no key is built into the APK. SEEK_INTELLIGENCE becomes AUTO with the first code
slice because previews only appear while the user scrubs; memory and disk budgets come from
AdaptiveResources. OFF restores the official scrubber exactly.

## D053 — G8 has one stream ranking engine, built on official stream facts
The only ranker is `fork/streams`, using official `DirectDebridStreamFilter` facts and
`DebridStreamPreferences` (REUSE, no second parser), with the ranking chain of Cxsmo
`StreamQualityRank` (ALGORITHM_PORT): cached debrid first (uncached stay visible), resolution,
quality (REMUX first), release-group ladder, HDR/DV, lossless audio, channels, encode, bitrate,
source reliability (G1 add-on health); stable, so add-on order survives every tie. The same ranker
orders the lists when the user picks a "Best quality" order and chooses the stream for a new
"Best quality" autoplay mode; official orders and modes remain the defaults. Reshaped connection fit
demotes unsustainable files in stable order; progressive AIOStreams is capability-detected with a
clean fallback; add-on retry (289) is one bounded retry inside the scrape timeout.
STREAM_INTELLIGENCE becomes AUTO with the first code slice; OFF restores official behavior.

## D054 — G9 has one skip aggregator: official `SkipIntroRepository`, extended
Official `SkipIntroRepository` stays the only skip entry point and keeps IntroDB, AniSkip and
Anime-Skip (Cxsmo's rewrite removes the anime providers; not inherited). Cxsmo's extra providers
(SkipMe.db, TheIntroDB, PublicMetaDB, MovieHavenDB, VideoSkip, NotScare) run beside them in
`fork/skip`, each with its own timeout, and results merge by Cxsmo's confidence-weighted evidence
merge, so one slow or failing provider never blocks the rest. New providers and the preview /
content-warning categories are opt-in; the defaults are official's providers and categories. Skip and
mute stay distinct: a mute segment lowers the volume for its span and never seeks. Provider keys use
Cxsmo's Keystore-encrypted per-profile store. Post-play keeps official's controller and timing and
gains sources (MDBList, Kurato AI and BingeCat AI add-on catalogs, an AUTO chain), paging and a
trailer fallback; random episodes build on official `EpisodeShuffle` (season scope, all-watched
fallback, Mystery mode that never reveals hidden metadata); Calendar and App Dimmer are Cxsmo
FILE_PORTs. DISCOVERY_SKIP_RECOMMENDATIONS becomes AUTO with the first code slice; OFF restores
official behavior.

## D055 — G10 has one Live TV owner, isolated from VOD playback
The only Live TV module is `fork/livetv`: Reshaped FILE_PORT (streaming M3U / Xtream / Stalker readers,
XMLTV pull parser, per-source jobs with failure isolation), adapted to a Hilt singleton bound to the
active profile. Source records (links, users, passwords, MAC) are encrypted per profile with
`KeystoreCipher`; favorites, hidden channels, category order and the last channel are keyed by
non-secret hashes, never by stream URL. Live TV uses its own OkHttp client without a logging
interceptor (Xtream URLs carry credentials), logs only source type and host, and sends Stalker's
Cookie / Authorization only to the portal host. Channels play through the official player route as
Stremio type `channel` (REUSE of `LivePlaybackUiPolicy`); every live-only playback rule (non-IDR TS
start, HTTP-refusal retries, live-edge rejoin, player reuse on zap, no disk cache / AutoSync / seek
preview / speed learning) is gated on the Live TV playback registry, so VOD keeps official behavior.
AFR stays with G5e `TrackAfrPolicy`, which gains a live branch (no preflight probe, track or measured
rate, one early switch). Preview size, default and guide window come from `AdaptiveResources`.
LIVE_TV becomes AUTO with the first code slice; the menu entry stays off until the user enables it,
and OFF removes the entry and every live-only hook.

## D056 — G11 has one Watch Party owner; consent first, nothing secret leaves the device
The only Watch Party module is `fork/watchparty`: AntoninoScardina/NuvioTV `ff597b1` FILE_PORT
(protocol v1, host-authority sync engine with soft speed correction and hard seek, hidden WebView
running the unmodified VDO.Ninja SDK (MPL-2.0) over a WebRTC data channel), adapted. The wire format,
code alphabet and room / password derivation stay identical so the Nuvio Party phone build can join.
Nothing is shared until the host confirms a consent dialog for that room. Only `User-Agent`,
`Referer`, `Origin`, `Accept` and `Accept-Language` are shared; a stream that needs credentials
(Authorization, Cookie, key / token headers, a user:password@ link), a torrent, a local link or a Live
TV channel is not shareable. Received media is validated, kept in memory only and never saved for
link reuse on the guest. No WebView console logging; errors are fixed codes; nothing logs URLs,
headers, codes or names. Codes come from `SecureRandom`. WATCH_PARTY becomes AUTO with the first code
slice; OFF hides every entry and never creates a WebView.

## D057 — G11 review corrections keep D056's promises at the edges
Corrections after the #71–#73 reviews, within D056's owner and flag. A guest's received link and
headers never enter navigation saved state: the player route carries a one-time in-memory ticket,
and the last-playback diagnostics never save a received link. Non-public destinations (loopback,
private, carrier-grade NAT, link-local, unique-local, multicast, reserved; LAN-only names; numeric
forms other than dotted IPv4) are never shared or opened, and a guest checks that every address a
name resolves to is public before opening it; redirects inside the player are not revalidated (a
recorded residual risk). A guest follows one host until it leaves. The host's speed travels as an
optional `rate` field, omitted at 1× so normal traffic stays protocol v1; the Nuvio Party phone build
(AntoninoScardina/NuvioMobile `ff7a16b`, pinned as a reference only) ignores it. IP-locked or local
sources show a guest-side unsupported state instead of failing silently.

## D058 — G12 has one UI-style owner; official layouts and sidebars stay as shipped
The only UI-style module is `fork/uistyle`: NuvioGlass `84098b7` glass surface, nav pill, clock and
scaffold (FILE_PORT), Reshaped `0ccf049` pill behaviour and AGSL liquid-glass effect (FILE_PORT) and
Cxsmo `3e0d0fa` top-bar profile access (ALGORITHM_PORT), as one top-chrome scaffold with three
looks (BAR, PILL, GLASS) rather than three top bars. Classic / Grid / Modern, both official sidebars,
the hero carousel, hero trailer and full-screen backdrop stay official (verified_official 188–192,
195, 201–203). New: a per-profile navigation style for official layouts (Sidebar default, Top bar,
Pill), `HomeLayout.GLASS` and `CINEMATIC_GLASS` on the Modern pipeline, effects gated by
AdaptiveResources tier and API level with a flat fallback (always over video, or by setting), and an
optional screensaver off by default. UI_STYLES becomes AUTO with the first code slice; OFF treats
Glass as Modern and restores the official sidebar. An upstream sync to official `dev` `5c1d9b0`
precedes G12 code because official changed the home layout files. Legacy PRs #1 / #2 stay
reference-only.

## D059 — Adopt official dev 5c1d9b0 before G12
Official `dev` `5c1d9b0` changed the home layouts, cards and layout settings that G12 builds on
(global landscape poster mode) and fixed Exo libass subtitles. It is merged into integration before
any G12 code (UPSTREAM_SYNC; D050 pattern), with no conflicts, and becomes the accepted baseline once
the clean replays of official `5c1d9b0` and the integration merge show no new debt and no lost or
newly skipped test. Any debt growth or reduced coverage needs exact-head maintainer approval (never
self-approved). The sync merges with a merge commit so official stays a parent.

## D060 — Glass is a navigation style over official's Modern home (amends D058)
D058 planned `HomeLayout.GLASS` and `CINEMATIC_GLASS` as new layouts on the Modern pipeline. Adding
values to official's `HomeLayout` reaches every `when (layout)` in official home, settings, chooser
and preference code, and every later upstream sync. Glass reskins Modern without changing its rows,
hero or enrichment (NuvioGlass `usesModernPipeline`), so it is instead a fourth Navigation style,
GLASS, in `fork_ui_style`: official's Modern home with frosted chrome floating over the full-bleed
hero (NuvioGlass `GlassScaffold` FILE_PORT), hiding itself on Home and returning on Up from the first
row through one hook in `ModernHomeRowsList`. With Classic or Grid it falls back to Pill. Official
layout preferences are never written. The glass is a live Haze blur only on Android 12+ outside the
LOW_RAM tier (AdaptiveResources) and without the per-profile "Lightweight effects" switch; otherwise an
opaque tint. The chrome is only on root screens, never over the player. Cinematic Glass (194) and the
AGSL liquid-glass refraction (199) build on the same style next.
