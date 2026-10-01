# Dependency & Supply-chain Policy

## Default
Prefer the existing official dependency graph. New dependencies require a concrete feature need.

## Before adding a library/service
Record:
- purpose and gate/feature IDs;
- license;
- source/repository;
- maintained status;
- size/runtime impact;
- network/security implications;
- Android/API requirements;
- replacement/removal path.

## Rules
- Prefer source-auditable dependencies from established repositories.
- Do not import arbitrary binary APK/JAR/AAR artifacts copied from fork releases when source integration is available.
- Pin or constrain versions according to the project's Gradle conventions.
- Do not weaken TLS/security configuration globally for one permissive addon/provider.
- New network providers should receive explicit clients/timeouts/trust boundaries.
- New Android permissions require an explicit architecture/security review and a user-facing need.
- Provider SDKs that can collect data require explicit review; do not add analytics/telemetry incidentally.
- Preserve GPLv3 and applicable third-party notices.
- VDO.Ninja/other Watch Party dependencies require a license/notice audit at G11.
- AI provider APKs remain isolated and require hash + signer verification.

## Dependency removal
A feature flag is not enough if disabling an experimental provider still initializes expensive/untrusted runtime code. Experimental architecture should allow the behavior/provider to be absent or inert.

## Reviewed dependencies (G14 accounting)
- `tv.seekr:seekr-android:0.2.0` (G7a): seek-preview thumbnails from Seekr for the
  bounded fallback. Apache-2.0, source at github.com/AKhalil609/seekr-android-sdk. Network: Seekr's API
  only with the user's own key (per profile, KeystoreCipher), only when local keyframes cannot serve;
  removal path: the local keyframe engine keeps working without it (SEEK_INTELLIGENCE gates it).
