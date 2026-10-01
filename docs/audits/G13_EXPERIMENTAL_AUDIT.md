# G13 audit — Experimental AI & MAT

Evidence for `tasks/G13_EXPERIMENTAL.md` (IDs 45 and 250–261). Official is the accepted baseline
`5c1d9b0` (D059). Official `dev` is now at `9e17941`: seven commits since `5c1d9b0` touching
MDBList ratings (disk cache), stream-screen focus and strings. None of them touches a G13 surface
(player audio sink, subtitles, settings, credentials), so no sync PR precedes G13 (the G8 pattern);
the observation is recorded in state.

Pinned sources (SOURCE_MAP):
- Fornace/nuvio-ai `dev` @ `518af71` (EXPERIMENTAL_FILE_PORT): the AI media provider host;
- ysosrs123/NuvioTV-Fork `nuvio-test` @ `45e0984`: the MAT / IEC 61937 path (`diagnostics/`).

D018 already holds: MAT stays Experimental and OFF until validated on target hardware. Everything in
G13 defaults OFF (task invariant). This audit changes no code.

## What official already has at `5c1d9b0`
- **AI:** nothing. No AI provider, no generated subtitles, no speech-to-text, no translation, no
  dubbing, no credential store for AI vendors.
- **Audio:** Media3 passthrough of what the HDMI / eARC chain reports (AC-3, E-AC-3, TrueHD, DTS),
  official's `PlaybackSpeedAwareAudioSink` around `DefaultAudioSink`. No app-side MAT packing: TrueHD
  reaches the receiver only where the platform bitstreams it.
- **Networking:** official's playback networking falls back to a trust-all client on TLS errors
  (`PlayerPlaybackNetworking.trustAllPlaybackHttpClient`), so no AI traffic may ride official's
  clients.
- **Fork owners that touch this:**
  - `FeatureId.AI_MEDIA` and `FeatureId.MAT_AUDIO` exist with `experimental = true`, both OFF, and
    nothing can turn them on yet (the registry's override seam is unused).
  - `fork/foundation/KeystoreCipher` (G7a, G9a, G10): one Android Keystore AES-GCM key per store, no
    associated data.
  - G5c `AudioPassthroughPolicy` (per-format passthrough switches) and the G5 HUD audio chain.

## What the pinned sources add
| Source | What | Size |
|---|---|---|
| Fornace `518af71` host (`core/media/provider`) | Provider registry client (`nuvio-extensions.fornace.net`) and models; vendor catalog (OpenAI-compatible ASR, Cloudflare Workers AI, OpenAI realtime, Gemini live, Qwen); provider APK download, SHA-256 check (constant-time) and system `PackageInstaller` install; exact signer-set match; Messenger contract negotiation validated field by field (exported service, bind permission, protocol, provider id, package, version, one capability, engine status, package unchanged after reply); per-profile BYOK vault (AES-256-GCM per record, AAD bound to installation, profile generation, provider and record; no plaintext getter, secret zeroed); dedicated platform-TLS client; capability grants; Provider Center screen | about 4,000 lines plus 3,000 lines of tests |
| Fornace provider APKs (`provider-subtitles`, `provider-voice`) | "Contract shells": negotiate and ping only ("no engine, no network, no credentials"); engine adaptors build request specs as pure data, nothing sends them | about 600 lines |
| Fornace player pieces | `MutableTimedCueStore`, `CueIntervalIndex` (sidecar cues), `DubPlaybackCoordinator`, `AudioDelayMediaSource`; the host's subtitle provider is bound to `FakeSubtitleCuesProvider`, and the cue store and dub coordinator are not constructed anywhere | about 1,100 lines |
| ysosrs `45e0984` MAT (`diagnostics/`) | `MatRoutingAudioSink` (a `ForwardingAudioSink` that routes TrueHD through the app when its toggle is on; non-blocking writes and a pending-frame queue), `TrueHdAuFramer`, `MatPacker`, `Iec61937MatSink`; toggle `mat_passthrough_enabled`, off by default; `Gate0Probe` diagnostic; no unit tests | 1,118 lines plus a 576-line probe |

**The key finding:** Fornace at `518af71` ships the provider *platform*, not working AI features. The
installable providers answer the contract handshake and nothing else, the live subtitle path is a
fake, and the voice overlay is not wired to the player. Its own decision record
(`docs/ai-media-research/decision.md`) calls generated subtitles a conditional go and live dubbing
research only.

**Not inherited:**
- Fornace fork identity, updater, signing, cloudstream / plugin changes and everything outside
  `core/media/provider`, the Provider Center and the two provider contracts.
- The fake subtitle provider and the unwired cue store and dub coordinator.
- ysosrs `Gate0Probe` and its device-assessment coupling (`DeviceAssessmentApplier` turning MAT on).

## Decision: platform now, engines when a source ships them (D063)
- **Owners:**
  - `fork/aimedia`: one owner for the AI provider host. FILE_PORT of Fornace's registry client,
    vendor catalog, artifact verifier, contract validator, Messenger client, package scanner /
    installer bridge, install coordinator, credential vault and TLS client, adapted to fork
    packages and per-profile stores.
  - `fork/audio` keeps the audio chain; MAT is added there (FILE_PORT of ysosrs `diagnostics/Mat*`,
    `TrueHdAuFramer`, `Iec61937MatSink`) and wraps official's sink only while MAT is on.
- **Credentials:** AI keys use the ported vault (AAD-bound, one key per record, per profile
  generation). It is stronger than `KeystoreCipher`, which keeps its existing stores; moving those
  over would rewrite users' saved keys and is not part of G13 (recorded as a follow-up).
- **Isolation:** providers are separate APKs bound by explicit intent with the bind permission; no
  AI key, playback URL, header or cookie reaches an add-on, scraper or in-process plugin.
- **Trust:** the registry is fetched from the pinned host only, over a dedicated client with platform
  TLS and hostname checks and no logging interceptor (never official's clients); each provider APK
  must match the registry's SHA-256 and its exact signer set before install, and the host re-checks
  identity on every negotiation (fail-closed). Unknown sources stay a system prompt the user
  confirms.
- **Opt-in (the only way anything in G13 turns on):** a Settings → Experimental screen with one
  switch per experimental group (AI media providers, MAT passthrough), each OFF by default with a
  warning; the choice feeds the feature registry's override seam and takes effect on the next app
  start. OFF hides every entry point and keeps every hook inert.
- **Deferred (no engine in any pinned source):**
  - 251 AI-generated subtitles, 252 AI speech-to-text and 254 AI voice translation / voice overlay:
    Fornace ships contract shells, a fake cue provider and an unwired dub coordinator; writing AI
    engines from scratch is out of scope for an experimental gate.
  - 253 AI subtitle translation: no pinned source implements it.
  - Re-audit when Fornace publishes engine providers (the capabilities `SUBTITLE_CUES_V1` and
    `DUB_ARTIFACT_V1` are already negotiated by the host).
- **MAT safety:** the wrapper is constructed only when MAT is on and the input is TrueHD; the HUD and
  the G5 passthrough switches keep working; volume is a no-op in MAT mode (gain corrupts the
  bitstream). The audio renderers' `PlaybackSpeedAwareAudioSink` lookup must see through the wrapper.

## Feature map
| ID | Feature | Official | Decision |
|---|---|---|---|
| 45 | Experimental Kodi-style MAT / IEC 61937 path | platform passthrough only | ysosrs MAT FILE_PORT in `fork/audio`, behind the MAT opt-in → G13c |
| 250 | AI Media Provider platform | missing | `fork/aimedia` host and Provider Center → G13a / G13b |
| 251 | AI-generated subtitles | missing | deferred: Fornace has contract shells and a fake cue provider only |
| 252 | AI speech-to-text | missing | deferred: no engine in the pinned source |
| 253 | AI subtitle translation | missing | deferred: no pinned source implements it |
| 254 | AI voice translation / voice overlay | missing | deferred: the dub coordinator is not wired and the provider is a shell |
| 255 | BYOK | missing | per-profile vault, paste a key per vendor, Verify → G13a / G13b |
| 256 | Multiple AI vendors | missing | vendor catalog and per-vendor credentials → G13a |
| 257 | Provider APK architecture | missing | registry, download, install, negotiation → G13a / G13b |
| 258 | Keep AI providers isolated from the core app | missing | separate APKs, explicit bind with permission, Messenger only → G13b |
| 259 | SHA-256 verification for provider APKs | missing | constant-time digest check before install → G13a |
| 260 | Signer verification | missing | exact signer-set match at install and on every negotiation → G13a |
| 261 | Encrypt API keys using Android Keystore | KeystoreCipher (other stores) | AES-256-GCM vault with bound AAD → G13a |

## Slices
- **G13a** (250, 255–257, 259–261 core): `fork/aimedia` pure core: registry and vendor-catalog models
  and parsing, artifact verifier, contract validator, credential vault with its Keystore bridge seam,
  install identity, profile generations, cipher-text store, capability grants; the experimental
  opt-in store and the registry override. Tests: ported Fornace vault, validator, verifier, registry
  and catalog tests, plus opt-in and inertness tests.
- **G13b** (250, 255, 257, 258 Android): registry client over the dedicated TLS client, package
  scanner, `PackageInstaller` bridge, Messenger contract client, install coordinator, Provider Center
  screen and the Settings → Experimental screen; strings en + ar.
- **G13c** (45): MAT routing sink, TrueHD framer, MAT packer and IEC 61937 sink behind the MAT opt-in;
  local tests for framing, packing and the wrapper's off path (the source has none).

Provider install and negotiation on a TV, BYOK Verify against a real vendor, and MAT on an eARC
receiver are device checks (HV-G13-1..HV-G13-3, G14). No device run is claimed.
