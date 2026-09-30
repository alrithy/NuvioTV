# G6 audit — Subtitle Intelligence

Evidence for `tasks/G6_SUBTITLE_INTELLIGENCE.md` (IDs 82–106). Official = accepted `56aaba2`
(D050, pre-G6 sync PR #40; includes official Subtitle AutoSync, upstream PR #3703). Sources
(SOURCE_MAP pins): DavidVamaiotu/NuvioTV-Reshaped `subtitle-autosync` @
`0ccf049d2789600835f3f7a75423e9149ea416ba`, SPxMM3R1/NuvioTV-VibeSubtitle `vibe-dev` @
`9520190184c7299c7ac34617856adde53c1ce7a2`. ysosrs `45e0984` has no subtitle-sync work (its
subtitle delta is overlay/settings churn and removals). This audit changes no code.

The first version of this audit (commit `5ad7563`, D049) was written against `71632b9`, before
official had AutoSync, and planned a fork engine. Official shipped AutoSync the same day; D050
supersedes D049: official AutoSync is the one engine (REUSE) and G6 only fills what it still lacks.

## What official has at `56aaba2`
- **AutoSync V2** (`ui/screens/player/autosync/*`, ~8.6k lines, the upstreamed form of Reshaped's
  `subtitle-autosync` work): `AutomaticSubtitleSync` first runs a fixed-scale delay-only check
  (`AutoSyncSampledReferenceAligner`, `AutoSyncDelayPreflight`); when no single offset is strong
  and stable it fits a whole-film affine transform (offset + clock scale) and runs cue/group DP
  retiming (`AutoSyncTimelineRetime`, "text-independent full-timeline retiming").
- **Reference:** subtitles embedded in the playing file only: `EmbeddedSubtitleTimelineLoader`
  indexes the container's subtitle tracks while the stream opens, and `AutoSyncExtractorsFactory`
  observes in-band cues passing through Media3 (output forwarded unchanged). Several references
  are checked against each other (`AutoSyncReferenceConsistency`, outlier removal).
- **Targets:** the selected add-on subtitle; with the thorough search, other add-on subtitles of
  the **same language** are tried as alternative targets. Add-on subtitles are never used as the
  timing reference.
- **Fail closed:** no confident fit (`confident` + margin gates) returns no retime and the subtitle
  starts with its original timing (`PlayerRuntimeControllerAutomaticSubtitleSync`), with a
  toast naming the reason (no match, no embedded subtitles, unsupported format).
- **Indicator:** `AutoSyncSyncedLabel` ("Auto synced").
- **Settings:** Auto Sync Subtitles (`automatic_subtitle_sync_enabled`, **default off**),
  tolerance, thorough search. Strings exist in `values/` only (no `values-ar` translation).
- Unchanged from `71632b9`: manual cue pick and delay (`PlayerRuntimeControllerSubtitleTiming`,
  `SubtitleTimingDialog`), header scope (`subtitleStreamHeaders` / `withoutCredentialHeaders`: stream
  Authorization/Cookie never leave the stream host), secondary language, buffer-preserving
  sidecar subtitles (`PlayerSidecarSubtitles`), OpenSubtitles hash (`OpenSubtitlesHasher`,
  `videoHash` sent to add-ons).
- **No** custom subtitle fonts, **no** style presets.

## What the pinned forks still add
| Area | Reshaped | VibeSubtitle |
|---|---|---|
| Sync engine | upstreamed into official (above); not imported again | `SubtitleCueAligner` 434: a second engine; not imported (one owner, D050) |
| Audio / ASR sync | `audiosync/*` (~6k: Silero VAD, FFT, spot sampler, speech timeline, aligner) + `asr/*` on the sherpa-onnx runtime (`com.k2fsa.sherpa.onnx`) with a **74 MB English-only model downloaded at runtime** from a GitHub mirror / Hugging Face | none |
| Fonts | `reshaped/subtitlefont/SubtitleFontStore` 329, `SubtitleFontUploadServer` 212 (LAN HTTP upload via QR), `SubtitleFontSettingsItems` 404 | none |

Not inherited: Reshaped build changes (`isDebuggable = false`, removal of the Nuvio engine aar,
`reshaped.gradle`, Lottie), Reshaped `bubble/*` toasts (official toasts cover 99). Where a fork
lacks current official files (VibeSubtitle: MDBList sync, `WatchStateMutationStore`,
`PlayerSubtitleDataSource`; ysosrs: `SubtitleSelectionOverlay` −1062), official is kept whether the
fork removed them or predates them. PR #38 (a separate fork engine) is not imported (D050).

## Features
| ID | Behavior | Class | Result / plan |
|---|---|---|---|
| 82 | Subtitle AutoSync | ALREADY_OFFICIAL | AutoSync V2, setting default off |
| 83 | Embedded subtitle reference | ALREADY_OFFICIAL | `EmbeddedSubtitleTimelineLoader` + in-band cue observation |
| 84 | Same-release subtitle reference | missing delta | official has no reference when the file has no embedded subtitles; use an add-on subtitle for the exact release as the reference → G6a |
| 85 | Hash-matched subtitle reference | PARTIAL_OVERLAP | official fetches hash-matched subtitles (`videoHash`) but never uses one as the reference → G6a |
| 86 | Cue-rhythm alignment across languages | ALREADY_OFFICIAL | text-independent timeline retime; embedded reference of any language |
| 87 | Automatic offset estimation | ALREADY_OFFICIAL | delay-only preflight / affine offset |
| 88 | Clock-scale correction | ALREADY_OFFICIAL | whole-film affine fit |
| 89 | Piecewise drift correction | ALREADY_OFFICIAL | cue/group DP retiming |
| 90 | Confidence scoring | ALREADY_OFFICIAL | coverage/margin gates, multi-reference consistency |
| 91 | Fail closed on low confidence | ALREADY_OFFICIAL | no fit keeps original timing + reason toast |
| 92 | Audio-based sync fallback | deferred | Reshaped `audiosync/*` needs continuous audio taps, VAD model and CPU on the TV; no bounded portable piece |
| 93 | On-device speech recognition | deferred | sherpa-onnx runtime + 74 MB English-only model downloaded from a GitHub mirror / Hugging Face; not language-independent |
| 94 | Arabic AutoSync | PARTIAL_OVERLAP | the engine is text-independent, so Arabic targets work; the AutoSync settings, toasts and label have no Arabic strings → G6a |
| 95 | Language-independent AutoSync | ALREADY_OFFICIAL | timing-only comparison |
| 96 | Secondary subtitle language | ALREADY_OFFICIAL | `secondaryPreferredLanguage` setting and selection |
| 97 | Automatic secondary-language fallback | ALREADY_OFFICIAL | second target in internal / MPV / add-on selection |
| 98 | Auto-Synced indicator | ALREADY_OFFICIAL | `AutoSyncSyncedLabel` |
| 99 | Sync notifications | ALREADY_OFFICIAL | AutoSync failure toasts |
| 100 | Custom subtitle fonts | missing delta | Reshaped `SubtitleFontStore` → G6b |
| 101 | Upload TTF/OTF via QR | missing delta | Reshaped `SubtitleFontUploadServer` (LAN, one-time token, size cap) → G6b, security review |
| 102 | Download font from URL | missing delta | bounded HTTPS download → G6b |
| 103 | Validate font before use | missing delta | Typeface load + magic check → G6b |
| 104 | Fall back to default font | missing delta | → G6b |
| 105 | Arabic cinema subtitle preset | missing delta | not in any pinned source; REWRITE as a style preset over official settings → G6b |
| 106 | No video reload on sidecar switch | ALREADY_OFFICIAL | official buffer-preserving sidecar path |

## Slice plan
- G6a (84, 85, 94): extend official AutoSync, not a second engine. When the file has no embedded
  subtitle reference, use an add-on subtitle matched to the exact release (OpenSubtitles hash
  match, else same release name) as the reference, only while AutoSync is on; official path
  unchanged whenever an embedded reference exists. Add the Arabic strings for the AutoSync UI.
  Header-scope rules unchanged (reference downloads use the add-on subtitle's own headers).
- G6b (100–105): custom fonts and the Arabic cinema preset; security review for the LAN upload
  (one-time token, size cap, font magic check, no path from the request).
- 92, 93 deferred (reasons in traceability); every other ID verified official.

Every slice keeps device checks MANUAL-PENDING in `docs/HARDWARE_VALIDATION_TCL_C6K.md` (G14, D047).
