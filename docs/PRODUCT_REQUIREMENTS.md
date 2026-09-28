# Product Requirements & Priority Stack

This document resolves trade-offs when multiple technically valid choices exist.

## North Star
Build one stable, maintainable Android TV Nuvio distribution that combines the strongest proven fork capabilities without sacrificing official-upstream compatibility.

## Priority order

### P0 — Never compromise
1. Playback stability / crash resistance.
2. User security, credential safety and signed-URL/header privacy.
3. Ability to continue syncing official Nuvio upstream.
4. Data/settings compatibility and safe fallback.
5. Honest test state: no invented PASS results.

### P1 — Core product experience
1. High-bitrate 4K playback including large REMUX files.
2. Lossless audio / HDR / Dolby Vision correctness when hardware supports it.
3. Reliable Arabic and language-independent subtitle behavior.
4. Remote-first Android TV navigation and focus.
5. Stream selection that prefers quality without choosing sources the connection cannot sustain.
6. Adaptive behavior on weaker devices rather than a permanently stripped product.

### P2 — Major capability expansion
- Stream intelligence / progressive AIOStreams.
- Live TV.
- Skip/discovery/recommendations.
- Diagnostics / add-on health.
- Calendar and Random/Mystery features.

### P3 — Experience and social
- Glass/Pill/top-nav visual options.
- Watch Party.
- richer post-play and discovery experiences.

### P4 — Experimental
- AI media providers.
- MAT/IEC61937 experimental audio path.
Experimental features remain OFF by default until explicitly promoted.

## Default stream preferences
When user settings do not override:
- Real-Debrid cached before uncached.
- Keep uncached visible.
- Quality-first mode: 4K before 1080p.
- Prefer REMUX, with WEB-DL fallback.
- Prefer DV/HDR when the output chain supports it.
- Prefer TrueHD Atmos/lossless audio when supported.
- Connection-fit may demote a theoretically better file if it is unlikely to play reliably.
- Preserve addon order when there is no strong evidence to override it.

## UX principles
- Simple defaults for normal users.
- Advanced controls for power users.
- New complex engines should default to Auto when safe.
- Never force one visual layout by deleting existing official layouts.
- TV remote behavior is part of correctness, not polish.

## Trade-off rule
If two requirements conflict, choose the higher priority above unless a durable exception is recorded in docs/DECISIONS.md.
