# Task Packet — G14 Hardening / Distribution / Upstream
Feature IDs: 293–310, 312, 316, 320
Branch: `chore/release-hardening`
Depends on: G13

## Objective
Run the final consolidated hardware-certification campaign before Stable release, then complete release/upstream hardening.

## Mandatory hardware certification
- Read `integration/state.yaml: validation_pending_gates`, `docs/MANUAL_TEST_LOG.md`, gate audits and hardware checklists.
- Execute every release-blocking MANUAL-PENDING check accumulated from G1–G13 in one planned device-validation campaign.
- Include `docs/HARDWARE_VALIDATION_TCL_C6K.md` and later gate-specific TV/audio/DV/AFR/subtitle/seek/Live TV/Watch Party/UI checks.
- Record real PASS/FAIL evidence. Never infer PASS from CI.
- Any FAIL reopens the affected gate/feature for a focused fix and re-test.
- Stable release is forbidden while a release-blocking MANUAL-PENDING remains.

Turn integrated work into a releasable, maintainable product.

## Scope
Validate official self-host/custom-server paths; security closure; Keystore/profile secrets; QR/session/CORS protection; APK hashes/ABI outputs; updater; stable/beta channels; release CI; automated upstream checks; final 320-feature accounting.

## Mandatory audits
All 320 traceability rows honest/accounted; IMPORT_LEDGER/license/dependency/sensitive-log audits; fresh install + upgrade; TEST_MATRIX; manual high-risk hardware results; release artifact/tag reproducibility.

## Release
Use `docs/RELEASE_CHECKLIST.md`. Stable is not allowed merely because an APK builds.