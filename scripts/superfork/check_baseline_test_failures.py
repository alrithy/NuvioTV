#!/usr/bin/env python3
from pathlib import Path
import sys
import xml.etree.ElementTree as ET

if len(sys.argv) != 3:
    print("usage: check_baseline_test_failures.py <junit-results-dir> <baseline-file>", file=sys.stderr)
    sys.exit(2)

results_dir = Path(sys.argv[1])
baseline_file = Path(sys.argv[2])

if not baseline_file.is_file():
    print(f"baseline failure file missing: {baseline_file}", file=sys.stderr)
    sys.exit(2)

allowed = {
    line.strip()
    for line in baseline_file.read_text(encoding="utf-8").splitlines()
    if line.strip() and not line.lstrip().startswith("#")
}

xml_files = sorted(results_dir.glob("TEST-*.xml")) if results_dir.is_dir() else []
if not xml_files:
    print(f"NO TEST XML FOUND in {results_dir}; test task may have failed before execution.", file=sys.stderr)
    sys.exit(1)

observed = set()
tests = failures = errors = skipped = 0

for xml_file in xml_files:
    try:
        root = ET.parse(xml_file).getroot()
    except ET.ParseError as exc:
        print(f"invalid JUnit XML {xml_file}: {exc}", file=sys.stderr)
        sys.exit(1)

    for case in root.iter("testcase"):
        tests += 1
        classname = case.attrib.get("classname", "").split(".")[-1]
        name = case.attrib.get("name", "")
        has_failure = case.find("failure") is not None
        has_error = case.find("error") is not None
        if case.find("skipped") is not None:
            skipped += 1
        if has_failure or has_error:
            if has_failure:
                failures += 1
            if has_error:
                errors += 1
            observed.add(f"{classname}#{name}")

new_failures = sorted(observed - allowed)
known_remaining = sorted(observed & allowed)
resolved_baseline = sorted(allowed - observed)

print(f"tests={tests} observed_failures={len(observed)} skipped={skipped}")
print(f"known_baseline_failures_remaining={len(known_remaining)}")
print(f"resolved_baseline_entries={len(resolved_baseline)}")

if known_remaining:
    print("\nKnown baseline failures still present:")
    for item in known_remaining:
        print(f"  BASELINE  {item}")

if resolved_baseline:
    print("\nBaseline entries no longer failing (candidate for debt-list cleanup):")
    for item in resolved_baseline:
        print(f"  RESOLVED? {item}")

if new_failures:
    print("\nNEW FAILURES DETECTED:", file=sys.stderr)
    for item in new_failures:
        print(f"  NEW  {item}", file=sys.stderr)
    sys.exit(1)

print("\nNO NEW UNIT-TEST FAILURES: PASS")
