# Known Baseline Unit-Test Debt

The pinned official baseline is not fully green under the complete `:app:testFullDebugUnitTest` suite on GitHub Actions.

Baseline:
`NuvioMedia/NuvioTV:dev@c257a2365ee3386b582dc2974ec235cfe0381f33`

Observed on 2026-09-28:
- 1611 tests executed
- 18 failures
- 1 skipped

These failures existed while the Superfork PR changed only documentation/governance/workflow files, so they are treated as inherited baseline debt rather than Superfork regressions.

## Policy
We still run the full suite. `scripts/superfork/check_test_failures.py` compares failures to `integration/known_baseline_test_failures.txt`.

- A known failure may remain temporarily.
- If a known failure disappears, that is an improvement and CI stays green.
- Any new failing test is a blocking regression.
- A non-zero Gradle test exit with no parsed JUnit failures is treated as infrastructure/compile failure and blocks.
- The allowlist must never be expanded merely to make a PR green. Any addition requires a documented baseline/regression investigation and decision.

## Cleanup
As upstream or Superfork work fixes known failures, remove entries from the allowlist when confidence is established.
