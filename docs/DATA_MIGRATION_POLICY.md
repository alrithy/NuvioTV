# Settings & Data Migration Policy

Many fork features add settings. Treat persistence compatibility as a core product requirement.

## Rules
- Never rename/delete an existing official preference key without a migration.
- Every new setting has an explicit safe default that preserves official behavior unless the product decision says otherwise.
- Keep profile-scoped settings profile-scoped; do not leak one profile's credentials/preferences to another.
- Secrets use the approved Keystore-backed path, not ordinary plaintext DataStore.
- New structured persisted models require backward-compatible parsing or schema migration.
- Failed/corrupt optional feature state must fall back safely rather than prevent app startup/playback.
- Importing a fork's persisted key names is not automatic; audit namespace collision first.
- Upstream sync conflicts must preserve official migrations already shipped.
- Experimental settings should survive disable/enable safely but must not force experimental runtime behavior when OFF.

## Gate PR requirements
If persistence changes:
- document old/new schema or key;
- add migration/default tests;
- test fresh install behavior;
- test upgrade from the previous integration state where practical;
- document downgrade limitation if one exists.

## Release
G14 validates fresh install + upgrade paths before Stable claims.
