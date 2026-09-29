# Active Task Packet — G0 Fork Foundation

Current status comes only from integration/state.yaml. Preflight must verify latest integration ancestry and that no conflicting writer/work is present before coding.

## Identity
- Gate: G0 — Fork Foundation
- Branch: `chore/fork-foundation-impl`
- Base: post-governance, final pre-G0 official-sync `superfork/integration` HEAD
- User-visible behavior changes allowed: NO
- External fork feature code allowed: NO
- Runtime feature activation allowed: NO

## Objective
Create the smallest fork-owned foundation that future gates can depend on without modifying current Nuvio behavior.

## Required code location
`app/src/main/java/com/nuvio/tv/fork/foundation/`

Prefer these files unless current Kotlin conventions strongly justify a small adjustment:
- `FeatureMode.kt`
- `FeatureId.kt`
- `FeatureRegistry.kt`
- `SourceAttribution.kt`

Tests:
- `app/src/test/java/com/nuvio/tv/fork/foundation/...`

## Required behavior

### FeatureMode
Exactly these semantic modes:
- `OFF`: Superfork-specific behavior is disabled.
- `ON`: explicitly enabled.
- `AUTO`: runtime policy/compatibility logic may decide.

Do not invent extra modes in G0.

### FeatureId
Use module-level feature IDs, not 320 individual product-feature IDs.

Initial registry should cover future architectural owners:
- UNIFIED_DIAGNOSTICS
- ADAPTIVE_RESOURCE_MANAGER
- PLAYBACK_STRATEGY_ENGINE
- REMUX_PERFORMANCE
- AUDIO_DV_AFR
- SUBTITLE_INTELLIGENCE
- SEEK_INTELLIGENCE
- STREAM_INTELLIGENCE
- DISCOVERY_SKIP_RECOMMENDATIONS
- LIVE_TV
- WATCH_PARTY
- UI_STYLES
- AI_MEDIA
- MAT_AUDIO

Naming may be adjusted only for a clear Kotlin/project convention reason; record the change in HANDOFF.

### FeatureRegistry
G0 invariant: **no Superfork runtime feature becomes active merely because the registry exists**.

Therefore initial defaults must preserve official behavior:
- imported/new runtime feature groups default `OFF`;
- experimental AI/MAT default `OFF`;
- a later gate may deliberately introduce safe `AUTO` defaults with tests and a decision.

Registry requirements:
- deterministic defaults;
- query by FeatureId;
- no Android Context requirement if unnecessary;
- no network/I/O;
- no side effects at construction;
- simple testability.

Do not build a full settings UI or persistence layer in G0.

### ForkSettingsDataStore
Do NOT create this just because an older plan mentioned it.

Only create persistence if G0 actually needs it. Current expected implementation does not. Future gates can introduce fork-specific persisted settings under DATA_MIGRATION_POLICY.

### SourceAttribution
Provide a small model/utility capable of describing imported source provenance later:
- repository
- branch/ref
- pinned SHA
- optional source commit(s)
- import mode / feature identifier where appropriate

G0 should not hardcode every future import into application runtime memory unless it provides real value. The canonical detailed provenance remains docs/IMPORT_LEDGER.md.

## Tests — mandatory
At minimum prove:
1. every FeatureId has a deterministic default;
2. every initial Superfork runtime feature default preserves official behavior (OFF);
3. AI_MEDIA and MAT_AUDIO are OFF;
4. lookup has no missing FeatureId;
5. registry data cannot be accidentally mutated globally if the design intends immutable defaults.

Add tests appropriate to the actual API.

## Traceability
Before implementation:
- set G0 rows being actively implemented to `in_progress` where appropriate.

At completion:
- Feature 1: likely `verified_official` or project-level satisfied with evidence.
- Feature 2: project governance/upstream-sync capability; only mark implemented if evidence is present.
- Feature 311: CI; mark implemented only when Superfork CI is actually merged/working.
- 313–315,317–319: assign honest statuses based on implemented governance/foundation.
Do not mechanically mark every G0 row implemented if its acceptance evidence is missing.

## Required commands
```bash
python3 scripts/superfork/validate_project_state.py

python3 scripts/superfork/run_full_unit_suite.py

./gradlew :app:assembleFullDebug --stacktrace
git diff --check
git status --short
```

## Definition of Done
In addition to docs/DEFINITION_OF_DONE.md:
- foundation unit tests pass;
- governance validator passes;
- no new full-suite test failure beyond recorded baseline debt;
- fullDebug APK assembles;
- no UI/settings/player/network behavior changed;
- no external fork implementation imported;
- traceability/state/PROJECT_STATUS/HANDOFF updated;
- branch is clean after final commit;
- PR targets `superfork/integration`.

## Explicitly out of scope
Do NOT implement:
- diagnostics metrics;
- resource tiers;
- playback strategies;
- ysosrs network code;
- subtitle AutoSync;
- seek previews;
- stream ranking;
- skip/calendar/recommendations;
- Live TV;
- Watch Party;
- UI styles;
- AI/MAT runtime paths.

Those belong to later gates.

## Handoff to G1
After G0 merges:
- active gate becomes G1;
- recommended branch: `feat/unified-diagnostics`;
- create it from the resulting integration HEAD;
- G1 reads official diagnostics first and imports only missing ysosrs delta.