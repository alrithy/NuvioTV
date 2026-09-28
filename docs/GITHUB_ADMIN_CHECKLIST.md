# GitHub Admin Checklist

Source files cannot enforce all repository-level settings. Complete this after the bootstrap governance PR is merged.

## Required
- [ ] Change repository default branch from dev to superfork/integration.
- [ ] Add a branch ruleset/protection rule for superfork/integration.
- [ ] Require pull requests before merging.
- [ ] Block force pushes and branch deletion.
- [ ] Require status checks:
  - Superfork Governance CI
  - Superfork Full Debug CI
  - Superfork PR Policy
  - Superfork State Handoff
- [ ] Require branch up-to-date before merge where practical.
- [ ] Keep dev as legacy/upstream mirror, not the Superfork product branch.

## Recommended
- [ ] Require one human review for high-risk playback/audio/security/release PRs when practical.
- [ ] Auto-delete merged task branches if desired.
- [ ] Enable Issues only if you want an additional tracking surface. Issues are currently disabled; canonical tracking is state + traceability + PRs.
- [ ] Suggested labels if Issues are enabled later: gate, blocked, experimental, playback, subtitles, livetv, security, upstream-sync.
