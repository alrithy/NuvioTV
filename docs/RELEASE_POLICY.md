# Release architecture and acceptance — canonical

G14 owns distribution, security closure and final regression certification. Gate/task
summaries cannot weaken this contract. RELEASE_CHECKLIST is a compatibility redirect.

## Channels and updater
- Development: exact task/integration commit artifacts; not a Stable claim.
- Beta: separate feed/version/channel identity; explicit known issues and manual gaps.
- Stable: only certified scope, signed artifacts and complete applicable regression evidence.
Updater must select the intended repository/channel/ABI, validate version/checksum, reject
wrong signatures/ABI/corrupt downloads, preserve channel choice and fail safely offline.
Test Stable/Beta selection, downgrade/reinstall behavior and official-server return path.

## Artifacts and provenance
Publish arm64-v8a, armeabi-v7a and universal APKs as explicitly supported by release build.
Each artifact needs SHA-256, exact tag/commit, build workflow/run, signing identity (never
private key), dependency/source provenance and required license notices. Manifest hashes
must match downloadable bytes; test fresh install and upgrade from prior shipped build.
Keep debug CI keystores separate from real release signing; no secrets in logs/artifacts.

## Beta acceptance
Required CI green under documented debt policy; successful build; no critical security or
data-loss blocker; high-risk manual cases PASS or explicitly MANUAL-PENDING; experimental
AI/MAT OFF by default; upgrade risk and known debt disclosed in release notes.

## Stable / G14 exit
- Applicable TEST_MATRIX cases PASS, device/OS/output chain/commit evidence recorded.
- Fresh install, upgrade, TV focus, Arabic/RTL and resource-tier tests pass.
- Playback fallback, DV/HDR/audio/AFR support certified only for tested hardware; limits explicit.
- Sensitive URL/header/credential, Watch Party, AI integrity and QR-origin audits complete.
- IMPORT_LEDGER, license, dependency and source pin audits complete.
- ABI APKs/checksums/updater/feed/signing/provenance verified against exact tagged commit.
- All 320 rows accounted for; blocked/deferred features disclosed and excluded from release claims.
- Required baseline debt is fixed or explicitly accepted for the released scope; never hidden.
- Automated upstream sync checks scheduled by repository CI: compare official dev with accepted
  pin, publish reviewable drift report, never auto-merge or silently update pins. Document
  schedule, permissions, failure alert and replay procedure. G14 must implement/verify it.
- Final regression certification records commit, CI runs, device matrix, remaining exclusions,
  reviewer and release decision. A built APK alone is not certification.
