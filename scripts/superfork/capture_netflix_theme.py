#!/usr/bin/env python3
"""Run production Compose fixtures on a TV device and collect genuine screenshots.

This produces review evidence, not Netflix reference assets or an automatic visual
fidelity score. The device is selected explicitly so an unrelated attached device
is never reconfigured.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import struct
import subprocess
import time
from pathlib import Path


SCREENS = (
    "01-home-hero",
    "02-home-rows",
    "03-focused-card",
    "04-expanded-card",
    "05-movie-details",
    "06-series-details",
    "07-episodes",
    "08-search",
    "09-navigation",
    "10-profiles",
    "11-player-controls",
)
TEST_CLASS = "com.nuvio.tv.ui.theme.NetflixThemeTvTest"


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--serial", required=True, help="adb device serial (explicitly selected)")
    parser.add_argument("--adb", default="adb")
    parser.add_argument("--package", default="com.nuvio.tv")
    parser.add_argument("--resolution", choices=("1080p", "4k"), default="1080p")
    parser.add_argument("--locale", choices=("en", "ar"), default="en")
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--capture-only", action="store_true", help="collect the most recent test screenshots")
    args = parser.parse_args()
    args.output.mkdir(parents=True, exist_ok=True)

    def adb(*command: str, check: bool = True) -> subprocess.CompletedProcess[str]:
        return subprocess.run(
            [args.adb, "-s", args.serial, *command], text=True,
            stdout=subprocess.PIPE, stderr=subprocess.STDOUT, check=check,
        )

    test_result = None
    if not args.capture_only:
        # Both use the canonical 960 x 540 dp Android TV canvas.
        size, density = ("1920x1080", "320") if args.resolution == "1080p" else ("3840x2160", "640")
        adb("shell", "wm", "size", size)
        adb("shell", "wm", "density", density)
        remote = f"/sdcard/Android/data/{args.package}/files/netflix-theme"
        adb("shell", "rm", "-rf", remote)
        test_result = adb(
            "shell", "am", "instrument", "-w", "-r", "-e", "class", TEST_CLASS,
            "-e", "netflix_locale", args.locale,
            f"{args.package}.test/androidx.test.runner.AndroidJUnitRunner", check=False,
        )
        (args.output / "instrumentation-result.txt").write_text(test_result.stdout, encoding="utf-8")

    remote = f"/sdcard/Android/data/{args.package}/files/netflix-theme/."
    pull = adb("pull", remote, str(args.output), check=False)
    if pull.returncode:
        print(pull.stdout)
        return 1

    files = []
    for path in sorted(args.output.glob("*.png")):
        data = path.read_bytes()
        if data[:8] != b"\x89PNG\r\n\x1a\n":
            raise ValueError(f"Not a PNG: {path.name}")
        width, height = struct.unpack(">II", data[16:24])
        files.append({"file": path.name, "width": width, "height": height,
                      "sha256": hashlib.sha256(data).hexdigest()})

    sha = subprocess.run(["git", "rev-parse", "HEAD"], text=True, capture_output=True, check=True).stdout.strip()
    dirty = subprocess.run(["git", "status", "--porcelain"], text=True, capture_output=True, check=True).stdout.strip()
    missing = [screen for screen in SCREENS if not any(f["file"].startswith(screen) for f in files)]
    manifest = {
        "source_commit": sha, "source_tree_dirty": bool(dirty),
        "captured_at_utc": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()),
        "device_serial": args.serial, "requested_resolution": args.resolution,
        "fixture_locale": args.locale,
        "device_fingerprint": adb("shell", "getprop", "ro.build.fingerprint").stdout.strip(),
        "screenshots": files, "missing_required_screens": missing,
        "visual_review_status": "PENDING", "netflix_reference_comparison_status": "PENDING",
        "hardware_performance_status": "MANUAL-PENDING",
        "scope": "Actual production Compose components with offline deterministic fixtures; no backend or playback-engine certification.",
    }
    (args.output / "manifest.json").write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")
    print(f"Collected {len(files)} screenshots; missing required screens: {len(missing)}")
    if test_result and (test_result.returncode or "FAILURES!!!" in test_result.stdout or "INSTRUMENTATION_FAILED" in test_result.stdout):
        return 1
    return 1 if missing else 0


if __name__ == "__main__":
    raise SystemExit(main())
