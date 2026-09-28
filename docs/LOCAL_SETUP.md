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
./gradlew :app:testFullDebugUnitTest --stacktrace
./gradlew :app:assembleFullDebug --stacktrace
```

Hardware-dependent Dolby Vision, passthrough, AFR, high-bitrate REMUX, TV focus and Live TV checks are manual/device tests unless an automated fixture exists. Record PASS / FAIL / MANUAL-PENDING honestly.
