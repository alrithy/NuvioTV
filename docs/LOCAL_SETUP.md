# Local / Agent Setup

Required: Git, JDK 17, Android SDK compatible with the existing project, Python 3.

## Preflight
```bash
git status --short
git branch --show-current
git rev-parse HEAD
python3 scripts/superfork/validate_project_state.py
```

## Core checks
```bash
./gradlew :app:testFullDebugUnitTest --stacktrace || true
python3 scripts/superfork/check_baseline_test_failures.py app/build/test-results/testFullDebugUnitTest integration/baseline_test_failures.txt
./gradlew :app:assembleFullDebug --stacktrace
```

Hardware-dependent Dolby Vision, passthrough, AFR, high-bitrate REMUX, TV focus and Live TV checks are manual/device tests unless an automated fixture exists. Record PASS / FAIL / MANUAL-PENDING honestly.

The `|| true` is used only so the baseline comparison script can inspect JUnit XML. The comparison script is the authoritative pass/fail gate and fails on any unrecorded test failure.
