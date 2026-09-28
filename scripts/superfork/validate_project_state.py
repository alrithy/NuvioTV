#!/usr/bin/env python3
from pathlib import Path
import csv, re, sys

ROOT = Path(__file__).resolve().parents[2]
errors=[]

required = [
 "AGENTS.md","CLAUDE.md","SUPERFORK.md","docs/START_HERE.md","docs/INDEX.md",
 "docs/PROJECT_STATUS.md","docs/HANDOFF.md","docs/BASELINE.md","docs/ARCHITECTURE.md",
 "docs/MASTER_FEATURES.md","docs/DECISIONS.md","docs/FORK_RESEARCH.md","docs/SOURCE_MAP.md",
 "docs/ROADMAP.md","docs/GATE_SPECS.md","docs/TEST_MATRIX.md","docs/DEFINITION_OF_DONE.md",
 "docs/IMPORT_LEDGER.md","docs/AGENT_PLAYBOOK.md","docs/BRANCHING.md","docs/UPSTREAM_SYNC.md","docs/UPSTREAM_SYNC_LOG.md",
 "docs/SECURITY_POLICY.md","docs/RELEASE_POLICY.md","docs/FAILURE_RECOVERY.md","docs/LOCAL_SETUP.md",
 "docs/GITHUB_ADMIN_CHECKLIST.md","integration/features.yaml","integration/state.yaml",
 "integration/feature_traceability.csv"
]
for p in required:
    if not (ROOT/p).is_file():
        errors.append(f"missing required file: {p}")

baseline="fd7973d91dd75d790c5f9b3d68dae652655e92c4"
for p in ["docs/BASELINE.md","docs/SOURCE_MAP.md","integration/features.yaml","integration/state.yaml"]:
    f=ROOT/p
    if f.exists() and baseline not in f.read_text(encoding="utf-8"):
        errors.append(f"baseline SHA mismatch/missing in {p}")

master={}
mf=ROOT/"docs/MASTER_FEATURES.md"
if mf.exists():
    for m in re.finditer(r"(?m)^(\d+)\.\s+(.+)$", mf.read_text(encoding="utf-8")):
        master[int(m.group(1))]=m.group(2).strip()
    if sorted(master) != list(range(1,321)) or len(master)!=320:
        errors.append("MASTER_FEATURES must contain IDs 1..320 exactly once")

tr=ROOT/"integration/feature_traceability.csv"
rows=[]
if tr.exists():
    with tr.open(encoding="utf-8", newline="") as f:
        rows=list(csv.DictReader(f))
    tids=[int(r["feature_id"]) for r in rows]
    if sorted(tids) != list(range(1,321)) or len(tids)!=320:
        errors.append("feature_traceability.csv must map all 320 IDs exactly once")
    allowed_gates={f"G{i}" for i in range(15)}
    allowed_status={"planned","in_progress","implemented","verified_official","blocked","deferred","experimental"}
    for row in rows:
        fid=int(row["feature_id"])
        if row["gate"] not in allowed_gates:
            errors.append(f"feature {fid}: invalid gate {row['gate']}")
        if row["status"] not in allowed_status:
            errors.append(f"feature {fid}: invalid status {row['status']}")
        if master and row["feature"].strip() != master.get(fid,""):
            errors.append(f"feature {fid}: traceability text does not match MASTER_FEATURES")

for doc in ["docs/ROADMAP.md","docs/GATE_SPECS.md"]:
    p=ROOT/doc
    if p.exists():
        txt=p.read_text(encoding="utf-8")
        for g in range(15):
            if f"G{g} " not in txt and f"G{g} —" not in txt:
                errors.append(f"{doc} missing G{g}")

agents=ROOT/"AGENTS.md"
if agents.exists():
    at=agents.read_text(encoding="utf-8")
    for p in ["integration/state.yaml","docs/AGENT_PLAYBOOK.md","docs/GATE_SPECS.md","docs/DEFINITION_OF_DONE.md","integration/feature_traceability.csv"]:
        if p not in at:
            errors.append(f"AGENTS.md does not reference {p}")

state=ROOT/"integration/state.yaml"
if state.exists():
    st=state.read_text(encoding="utf-8")
    for key in ["active_gate:","active_branch:","active_status:","integration_branch:","last_green_commit:"]:
        if key not in st:
            errors.append(f"state.yaml missing {key}")
    gate=re.search(r"(?m)^active_gate:\s*(G\d+)\s*$",st)
    branch=re.search(r"(?m)^active_branch:\s*(\S+)\s*$",st)
    status=re.search(r"(?m)^active_status:\s*(\S+)\s*$",st)
    if gate and gate.group(1) not in {f"G{i}" for i in range(15)}:
        errors.append(f"state.yaml invalid active_gate {gate.group(1)}")
    if branch and branch.group(1) in {"dev","superfork/integration"}:
        errors.append("state.yaml active_branch must be a task branch, not dev/integration")
    if status and status.group(1) not in {"READY","IN_PROGRESS","BLOCKED","REVIEW","DONE"}:
        errors.append(f"state.yaml invalid active_status {status.group(1)}")
    for p in ["docs/PROJECT_STATUS.md","docs/HANDOFF.md"]:
        f=ROOT/p
        if f.exists():
            txt=f.read_text(encoding="utf-8")
            if gate and gate.group(1) not in txt:
                errors.append(f"{p} does not mention active gate {gate.group(1)}")
            if branch and branch.group(1) not in txt:
                errors.append(f"{p} does not mention active branch {branch.group(1)}")

if errors:
    print("SUPERFORK GOVERNANCE CHECK: FAIL")
    for e in errors:
        print(f"- {e}")
    sys.exit(1)

print("SUPERFORK GOVERNANCE CHECK: PASS")
print("320/320 feature IDs mapped; canonical docs, baseline, state and traceability are internally consistent.")