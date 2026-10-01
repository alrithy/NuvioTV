# G11 development closeout — CODE-COMPLETE

2026-10-01. This records merged work. The G11 source audit (`docs/audits/G11_WATCH_PARTY_AUDIT.md`),
D056 and D057 remain authoritative. Scope: all 13 IDs, 237–249. Traceability has 13 implemented rows,
each with evidence, and no planned, in_progress, blocked or deferred rows. No ID is
verified_official: official has the player controls a sync engine needs, but no Watch Party.

| Slice | PR | IDs | Squash merge on superfork/integration |
|---|---|---|---|
| Source audit | #71 | 237–249 (plan) | 2d39a6d4f91779f15e040f3ae344fd9385202437 |
| G11a core: protocol, sync engine, share policy, transport | #72 | 237–242, 244–249 | 74ee62ff3f5a143735b91140107c19e6684ed216 |
| G11b UI: player button, consent, room panel, join | #73 | 237–244 (UI) | 1002530090c25c6ad41f4826afe730ffe267f258 |
| Review corrections | #74 | 240, 241, 244, 247 (and hardening of the rest) | 65e1c57e828daa6323f5968adbad627c1369e490 |

Exact-head Full Debug CI. Each PR was squash-merged with its expected head, and integration was fetched afterwards.

| PR | Head | Run | Tests | Registered failures | New | Skipped | APK artifact |
|---|---|---|---|---|---|---|---|
| #72 | c507bc8ff1a201fa3eae53849a92b24e6d531145 | 36835101903 | 2197 | 15 | 0 | 1 | 11148704473 |
| #73 | c4e3a953a80d68c7da2d872d4094eeccbf1bd2dc | 36836494526 | 2198 | 15 | 0 | 1 | 11149093802 |
| #74 | af7de6221030afbe5bde4f5c14cd11a35e9b2241 | 36842866031 | 2205 | 15 | 0 | 1 | 11152162286 |

The audit #71 ran at 2186 tests (15 registered, 0 new; recorded in HANDOFF). Governance CI, PR Policy,
Baseline Change and State Handoff succeeded on every head; the unchanged baseline replay was skipped.
These are the actual CI values, not a claim that every test passes.

## Review
Codex left its findings on #71 (6), #72 (4) and #73 (4) after or around each merge: 14 findings,
three of them P1. All were fixed together in #74 (D057). Each thread got a reply naming the
fix and was resolved:
- **#71:** pin the phone build; IP-locked sources; guest media in saved state; base playback speed;
  remote pause bookkeeping; private-network targets.
- **#72:** host impersonation; private and link-local ranges; host state after the host leaves;
  terminal signaling loss.
- **#73:** stream secrets in navigation state; stale media after the stream became unshareable;
  selected speed during drift correction; streams without a finite duration.

#74 itself had no review threads.

## What G11 delivers (D056, D057)
- **Owner:** one `fork/watchparty` owner, a FILE_PORT of AntoninoScardina/NuvioTV `ff597b1`, adapted.
- **Wire format:** protocol v1 is kept, so the Nuvio Party phone build can join
  (AntoninoScardina/NuvioMobile `watchparty` @ `ff7a16b`, pinned as a reference only).
- **Sync:** the host is the authority. Small drift is caught up with a speed change of up to ±10 %
  (pitch kept) around the host's speed; larger drift triggers a hard seek with a learned lead.
- **Room code:** six characters from `SecureRandom`.
- **Consent:** nothing is shared until the host agrees to a consent step that names what joiners receive.
- **Headers:** only allow-listed headers ever leave the device. Credential, torrent, local / non-public
  and Live TV streams are not shareable, and the panel says why.
- **Guest side:**
  - accepts only bounded public HTTP(S) media, after checking that its resolved addresses are public;
  - follows one host until it leaves;
  - opens the host's stream through a one-time in-memory ticket, so the link is never in saved state;
  - never saves the link for reuse or in diagnostics;
  - sees a notice when the link does not open on that device.
- **Transport:** a hidden WebView running the upstream VDO.Ninja SDK v1.6.1 (MPL-2.0, source form)
  over a WebRTC data channel, with no console logging and fixed error codes. Terminal signaling
  loss ends the room.
- **Flag:** WATCH_PARTY is AUTO. OFF hides every entry point and never creates a WebView.

## Not done, by design or as a residual risk
**Not imported, by design:**
- unconditional sharing of the link with every header;
- WebView console logging;
- `String.random()` room codes;
- a separate Settings category for joining;
- Italian strings.

See IMPORT_LEDGER G11a, G11b and G11 review corrections.

**Residual risks:**
- Redirects and DNS rebinding inside the player's data source are not revalidated.
- The availability and terms of VDO.Ninja's hosted signaling / TURN service.
- WebView WebRTC support on TV boxes.

## Checks and device validation
Local closeout checks: project validator, state views, governance regression suite and git diff
--check. Android tests and build evidence come from GitHub CI. During development a standalone JVM
harness exercised the `fork` packages (330 tests at #74, Android seams stubbed). No manual or device
testing is claimed.

HV-G11-1, HV-G11-2, HV-G11-3 and HV-G11-4 all remain **MANUAL-PENDING**. They are carried into
validation_pending_gates, the hardware checklist (§4h) and MANUAL_TEST_LOG for G14 under D047.
G11 is CODE-COMPLETE with hardware validation pending; this is not a Stable-release claim.

## Next gate
This governance transition activates G12 UI Styles on `feat/ui-styles` (task
`tasks/G12_UI_STYLES.md`), owner Claude (sequential; no lease). The gate after it is G13
Experimental. G12 begins only after this PR merges, with an official-first audit against official
`56aaba2` / `dev` and the pinned NuvioGlass, Reshaped and Cxsmo sources.
