# Nuvio Superfork Handoff

## Current state
- Repository: alrithy/NuvioTV
- Integration branch: superfork/integration
- Current task branch: chore/fork-foundation
- Official baseline: NuvioMedia/NuvioTV dev @ c257a2365ee3386b582dc2974ec235cfe0381f33
- Current roadmap gate: Gate 0 — Fork Foundation
- Status: READY TO IMPLEMENT

## Repository knowledge is now self-contained
The repository contains the project context required for a coding agent to continue without the original chat:
- docs/MASTER_FEATURES.md — authoritative 1–320 feature inventory
- docs/DECISIONS.md — architecture/product decisions and exclusions
- docs/FORK_RESEARCH.md — per-fork implementation findings and important commits
- docs/SOURCE_MAP.md — pinned source repos/branches/SHAs
- docs/ARCHITECTURE.md — target architecture and integration seams
- docs/ROADMAP.md — gate order
- docs/TEST_MATRIX.md — regression matrix
- docs/IMPORT_LEDGER.md — source/attribution template
- integration/features.yaml — machine-readable feature groups
- AGENTS.md — agent-agnostic workflow
- CLAUDE.md — Claude Code entrypoint
- .github/pull_request_template.md — import/testing checklist

## Important project policy
Reuse proven fork implementations first. Never merge a whole fork. Diff current official upstream before importing. Preserve official fallback for core playback. Record all imports and attribution.

## Next action
Implement Gate 0 exactly as defined in AGENTS.md.

Do not import user-facing fork features yet.

## When Gate 0 is complete
Update this file with:
- completed gate
- current branch
- latest commit SHA
- feature IDs touched
- changed modules/files
- build/test results
- unresolved issues
- exact next recommended action

Recommended next branch after Gate 0:
feat/unified-diagnostics

Recommended next gate:
Gate 1 — Unified Diagnostics
