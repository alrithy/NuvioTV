"""Require Android runner evidence, since adb's success exit alone is insufficient."""
from pathlib import Path
import subprocess
import sys
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import contextlib
import io

from capture_netflix_theme import ALIASES, SCREENS, instrumentation_succeeded, print_failures


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


class NetflixCaptureLogTests(unittest.TestCase):
    def test_failures_are_named_with_their_stack_in_the_log(self):
        output = ("INSTRUMENTATION_STATUS: test=passes\nINSTRUMENTATION_STATUS_CODE: 1\n"
                  "INSTRUMENTATION_STATUS: test=passes\nINSTRUMENTATION_STATUS_CODE: 0\n"
                  "INSTRUMENTATION_STATUS: stack=java.lang.AssertionError: expected:<2>\n\tat X.kt:9\n"
                  "INSTRUMENTATION_STATUS: test=breaks\nINSTRUMENTATION_STATUS_CODE: -2\n"
                  "Tests run: 2,  Failures: 1\n")
        buffer = io.StringIO()
        with contextlib.redirect_stdout(buffer):
            print_failures(output)
        log = buffer.getvalue()
        self.assertIn("FAILED breaks", log)
        self.assertIn("expected:<2>", log)
        self.assertNotIn("FAILED passes", log)

    def test_matrix_names_the_review_surfaces_once(self):
        self.assertEqual(34, len(SCREENS))
        self.assertFalse(any(name.startswith("32-") for name in SCREENS))
        self.assertTrue(all(alias <= set(SCREENS) for alias in ALIASES))
        self.assertEqual(len(SCREENS), len(set(SCREENS)))
        self.assertFalse(any(name.startswith("09-navigation") for name in SCREENS))


if __name__ == "__main__":
    unittest.main()
