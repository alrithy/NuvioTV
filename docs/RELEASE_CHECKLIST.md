# Release Checklist

## Development artifact
- [ ] Governance validator green.
- [ ] No-new-unit-test-regressions guard green.
- [ ] APK builds from the exact commit.
- [ ] Known issues documented.

## Beta
- [ ] Relevant gate PRs merged.
- [ ] fullDebug tests/build green under baseline policy.
- [ ] Manual high-risk items are PASS or explicitly MANUAL-PENDING.
- [ ] No known Critical security/data-loss blocker.
- [ ] Experimental flags disclosed and OFF by default unless intentionally testing.
- [ ] Upgrade path from previous beta considered.
- [ ] Release notes identify risky subsystems.

## Stable
- [ ] G14 complete.
- [ ] Applicable TEST_MATRIX cases PASS.
- [ ] Fresh install PASS.
- [ ] Upgrade from prior Stable PASS.
- [ ] Android TV D-pad/focus sanity PASS.
- [ ] Low-memory sanity PASS for resource-affecting releases.
- [ ] DV/HDR/audio passthrough status validated on applicable hardware or unsupported status is explicit.
- [ ] Sensitive-log audit PASS.
- [ ] Dependency/license/import-ledger audit PASS.
- [ ] APK hashes generated.
- [ ] ABI artifacts match intended release set.
- [ ] In-app updater points to correct channel/artifacts.
- [ ] Tag/release artifacts correspond exactly to reviewed commit.
- [ ] All 320 feature rows have honest terminal/accounted status for the claimed release scope.
- [ ] Known experimental features remain clearly marked.

Stable must not be declared merely because the APK builds.
