# Risk Register

| ID | Risk | Severity | Mitigation / owner | Status |
|---|---|---:|---|---|
| R1 | Official upstream moves in same subsystems while we port forks | High | pinned baseline + UPSTREAM_SYNC + delta-port | Open/controlled |
| R2 | Official baseline has 18 failing full-suite unit tests | Medium | allowlisted baseline debt; any new failure blocks | Controlled |
| R3 | Reshaped seek buffer conflicts with ysosrs throughput path | High | distinct Playback Strategies, never blind-stack | Controlled |
| R4 | Resource-heavy features regress low-RAM TVs | High | G2 Adaptive Resource Manager + manual low-RAM checks | Planned |
| R5 | Android TV vendor differences affect DV/AFR/audio | High | capability checks + official fallback + hardware log | Open |
| R6 | Watch Party leaks signed URLs/headers | High | consent, redaction, no persistence, session cleanup | Planned G11 |
| R7 | AI providers leak keys or create hard dependency | High | BYOK/Keystore/provider isolation/OFF default | Planned G13 |
| R8 | Progressive AIOStreams endpoint unavailable | Medium | capability detect + standard fallback | Planned G8 |
| R9 | Imported code loses attribution/license obligations | High | IMPORT_LEDGER + release audit | Controlled |
| R10 | Two agents edit same branch or stale handoff | High | one-agent-per-branch + state/handoff protocol | Process-controlled |
| R11 | Resolved streams are IP/session locked | Medium | fail safely; do not promise cross-network sharing | Planned |
| R12 | GitHub default branch remains legacy dev | Medium | dev safety redirects + admin checklist | Manual action pending |
| R13 | Hardware-only tests are accidentally claimed as automated | High | MANUAL-PENDING semantics + DoD | Controlled |

Update this register when a new cross-gate risk is discovered.
