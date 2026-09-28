# GitHub Admin Checklist

Source files cannot enforce all repository-level settings.

## Required
- [ ] After bootstrap docs are merged, change default branch from `dev` to `superfork/integration`.
- [ ] Protect `superfork/integration`: PR-only, no force pushes.
- [ ] Require `Superfork Governance CI`.
- [ ] Require `Superfork Full Debug CI`.
- [ ] Require branch up-to-date before merge where practical.
- [ ] Keep `dev` as legacy/upstream mirror, not the Superfork product branch.

## Recommended
- [ ] Review requirement for high-risk playback/audio/security PRs when a reviewer is available.
- [ ] Auto-delete merged task branches if desired.
- [ ] Labels: gate, blocked, experimental, playback, subtitles, livetv, security, upstream-sync.
