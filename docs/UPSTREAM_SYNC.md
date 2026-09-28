# Upstream Sync Policy

Official upstream is `NuvioMedia/NuvioTV:dev`.

The pinned baseline is immutable evidence. A later sync creates a new recorded sync point; it does not rewrite history.

## Sync when
- a new high-risk gate touches code changed materially upstream;
- release hardening begins;
- upstream gained a feature/fix we were about to port.

## Procedure
1. Fetch official upstream and record old/new SHA.
2. Review commits touching relevant subsystems.
3. Use dedicated `chore/upstream-sync-YYYYMMDD` branch.
4. Resolve conflicts preferring current official behavior unless an intentional Superfork delta is documented.
5. Run validator, fullDebug tests/build and affected gate checks.
6. Update baseline/source docs only if the project intentionally advances.
7. Record redundant/removed fork deltas in IMPORT_LEDGER.
8. Merge via PR.

Never silently force integration to upstream HEAD or retain duplicate fork code after official gains equivalent behavior.
