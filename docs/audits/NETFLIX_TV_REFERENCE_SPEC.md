# Netflix TV reference spec (current top-navigation generation)

Status: PHASE 0 — EVIDENCE INCOMPLETE. Do not implement a state marked NOT OBSERVED.
Task: NETFLIX_THEME · Branch: feat/netflix-theme · Baseline: Test Build 76 (5cfbdd7)

## Evidence actually available to the implementing agent

| Source | Access | What it supports |
|---|---|---|
| E1 Maintainer frame measurement of a 102.533 s TCL C6K Netflix TV recording (30 fps, 3076 frames, 910×512, perspective-normalised to 960×540, ±4–6 dp), PR #100 comment 2026-10-03 | Received as numbers; the video itself was never visible to the agent | Home top state, Home browse state, category strip, focused expansion, Top 10 presence, Search + Arabic keyboard, Search results, motion timing |
| E2 Public press coverage of Netflix's May 2025 TV redesign (IBC, Engadget, MacRumors, Tom's Guide via web-search result text) | Search-result text only. about.netflix.com, help.netflix.com and press pages are blocked by this environment's network policy; no screenshot or video was seen | Navigation items and position; My Netflix contents; focused titles show richer info |
| Official Netflix Help Center / Tudum pages | NOT ACCESSED (egress blocked) | — |
| Interactive Netflix | NOT AVAILABLE | — |

Nothing in this document comes from memory of older Netflix UIs. The legacy left sidebar is not a reference.

## Structural facts (E2, textual)

- Navigation is a bar across the top of the screen, not a left sidebar.
- Items: Home, Shows, Movies, Games (where supported), My Netflix, plus Search and the profile.
- My Netflix combines Continue Watching, My List and Reminders.
- Moving across titles shows contextual information (genre, synopsis, length, rating) for each title without opening it; previews are larger.
- NOT CONFIRMED by any accessible source: Back returning to the top bar; categories surfaced from Search. These were stated in the maintainer brief; they need E1-style evidence before implementation.

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
| Genre/category discovery from Search | layout, entry point, D-pad path |
| My Netflix | layout of Continue Watching / My List / Reminders, ordering, empty state |
| Continue Watching row | card geometry, progress treatment |
| Standard row title typography | size, weight (E1 gives geometry only) |
| Focused title metadata block | exact text sizes, line counts, callout styling |
| Title Detail (movie) | hero framing, metadata hierarchy, actions, related rows, scrolling |
| Series / season / episode detail | season selector, episode cards, progress |
| Play / My List / rating actions | sizes, order, focus |
| Settings entry route | where it lives (profile menu?), presentation |
| Back behaviour (every screen) | target of Back from content, from Detail, from Search |
| Loading, empty and error states | all |
| Gradients/scrims | stop positions and alphas (E1 gives none) |
| Focus ring | width, colour, radius (E1: "thin light outline" only) |
| Typography | sizes/weights per role (E1 gives keyboard glyphs only) |

## How to close the gaps

1. Maintainer: record each NOT OBSERVED state on the TCL C6K (EN and AR when the UI differs), or allow the environment to reach help.netflix.com / about.netflix.com so official screenshots can be inspected.
2. Measure with the same rectification (quadrilateral TL (90,58), TR (749,91), BR (752,461), BL (79,469) for the current camera position) and add a table per state here.
3. Only then implement that state and add a geometry test against the table.
