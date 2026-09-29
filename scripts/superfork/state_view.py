#!/usr/bin/env python3
"""Render only the state headers; narrative handoff evidence stays human-owned."""
from pathlib import Path
import argparse
import yaml
ROOT = Path(__file__).resolve().parents[2]
START = '<!-- canonical-state:start -->'
END = '<!-- canonical-state:end -->'


def snapshot(state):
    return '\n'.join([START,
        f"- Active gate: {state['active_gate']} — {state['active_gate_name']}",
        f"- Active branch: `{state['active_branch']}`",
        f"- Status: {state['active_status']}",
        f"- Task: `{state['active_task_packet']}`",
        f"- Accepted official baseline: `{state['current_official_baseline_sha']}`",
        f"- Governance: {state['governance_status']}",
        '- Owner of these fields: `integration/state.yaml`; regenerate with `state_view.py`.', END])


def main():
    p=argparse.ArgumentParser();p.add_argument('--check',action='store_true');a=p.parse_args()
    text=snapshot(yaml.safe_load((ROOT/'integration/state.yaml').read_text()))
    bad=[]
    for name in ['docs/PROJECT_STATUS.md','docs/HANDOFF.md']:
        path=ROOT/name;s=path.read_text()
        if START in s and END in s:
            before, rest=s.split(START,1);_,after=rest.split(END,1)
            new=before+text+after
        else:
            first,rest=s.split('\n',1);new=first+'\n\n'+text+'\n'+rest
        if a.check and new!=s: bad.append(name)
        elif not a.check: path.write_text(new)
    if bad: raise SystemExit('Stale state views: '+', '.join(bad))

if __name__=='__main__':main()
