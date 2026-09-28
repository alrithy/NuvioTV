# Task Packet — G4 REMUX / Network Performance
Feature IDs: 4–13, 17–26
Branch: `feat/remux-network`
Depends on: G3
Sources: current official first; ysosrs + Reshaped pinned deltas.

## Objective
Improve high-bitrate/large-REMUX reliability without duplicating current official parallel-range/speed-test code.

## Mandatory audit
Diff official `ParallelRangeDataSource`, media source, StreamSpeedTester and container paths against source forks. Import only missing behavior.

## Candidate deltas
Adaptive connections/chunks, reduced re-download, rate-limit/stall recovery, speed test on actual source, pre-resolve/warm startup, source failover, malformed/truncated MKV recovery, non-faststart MP4 seeking, demux-cache policy.

Disk seek buffer must remain a distinct Seek Optimized strategy; do not blindly stack it with throughput parallelism.

## Required evidence
Same-file A/B where feasible: startup, sustained throughput, buffer, rebuffer, wasted bytes where measurable, seek, RAM.

## Tests
429/503, cancellation, hung read, range behavior, failover, malformed fixtures, no-new-regressions full suite.
