#!/usr/bin/env python3
"""Advisory SessionStart snapshot. Standard library only; no writes or network.

Project files always come from the session checkout, never PLUGIN_ROOT/cache.
Only Git's read operations are allowed, with optional locks, fsmonitor and lazy
object fetching disabled. This is inspection, not mutation-mode preflight.
"""
import csv
import io
import json
import os
from pathlib import Path
import re
import subprocess
import sys
from urllib.parse import urlsplit

EXPECTED_REPOSITORY = 'alrithy/NuvioTV'
FILES = {
    'state': 'integration/state.yaml',
    'handoff': 'docs/HANDOFF.md',
    'traceability': 'integration/feature_traceability.csv',
    'features': 'docs/MASTER_FEATURES.md',
    'sources': 'docs/SOURCE_MAP.md',
    'agents': 'AGENTS.md',
    'recovery': 'docs/RECOVERY.md',
}
TASK_KEYS = ('repository', 'integration_branch', 'active_gate', 'active_branch',
             'active_task_packet', 'active_status', 'active_pr', 'active_owner')


def git(cwd, *args):
    env = dict(os.environ, GIT_OPTIONAL_LOCKS='0', GIT_NO_LAZY_FETCH='1')
    try:
        result = subprocess.run(
            ['git', '-c', 'core.fsmonitor=false', *args], cwd=cwd, env=env,
            capture_output=True, text=True, timeout=3, check=False,
        )
        return result.stdout.rstrip('\n') if result.returncode == 0 else None
    except (OSError, subprocess.TimeoutExpired):
        return None


def repository_id(url):
    """Return credential-free host/path for HTTPS/SSH/scp remotes."""
    if '://' in url:
        parsed = urlsplit(url)
        host, path = parsed.hostname or '', parsed.path
    else:
        match = re.fullmatch(r'(?:[^@/:]+@)?([^/:]+):(.+)', url)
        if not match:
            return 'local-or-unrecognized'
        host, path = match.groups()
    path = path.strip('/').removesuffix('.git')
    # Queries, userinfo and fragments are deliberately never emitted.
    return host.lower() + '/' + path


def task_fields(text):
    """Read only simple top-level scalar task hints, not a YAML authority parser.

    The repository validator/preflight owns full YAML validation. Missing,
    duplicate or complex values are unverified, never inferred by the hook.
    """
    result = {}
    for key in TASK_KEYS:
        values = re.findall(r'^' + key + r':[ \t]*([^\n]*)$', text, re.MULTILINE)
        if len(values) != 1:
            continue
        value = values[0].strip()
        if value.startswith('"'):
            try:
                value = json.loads(value)
            except ValueError:
                continue
        elif value.startswith("'") and value.endswith("'"):
            value = value[1:-1].replace("''", "'")
        elif not value or value[0] in '|>{[&*!' or ' #' in value:
            continue
        result[key] = value
    return result


def read_project_file(root, relative):
    path = root / relative
    # Do not follow project-file symlinks outside the checkout.
    try:
        if not path.resolve().is_relative_to(root):
            return None
        return path.read_text(encoding='utf-8')
    except (OSError, UnicodeError):
        return None


def snapshot(cwd):
    root_value = git(cwd, 'rev-parse', '--show-toplevel')
    if not root_value:
        return {'repository_identity': 'NOT_A_GIT_CHECKOUT',
                'warnings': ['Open an alrithy/NuvioTV working checkout; no recovery was performed.']}
    root = Path(root_value).resolve()
    remotes = []
    for line in (git(root, 'remote', '-v') or '').splitlines():
        parts = line.split()
        if len(parts) == 3:
            remotes.append({'name': parts[0], 'repository': repository_id(parts[1]),
                            'direction': parts[2].strip('()')})
    origin = [r for r in remotes if r['name'] == 'origin']
    identity = bool(origin) and all(
        r['repository'].lower() == ('github.com/' + EXPECTED_REPOSITORY).lower()
        for r in origin
    )
    status = git(root, 'status', '--porcelain=v1', '--untracked-files=all', '--no-renames')
    result = {
        'repository_root': str(root),
        'repository_identity': 'MATCH' if identity else 'MISMATCH_OR_UNKNOWN',
        'expected_repository': EXPECTED_REPOSITORY,
        'branch': git(root, 'branch', '--show-current') or 'DETACHED_OR_UNKNOWN',
        'head': git(root, 'rev-parse', '--verify', 'HEAD'),
        'git_status': status,
        'dirty_tree': bool(status) if status is not None else None,
        'remotes': remotes,
        'remote_freshness': 'CACHED_ONLY — no fetch/network in SessionStart',
        'warnings': [],
    }
    if not identity:
        result['warnings'].append('Repository identity mismatch; do not apply Nuvio mutations.')
        return result
    texts = {key: read_project_file(root, path) for key, path in FILES.items()}
    result['files'] = {key: {'path': FILES[key], 'available': value is not None}
                       for key, value in texts.items()}
    local = task_fields(texts['state'] or '')
    # The bootstrap integration ref is a locator, not a cached project task/pin.
    ref = 'refs/remotes/origin/superfork/integration'
    remote_text = git(root, 'show', ref + ':' + FILES['state'])
    remote = task_fields(remote_text or '')
    result['local_task_hints'] = local
    result['cached_integration_task_hints'] = remote
    result['cached_integration_head'] = git(root, 'rev-parse', '--verify', ref)
    result['cached_integration_vs_head_counts'] = git(
        root, 'rev-list', '--left-right', '--count', ref + '...HEAD') if remote else None
    result['task_hint_conflicts'] = [key for key in TASK_KEYS
                                     if local.get(key) != remote.get(key)] if remote else []
    current = remote or local
    result['current_task'] = {
        'packet': current.get('active_task_packet'),
        'branch': current.get('active_branch'), 'gate': current.get('active_gate'),
        'verified_live': False,
    }
    packet = current.get('active_task_packet')
    result['current_task']['packet_available'] = bool(
        packet and read_project_file(root, packet) is not None)
    handoff = texts['handoff']
    if handoff is not None:
        # Emit current owned header and exact-next-action, not an entire history.
        header = re.search(r'<!-- canonical-state:start -->(.*?)<!-- canonical-state:end -->',
                           handoff, re.DOTALL)
        next_action = re.search(r'^## Exact next action\s*\n(.*?)(?=^## |\Z)',
                                handoff, re.MULTILINE | re.DOTALL)
        result['handoff_state'] = {
            'path': FILES['handoff'],
            'canonical_header': header.group(1).strip() if header else None,
            'next_action': next_action.group(1).strip() if next_action else None,
        }
    else:
        result['handoff_state'] = {'path': FILES['handoff'], 'available': False}
    trace = texts['traceability']
    result['traceability'] = {'available': trace is not None, 'active_gate_rows': []}
    if trace is not None:
        reader = csv.DictReader(io.StringIO(trace))
        required = {'feature_id', 'gate', 'status', 'evidence', 'next_action'}
        result['traceability']['columns_valid'] = required <= set(reader.fieldnames or [])
        if result['traceability']['columns_valid']:
            result['traceability']['active_gate_rows'] = [
                row for row in reader if row.get('gate') == current.get('active_gate')
            ]
    missing = [FILES[key] for key, value in texts.items() if value is None]
    if missing:
        result['warnings'].append('Missing repository files: ' + ', '.join(missing))
    if result['dirty_tree']:
        result['warnings'].append('Inspect/preserve dirty work using RECOVERY and its canonical target before mutation.')
    if result['branch'] != current.get('active_branch'):
        result['warnings'].append('Checkout is not the cached active task branch; read live state before recovery.')
    if not remote or result['task_hint_conflicts']:
        result['warnings'].append('Cached integration state missing/conflicting; verify live remote state.')
    return result


def main():
    try:
        event = json.load(sys.stdin)
        cwd = event.get('cwd') or os.getcwd()
        state = snapshot(cwd)
    except (ValueError, TypeError, AttributeError, OSError) as exc:
        state = {'warnings': ['Snapshot unavailable: ' + type(exc).__name__]}
    context = (
        'Nuvio Superfork read-only snapshot (advisory; no files/refs changed). '
        'Repository evidence below may be stale; verify live GitHub before decisions. '
        'For اشتغل على نوفيو use nuvio-orchestrator; for شيك لي use nuvio-status. '
        'Read working-repo AGENTS, state, HANDOFF (AGENT_HANDOFF), affected '
        'feature_traceability/MASTER_FEATURES and SOURCE_MAP this session. '
        'Do not use installed-plugin copies as project state.\n' +
        json.dumps(state, ensure_ascii=False, indent=2)
    )
    print(json.dumps({'hookSpecificOutput': {
        'hookEventName': 'SessionStart', 'additionalContext': context,
    }}, ensure_ascii=False))
    return 0


if __name__ == '__main__':
    sys.exit(main())
