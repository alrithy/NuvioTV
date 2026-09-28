# Task Packet — G8 Stream Intelligence
Feature IDs: 147–168
Branch: `feat/stream-intelligence`
Depends on: G7
Sources: official + Reshaped + Cxsmo.

## Objective
One ranking engine powers both list ordering and autoplay.

## Pipeline
progressive collection -> dedup -> cache state -> quality/release/source analysis -> connection fit -> ranking -> autoplay candidate.

## Defaults
RD cached first; uncached visible; quality-first 4K>1080p; REMUX preferred/WEB-DL fallback; DV/HDR/lossless when supported; connection-fit may demote unsustainable files; preserve addon order absent strong override.

## Progressive AIOStreams
Capability-detect compatible progressive endpoint/NDJSON. Cleanly fall back to ordinary endpoint/manifest.

## Tests
dedup, progressive snapshots, fallback, deterministic ranking, connection-fit, missing metadata, autoplay/list parity.
