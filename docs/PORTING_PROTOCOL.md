# Fork Feature Porting Protocol

Follow this for every feature sourced from another Nuvio fork.

## 1. Identify
- feature ID(s) from MASTER_FEATURES;
- gate from feature_traceability;
- canonical source and pinned SHA from SOURCE_MAP;
- known commits/caveats from FORK_RESEARCH.

## 2. Prove the source behavior
Inspect the pinned source implementation, related tests, settings, resources and commits. Do not port a README claim without code evidence.

## 3. Compare current official
Classify each source behavior:
- ALREADY_OFFICIAL: current official already provides equivalent behavior;
- PARTIAL_OVERLAP: official owns the subsystem but source has a useful missing delta;
- ISOLATED_FEATURE: source module is mostly independent;
- ARCHITECTURE_MISMATCH: useful algorithm exists but old wiring cannot safely move;
- OBSOLETE/REVERTED: do not port without a new explicit decision.

## 4. Choose import mode
- REUSE: use official implementation as-is.
- CHERRY_PICK: only for coherent commits that apply cleanly without dragging unrelated/outdated changes.
- FILE_PORT: copy isolated source files and adapt package/API boundaries.
- DELTA_PORT: manually transfer only the missing diff into current official owner.
- ALGORITHM_PORT: preserve tested algorithm/behavior while rewriting integration boundaries.
- ADAPTER: keep source behavior behind a current official interface/bridge.
- REWRITE: last resort only; document why all earlier modes failed.

## 5. Scope before coding
Record in IMPORT_LEDGER:
- source repo/branch/SHA/commits/files;
- official equivalent;
- already-upstream behavior;
- exact delta to import;
- explicitly excluded behavior;
- license/security notes;
- feature flag/fallback plan;
- tests to port.

Move affected traceability rows to `in_progress`.

## 6. Port behavior and tests together
Do not import code first and “add tests later.” Adapt relevant source tests or create equivalent coverage in the same task.

## 7. Conflict rules
When old fork code conflicts with current official:
1. preserve current official public contracts where possible;
2. keep official owner;
3. port the smallest missing behavior;
4. never resolve by copying an entire old file over a newer official file without a reviewed diff;
5. record non-trivial conflict decisions.

## 8. Validate
Use gate-specific tests plus:
- governance validator;
- fullDebug unit tests;
- fullDebug assemble;
- relevant TEST_MATRIX scenarios;
- same-file A/B for playback-performance claims.

Hardware/manual work is PASS / FAIL / MANUAL-PENDING only.

## 9. Complete provenance
Before PR completion:
- update IMPORT_LEDGER;
- update traceability status;
- update state/PROJECT_STATUS/HANDOFF;
- preserve license notices;
- record resulting local commit.

## 10. Never do these
- whole-fork merge;
- silent source-pin advance;
- duplicate an official subsystem;
- import a reverted experiment as stable;
- hide a failed/manual-pending test;
- remove official features merely because a source fork removed them.
