# Private fonts: Thmanyah Sans

Thmanyah Sans is the official Nuvio UI typeface (Light 300, Regular 400, Medium 500, Bold 700,
Black 900). The maintainer holds a licence from Thmanyah Publishing and Distribution that allows
embedding the font in a compiled, packaged application. The same licence forbids redistributing,
uploading or hosting the font files, and making them available so end users can extract them as
standalone font files. So the font files are **build input only**: they never enter Git, a GitHub
Release as separate files, a workflow artifact or a screenshot directory. The licence asks for no
attribution or notice inside the product, so none is added. Thmanyah Serif Text/Display are not used.

An APK is a zip: the embedded font can be extracted from an installed app by anyone determined to.
Nothing here makes it non-extractable. Whether shipping it as a normal Android font resource meets
the licence's "end users may not extract" clause is the licensee's call (ask@thmanyah.com handles
licence questions).

## Code owner

- `app/src/main/java/com/nuvio/tv/ui/theme/Type.kt`: `NuvioFontFamily`, the only app font family.
  Every theme (Netflix included) and the Compose Material 3 typography read it; `AppFont.THMANYAH_SANS`
  is the default UI font. Only the five real files are declared; 600 requests use the Bold file and
  `FontSynthesis.None` forbids fake bold. Missing glyphs (arrows, ★, Persian letters) fall back per
  glyph to the platform Sans. Monospace debug text, icons, artwork and subtitles keep their own fonts.
- `ThmanyahFontResources` (generated Java) and `BuildConfig.THMANYAH_EMBEDDED` say whether the build
  embedded the real files. Without them `NuvioFontFamily` is the platform Sans.

## Local builds

Put exactly these five files, unmodified, in the ignored `private-fonts/thmanyah/`:

```
thmanyahsans-Light.otf  thmanyahsans-Regular.otf  thmanyahsans-Medium.otf
thmanyahsans-Bold.otf   thmanyahsans-Black.otf
```

Gradle checks them against `scripts/superfork/thmanyah_sans.sha256` (hashes only, no font data) and
copies them into `app/build/generated/res/<variant>/prepare<Variant>ThmanyahFonts/font/`. Another
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
(`ci-cleanup`) overwrites and deletes the decoded files and the generated font resources.

| Workflow | Missing input |
| --- | --- |
| Superfork Test Build, Android Release | fails (`THMANYAH_PRIVATE_FONT = missing (required)`) |
| Superfork Netflix Visual | fails for same-repository runs; fork PRs fall back |
| Superfork CI, Device Smoke | falls back, reported as `THMANYAH_PRIVATE_FONT = unavailable / fallback` |

Uploaded artifacts are APKs, test reports and screenshots; the generated font directory is never
uploaded. `private_fonts.py guard` (Superfork CI) fails if any font binary other than the three
upstream OFL fonts in `app/src/main/res/font/`, a Thmanyah archive, or base64 font/zip data is tracked.

## Verification

`NuvioTypographyTvTest` (root owner, five real weights, Arabic glyphs without tofu, mixed-script
bounds) and the Thmanyah tests in `NetflixThemeTvTest` (Netflix family, Arabic keyboard, top nav,
hero synopsis, category labels, Search query line, long detail title) run in every Netflix Visual
matrix job with the font embedded.
