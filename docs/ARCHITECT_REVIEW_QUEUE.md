# Architect Review Queue

This is an **advisory review queue**, not a second roadmap and not an automatic implementation list.

Purpose:
- preserve architecture/code-review suggestions discovered outside the active coding session;
- make every suggestion reviewable by the current developer/agent;
- let the developer accept, reject, defer, or supersede a suggestion with evidence;
- preserve the response in GitHub so a later reviewer can see what happened without chat history.

## Workflow

Before finalizing a PR, review only queue items whose **Applies to** matches the current gate, PR, or touched subsystem.

Do **not** implement a suggestion automatically.

For each relevant item, set one of:
- `REVIEW_REQUIRED` — not yet evaluated;
- `ACCEPTED` — technically appropriate; implementation planned;
- `REJECTED` — not appropriate; rationale/evidence required;
- `DEFERRED` — valid but belongs to a later gate/task; name it;
- `IMPLEMENTED` — accepted and completed; link commit/PR/test evidence;
- `OBSOLETE` — no longer applicable because later code/upstream superseded it.

Developer response must include:
- **Decision**
- **Rationale**
- **Evidence**
- **Resolution / next action**

A rejected suggestion is completely acceptable when the evidence supports it. The goal is review quality, not agreement.

---

## AR-001 — Do not log raw playback error text in failover paths

- **Status:** REVIEW_REQUIRED
- **Severity:** HIGH
- **Applies to:** G4 / merged G4a follow-up / player recovery logging
- **Observed in:** `PlayerRuntimeControllerSourceFailover.kt`
- **Concern:** G4a intentionally avoids logging full source URLs, but failover log messages currently interpolate `detailedError`. Playback/network exception text can sometimes include request URLs or other source details. The PR security statement should be true by construction, not by assumption.
- **Suggested review:** Determine whether any producer of `detailedError` can contain a signed URL, query token, header value, or other sensitive request data. If yes or not provably impossible, replace raw text with structured safe fields such as error code, HTTP status, exception class, and host.
- **Do not over-engineer:** no new logging framework is requested.

### Developer response
- Decision:
- Rationale:
- Evidence:
- Resolution / next action:

---

## AR-002 — Use a monotonic clock for startup timeout measurement

- **Status:** REVIEW_REQUIRED
- **Severity:** MEDIUM
- **Applies to:** G4 / merged G4a follow-up / startup watchdog
- **Observed in:** `PlayerRuntimeControllerSourceFailover.scheduleStartupWatchdog`
- **Concern:** timeout elapsed time is measured with `System.currentTimeMillis()`, which is wall-clock time and can jump when device time changes.
- **Suggested review:** Prefer `SystemClock.elapsedRealtime()` or another monotonic source for elapsed timeout calculations.
- **Do not over-engineer:** a direct clock substitution is enough if compatible with tests.

### Developer response
- Decision:
- Rationale:
- Evidence:
- Resolution / next action:

---

## AR-003 — Re-evaluate REMUX_PERFORMANCE default before hardware validation

- **Status:** REVIEW_REQUIRED
- **Severity:** HIGH
- **Applies to:** G4 / D044 / TCL C6K hardware validation
- **Observed in:** G4a sets `FeatureId.REMUX_PERFORMANCE` to `AUTO`.
- **Concern:** G4 recovery changes user-visible playback behavior, while G4's mandatory real-device A/B remains MANUAL-PENDING. Stability-first policy may favor keeping the new recovery/network group OFF by default until the TCL C6K validation passes.
- **Suggested review:** Decide explicitly between:
  1. keep AUTO because recovery behavior is independently safe and OFF remains available; or
  2. default OFF until G4 hardware validation passes, then promote to AUTO in a small reviewed decision.
- **Required:** whichever choice is made, document the reasoning in `docs/DECISIONS.md`. Do not change the default merely to satisfy this suggestion.

### Developer response
- Decision:
- Rationale:
- Evidence:
- Resolution / next action:

---

## AR-004 — Feature 3 traceability may be ahead of implementation

- **Status:** REVIEW_REQUIRED
- **Severity:** MEDIUM
- **Applies to:** G3/G4 traceability
- **Feature:** 3 — Automatic Playback Strategy selection by device/file/network
- **Concern:** current Auto strategy uses device tier, file size/name, and stream type; D043 explicitly says measured network signal is left to G4. The row is currently `implemented`.
- **Suggested review:** Either:
  - change the status to an honest partial/in-progress state until the network signal exists; or
  - document that Feature 3's accepted product definition does not require a measured network signal and update wording consistently.
- **Goal:** traceability status must describe reality, not roadmap intent.

### Developer response
- Decision:
- Rationale:
- Evidence:
- Resolution / next action:

---

## AR-005 — Feature 77 currently measures drift, not full sink-level jitter

- **Status:** REVIEW_REQUIRED
- **Severity:** MEDIUM
- **Applies to:** G1 diagnostics / G5 audio
- **Feature:** 77 — Audio clock jitter
- **Concern:** current HUD implementation measures position-vs-wall-clock drift at HUD cadence. Traceability evidence itself states sub-frame sink jitter is left to G5, yet the feature row is `implemented`.
- **Suggested review:** Mark it partial/deferred-to-G5, or explicitly redefine the product metric as drift if that is the intended observable.
- **Goal:** avoid claiming more precision than the platform currently exposes.

### Developer response
- Decision:
- Rationale:
- Evidence:
- Resolution / next action:

---

## AR-006 — Re-check official upstream before touching changed player seams

- **Status:** REVIEW_REQUIRED
- **Severity:** MEDIUM
- **Applies to:** G4+ player/controller changes
- **Observed upstream:** official `dev` advanced beyond accepted `71632b9`; latest observed during review was `8e728cae7c6d8f2a0a089d7a240675e25a33a883`.
- **Relevant changed file:** `PlayerRuntimeControllerPlaybackEvents.kt` is among official changes since the accepted baseline.
- **Suggested review:** Do not perform a blind sync. Before a slice edits an upstream-changed seam, compare current official vs accepted baseline and use the existing reviewed upstream-sync process only if the overlap is material.
- **No blocker:** slices that do not touch affected seams may proceed after documenting the check.

### Developer response
- Decision:
- Rationale:
- Evidence:
- Resolution / next action:

---

## AR-007 — Review G4b prewarm cost and stream-type ambiguity before merge

- **Status:** REVIEW_REQUIRED
- **Severity:** MEDIUM
- **Applies to:** PR #22 / G4b
- **Observed design:** press-time REMUX warm-up can request ~256 KiB head + 4 MiB suffix, with official prefetch-store TTL/cap; low-RAM skips the tail. The press-time decision cannot see resolved MIME, so a REMUX-class URL that resolves to HLS could waste the warm-up.
- **Suggested review:** Confirm with code/tests that:
  - the path only activates when REMUX / Throughput is the effective strategy;
  - repeated presses are deduplicated as intended;
  - stored windows remain bounded by the official store;
  - a failed/unsupported range request cannot interfere with playback;
  - the MIME ambiguity is an accepted MANUAL-PENDING/A-B risk or has a simple low-cost guard.
- **Do not over-engineer:** do not add probing layers solely to eliminate a theoretical one-off warm-up unless evidence justifies them.

### Developer response
- Decision:
- Rationale:
- Evidence:
- Resolution / next action:

---

## Review history

Keep short entries here when a review cycle finishes.

| Date | Gate / PR | Reviewed IDs | Result |
|---|---|---|---|
| 2026-09-29 | G4 / initial queue creation | AR-001..AR-007 | Awaiting developer review |
