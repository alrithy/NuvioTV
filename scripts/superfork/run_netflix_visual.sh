#!/usr/bin/env bash
# CI runner: install the already-built APKs on one explicit TV emulator and collect evidence.
# All fixtures and configuration are offline; no backend or release credentials are used.
set -euo pipefail

serial="${NETFLIX_VISUAL_SERIAL:?}"
resolution="${NETFLIX_VISUAL_RESOLUTION:?}"
locale="${NETFLIX_VISUAL_LOCALE:?}"
package="${NETFLIX_VISUAL_PACKAGE:?}"
apk_dir="${NETFLIX_VISUAL_APK_DIR:?}"
output="${NETFLIX_VISUAL_OUTPUT:?}"
mkdir -p "$output"

collect_log() {
  local status=$?
  trap - EXIT
  adb -s "$serial" logcat -d -v time > "$output/logcat.txt" 2>&1 || true
  grep -E "NetflixSearchRestore|FocusOwner" "$output/logcat.txt" | tail -60 || true
  python3 scripts/superfork/print_visual_previews.py "$output" || true
  exit "$status"
}
trap collect_log EXIT

adb -s "$serial" wait-for-device
deadline=$((SECONDS + 180))
until [ "$(adb -s "$serial" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; do
  if [ "$SECONDS" -ge "$deadline" ]; then
    echo "TV emulator did not finish booting within 180 seconds." >&2
    exit 1
  fi
  sleep 2
done
adb -s "$serial" shell input keyevent 82 || true
adb -s "$serial" shell wm dismiss-keyguard || true
adb -s "$serial" shell am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS >/dev/null 2>&1 || true
sleep 10
adb -s "$serial" logcat -c

# upload-artifact preserves the common APK directory tree. Reject missing or
# ambiguous APKs instead of accidentally installing another flavor or ABI split.
mapfile -t app_apks < <(find "$apk_dir" -type f -name 'app-full-universal-debug.apk')
mapfile -t test_apks < <(find "$apk_dir" -type f -name 'app-full-debug-androidTest.apk')
if [ "${#app_apks[@]}" -ne 1 ] || [ "${#test_apks[@]}" -ne 1 ]; then
  echo "Expected exactly one full universal app APK and one full instrumentation APK." >&2
  exit 1
fi
adb -s "$serial" install -r -t "${app_apks[0]}"
adb -s "$serial" install -r -t "${test_apks[0]}"

python3 scripts/superfork/capture_netflix_theme.py \
  --serial "$serial" --package "$package" --resolution "$resolution" --locale "$locale" --output "$output"

# adb may exit zero after an instrumentation crash, so require a positive JUnit
# completion as well as the Android runner's successful final code.
adb -s "$serial" shell am instrument -w -r \
  -e class com.nuvio.tv.ui.theme.NuvioDialogButtonThemeTest \
  "$package.test/androidx.test.runner.AndroidJUnitRunner" \
  > "$output/dialog-instrumentation-result.txt" 2>&1
python3 - "$output/dialog-instrumentation-result.txt" <<'PY'
import re
import sys
from pathlib import Path

result = Path(sys.argv[1]).read_text(encoding="utf-8")
print(result)
completed = re.search(r"OK \(([1-9][0-9]*) tests?\)", result)
success_code = re.search(r"^INSTRUMENTATION_CODE: -1\s*$", result, re.MULTILINE)
failed = any(marker in result for marker in (
    "FAILURES!!!", "INSTRUMENTATION_FAILED", "shortMsg=", "Process crashed",
))
if not completed or not success_code or failed:
    raise SystemExit("Dialog theme instrumentation did not complete successfully.")
PY
