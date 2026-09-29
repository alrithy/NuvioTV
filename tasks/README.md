# Superfork Task Packets

This directory contains deterministic execution packets for every implementation gate.

The canonical active task is selected by:
1. `integration/state.yaml: active_gate`
2. `integration/task_queue.csv`

An agent must not pick a later packet because it looks easier.

## Packets
- G0: Fork Foundation
- G1: Unified Diagnostics & Add-on Health
- G2: Adaptive Resource Manager
- G3: Playback Strategy Framework
- G4: REMUX / Network Performance
- G5: Audio / DV / HDR / AFR
- G6: Subtitle Intelligence
- G7: Seek Intelligence
- G8: Stream Intelligence
- G9: Skip / Recommendations / Discovery
- G10: Live TV
- G11: Watch Party
- G12: UI Styles & Screensaver
- G13: Experimental AI & MAT
- G14: Hardening / Distribution / Upstream

Task packets refine, but do not override:
AGENTS.md -> DECISIONS -> GATE_SPECS/ROADMAP -> MASTER_FEATURES/traceability.

If a packet conflicts with a higher-authority source, stop and repair the documentation inconsistency before coding.
