#!/usr/bin/env python3
from pathlib import Path
import re, sys

ROOT = Path(__file__).resolve().parents[1]
errors = []

required = [
    "AGENTS.md",
    "docs/START_HERE.md",
    "docs/HANDOFF.md",
    "docs/BASELINE.md",
    "docs/ARCHITECTURE.md",
    "docs/MASTER_FEATURES.md",
    "docs/DECISIONS.md",
    "docs/FORK_RESEARCH.md",
    "docs/SOURCE_MAP.md",
    "docs/ROADMAP.md",
    "docs/GATE_CHECKLISTS.md",
    "docs/DEFINITION_OF_DONE.md",
    "docs/TEST_MATRIX.md",
    "docs/IMPORT_LEDGER.md",
    "docs/RUNBOOK.md",
    "docs/BRANCHING.md",
    "docs/RECOVERY.md",
    "docs/UPSTREAM_SYNC.md",
    "integration/features.yaml",
    "integration/feature_coverage.yaml",
    "integration/state.yaml",
]
for rel in required:
    if not (ROOT / rel).is_file():
        errors.append(f"missing required file: {rel}")

master_path = ROOT / "docs/MASTER_FEATURES.md"
master = master_path.read_text(encoding="utf-8") if master_path.exists() else ""
ids = [int(x) for x in re.findall(r"(?m)^(\d+)\.\s+", master)]
if ids != list(range(1, 321)):
    missing = sorted(set(range(1,321)) - set(ids))
    dupes = sorted({x for x in ids if ids.count(x) > 1})
    errors.append(f"MASTER_FEATURES must contain IDs 1..320 exactly once; missing={missing}, duplicates={dupes}")

coverage_path = ROOT / "integration/feature_coverage.yaml"
if coverage_path.exists():
    coverage = coverage_path.read_text(encoding="utf-8")
    mapped = []
    for line in coverage.splitlines():
        if "feature_ids:" in line:
            part = line.split("feature_ids:",1)[1]
            mapped += [int(x) for x in re.findall(r"\b\d+\b", part)]
        if "feature_range:" in line and "feature_ranges:" not in line:
            m = re.search(r'"(\d+)-(\d+)"', line)
            if m:
                mapped += list(range(int(m.group(1)), int(m.group(2))+1))
        if "feature_ranges:" in line:
            for a,b in re.findall(r'"(\d+)-(\d+)"', line):
                mapped += list(range(int(a), int(b)+1))
    missing = sorted(set(range(1,321)) - set(mapped))
    dupes = sorted({x for x in mapped if mapped.count(x) > 1})
    extra = sorted(set(mapped) - set(range(1,321)))
    if missing or dupes or extra:
        errors.append(f"feature coverage invalid; missing={missing}, duplicates={dupes}, extra={extra}")

baseline = "c257a2365ee3386b582dc2974ec235cfe0381f33"
for rel in ["docs/BASELINE.md","docs/SOURCE_MAP.md","integration/state.yaml"]:
    p = ROOT / rel
    if p.exists() and baseline not in p.read_text(encoding="utf-8"):
        errors.append(f"{rel} does not reference pinned baseline {baseline}")

agents = (ROOT / "AGENTS.md").read_text(encoding="utf-8") if (ROOT / "AGENTS.md").exists() else ""
for rel in ["docs/START_HERE.md","docs/MASTER_FEATURES.md","docs/DECISIONS.md","docs/FORK_RESEARCH.md","docs/HANDOFF.md"]:
    if rel not in agents:
        errors.append(f"AGENTS.md does not require/read {rel}")

state = (ROOT / "integration/state.yaml").read_text(encoding="utf-8") if (ROOT / "integration/state.yaml").exists() else ""
for token in ["integration_branch: superfork/integration","task_branch:","status:","gate:"]:
    if token not in state:
        errors.append(f"integration/state.yaml missing required token: {token}")

if errors:
    print("SUPERFORK GUARD: FAILED")
    for e in errors:
        print(f"- {e}")
    sys.exit(1)

print("SUPERFORK GUARD: PASS")
print("- governance files present")
print("- master feature IDs 1..320 complete")
print("- feature coverage maps IDs 1..320 exactly once")
print("- baseline references consistent")
print("- agent entrypoint references required context")
