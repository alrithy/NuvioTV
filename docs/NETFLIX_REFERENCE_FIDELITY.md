# Netflix reference fidelity

Task: NETFLIX_THEME · Contract: docs/NETFLIX_TV_2026_PARITY_AUDIT.md §1 · Matrix: docs/NETFLIX_THEME_VISUAL_VERIFICATION.md

This table compares every element visible in the maintainer's reference recording (102 s, Arabic, current
Netflix TV) with the current Nuvio implementation. The recording and its frames are not committed and are
not available to automated agents: the REFERENCE column records the observations written down in the
2026-10-03 audit correction. Where an exact reference number would need a frame measurement that has not
been made, the field is **NOT_MEASURABLE** and the element is not scored as geometrically matched.

## Tolerances

| Property | Tolerance |
|---|---|
| Geometry (sizes, positions) | ≤ 2 % of the 960×540 dp canvas dimension |
| Spacing | ≤ 4 dp |
| Proportions (aspect ratios, fractions) | ≤ 2 % |
| Motion (durations) | ≤ 30 ms |
| Structural differences | zero |

STATUS values: **PASS** (inside tolerance, with automated evidence) · **STRUCTURE_PASS** (structure matches,
numbers NOT_MEASURABLE against the reference) · **DIFF** (known difference) · **NOT_MEASURABLE** · **N/A**
(the reference does not show it). CURRENT values are dp on the 960×540 canvas (1080p = 320 dpi, 4K = 640 dpi).

## Home structure

| Element | REFERENCE | CURRENT | DELTA | STATUS | Evidence |
|---|---|---|---|---|---|
| Top state order | nav → hero card → category tiles → rows | same | none | STRUCTURE_PASS | productionScaffoldHomeShowsNavHeroCategoriesAndFirstRow, 36 |
| Browse state | hero scrolled out, nav stays, active row main surface, facts beneath, next row peeks | same | none | STRUCTURE_PASS | productionScaffoldHomeScrolledShowsExpandedCardFactsAndNextRow, scrollingMovesHeroOut…, 37 |
| Section gap | NOT_MEASURABLE | 20 dp | — | NOT_MEASURABLE | tokens Home.sectionGap |
| Top 10 row | rank numbers with posters | not rendered (no factual source) | structural, intentional | DIFF (data honesty) | 32 absent |

## Top navigation

| Element | REFERENCE | CURRENT | DELTA | STATUS | Evidence |
|---|---|---|---|---|---|
| Brand anchor | far left, both languages | far left, both languages (Nuvio wordmark) | none | STRUCTURE_PASS | brandAndProfileAnchorsDoNotMirrorInArabic |
| Profile anchor | far right | far right | none | STRUCTURE_PASS | topNavigationReaches… |
| Selected cue | light pill, bold label, no underline | light pill (white @ .26), bold, 0 red pixels | none | PASS | topNavigationHasNoPrimarySettingsGear… |
| Settings tab | absent | absent | none | PASS | same |
| Bar height / item height / label size | NOT_MEASURABLE | 64 / 36 dp / 17 sp | — | NOT_MEASURABLE | TopNav tokens |

## Hero

| Element | REFERENCE | CURRENT | DELTA | STATUS | Evidence |
|---|---|---|---|---|---|
| Shape | rounded card inside safe margins | 864×300 dp, radius 12, 48 dp side margins | — | STRUCTURE_PASS | heroIsLargeRoundedCardInsideSafeMargins |
| Height ratio | "large, dominant" | 300 / 476 dp content height ≈ 63 % | NOT_MEASURABLE | NOT_MEASURABLE | — |
| Title | title logo | logo preferred, text while loading / on failure | none | STRUCTURE_PASS | missingLogoAndArtworkFallBack…, 22 |
| Synopsis | not dominant | one line | none | STRUCTURE_PASS | 01 / 36 |
| CTA sizes | NOT_MEASURABLE | 44 dp high | — | NOT_MEASURABLE | tokens buttonHeight |

## Category strip

| Element | REFERENCE | CURRENT | DELTA | STATUS | Evidence |
|---|---|---|---|---|---|
| Source | real categories | real catalog rows → existing See All owner | none | PASS | categoryShortcutsOpenTheRealCatalogThroughSeeAll |
| Artwork | genre artwork | tonal text tiles (no catalog artwork owner) | visual | DIFF | 41 |
| Tile size / gap | NOT_MEASURABLE | 200×88 dp, gap 12 dp, radius 12 | — | NOT_MEASURABLE (implementation verified) | categoryStripTilesAreIntentionalTonalTiles |

## Browse rows and inline focus

| Element | REFERENCE | CURRENT | DELTA | STATUS | Evidence |
|---|---|---|---|---|---|
| Idle card | portrait ~2:3 | 112×168 dp (2:3) | 0 % proportion | PASS | primaryRowUsesPortraitIdleCards, 29 |
| Focused card | inline landscape ~16:9, ≈2–2.5× idle width | 299×168 dp (16:9, 2.67× idle) | ratio vs observed "2–2.5×": +7 % | DIFF (needs a frame measurement) | focusedPosterExpandsInline…, 30 |
| Single expansion | one item | exactly one under 100 rapid moves and 20 row changes | none | PASS | stressRapidFocusAndRowChangesKeepOneExpandedCard |
| Floating popup | none | none | none | PASS | 03 / 37 |
| Horizontal scroll | selection stays in view without jumping | comfort zone: scroll only for overflow, ½-poster neighbour peek | none observed | STRUCTURE_PASS | the six comfort-zone tests (unit + instrumented), 38–40 |
| Facts under the row | type · year · runtime/seasons · age | same, from real data only | none | STRUCTURE_PASS | 37 |
| Outline | thin light | 2 dp white @ .70 | NOT_MEASURABLE | NOT_MEASURABLE | — |

## Search

| Element | REFERENCE | CURRENT | DELTA | STATUS | Evidence |
|---|---|---|---|---|---|
| Results | portrait posters | 2:3 grid, 5 columns | — | STRUCTURE_PASS | 33 / 42 |
| Keyboard side | reading start (right in AR) | same | none | PASS | searchFullProductionScaffold…, 42 / 43 |
| Nav visible | yes | yes | none | PASS | 42 |
| Keyboard fraction / key size | NOT_MEASURABLE | < 45 % of width (asserted) | — | NOT_MEASURABLE | — |

## RTL

| Element | REFERENCE | CURRENT | DELTA | STATUS | Evidence |
|---|---|---|---|---|---|
| Nav anchors | brand left, profile right | same | none | PASS | brandAndProfileAnchors… |
| Row direction | reading order from the right | same; comfort zone mirrors physically | none | PASS | rtlComfortZoneMatchesVisibleBounds (AR runs) |
| Mixed tokens | isolated | FSI/PDI isolation | none | PASS | arabicTitleWithLatinTokens…, 24 |
| Original-title secondary line | not observed | not shown (no data owner for an original title) | — | N/A | — |

## Motion

| Element | REFERENCE | CURRENT | DELTA | STATUS |
|---|---|---|---|---|
| Expand duration | NOT_MEASURABLE (no frame timing of the recording) | 160 ms tween, 0 on LOW_RAM | — | NOT_MEASURABLE |
| Row scroll | NOT_MEASURABLE | 160 ms only when the comfort zone requires it | — | NOT_MEASURABLE |
| Frame intervals | — | not measured: the emulator uses a software GPU and cannot represent TCL C6K timing | — | MANUAL-PENDING (TCL) |

## OBSERVABLE_REFERENCE_PARITY score

Points are given only for items with automated evidence; NOT_MEASURABLE geometry and motion earn nothing.

| Category | Max | Score | Why points are withheld |
|---|---|---|---|
| Home structure | 20 | 15 | section spacing unmeasured; Top 10 not rendered (no source) |
| Top nav | 10 | 7 | bar/item geometry unmeasured |
| Hero | 10 | 5 | height ratio, CTA, fades and offsets unmeasured |
| Browse rows | 20 | 13 | expanded ratio 2.67× vs observed 2–2.5×; category artwork is text tiles |
| Inline focus | 15 | 10 | outline and comfort-zone peek unmeasured |
| Search | 10 | 6 | keyboard/result fractions and key size unmeasured |
| RTL | 10 | 8 | Arabic captures await maintainer review |
| Motion | 5 | 0 | no reference timing; TCL pending |
| **Total** | **100** | **64** | |

This is not a global "100 % Netflix" claim. Raising the score requires frame measurements from the reference
(by the maintainer), then tuning to the tolerances above. Visual review stays PENDING.
