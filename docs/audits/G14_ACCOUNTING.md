# G14 accounting — 320 features, audits and the hardware campaign

2026-10-01. Closes the code side of G14 (`tasks/G14_HARDENING_RELEASE.md`). The G14 audit
(`docs/audits/G14_HARDENING_AUDIT.md`), D064 and D065 remain authoritative. Every gate G0–G14 is merged
on `superfork/integration` with exact-head Full Debug CI; the latest is G14c #92 (`dc676bf`; exact head
`d5832b8`, run 36917962782: 2373 tests, 15 registered failures, 0 new, 1 skipped; APK artifact 11190309840).

## 320-row accounting
318 of 320 rows are terminal with evidence; 2 are blocked with a reason and a next action; none
is planned or in progress.

| Gate | implemented | verified_official | experimental | deferred | blocked | planned | in_progress | Rows |
|---|---|---|---|---|---|---|---|---|
| G0 | 8 | 0 | 0 | 1 | 0 | 0 | 0 | 9 |
| G1 | 26 | 8 | 0 | 1 | 0 | 0 | 0 | 35 |
| G2 | 16 | 1 | 0 | 2 | 0 | 0 | 0 | 19 |
| G3 | 4 | 1 | 0 | 0 | 1 | 0 | 0 | 6 |
| G4 | 6 | 10 | 0 | 4 | 0 | 0 | 0 | 20 |
| G5 | 13 | 14 | 0 | 1 | 0 | 0 | 0 | 28 |
| G6 | 8 | 14 | 0 | 3 | 0 | 0 | 0 | 25 |
| G7 | 10 | 0 | 0 | 0 | 0 | 0 | 0 | 10 |
| G8 | 16 | 6 | 0 | 0 | 0 | 0 | 0 | 22 |
| G9 | 40 | 11 | 0 | 0 | 0 | 0 | 0 | 51 |
| G10 | 29 | 0 | 0 | 0 | 0 | 0 | 0 | 29 |
| G11 | 13 | 0 | 0 | 0 | 0 | 0 | 0 | 13 |
| G12 | 10 | 9 | 0 | 0 | 0 | 0 | 0 | 19 |
| G13 | 0 | 0 | 9 | 4 | 0 | 0 | 0 | 13 |
| G14 | 10 | 10 | 0 | 0 | 1 | 0 | 0 | 21 |
| **All** | **209** | **84** | **9** | **16** | **2** | **0** | **0** | **320** |

**Blocked (both wait on the maintainer's devices):**
- 3 Automatic strategy by network: Auto already uses device, file and stream type; the measured
  throughput input exists (G8c `ConnectionSpeedEstimator`), but its thresholds must come from the TCL
  C6K same-file A/B, not a guess (AR-004).
- 320 One Nuvio distribution: Stable needs the hardware certification and the first signed fork
  release.

**Deferred (16), each with its reason and re-audit trigger in the traceability file:**
- 5 (G4) Adaptive Connection Count: No sourced runtime algorithm and no device measurement to tune one.
- 6 (G4) Adaptive Chunk Size: No sourced runtime algorithm and no device measurement to tune one.
- 17 (G4) Pre-resolve stream before playback: Nothing to port in G4; stream-list prefetch belongs to the G8 source-list owner.
- 26 (G4) Adaptive MPV demux cache by RAM/device tier: G3 strategies are ExoPlayer-only (D043); an MPV cache policy needs the same MPV memory measurement as 280.
- 46 (G5) Reduce cold-start delay for TrueHD / DTS-HD: No bounded, portable algorithm in any pinned source; tuning needs device measurements.
- 77 (G1) Audio clock jitter: Sub-frame sink jitter needs AudioTrack timestamp work in the audio owner; the HUD row measures clock drift only.
- 85 (G6) Hash-matched subtitle reference: A hash-matched subtitle cannot be identified client-side; inferring it from result rank could feed AutoSync a wrong timing reference.
- 92 (G6) Audio-based subtitle sync fallback: No bounded, portable piece; CPU/memory cost on TV hardware is unmeasured and official AutoSync (D050) covers the common case without audio.
- 93 (G6) On-device speech recognition where practical: New native runtime, large third-party model download and English-only recognition (not language-independent, no Arabic).
- 251 (G13) AI-generated subtitles: No engine to port in any pinned source; writing an AI engine from scratch is out of scope (D063).
- 252 (G13) AI speech-to-text: No engine to port in any pinned source (D063).
- 253 (G13) AI subtitle translation: No pinned source (D063).
- 254 (G13) AI voice translation / voice overlay: No wired engine in any pinned source (D063).
- 276 (G2) Bounded offline sync queue: Nothing to bound: official has no offline sync queue and the source has no bounded-queue delta.
- 280 (G2) Low-RAM MPV cache policy: No sourced or measured low-RAM MPV cache value; G3 strategies never change the MPV path.
- 318 (G0) Simple settings for normal users plus Advanced controls for power users: No fork setting is exposed yet; a Simple/Advanced split without real controls would be speculative UI, and DECISIONS requires the observed upstream settings change (#3746) to be synced before a settings-touching gate.

**Experimental (9):** 45, 250, 255–261, OFF unless opted in under Settings → Advanced →
Experimental (D063).

## Audits
- **IMPORT_LEDGER:** one entry per importing slice (G1a … G14c, 48 entries); gates that import
  nothing (G0 foundation, G3 strategies) have none, as the ledger rule covers external imports only.
  Each entry records its source repository, pin, import mode and license notes.
- **Licenses:** GPL-3.0 fork of official. Imported sources are GPL-3.0 forks of the same project;
  `MatPacker` keeps its Kodi / LAV GPL-2.0-or-later header; the VDO.Ninja SDK is MPL-2.0, unmodified
  with its license beside it (G11a); Seekr is Apache-2.0 (reviewed below). All are GPL-3.0 compatible.
- **Dependencies:** the fork adds one library to official's graph, `tv.seekr:seekr-android:0.2.0`
  (G7a), now recorded in DEPENDENCY_POLICY and LICENSE_AND_ATTRIBUTION. Everything else is official's.
- **Sensitive logs:** official's `urlForLog()` is host-only and the URL log sites go through it (G14a);
  a scan of `fork/` finds no log line that prints a URL path, header, key, token, cookie or password
  (the two skip-provider lines print the provider id). Security classes in `fork/aimedia` never
  reference `android.util.Log` (bytecode test, G13a).
- **Local servers:** all seven NanoHTTPD servers (official's five, the fork's two) require the QR
  session key and refuse cross-site writes (G14a).
- **TEST_MATRIX:** the corpus and measurements in `docs/TEST_MATRIX.md` are covered by the hardware
  campaign below; nothing in it is claimed from CI.
- **Not done here (need a device or a signed release):** fresh install and upgrade on a TV, release
  artifact / tag reproducibility, HV results.

## Hardware campaign (D047)
75 MANUAL-PENDING cases (G1: 3, G2: 3, G3: 3, G4: 9, G5: 10, G6: 5, G7: 5, G8: 7, G9: 7, G10: 8, G11: 4, G12: 5, G13: 3, G14: 3) plus the G4 same-file A/B, all in
`docs/HARDWARE_VALIDATION_TCL_C6K.md` with steps, expected results and the evidence to attach. Suggested
order, one device set at a time:
1. TCL C6K (Android TV, HDMI / eARC): sections 0–4b, 4c–4f, 4i, 4k, then the A/B (section 5).
2. The eARC receiver: HV-G5 audio and passthrough, HV-G13-3 MAT.
3. A 2 GB box and an Android 11 box: HV-G2, HV-G12 effects, Live TV (4g).
4. Two TVs and a phone on two networks: Watch Party (4h); a phone for the QR pages (HV-G14-1).
5. After the first signed fork release: HV-G14-3 updates; HV-G13-1 / HV-G13-2 provider and BYOK.

Record each case as PASS or FAIL with its evidence in `docs/MANUAL_TEST_LOG.md`; a FAIL reopens the
feature for a focused fix and re-test. Nothing is inferred from CI.

## State
G14 is BLOCKED with one reason: the hardware campaign and the maintainer release actions
(GITHUB_ADMIN_CHECKLIST A–D). No code work is pending; Stable is not claimed.
