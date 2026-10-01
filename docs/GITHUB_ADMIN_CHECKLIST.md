# GitHub Admin Checklist

Some repository-level controls cannot be enforced from committed source files. These are owner/admin actions.

## Remaining manual admin actions (development may continue)

### A. Default branch
- [ ] Change repository default branch from `dev` to `superfork/integration`.
- [ ] Verify opening the repository normally lands on `superfork/integration`.
- [ ] Keep `dev` as legacy/upstream mirror only.

Until this is done, `dev` contains safety redirect files that tell coding agents to switch to the Superfork branch.

### B. Protect `superfork/integration`
Create a branch ruleset/protection rule:
- [ ] Require pull request before merge.
- [ ] Block force pushes.
- [ ] Block branch deletion.
- [ ] Require conversation resolution if supported.
- [ ] Require branch to be up to date before merge where practical.

### C. Required status checks
After the final governance workflows are merged and have run at least once, require the exact check names visible in GitHub:
- [ ] `Superfork Governance CI`
- [ ] `Superfork Full Debug CI`
- [ ] `Superfork PR Policy`
- [ ] `Superfork State Handoff`
- [ ] `Superfork Baseline Change`

Do not guess check names in GitHub settings; select the actual checks emitted by Actions.

### D. Fork releases (G14b, D064)
- [ ] Add the release signing secrets the release workflow reads (`NUVIO_RELEASE_KEYSTORE_BASE64`, `NUVIO_RELEASE_KEY_ALIAS`, `NUVIO_RELEASE_KEY_PASSWORD`, `NUVIO_RELEASE_STORE_PASSWORD`) and the `LOCAL_PROPERTIES_BASE64` / `LOCAL_DEV_PROPERTIES_BASE64` build configuration; keep the keystore offline as well.
- [ ] Run "Build Android TV Release" in `dry-run`, then `draft`, and publish the first fork release; its assets must show a `sha256:` digest in the GitHub release API.
- [ ] Decide the distribution identity: keep `com.nuvio.tv` (replaces official; installs need official removed because the signers differ) or a fork application id (installs beside official).

### E. Device test builds (D066)
Repository → Settings → Secrets and variables → Actions → New repository secret:
- [ ] Backend, one of:
  - `LOCAL_PROPERTIES_BASE64` = `base64 -w0 local.properties` of a properties file with at least
    `NUVIO_SUPABASE_URL` and `NUVIO_SUPABASE_ANON_KEY` (plus `TMDB_API_KEY`, `TRAKT_CLIENT_ID`,
    `TRAKT_CLIENT_SECRET`, ... as available); optionally `LOCAL_DEV_PROPERTIES_BASE64` the same way;
  - or just `SUPERFORK_SUPABASE_URL` and `SUPERFORK_SUPABASE_ANON_KEY`.
- [ ] Fixed test signing key, created once and kept (losing it means one uninstall):
  `keytool -genkeypair -keystore superfork-test.jks -alias superforktest -keyalg RSA -keysize 4096 -validity 10000 -dname "CN=Superfork Test"`,
  then `SUPERFORK_TEST_KEYSTORE_BASE64` = `base64 -w0 superfork-test.jks`, `SUPERFORK_TEST_KEYSTORE_PASSWORD`
  = its password, `SUPERFORK_TEST_KEY_ALIAS` = `superforktest` (and `SUPERFORK_TEST_KEY_PASSWORD` only
  if the key password differs).
- [ ] Re-run "Superfork Test Build" (or push to `superfork/integration`); the run summary shows which
  secrets are present (names only), the package, version and signing-certificate SHA-256.

## Recommended before high-risk gates
- [ ] Require at least one review for playback/audio/security/release PRs if another reviewer is available.
- [ ] Require CODEOWNERS review where practical.
- [ ] Restrict direct pushes to integration.

## Before Stable release
- [ ] Verify branch protection still applies.
- [ ] Verify release workflow permissions/secrets.
- [ ] Verify no bypass actor is routinely merging red CI.
- [ ] Review repository collaborators/integrations that can write to release/integration branches.

## Optional
- [ ] Enable Issues if an additional human planning surface is desired. Issues are currently disabled; canonical work tracking is repository state + traceability + PRs.
- [ ] Auto-delete merged task branches if desired.
- [ ] Suggested labels if Issues are enabled: `gate`, `blocked`, `experimental`, `playback`, `subtitles`, `livetv`, `security`, `upstream-sync`.

## Access verified during governance hardening
2026-09-28: repository metadata reports account admin rights, but the connected GitHub
App does not expose admin writes. Reading `/branches/superfork/integration/protection`
returned HTTP 403 `Resource not accessible by integration`. Rulesets GET returned `[]`;
branch metadata reported unprotected. Default branch was `dev`.
This is a connector capability limit, not evidence that the account lacks ownership.
No browser workaround or settings change is claimed. Existing dev redirects were verified.

## Completion record
When these are configured, update `integration/state.yaml` risks R001/R004 and `docs/PROJECT_STATUS.md` in a small governance PR. Do not leave the repository claiming these admin controls are open after they have been enabled.
