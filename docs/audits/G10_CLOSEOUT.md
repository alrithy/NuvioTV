# G10 development closeout — CODE-COMPLETE

2026-10-01. This records merged work; the G10 source audit (`docs/audits/G10_LIVE_TV_AUDIT.md`) and
D055 remain authoritative. Scope: all 29 IDs 208–236. Traceability contains 29 implemented rows with
evidence for every row; no planned, in_progress, blocked or deferred rows. No ID is verified_official:
official has live-playback plumbing (type `channel`, live latch, live-window retry) but no Live TV.

| Slice | PR | IDs | Squash merge on superfork/integration |
|---|---|---|---|
| Source audit | #62 | 208–236 (plan) | 43a1dfb8af75618563cfc785a79f9a9370cca3a2 |
| G10a sources, readers, encrypted storage | #63 | 208–212, 233 (part) | 09b808ad902c9d5d1bcf6a8d1468402aaee91a89 |
| G10b screen, menu, organisation | #64 | 208, 219–225 | 93ddc6ee12dda68c31b80defb3c7c13df1f12ae8 |
| G10c guide | #65 | 214–218, 225 | 4bc3621000d7ab383fc1d64d2cf20c03e6cc27e9 |
| G10d live playback rules, live AFR | #66 | 233–236 | 4c97b1bd4a43a312645d91fefaac35e0c1f48079 |
| G10e in-player zapping, panels, Now/Next | #67 | 216, 228–232 | 55c7b037926490aaa07ea20ab9b9a147b53c96af |
| G10f channel preview | #68 | 226, 227 | f374ccf81eea7c7eb26d9b9589ab2499145fbe62 |
| G10g phone setup via QR | #69 | 213 | 5aba4715367d7a44ae7de1b4414393bca1989f17 |

Exact-head Full Debug CI (each PR squash-merged with its expected head, integration fetched after):

| PR | Head | Run | Tests | Registered failures | New | Skipped | APK artifact |
|---|---|---|---|---|---|---|---|
| #65 | f6fe186718f0cad36d1cebd6d2fb1a171bf28e71 | 36791938657 | 2169 | 15 | 0 | 1 | 11133235363 |
| #66 | f1a48f024724b8357f9b10ac749a5521028eeb4c | 36827186218 | 2175 | 15 | 0 | 1 | 11146585441 |
| #67 | dc0dfb321454465f8e4faaadaa1a3a8a47cd6082 | 36828493852 | 2177 | 15 | 0 | 1 | 11146921479 |
| #68 | 6a7fe12d86b09354ea1ca6b44f2996a173afc964 | 36829761290 | 2181 | 15 | 0 | 1 | 11147038290 |
| #69 | 70035de8baa51cc276347ae922a58d7f53909d35 | 36831015949 | 2186 | 15 | 0 | 1 | 11147825837 |

Earlier slices: #62 2111 tests, #63 2136, #64 2143 (15 registered, 0 new each; recorded in HANDOFF).
Governance CI, PR Policy, Baseline Change and State Handoff succeeded on every head; the unchanged
baseline replay was skipped. These are actual CI values, not an all-tests-pass claim.

Review: #65 received eight Codex review findings (source-scoped guide identities, SHA-256 guide
filenames / cache v2, duplicate-id aliases and logos, partial XMLTV reads never cached as complete,
cancellation of guide IO when the last collector leaves, Refresh reloading guides when every source
fails, AdaptiveResources download budgets, 216 kept in_progress until displayed); all were fixed on the
PR before merge and every thread was resolved. The Codex reviewer reached its usage limit from #67 on;
#66–#69 had no review threads.

What G10 delivers (D055): one `fork/livetv` owner; M3U, Xtream and Stalker sources Keystore-encrypted
per profile; channel identity a hash, never the link; its own OkHttp client without logging and
host-only logs; Stalker auth headers only to the portal host; the list screen with categories,
favorites, hiding, order, names, search and the last channel; the XMLTV guide (now, next, progress,
time left, guide logos) bounded by AdaptiveResources; channels on the official player as type
`channel`, with live-only rules gated on an in-memory playback registry so VOD is unchanged; live AFR
on the G5e `TrackAfrPolicy` owner; in-player zapping in the picked list with the ExoPlayer kept, a
channel list, categories and a Now/Next card; a channel preview sized by AdaptiveResources (off by
default on low-RAM boxes); setup from a phone through a token-path LAN page. LIVE_TV is AUTO; the
menu entry stays off per profile until the user turns it on.

Not imported, by design: Reshaped's separate AFR switcher, its second device-memory check, its
full-screen guide grid, URL-keyed persisted state and app-wide preferences (see IMPORT_LEDGER G10a–G10g).

Local closeout checks: project validator, state views, governance regression suite and git diff
--check. Android tests and build evidence come from GitHub CI; a standalone JVM harness exercised the
`fork` packages during development (Android seams stubbed). No manual or device testing is claimed.

HV-G10-1, HV-G10-2, HV-G10-3, HV-G10-4, HV-G10-5, HV-G10-6, HV-G10-7, HV-G10-8 all remain
**MANUAL-PENDING**. They are carried into validation_pending_gates, the hardware checklist (§4g) and
MANUAL_TEST_LOG for G14 under D047. G10 is CODE-COMPLETE with hardware validation pending; this is no
Stable-release claim.

This governance transition activates G11 Watch Party / feat/watch-party / tasks/G11_WATCH_PARTY.md,
owner Claude (sequential; no lease), next G12 UI Styles. G11 begins only after this PR merges, with an
official-first audit against current `dev` and the unchanged Reshaped source pin.
