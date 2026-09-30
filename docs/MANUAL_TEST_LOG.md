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
