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
- [ ] Backend: nothing needed for official's account; the workflow reads the public client
  configuration (anon key) from official's latest release APK. Only to use another backend or to
  add TMDB / Trakt / ... keys, one of:
  - `NUVIO_SUPABASE_URL` and `NUVIO_SUPABASE_ANON_KEY` (optional `NUVIO_SUPABASE_FALLBACK_URL`), the
    names other Nuvio forks use;
  - or `LOCAL_PROPERTIES_BASE64` = `base64 -w0 local.properties` of a full properties file (those two
    keys plus `TMDB_API_KEY`, `TRAKT_CLIENT_ID`, `TRAKT_CLIENT_SECRET`, ... as available), optionally
    `LOCAL_DEV_PROPERTIES_BASE64` the same way.
- [ ] Fixed test signing key (required): `SUPERFORK_TEST_KEYSTORE_BASE64` (the base64 PKCS12 keystore)
  and `SUPERFORK_TEST_KEYSTORE_PASSWORD`. The alias defaults to `superforktest`
  (`SUPERFORK_TEST_KEY_ALIAS` only if it differs). The workflow pins the certificate SHA-256
  (`SUPERFORK_TEST_CERT_SHA256`), so a different key fails before the build. A replacement key
  (`keytool -genkeypair -storetype PKCS12 -keystore superfork-test.p12 -alias superforktest -keyalg RSA -keysize 4096 -validity 10000`)
  needs the pin updated and one uninstall on every device.
- [ ] Account integrations (optional, one secret each; without them the app says the service is not
  configured). These are the maintainer's own registered apps; official's are never reused:
  - `TRAKT_CLIENT_ID` and `TRAKT_CLIENT_SECRET`: a Trakt API app (trakt.tv → Settings → Your API
    Apps → New application; Redirect URI `urn:ietf:wg:oauth:2.0:oob`, the device-code login).
  - `SIMKL_CLIENT_ID`: a SIMKL developer app (simkl.com → Settings → Developer).
  - `MDBLIST_CLIENT_ID`: an MDBList app client id (mdblist.com, the account's API / developer
    settings).
  - `TMDB_API_KEY`: a TMDB v3 API key (themoviedb.org → Settings → API).
  - `PREMIUMIZE_CLIENT_ID`: a Premiumize device-login client id (from Premiumize), only for
    Premiumize debrid login.
  The run summary's "Account integrations" table shows each one as set or missing (names only), and
  the APK check fails if a provided one did not reach `BuildConfig`.
- [ ] Re-run "Superfork Test Build" (or push to `superfork/integration`); the run summary shows which
  secrets are present (names only), the official APK it read, the package, version, the
  signing-certificate SHA-256, the account integrations and the TV QR login session result.

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
