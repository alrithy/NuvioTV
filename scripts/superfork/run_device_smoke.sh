#!/usr/bin/env bash
# Runs the on-device smoke tests on the emulator the workflow started and keeps the crash log.
# Device findings reproduced here (G14): Live TV opening. Logs carry no secrets (no backend config).
set -uo pipefail
adb logcat -c || true
status=0
./gradlew :app:connectedFullDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class="${SMOKE_TEST_CLASSES:?}" --stacktrace || status=$?
mkdir -p build/device-smoke
adb logcat -d -v time > build/device-smoke/logcat.txt || true
# The fatal exception, if any, so it shows in the job log and summary.
grep -n -A40 "FATAL EXCEPTION" build/device-smoke/logcat.txt | head -120 > build/device-smoke/fatal.txt || true
if [ -s build/device-smoke/fatal.txt ]; then
  echo "::group::Fatal exception on the device"; cat build/device-smoke/fatal.txt; echo "::endgroup::"
fi
exit $status
