# Source Map — pinned 2026-09-28

| Source | Branch | Pinned SHA | Harvest | Import mode |
|---|---|---|---|---|
| NuvioMedia/NuvioTV | dev | fd7973d91dd75d790c5f9b3d68dae652655e92c4 | official base | BASELINE |
| DavidVamaiotu/NuvioTV-Reshaped | subtitle-autosync | 0ccf049d2789600835f3f7a75423e9149ea416ba | AutoSync, fonts, seek previews/buffer, volume boost, pill nav, connection-fit, Live TV | CHERRY_PICK / FILE_PORT |
| ysosrs123/NuvioTV-Fork | nuvio-test | 45e0984c18460d2a65c5d745999011b4314328eb | REMUX/network, assessment, lossless audio, DV, failover, diagnostics, health | DELTA_PORT |
| Cxsmo-ai/NuvioTV-Custom | main | 3e0d0fad60a2721adec133b88640b49c0183883f | Random/Mystery, Calendar, skip providers, progressive AIOStreams, recommendations, dimmer, Seekr calibration | CHERRY_PICK / FILE_PORT |
| hackerslash/NuvioTV-Lite | dev | 2afdcd05d45e27afd48fb83ef9db6c286216a44c | RAM tiers, bounded caches/fan-out, low-memory policies | LOGIC_PORT |
| SPxMM3R1/NuvioTV-VibeSubtitle | vibe-dev | 9520190184c7299c7ac34617856adde53c1ce7a2 | cue-rhythm alignment, embedded reference | ALGORITHM_PORT |
| AntoninoScardina/NuvioTV | watchparty | ff597b12bc7834b07dedda92c88e0d574c0aad81 | Watch Party | FILE_PORT |
| Fornace/nuvio-ai | dev | 518af7120c82d014c02f7717f4c9c6e396eb215c | external AI providers, BYOK, provider verification | EXPERIMENTAL_FILE_PORT |
| xnucade/NuvioGlass | dev | 84098b7de222fb599e30d12f7a6f5b9b1e347fee | Glass UI/design system | UI_PORT |

## No-duplicate rules
Official already contains parallel range code, stream speed testing, DV/libdovi work, MDBList, custom server infrastructure, and a debug stats overlay. Always diff first and import only missing behavior.
## Source aliases used by traceability
| Alias | Source |
|---|---|
| official | NuvioMedia/NuvioTV |
| project | Local project glue; no external import |
| foundation | Local project glue; official integration anchor |
| reshaped | DavidVamaiotu/NuvioTV-Reshaped |
| ysosrs | ysosrs123/NuvioTV-Fork |
| cxsmo | Cxsmo-ai/NuvioTV-Custom |
| hackerslash-lite | hackerslash/NuvioTV-Lite |
| vibe-subtitle | SPxMM3R1/NuvioTV-VibeSubtitle |
| antonino-watchparty | AntoninoScardina/NuvioTV |
| fornace | Fornace/nuvio-ai |
| nuvio-glass | xnucade/NuvioGlass |

`+` combines candidate sources, not permission to import them all. G0 imports no fork code.
Modes in older research are aliases: LOGIC_PORT = ALGORITHM_PORT; UI_PORT and
EXPERIMENTAL_FILE_PORT = FILE_PORT with the applicable flag/release constraint.
Before any import, resolve short research hashes to full commits under the pinned SHA,
identify files/tests, and complete IMPORT_LEDGER. A candidate is not verified upstream
coverage: only `verified_official` rows with evidence make that claim.
