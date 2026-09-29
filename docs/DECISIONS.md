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
