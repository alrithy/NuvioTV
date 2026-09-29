# G6 audit — Subtitle Intelligence

Evidence for `tasks/G6_SUBTITLE_INTELLIGENCE.md` (IDs 82–106). Official = accepted `71632b9`
(integration `b8e6385`; official `dev` observed at `8e728ca`, not accepted). Sources (SOURCE_MAP pins):
DavidVamaiotu/NuvioTV-Reshaped `subtitle-autosync` @ `0ccf049d2789600835f3f7a75423e9149ea416ba`,
SPxMM3R1/NuvioTV-VibeSubtitle `vibe-dev` @ `9520190184c7299c7ac34617856adde53c1ce7a2`; ysosrs
`45e0984` has no subtitle-sync work (its subtitle delta is overlay/settings churn and removals).
This audit changes no code.

## What official already has
- **Manual sync assist:** `PlayerRuntimeControllerSubtitleTiming` "auto sync" is a user-driven cue
  pick (`captureSubtitleAutoSyncTime` / `applySubtitleAutoSyncCue`): the user marks when a line is
  spoken and the delay is set from that cue. Plus `SubtitleTimingDialog` / `SubtitleDelayConfig`
  (manual offset).
- **Header safety:** subtitle downloads use `subtitleStreamHeaders` / `isInHeaderScope` /
  `withoutCredentialHeaders`: stream Authorization/Cookie never leave the stream host (task-packet
  security rule already official).
- **Secondary language:** `SubtitleStyleSettings.secondaryPreferredLanguage` is the second target of
  internal auto-selection (`subtitleLanguageTargets` / `findBestInternalSubtitleTrackIndex`), the
  MPV preference (`applySubtitleLanguagePreferences`) and add-on subtitle fetching.
- **Buffer-preserving sidecar subtitles:** `PlayerSidecarSubtitles` attaches external add-on
  subtitles on ExoPlayer without a `setMediaSource` reload.
- **Hash:** `OpenSubtitlesHasher` computes the OpenSubtitles hash and `videoHash` reaches subtitle
  add-ons, so hash-matched subtitles are fetched; they are not used as a timing reference.
- **No** automatic timing correction, **no** custom subtitle fonts, **no** style presets.

## Source comparison (lines vs official)
| Area | Reshaped | VibeSubtitle |
|---|---|---|
| Auto sync core | `autosync/*`: `AutomaticSubtitleSync` 2663, `AutoSyncTimelineRetime` 2371 (affine + cue/group DP), `EmbeddedSubtitleTimelineLoader` 1907, `AutoSyncSubtitleCueParser` 414, `SubtitleLanguageMatching` 388, `AutoSyncSidecarBridge` 326, `AutoSyncExtractorsFactory` 186, candidate pool, debug dumps (~12k total) | `SubtitleCueAligner` 434 (language-independent cue-rhythm anchors → offset + clock scale + confidence, null = fail closed), `EmbeddedSubtitleCueCollector` 128, `PlayerRuntimeControllerEmbeddedSubtitleReference` 394, `PlayerRuntimeControllerSubtitleTiming` +464 |
| Audio / ASR sync | `audiosync/*` (~6k: Silero VAD, FFT, spot sampler, speech timeline, aligner) + `asr/*` on the sherpa-onnx runtime (`com.k2fsa.sherpa.onnx`) with a **74 MB English-only model downloaded at runtime** from a GitHub mirror / Hugging Face | none |
| Fonts | `reshaped/subtitlefont/SubtitleFontStore` 329, `SubtitleFontUploadServer` 212 (LAN HTTP upload via QR), `SubtitleFontSettingsItems` 404 | none |
| Indicator / notifications | `AutoSyncSyncedLabel`, `bubble/AutoSyncBubbleToast(s)`, `SubtitleSyncStatusOverlay` | status via timing dialog |
| Settings | `AutoSyncSettingsItems`, `AudioSyncSettingsItems`, `PlaybackSubtitleSettings` +550/−… | `PlaybackSubtitleSettings` +550 |

Not inherited: Reshaped build changes (`isDebuggable = false`, removal of the Nuvio engine aar,
`reshaped.gradle`, Lottie). Where a fork lacks current official files (VibeSubtitle: MDBList sync,
`WatchStateMutationStore`, `PlayerSubtitleDataSource`; ysosrs: `SubtitleSelectionOverlay` −1062),
official is kept whether the fork removed them or predates them.

## Upstream overlap (AR-006)
Official `8e728ca` changes `AudioSelectionOverlay`, `PlaybackEvents`, `PlayerScreen` and post-play
files. The G6 slices below touch none of them; a slice that needs one adopts official through a
reviewed `chore/upstream-sync-*` PR first.

## Decision: one sync engine (D049)
The engine is an ALGORITHM_PORT of VibeSubtitle's `SubtitleCueAligner` (pure, compact, conservative,
fail-closed) as `fork/subtitles`, fed by one reference pipeline in the task-packet order:
embedded reference → hash / same-release add-on reference → cue-rhythm alignment → original timing +
manual offset. Reshaped contributes bounded pieces (reference ranking and consistency checks,
piecewise retime) ported into that engine, never its whole `autosync/*` system alongside it.
Automatic retiming changes what every playback shows, so it runs only while a per-profile
**AutoSync** setting is on (default off = official timing, D048 principle); SUBTITLE_INTELLIGENCE
becomes AUTO so the setting is visible. The manual cue-pick and delay stay official and always win.

## Features
| ID | Behavior | Class | Result / plan |
|---|---|---|---|
| 82 | Subtitle AutoSync | missing delta | official = manual cue pick only; engine + wiring → G6a/G6b |
| 83 | Embedded subtitle reference | missing delta | Vibe `EmbeddedSubtitleCueCollector` (in-band cues of the playing file) → G6b |
| 84 | Same-release subtitle reference | missing delta | Reshaped reference ranking over add-on subtitles of the same release → G6b |
| 85 | Hash-matched subtitle reference | PARTIAL_OVERLAP | official fetches hash-matched subtitles (`videoHash`); using one as the timing reference → G6b |
| 86 | Cue-rhythm alignment across languages | missing delta | Vibe `SubtitleCueAligner` → G6a |
| 87 | Automatic offset estimation | missing delta | aligner offset → G6a |
| 88 | Clock-scale correction | missing delta | aligner scale → G6a |
| 89 | Piecewise drift correction | missing delta | anchor-segment mapping from the aligner's anchors (Reshaped retime idea, bounded) → G6a |
| 90 | Confidence scoring | missing delta | aligner confidence → G6a |
| 91 | Fail closed on low confidence | missing delta | null result keeps original timing → G6a |
| 92 | Audio-based sync fallback | deferred | Reshaped `audiosync/*` needs continuous audio taps, VAD model and CPU on the TV; no bounded portable piece |
| 93 | On-device speech recognition | deferred | sherpa-onnx runtime + 74 MB English-only model downloaded from a GitHub mirror / Hugging Face; not language-independent |
| 94 | Arabic AutoSync | missing delta | covered by the language-independent rhythm engine; Arabic target + English embedded reference test → G6a |
| 95 | Language-independent AutoSync | missing delta | rhythm alignment compares timing, never words → G6a |
| 96 | Secondary subtitle language | ALREADY_OFFICIAL | `secondaryPreferredLanguage` setting and selection |
| 97 | Automatic secondary-language fallback | ALREADY_OFFICIAL | second target in internal / MPV / add-on selection |
| 98 | Auto-Synced indicator | missing delta | small label when a correction is active → G6b |
| 99 | Sync notifications | missing delta | one message on apply / fail-closed → G6b |
| 100 | Custom subtitle fonts | missing delta | Reshaped `SubtitleFontStore` → G6c |
| 101 | Upload TTF/OTF via QR | missing delta | Reshaped `SubtitleFontUploadServer` (LAN, one-time token, size cap) → G6c |
| 102 | Download font from URL | missing delta | bounded HTTPS download → G6c |
| 103 | Validate font before use | missing delta | Typeface load + magic check → G6c |
| 104 | Fall back to default font | missing delta | → G6c |
| 105 | Arabic cinema subtitle preset | missing delta | not in any pinned source; REWRITE as a style preset over official settings → G6c |
| 106 | No video reload on sidecar switch | ALREADY_OFFICIAL | official buffer-preserving sidecar path |

## Slice plan
- G6a: pure sync engine in `fork/subtitles` (86–91, 94, 95) with fixture tests (aligned, fixed
  offset, gradual drift, piecewise drift, low confidence, no reference, Arabic target + English
  reference).
- G6b: wiring behind the AutoSync setting (82–85, 98, 99): embedded / hash / same-release reference,
  apply as a retimed sidecar, manual delay keeps priority, header-scope rules unchanged.
- G6c: custom fonts and the Arabic cinema preset (100–105); security review for the LAN upload.
- 92, 93 deferred with reasons below; 96, 97, 106 verified official.

Every slice keeps device checks MANUAL-PENDING in `docs/HARDWARE_VALIDATION_TCL_C6K.md` (G14, D047).
