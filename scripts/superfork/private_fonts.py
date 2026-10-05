#!/usr/bin/env python3
"""Private Thmanyah Sans input for builds, and the guard that keeps font binaries out of Git.

The maintainer's licence allows the font inside the compiled app, not in the repository, a release
or a workflow artifact. Builds read the five OTF files from an ignored directory
(private-fonts/thmanyah/ locally, $RUNNER_TEMP in CI); Gradle copies them into
app/build/generated/ and from there into the APK. This tool never prints font bytes or secret contents.

GitHub secrets are capped at 48 KB, so CI receives the fonts as a base64 tar.xz split across
THMANYAH_SANS_B64_01 … THMANYAH_SANS_B64_10 (unused slots stay empty).

  pack <source-dir> <out-dir>   maintainer, local: write the secret parts and print the gh commands
  ci-prepare --policy P         CI: rebuild the archive from the secrets, validate, export THMANYAH_FONT_DIR
  ci-cleanup                    CI (always()): overwrite and delete the decoded and generated copies
  verify <dir>                  check a directory holds exactly the five expected files
  guard                         fail if a font binary or embedded font data is tracked by Git
  apk-check <apk> [--embedded]  fail if an APK carries a standalone font file other than the upstream OFL ones;
                                with --embedded, also require the encrypted pack asset
"""

from __future__ import annotations

import argparse
import base64
import hashlib
import io
import lzma
import os
import re
import shutil
import subprocess
import sys
import tarfile
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
MANIFEST = ROOT / "scripts/superfork/thmanyah_sans.sha256"
SECRET_PREFIX = "THMANYAH_SANS_B64_"
SECRET_SLOTS = 10
# GitHub's limit is 48 KB per secret; stay below it with ASCII base64.
PART_CHARS = 48_000
# AGP places each variant's generated directories under app/build/generated/<kind>/<variant>/<task>/.
GENERATED_TASK_GLOB = "app/build/generated/**/prepare*ThmanyahFonts"

# Fonts committed by upstream under the SIL Open Font License; every other font binary is refused.
ALLOWED_TRACKED_FONTS = {
    "app/src/main/res/font/dm_sans_variable.ttf",
    "app/src/main/res/font/inter_variable.ttf",
    "app/src/main/res/font/opensans_variable.ttf",
}
FONT_SUFFIXES = (".otf", ".ttf", ".woff", ".woff2", ".ttc", ".eot")
# Base64 of the OpenType/CFF, TrueType, WOFF, WOFF2 and ZIP signatures at the start of a long run.
FONT_MAGIC = (b"OTTO", b"wOFF", b"wOF2", b"\x00\x01\x00\x00", b"ttcf")
FAMILY_MARKERS = (b"thmanyah", "thmanyah".encode("utf-16-be"))
# The encrypted asset the build packs the five weights into (app/build.gradle.kts, NuvioUiFonts.kt).
PACK_ASSET = "assets/nuvio/ui-type.pack"
EMBEDDED_FONT_PATTERN = re.compile(rb"(?:T1RUT|AAEAA|d09GR|d09GM|UEsDB)[A-Za-z0-9+/]{1500,}")


def expected_files() -> dict[str, str]:
    entries: dict[str, str] = {}
    for line in MANIFEST.read_text(encoding="utf-8").splitlines():
        if line.strip():
            digest, name = line.split()
            entries[name] = digest
    return entries


def problem_with(directory: Path) -> str | None:
    """Why the directory is not exactly the licensed five-file family, or None when it is."""
    expected = expected_files()
    if not directory.is_dir():
        return f"no directory at {directory}"
    present = {p.name for p in directory.iterdir() if not p.name.startswith(".")}
    if missing := sorted(set(expected) - present):
        return f"missing {missing}"
    if extra := sorted(present - set(expected)):
        return f"unexpected files {extra}"
    for name, digest in expected.items():
        path = directory / name
        if not path.is_file() or path.is_symlink():
            return f"{name} is not a regular file"
        if hashlib.sha256(path.read_bytes()).hexdigest() != digest:
            return f"{name} does not match scripts/superfork/thmanyah_sans.sha256"
    return None


def pack(source: Path, out_dir: Path) -> int:
    if problem := problem_with(source):
        print(f"error: {problem}", file=sys.stderr)
        return 1
    buffer = io.BytesIO()
    with tarfile.open(fileobj=buffer, mode="w") as archive:
        for name in expected_files():
            info = archive.gettarinfo(str(source / name), arcname=name)
            # Deterministic archive: no owner, time or mode from the maintainer's machine.
            info.uid = info.gid = 0
            info.uname = info.gname = ""
            info.mtime = 0
            info.mode = 0o644
            with open(source / name, "rb") as handle:
                archive.addfile(info, handle)
    encoded = base64.b64encode(lzma.compress(buffer.getvalue(), preset=9 | lzma.PRESET_EXTREME)).decode("ascii")
    parts = [encoded[i:i + PART_CHARS] for i in range(0, len(encoded), PART_CHARS)]
    if len(parts) > SECRET_SLOTS:
        print(f"error: {len(parts)} parts exceed the {SECRET_SLOTS} secret slots", file=sys.stderr)
        return 1
    out_dir.mkdir(parents=True, exist_ok=True)
    os.chmod(out_dir, 0o700)
    print(f"{len(parts)} parts. Set them as repository secrets (never commit these files):")
    for index in range(1, SECRET_SLOTS + 1):
        name = f"{SECRET_PREFIX}{index:02d}"
        if index <= len(parts):
            path = out_dir / name
            path.write_text(parts[index - 1], encoding="ascii")
            os.chmod(path, 0o600)
            print(f"  gh secret set {name} --repo alrithy/NuvioTV < '{path}'")
        else:
            print(f"  gh secret delete {name} --repo alrithy/NuvioTV   # only if it exists from an older pack")
    print(f"Then delete {out_dir}.")
    return 0


def github_output(file_env: str, line: str) -> None:
    target = os.environ.get(file_env)
    if target:
        with open(target, "a", encoding="utf-8") as handle:
            handle.write(line + "\n")


def report(state: str) -> None:
    print(f"THMANYAH_PRIVATE_FONT = {state}")
    github_output("GITHUB_STEP_SUMMARY", f"- `THMANYAH_PRIVATE_FONT = {state}`")


def ci_prepare(policy: str) -> int:
    encoded = "".join(os.environ.get(f"{SECRET_PREFIX}{i:02d}", "").strip() for i in range(1, SECRET_SLOTS + 1))
    if not encoded:
        if policy == "required":
            print(f"::error title=Thmanyah Sans missing::This build requires the private font input; "
                  f"set the {SECRET_PREFIX}01..NN repository secrets (docs/PRIVATE_FONTS.md).")
            report("missing (required)")
            return 1
        print("::warning title=Thmanyah Sans unavailable::No private font input; building with the platform Sans fallback.")
        report("unavailable / fallback")
        return 0
    destination = Path(os.environ.get("RUNNER_TEMP", "/tmp")) / "thmanyah-sans"
    shutil.rmtree(destination, ignore_errors=True)
    destination.mkdir(parents=True)
    os.chmod(destination, 0o700)
    try:
        payload = lzma.decompress(base64.b64decode(encoded, validate=True))
        expected = expected_files()
        with tarfile.open(fileobj=io.BytesIO(payload), mode="r:") as archive:
            members = archive.getmembers()
            names = [m.name for m in members]
            if sorted(names) != sorted(expected) or not all(m.isfile() for m in members):
                raise ValueError(f"archive must hold exactly {sorted(expected)} as regular files")
            for member in members:
                data = archive.extractfile(member).read()
                path = destination / member.name
                path.write_bytes(data)
                os.chmod(path, 0o600)
    except Exception as error:  # noqa: BLE001 - any malformed input is a hard failure without content
        shutil.rmtree(destination, ignore_errors=True)
        print(f"::error title=Thmanyah Sans input invalid::{type(error).__name__}: {error}")
        report("invalid")
        return 1
    if problem := problem_with(destination):
        shutil.rmtree(destination, ignore_errors=True)
        print(f"::error title=Thmanyah Sans input invalid::{problem}")
        report("invalid")
        return 1
    github_output("GITHUB_ENV", f"THMANYAH_FONT_DIR={destination}")
    # Once the input is present the build must embed it; Gradle fails rather than falling back.
    github_output("GITHUB_ENV", "THMANYAH_FONT_REQUIRED=true")
    report("embedded")
    return 0


def scrub(path: Path) -> None:
    if path.is_dir() and not path.is_symlink():
        for child in path.iterdir():
            scrub(child)
        path.rmdir()
    elif path.exists() or path.is_symlink():
        if path.is_file() and not path.is_symlink():
            size = path.stat().st_size
            with open(path, "r+b") as handle:
                handle.write(b"\0" * size)
                handle.flush()
                os.fsync(handle.fileno())
        path.unlink()


def ci_cleanup() -> int:
    targets = [Path(os.environ.get("RUNNER_TEMP", "/tmp")) / "thmanyah-sans"] + sorted(ROOT.glob(GENERATED_TASK_GLOB))
    for target in targets:
        scrub(target)
    print("Removed the decoded Thmanyah Sans input and the generated font resources.")
    return 0


def tracked_files() -> list[str]:
    output = subprocess.run(["git", "ls-files", "-z"], cwd=ROOT, check=True, capture_output=True).stdout
    return [name for name in output.decode("utf-8").split("\0") if name]


def guard() -> int:
    failures: list[str] = []
    for name in tracked_files():
        lower = name.lower()
        if lower.endswith(FONT_SUFFIXES) and name not in ALLOWED_TRACKED_FONTS:
            failures.append(f"font binary tracked: {name}")
            continue
        if "thmanyah" in lower and lower.endswith((".zip", ".tar", ".xz", ".gz", ".7z", ".b64", ".base64")):
            failures.append(f"font archive tracked: {name}")
            continue
        path = ROOT / name
        if lower.endswith(FONT_SUFFIXES) or not path.is_file() or path.stat().st_size > 20_000_000:
            continue
        data = path.read_bytes()
        if data[:4] in (b"OTTO", b"wOFF", b"wOF2", b"\x00\x01\x00\x00") and not lower.endswith((".png", ".jpg", ".webp", ".jks")):
            failures.append(f"font data under another name: {name}")
        elif EMBEDDED_FONT_PATTERN.search(data):
            failures.append(f"base64 font or archive data embedded in: {name}")
    if failures:
        print("Prohibited font content is tracked by Git:")
        for failure in failures:
            print(f"  {failure}")
        return 1
    print(f"Font guard: no prohibited font binaries or embedded font data in {len(tracked_files())} tracked files.")
    return 0


def apk_check(apk: Path, embedded: bool) -> int:
    """The APK may hold the encrypted pack, never a standalone Thmanyah Sans file under any name.

    Other fonts (the upstream OFL UI fonts, subtitle fonts from libraries) are listed, not refused.
    """
    licensed = set(expected_files().values())
    failures: list[str] = []
    other_fonts: list[str] = []
    with zipfile.ZipFile(apk) as archive:
        names = archive.namelist()
        for name in names:
            lower = name.lower()
            data = archive.read(name)
            if hashlib.sha256(data).hexdigest() in licensed:
                failures.append(f"licensed font file stored as {name}")
            elif "thmanyah" in lower:
                failures.append(f"font-named entry {name}")
            elif lower.endswith(FONT_SUFFIXES) or (lower.startswith(("res/", "assets/")) and data[:4] in FONT_MAGIC):
                if any(marker in data.lower() for marker in FAMILY_MARKERS):
                    failures.append(f"Thmanyah font data in {name}")
                else:
                    other_fonts.append(name)
        if embedded:
            if PACK_ASSET not in names:
                failures.append(f"{PACK_ASSET} is missing")
            elif archive.read(PACK_ASSET)[:4] in FONT_MAGIC:
                failures.append(f"{PACK_ASSET} is a raw font, not the encrypted pack")
    if failures:
        print(f"{apk.name} exposes Thmanyah Sans font files:")
        for failure in failures:
            print(f"  {failure}")
        return 1
    print(f"{apk.name}: no standalone Thmanyah Sans file" + (" (encrypted pack present)" if embedded else "") + ".")
    if other_fonts:
        print(f"  other fonts: {', '.join(sorted(other_fonts))}")
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    commands = parser.add_subparsers(dest="command", required=True)
    pack_parser = commands.add_parser("pack")
    pack_parser.add_argument("source", type=Path)
    pack_parser.add_argument("out_dir", type=Path)
    prepare = commands.add_parser("ci-prepare")
    prepare.add_argument("--policy", choices=("required", "optional"), required=True)
    commands.add_parser("ci-cleanup")
    verify = commands.add_parser("verify")
    verify.add_argument("directory", type=Path)
    commands.add_parser("guard")
    apk = commands.add_parser("apk-check")
    apk.add_argument("apks", type=Path, nargs="+")
    apk.add_argument("--embedded", action="store_true")
    args = parser.parse_args()
    if args.command == "pack":
        return pack(args.source, args.out_dir)
    if args.command == "ci-prepare":
        return ci_prepare(args.policy)
    if args.command == "ci-cleanup":
        return ci_cleanup()
    if args.command == "apk-check":
        return max(apk_check(path, args.embedded) for path in args.apks)
    if args.command == "verify":
        problem = problem_with(args.directory)
        print(problem or "Thmanyah Sans: the five expected files are present and match the manifest.")
        return 1 if problem else 0
    return guard()


if __name__ == "__main__":
    sys.exit(main())
