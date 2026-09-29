# Upstream Sync Procedure

Permanent history: `docs/UPSTREAM_SYNC_LOG.md`. Every accepted sync must append an entry there.

Official upstream: NuvioMedia/NuvioTV
Integration branch: superfork/integration

## Rules
- docs/BASELINE.md preserves the original pinned baseline as history.
- New upstream sync points are appended to an upstream-sync log.
- Never mix an upstream sync with a feature port in the same commit.
- Never silently advance fork source SHAs.

## Sync workflow
1. Ensure feature tasks have no uncommitted work.
2. Fetch official upstream.
3. Create `chore/upstream-sync-YYYY-MM-DD` from superfork/integration.
4. Integrate the new official dev state.
5. Resolve conflicts using docs/DECISIONS.md.
6. Re-audit PlayerRuntimeController, PlayerMediaSourceFactory, ParallelRangeDataSource, StreamSpeedTester, PlayerDebugStatsOverlay, PlayerSettingsDataStore, SkipIntroRepository, navigation and layout stores.
7. Run full debug unit tests/build plus relevant regressions.
8. Record old/new official SHA, conflicts and decisions.
9. PR only the upstream sync into superfork/integration.
10. Rebase active feature branches only after the sync is accepted.

## Feature convergence
If official gains a feature already imported here, compare implementations, prefer official ownership when equivalent/better, migrate only our unique delta, remove duplication with tests, and update the ledger/decisions.

## Baseline freeze policy
Before the first implementation gate begins, refresh to the latest reviewed official `dev` available at that moment.

After G0 starts, do **not** chase every upstream commit continuously inside feature branches. Upstream movement must use dedicated sync PRs and should normally occur:
- between gates;
- before beginning a gate that will touch an upstream subsystem that changed materially;
- for a critical official bug/security fix;
- when drift becomes large enough to increase future merge risk.

An active feature PR must not silently absorb unrelated upstream commits.

This balances two goals: start from current official code, and avoid permanent moving-target development.
