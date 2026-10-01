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

Reviewed (G11a): `app/src/main/assets/watchparty/vdoninja-sdk.js` is the VDO.Ninja SDK v1.6.1 by
Steve Seguin, MPL-2.0, taken unmodified from the upstream npm release `@vdoninja/sdk@1.6.1`
(repository `github.com/steveseguin/ninjasdk`), in its Source Code Form (SHA-256
`88f623ac8b6dcf2ab66b4bddc70b66f1a2d7d04198f143a6efe1b0d4c89c440c`) with its MPL-2.0 header kept and
the license text beside it (`LICENSE-vdoninja-sdk.txt`). MPL-2.0 is file-level and compatible with
the GPL-3.0 larger work. The SDK license grants no right to use VDO.Ninja's hosted signaling or
STUN / TURN services (their terms apply) and no trademark rights; the app does not use the VDO.Ninja
name or claim affiliation. The Watch Party code around it is a GPL-3.0 port of AntoninoScardina/NuvioTV.

## Release gate
Before stable release, audit IMPORT_LEDGER against distributed source/artifacts and verify required notices are included. This document is an engineering compliance policy, not legal advice.

Reviewed (G14 accounting): `tv.seekr:seekr-android:0.2.0` (G7a) is Apache-2.0
(github.com/AKhalil609/seekr-android-sdk), consumed as a Maven dependency, not copied; Apache-2.0 is
compatible with the GPL-3.0 larger work; this record and the IMPORT_LEDGER G7a entry are its
attribution.
