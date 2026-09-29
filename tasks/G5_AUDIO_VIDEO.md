# Task Packet — G5 Audio / Dolby Vision / HDR / AFR
Feature IDs: 32–44, 46–60
Branch: `feat/audio-video`
Depends on: G4 implementation merged. G4 device validation is intentionally batched to G14 and does not block G5.
Sources: current official first; ysosrs/Reshaped deltas. MAT ID 45 belongs to G13.

## Objective
Port only missing audio/video output-correctness behavior.

## Scope
Per-format passthrough; lossless track preference; sink/output diagnostics; downmix/software fallback; cold-start behavior; volume boost/soft clipping; DV profile/enhancement handling; HDR fallback/letterboxing; precise AFR; eARC settle/resume; source vs output resolution.

## Rules
Current official libdovi/DV owns the subsystem. Never overwrite with old fork files. Unsupported hardware must fall back safely.

## Tests
Pure policy/selection tests in CI. DV, TrueHD/DTS-HD, Atmos/DTS:X and AFR/eARC require device evidence: PASS/FAIL/MANUAL-PENDING.

## Manual-validation policy
Do not stop G5 development for device-only DV/passthrough/AFR/eARC checks. Mark them MANUAL-PENDING with exact expected evidence and carry them into the consolidated G14 hardware-certification campaign. Automated/pure-policy regressions still block immediately.
