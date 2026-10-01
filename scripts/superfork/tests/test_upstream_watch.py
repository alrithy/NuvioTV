"""G14c (316): the upstream watch reports commits and fork seams, and governance paths are no seam."""
from pathlib import Path
import sys
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import upstream_watch


class UpstreamWatchTests(unittest.TestCase):
    def test_no_movement_is_a_notice(self):
        r = upstream_watch.summarize([], [], ['app/x.kt'], 'base', 'base')
        self.assertEqual(('notice', 0, []), (r['level'], r['commits'], r['seams']))
        self.assertIn('no commits after the accepted baseline', r['message'])

    def test_overlap_with_fork_changes_is_a_warning(self):
        r = upstream_watch.summarize(
            [('abc1234', 'fix: player'), ('def5678', 'strings')],
            ['app/src/main/java/p/Player.kt', 'app/src/main/res/values/strings.xml', 'docs/README.md'],
            ['app/src/main/java/p/Player.kt', 'docs/README.md', 'app/src/main/java/com/nuvio/tv/fork/a.kt'],
            'base', 'up')
        self.assertEqual('warning', r['level'])
        self.assertEqual(['app/src/main/java/p/Player.kt'], r['seams'])
        self.assertIn('- `abc1234` fix: player', r['markdown'])
        self.assertIn('Fork seams touched', r['markdown'])
        self.assertNotIn('`docs/README.md`', r['markdown'])

    def test_movement_without_overlap_is_a_notice(self):
        r = upstream_watch.summarize([('abc1234', 'x')], ['app/a.kt'], ['app/b.kt'], 'base', 'up')
        self.assertEqual(('notice', []), (r['level'], r['seams']))
        self.assertIn('1 commit(s) ahead; no fork seam touched', r['message'])


if __name__ == '__main__':
    unittest.main()
