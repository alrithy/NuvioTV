"""Package validation and real-Git read-only SessionStart regression cases."""
import hashlib
import importlib.util
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[3]
sys.path.insert(0, str(ROOT / 'scripts/superfork'))
from check_pr_scope import is_governance_path
from validate_plugin import validate

spec = importlib.util.spec_from_file_location('nuvio_session_start', ROOT / 'hooks/session_start.py')
hook = importlib.util.module_from_spec(spec)
spec.loader.exec_module(hook)


class PluginTests(unittest.TestCase):
    def test_package_contract(self):
        self.assertEqual(validate(ROOT), [])

    def test_plugin_governance_does_not_exempt_application_code(self):
        for path in ('plugin.json', 'hooks/session_start.py', 'skills/nuvio-status/SKILL.md'):
            self.assertTrue(is_governance_path(path))
        for path in ('app/src/main/Player.kt', 'build.gradle.kts', 'hooks/other.py',
                     'skills/unrelated/SKILL.md'):
            self.assertFalse(is_governance_path(path))


class SessionStartTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name).resolve()
        self.git('init', '-q')
        self.git('config', 'user.name', 'Plugin Test')
        self.git('config', 'user.email', 'test@example.invalid')
        self.git('remote', 'add', 'origin', 'https://github.com/alrithy/NuvioTV.git')
        for path in hook.FILES.values():
            target = self.root / path
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_text('fixture\n')
        (self.root / hook.FILES['state']).write_text(
            'repository: alrithy/NuvioTV\nintegration_branch: superfork/integration\n'
            'active_gate: G9\nactive_branch: feat/example\n'
            'active_task_packet: tasks/G9.md\nactive_status: IN_PROGRESS\n')
        (self.root / hook.FILES['handoff']).write_text(
            '# Handoff\n<!-- canonical-state:start -->\nActive gate: G9\n'
            '<!-- canonical-state:end -->\n## Exact next action\nContinue real task.\n'
            '## Evidence\nDo not include old history.\n')
        (self.root / hook.FILES['traceability']).write_text(
            'feature_id,gate,status,evidence,next_action\n'
            '1,G9,in_progress,real evidence,continue\n2,G8,planned,,later\n')
        self.git('add', '.')
        self.git('commit', '-qm', 'fixture')
        self.git('branch', '-M', 'feat/example')
        self.git('update-ref', 'refs/remotes/origin/superfork/integration', 'HEAD')

    def tearDown(self):
        self.temp.cleanup()

    def git(self, *args):
        return subprocess.check_output(['git', *args], cwd=self.root, text=True).strip()

    def files(self):
        return {str(p.relative_to(self.root)): hashlib.sha256(p.read_bytes()).hexdigest()
                for p in self.root.rglob('*') if p.is_file()}

    def test_clean_snapshot_uses_working_checkout_not_plugin_cache(self):
        result = hook.snapshot(self.root)
        self.assertEqual(result['repository_identity'], 'MATCH')
        self.assertFalse(result['dirty_tree'])
        self.assertEqual(result['current_task']['packet'], 'tasks/G9.md')
        self.assertEqual(result['handoff_state']['next_action'], 'Continue real task.')
        self.assertEqual(len(result['traceability']['active_gate_rows']), 1)
        self.assertFalse(result['current_task']['verified_live'])

    def test_dirty_staged_untracked_and_git_metadata_are_unchanged(self):
        (self.root / 'AGENTS.md').write_text('staged unknown work\n')
        self.git('add', 'AGENTS.md')
        (self.root / 'unknown.txt').write_text('preserve this\n')
        before = self.files()
        result = hook.snapshot(self.root)
        self.assertTrue(result['dirty_tree'])
        self.assertIn('unknown.txt', result['git_status'])
        self.assertEqual(before, self.files())

    def test_old_wrong_branch_is_reported_without_switching(self):
        self.git('switch', '-qc', 'dev')
        before = self.files()
        result = hook.snapshot(self.root)
        self.assertEqual(result['branch'], 'dev')
        self.assertTrue(any('not the cached active' in w for w in result['warnings']))
        self.assertEqual(before, self.files())

    def test_detached_head_is_reported(self):
        self.git('checkout', '-q', '--detach')
        self.assertEqual(hook.snapshot(self.root)['branch'], 'DETACHED_OR_UNKNOWN')

    def test_wrong_repository_does_not_read_nuvio_task(self):
        self.git('remote', 'set-url', 'origin', 'https://github.com/example/other.git')
        result = hook.snapshot(self.root)
        self.assertEqual(result['repository_identity'], 'MISMATCH_OR_UNKNOWN')
        self.assertNotIn('current_task', result)

    def test_remote_credentials_are_not_emitted(self):
        self.git('remote', 'set-url', 'origin',
                 'https://user:secret-value@github.com/alrithy/NuvioTV.git?token=secret-value')
        result = hook.snapshot(self.root)
        self.assertNotIn('secret-value', json.dumps(result))
        self.assertEqual(result['repository_identity'], 'MATCH')
        self.assertEqual(hook.repository_id('git@github.com:alrithy/NuvioTV.git'),
                         'github.com/alrithy/NuvioTV')

    def test_missing_traceability_and_handoff_are_explicit(self):
        (self.root / hook.FILES['traceability']).unlink()
        (self.root / hook.FILES['handoff']).unlink()
        result = hook.snapshot(self.root)
        self.assertFalse(result['traceability']['available'])
        self.assertFalse(result['handoff_state']['available'])

    def test_conflicting_cached_task_is_not_claimed_live(self):
        with (self.root / hook.FILES['state']).open('a') as output:
            output.write('active_pr: 99\n')
        result = hook.snapshot(self.root)
        self.assertIn('active_pr', result['task_hint_conflicts'])
        self.assertFalse(result['current_task']['verified_live'])

    def test_files_outside_checkout_are_not_read(self):
        target = self.root / hook.FILES['handoff']
        target.unlink()
        target.symlink_to(ROOT / 'docs/HANDOFF.md')
        self.assertFalse(hook.snapshot(self.root)['handoff_state']['available'])

    def test_subdirectory_and_wire_contract_preserve_all_files(self):
        before = self.files()
        result = subprocess.run(
            [sys.executable, '-B', str(ROOT / 'hooks/session_start.py')],
            input=json.dumps({'cwd': str(self.root / 'docs'), 'hook_event_name': 'SessionStart'}),
            text=True, capture_output=True, check=True)
        output = json.loads(result.stdout)['hookSpecificOutput']
        self.assertEqual(output['hookEventName'], 'SessionStart')
        self.assertIn('tasks/G9.md', output['additionalContext'])
        self.assertEqual(before, self.files())

    def test_outside_git_and_bad_input_are_advisory(self):
        with tempfile.TemporaryDirectory() as empty:
            self.assertEqual(hook.snapshot(empty)['repository_identity'], 'NOT_A_GIT_CHECKOUT')
        result = subprocess.run([sys.executable, '-B', str(ROOT / 'hooks/session_start.py')],
                                input='invalid JSON', capture_output=True, text=True, check=True)
        self.assertIn('Snapshot unavailable', json.loads(result.stdout)
                      ['hookSpecificOutput']['additionalContext'])


if __name__ == '__main__':
    unittest.main()
