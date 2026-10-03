# Netflix reference fidelity

Task: NETFLIX_THEME · Contract: docs/NETFLIX_TV_2026_PARITY_AUDIT.md §1 · Matrix: docs/NETFLIX_THEME_VISUAL_VERIFICATION.md

## Reference

Maintainer-measured packet (PR #100, 2026-10-03), the authoritative reference for this table:

- Recording: 102.533 s, 30 fps, 3076 frames, 910×512 H.264, current Netflix TV UI in Arabic.
- The TV screen was perspective-corrected to the project's 960×540 dp canvas (source quadrilateral TL (90,58),
  TR (749,91), BR (752,461), BL (79,469)).
- Uncertainty ≈ ±4–6 dp unless a range is given.
- The recording and its frames are not committed; only these measurements are.

This packet supersedes the earlier guessed values and NOT_MEASURABLE rows; in particular, the old "focused ≈ 2–2.5×
idle" estimate is wrong — the measured ratio is 2.73–2.84× (median 2.77×).

## Tolerances

| Property | Tolerance |
|---|---|
| Geometry | within the measured range, or ±6 dp of the representative value |
| Spacing | ±2–4 dp |
| Proportions | ≤ 2 % outside the measured range |
| Motion | inside the measured 180–230 ms band |
| Structure | zero differences |

## Measured table

CURRENT values are dp on the 960×540 canvas. Evidence = the instrumentation test that asserts the value
(1080p and 4K, EN and AR) and the capture that shows it.

| Area | REFERENCE (measured) | Before | CURRENT | STATUS | Evidence |
|---|---|---|---|---|---|
| Nav height | 60–64 | 64 | 64 | PASS | tokens |
| Selected pill height | 34–37 | 36 | 36 | PASS | topNavigationHasNoPrimarySettingsGear… |
| Profile | 30–32 | 32 | 32 | PASS | tokens |
| Brand footprint | compact mark | long wordmark | 30 dp Nuvio mark | PASS | 09–11, 36 |
| Nav anchors / pill / no underline / no Settings | as reference | same | same | PASS | brandAndProfileAnchors…, topNavigationHasNo… |
| Hero size | ≈880×400 (875–885 × 395–405) | 864×300 | 880×400 | PASS | heroIsLargeRoundedCardInsideSafeMargins, 36 |
| Hero inset / radius | x 40–45 / 12–16 | 48 / 12 | 40 / 12 | PASS | same |
| Hero synopsis | up to ~2 lines | 1 line | 2 lines | PASS | 01 / 36 |
| Hero actions | TV pills ≈44 | 44 | 44 | PASS | tokens |
| Hero title | real logo, text fallback | text | logo preferred, text fallback | PASS | missingLogoAndArtwork…, 22 |
| Top state | hero dominates, category strip only peeks | whole strip + next row visible | strip peeks | PASS | productionScaffoldHomeShowsNav…, 36 |
| Category tile height | 82–90 | 88 | 88 | PASS | categoryStripTilesAre… |
| Category tile width | content-driven ≈120–185 | 200 fixed | label-driven, bounded 120–185 | PASS | same, 41 |
| Category gap | 5–7 | 12 | 6 | PASS | same |
| Idle poster | ≈160×250 (156–162 × 245–252) | 112×168 | 160×250 | PASS | productionScaffoldHomeScrolled…, 37 |
| Focused card | ≈440×250 (438–443 × 249–253) | 299×168 | 440×250 | PASS | same |
| Expansion ratio | 2.73–2.84× (median 2.77×) | 2.67× | 2.75× | PASS | same |
| Card gap | 4–7 | 10 | 6 | PASS | same |
| Browse density | active card ≈46 % of 540; next row peeks; never two full rows | 31 % | 46 %; next row cut by the screen edge | PASS | same |
| Comfort zone | scroll only as needed; neighbours visible; edges reachable | anchor to start | comfort zone, ½-poster peek | PASS | 6 comfort-zone tests, 38–40 |
| Expansion motion | ≈200 ms (180–230) | 160 ms | 200 ms; latest focus wins; LOW_RAM instant | PASS (token) | expansionMotionMatchesMeasuredReference |
| Search columns | 4 | 5 | 4 | PASS | searchUsesFourColumnsOfMeasuredPosters… |
| Search poster | ≈150×210 (145–155 × 205–215) | ≈131×197 | 150×210 | PASS | same |
| Search poster labels | none | title under every poster | none (semantic only) | PASS | same |
| Search query UI | small icon + query line | 56 dp outlined field + title | icon + query line, 35 dp | PASS | searchQueryIsACompactLineNotAFormBox |
| Search keyboard width | 190–200 | 240 | 196 | PASS | same |
| Search keys | 30–32, 6 columns | 32 (glyphs squeezed by padding) | 31 + 2 dp gap, 6 columns | PASS | searchKeyboardGlyphsAreLegible |
| Arabic key glyphs | clearly legible 14–16 sp | weak / near-empty | SansSerif 15 sp Medium, ink asserted | PASS | same, 42 (AR) |
| Keyboard side | AR right / EN left | same | same | PASS | searchFullProductionScaffold… |
| Search scroll | rows never clipped under the header | first row clipped (old 43) | explicit row-aligned scroll | PASS | searchFirstVisibleResultNeverClipsUnderHeader, 43 |

## Data-limited differences (not UI defects)

| Reference element | Why it is absent |
|---|---|
| Top 10 row (≈00:77–00:80) | Nuvio has no factual ranking source; fabricating ranks is prohibited. |
| Category artwork | No catalog owns a genuine identifying image; title posters are not used as fake category art. |
| Original-language secondary title | No data owner provides an original title. |

## Not verifiable on the emulator (TCL C6K, MANUAL-PENDING)

Real D-pad feel, sustained frame time during the 200 ms expansion and row scroll, 4K image memory, LOW_RAM behaviour on
the device. The emulator uses a software GPU; no frame-rate claim is made from it.

## Scores

**A. Implementable observable parity: see the final report for the score on the verified head.** Every row above is an
implementable visible measurement. A row counts only when the CI run on the head asserts it in all four
configurations. Motion is asserted as a token, not as measured frame timing, and frame timing is TCL-pending.

**B. Raw reference coverage:** all reference-visible elements except the three data-limited rows above.

Visual review stays PENDING for the maintainer.
