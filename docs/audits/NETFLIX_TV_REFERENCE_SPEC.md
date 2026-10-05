# Netflix TV reference spec (current top-navigation generation)

Status: PHASE 0 — EVIDENCE INCOMPLETE. Do not implement a state marked NOT OBSERVED.
Task: NETFLIX_THEME · Branch: feat/netflix-theme · Baseline: Test Build 76 (5cfbdd7)

## Evidence actually available to the implementing agent

| Source | Access | What it supports |
|---|---|---|
| E1 Maintainer frame measurement of a 102.533 s TCL C6K Netflix TV recording (30 fps, 3076 frames, 910×512, perspective-normalised to 960×540, ±4–6 dp), PR #100 comment 2026-10-03 | Received as numbers; the video itself was never visible to the agent | Home top state, Home browse state, category strip, focused expansion, Top 10 presence, Search + Arabic keyboard, Search results, motion timing |
| E2 Public press coverage of Netflix's May 2025 TV redesign (IBC, Engadget, MacRumors, Tom's Guide via web-search result text) | Search-result text only. about.netflix.com, help.netflix.com and press pages are blocked by this environment's network policy; no screenshot or video was seen | Navigation items and position; My Netflix contents; focused titles show richer info |
| E3 Official Help Center / Tudum article text as quoted by web search (2026-10-05) | Search-result quotes only; the pages are egress-blocked | Back → top menu; Categories moved into Search; My Netflix contents; focused-card information |
| Official Netflix Help Center / Tudum pages and screenshots | NOT ACCESSED (egress blocked) | — |
| Interactive Netflix | NOT AVAILABLE | — |

Nothing in this document comes from memory of older Netflix UIs. The legacy left sidebar is not a reference.

## Structural facts (E2/E3, textual)

E3 (added 2026-10-05): web-search result text for the official Help Center article "An update to the Netflix TV experience and layout" (help.netflix.com/en/node/321880164349028), the Tudum article "Netflix's New Layout: What to Know About the TV Redesign" and press coverage (Engadget, Tom's Guide, SlashGear, BGR, IBC). The pages themselves are blocked by this environment's egress policy (help.netflix.com, www.netflix.com, androidpolice.com, informitv.com, medium.com all returned EGRESS_BLOCKED on 2026-10-05); only the search engine's quoted text was read. No image or video from these sources was seen.

| Fact | Source text (search-result quote) | Status |
|---|---|---|
| Navigation is a bar across the top, not a left sidebar | "a navigation bar across the top of the screen, instead of on the left-hand side" (IBC) | CONFIRMED (E2, E3) |
| Items | "shortcuts to TV shows, movies, games, My Netflix, and your profiles in a menu at the top" (Help Center); "Search, Shows, Movies, Games, and My Netflix … always visible" (Tudum); press lists "Search", "Home", "Shows", "Movies", "My Netflix" | CONFIRMED; Games only where supported |
| Back returns to the top menu | "Press the back button on your remote to quickly get back to the menu at the top of the homepage any time" (Help Center) | CONFIRMED (E3) — was NOT CONFIRMED in the first draft |
| Categories live in Search | "The Categories shortcut has been removed from the menu options. You can choose Search … and select from the list on the left side, or search by categories or genres" (Tudum) | CONFIRMED (E3); exact Search category layout NOT OBSERVED |
| My Netflix contents | "everything you've added to My List, titles to Continue Watching, shows and movies you watched and loved, Reminders, and more" (Help Center) | CONFIRMED |
| Focused title expands | "the currently selected title card will expand into a larger, rectangular box"; "synopsis, runtime, award wins, Top 10 history, or key cast all up-front while you browse" (press) | CONFIRMED; geometry from E1 below |
| Responsive recommendations | "the next row of recommendations changes after the user lingers on a title for a few seconds" (Engadget, Tom's Guide) | CONFIRMED as behaviour; Nuvio has no equivalent signal: DATA-LIMITED |
| Fewer titles, more video/animation on Home | "fewer titles but more video and animation" (press) | CONFIRMED as intent; no geometry |

## Measured states (E1)

All values are dp on the 960×540 canvas.

### Top navigation
| Property | Value |
|---|---|
| Bar height | 60–64 |
| Selected item | light rounded pill, 34–37 high, bold label; no underline |
| Profile | 30–32, physically right in Arabic too |
| Brand | compact mark, physically left |

### Home — top state
| Property | Value |
|---|---|
| Hero | x 40–45, y 66–72, 875–885 × 395–405, radius 12–16 |
| Hero content | title logo when available; compact metadata; synopsis up to ~2 lines; two TV-sized action pills (~44); factual callout; text occupies part of the artwork |
| Below hero | only a small part of the category strip is visible |

### Home — category strip
| Property | Value |
|---|---|
| Tile height | 82–90 |
| Tile width | content-dependent, observed ≈120, 123, 140, 140, 180 |
| Gap | 5–7 |

### Home — browse state and focused expansion
| Property | Value |
|---|---|
| Idle portrait | 156–162 × 245–252 |
| Focused landscape | 438–443 × 249–253 (≈16:9) |
| Focused ÷ idle width | 2.73–2.84×, median 2.77× |
| Card gap | 4–7 |
| Vertical | nav, active row title, ~250 row (≈46 % of 540), compact metadata/synopsis below, next row peeks; never two full rows |
| Row scroll | middle selections expand in place; scroll only as needed; neighbours visible; first/last approach the edges |
| Motion | expansion ≈200 ms (stable transitions 180–230; one clean sample 230–270 incl. settling); hero→browse ≈200 ms |

### Top 10
Present in the recording (≈00:77–00:80). Geometry not supplied. Nuvio has no factual ranking source: DATA-LIMITED.

### Search
| Property | Value |
|---|---|
| Arabic layout | keyboard right, results left; top nav persists |
| Keyboard | x 680–875, width 190–200, top 52–58, height 245–255; 6 columns of 30–32 keys; glyphs 14–16 sp |
| Query | small search icon + query text; no boxed input field |
| Results | 4 columns, posters 145–155 × 205–215, no title labels, 2 rows visible |

## States with NO evidence (NOT OBSERVED — do not implement)

Every state below needs a recording or a frame measurement before any layout work:

| State | Missing |
|---|---|
| Profile startup | all geometry, focus, Back |
| Shows destination | layout, filters/genre entry, rows |
| Movies destination | same |
| Genre/category discovery from Search | Entry point is Search (E3), list "on the left side" in LTR (E3). Tile layout, D-pad path, RTL: NOT OBSERVED |
| My Netflix | layout of Continue Watching / My List / Reminders, ordering, empty state |
| Continue Watching row | card geometry, progress treatment |
| Standard row title typography | size, weight (E1 gives geometry only) |
| Focused title metadata block | exact text sizes, line counts, callout styling |
| Title Detail (movie) | hero framing, metadata hierarchy, actions, related rows, scrolling |
| Series / season / episode detail | season selector, episode cards, progress |
| Play / My List / rating actions | sizes, order, focus |
| Settings entry route | where it lives (profile menu?), presentation |
| Back behaviour (every screen) | From Home content: top menu (E3). Back from Detail, from Search and from nested rows: NOT OBSERVED |
| Loading, empty and error states | all |
| Gradients/scrims | stop positions and alphas (E1 gives none) |
| Focus ring | width, colour, radius (E1: "thin light outline" only) |
| Typography | sizes/weights per role (E1 gives keyboard glyphs only) |

## How to close the gaps

1. Maintainer: record each NOT OBSERVED state on the TCL C6K (EN and AR when the UI differs), or allow the environment to reach help.netflix.com / about.netflix.com so official screenshots can be inspected.
2. Measure with the same rectification (quadrilateral TL (90,58), TR (749,91), BR (752,461), BL (79,469) for the current camera position) and add a table per state here.
3. Only then implement that state and add a geometry test against the table.
