import copy
import json
from pathlib import Path
import shutil
import sys
import tempfile
import unittest
import xml.etree.ElementTree as ET
sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
from check_baseline_test_failures import evaluate,read_results
from validate_project_state import validate
ROOT=Path(__file__).resolve().parents[3]
A='example.one.SameTest#known'
B='example.two.OtherTest#passing'

class FailureGuardTests(unittest.TestCase):
    def setUp(self):
        self.debt={'minimum_test_count':2,'failures':[{'test_id':A}]}
        self.results={A:'failure',B:'passed'}
        self.receipt={'completed':True,'tests':2,'failures':1,'skipped':0}
        self.inventory={'results':self.results.copy()}
    def run_guard(self,**kwargs):
        return evaluate(kwargs.get('results',self.results),kwargs.get('debt',self.debt),
                        kwargs.get('receipt',self.receipt),kwargs.get('exit',0),self.inventory)
    def test_known_failure_remains_visible(self):
        result=self.run_guard();self.assertFalse(result['errors']);self.assertEqual(result['known_remaining'],[A])
    def test_new_failure_blocks(self):
        result=self.run_guard(results={A:'failure',B:'failure'},receipt=dict(self.receipt,failures=2))
        self.assertTrue(result['errors']);self.assertEqual(result['new_failures'],[B])
    def test_same_simple_name_other_package_is_new(self):
        c='unrelated.SameTest#known';r=self.run_guard(results={A:'failure',B:'passed',c:'failure'},receipt=dict(self.receipt,tests=3,failures=2))
        self.assertIn(c,r['new_failures'])
    def test_infrastructure_exit_cannot_hide_behind_known_failure(self):self.assertTrue(self.run_guard(exit=1)['errors'])
    def test_missing_completion_blocks(self):self.assertTrue(self.run_guard(receipt={})['errors'])
    def test_missing_test_blocks_even_with_matching_partial_receipt(self):
        self.assertTrue(self.run_guard(results={A:'failure'},receipt=dict(self.receipt,tests=1))['errors'])
    def test_known_skip_is_not_fix(self):
        r=self.run_guard(results={A:'skipped',B:'passed'},receipt=dict(self.receipt,failures=0,skipped=1))
        self.assertTrue(r['errors']);self.assertEqual(r['resolved_candidates'],[])
    def test_new_skip_blocks(self):
        self.assertTrue(self.run_guard(results={A:'failure',B:'skipped'},receipt=dict(self.receipt,skipped=1))['errors'])
    def test_passing_known_case_is_candidate_not_absence(self):
        r=self.run_guard(results={A:'passed',B:'passed'},receipt=dict(self.receipt,failures=0))
        self.assertFalse(r['errors']);self.assertEqual(r['resolved_candidates'],[A])
    def test_debt_can_shrink_without_fixed_count(self):
        r=self.run_guard(debt={'minimum_test_count':2,'failures':[]},results={A:'passed',B:'passed'},receipt=dict(self.receipt,failures=0))
        self.assertFalse(r['errors'])
    def test_removed_debt_failing_again_blocks(self):self.assertTrue(self.run_guard(debt={'minimum_test_count':2,'failures':[]})['errors'])
    def test_duplicate_debt_blocks(self):
        d=copy.deepcopy(self.debt);d['failures']*=2;self.assertTrue(self.run_guard(debt=d)['errors'])
    def test_empty_reports_block(self):
        with tempfile.TemporaryDirectory() as t:
            with self.assertRaises(ValueError):read_results(t)
    def test_malformed_xml_blocks(self):
        with tempfile.TemporaryDirectory() as t:
            Path(t,'TEST-x.xml').write_text('<testsuite>')
            with self.assertRaises(ET.ParseError):read_results(t)
    def test_xml_count_mismatch_blocks(self):
        with tempfile.TemporaryDirectory() as t:
            Path(t,'TEST-x.xml').write_text('<testsuite tests="2"><testcase classname="a.b.Test" name="x"/></testsuite>')
            with self.assertRaises(ValueError):read_results(t)
    def test_duplicate_xml_identifier_blocks(self):
        with tempfile.TemporaryDirectory() as t:
            Path(t,'TEST-x.xml').write_text('<testsuite tests="2"><testcase classname="a.b.Test" name="x"/><testcase classname="a.b.Test" name="x"/></testsuite>')
            with self.assertRaises(ValueError):read_results(t)

class GovernanceMutationTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.temp=tempfile.TemporaryDirectory();cls.root=Path(cls.temp.name)
        for name in ['docs','integration','tasks','scripts','.github']:
            shutil.copytree(ROOT/name,cls.root/name,ignore=shutil.ignore_patterns('__pycache__'))
        for name in ['AGENTS.md','CLAUDE.md','CODEX_START.md','SUPERFORK.md','CONTRIBUTING_SUPERFORK.md']:
            shutil.copy(ROOT/name,cls.root/name)
        (cls.root/'app').symlink_to(ROOT/'app',target_is_directory=True)
    @classmethod
    def tearDownClass(cls):cls.temp.cleanup()
    def mutate(self,path,change):
        p=self.root/path;old=p.read_text()
        try:p.write_text(change(old));return validate(self.root)
        finally:p.write_text(old)
    def test_valid_repository(self):self.assertEqual(validate(self.root),[])
    def test_duplicate_master_id_is_not_hidden_by_dictionary(self):
        self.assertTrue(self.mutate('docs/MASTER_FEATURES.md',lambda s:s+'\n1. Duplicate\n'))
    def test_missing_feature_rejected(self):
        self.assertTrue(self.mutate('integration/feature_traceability.csv',lambda s:'\n'.join(s.splitlines()[:-1])+'\n'))
    def test_unknown_source_rejected(self):
        self.assertTrue(self.mutate('integration/feature_traceability.csv',lambda s:s.replace('foundation,project','unknown,project',1)))
    def test_empty_owner_rejected(self):
        self.assertTrue(self.mutate('integration/feature_traceability.csv',lambda s:s.replace('foundation,project','foundation,',1)))
    def test_blocked_requires_evidence(self):
        # An implemented row has evidence but no reason / next action; marking it blocked must fail.
        self.assertTrue(self.mutate('integration/feature_traceability.csv',lambda s:s.replace(',implemented,',',blocked,',1)))
    def test_duplicate_yaml_key_rejected(self):
        with self.assertRaises(ValueError):self.mutate('integration/state.yaml',lambda s:s+'\nactive_gate: G9\n')
    def test_stale_state_view_rejected(self):
        self.assertTrue(self.mutate('docs/HANDOFF.md',lambda s:s.replace('Active branch:','Old branch:')))
    def test_competing_redirect_rejected(self):
        self.assertTrue(self.mutate('docs/RUNBOOK.md',lambda s:s+'\nIndependent policy\n'*20))
    def test_debt_reduction_valid(self):
        def shrink(s):d=json.loads(s);d['failures'].pop();return json.dumps(d)
        self.assertEqual(self.mutate('integration/baseline_test_debt.json',shrink),[])

if __name__=='__main__':unittest.main()
