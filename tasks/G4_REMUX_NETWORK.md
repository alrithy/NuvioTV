# Task Packet — G4 REMUX / Network Performance
Feature IDs: 4–13, 17–26
Branch: `feat/remux-network`
Depends on: G3
Sources: current official first; ysosrs + Reshaped deltas.

## Objective
Improve large/high-bitrate REMUX reliability without duplicating official parallel-range/speed-test code.

## Mandatory audit
Diff official media source, ParallelRangeDataSource, StreamSpeedTester and container paths against pinned sources. Port only missing behavior.

## Candidate deltas
Adaptive connections/chunks, reduced re-download, 429/503/stall recovery, actual-source speed test, pre-resolve/warm startup, source failover, malformed/truncated MKV recovery, non-faststart MP4 seek, demux cache.

Disk seek buffer remains a distinct Seek Optimized strategy; never blindly stack with parallel throughput.

## Evidence
Same-file A/B: startup, throughput, buffer, rebuffer, wasted bytes when measurable, seek, RAM.

## Tests
Rate limit, cancellation, hung reads, ranges, failover, malformed fixtures, full-suite no-new-regressions.
