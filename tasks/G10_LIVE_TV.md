# Task Packet — G10 Live TV
Feature IDs: 208–236
Branch: `feat/live-tv`
Depends on: G9
Primary source: Reshaped pinned SHA.

## Scope
M3U, Xtream, Stalker, multiple sources, QR source setup, EPG, Now/Next/progress/time remaining, favorites/categories/reorder/hide/search/logos, preview, zapping/CH keys, in-player list/categories, retries, MPEG-TS startup and live-aware AFR.

## Architecture
Live TV is isolated behind current navigation/player bridges. Do not contaminate VOD behavior with live-only retry/AFR rules.

## Resource policy
Preview resolution/concurrency obey AdaptiveResourceManager.

## Tests
Parser/source isolation, EPG present/absent, categories, dead-channel retry, remote focus/zapping. Real-provider/device cases may be MANUAL-PENDING.
