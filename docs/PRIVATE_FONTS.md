# Private fonts: Thmanyah Sans

Thmanyah Sans is the official Nuvio UI typeface (Light 300, Regular 400, Medium 500, Bold 700,
Black 900) for Arabic and English. Thmanyah Serif Text/Display are not used.

## Licence status

```
THMANYAH_APP_EMBEDDING = PERMITTED_BY_SUPPLIED_LICENSE
THMANYAH_STANDALONE_REDISTRIBUTION = PROHIBITED
```

The maintainer's Arabic licence from Thmanyah Publishing and Distribution permits commercial use in
applications and embedding the font in software when it is part of a compiled, packaged or obfuscated
product (maintainer decision, 2026-10-04). It forbids redistributing, uploading or hosting the font
files and making them available for independent extraction, download or reuse. The licence asks for no
attribution or notice inside the product, so none is added. No backend or per-device licensing is used.

So the original font files are **build input only**: they never enter Git, a GitHub Release, a
workflow artifact or a screenshot directory, and the APK holds no standalone font file.

## How the font reaches the screen

```
private-fonts/thmanyah/ (local) or THMANYAH_SANS_B64_NN secrets (CI)
  -> Gradle prepare<Variant>ThmanyahFonts: SHA-256 check, AES-CTR encrypt with a fresh per-build key
  -> one neutral asset, assets/nuvio/ui-type.pack (no font filename, family name or extension)
  -> APK
  -> NuvioUiFonts: decrypt once per process into direct memory, verify each file's SHA-256
  -> android.graphics.fonts.Font from that memory -> Typeface -> Compose NuvioFontFamily
```

- The pack is storage protection only. Decryption restores the exact original bytes, checked against
  `scripts/superfork/thmanyah_sans.sha256`; glyphs, tables and the family name are never changed.
- The decrypted fonts are never written to disk or cache. The encrypted copy read from the asset and
  the key copy are zeroed after use; the decrypted buffers stay in memory because the platform font
  draws from them.
- An APK is a zip and an app holds its decryption key, so a determined person can still recover the
  font from an installed app. The pack keeps the files from being exposed as standalone fonts; it is
  not cryptographic protection.
- In-memory fonts need API 29. Devices below it, builds without the private input (fork PRs) and a
  pack that fails its checks use the platform Sans.

## Code owner

- `app/src/main/java/com/nuvio/tv/ui/theme/Type.kt`: `NuvioFontFamily`, the only app font family.
  Every theme (Netflix included) and the Compose Material 3 typography read it; `AppFont.THMANYAH_SANS`
  is the default UI font. Only the five real weights are declared; 600 requests use the Bold file and
  `FontSynthesis.None` forbids fake bold. Missing glyphs (arrows, ★, Persian letters) fall back per
  glyph to the platform Sans. Monospace debug text, icons, artwork and subtitles keep their own fonts.
- `NuvioUiFonts.kt`: the in-memory loader. `NuvioUiFontPack` (generated Java) carries the pack layout,
  the expected SHA-256 values and the key; `BuildConfig.THMANYAH_EMBEDDED` says whether the build packed
  the real files.

## Local builds

Put exactly these five files, unmodified, in the ignored `private-fonts/thmanyah/`:

```
thmanyahsans-Light.otf  thmanyahsans-Regular.otf  thmanyahsans-Medium.otf
thmanyahsans-Bold.otf   thmanyahsans-Black.otf
```

Gradle checks them against `scripts/superfork/thmanyah_sans.sha256` (hashes only, no font data) and
writes the encrypted pack under `app/build/generated/` (ignored). Another
location can be given with `THMANYAH_FONT_DIR` or `-PthmanyahFontDir`. `THMANYAH_FONT_REQUIRED=true`
(or `-PthmanyahFontRequired=true`) turns a missing or mismatched input into a build failure.
`python3 scripts/superfork/private_fonts.py verify private-fonts/thmanyah` checks a directory.

## CI secrets

GitHub caps a secret at 48 KB, and the five OTF files are about 1.2 MB, so one
`THMANYAH_SANS_ZIP_BASE64` secret cannot hold them. The fonts travel as a deterministic tar.xz,
base64-encoded and split into `THMANYAH_SANS_B64_01` … `THMANYAH_SANS_B64_10` (currently 8 parts).
On the maintainer's machine, from the repository root:

```
python3 scripts/superfork/private_fonts.py pack private-fonts/thmanyah /tmp/thmanyah-secrets
```

It prints one `gh secret set THMANYAH_SANS_B64_NN --repo alrithy/NuvioTV < file` line per part
(or paste each file's content in Settings > Secrets and variables > Actions); then delete the
temporary folder. The step `private_fonts.py ci-prepare` rebuilds the archive in `$RUNNER_TEMP`,
accepts only the five expected regular files with the manifest hashes, exports `THMANYAH_FONT_DIR`
and `THMANYAH_FONT_REQUIRED=true`, and never prints contents. An `always()` step
(`ci-cleanup`) overwrites and deletes the decoded files and the generated pack directories.

| Workflow | Missing input |
| --- | --- |
| Superfork Test Build, Android Release | fails (`THMANYAH_PRIVATE_FONT = missing (required)`) |
| Superfork Netflix Visual | fails for same-repository runs; fork PRs fall back |
| Superfork CI, Device Smoke | falls back, reported as `THMANYAH_PRIVATE_FONT = unavailable / fallback` |

Uploaded artifacts are APKs, test reports and screenshots; the generated directories are never
uploaded. `private_fonts.py guard` (Superfork CI) fails if any font binary other than the three
upstream OFL fonts in `app/src/main/res/font/`, a Thmanyah archive, or base64 font/zip data is tracked.
After every build, `private_fonts.py apk-check` fails if the APK holds a licensed file under any name, a
Thmanyah-named entry, or font data naming the family; with the input present it also requires the pack.

## Verification

`NuvioTypographyTvTest` (root owner, five real weights read from the decrypted files, Arabic glyphs
without tofu, mixed-script bounds, no font resource or font-shaped asset in the APK, nothing written to
disk while loading) and the Thmanyah tests in `NetflixThemeTvTest` (Netflix family, Arabic keyboard, top nav,
hero synopsis, category labels, Search query line, long detail title) run in every Netflix Visual
matrix job with the font embedded.
