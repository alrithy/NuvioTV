# Task Packet — G5 Audio / Dolby Vision / HDR / AFR
Feature IDs: 32–44, 46–60
Branch: `feat/audio-video`
Depends on: G4
Sources: current official first; ysosrs/Reshaped deltas.
MAT feature 45 belongs to G13.

## Objective
Port only missing audio/video output correctness features.

## Scope
Per-format passthrough; lossless track preference; sink capability/output diagnostics; downmix/software fallback; cold-start behavior; volume boost/soft clipping; DV profile/enhancement handling; HDR fallback/letterboxing; precise AFR; eARC settle/resume; source vs output resolution.

## Rules
Current official libdovi/DV code owns the subsystem. Never overwrite with old fork files. Hardware capability fallback must be safe.

## Tests
Pure policy/selection tests in CI. Hardware-dependent DV, TrueHD/DTS-HD, Atmos/DTS:X, AFR/eARC are PASS/FAIL/MANUAL-PENDING with device evidence.
