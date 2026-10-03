# NETFLIX_THEME — explicit-user-request

Task ID: NETFLIX_THEME
Branch: feat/netflix-theme
Authorization: explicit-user-request
Feature ID(s): 188, 189, 190, 191, 192, 195, 201, 202, 203, 205

Authorized branch: `feat/netflix-theme`, based on `superfork/integration`
`1f63418e33ac53e9ebd9428c2c30adaba974bb79`, advanced to reviewed integration
`e07d684009bcb5b9c41d00c777b11b2318752c53` to preserve the G14 device fixes
and instrumentation signature correction (#95–#98). No official baseline pin changed.

This separate task is explicitly requested by the repository owner in this session.
It extends the presentation seams of feature IDs 188, 189, 190, 191, 192, 195,
201, 202, 203 and 205. It does not reopen or certify G14, replace the accepted
official baseline, change the existing 320-feature scope, or claim that a
completed official layout has been replaced. G14's blocked hardware campaign
and canonical state remain intact.

## Mandatory current-Netflix parity audit

Before any further Netflix-theme implementation, read and follow:

- docs/NETFLIX_TV_2026_PARITY_AUDIT.md

That audit was created after reviewing the current Netflix TV Help/Tudum/About sources and the
actual Superfork Netflix Visual #3 artifacts from commit 6a8389a. It supersedes any earlier assumption
that the pre-2025 Netflix left sidebar is the target. P0/P1 items in that document are part of this
task's acceptance criteria. In particular: current Netflix TV navigation is top-of-screen, My Netflix
is a hub rather than a My List label, Home is focus-driven/information-rich, and the five failing EN/AR
instrumentation interactions must be fixed rather than skipped. Do not merge until the audit's Done
definition is satisfied or the maintainer explicitly waives an item.

## Requested outcome

An independent `Netflix` theme selected in Settings → Appearance → Theme,
persisted through existing profile-scoped ThemeDataStore and profile settings
sync. Reuse the current theme, Modern home/enrichment, existing navigation ROUTER/route ownership,
details/episodes, search, profile and player presentation owners. For AppTheme.NETFLIX, the visible
navigation presentation must follow the current Netflix TV top-navigation model; do not preserve the
old left sidebar merely because it already exists. Other Nuvio themes must keep their existing navigation. Retain the
existing Classic/Grid/Modern and Glass/Cinematic Glass preferences, and restore
their presentation when Netflix is deselected.

Cover cinematic Home hero, compact landscape rows, delayed expanded preview,
factual metadata and genuine Play/List/Info actions; movies/series navigation;
details, episodes, remote keyboard/live search/history; cinematic profiles;
player controls and shared dialogs/loading/empty/error chrome. Use centralized
black/white theme, dimensions, typography and motion tokens and existing
AdaptiveResources policy. Arabic/RTL, D-pad focus, Back and resume are correctness.

No Netflix logos, artwork, proprietary fonts, internal files, hardcoded catalog,
WebView, new backend/router/playback engine, whole-fork merge or animation
dependency is authorized. New code is authored in this repository, extending
the existing Nuvio owners, rather than ported from a Netflix client.

## Evidence and completion

Run governance validation/regressions, the full unit suite with the existing
exact baseline guard, `assembleFullDebug`, theme instrumentation, full Superfork
CI and Superfork Test Build. Capture/review the eleven requested major screens
at 1080p and 4K, including English LTR and Arabic RTL. The matrix and reproducible
commands live in `docs/NETFLIX_THEME_VISUAL_VERIFICATION.md`.

There is no unmeasured 99.99% fidelity or TCL 60fps claim. A software emulator
cannot certify TCL C6K frame times, remote feel or HDR/DV/AFR/audio output. Missing
stream quality/match/user-rating facts must remain absent, rather than invented.
Do not merge while visual verification is incomplete. Record remaining gaps
and hardware checks honestly; open a draft review artifact if those checks are
still pending, without marking this task Done.

## Current status

IN_PROGRESS. Implementation, local builds and visual evidence are being prepared.
The recorded G14 task remains blocked on its existing maintainer campaign.
