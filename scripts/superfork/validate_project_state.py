#!/usr/bin/env python3
"""Canonical governance validator; no network, no mutations, no fixed debt count."""
import argparse
from collections import Counter
import csv
import json
import io
from pathlib import Path
import re
import sys
import yaml
from state_view import snapshot
from user_authorized_task import validate_user_authorized_task

class UniqueLoader(yaml.SafeLoader):
    pass

def mapping(loader,node,deep=False):
    result={}
    for k,v in node.value:
        key=loader.construct_object(k,deep=deep)
        if key in result:raise ValueError('Duplicate YAML key: '+str(key))
        result[key]=loader.construct_object(v,deep=deep)
    return result
UniqueLoader.add_constructor(yaml.resolver.BaseResolver.DEFAULT_MAPPING_TAG,mapping)


def validate(root):
    errors=[]
    def need(ok,message):
        if not ok:errors.append(message)
    required=['AGENTS.md','CLAUDE.md','CODEX_START.md','SUPERFORK.md','CONTRIBUTING_SUPERFORK.md']
    required+=['docs/'+x+'.md' for x in ['START_HERE','INDEX','PROJECT_STATUS','HANDOFF','BASELINE','ARCHITECTURE','MASTER_FEATURES','DECISIONS','FORK_RESEARCH','SOURCE_MAP','ROADMAP','GATE_SPECS','TEST_MATRIX','DEFINITION_OF_DONE','IMPORT_LEDGER','AGENT_PLAYBOOK','BRANCHING','UPSTREAM_SYNC','UPSTREAM_SYNC_LOG','SECURITY_POLICY','RELEASE_POLICY','FAILURE_RECOVERY','LOCAL_SETUP','GITHUB_ADMIN_CHECKLIST','COMPONENT_MAP','PORTING_PROTOCOL','STATUS_MODEL','BASELINE_TEST_DEBT','GOVERNANCE_OWNERS','FEATURE_FLAG_POLICY','MANUAL_TEST_LOG','LICENSE_AND_ATTRIBUTION']]
    required+=['integration/'+x for x in ['features.yaml','state.yaml','feature_traceability.csv','task_queue.csv','baseline_test_debt.json']]
    required+=['scripts/superfork/'+x for x in ['preflight.py','claim_task.py','run_full_unit_suite.py','check_baseline_test_failures.py','test-report.init.gradle']]
    for path in required:need((root/path).is_file(),'Missing '+path)
    if errors:return errors
    def y(path):return yaml.load((root/path).read_text(),Loader=UniqueLoader)
    state=y('integration/state.yaml');base=state['current_official_baseline_sha']
    need(re.fullmatch(r'[0-9a-f]{40}',base) is not None,'Invalid official baseline SHA')
    need(state['repository']=='alrithy/NuvioTV','Wrong repository')
    need(state['integration_branch']=='superfork/integration','Wrong integration branch')
    need(state['active_status'] in {'READY','IN_PROGRESS','BLOCKED','REVIEW','DONE'},'Invalid gate status')
    need(state['governance_status'] in {'REVIEW','READY'},'Invalid governance status')
    need(state['concurrent_writers_allowed'] is False,'Exactly one writer required')
    errors.extend(validate_user_authorized_task(state.get('user_authorized_task'), root))
    if state['active_status']=='BLOCKED':need(bool(state.get('blocker')),'Blocked gate requires evidence/next step in blocker')
    for doc in ['docs/BASELINE.md','docs/SOURCE_MAP.md']:
        need(base in (root/doc).read_text(),doc+' disagrees with accepted baseline')
    views=snapshot(state)
    for doc in ['docs/PROJECT_STATUS.md','docs/HANDOFF.md']:
        need(views in (root/doc).read_text(),'Stale generated state view: '+doc)
    master_matches=re.findall(r'(?m)^(\d+)\.\s+(.+)$',(root/'docs/MASTER_FEATURES.md').read_text())
    master_ids=[int(x[0]) for x in master_matches]
    need(sorted(master_ids)==list(range(1,321)),'MASTER_FEATURES needs each ID 1..320 exactly once (including duplicates)')
    master={int(k):v.strip() for k,v in master_matches}
    rows=list(csv.DictReader(io.StringIO((root/'integration/feature_traceability.csv').read_text())))
    need(sorted(int(r['feature_id']) for r in rows)==list(range(1,321)),'Traceability needs each ID 1..320 exactly once')
    gates={f'G{i}' for i in range(15)}
    aliases={'official','project','foundation','ysosrs','reshaped','cxsmo','hackerslash-lite','vibe-subtitle','antonino-watchparty','fornace','nuvio-glass'}
    for row in rows:
        fid=int(row['feature_id']);prefix=f'Feature {fid}: '
        need(row['gate'] in gates,prefix+'invalid gate')
        need(bool(row['owner'].strip()),prefix+'missing owner')
        need(bool(row['source_group']) and set(row['source_group'].split('+'))<=aliases,prefix+'unresolved source group')
        need(row['feature'].strip()==master.get(fid),prefix+'scope text differs')
        need(row['status'] in {'planned','in_progress','implemented','verified_official','blocked','deferred','experimental'},prefix+'invalid status')
        if row['status'] in {'implemented','verified_official','experimental','blocked','deferred'}:
            need(bool(row['evidence']),prefix+'status requires evidence')
        if row['status'] in {'blocked','deferred'}:
            need(bool(row['reason']) and bool(row['next_action']),prefix+'requires reason and next action')
    queue=list(csv.DictReader(io.StringIO((root/'integration/task_queue.csv').read_text())))
    need([r['gate'] for r in queue]==[f'G{i}' for i in range(15)],'Queue must be G0..G14 exactly in order')
    need('status' not in queue[0],'Queue must not duplicate state progress')
    specs=(root/'docs/GATE_SPECS.md').read_text()
    for i,row in enumerate(queue):
        need(row['depends_on']==('governance' if i==0 else f'G{i-1}'),'Queue dependency mismatch '+row['gate'])
        need(row['branch'] not in {'dev',state['integration_branch']},'Unsafe task branch')
        packet=root/row['task_packet'];need(packet.is_file(),'Missing '+row['task_packet'])
        need('## '+row['gate']+' —' in specs,'Missing gate specification '+row['gate'])
        if row['gate']==state['active_gate']:
            need(row['branch']==state['active_branch'] and row['task_packet']==state['active_task_packet'],'Active state/queue mismatch')
    need(state['active_gate'] in gates,'Unknown active gate')
    if state['active_status']=='DONE':
        need(not any(r['gate']==state['active_gate'] and r['status'] in {'planned','in_progress'} for r in rows),'DONE gate contains unfinished features')
        need(any(g['gate']==state['active_gate'] and g.get('merge_sha') and g.get('pr') and g.get('evidence') for g in state['completed_gates']),'DONE needs merged PR and evidence')
    debt=json.loads((root/'integration/baseline_test_debt.json').read_text())
    ids=[x['test_id'] for x in debt['failures']]
    need(len(ids)==len(set(ids)),'Duplicate test debt')
    need(debt['baseline_sha']==base,'Debt baseline mismatch; review before upstream sync')
    need(isinstance(debt['minimum_test_count'],int) and debt['minimum_test_count']>0,'Missing baseline suite floor')
    for item in debt['failures']:
        need(re.fullmatch(r'[\w.$]+\.[\w.$]+#.+',item['test_id']) is not None,'Debt requires full test identifier')
        need(all(item.get(k) for k in ['baseline_sha','recorded_on','reason','evidence','source_file','removal']),'Incomplete test debt evidence')
        need(re.fullmatch(r'[0-9a-f]{40}',item['baseline_sha']) is not None,'Invalid original debt SHA')
        need((root/item['source_file']).is_file(),'Debt source file missing')
    inventory=root/'integration/evidence/baseline-suite.json'
    if inventory.exists():
        observed=json.loads(inventory.read_text())
        need(observed['tested_sha']==debt['baseline_sha'],'Baseline inventory SHA mismatch')
        need(observed['tests']==len(observed['results']),'Inventory count mismatch')
        need(observed['gradle_exit']==0 and all(e.endswith(' new unit-test failures') for e in observed['errors']),'Baseline evidence contains unexplained execution/regression failures')
        need(set(ids)<=set(observed['results']),'Debt identifier absent from clean baseline evidence')
    # Compatibility files can redirect/delegate only; independent requirements are forbidden.
    redirects={'GATE_CHECKLISTS':'GATE_SPECS','RUNBOOK':'AGENT_PLAYBOOK','RECOVERY':'FAILURE_RECOVERY','OWNERSHIP_MAP':'COMPONENT_MAP','DATA_MIGRATIONS':'DATA_MIGRATION_POLICY','PERFORMANCE_VALIDATION':'TEST_STRATEGY','RELEASE_CHECKLIST':'RELEASE_POLICY'}
    for old,new in redirects.items():
        p=root/'docs'/f'{old}.md';text=p.read_text()
        need('redirect' in text.lower() and new+'.md' in text and len(text.splitlines())<=10,'Competing canonical document: '+old)
    for p in [root/'.github/workflows/superfork-ci.yml',root/'.github/workflows/superfork-baseline-audit.yml']:
        text=p.read_text();y(str(p.relative_to(root)))
        need('continue-on-error:' not in text and '|| true' not in text,'Catch-all failure suppression in '+p.name)
    need('run_full_unit_suite.py' in (root/'.github/workflows/superfork-ci.yml').read_text(),'Full suite not wired to CI')
    return errors


def main():
    p=argparse.ArgumentParser();p.add_argument('--root',type=Path,default=Path(__file__).resolve().parents[2]);a=p.parse_args()
    try:errors=validate(a.root)
    except (KeyError,ValueError,TypeError,OSError,yaml.YAMLError) as exc:errors=['Invalid governance data: '+str(exc)]
    print('SUPERFORK GOVERNANCE CHECK: '+('FAIL' if errors else 'PASS'))
    for error in errors:print('- '+error)
    if not errors:print('320/320 unique features; state/queue/views, sources, debt and canonical ownership validated.')
    return int(bool(errors))

if __name__=='__main__':sys.exit(main())
