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
