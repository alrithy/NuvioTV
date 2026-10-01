# G13 development closeout — CODE-COMPLETE (experimental)

2026-10-01. This records merged work. The G13 source audit (`docs/audits/G13_EXPERIMENTAL_AUDIT.md`)
and D063 remain authoritative. Scope: 13 IDs, 45 and 250–261. Traceability has 9 experimental rows
(45, 250, 255–261), the final status the gate spec requires, and 4 deferred rows (251–254) with their
reason and re-audit trigger; no planned, in_progress or blocked rows.

| Slice | PR | IDs | Merge on superfork/integration |
|---|---|---|---|
| Source audit | #83 | 45, 250–261 (plan; 251–254 deferred) | a62ce1d78109237868b7d798357f86c187ad71e8 |
| G13a `fork/aimedia` core and the experimental opt-in | #84 | 255, 256, 259–261 (core) | b5da99514949ed2df5128b96bf9652626e53a12e |
| G13b Provider Center, install and negotiation | #85 | 250, 255–261 | a4f66f51e3a54819de884bf205028dd27491f6b1 |
| G13c MAT / IEC 61937 TrueHD passthrough | #86 | 45 | 746c82aa49b299ab77ac1a5541b6b516b5e01121 |

Exact-head Full Debug CI. Each slice was squash-merged with its expected head, and integration was
fetched afterwards. Official `dev` `9e17941` touched no G13 seam, so no sync PR preceded G13.

| PR | Head | Run | Tests | Registered failures | New | Skipped | APK artifact |
|---|---|---|---|---|---|---|---|
| #83 | 498da52f25df308342674f8c5178128a5c9e449a | 36877526195 | 2219 | 15 | 0 | 1 | 11170525995 |
| #84 | fdc8640cc37f8696c6bf717cc206ef72dcf31f19 | 36879784424 | 2290 | 15 | 0 | 1 | 11170983846 |
| #85 | ebe2e4944e501951cb0e125b24aa495f9f33b66d | 36882099635 | 2323 | 15 | 0 | 1 | 11173800583 |
| #86 | a8eeb15782a3708b692b3d8ca3be365e09e59952 | 36885503149 | 2330 | 15 | 0 | 1 | 11174408183 |

Governance CI, PR Policy, Baseline Change and State Handoff succeeded on every head. These are the
actual CI values, not a claim that every test passes.

## Review
No review threads were opened on #83–#86. A later review that lands after merge is handled as the
G11 corrections were.

## What G13 delivers (D063)
- **Opt-in, the only way anything in G13 turns on:** Settings → Advanced → Experimental, one switch
  per experimental group (AI media providers, MAT passthrough), each OFF by default with a warning;
  the choice is stored device-wide, read in `NuvioApplication.onCreate` before any registry read and
  fed to the feature registry's override seam, so it takes effect on the next start. Only
  experimental groups can be opted in. OFF hides every entry point and constructs nothing.
- **AI provider platform (`fork/aimedia`, Fornace `518af71` FILE_PORT):**
  - registry client and APK downloader over a dedicated platform-TLS client with hostname checks and
    no interceptors (never official's clients, whose playback client falls back to trust-all);
  - constant-time SHA-256 check against the registry before install, exact signer-set match at
    install and on every negotiation (fail-closed); install through the system `PackageInstaller`
    prompt;
  - providers are separate APKs bound by explicit intent with the signature-level bind permission,
    Messenger only; no AI key, playback URL, header or cookie reaches an add-on or plugin;
  - BYOK per profile: AES-256-GCM vault with one Keystore key per record and AAD bound to the
    installation, profile generation, provider and record; no plaintext getter; deleting a profile
    deletes its keys and retires its generation through official's `ProfileScopedCredentialStore` set;
  - multiple vendors from the vendor catalog; Provider Center with a preview notice (the published
    providers are contract shells).
- **MAT (`fork/audio/mat`, ysosrs `45e0984` FILE_PORT):** TrueHD framer, Kodi-derived MAT packer,
  IEC 61937 sink and routing sink; the wrapper is constructed only while MAT is opted in; the G5
  passthrough policy and Bluetooth PCM keep working through the wrapper; volume is ignored in MAT mode.

## Not done, by design or as a residual risk
**Deferred (251–254):** AI-generated subtitles, AI speech-to-text, AI subtitle translation and AI
voice translation / overlay. No pinned source ships an engine: Fornace's providers answer the
contract handshake only, its live subtitle path is a fake and its dub coordinator is unwired.
Re-audit when Fornace publishes engine providers; the host already negotiates `SUBTITLE_CUES_V1` and
`DUB_ARTIFACT_V1`.

**Not imported, by design:**
- Fornace fork identity, updater, signing and plugin changes; its suspend change to official's
  `ProfileScopedCredentialStore` and its edit to official's `ActiveProfileProvider`; the fake
  subtitle provider, cue store, dub coordinator and transform registry;
- ysosrs `Gate0Probe`, its player setting, the device-assessment step that turned MAT on and the HUD
  MAT row.

See IMPORT_LEDGER G13a–G13c.

**Residual risks:**
- The provider APKs fix the bind permission's name, so another differently signed app that defines
  it (Fornace's Nuvio AI, a differently signed build of this fork) cannot be installed alongside.
- Fornace's registry is a third-party trust root, reached only behind the explicit opt-in.
- `PackageInstaller` and Messenger behaviour on TV boxes; receivers that need IEC pause bursts on a
  rebuffer; IEC 61937 AudioTrack support at 192 kHz / 8 channels (falls back to the delegate).
- Migrating the older `KeystoreCipher` stores to the AAD-bound vault would rewrite users' saved keys
  and is left as a follow-up.

## Checks and device validation
Local closeout checks: project validator, state views, governance regression suite and git diff
--check. Android tests and build evidence come from GitHub CI. During development a standalone JVM
harness exercised the `fork` packages (455 tests at #86, Android seams stubbed). No manual or device
testing is claimed.

HV-G13-1, HV-G13-2 and HV-G13-3 all remain **MANUAL-PENDING**. They are carried into
validation_pending_gates, the hardware checklist (§4j) and MANUAL_TEST_LOG for G14 under D047.
G13 is CODE-COMPLETE with its features `experimental` and hardware validation pending; this is not a
Stable-release claim.

## Next gate
This governance transition activates G14 Hardening / Distribution / Upstream on
`chore/release-hardening` (task `tasks/G14_HARDENING_RELEASE.md`, IDs 293–310, 312, 316, 320), owner
Claude (sequential; no lease). G14 is the last gate: it runs the consolidated hardware-certification
campaign for every MANUAL-PENDING check from G1–G13 (real PASS / FAIL only, a FAIL reopens the
affected feature) and the release and upstream hardening. G14 begins only after this PR merges.
