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