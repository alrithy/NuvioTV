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
Do not have two agents editing the same task branch concurrently unless explicitly coordinated.

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