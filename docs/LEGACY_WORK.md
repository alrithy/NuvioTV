# Legacy / Pre-Superfork Work

These branches/PRs predate the current Superfork architecture and are NOT integration sources of truth.

## Open historical PRs
- PR #1 — Cinema View / immersive home layout.
- PR #2 — ten isolated TV design prototypes / Prototype Hub.

They may contain useful UI ideas or code, but they target legacy `dev` and were created before the current official baseline, gate model, test policy and porting protocol.

## Rule
Do not merge them directly into `superfork/integration`.

If a G12 task wants to reuse anything from them:
1. treat the old PR/branch as an additional candidate source;
2. diff against current official and the selected NuvioGlass/Reshaped/Cxsmo UI sources;
3. assign the relevant MASTER_FEATURES IDs;
4. port only selected behavior through PORTING_PROTOCOL;
5. run current CI/TV focus checks;
6. record provenance in IMPORT_LEDGER.

Historical work can be harvested; it cannot bypass current governance.
