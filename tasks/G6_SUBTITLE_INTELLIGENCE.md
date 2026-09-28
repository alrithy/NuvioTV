# Task Packet — G6 Subtitle Intelligence
Feature IDs: 82–106
Branch: `feat/subtitle-intelligence`
Depends on: G5
Sources: Reshaped + VibeSubtitle pinned SHAs, current official subtitle system.

## Objective
One subtitle sync engine; never run multiple independent AutoSync systems.

## Pipeline
1. embedded reference
2. hash/release matched reference
3. cue-rhythm alignment
4. optional audio/ASR fallback
5. original timing + manual offset

## Scope
Offset, clock scale, piecewise drift, confidence fail-closed, Arabic and language-independent support, secondary language, AutoSynced status/notifications, custom TTF/OTF via QR/URL/picker, safe fallback.

## Security
Do not forward stream Authorization/Cookie headers to unrelated subtitle hosts. No stream credentials enter sync services.

## Tests
Aligned/fixed-offset/gradual drift/piecewise drift/low-confidence/no-reference, Arabic target + embedded English reference, valid/invalid custom fonts, header-host boundary tests.
