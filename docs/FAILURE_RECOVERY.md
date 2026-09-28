# Failure & Recovery Runbook

## Build failure after import
Stop the gate, classify compile/test/runtime/dependency/source incompatibility, compare against official API, prefer minimal adapter/port, and record blocker if unresolved.

## Agent stopped mid-task
Inspect status/diff/history; preserve files; continue coherent work on same branch; reconcile state/handoff before advancing.

## Wrong branch
Do not continue. Create the proper task branch from the correct base and move only intended commits. Do not delete evidence.

## Source moved
Pinned SHA is authoritative. Never silently substitute latest HEAD.

## Integration regression
Use official fallback/A-B path to isolate the imported delta; bisect coherent feature commits.

## Upstream conflict
Prefer current official behavior unless DECISIONS/IMPORT_LEDGER documents an intentional Superfork delta.

## Security concern
Stop merge/release, redact exposed logs/artifacts where possible, rotate affected credentials outside source control, and document remediation without storing secrets.
