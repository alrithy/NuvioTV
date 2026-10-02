#!/usr/bin/env bash
# Runs the on-device smoke tests on the emulator the workflow started and keeps the crash log.
# Device findings reproduced here (G14): Live TV opening. Logs carry no secrets (no backend config).
set -uo pipefail
# A just-booted emulator can still be settling (lock screen, system dialogs) when the tests start;
# a test activity behind them can lose its window. Wait for boot, unlock, close system dialogs.
adb wait-for-device
until [ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; do sleep 2; done
adb shell input keyevent 82 || true
adb shell wm dismiss-keyguard || true
adb shell am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS >/dev/null 2>&1 || true
sleep 10
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
if [ "$status" -ne 0 ]; then
  # What the system did to the test activity (launch, pause, ANR dialogs), app package only.
  echo "::group::Activity and ANR lines"
  grep -E "ActivityTaskManager|ActivityManager: (ANR|Killing|Process)|am_anr|isn't responding|TestRunner|ComponentActivity" \
    build/device-smoke/logcat.txt | tail -80 || true
  echo "::endgroup::"
fi
exit $status
