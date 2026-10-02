"""Require Android runner evidence, since adb's success exit alone is insufficient."""
from pathlib import Path
import subprocess
import sys
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from capture_netflix_theme import instrumentation_succeeded


class NetflixCaptureReceiptTests(unittest.TestCase):
    def receipt(self, output, exit_code=0):
        return subprocess.CompletedProcess(["adb"], exit_code, output)

    def test_complete_runner_receipt_passes(self):
        self.assertTrue(instrumentation_succeeded(self.receipt(
            "Time: 2.41\n\nOK (19 tests)\n\nINSTRUMENTATION_CODE: -1\n")))
        self.assertTrue(instrumentation_succeeded(self.receipt(
            "OK (1 test)\nINSTRUMENTATION_CODE: -1\n")))

    def test_zero_exit_crash_is_not_success(self):
        self.assertFalse(instrumentation_succeeded(self.receipt(
            "INSTRUMENTATION_RESULT: shortMsg=Process crashed.\nINSTRUMENTATION_CODE: 0\n")))

    def test_missing_or_zero_test_success_receipt_fails(self):
        for output in ("", "OK (19 tests)\n", "INSTRUMENTATION_CODE: -1\n",
                       "OK (0 tests)\nINSTRUMENTATION_CODE: -1\n"):
            with self.subTest(output=output):
                self.assertFalse(instrumentation_succeeded(self.receipt(output)))

    def test_failure_or_nonzero_adb_exit_overrides_positive_receipt(self):
        success = "OK (19 tests)\nINSTRUMENTATION_CODE: -1\n"
        self.assertFalse(instrumentation_succeeded(self.receipt(success, exit_code=1)))
        self.assertFalse(instrumentation_succeeded(self.receipt("FAILURES!!!\n" + success)))
        self.assertFalse(instrumentation_succeeded(self.receipt("INSTRUMENTATION_CODE: 0\n" + success)))


if __name__ == "__main__":
    unittest.main()
