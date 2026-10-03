import copy
import csv
import io
from pathlib import Path
import sys
import tempfile
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from check_pr_scope import evaluate_scope
from user_authorized_task import TASK_BRANCH, TASK_FEATURE_IDS, TASK_PACKET


class PrScopeTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        (self.root / 'tasks').mkdir()
        (self.root / 'integration').mkdir()
        self.packet = self.root / TASK_PACKET
        self.packet.write_text(
            '# Netflix task\n'
            'Task ID: NETFLIX_THEME\n'
            'Branch: feat/netflix-theme\n'
            'Authorization: explicit-user-request\n'
            'Feature ID(s): ' + ', '.join(map(str, TASK_FEATURE_IDS)) + '\n'
        )
        self.base = {'active_gate': 'G14', 'active_branch': 'chore/release-hardening',
                     'active_status': 'BLOCKED', 'blocker': 'Hardware certification pending'}
        self.head = copy.deepcopy(self.base)
        self.head['user_authorized_task'] = {
            'id': 'NETFLIX_THEME', 'branch': TASK_BRANCH, 'task_packet': TASK_PACKET,
            'authorization': 'explicit-user-request', 'status': 'IN_PROGRESS',
            'feature_ids': TASK_FEATURE_IDS.copy(),
        }
        self.base_rows = [
            {'feature_id': str(fid), 'status': 'implemented', 'evidence': 'Prior evidence.'}
            for fid in range(1, 321)
        ]
        self.head_rows = copy.deepcopy(self.base_rows)
        for row in self.head_rows:
            if int(row['feature_id']) in TASK_FEATURE_IDS:
                row['evidence'] += ' Explicit user theme scope: ' + TASK_PACKET
        self.base_traceability = self.csv_text(self.base_rows)
        self.traceability_path = self.root / 'integration/feature_traceability.csv'
        self.write_traceability()
        self.changed = {'app/src/main/java/com/nuvio/tv/ui/theme/Theme.kt',
                        'docs/HANDOFF.md', 'docs/PROJECT_STATUS.md', 'integration/state.yaml',
                        'integration/feature_traceability.csv', TASK_PACKET}

    def csv_text(self, rows):
        text = io.StringIO()
        writer = csv.DictWriter(text, fieldnames=['feature_id', 'status', 'evidence'])
        writer.writeheader()
        writer.writerows(rows)
        return text.getvalue()

    def write_traceability(self):
        self.traceability_path.write_text(self.csv_text(self.head_rows))

    def evaluate(self, **overrides):
        return evaluate_scope(
            self.base, overrides.get('head', self.head), overrides.get('changed', self.changed),
            overrides.get('branch', TASK_BRANCH), self.root,
            overrides.get('base_traceability', self.base_traceability),
        )

    def test_exact_explicit_task_preserves_blocked_gate(self):
        self.assertEqual(self.evaluate(), [])
        self.assertEqual(self.head['active_status'], 'BLOCKED')

    def test_review_status_does_not_claim_completion(self):
        self.head['user_authorized_task']['status'] = 'REVIEW'
        self.assertEqual(self.evaluate(), [])
        self.head['user_authorized_task']['status'] = 'DONE'
        self.assertTrue(self.evaluate())

    def test_record_required_even_for_exact_branch(self):
        self.head.pop('user_authorized_task')
        self.assertTrue(self.evaluate())

    def test_record_cannot_authorize_another_branch_or_packet(self):
        for key, value in [('id', 'OTHER_TASK'), ('branch', 'feat/other-theme'),
                           ('task_packet', 'tasks/OTHER.md'), ('authorization', 'assumed')]:
            with self.subTest(key=key):
                head = copy.deepcopy(self.head)
                head['user_authorized_task'][key] = value
                self.assertTrue(self.evaluate(head=head))

    def test_branch_prefix_is_not_a_general_exception(self):
        for branch in ['feat/netflix-theme-other', 'feat/other-theme', 'feat/custom-theme']:
            with self.subTest(branch=branch):
                self.assertIn('PR branch differs from base integration active branch',
                              self.evaluate(branch=branch))

    def test_task_record_rejects_undeclared_fields_and_scope(self):
        self.head['user_authorized_task']['extra_branch'] = 'feat/other'
        self.assertTrue(self.evaluate())
        del self.head['user_authorized_task']['extra_branch']
        self.head['user_authorized_task']['feature_ids'].append(320)
        self.assertTrue(self.evaluate())

    def test_duplicate_feature_declaration_rejected(self):
        self.head['user_authorized_task']['feature_ids'].append(188)
        self.assertTrue(self.evaluate())

    def test_packet_missing_or_metadata_mismatch_rejected(self):
        self.packet.write_text(self.packet.read_text().replace('feat/netflix-theme', 'feat/other'))
        self.assertTrue(self.evaluate())
        self.packet.unlink()
        self.assertTrue(self.evaluate())

    def test_duplicate_authorization_in_packet_rejected(self):
        self.packet.write_text(self.packet.read_text() + 'Authorization: explicit-user-request\n')
        self.assertTrue(self.evaluate())

    def test_canonical_gate_state_cannot_be_changed(self):
        for key, value in [('active_status', 'DONE'), ('active_branch', TASK_BRANCH),
                           ('blocker', ''), ('new_gate_field', 'claimed')]:
            with self.subTest(key=key):
                head = copy.deepcopy(self.head)
                head[key] = value
                self.assertIn('User-authorized task cannot change canonical gate state',
                              self.evaluate(head=head))

    def test_all_handoff_and_packet_changes_still_required(self):
        for path in self.changed - {'app/src/main/java/com/nuvio/tv/ui/theme/Theme.kt'}:
            with self.subTest(path=path):
                errors = self.evaluate(changed=self.changed - {path})
                self.assertTrue(any('Missing required handoff/audit updates:' in e for e in errors))

    def test_base_traceability_cannot_be_assumed(self):
        self.assertTrue(self.evaluate(base_traceability=None))

    def test_every_affected_feature_requires_added_packet_evidence(self):
        self.head_rows[187]['evidence'] = 'Prior evidence.'
        self.write_traceability()
        self.assertTrue(self.evaluate())

    def test_prior_feature_evidence_cannot_be_replaced(self):
        self.head_rows[187]['evidence'] = TASK_PACKET
        self.write_traceability()
        self.assertTrue(self.evaluate())

    def test_feature_status_cannot_be_promoted(self):
        self.head_rows[187]['status'] = 'verified_official'
        self.write_traceability()
        self.assertTrue(self.evaluate())

    def test_unrelated_feature_cannot_be_changed(self):
        self.head_rows[319]['evidence'] += ' Claimed done.'
        self.write_traceability()
        self.assertTrue(self.evaluate())

    def test_removed_feature_is_an_error_not_inferred_scope(self):
        self.head_rows.pop()
        self.write_traceability()
        with self.assertRaises(ValueError):
            self.evaluate()

    def test_ordinary_active_branch_policy_remains(self):
        self.assertEqual(self.evaluate(branch=self.base['active_branch']), [])
        self.assertTrue(self.evaluate(branch=self.base['active_branch'],
                                      changed={'app/src/main/java/Runtime.kt'}))

    def test_governance_exception_still_cannot_change_runtime(self):
        self.assertIn('Governance exception cannot change runtime/build code',
                      self.evaluate(branch='chore/governance-example'))
        self.assertEqual(self.evaluate(branch='chore/governance-example',
                                       changed={'docs/DECISIONS.md'}), [])

    def test_sync_still_requires_its_baseline_audit(self):
        self.assertTrue(self.evaluate(branch='chore/upstream-sync-example'))
        changed = self.changed | {'docs/UPSTREAM_SYNC_LOG.md', 'docs/SOURCE_MAP.md',
                                  'docs/BASELINE.md', 'integration/baseline_test_debt.json',
                                  'docs/DECISIONS.md'}
        self.assertEqual(self.evaluate(branch='chore/upstream-sync-example', changed=changed), [])


if __name__ == '__main__':
    unittest.main()
