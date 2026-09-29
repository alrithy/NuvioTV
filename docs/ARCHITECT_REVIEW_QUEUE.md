# Architect Review Queue

This is an **advisory review queue**, not a second roadmap and not an automatic implementation list.

Purpose:
- preserve architecture/code-review suggestions discovered outside the active coding session;
- make each suggestion reviewable by the current developer/agent;
- allow ACCEPTED / REJECTED / DEFERRED / IMPLEMENTED / OBSOLETE outcomes with evidence;
- preserve the response in GitHub so later agents do not need chat history.

## Workflow

Before finalizing a PR, review **only** queue items whose **Applies to** matches the current gate, PR, or touched subsystem.

Do **not** implement a suggestion automatically.

For each relevant item, record:
- **Decision**
- **Rationale**
- **Evidence**
- **Resolution / next action**

A rejected suggestion is valid when evidence supports it. The goal is review quality, not agreement.

---

## AR-001 — Do not log raw playback error text in failover paths
- **Status:** IMPLEMENTED
- **Severity:** HIGH
- **Applies to:** G4 / player recovery logging
- **Concern:** raw `detailedError` / exception text can contain request URLs or sensitive source details.
- **Decision:** ACCEPTED and implemented.
- **Rationale:** init/error paths could pass raw exception messages; safety was not provable.
- **Evidence:** PR #25; failover logs now use error code name, HTTP status, exception class and host only. G4d read-ahead uses exception class rather than throwable text.
- **Resolution / next action:** complete.

---

## AR-002 — Use a monotonic clock for startup timeout measurement
- **Status:** IMPLEMENTED
- **Severity:** MEDIUM
- **Applies to:** G4 / startup watchdog
- **Decision:** ACCEPTED and implemented.
- **Rationale:** wall-clock jumps must not affect elapsed timeout logic.
- **Evidence:** PR #25; watchdog uses `SystemClock.elapsedRealtime()`.
- **Resolution / next action:** complete.

---

## AR-003 — Re-evaluate REMUX_PERFORMANCE default before hardware validation
- **Status:** ACCEPTED_AS_IS
- **Severity:** HIGH
- **Applies to:** G4 / D044 / TCL C6K validation
- **Decision:** keep `REMUX_PERFORMANCE = AUTO`; recorded as D046.
- **Rationale:** default-on recovery paths engage only after official behavior already fails or hangs. Transfer changes remain behind the playback strategy selection whose default remains Official.
- **Evidence:** PR #25 / D046; failover follows official same-URL retries, watchdog retracts on a late frame, MKV resync runs only on data official would fail and preserves official truncated-tail handling.
- **Resolution / next action:** keep AUTO unless TCL C6K A/B demonstrates a regression; then disable the affected path.

---

## AR-004 — Feature 3 traceability was ahead of implementation
- **Status:** IMPLEMENTED
- **Severity:** MEDIUM
- **Applies to:** G3/G4 traceability
- **Feature:** 3 — Automatic Playback Strategy selection by device/file/network
- **Decision:** ACCEPTED and corrected.
- **Rationale:** measured network input does not exist yet.
- **Evidence:** PR #25; feature 3 returned to `in_progress`.
- **Resolution / next action:** close only after the measured network signal is implemented/validated.

---

## AR-005 — Feature 77 measured drift, not full sink-level jitter
- **Status:** DEFERRED
- **Severity:** MEDIUM
- **Applies to:** G1 diagnostics / G5 audio
- **Feature:** 77 — Audio clock jitter
- **Decision:** ACCEPTED; defer to G5.
- **Rationale:** current HUD shows clock drift, not sink-level jitter.
- **Evidence:** PR #25; traceability changed to deferred.
- **Resolution / next action:** G5 should either measure sink jitter from AudioTrack timestamps or explicitly redefine the product metric.

---

## AR-006 — Re-check official upstream before touching changed player seams
- **Status:** REVIEWED
- **Severity:** MEDIUM
- **Applies to:** G4+ player/controller changes
- **Decision:** no G4 sync required.
- **Rationale:** official changes since accepted baseline touched `PlayerRuntimeControllerPlaybackEvents`, `AudioSelectionOverlay`, `PlayerScreen`, and `PostPlay*`; G4 did not edit those owners. `strings.xml` overlap was additive only.
- **Evidence:** Claude review recorded on old PR #23; G4 slices #21/#22/#24/#25/#26 merged without touching the conflicting seams.
- **Resolution / next action:** re-run overlap review at the next upstream sync or before editing one of those seams.

---

## AR-007 — Review G4b prewarm cost and stream-type ambiguity
- **Status:** IMPLEMENTED
- **Severity:** MEDIUM
- **Applies to:** G4b / PR #22
- **Decision:** ACCEPTED with a low-cost guard.
- **Rationale:** prewarm is bounded, deduplicated, strategy-gated and fire-and-forget, but press-time MIME ambiguity could waste one warm-up on recognizable HLS/DASH.
- **Evidence:** PR #25; press-time decision now uses `PlayerMediaSourceFactory.inferMimeType(url, filename)` so recognizable HLS/DASH is never warmed. Existing tests cover REMUX-only activation and same-URL dedup. Official `PrefetchWindowStore` bounds storage.
- **Resolution / next action:** keep response-Content-Type-only ambiguity as MANUAL-PENDING/A-B risk; no probing layer added.

---

## AR-008 — Narrow/prove the IllegalStateException resync catch
- **Status:** IMPLEMENTED
- **Severity:** HIGH
- **Applies to:** G4c / PR #24
- **Concern:** a type-wide `IllegalStateException` catch could hide an internal invariant bug by treating it as malformed media.
- **Decision:** ACCEPTED and narrowed.
- **Rationale:** current official VarintReader/block parsing reports malformed-input cases as `ParserException`; the broader ISE catch existed for older ysosrs behavior. Remaining ISEs represent internal consistency failures and should surface.
- **Evidence:** PR #24 commit `f5f7d96`; resync now catches `ParserException` only. Local harness 19/19; exact-head CI passed 1859 tests, 15 known failures, 0 new, APK built; PR #24 merged as `e052568`.
- **Resolution / next action:** complete; on-device damaged-MKV validation remains MANUAL-PENDING.

---

## Review history

| Date | Gate / PR | Reviewed IDs | Result |
|---|---|---|---|
| 2026-09-29 | G4 initial architecture review | AR-001..AR-007 | Reviewed; implemented/deferred/accepted-as-is per entries above |
| 2026-09-29 | G4c PR #24 | AR-008 | Implemented before merge |
