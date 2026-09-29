#!/usr/bin/env python3
"""Read-only preflight (except refreshing remote-tracking refs). Never reset work."""
import argparse
import json
from pathlib import Path
import subprocess
import sys
import yaml
ROOT=Path(__file__).resolve().parents[2]


def git(*args, check=True):
    return subprocess.run(['git',*args],cwd=ROOT,check=check,text=True,capture_output=True)


def main():
    p=argparse.ArgumentParser();p.add_argument('--fetch',action='store_true');a=p.parse_args()
    errors=[]
    if git('status','--porcelain').stdout.strip():errors.append('Dirty tree: preserve and inspect work using FAILURE_RECOVERY')
    if a.fetch:
        git('fetch','origin','+refs/heads/*:refs/remotes/origin/*','--prune')
        git('fetch','https://github.com/NuvioMedia/NuvioTV.git','dev:refs/remotes/upstream/dev')
    local=yaml.safe_load((ROOT/'integration/state.yaml').read_text())
    target='origin/'+local['integration_branch']
    remote=yaml.safe_load(git('show',target+':integration/state.yaml').stdout)
    for key in ['active_gate','active_branch','active_task_packet','current_official_baseline_sha']:
        if local.get(key)!=remote.get(key):errors.append('Stale/conflicting state field: '+key)
    branch=git('branch','--show-current').stdout.strip()
    if branch!=remote['active_branch']:errors.append('Wrong branch; use '+remote['active_branch'])
    integration=git('rev-parse',target).stdout.strip()
    if git('merge-base','--is-ancestor',integration,'HEAD',check=False).returncode:
        errors.append('Task branch does not contain latest integration; inspect unique work then fast-forward/merge')
    baseline=remote.get('current_official_baseline_sha') or remote.get('baseline_sha')
    if not baseline:raise ValueError('Integration state schema is incomplete; repair governance before coding')
    if git('merge-base','--is-ancestor',baseline,target,check=False).returncode:
        errors.append('Accepted official baseline is not an integration ancestor')
    if remote.get('governance_status')!='READY':errors.append('Governance is not ready on integration')
    if remote.get('active_status') not in {'READY','IN_PROGRESS','REVIEW'}:errors.append('Active task is blocked or needs transition')
    lease='refs/heads/agent-locks/'+remote['active_branch']
    lock=git('ls-remote','origin',lease).stdout.strip()
    head=git('rev-parse','HEAD').stdout.strip()
    upstream=git('rev-parse','refs/remotes/upstream/dev',check=False)
    print(json.dumps({'branch':branch,'head':head,'integration_head':integration,
        'active_gate':remote['active_gate'],'task':remote.get('active_task_packet','MISSING — repair integration governance'),
        'accepted_baseline':baseline,'observed_official_head':upstream.stdout.strip() if upstream.returncode==0 else 'NOT_FETCHED',
        'writer_lease':lock or 'UNCLAIMED — acquire before editing',
        'errors':errors},indent=2))
    print('Inspect remote open PRs and exact-head CI; then read AGENT_PLAYBOOK. Upstream movement is reviewed separately, never silently adopted.')
    return int(bool(errors))

if __name__=='__main__':
    try:sys.exit(main())
    except (subprocess.CalledProcessError,OSError,KeyError,ValueError) as exc:
        sys.exit('PREFLIGHT BLOCKED: '+str(exc))
