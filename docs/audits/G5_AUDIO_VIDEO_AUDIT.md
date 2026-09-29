# G5 audit — Audio / Dolby Vision / HDR / AFR

Evidence for `tasks/G5_AUDIO_VIDEO.md`. Official = accepted `71632b9` (integration `9c627ca`;
official `dev` observed at `8e728ca`, not accepted). Sources: ysosrs123/NuvioTV-Fork `nuvio-test`
@ `45e0984c18460d2a65c5d745999011b4314328eb`, DavidVamaiotu/NuvioTV-Reshaped `subtitle-autosync` @
`0ccf049d2789600835f3f7a75423e9149ea416ba` (SOURCE_MAP pins). Device evidence (DV output,
passthrough, AFR/eARC) is batched into the G14 hardware campaign (validation policy in
`integration/state.yaml`; D047); this audit changes no code.

## File-level comparison (lines added/removed vs official)
| File | ysosrs | Reshaped |
|---|---|---|
| `ui/screens/player/PlaybackSpeedAwareAudioSink.kt` | +1392/−19 | = |
| `ui/screens/settings/PlaybackAudioSettings.kt` | +881/−137 | +657/−137 (settings UI) |
| `core/player/FrameRateUtils.kt` | +367/−93 | = |
| `core/player/DolbyVisionMatroskaTransformer.kt` | +216/−18 | = |
| `core/player/DolbyVisionExtractorsFactory.kt` | +210/−14 | = |
| `ui/screens/player/PlayerRuntimeControllerAfrPreflight.kt` | +137 | +2 |
| `core/player/DoviBridge.kt` | +110/−2 | = |
| `core/player/MatroskaAfrProbe.kt` | +109 | = |
| `ui/screens/player/AudioSelectionOverlay.kt` | +89/−145 | +13/−2 |
| `core/player/HevcDvRpuStripper.kt` | +77 | = |
| `core/player/LetterboxRenderPolicy.kt` | file removed | +8 (Amazon default) |
| `ui/screens/player/GainAudioProcessor.kt` | = | +3/−2 (soft clip hook) |

Fork-only files. ysosrs: `AudioPassthroughPolicy` (108), `AudioCapabilityReport` (281),
`AudioTrackRejectionLog` (98), `LosslessAudioTrackDefault` (183), `PlayerRuntimeControllerAfrTrack`
(296), `Hdr10SeiInjector` (439); `diagnostics/Mat*`, `TrueHdAuFramer`, `Iec61937MatSink` belong to
MAT (45, G13). Reshaped: `volumeboost/VolumeBoostSoftClip`, `VolumeBoostBar`; `audiosync/*` is G6.

The ysosrs audio-sink delta is mostly device-measured experiments (Amlogic MS12 prefill governors,
start-threshold trials, a fault-injection harness, ordering trace logs marked "strip before
publication"). It is not ported as a file; only named, bounded behaviors are, per slice below.

## Upstream overlap (AR-006)
Official `8e728ca` changes `AudioSelectionOverlay.kt` (+59/−6, audio delay UI) and
`PlayerRuntimeControllerPlaybackEvents.kt` (+9/−6). No G5 slice below edits either file. A slice
that needs them first adopts official through a reviewed `chore/upstream-sync-*` PR.

## Features
| ID | Behavior | Class | Result / plan |
|---|---|---|---|
| 32 | TrueHD passthrough | ALREADY_OFFICIAL | `buildStableAudioCapabilities` claims `ENCODING_DOLBY_TRUEHD` when the sink supports it |
| 33 | Atmos passthrough | ALREADY_OFFICIAL | TrueHD (Atmos rides inside) and `ENCODING_E_AC3_JOC` claimed |
| 34 | DTS-HD MA passthrough | missing delta | official never claims `ENCODING_DTS_HD`: a DTS-HD sink gets core DTS only (only the "force optical" switch adds it). ysosrs per-format policy claims it → G5c |
| 35 | DTS:X passthrough | missing delta | same gap as 34 (DTS:X rides in DTS-HD) → G5c |
| 36 | DD / DD+ passthrough | ALREADY_OFFICIAL | `ENCODING_AC3`, `ENCODING_E_AC3` claimed |
| 37 | Best lossless track by default | missing delta | official picks by language/container flags only; ysosrs `LosslessAudioTrackDefault` (below user pick / remembered / failover carry-over) → G5b |
| 38 | Per-format passthrough controls | missing delta | official has one "force optical passthrough" switch; ysosrs `AudioPassthroughPolicy` (per-format receiver capability, Kodi model) → G5c |
| 39 | Detect real device/TV/AVR capabilities | PARTIAL_OVERLAP | official `AudioCapabilities` + `AudioOutputRouteDetector`; ysosrs `AudioCapabilityReport` (what the platform claimed, per sink build) → G5c |
| 40 | Audio output diagnostics | PARTIAL_OVERLAP | G1 HUD `output` row (passthrough encoding / PCM); ysosrs rejection log + capability report → G5c |
| 41 | Actual audio bitrate | missing delta | not shown (HUD shows codec/channels/rate) → G5c if the format exposes it, else deferred with reason |
| 42 | Source vs output channels | PARTIAL_OVERLAP | HUD `audio` row = source channels; `output` row = encoding only; output channel count → G5c |
| 43 | Improved FFmpeg downmix | ALREADY_OFFICIAL | official downmix, `audioOutputChannels`, center mix level, maintain-original-on-downmix |
| 44 | Software decode fallback | ALREADY_OFFICIAL | official `decoderPriority` (FFmpeg extension on/prefer) and `DolbyVisionCodecFallback` |
| 46 | Reduce TrueHD/DTS-HD cold-start delay | deferred (candidate) | ysosrs only has device-specific Amlogic MS12 prefill experiments (one "experiment closed"); no bounded, portable algorithm → defer to G14 with device data |
| 47 | Volume boost up to 200% | ALREADY_OFFICIAL | official amplification 0–10 dB (≈316% amplitude) via `GainAudioProcessor` |
| 48 | Soft clipping for boosted volume | missing delta | official hard-clamps; Reshaped `softClipBoosted` (tanh knee at 0.8) → G5a |
| 49 | libdovi integration | ALREADY_OFFICIAL | `DoviBridge` native conversion path |
| 50 | DV P7 → 8.1 | ALREADY_OFFICIAL | `dv7HandlingMode` (`DV81_LIBDOVI`), libdovi mode override |
| 51 | DV P5 → 8.1 fallback | ALREADY_OFFICIAL | `dv5ToDv81Enabled` |
| 52 | DV enhancement layer / single-track cases | PARTIAL_OVERLAP | official `DolbyVisionCompatibility` + base-layer policy; ysosrs EL-type stats and RPU drop counting → G5d (only the bounded parts) |
| 53 | Detect actual DV/HDR playback state | PARTIAL_OVERLAP | G1 HUD `hdr` row shows the stream; output state (what the display is in) needs `Display.getHdrCapabilities`/mode → G5d |
| 54 | Improved HDR10 fallback | missing delta | ysosrs `Hdr10SeiInjector` + HDR colour signalling on the DV strip path (the stripped base layer carries HDR10 metadata) → G5d |
| 55 | True-black HDR letterboxing | PARTIAL_OVERLAP | official `LetterboxRenderPolicy`/`PlayerWindowBackdrop`; ysosrs deletes it (removal not inherited); Reshaped adds an Amazon-aware default → G5d |
| 56 | Accurate 23.976 / 24 Hz matching | PARTIAL_OVERLAP | official `FrameRateUtils` NTSC film constant and tolerance matching; ysosrs refinements → G5e |
| 57 | Automatic frame rate matching | ALREADY_OFFICIAL | `frameRateMatchingMode` OFF / START / START_STOP |
| 58 | Seamless frame-rate-switch detection | missing delta | ysosrs track-format AFR (`PlayerRuntimeControllerAfrTrack`, reported format instead of a probe) and MKV DefaultDuration frame rate (G4c nt2) → G5e |
| 59 | HDMI/eARC settle/resume after mode change | PARTIAL_OVERLAP | official settle delay on the MPV path only; ysosrs settle hold on the ExoPlayer track path (2 s, 8 s ceiling) → G5e |
| 60 | Source vs output resolution | PARTIAL_OVERLAP | HUD `video` row = source size; `display` row = refresh only; output mode size → G5d |

MAT (45) belongs to G13 and is not touched here.

## Slice plan
- G5a: soft clipping for boosted volume (48); 47 verified official.
- G5b: default to the best lossless audio track (37).
- G5c: per-format passthrough (34, 35, 38) with capability report and output diagnostics (39–42).
- G5d: DV/HDR: HDR10 fallback on the strip path (54), EL handling (52), output state (53, 60), letterbox default (55).
- G5e: AFR precision, track-format AFR and settle hold (56, 58, 59).
- 46 deferred: needs device data, recorded in the G14 campaign.

Every slice keeps its device checks MANUAL-PENDING and appends them to
`docs/HARDWARE_VALIDATION_TCL_C6K.md` (G14 campaign).
