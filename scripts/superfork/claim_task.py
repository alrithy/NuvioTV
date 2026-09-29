#!/usr/bin/env python3
"""Atomic one-writer lease. Only lock refs may be compare-and-deleted."""
import argparse
import json
import subprocess
import uuid
from pathlib import Path
import yaml
ROOT=Path(__file__).resolve().parents[2]


def git(*args,input=None):
    return subprocess.check_output(['git',*args],cwd=ROOT,text=True,input=input).strip()


def main():
    p=argparse.ArgumentParser();p.add_argument('action',choices=['status','claim','release'])
    p.add_argument('--owner');p.add_argument('--lease-sha');a=p.parse_args()
    state=yaml.safe_load((ROOT/'integration/state.yaml').read_text())
    ref='refs/heads/agent-locks/'+state['active_branch']
    current=git('ls-remote','origin',ref)
    if a.action=='status':print(current or 'UNCLAIMED');return
    if a.action=='claim':
        if not a.owner:p.error('--owner is required')
        if current:raise SystemExit('WRITER EXISTS: inspect lease/HANDOFF; do not steal it')
        if git('status','--porcelain'):raise SystemExit('Commit/document dirty work before claiming')
        if git('branch','--show-current')!=state['active_branch']:raise SystemExit('Wrong task branch')
        # Sibling commits with unique messages make concurrent pushes non-fast-forward.
        msg=json.dumps({'owner':a.owner,'branch':state['active_branch'],'token':str(uuid.uuid4())})
        sha=git('commit-tree','HEAD^{tree}','-p','HEAD',input=msg+'\n')
        subprocess.run(['git','push','origin',sha+':'+ref],cwd=ROOT,check=True)
        print('LEASE '+sha+' — record owner in state/HANDOFF before work; keep SHA for release')
    else:
        if not a.lease_sha or not current or current.split()[0]!=a.lease_sha:
            raise SystemExit('Lease SHA mismatch; refusing to release another writer')
        if git('status','--porcelain'):raise SystemExit('Dirty work must be committed/documented before release')
        remote=git('ls-remote','origin','refs/heads/'+state['active_branch'])
        if not remote or remote.split()[0]!=git('rev-parse','HEAD'):
            raise SystemExit('Push coherent work and verify remote HEAD before release')
        subprocess.run(['git','push','--force-with-lease='+ref+':'+a.lease_sha,'origin',':'+ref],cwd=ROOT,check=True)
        print('Released exact writer lease; task/integration history unchanged')

if __name__=='__main__':main()
