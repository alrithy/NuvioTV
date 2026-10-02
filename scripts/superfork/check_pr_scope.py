#!/usr/bin/env python3
"""PR lifecycle/scope gate; never guesses a diff when fetch/history is missing."""
import argparse
import csv
import io
from pathlib import Path
import subprocess
import sys
import yaml
from user_authorized_task import (
    TASK_BRANCH,
    TASK_FEATURE_IDS,
    TASK_PACKET,
    validate_user_authorized_task,
)
from validate_project_state import UniqueLoader

ROOT = Path(__file__).resolve().parents[2]


def read_traceability(text):
    rows = list(csv.DictReader(io.StringIO(text)))
    ids = [int(row['feature_id']) for row in rows]
    if sorted(ids) != list(range(1, 321)):
        raise ValueError('Traceability needs each existing feature ID 1..320 exactly once')
    return {int(row['feature_id']): row for row in rows}


def user_task_traceability_errors(base_text, head_text):
    errors = []
    base_rows = read_traceability(base_text)
    head_rows = read_traceability(head_text)
    for fid, base_row in base_rows.items():
        head_row = head_rows[fid]
        if fid not in TASK_FEATURE_IDS:
            if head_row != base_row:
                errors.append('User-authorized task changed undeclared feature ' + str(fid))
            continue
        for key, value in base_row.items():
            if key != 'evidence' and head_row.get(key) != value:
                errors.append('User-authorized task may only append evidence for feature ' + str(fid))
                break
        prior = base_row.get('evidence', '')
        current = head_row.get('evidence', '')
        if not current.startswith(prior) or current == prior or TASK_PACKET not in current[len(prior):]:
            errors.append('Feature ' + str(fid) + ' must retain prior evidence and append its task packet')
    return errors


def evaluate_scope(base, head, changed, head_branch, root, base_traceability=None):
    runtime = any(not (
        path.startswith(('docs/', 'integration/', 'tasks/', '.github/', 'scripts/superfork/')) or
        path in {'AGENTS.md', 'CLAUDE.md', 'CODEX_START.md', 'SUPERFORK.md',
                 'CONTRIBUTING_SUPERFORK.md', '.gitignore', 'scripts/superfork_guard.py'}
    ) for path in changed)
    governance = head_branch.startswith('chore/governance-')
    sync = head_branch.startswith('chore/upstream-sync-')
    standalone = head_branch == TASK_BRANCH
    errors = []
    if governance and runtime:
        errors.append('Governance exception cannot change runtime/build code')
    if standalone:
        record = head.get('user_authorized_task')
        if record is None:
            errors.append('Netflix branch requires the explicit user-authorized task record')
        else:
            errors.extend(validate_user_authorized_task(record, root))
        canonical_base = {k: v for k, v in base.items() if k != 'user_authorized_task'}
        canonical_head = {k: v for k, v in head.items() if k != 'user_authorized_task'}
        if canonical_head != canonical_base:
            errors.append('User-authorized task cannot change canonical gate state')
        if base_traceability is None:
            errors.append('User-authorized task requires base traceability evidence')
        else:
            errors.extend(user_task_traceability_errors(
                base_traceability, (root / 'integration/feature_traceability.csv').read_text()
            ))
    elif not governance and not sync and head_branch != base['active_branch']:
        errors.append('PR branch differs from base integration active branch')

    required = set()
    if 'docs/SOURCE_MAP.md' in changed:
        required.add('docs/DECISIONS.md')
    if runtime or standalone:
        required |= {'docs/HANDOFF.md', 'docs/PROJECT_STATUS.md', 'integration/state.yaml'}
    if (runtime and not sync) or standalone:
        required.add('integration/feature_traceability.csv')
    if standalone:
        required.add(TASK_PACKET)
    if sync:
        required |= {'docs/UPSTREAM_SYNC_LOG.md', 'docs/SOURCE_MAP.md', 'docs/BASELINE.md',
                     'integration/baseline_test_debt.json'}
    if not required <= changed:
        errors.append('Missing required handoff/audit updates: ' + str(sorted(required - changed)))
    return errors


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--base', required=True)
    parser.add_argument('--head-branch', required=True)
    args = parser.parse_args()
    base = yaml.load(subprocess.check_output(
        ['git', 'show', args.base + ':integration/state.yaml'], text=True, cwd=ROOT
    ), Loader=UniqueLoader)
    head = yaml.load((ROOT / 'integration/state.yaml').read_text(), Loader=UniqueLoader)
    changed = set(subprocess.check_output(
        ['git', 'diff', '--name-only', args.base + '...HEAD'], text=True, cwd=ROOT
    ).splitlines())
    base_traceability = None
    if args.head_branch == TASK_BRANCH:
        base_traceability = subprocess.check_output(
            ['git', 'show', args.base + ':integration/feature_traceability.csv'], text=True, cwd=ROOT
        )
    errors = evaluate_scope(base, head, changed, args.head_branch, ROOT, base_traceability)
    for error in errors:
        print(error, file=sys.stderr)
    print('PR scope/handoff: ' + ('FAIL' if errors else 'PASS'))
    return int(bool(errors))


if __name__ == '__main__':
    sys.exit(main())
