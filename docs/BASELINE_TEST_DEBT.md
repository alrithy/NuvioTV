# Baseline test debt — policy and evidence

The accepted baseline is `fd7973d91dd75d790c5f9b3d68dae652655e92c4`; integration snapshot
`3cf04ccdcc20515acb093c28ad9b7c3943a39057` contains no Superfork feature code. A tree diff
found only a missing final newline in ModernHomeContent.kt among production files; all
unit tests/build inputs are unchanged. Do not describe that as byte-identical application code.
Official PR #3720's green job ran updater tests plus assemble, not the full suite.

Original observation: [run 36479376404](https://github.com/alrithy/NuvioTV/actions/runs/36479376404),
job 109121079197: 1,611 tests, 18 failed, 1 skipped. Independent clean replays are recorded
in `integration/evidence/` and per-entry evidence in `integration/baseline_test_debt.json`.
The JSON is the **only** exception registry. It records fully qualified class#method,
baseline SHA, date, reason, CI evidence, source file and removal rule for every entry.
Old TXT paths are redirects and cannot be passed to the runner.

## Mandatory execution
`python3 scripts/superfork/run_full_unit_suite.py`

The Gradle init script sets ignoreFailures only on the complete test task so assertion
results reach the classifier ([Gradle Test API](https://docs.gradle.org/current/dsl/org.gradle.api.tasks.testing.Test.html)).
It never suppresses the Gradle process exit. No `continue-on-error`, shell catch-all, test
filter or disabled suite is permitted. The runner deletes old generated XML, forces fresh
execution, requires a suite-completion receipt, checks XML counts/identifiers, verifies the
reviewed full test inventory and minimum count, then invokes the canonical classifier.

| Outcome | Result |
|---|---|
| Exact known failures remain | Regression guard passes; debt remains visible |
| Any other fully qualified test fails | CI fails |
| Gradle/compiler/dependency/worker failure | CI fails even if old/partial known failures exist |
| Missing/empty/malformed XML, partial suite, absent test | CI fails |
| Newly skipped baseline test or skipped/missing known failure | CI fails; never classified as fixed |
| Known test executes and passes | Reported as resolved candidate; verify and explicitly remove its debt entry |
| Removed debt fails again | CI fails as a new regression |

`baseline-suite.json` preserves the full observed inventory, including the pre-existing
skip. Deleting/renaming/skipping a baseline test requires explicit reviewed inventory
change with replacement coverage, never automatic baseline growth.

## Baseline changes / ratchet
Only a deliberate PR may edit debt. The normal runner never writes the exception registry.
Remove a fixed entry after a full-suite pass of that exact test and another confirming run;
attach evidence. CI permits reductions, unlike the old validator's fixed count of 18.
Additions, lower coverage floor, removed inventory cases, or additional skips require a
`## Baseline debt change` PR section and a maintainer's exact-head approval. Reproduce the
failure on a clean accepted baseline first; record classification, source SHA, environment,
evidence and next fix. Never justify a new entry solely because a feature PR fails.

## Logs and ownership
CI consumes no production secrets. Publish only safe identifiers/outcomes/counts; raw
JUnit stdout/stderr, stack payloads and Gradle logs are not uploaded. No signed URLs,
headers, keys or local properties belong in logs/artifacts. A gate touching a debt test
must explicitly fix, preserve with evidence, or supersede it; a green guard is not a PASS
claim for those failing tests. Hardware status is independent.

## Initial audit result / explicit D039 classification
Clean integration: 1,611 tests, 18 failed, 1 skipped. Clean official: 1,611 tests,
19 failed, 1 skipped. The additional HomeEnrichmentRepositoryBoundaryTest case passed
on integration with identical inputs, so the registry has **19 exact entries: 18 reproduced
and 1 intermittent**. The initial official guard failure is retained unchanged in evidence;
D039 explicitly classifies the additional failure after source/clean-baseline review.
This is not a claim that governance caused it or that its underlying defect is fixed.
The original registration SHA/date are immutable across later accepted upstream syncs.

Independent official replay, attempt 2, job 109154439698: 1,611 tests, 18 failed,
1 skipped, zero new failures. The extra HomeEnrichment test passed. This confirms varying
outcomes; it does not establish a fix. Both attempt 1 failure and attempt 2 pass are retained.
