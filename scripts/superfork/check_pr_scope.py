#!/usr/bin/env python3
"""PR lifecycle/scope gate; never guesses a diff when fetch/history is missing."""
import argparse
from pathlib import Path
import subprocess
import sys
import yaml
ROOT=Path(__file__).resolve().parents[2]

def main():
    p=argparse.ArgumentParser();p.add_argument('--base',required=True);p.add_argument('--head-branch',required=True);a=p.parse_args()
    base=yaml.safe_load(subprocess.check_output(['git','show',a.base+':integration/state.yaml'],text=True,cwd=ROOT))
    changed=set(subprocess.check_output(['git','diff','--name-only',a.base+'...HEAD'],text=True,cwd=ROOT).splitlines())
    runtime=any(not (x.startswith(('docs/','integration/','tasks/','.github/','scripts/superfork/')) or x in
        {'AGENTS.md','CLAUDE.md','CODEX_START.md','SUPERFORK.md','CONTRIBUTING_SUPERFORK.md','.gitignore','scripts/superfork_guard.py'}) for x in changed)
    governance=a.head_branch.startswith('chore/governance-')
    sync=a.head_branch.startswith('chore/upstream-sync-')
    errors=[]
    if governance and runtime:errors.append('Governance exception cannot change runtime/build code')
    if not governance and not sync and a.head_branch!=base['active_branch']:errors.append('PR branch differs from base integration active branch')
    required=set()
    if 'docs/SOURCE_MAP.md' in changed:required.add('docs/DECISIONS.md')
    if runtime:required|={'docs/HANDOFF.md','docs/PROJECT_STATUS.md','integration/state.yaml'}
    if runtime and not sync:required.add('integration/feature_traceability.csv')
    if sync:required|={'docs/UPSTREAM_SYNC_LOG.md','docs/SOURCE_MAP.md','docs/BASELINE.md','integration/baseline_test_debt.json'}
    if not required<=changed:errors.append('Missing required handoff/audit updates: '+str(sorted(required-changed)))
    for e in errors:print(e,file=sys.stderr)
    print('PR scope/handoff: '+('FAIL' if errors else 'PASS'))
    return int(bool(errors))
if __name__=='__main__':sys.exit(main())
