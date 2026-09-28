#!/usr/bin/env python3
from pathlib import Path
import argparse
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]
DEFAULT_RESULTS = ROOT / "app/build/test-results/testFullDebugUnitTest"
DEFAULT_ALLOW = ROOT / "integration/known_baseline_test_failures.txt"

parser = argparse.ArgumentParser()
parser.add_argument("--results", type=Path, default=DEFAULT_RESULTS)
parser.add_argument("--allowlist", type=Path, default=DEFAULT_ALLOW)
parser.add_argument("--exit-code-file", type=Path)
args = parser.parse_args()

known = {
    line.strip()
    for line in args.allowlist.read_text(encoding="utf-8").splitlines()
    if line.strip() and not line.lstrip().startswith("#")
}

actual = set()
xml_files = sorted(args.results.glob("*.xml")) if args.results.exists() else []
for path in xml_files:
    try:
        root = ET.parse(path).getroot()
    except ET.ParseError as exc:
        print(f"Invalid JUnit XML {path}: {exc}")
        sys.exit(1)
    for case in root.iter("testcase"):
        if case.find("failure") is None and case.find("error") is None:
            continue
        classname = (case.attrib.get("classname") or "Unknown").split(".")[-1]
        name = case.attrib.get("name") or "Unknown"
        actual.add(f"{classname}#{name}")

exit_code = 0
if args.exit_code_file:
    try:
        exit_code = int(args.exit_code_file.read_text(encoding="utf-8").strip())
    except Exception as exc:
        print(f"Unable to read Gradle test exit code: {exc}")
        sys.exit(1)

new_failures = sorted(actual - known)
fixed_or_absent = sorted(known - actual)

print(f"JUnit XML files: {len(xml_files)}")
print(f"Current failing tests: {len(actual)}")
print(f"Known baseline failures still present: {len(actual & known)}")
print(f"Known baseline failures now fixed/absent: {len(fixed_or_absent)}")

if new_failures:
    print("NEW UNIT TEST FAILURES:")
    for item in new_failures:
        print(f"- {item}")
    sys.exit(1)

if exit_code != 0 and not actual:
    print(f"Gradle tests exited {exit_code}, but no JUnit failures were parsed. Treating as infrastructure/compile failure.")
    sys.exit(1)

if actual:
    print("Only allowlisted baseline failures are present.")
else:
    print("All parsed unit tests pass.")

sys.exit(0)
