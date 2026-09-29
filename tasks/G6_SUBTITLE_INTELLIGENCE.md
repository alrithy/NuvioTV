# Task Packet — G6 Subtitle Intelligence
Feature IDs: 82–106
Branch: `feat/subtitle-intelligence`
Depends on: G5
Sources: Reshaped + VibeSubtitle pinned SHAs + current official.

## Objective
One subtitle sync engine. Never run Reshaped and Vibe as independent competing systems.

## Pipeline
embedded reference -> hash/release reference -> cue-rhythm alignment -> optional audio/ASR -> original timing/manual offset.

## Scope
Offset, clock scale, piecewise drift, confidence fail-closed, Arabic/language-independent support, secondary language, AutoSynced status, custom TTF/OTF via QR/URL/picker and safe fallback.

## Security
Never forward stream Authorization/Cookie headers to unrelated subtitle hosts or sync services.

## Tests
Aligned/fixed-offset/gradual-drift/piecewise-drift/low-confidence/no-reference; Arabic target + embedded English reference; valid/invalid fonts; header-host boundary.
