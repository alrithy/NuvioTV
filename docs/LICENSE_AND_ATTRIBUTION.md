# License & Attribution Policy

The Superfork is derived from NuvioTV and imports code from other repositories. Preserve applicable upstream license notices and attribution.

## Import rules
For every external feature import, record in `docs/IMPORT_LEDGER.md`:
- source repository
- pinned SHA / commits
- files/logic imported
- local modifications
- license/notice notes

Do not strip copyright/license headers from imported files.

## Third-party components
Third-party SDKs/libraries require:
- source and version/pin
- license review
- security/dependency justification
- required notices preserved
- no arbitrary prebuilt binary copied from a fork when source/dependency form is available

Watch Party work using VDO.Ninja requires explicit review of its applicable notice/license obligations during import. AI provider APKs and any external SDKs require the same review.

## Release gate
Before stable release, audit IMPORT_LEDGER against distributed source/artifacts and verify required notices are included. This document is an engineering compliance policy, not legal advice.
