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
import re
import struct
import subprocess
import time
from pathlib import Path


# The review matrix in docs/NETFLIX_THEME_VISUAL_VERIFICATION.md; each name is captured by the
# instrumentation test that drives that exact state (a PNG existing is not a visual pass).
SCREENS = (
    "01-home-hero",
    "02-home-rows",
    "03-focused-card",
    "04-expanded-card",
    "05-movie-details",
    "06-series-details",
    "07-episodes",
    "08-search",
    "09-top-nav-home",
    "10-top-nav-search",
    "11-top-nav-my-netflix",
    "12-my-netflix-hub",
    "13-profiles",
    "14-player-controls",
    "15-resume-actions",
    "16-confirmation-dialog",
    "17-empty-state",
    "18-network-error",
    "19-playback-loading",
    "20-contextual-callout",
    "21-low-memory-fallback",
    "22-missing-logo-fallback",
    "23-very-long-title",
    "24-arabic-mixed-bidi",
    "25-search-error-keyboard",
    "26-details-return-focus",
    # Visual Round 3: maintainer reference correction (audit §0). 32-top10-row is intentionally absent:
    # Nuvio has no factual ranking source, so no Top 10 row is rendered.
    "27-home-hero-full-reference-state",
    "28-home-category-shortcuts",
    "29-browse-row-portrait-idle",
    "30-browse-row-focused-landscape",
    "31-browse-row-focus-moved-next-item",
    "33-search-portrait-results",
    "34-arabic-browse-expanded",
    "35-arabic-search-keyboard-right",
)
# Review names that intentionally capture one scene (the maintainer asked for them by name); any other
# pair of identical required states still fails the run.
ALIASES = (
    {"01-home-hero", "27-home-hero-full-reference-state"},
    {"03-focused-card", "04-expanded-card", "30-browse-row-focused-landscape", "34-arabic-browse-expanded"},
    {"08-search", "33-search-portrait-results", "35-arabic-search-keyboard-right"},
)
TEST_CLASS = "com.nuvio.tv.ui.theme.NetflixThemeTvTest"


def instrumentation_succeeded(result: subprocess.CompletedProcess[str]) -> bool:
    """adb can exit zero after a runner crash; require the runner's success receipt."""
    output = result.stdout
    if result.returncode or any(marker in output for marker in (
        "FAILURES!!!", "INSTRUMENTATION_FAILED", "shortMsg=Process crashed",
        "INSTRUMENTATION_ABORTED", "INSTRUMENTATION_RESULT: shortMsg=",
    )):
        return False
    receipts = re.findall(r"^INSTRUMENTATION_CODE:\s*(-?\d+)\s*$", output, re.MULTILINE)
    return (receipts == ["-1"] and
            re.search(r"^OK \([1-9]\d* tests?\)\s*$", output, re.MULTILINE) is not None)


def print_failures(output: str) -> None:
    """Put each failing test and the top of its stack in the job log; artifacts are not always reachable."""
    # In `am instrument -r` output a test's STATUS lines precede its STATUS_CODE line (-2 = failure).
    for match in re.finditer(r"(.*?)^INSTRUMENTATION_STATUS_CODE: (-?\d+)\s*$", output, re.MULTILINE | re.DOTALL):
        block, code = match.group(1), match.group(2)
        if code not in ("-2", "-4"):
            continue
        test = re.search(r"^INSTRUMENTATION_STATUS: test=(.*)$", block, re.MULTILINE)
        stack = re.search(r"^INSTRUMENTATION_STATUS: stack=(.*?)(?=^INSTRUMENTATION_STATUS: |\Z)", block, re.MULTILINE | re.DOTALL)
        name = test.group(1).strip() if test else "?"
        lines = (stack.group(1) if stack else "").strip().splitlines()[:16]
        print(f"FAILED {name}")
        print("\n".join("    " + line for line in lines))
    summary = [line for line in output.splitlines() if line.startswith(("OK (", "Tests run:", "FAILURES!!!"))]
    print("Instrumentation summary: " + (" | ".join(summary) or "no JUnit summary"))


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
        print_failures(test_result.stdout)

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
    # Two required states rendering the same pixels means one capture does not show its state.
    by_hash: dict[str, list[str]] = {}
    for screen in SCREENS:
        for f in files:
            if f["file"].startswith(screen):
                by_hash.setdefault(f["sha256"], []).append(screen)
    identical = sorted(names for names in by_hash.values()
                       if len(names) > 1 and not any(set(names) <= alias for alias in ALIASES))
    expected_dimensions = (1920, 1080) if args.resolution == "1080p" else (3840, 2160)
    unexpected_dimensions = [f["file"] for f in files if (f["width"], f["height"]) != expected_dimensions]
    manifest = {
        "source_commit": sha, "source_tree_dirty": bool(dirty),
        "captured_at_utc": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()),
        "device_serial": args.serial, "requested_resolution": args.resolution,
        "fixture_locale": args.locale,
        "device_fingerprint": adb("shell", "getprop", "ro.build.fingerprint").stdout.strip(),
        "screenshots": files, "missing_required_screens": missing,
        "unexpected_dimensions": unexpected_dimensions,
        "identical_required_screens": identical,
        "instrumentation_status": ("PASS" if instrumentation_succeeded(test_result) else "FAIL")
            if test_result is not None else "NOT_RUN_CAPTURE_ONLY",
        "visual_review_status": "PENDING", "netflix_reference_comparison_status": "PENDING",
        "hardware_performance_status": "MANUAL-PENDING",
        "scope": "Actual production Compose components with offline deterministic fixtures; no backend or playback-engine certification.",
    }
    (args.output / "manifest.json").write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")
    print(f"Collected {len(files)} screenshots; missing required screens: {len(missing)}; "
          f"unexpected dimensions: {len(unexpected_dimensions)}; identical required states: {identical}")
    if missing:
        print("Missing: " + ", ".join(missing))
    if test_result and not instrumentation_succeeded(test_result):
        return 1
    return 1 if missing or unexpected_dimensions or identical else 0


if __name__ == "__main__":
    raise SystemExit(main())
