# Codex Instructions — Nuvio Superfork

You are working on a maintainable integration fork of NuvioTV. Do not rewrite proven fork features by default. Reuse working implementations, adapt only their boundaries to current official Nuvio, and keep official behavior available as fallback.

## Read first
1. docs/BASELINE.md
2. docs/ARCHITECTURE.md
3. docs/SOURCE_MAP.md
4. docs/ROADMAP.md
5. docs/TEST_MATRIX.md
6. integration/features.yaml

Pinned official baseline:
NuvioMedia/NuvioTV:dev@c257a2365ee3386b582dc2974ec235cfe0381f33

## Rules
1. Never merge another fork wholesale.
2. Never replace an official subsystem with an older fork copy without a file-level diff.
3. Prefer CHERRY_PICK / FILE_PORT / ALGORITHM_PORT / DELTA_PORT / LOGIC_PORT before rewrite.
4. If upstream already contains equivalent functionality, keep upstream and port only the missing delta.
5. Preserve license notices and record repo + SHA/commits for every imported feature.
6. One owner per concern: one subtitle engine, one diagnostics model, one skip aggregator, one resource manager, one ranker.
7. Keep official playback as selectable/fallback behavior.
8. AI and MAT are experimental and OFF by default.
9. Gate 0 must not change user-visible playback/UI behavior.
10. Every feature PR must build/test before the next feature starts.

## Workflow for every imported feature
- Audit source repo/branch/SHA/commits and source files.
- Identify equivalent files in current official baseline.
- Record what already exists upstream.
- Import the smallest coherent unit.
- Adapt only boundaries needed for current APIs.
- Add flag/fallback for core behavior.
- Port/add tests.
- Update docs/IMPORT_LEDGER.md.
- Stop if the current gate is not green.

## First task — Gate 0 only
1. Verify HEAD matches the pinned baseline.
2. Create com.nuvio.tv.fork.foundation.
3. Add FeatureId and FeatureMode (OFF, ON, AUTO).
4. Add FeatureRegistry with defaults that preserve official behavior.
5. Add ForkSettingsDataStore only if required; do not migrate official settings.
6. Add SourceAttribution model/utility.
7. Add docs/IMPORT_LEDGER.md template.
8. Add unit tests for feature default semantics.
9. Run build and tests.
10. Commit as: chore(fork): establish integration foundation

Do not port user-facing fork features in Gate 0.

## After Gate 0
Gate 1 is unified diagnostics. Keep PlayerDebugStatsOverlay as UI owner and port only metrics missing from official upstream.
