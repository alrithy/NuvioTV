"""Exercise real local Git refs: writer races and fresh-agent branch recovery."""
import contextlib
import io
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch
import yaml
sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
import preflight

class GitSafetyTests(unittest.TestCase):
    def setUp(self):
        self.tmp=tempfile.TemporaryDirectory();self.root=Path(self.tmp.name);self.repo=self.root/'work';self.repo.mkdir();self.remote=self.root/'remote.git'
        subprocess.run(['git','init','--bare','-q',str(self.remote)],check=True)
        self.git('init','-q');self.git('config','user.email','test@example.invalid');self.git('config','user.name','Safety Test')
        (self.repo/'README').write_text('fixture\n');self.git('add','.');self.git('commit','-qm','anchor');base=self.git('rev-parse','HEAD').stdout.strip()
        state={'active_gate':'G0','active_branch':'chore/fork-foundation-impl','active_task_packet':'tasks/G0.md','current_official_baseline_sha':base,'integration_branch':'superfork/integration','governance_status':'READY','active_status':'READY'}
        (self.repo/'integration').mkdir();(self.repo/'integration/state.yaml').write_text(yaml.safe_dump(state))
        self.git('add','.');self.git('commit','-qm','state');self.git('branch','-M','superfork/integration');self.git('remote','add','origin',str(self.remote));self.git('push','-q','-u','origin','superfork/integration');self.git('switch','-qc',state['active_branch'])
    def tearDown(self):self.tmp.cleanup()
    def git(self,*args,check=True,input=None):
        return subprocess.run(['git',*args],cwd=self.repo,text=True,capture_output=True,check=check,input=input)
    def preflight_result(self):
        with patch.object(preflight,'ROOT',self.repo),patch.object(sys,'argv',['preflight.py']),contextlib.redirect_stdout(io.StringIO()):return preflight.main()
    def test_fresh_agent_correct_clean_branch_passes(self):self.assertEqual(self.preflight_result(),0)
    def test_dirty_tree_blocks_without_discard(self):
        f=self.repo/'unknown.txt';f.write_text('preserve me');self.assertEqual(self.preflight_result(),1);self.assertEqual(f.read_text(),'preserve me')
    def test_stale_branch_is_detected(self):
        self.git('switch','-q','superfork/integration');(self.repo/'README').write_text('new integration\n');self.git('commit','-qam','integration moved');self.git('push','-q','origin','superfork/integration');self.git('switch','-q','chore/fork-foundation-impl');self.assertEqual(self.preflight_result(),1)
    def test_wrong_branch_is_detected(self):
        self.git('switch','-q','superfork/integration');self.assertEqual(self.preflight_result(),1)
    def test_competing_writers_and_wrong_release_are_rejected(self):
        one=self.git('commit-tree','HEAD^{tree}','-p','HEAD',input='writer one unique\n').stdout.strip()
        two=self.git('commit-tree','HEAD^{tree}','-p','HEAD',input='writer two unique\n').stdout.strip()
        ref='refs/heads/agent-locks/chore/fork-foundation-impl'
        self.git('push','-q','origin',one+':'+ref)
        self.assertNotEqual(self.git('push','-q','origin',two+':'+ref,check=False).returncode,0)
        self.assertNotEqual(self.git('push','-q','--force-with-lease='+ref+':'+two,'origin',':'+ref,check=False).returncode,0)
        self.git('push','-q','--force-with-lease='+ref+':'+one,'origin',':'+ref)
        self.assertEqual(self.git('ls-remote','origin',ref).stdout,'')

if __name__=='__main__':unittest.main()
