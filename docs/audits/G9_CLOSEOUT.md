# G9 development closeout — CODE-COMPLETE

2026-09-30. This records merged work; the existing G9 source audit and D054 remain authoritative.
Scope: all 51 IDs 117–146, 169–187, 206–207. Traceability contains 40 implemented and 11
verified_official rows, with evidence for every row; no planned, in_progress, blocked or deferred rows.

| Slice | PR | Squash merge on superfork/integration |
|---|---|---|
| Source audit | #53 | 4379a17b86ef784480d3e526243f1dd2c3cd054b |
| G9a skip core | #54 | 1dc2760a7b036860c4e57ba53e21f255b243ed79 |
| G9b warnings / mute | #55 | e526e9dbfa298e3c31f45fd09bb5159d9f676728 |
| G9c post-play | #56 | 0e41e4f06ea45e35d43e75d57d1bb048c2fca1be |
| G9d shuffle / Mystery | #57 | 317afca6a05fc89a605c01cdd476f0fa6565b3b5 |
| G9e Calendar | #58 | 14f49be97cdb8ca3c7b72ef3a3da179f5968dc15 |
| G9f App Dimmer | #59 | 9e1cd8002085c6b228206f0035a0c8f4d5dabc1b |
| Closeout review corrections | #61 | 7552ef8853d3aca69ffb82f07dc3057f56246d13 |

G9f PR head verified directly on GitHub before merge:
`5dcde97b2349dd92bc403abbdca2ae06d406337b`. No reviews or unresolved review threads.
Ready for Review then squash-merged with that expected exact HEAD. Integration was fetched afterwards.

Green evidence: https://github.com/alrithy/NuvioTV/actions/runs/36749475045 belongs to this PR head.
Full Debug CI exercised GitHub's synthetic merge ref `1e5b8521cb557761eca7c1dd5587a62b43ef9046`
against integration base `14f49be97cdb8ca3c7b72ef3a3da179f5968dc15`: 2098 tests, 15 registered
failures, 0 new failures, 1 skipped, guard errors=[], Gradle exit 0, assembleFullDebug success.
Governance CI, PR Policy, Baseline Change and State Handoff succeeded; unchanged baseline replay skipped.
APK artifact: https://github.com/alrithy/NuvioTV/actions/runs/36749475045/artifacts/11114123283
(154205172 bytes, upload SHA256 7671994fb0a6e17726ccb77d808c07eca8c908520034ba9b2c7cbf9ed4d4b3d7).
Unit report artifact: 11114198234. These are actual CI values, not an all-tests-pass claim.

G9f review confirmed per-profile fork_app_dimmer storage, 0% default, clamp/presets to 90%,
no pointer/key handling on the overlay, app and NuvioDialog draw layers, player picker, flag OFF
returning official behavior and unchanged official ThemeDataStore. IMPORT_LEDGER records GPL-3.0
source/attribution and adaptations. No out-of-scope runtime changes or new network behavior.
Closeout review corrections (#61, exact head `058d99135ee8d0164db76981b82354c81b1433df`, run
https://github.com/alrithy/NuvioTV/actions/runs/36777344928: 2111 tests, 15 registered failures, 0 new,
1 skipped, fullDebug APK) close the four findings raised on this PR and one raised on #61:
raw Compose dialog and popup windows draw the same input-transparent dimmer layer (206, 207);
the Mystery route context is saved across process recreation and manual / failed-autoplay source lists
show neutral numbered cards (174–177); skip HTTP is cancellation-aware and bounded inside the
six-second provider deadline, official provider deadlines start independently and the Simkl lookups
behind AniSkip / Anime-Skip share it (131); encrypted credential writes change a non-secret cache
revision with profile identity, so corrected or rotated keys are retried (125, 126, 135).
Remaining recorded limits: some external skip endpoints and recommendation add-ons are unverified
from CI; hardware certification must evaluate every HV-G9 row honestly.

Local closeout checks: project validator, governance regression suite and git diff --check.
Android tests/build evidence comes from GitHub CI; no manual/device testing is claimed.

HV-G9-1, HV-G9-2, HV-G9-3, HV-G9-4, HV-G9-5, HV-G9-6, HV-G9-7 all remain **MANUAL-PENDING**.
They are carried into validation_pending_gates, the hardware checklist and MANUAL_TEST_LOG for G14
under D047. G9 is CODE-COMPLETE, with hardware validation pending; this is no Stable-release claim.

The separate governance transition activates G10 Live TV / feat/live-tv / tasks/G10_LIVE_TV.md,
owner claude-code (sequential; no lease; taken over from Codex at the user's request), next G11 Watch Party. G10 begins only after that PR merges,
with an official-first audit against current dev and the unchanged Reshaped source pin.
