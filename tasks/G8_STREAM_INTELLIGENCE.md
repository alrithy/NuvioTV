# Task Packet — G8 Stream Intelligence
Feature IDs: 147–168
Branch: `feat/stream-intelligence`
Depends on: G7
Sources: official + Reshaped + Cxsmo.

## Objective
One ranking engine powers list ordering and autoplay.

## Pipeline
progressive collection -> dedup -> cache -> quality/release/source analysis -> connection fit -> ranking -> autoplay.

## Defaults
RD cached first; uncached visible; quality-first 4K>1080p; REMUX preferred with WEB-DL fallback; DV/HDR/lossless when supported; connection-fit can demote unsustainable files; preserve addon order absent strong reason.

## Progressive AIOStreams
Capability-detect compatible progressive endpoint/NDJSON and fall back cleanly to ordinary path.

## Tests
Progressive snapshots/fallback, dedup, deterministic ranking, connection-fit, missing metadata, autoplay/list parity.
