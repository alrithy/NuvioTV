# Data & Settings Migration Policy

Any change to persisted settings, profile data, caches with schema meaning, or synced payloads must be migration-aware.

## Rules
- New settings use safe defaults preserving existing official behavior unless DECISIONS explicitly says otherwise.
- Profile-scoped data remains profile-scoped.
- Device-local capability settings remain device-local.
- Do not reuse an existing key with a different type/meaning.
- Migration code must tolerate old/missing values.
- Destructive migrations require an explicit architecture decision and rollback/backup consideration.
- Imported fork settings must be mapped into our canonical owner rather than duplicating stores.
- Synced/shared payload changes require backward-compatibility analysis.

## Required tests
- clean install/default
- existing value migration
- malformed/legacy value fallback when relevant
- downgrade/rollback impact documented if not reversible

Record meaningful migrations in the PR and HANDOFF.
