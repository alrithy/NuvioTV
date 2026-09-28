#!/usr/bin/env python3
from pathlib import Path
import csv, re, sys

ROOT = Path(__file__).resolve().parents[2]
errors=[]

required = [
 "AGENTS.md","CLAUDE.md","CONTRIBUTING_SUPERFORK.md","docs/PROJECT_STATUS.md","docs/HANDOFF.md",
 "docs/BASELINE.md","docs/ARCHITECTURE.md","docs/MASTER_FEATURES.md",
 "docs/DECISIONS.md","docs/FORK_RESEARCH.md","docs/SOURCE_MAP.md",
 "docs/ROADMAP.md","docs/GATE_SPECS.md","docs/TEST_MATRIX.md",
 "docs/DEFINITION_OF_DONE.md","docs/IMPORT_LEDGER.md","docs/AGENT_PLAYBOOK.md",
 "docs/BRANCHING.md","docs/UPSTREAM_SYNC.md","docs/SECURITY_POLICY.md",
 "docs/RELEASE_POLICY.md","docs/FAILURE_RECOVERY.md","docs/LOCAL_SETUP.md",
 "docs/BASELINE_TEST_DEBT.md",
 "integration/features.yaml","integration/state.yaml","integration/feature_traceability.csv",
 "integration/known_baseline_test_failures.txt",
 "scripts/superfork/check_test_failures.py"
]
for p in required:
    if not (ROOT/p).exists():
        errors.append(f"missing required file: {p}")

baseline="c257a2365ee3386b582dc2974ec235cfe0381f33"
for p in ["docs/BASELINE.md","docs/SOURCE_MAP.md","integration/features.yaml","integration/state.yaml","docs/BASELINE_TEST_DEBT.md"]:
    f=ROOT/p
    if f.exists() and baseline not in f.read_text(encoding="utf-8"):
        errors.append(f"baseline SHA mismatch/missing in {p}")

mf=ROOT/"docs/MASTER_FEATURES.md"
ids=[]
if mf.exists():
    ids=[int(x) for x in re.findall(r"(?m)^(\d+)\.\s+", mf.read_text(encoding="utf-8"))]
    if ids != list(range(1,321)):
        missing=sorted(set(range(1,321))-set(ids))
        dup=sorted({x for x in ids if ids.count(x)>1})
        errors.append(f"MASTER_FEATURES must contain IDs 1..320 exactly once; missing={missing}, duplicates={dup}")

tr=ROOT/"integration/feature_traceability.csv"
if tr.exists():
    with tr.open(encoding="utf-8", newline="") as f:
        rows=list(csv.DictReader(f))
    tids=[int(r["feature_id"]) for r in rows]
    if sorted(tids) != list(range(1,321)) or len(tids)!=320:
        errors.append("feature_traceability.csv must map all 320 IDs exactly once")
    if any(r["gate"]=="UNMAPPED" for r in rows):
        errors.append("feature_traceability.csv contains UNMAPPED feature IDs")

allow=ROOT/"integration/known_baseline_test_failures.txt"
if allow.exists():
    known=[x.strip() for x in allow.read_text(encoding="utf-8").splitlines() if x.strip() and not x.lstrip().startswith("#")]
    if len(known) != len(set(known)):
        errors.append("known baseline unit-test allowlist contains duplicates")
    if len(known) != 18:
        errors.append(f"expected 18 captured baseline unit-test failures, found {len(known)}; investigate and update debt docs intentionally")

road=ROOT/"docs/ROADMAP.md"
if road.exists():
    txt=road.read_text(encoding="utf-8")
    for g in range(15):
        if f"G{g} " not in txt and f"G{g} —" not in txt:
            errors.append(f"ROADMAP missing G{g}")

agents=ROOT/"AGENTS.md"
if agents.exists():
    at=agents.read_text(encoding="utf-8")
    for p in ["CONTRIBUTING_SUPERFORK.md","integration/state.yaml","docs/AGENT_PLAYBOOK.md","docs/DEFINITION_OF_DONE.md","integration/feature_traceability.csv"]:
        if p not in at:
            errors.append(f"AGENTS.md does not reference {p}")

state=ROOT/"integration/state.yaml"
if state.exists():
    st=state.read_text(encoding="utf-8")
    for key in ["active_gate:","active_branch:","active_status:","integration_branch:","last_green_commit:"]:
        if key not in st:
            errors.append(f"state.yaml missing {key}")

if errors:
    print("SUPERFORK GOVERNANCE CHECK: FAIL")
    for e in errors:
        print(f"- {e}")
    sys.exit(1)

print("SUPERFORK GOVERNANCE CHECK: PASS")
print("320/320 feature IDs mapped; required docs and baseline references present.")
