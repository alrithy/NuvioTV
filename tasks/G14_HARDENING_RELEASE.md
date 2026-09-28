# Task Packet — G14 Hardening / Distribution / Upstream
Feature IDs: 293–310, 312, 316, 320
Branch: `chore/release-hardening`
Depends on: G13

## Objective
Turn integrated work into a releasable, maintainable product.

## Scope
Validate official self-host/custom-server paths; security closure; Keystore/profile secrets; QR/session/CORS protection; APK hashes/ABI outputs; updater; stable/beta channels; release CI; automated upstream checks; final 320-feature accounting.

## Mandatory audits
All 320 traceability rows honest/accounted; IMPORT_LEDGER/license/dependency/sensitive-log audits; fresh install + upgrade; TEST_MATRIX; manual high-risk hardware results; release artifact/tag reproducibility.

## Release
Use `docs/RELEASE_CHECKLIST.md`. Stable is not allowed merely because an APK builds.
