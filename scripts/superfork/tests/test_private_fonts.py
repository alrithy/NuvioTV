"""Private Thmanyah Sans input: deterministic secret round trip, strict validation, Git guard."""
import base64
import contextlib
import hashlib
import io
import lzma
import os
from pathlib import Path
import sys
import tarfile
import tempfile
import unittest
from unittest import mock

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import private_fonts as pf

NAMES = ["thmanyahsans-Light.otf", "thmanyahsans-Regular.otf", "thmanyahsans-Medium.otf",
         "thmanyahsans-Bold.otf", "thmanyahsans-Black.otf"]


class PrivateFontsTests(unittest.TestCase):
    def setUp(self):
        self.tmp = Path(tempfile.mkdtemp())
        self.source = self.tmp / "fonts"
        self.source.mkdir()
        # Synthetic stand-ins (random bytes behind an OpenType tag); no real font is used.
        lines = []
        for index, name in enumerate(NAMES):
            data = b"OTTO" + os.urandom(60_000 + index)
            (self.source / name).write_bytes(data)
            lines.append(f"{hashlib.sha256(data).hexdigest()}  {name}")
        self.manifest = self.tmp / "manifest.sha256"
        self.manifest.write_text("\n".join(lines) + "\n", encoding="utf-8")
        patcher = mock.patch.object(pf, "MANIFEST", self.manifest)
        patcher.start()
        self.addCleanup(patcher.stop)

    def secrets_from(self, parts_dir):
        return {p.name: p.read_text(encoding="ascii") for p in parts_dir.iterdir()}

    def run_quiet(self, function, *args):
        with contextlib.redirect_stdout(io.StringIO()) as out:
            code = function(*args)
        return code, out.getvalue()

    def test_pack_and_prepare_round_trip_exactly_the_five_files(self):
        parts = self.tmp / "parts"
        code, _ = self.run_quiet(pf.pack, self.source, parts)
        self.assertEqual(0, code)
        secrets = self.secrets_from(parts)
        self.assertTrue(all(len(v) <= pf.PART_CHARS for v in secrets.values()))
        env_file, summary = self.tmp / "env", self.tmp / "summary"
        runner = self.tmp / "runner"
        runner.mkdir()
        with mock.patch.dict(os.environ, {**secrets, "RUNNER_TEMP": str(runner), "GITHUB_ENV": str(env_file),
                                          "GITHUB_STEP_SUMMARY": str(summary)}):
            code, output = self.run_quiet(pf.ci_prepare, "required")
        self.assertEqual(0, code)
        decoded = runner / "thmanyah-sans"
        self.assertEqual(sorted(NAMES), sorted(p.name for p in decoded.iterdir()))
        for name in NAMES:
            self.assertEqual((self.source / name).read_bytes(), (decoded / name).read_bytes())
        self.assertIn(f"THMANYAH_FONT_DIR={decoded}", env_file.read_text())
        self.assertIn("THMANYAH_FONT_REQUIRED=true", env_file.read_text())
        self.assertIn("THMANYAH_PRIVATE_FONT = embedded", output)
        # Nothing secret reaches the log.
        self.assertNotIn(next(iter(secrets.values()))[:200], output)
        # Deterministic: packing again yields identical secret values.
        again = self.tmp / "again"
        self.run_quiet(pf.pack, self.source, again)
        self.assertEqual(secrets, self.secrets_from(again))
        with mock.patch.dict(os.environ, {"RUNNER_TEMP": str(runner)}):
            self.run_quiet(pf.ci_cleanup)
        self.assertFalse(decoded.exists())

    def test_missing_input_fails_when_required_and_falls_back_when_optional(self):
        empty = {f"{pf.SECRET_PREFIX}{i:02d}": "" for i in range(1, pf.SECRET_SLOTS + 1)}
        with mock.patch.dict(os.environ, empty):
            required, output = self.run_quiet(pf.ci_prepare, "required")
            optional, fallback = self.run_quiet(pf.ci_prepare, "optional")
        self.assertEqual(1, required)
        self.assertIn("missing (required)", output)
        self.assertEqual(0, optional)
        self.assertIn("THMANYAH_PRIVATE_FONT = unavailable / fallback", fallback)

    def test_archive_with_an_extra_member_is_rejected(self):
        buffer = io.BytesIO()
        with tarfile.open(fileobj=buffer, mode="w") as archive:
            for name in NAMES + ["thmanyahseriftext-Regular.otf"]:
                data = (self.source / NAMES[0]).read_bytes()
                info = tarfile.TarInfo(name)
                info.size = len(data)
                archive.addfile(info, io.BytesIO(data))
        encoded = base64.b64encode(lzma.compress(buffer.getvalue())).decode()
        runner = self.tmp / "runner"
        runner.mkdir()
        with mock.patch.dict(os.environ, {f"{pf.SECRET_PREFIX}01": encoded, "RUNNER_TEMP": str(runner)}):
            code, output = self.run_quiet(pf.ci_prepare, "required")
        self.assertEqual(1, code)
        self.assertIn("invalid", output)
        self.assertFalse((runner / "thmanyah-sans").exists())

    def test_modified_file_does_not_match_the_manifest(self):
        (self.source / NAMES[1]).write_bytes(b"OTTO" + b"\0" * 10)
        self.assertIn("does not match", pf.problem_with(self.source))

    def test_guard_refuses_font_binaries_and_embedded_font_data(self):
        repo = self.tmp / "repo"
        (repo / "app/src/main/res/font").mkdir(parents=True)
        (repo / "app/src/main/res/font/inter_variable.ttf").write_bytes(b"\0\1\0\0allowed")
        (repo / "notes.txt").write_text("plain text", encoding="utf-8")
        with mock.patch.object(pf, "ROOT", repo):
            with mock.patch.object(pf, "tracked_files", return_value=["app/src/main/res/font/inter_variable.ttf", "notes.txt"]):
                self.assertEqual(0, self.run_quiet(pf.guard)[0])
            (repo / "leak.otf").write_bytes(b"OTTO")
            (repo / "blob.kt").write_text("val f = \"" + base64.b64encode(b"OTTO" + os.urandom(3000)).decode() + "\"")
            (repo / "renamed.bin").write_bytes(b"OTTO" + os.urandom(100))
            with mock.patch.object(pf, "tracked_files", return_value=["leak.otf", "blob.kt", "renamed.bin"]):
                code, output = self.run_quiet(pf.guard)
        self.assertEqual(1, code)
        for name in ("leak.otf", "blob.kt", "renamed.bin"):
            self.assertIn(name, output)


if __name__ == "__main__":
    unittest.main()
