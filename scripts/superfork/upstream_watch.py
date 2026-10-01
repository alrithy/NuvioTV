#!/usr/bin/env python3
"""G14c (D064; feature 316): report official `dev` movement since the accepted baseline.

Read-only. Prints a Markdown report (for the job summary): the commits official made after
`current_official_baseline_sha`, and which of the files they touch the fork also changed
(the seams where the next upstream sync can conflict). With --annotate it adds one GitHub
`::notice` / `::warning` line. It never fetches secrets, pushes or merges; the sync stays a
reviewed PR (UPSTREAM_SYNC).
"""
import argparse
from pathlib import Path
import subprocess
import sys

import yaml

ROOT = Path(__file__).resolve().parents[2]
# Governance-only paths are never a code seam.
IGNORED_PREFIXES = ('docs/', 'integration/', 'tasks/', 'scripts/superfork/', '.github/', '.claude/')


def git(*args):
    return subprocess.run(['git', *args], cwd=ROOT, check=True, capture_output=True, text=True).stdout


def summarize(commits, upstream_files, fork_files, baseline, upstream_sha):
    """Pure report: commits is [(short_sha, subject)], the file arguments are path sets."""
    seams = sorted(f for f in set(upstream_files) & set(fork_files) if not f.startswith(IGNORED_PREFIXES))
    lines = ['## Official upstream watch', '',
             f'- Accepted baseline: `{baseline}`',
             f'- Official `dev`: `{upstream_sha}`',
             f'- Commits since the baseline: {len(commits)}',
             f'- Files official changed: {len(set(upstream_files))}; also changed by the fork: {len(seams)}']
    if commits:
        lines += ['', '### Commits', ''] + [f'- `{sha}` {subject}' for sha, subject in commits]
    if seams:
        lines += ['', '### Fork seams touched (review before the next sync)', ''] + [f'- `{f}`' for f in seams]
    if not commits:
        level, message = 'notice', 'Official dev has no commits after the accepted baseline.'
    elif seams:
        level, message = 'warning', (f'Official dev is {len(commits)} commit(s) ahead and touches {len(seams)} '
                                     'fork seam(s); plan an upstream sync PR (UPSTREAM_SYNC).')
    else:
        level, message = 'notice', f'Official dev is {len(commits)} commit(s) ahead; no fork seam touched.'
    return {'markdown': '\n'.join(lines) + '\n', 'level': level, 'message': message,
            'commits': len(commits), 'seams': seams}


def main(argv=None):
    p = argparse.ArgumentParser()
    p.add_argument('--upstream', default='official/dev', help='ref of official dev, already fetched')
    p.add_argument('--head', default='HEAD', help='fork integration ref')
    p.add_argument('--annotate', action='store_true', help='also print one GitHub annotation line')
    a = p.parse_args(argv)
    state = yaml.safe_load((ROOT / 'integration/state.yaml').read_text())
    baseline = state['current_official_baseline_sha']
    upstream_sha = git('rev-parse', a.upstream).strip()
    commits = [tuple(line.split(' ', 1)) if ' ' in line else (line, '')
               for line in git('log', '--no-merges', '--format=%h %s', f'{baseline}..{upstream_sha}').splitlines()]
    upstream_files = git('diff', '--name-only', baseline, upstream_sha).split()
    fork_files = git('diff', '--name-only', baseline, a.head).split()
    report = summarize(commits, upstream_files, fork_files, baseline, upstream_sha)
    sys.stdout.write(report['markdown'])
    if a.annotate:
        print(f"::{report['level']} title=Official upstream::{report['message']}", file=sys.stderr)
    return 0


if __name__ == '__main__':
    sys.exit(main())
