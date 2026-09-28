# Task Packet — G12 UI Styles & Screensaver
Feature IDs: 188–205, 292
Branch: `feat/ui-styles`
Depends on: G11
Sources: official + NuvioGlass + Reshaped + Cxsmo. Legacy PR #1/#2 are reference-only.

## Objective
Add selectable visual/navigation styles without deleting official layouts.

## Scope
Preserve Original/Classic/Grid/Modern; add Glass/Cinematic Glass, sidebar/top/glass/pill navigation, capability-aware liquid glass fallback, hero/art/trailer/clock/profile access and optional screensaver.

## Rules
D-pad/focus is correctness. Weak devices get lightweight effects. Never directly merge legacy Cinema/prototype PRs; harvest selected ideas via PORTING_PROTOCOL.

## Tests
Focus entry/exit/restore, Back behavior, RTL, weak-device fallback, layout switching and settings migration.
