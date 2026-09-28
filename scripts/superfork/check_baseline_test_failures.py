#!/usr/bin/env python3
"""Fail closed on new failures, incomplete execution, or hidden/deleted tests."""
import argparse
import json
from pathlib import Path
import sys
import xml.etree.ElementTree as ET


def read_results(directory):
    files = sorted(Path(directory).glob('TEST-*.xml'))
    if not files:
        raise ValueError('No JUnit reports: tests did not complete')
    results = {}
    for path in files:
        root = ET.parse(path).getroot()
        cases = list(root.iter('testcase'))
        if root.tag != 'testsuite' or int(root.get('tests', '-1')) != len(cases):
            raise ValueError(f'Incomplete JUnit suite: {path.name}')
        counts = {'failure': 0, 'error': 0, 'skipped': 0}
        for case in cases:
            cls, name = case.get('classname', ''), case.get('name', '')
            if '.' not in cls or not name:
                raise ValueError('Missing fully qualified test identifier')
            key = cls + '#' + name
            if key in results:
                raise ValueError(f'Duplicate test identifier: {key}')
            statuses = [x for x in counts if case.find(x) is not None]
            if len(statuses) > 1:
                raise ValueError(f'Conflicting result: {key}')
            kind = statuses[0] if statuses else 'passed'
            if statuses:
                counts[kind] += 1
            results[key] = kind
        for kind, attr in [('failure', 'failures'), ('error', 'errors'), ('skipped', 'skipped')]:
            if int(root.get(attr, '0')) != counts[kind]:
                raise ValueError(f'JUnit count mismatch: {path.name} {attr}')
    return results


def evaluate(results, debt, completion, gradle_exit, inventory=None):
    errors = []
    failed = {k for k, v in results.items() if v in {'failure', 'error'}}
    skipped = {k for k, v in results.items() if v == 'skipped'}
    entries = debt['failures']
    allowed = {r['test_id'] for r in entries}
    if len(allowed) != len(entries):
        errors.append('Duplicate baseline debt identifiers')
    if gradle_exit != 0:
        errors.append(f'Gradle infrastructure/build failure: exit {gradle_exit}')
    if not completion.get('completed') or any(completion.get(k) != v for k, v in
            [('tests', len(results)), ('failures', len(failed)), ('skipped', len(skipped))]):
        errors.append('Missing/inconsistent full-suite completion receipt')
    if len(results) < debt['minimum_test_count']:
        errors.append('Full suite is smaller than the reviewed baseline inventory')
    missing_debt = allowed - results.keys()
    if missing_debt or allowed & skipped:
        errors.append('Known failing tests are missing or skipped; this is not a fix')
    if inventory:
        missing = set(inventory['results']) - results.keys()
        new_skips = skipped - {k for k, v in inventory['results'].items() if v == 'skipped'}
        if missing:
            errors.append(f'{len(missing)} baseline tests missing; explicit inventory review required')
        if new_skips:
            errors.append(f'{len(new_skips)} newly skipped tests; explicit inventory review required')
    new = sorted(failed - allowed)
    if new:
        errors.append(f'{len(new)} new unit-test failures')
    return {
        'tests': len(results), 'failures': len(failed), 'skipped': len(skipped),
        'known_remaining': sorted(failed & allowed), 'new_failures': new,
        'resolved_candidates': sorted(k for k in allowed if results.get(k) == 'passed'),
        'errors': errors, 'results': results,
    }


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('results', type=Path)
    p.add_argument('baseline', type=Path)
    p.add_argument('--completion', type=Path, required=True)
    p.add_argument('--gradle-exit', type=int, required=True)
    p.add_argument('--inventory', type=Path)
    p.add_argument('--output', type=Path, required=True)
    a = p.parse_args()
    try:
        report = evaluate(read_results(a.results), json.loads(a.baseline.read_text()),
                          json.loads(a.completion.read_text()), a.gradle_exit,
                          json.loads(a.inventory.read_text()) if a.inventory else None)
    except (ValueError, KeyError, OSError, ET.ParseError) as exc:
        print(f'UNIT SUITE: FAIL ({type(exc).__name__}: {exc})', file=sys.stderr)
        return 1
    a.output.parent.mkdir(parents=True, exist_ok=True)
    a.output.write_text(json.dumps(report, indent=2, sort_keys=True) + '\n')
    print(json.dumps({k:v for k,v in report.items() if k != 'results'}, indent=2))
    return int(bool(report['errors']))


if __name__ == '__main__':
    sys.exit(main())
