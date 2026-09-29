#!/usr/bin/env python3
"""Only supported full-suite entrypoint; never turns infrastructure errors green."""
import argparse
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
from check_baseline_test_failures import read_results, evaluate

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--project', type=Path, default=ROOT)
    p.add_argument('--output', type=Path, default=ROOT / 'build/superfork/test-report.json')
    p.add_argument('--inventory', type=Path)
    a = p.parse_args()
    project = a.project.resolve()
    results = project / 'app/build/test-results/testFullDebugUnitTest'
    # This exact generated directory is disposable. Old XML must never satisfy a new run.
    if results.exists():
        shutil.rmtree(results)
    a.output.parent.mkdir(parents=True, exist_ok=True)
    debt = json.loads((ROOT / 'integration/baseline_test_debt.json').read_text())
    inventory_path = a.inventory or ROOT / 'integration/evidence/baseline-suite.json'
    inventory = json.loads(inventory_path.read_text()) if inventory_path.exists() else None
    with tempfile.TemporaryDirectory(prefix='superfork-suite-') as tmp:
        completion = Path(tmp) / 'completion.json'
        env = dict(os.environ, SUPERFORK_TEST_COMPLETION=str(completion))
        # No raw test output, signed URL, authorization header, or stack dump is
        # published. The artifact includes only fully qualified IDs and outcomes.
        with (Path(tmp) / 'gradle.log').open('w+') as log:
            code = subprocess.run(['./gradlew', ':app:testFullDebugUnitTest',
                                   '--init-script', str(HERE / 'test-report.init.gradle'),
                                   '--no-build-cache', '--console=plain'], cwd=project,
                                  env=env, stdout=log, stderr=subprocess.STDOUT).returncode
            try:
                receipt = json.loads(completion.read_text())
                report = evaluate(read_results(results), debt, receipt, code, inventory)
            except (ValueError, KeyError, OSError) as exc:
                report = {'errors': [f'Incomplete test execution: {type(exc).__name__}'],
                          'gradle_exit': code}
                # Print only Gradle task/error category lines without free-form values.
                log.seek(0)
                for line in log:
                    if line.startswith('> Task ') and ' FAILED' in line:
                        print(line.split(' FAILED')[0] + ' FAILED')
        report['tested_sha'] = subprocess.check_output(['git','rev-parse','HEAD'], cwd=project, text=True).strip()
        report['ci_run'] = os.getenv('GITHUB_RUN_ID', 'local')
        report['gradle_exit'] = code
        a.output.write_text(json.dumps(report, indent=2, sort_keys=True) + '\n')
        print(json.dumps({k:v for k,v in report.items() if k != 'results'}, indent=2))
        return int(bool(report['errors']) or code != 0)


if __name__ == '__main__':
    sys.exit(main())
