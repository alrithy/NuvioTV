# Risk Register

This register contains architectural/project risks that must remain visible across agents.

## R-A01 — Upstream convergence/conflict
Severity: High
Risk: official Nuvio changes the same subsystem while a fork delta is being ported.
Mitigation: pinned baseline, dedicated upstream sync PRs, current-official diff before every import, one owner per concern.

## R-A02 — Feature interaction regressions
Severity: High
Risk: individually proven fork features interact badly after integration.
Mitigation: gate order, feature isolation, flags/fallbacks, integration regression matrix, A/B for playback paths.

## R-A03 — Resource bloat
Severity: High
Risk: seek previews, Live TV previews, poster caches, parallel downloads and AI collectively exceed weak-device RAM/CPU.
Mitigation: Adaptive Resource Manager, bounded caches/queues, low-RAM tests, no unbounded background work.

## R-A04 — Playback strategy conflict
Severity: High
Risk: Reshaped seek-buffer and ysosrs throughput strategies are stacked and fight each other.
Mitigation: mutually aware PlaybackStrategy layer; strategies remain distinct; Auto selects one.

## R-A05 — Subtitle engine duplication
Severity: High
Risk: Reshaped AutoSync and VibeSubtitle run as independent competing systems.
Mitigation: one Subtitle Intelligence pipeline and one timing owner.

## R-A06 — Credential/signed URL leakage
Severity: Critical
Risk: subtitle, Watch Party, diagnostics or providers log/forward sensitive headers/URLs.
Mitigation: SECURITY_POLICY, host-bound header forwarding, redaction, consent, no persistence, code review.

## R-A07 — Source drift
Severity: Medium
Risk: agent fetches latest fork branch instead of audited code and behavior changes mid-port.
Mitigation: SOURCE_MAP pins; pin changes require explicit decision.

## R-A08 — Baseline test debt masks regression
Severity: High
Risk: known failing tests create normalization of failure.
Mitigation: full suite always runs; exact allowlist; any new failure blocks CI; debt entries pruned when fixed.

## R-A09 — Agent handoff loss
Severity: High
Risk: Agent B repeats/overwrites Agent A or works wrong gate.
Mitigation: state.yaml + PROJECT_STATUS + HANDOFF + traceability + single-writer branch + PR guards.

## R-A10 — Default branch confusion
Severity: High until admin migration
Risk: agent starts on legacy dev.
Mitigation: redirect AGENTS/CLAUDE/SUPERFORK files on dev; admin checklist to change default branch.

## R-A11 — Old prototype/feature branches bypass architecture
Severity: Medium
Risk: old PR #1/#2 gets merged directly into product.
Mitigation: treat as reference only; harvest through G12 after current-official diff and normal porting protocol.

## R-A12 — Persistence breakage
Severity: High
Risk: imported settings collide or upgrade breaks profiles.
Mitigation: DATA_MIGRATION_POLICY, explicit defaults/migrations, fresh-install + upgrade checks.

## R-A13 — Dependency/supply-chain expansion
Severity: High
Risk: fork code adds unreviewed binaries/SDKs/permissions.
Mitigation: DEPENDENCY_POLICY, source-level imports, license/security review.

## R-A14 — Manual test fiction
Severity: High
Risk: agent claims DV/Atmos/remote/hardware behavior passed without device evidence.
Mitigation: PASS/FAIL/MANUAL-PENDING vocabulary enforced in docs and review.

## R-A15 — Release before hardening
Severity: High
Risk: many features exist but upgrade/security/license/test matrix is incomplete.
Mitigation: G14 and RELEASE_CHECKLIST; Stable requires closure.

## Risk acceptance
No agent may silently downgrade a High/Critical risk. Durable acceptance/exceptions go in docs/DECISIONS.md.
