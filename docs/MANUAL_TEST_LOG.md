# Manual / Hardware Test Log

Append entries for tests that cannot be honestly automated.

## Entry template
- Date:
- Gate / Feature IDs:
- Commit:
- Device:
- Android / firmware:
- Display / AVR / audio route:
- Network:
- Media/source characteristics:
- Player strategy:
- Test case:
- Expected:
- Actual:
- Result: PASS | FAIL | MANUAL-PENDING
- Diagnostics captured:
- Notes / follow-up:

## Batched validation policy
G1–G13 normally **record** device-only checks here as MANUAL-PENDING and continue development after automated CI/DoD passes. Do not stop each gate for a TV/device session unless the missing result is needed to design the next code safely.

G14 is the planned single hardware-certification stop: execute the accumulated checks together, replace MANUAL-PENDING with real PASS/FAIL evidence, and reopen/fix affected features when a test fails.

## Rules
- Never convert MANUAL-PENDING to PASS without a real run.
- Use the same file/source for A/B performance comparisons when possible.
- Record vendor/device quirks rather than encoding them as universal assumptions.

## 2026-09-30 — G9 carry-forward to G14
- Gate / Feature IDs: G9, 117–146, 169–187, 206–207.
- Commit: final squash 9e1cd8002085c6b228206f0035a0c8f4d5dabc1b (PR #59).
- Test cases: HV-G9-1, HV-G9-2, HV-G9-3, HV-G9-4, HV-G9-5, HV-G9-6, HV-G9-7.
- Device / Android / display / network / media: not run; to be captured in G14.
- Expected: exact procedures and expected results in docs/HARDWARE_VALIDATION_TCL_C6K.md §4f.
- Actual: no hardware execution or diagnostics captured.
- Result: MANUAL-PENDING for every listed case.
- Follow-up: execute all seven during G14, attach real evidence, reopen affected features on FAIL.
- Automated evidence is separate: docs/audits/G9_CLOSEOUT.md (2098 tests, 15 known failures, 0 new, APK).

## 2026-10-01 — G10 carry-forward to G14
- Gate / Feature IDs: G10, 208–236.
- Commit: final squash 5aba4715367d7a44ae7de1b4414393bca1989f17 (PR #69).
- Test cases: HV-G10-1, HV-G10-2, HV-G10-3, HV-G10-4, HV-G10-5, HV-G10-6, HV-G10-7, HV-G10-8.
- Device / Android / display / network / media: not run; to be captured in G14.
- Expected: exact procedures and expected results in docs/HARDWARE_VALIDATION_TCL_C6K.md §4g.
- Actual: no hardware execution or diagnostics captured.
- Result: MANUAL-PENDING for every listed case.
- Follow-up: execute all eight during G14 with real M3U / Xtream / Stalker providers, attach real evidence, reopen affected features on FAIL.
- Automated evidence is separate: docs/audits/G10_CLOSEOUT.md (2186 tests, 15 known failures, 0 new, APK).

## 2026-10-01 — G11 carry-forward to G14
- Gate / Feature IDs: G11, 237–249.
- Commit: final squash 65e1c57e828daa6323f5968adbad627c1369e490 (PR #74).
- Test cases: HV-G11-1, HV-G11-2, HV-G11-3, HV-G11-4.
- Device / Android / display / network / media: not run; to be captured in G14.
- Expected: exact procedures and expected results in docs/HARDWARE_VALIDATION_TCL_C6K.md §4h.
- Actual: no hardware execution or diagnostics captured.
- Result: MANUAL-PENDING for every listed case.
- Follow-up: execute all four during G14 with two TVs, the Nuvio Party phone build and two networks, attach real evidence, reopen affected features on FAIL.
- Automated evidence is separate: docs/audits/G11_CLOSEOUT.md (2205 tests, 15 known failures, 0 new, APK).
