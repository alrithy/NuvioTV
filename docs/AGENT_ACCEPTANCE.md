# Fresh-agent acceptance record

Start only at AGENTS.md; do not use chat history. The following routes were exercised
against the proposed canonical system; after PR merge repeat live preflight on the refreshed
G0 branch. Exact final GitHub heads/CI are read live rather than hardcoded as self-references.

| Question | Deterministic answer / evidence |
|---|---|
| What project / where now? | AGENTS -> state -> generated PROJECT_STATUS/HANDOFF; G0 unimplemented |
| Branch / gate / work? | state + task_queue -> G0 packet; FeatureId/Mode/Registry/Attribution only |
| Source / upstream overlap? | SOURCE_MAP aliases/pins -> FORK_RESEARCH -> COMPONENT_MAP -> PORTING_PROTOCOL |
| Forbidden actions? | AGENTS + DECISIONS + NON_GOALS; no feature imports in this governance task/G0 |
| Tests / Done? | suite runner + TEST_STRATEGY/MATRIX + GATE_SPECS/DoD; no invented hardware PASS |
| Handoff / agent change? | AGENT_PLAYBOOK, state view generator, HANDOFF and exact lease release |
| Old state / stale branch / dirty tree? | preflight checks fetched integration; real local Git tests prove rejection without discarding work |
| Writer contention? | two sibling lease commits race; second push rejected; wrong-SHA release rejected |
| New regression / missing test? | 31 governance/Git tests cover guard fail-closed cases; complete 1,611-case inventory retained |
| All scope traceable? | validator checks exactly 1–320, gate/source/owner/status/evidence rules |
| Conflicting canonical documents? | GOVERNANCE_OWNERS; redirects validated; PR #5 useful content retained |
| Upstream drift? | accepted pin and observed HEAD differ explicitly; dedicated sync required |
| Admin limit? | 403 recorded in GITHUB_ADMIN_CHECKLIST; dev entrypoint redirects verified |

No runtime feature or G0 code is introduced. A green baseline-aware guard means no new
failures, not zero debt. Final PR must pass its exact-head CI before merge.
