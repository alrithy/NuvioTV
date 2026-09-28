# Test Strategy

## T0 — Governance
Mandatory on every Superfork PR:
- canonical state validator;
- 320/320 traceability integrity;
- baseline/source consistency;
- PR state/handoff policy.

## T1 — Build parity
Every PR must produce the supported fullDebug build unless the task is documentation-only and CI policy explicitly allows otherwise.

## T2 — Full unit suite / no-new-regressions
Run the complete `:app:testFullDebugUnitTest` suite.

The current official-based baseline has recorded pre-existing failures in:
`integration/baseline_test_failures.txt`.

Policy:
- any NEW unit-test failure fails CI;
- known baseline failures may remain temporarily;
- resolved baseline failures are welcomed and should be removed from the debt list in a dedicated cleanup;
- compilation/test-run failure that produces no JUnit XML is a hard failure.

See `docs/BASELINE_TEST_DEBT.md`.

## T3 — Gate-specific automated tests
Every imported feature ports/adapts relevant source tests or adds equivalent tests in the same gate.

Examples:
- subtitle drift/confidence cases;
- stream ranking and fallback;
- resource tier boundaries;
- skip evidence merge;
- watch-party drift logic;
- source failure isolation.

## T4 — Integration regression
Use `docs/TEST_MATRIX.md`.

Playback-changing work requires A/B against official behavior when feasible:
- same file/source;
- same device/network;
- same measurement window;
- startup time;
- throughput;
- buffer ahead;
- rebuffer count/duration;
- seek latency/correctness;
- RAM;
- dropped frames;
- audio/HDR output state.

## T5 — Hardware/manual
Some behavior cannot be honestly validated in ordinary CI:
- Dolby Vision output;
- TrueHD/DTS-HD passthrough;
- AFR/eARC renegotiation;
- high-bitrate real-device playback;
- TV D-pad/focus feel;
- device thermal behavior;
- Live TV provider/device edge cases.

Status must be one of:
- PASS
- FAIL
- MANUAL-PENDING

MANUAL-PENDING is acceptable for development/beta only where the gate/release policy permits it. Never convert MANUAL-PENDING to PASS without execution evidence.

## Regression ownership
If a gate touches a known baseline-failing component/test, the PR must explicitly state whether it fixes, preserves or supersedes that debt.

## Test evidence
PR/handoff records:
- commands;
- CI run;
- device/environment for manual tests;
- relevant fixtures/source;
- failures and unresolved risks.
