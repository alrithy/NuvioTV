# Baseline Full-Suite Test Debt

## Why this exists
The Superfork intentionally runs the entire `:app:testFullDebugUnitTest` suite, which is stricter than the current official Nuvio PR workflow.

On the official-based integration baseline `fd7973d91dd75d790c5f9b3d68dae652655e92c4`, the full suite produced **18 pre-existing failures** before any Superfork feature implementation.

The official PR that produced this baseline, NuvioMedia/NuvioTV #3720, had a green **PR Full Debug Build** on its source head `514771f21221169ed209ca584fb850bee34d9bcf`. That official workflow validates updater-focused unit tests plus the debug build; it does not require the complete 1,611-test suite to be green.

## Policy
We do not:
- hide these failures;
- mark them as passing;
- require unrelated feature PRs to fix all upstream debt before work can begin.

We do:
- run the full unit-test suite on every Superfork PR;
- compare actual failures against `integration/baseline_test_failures.txt`;
- fail CI if **any new failing test** appears;
- allow known baseline failures to remain temporarily;
- allow known failures to disappear without penalty;
- remove entries from the baseline list once a failure is deliberately fixed and verified.

This is a **no-new-regressions** policy, not a waiver of testing.

## Baseline observation
Observed in Superfork CI run 36479376404 on 2026-09-28 against a merge containing official baseline `fd7973d...` and governance-only changes. Governance validation passed; the full unit suite completed 1,611 tests with 18 failures and 1 skipped.

No runtime/application feature code was introduced by the governance changes that exposed this debt.

## Guard behavior
`scripts/superfork/check_baseline_test_failures.py` reads Gradle JUnit XML:
- if test execution produced no XML, CI fails;
- if a failure is not in the baseline allowlist, CI fails;
- if only baseline failures remain, CI passes the regression guard;
- if fewer baseline failures remain, CI passes and the debt list should be pruned in a dedicated cleanup change.

## Ownership
Baseline debt is project debt, not permission to ignore failures in new or modified code. A gate that touches one of these tests/components must explicitly decide whether to fix, preserve, or supersede the baseline failure and document that in the PR/handoff.
