# Netflix TV 2026 parity audit — mandatory implementation brief

Status: MANDATORY / OPEN
Task: NETFLIX_THEME
Branch: feat/netflix-theme
Audit date: 2026-10-03
Audited implementation commit: 6a8389a1e0fdc8fca140007fba68d17357ab597f
Audited visual workflow: Superfork Netflix Visual #3, run 37076381309
Authoritative project base at audit time: superfork/integration e07d684009bcb5b9c41d00c777b11b2318752c53

This document is the required design and verification baseline for the Netflix theme. Read it before changing the theme. Do not mark the theme Done, merge it, or describe it as current-Netflix parity until every P0/P1 acceptance item below is either implemented and verified or explicitly waived by the maintainer.

The objective is not to copy Netflix assets or proprietary implementation. The objective is to reproduce the current observable TV interaction model and visual hierarchy using Nuvio-owned code, Nuvio content and Nuvio data. Do not add Netflix logos, proprietary fonts, copied artwork, internal code, WebViews, a new playback engine, a second navigation owner, or a second data model.

---

## 1. Current Netflix TV reference — what is authoritative

The comparison target is the current Netflix TV experience, not the pre-2025 left-sidebar design.

Primary current sources:

1. Netflix Help Center — "An update to the Netflix TV experience and layout"
   - https://help.netflix.com/en/node/321880164349028
   - Current Help Center text says the new TV experience uses shortcuts to TV shows, movies, games, My Netflix and profiles in a menu at the TOP of the screen.
   - Back on the remote returns to the top menu.
   - My Netflix is the destination for My List, Continue Watching, watched/liked titles, reminders and more.
   - Categories is no longer a main-menu shortcut; category discovery is reached through Search.
   - New & Popular is no longer a main-menu shortcut; new/trending content appears in rows such as New on Netflix / Top 10.

2. Netflix Tudum — "A User Guide to Netflix's New TV Experience", 2025-05-19
   - https://www.netflix.com/tudum/articles/netflix-new-homepage-layout-user-guide
   - Confirms top navigation.
   - Confirms richer focus state before opening details: preview, runtime, rating and synopsis.
   - Confirms contextual callouts in title focus state, e.g. award/status/relevance labels.
   - Confirms responsive recommendations that react to actions such as rating, trailer viewing and My List changes.

3. About Netflix — "Unveiling Our Innovative New TV Experience", 2025-05-07
   - https://about.netflix.com/en/news/unveiling-our-innovative-new-tv-experience
   - Confirms information needed to choose is moved front and center.
   - Confirms Search/My List shortcuts moved from the old left side to the top.
   - Confirms more responsive real-time homepage recommendations.

Secondary / provisional source:

4. 2026 TV player experiment reported 2026-07-29
   - https://www.whats-on-netflix.com/news/first-look-netflix-new-tv-video-player-redesign/
   - Treat this only as a forward-looking A/B reference, NOT as the mandatory stable Netflix player baseline.
   - Do not redesign Nuvio playback around an unconfirmed experiment unless the maintainer explicitly approves it.

Reference policy:
- Official Netflix Help/Tudum/About sources outrank blogs and screenshots from old versions.
- When a visual reference conflicts with the current Help Center navigation description, use the current Help Center model.
- Re-check these sources before final merge if more than 30 days have passed.

---

## 2. Current Nuvio theme implementation — audit facts

At commit 6a8389a the Netflix branch is 5 commits ahead of the audited integration base and contains a large implementation, not a simple color skin.

The compare at audit time showed:
- 127 changed files
- approximately +5,955 / -578 lines
- dedicated Netflix home, search, details, tokens and TV instrumentation
- AppTheme.NETFLIX is a real selectable theme
- English LTR and Arabic RTL fixture capture exists
- 1080p and 4K visual jobs exist
- no Netflix proprietary logo/font/artwork is included by design

Major dedicated files already present include:
- ui/screens/home/NetflixHomePresentation.kt
- ui/screens/home/NetflixHomeRowsPolicy.kt
- ui/screens/search/NetflixSearchContent.kt
- ui/screens/detail/NetflixDetailHero.kt
- ui/theme/NetflixThemeTokens.kt
- androidTest/ui/theme/NetflixThemeTvTest.kt
- .github/workflows/superfork-netflix-visual.yml

Do not throw this implementation away. Reuse the existing presentation owners and refactor the architecture where the audit below requires it.

---

## 3. Executive parity assessment

This is a visual/interaction parity assessment, not a software quality score.

Current implementation is a hybrid of:
- old Netflix TV architecture: left sidebar + full-screen hero
- newer Netflix visual traits: dark cinematic surfaces, richer focus cards, metadata, large artwork

Approximate current-generation parity at the audited commit: about 50–55%.

Strong areas:
- black/white cinematic palette
- details hierarchy
- episode layout
- search foundation
- profile selector foundation
- Arabic RTL foundation
- genuine Nuvio actions rather than fake controls
- centralized theme tokens
- deterministic visual fixtures

Largest mismatch:
- navigation and home information architecture still resemble the older Netflix TV generation.

The highest-value redesign is architectural, not cosmetic.

---

## 4. P0 — navigation must match the current-generation TV model

### Current problem
The audited screenshots show a collapsed left rail and an expanded left sidebar. That is the old interaction model. Current Netflix Help explicitly documents top navigation.

### Required end state
For AppTheme.NETFLIX only:

1. Replace the Netflix-theme left navigation rail with a top navigation bar.
2. Keep other Nuvio themes unchanged.
3. Top-level destinations should map to Nuvio equivalents of:
   - Home
   - Shows / TV Shows
   - Movies
   - Search
   - My Netflix
   - Profile
   - Games only if Nuvio genuinely has a supported equivalent; do not add a dead tab.
4. Do not keep "Settings" as a Netflix-style primary content shortcut if it makes the bar unlike the current content-first model. Settings must remain reachable through profile/account/settings flow without breaking Nuvio usability.
5. Back behavior on Home should return focus to the top menu from deep row focus.
6. Returning from a child screen must restore the correct prior content focus where existing Nuvio navigation supports it.
7. Navigation expansion/collapse semantics from the old sidebar must not leak into Netflix theme.
8. Focus must be visually obvious without a huge permanent rectangular highlight.
9. Top navigation must respect safe-area margins on 1080p and 4K.
10. Top navigation must mirror correctly in Arabic RTL.
11. Do not duplicate the router. Implement this as presentation over existing destinations.
12. Do not change navigation semantics for Classic/Grid/Modern/Glass/Cinematic Glass themes.
13. Profile control should be visually integrated into the top bar.
14. Search should be reachable from the top bar in one deterministic D-pad path.
15. My Netflix should be a real destination, not merely a renamed My List label.

### Acceptance
- No left rail is visible anywhere in the Netflix theme.
- Home screenshot clearly shows a current-style top navigation model.
- Back from rows reaches top navigation.
- D-pad path is deterministic in LTR and RTL.
- Existing non-Netflix navigation regression tests remain green.

---

## 5. P0 — Home architecture must be redesigned around current title focus, not the old giant hero

### Current problem
Audited Home uses:
full-screen backdrop -> large title block -> metadata -> synopsis -> Play/More Info -> small rows.

That is visually polished but still follows the older Netflix hierarchy.

### Required end state
1. Keep immersive background art, but make the selected/focused title the central decision surface.
2. The focused title must expose enough information to decide without opening Details:
   - title/logo when available
   - short synopsis
   - runtime or season count
   - age/content rating when available
   - release/year when useful
   - match/relevance only if Nuvio has real data; never invent it
   - factual contextual callout when real data exists
3. The focused presentation must be spatially tied to the focused card/row, not feel like an unrelated hero several eye-travel zones away.
4. Hero-only catalogs can still use an immersive surface, but ordinary browsing should behave like focus-driven discovery.
5. Reduce dead/empty space.
6. Show fewer but larger/high-value cards where that matches the current TV feel.
7. Preserve actual Play, My List and Info actions.
8. Do not make More Info the only way to learn basic facts.
9. Short previews/trailers may play only through existing Nuvio trailer ownership and resource policy.
10. Trailer/audio policy must obey existing AdaptiveResources and low-memory behavior.
11. Focus change must not cause a visible layout jump.
12. The focused card must render above neighbors without reflowing the row.
13. Focus state must survive navigation away/back.
14. Delayed expansion must be deterministic and cancel cleanly on rapid D-pad movement.
15. Do not load multiple video previews simultaneously.
16. Continue Watching must not visually dominate the homepage merely because it is technically first.
17. Home rows should remain driven by actual Nuvio catalogs and personalization, not a hardcoded Netflix-like catalog.

### Contextual callout model
Support a generic Nuvio-owned callout slot that can render real facts such as:
- Top 10 / trending rank, only if data exists
- New season, only if metadata proves it
- Recently added, only if data exists
- Leaving soon, only if expiry data exists
- Award status, only if metadata exists
- Highly rated, only if backed by a real rating rule
- Because you watched X, only if Nuvio has a real recommendation reason

Never hardcode fake "Emmy Winner", "Highly Rewatched", "98% Match" or similar fixture text into production data.

---

## 6. P0 — My Netflix must be a hub, not just a My List label

Current Netflix uses My Netflix as a consolidation destination.

For Nuvio Netflix theme, design a hub backed by existing real data. It should aggregate what Nuvio actually owns, for example:
- My List / saved library
- Continue Watching
- recently watched / history if available
- reminders if Nuvio has them
- liked/rated content if Nuvio has it
- downloads only if Nuvio has a supported local download feature

Rules:
1. Do not invent unsupported sections.
2. Do not add backend dependencies solely to imitate Netflix.
3. Reuse existing library/watch-progress/account sources.
4. Preserve offline/failure states.
5. My Netflix must work without Trakt/MDBList credentials if local Nuvio library/progress exists.
6. Connected services may enrich the hub but must not become mandatory.
7. Arabic labels and RTL order must be native, not string-reversed.

---

## 7. P0 — known automated interaction failures from Visual run #3

The visual run collected all screenshots, but the instrumentation suite failed in BOTH English and Arabic.

At 1080p EN:
- 19 tests run
- 5 failures

At 1080p AR:
- 19 tests run
- 5 failures

The failing test names are the same in both locales:

1. playerChromeUsesTheExistingSeekCallbacksAndLogicalEpisodeTitle
   - assertion failure at NetflixThemeTvTest.kt line 440 in the audited artifact
   - the player screenshot in English was effectively blank/white, so do not consider player visual verification valid

2. deeplyScrolledSearchReturnsToAComposedResultAfterBack
   - focus restoration failure after deep search scrolling/back

3. savedSearchFocusWaitsForResultsAndKeepsTheKeyboardAvailableDuringAnError
   - timeout waiting for the intended search focus/error state

4. homeDwellPreviewRestoresItsAnchorAndAllowsRepeatedRemoteNavigation
   - timeout waiting for Home preview state

5. focusedLandscapeCardMovesWithoutChangingItsLayoutBounds
   - expected two state events but saw one; focus/interaction contract not satisfied

Do not weaken, delete, skip or quarantine these tests to get green.
Fix the implementation or make the test deterministic only when the test itself is proven incorrect.
Re-run all four visual matrix combinations after the fix.

---

## 8. Home hero / title block detailed visual checklist

Current audited Home is attractive but too close to the previous-generation Netflix home.

Required:
1. Top navigation visible.
2. Content title block not excessively small relative to 10-foot viewing distance.
3. Logo art preferred when real title logo exists.
4. Text title fallback must preserve the title's real script direction.
5. Metadata should form one concise line/group, not several visually weak fragments.
6. Synopsis line length must be controlled for TV readability.
7. Synopsis should generally be 2–3 lines max in focus state.
8. Genres should not compete with the primary metadata.
9. Primary Play button must be the strongest action.
10. Secondary actions must have lower visual weight.
11. Buttons must not use an old mobile-style compact rectangle.
12. Focused action must show high-contrast focus without overwhelming the background.
13. Side gradient must protect text readability over bright artwork.
14. Bottom gradient must blend selected title and rows.
15. Background should not become near-black so early that the artwork loses cinematic value.
16. Hero transition must avoid a one-frame wrong-title flash.
17. Focused-title change and backdrop change must be coordinated.
18. Trailer first frame must not cause a bright flash.
19. Low-memory tier should use static art instead of a degraded stuttering preview.
20. Preserve Nuvio trailer mute/unmute policy.

---

## 9. Rows and cards detailed checklist

Current screenshots show small landscape cards and a white focus border. Foundation is usable, but current Netflix uses more information-rich, larger focus states.

Required:
1. Default card ratio should remain 16:9 for Netflix-theme browse rows unless source art requires fallback.
2. Card width/spacing must be tuned for 10-foot readability.
3. Focused card should scale subtly, not aggressively.
4. Scaling must not shift the row or clip neighboring cards.
5. Focused card must draw over neighbors.
6. Border should be refined; current heavy white border reads more like a generic TV launcher.
7. Use elevation/scale/contrast/overlay together rather than relying only on a rectangular border.
8. Keep rounded corners modest.
9. Continue Watching needs a clear progress indicator.
10. Progress bar thickness and red accent should be consistent.
11. Watched/completed state must remain visible but not noisy.
12. Row title hierarchy should be stronger than card metadata.
13. Avoid showing source/add-on/debug labels in Netflix presentation.
14. Do not truncate first/last cards against safe margins.
15. Endless/load-more behavior must not reset focus.
16. New items inserted before a row must preserve the focused item's identity.
17. Return from Details must restore the same item and row viewport.
18. Rapid left/right must cancel pending expanded preview.
19. Long dwell should open/enrich only one focused preview.
20. Large catalogs must remain virtualized and bounded.

---

## 10. Expanded card / focused preview detailed checklist

Current expanded card is a useful implementation seam and should be kept, but it needs current-generation hierarchy.

Required:
1. Expanded state should feel attached to the selected card.
2. Artwork remains dominant.
3. Title/logo appears before secondary metadata.
4. Play is primary.
5. Add/Remove My List is secondary.
6. Info is tertiary.
7. If a thumbs/rating control is supported by Nuvio, it may appear only with a genuine backend/local action.
8. Runtime/year/rating should be readable at TV distance.
9. Contextual callout should have a dedicated slot.
10. Genres are secondary and limited.
11. Trailer preview should start only after dwell threshold.
12. Trailer must stop immediately when focus leaves.
13. Trailer must stop when screensaver engages.
14. Trailer must obey low-memory/lightweight mode.
15. No layout reflow when the preview expands.
16. Closing/dismissing must restore the original card.
17. Down/up navigation from preview actions must be deterministic.
18. RTL action order must be deliberate, not accidental mirroring that breaks semantic priority.
19. Do not invent IMDb/match values when absent.
20. If metadata is absent, omit the field cleanly instead of showing placeholders.

---

## 11. Search detailed checklist

Search is one of the stronger audited areas, but two focus tests currently fail.

Current-generation direction:
- Search is a top-level destination.
- Categories/genres can be discovered through Search.
- Search remains highly visual.

Required:
1. Top-nav Search entry.
2. Remote keyboard remains usable.
3. Query field must keep logical cursor/text direction.
4. Arabic search must use RTL text field behavior while mixed Latin titles remain readable.
5. Results update without destroying keyboard focus.
6. Result focus moves predictably between keyboard and grid.
7. Deep scrolling then Back must restore a composed result deterministically.
8. Search error must not hide or disable the keyboard.
9. Saved query/history focus must wait for results without a race.
10. Recent searches must be real and clearable.
11. Category/genre discovery should be available here if Nuvio has category data.
12. Results should use larger, visually consistent landscape/poster cards.
13. Grid must not jump when results stream in.
14. Search provider/add-on arrival must not reset the selected item.
15. Loading and zero-results states need Netflix-theme chrome, not generic white surfaces.
16. Keyboard focus indicator must meet contrast requirements.
17. Pressing Back from text entry should have a predictable hierarchy: keyboard/input -> search page -> top nav.
18. Voice search only if existing platform support exists; do not fake it.
19. Search must not issue unbounded requests per key press.
20. Debounce/cancellation must preserve responsiveness.

---

## 12. Movie details detailed checklist

The audited Movie Details is structurally strong.

Keep:
- cinematic art
- title/logo
- compact metadata
- synopsis
- real Play
- list action
- trailer action
- cast

Refine:
1. Reduce excessive dead space.
2. Raise visual density of useful information without clutter.
3. Keep title/logo hierarchy dominant.
4. Keep age rating compact.
5. Runtime/year/rating in one consistent group.
6. Genres below or adjacent based on available width.
7. Primary Play remains visually strongest.
8. My List state must show added/removed correctly.
9. Trailer button only if trailer exists.
10. Resume vs Start from Beginning must remain separate real actions.
11. Cast/director should not dominate the first viewport.
12. Missing metadata should collapse gracefully.
13. Backdrop/text contrast must be safe on bright art.
14. RTL alignment and mixed metadata order must be tested.
15. No quality/HDR/match badges unless known from actual selected media/source at the appropriate stage.

---

## 13. Series details detailed checklist

In addition to Movie Details requirements:

1. Show season count when known.
2. Show next episode context.
3. Resume should refer to the correct episode.
4. Start from beginning must be distinct from Resume.
5. Season/episode labels must use locale-aware formatting.
6. Do not let "S1 E2" reverse into confusing bidi order in Arabic.
7. Next episode artwork/title must be factual.
8. Season metadata must not imply availability from sources that have not been queried.
9. Details -> Episodes focus transition must be deterministic.
10. Return from Episodes must restore the previous Details action/focus.

---

## 14. Episodes detailed checklist

Episodes is one of the strongest current screens. Refine rather than rewrite.

Required:
1. Season selector remains obvious.
2. Season selector should use compact current-style chips/tabs.
3. Episode cards use landscape art.
4. Episode number + title hierarchy must be clear.
5. Duration visible when real.
6. Synopsis readable but compact.
7. Progress line clearly visible.
8. Watched/completed marker visible and not intrusive.
9. Current/next episode state visually distinct.
10. Focus card should not use an excessively heavy white box.
11. First/last episode reachability.
12. Row/vertical scroll must preserve focus.
13. Switching season must reset to a sensible episode and not stale focus.
14. Arabic episode numbering/title must preserve correct bidi.
15. Very long Arabic/English titles must ellipsize consistently.
16. Missing thumbnail should use a dark cinematic fallback, not a bright generic placeholder.
17. No layout jump as thumbnails load.
18. Episode selection must use existing playback/stream routing.

---

## 15. Profile selector detailed checklist

Current profile selector foundation is good but visually generic.

Required:
1. Centered composition appropriate to 10-foot UI.
2. Larger avatar targets.
3. Strong but tasteful focus state.
4. Profile name readable under avatar.
5. Add Profile lower emphasis than existing profiles.
6. Selected profile loads its saved Netflix theme.
7. Theme persistence must be profile-scoped and deterministic.
8. Switching profile must not leak prior profile Home focus/state.
9. Child profile presentation should remain compatible with existing Nuvio profile policy.
10. Arabic screen title and labels aligned correctly.
11. Avoid proprietary Netflix avatar assets.
12. Preserve Nuvio avatars/custom avatars.

---

## 16. Player controls detailed checklist

Important: the audited English player capture is not valid visual evidence because the visual suite failed and the captured frame was effectively blank/white.

Mandatory before visual approval:
1. Fix playerChromeUsesTheExistingSeekCallbacksAndLogicalEpisodeTitle.
2. Capture a real visible controls overlay in EN and AR at 1080p and 4K.
3. Confirm title/episode label.
4. Confirm elapsed/remaining or duration presentation.
5. Confirm seek bar.
6. Confirm focused seek behavior.
7. Confirm seek callback and commit callback both fire.
8. Confirm play/pause.
9. Confirm Next Episode where applicable.
10. Confirm audio/subtitle entry.
11. Confirm source/stream selector remains reachable if Nuvio supports it.
12. Confirm Back hierarchy.
13. Confirm controls auto-hide.
14. Confirm pause overlay.
15. Confirm subtitle timing/style surfaces.
16. Confirm skip intro/recap buttons.
17. Confirm post-play overlay.
18. Confirm HDR/DV/AFR/audio behavior is untouched by theme presentation.
19. Do not treat the July 2026 reported Netflix player A/B experiment as mandatory baseline.
20. If maintainer later requests that experiment, implement it as a separate explicit design decision.

---

## 17. Shared dialogs detailed checklist

Audited confirmation dialog is functional but too generic.

Required:
1. Netflix-theme surface only when AppTheme.NETFLIX.
2. Dark neutral surface, no bright Material default.
3. Title hierarchy clear.
4. Buttons have deterministic initial focus.
5. Primary/destructive action visually differentiated without unsafe ambiguity.
6. Cancel remains obvious.
7. Back dismisses and restores underlying focus.
8. Dialog width appropriate to TV, not phone-sized.
9. Safe margins at 1080p/4K.
10. Arabic button order follows deliberate RTL policy.
11. Long localized text wraps without clipping.
12. Existing non-Netflix dialogs remain unchanged.

---

## 18. Empty / error / loading states detailed checklist

Current empty/error screenshots are extremely sparse. They need theme polish without becoming noisy.

Empty:
1. Clear title.
2. One concise explanatory line.
3. Optional real CTA when an action exists.
4. Do not leave a tiny text island in a huge black canvas.
5. Keep accessibility focus target if CTA exists.

Error:
1. Human-readable error title.
2. Concise cause/retry guidance when safe.
3. Retry button with strong focus.
4. Do not expose raw URLs, provider secrets, tokens or stack traces.
5. Add-on/provider errors remain distinguishable from app errors where existing ownership supports that.

Playback loading:
1. Cinematic backdrop if available.
2. Current title/episode.
3. Progress/loading indication.
4. No fake numeric percent unless real progress is available.
5. Avoid near-invisible text.
6. Theme red accent may be used sparingly.
7. If playback fails, transition to the existing real error path.

---

## 19. Arabic / RTL — mandatory correctness list

Arabic is a first-class requirement, not a screenshot afterthought.

1. Top navigation mirrors appropriately.
2. Text alignment is RTL for Arabic strings.
3. Latin title/logo remains logically rendered.
4. Mixed metadata uses bidi-safe formatting.
5. Season/episode tokens remain readable in intended order.
6. Time/runtime tokens remain readable.
7. IMDb/rating labels do not become scrambled.
8. D-pad left/right follows visual direction consistently.
9. Search keyboard/grid transitions work in RTL.
10. Search query cursor direction correct.
11. Details action priority remains understandable after mirroring.
12. Expanded card action order deliberate.
13. Episode season tabs mirror.
14. Progress bars keep semantic start/end behavior expected by the component.
15. Navigation profile/search destinations remain reachable.
16. Dialog initial focus and button order correct.
17. Back/focus restoration same as English.
18. Long Arabic titles do not clip.
19. Arabic synopsis line-height must remain readable.
20. Do not manually reverse strings.

Observed audit note:
- the current Arabic captures are substantially better than naive mirrored UI, but mixed tokens such as season/episode metadata still need explicit bidi review.

---

## 20. Typography and spacing system

Do not use Netflix Sans or any proprietary font.

Use Nuvio/system fonts but approximate hierarchy through weight/size/spacing.

Required token groups:
1. top navigation label
2. hero/title fallback
3. section title
4. card title
5. metadata primary
6. metadata secondary
7. synopsis
8. contextual callout
9. button label
10. dialog title/body
11. episode title/body
12. player title/time

Rules:
- all values centralized in NetflixThemeTokens or an equivalent single owner
- no screen-local random font sizes
- line heights must be explicit where large Arabic text needs them
- safe margins centralized
- card gap centralized
- row vertical gap centralized
- action gap centralized
- focus scale centralized
- focus border/outline centralized
- motion durations centralized

---

## 21. Color / surface system

The existing black/white base is close.

Required:
1. primary background near-black, not pure-black everywhere when depth is needed
2. raised surfaces distinguishable in dark rooms
3. white primary text
4. muted secondary text still readable
5. restrained red accent
6. red should not become a universal focus color if it lowers visibility
7. primary focused Play may use light/white fill with dark content
8. secondary focused actions need a strong focus delta
9. errors use semantic styling but avoid bright full-screen red
10. progress red consistent across Continue Watching/player/loading where appropriate
11. scrims/gradients tuned against real high-luminance artwork
12. OLED/dark-room consideration: avoid large static bright areas when not necessary

---

## 22. Motion and focus behavior

Netflix-like feel depends heavily on motion, not just screenshots.

Required:
1. fast D-pad response
2. no delayed input queue that moves after the user stops
3. subtle focus scale
4. selected card z-order above neighbors
5. no row relayout during focus scale
6. dwell preview delay long enough to avoid accidental starts
7. dwell timer cancelled on every focus change
8. preview entrance smooth
9. preview dismissal restores exact card
10. backdrop crossfade without white/black flash
11. metadata transition tied to the same selected identity
12. repeated rapid D-pad motion remains stable
13. Back animation does not block focus restore
14. top nav focus transition does not scroll content unexpectedly
15. low-memory/lightweight mode reduces expensive animation
16. screensaver and trailer ownership remain compatible
17. app dimmer does not break contrast/focus
18. animation duration scale accessibility/system setting should be respected where Compose/platform behavior allows it

No TCL 60 fps claim may be made from emulator evidence.

---

## 23. Accessibility and 10-foot UI

Required:
1. readable from normal TV distance
2. focus visible on every actionable element
3. no action reachable only by color difference
4. meaningful content descriptions for icon-only controls
5. no decorative image announced as a control
6. logical focus traversal
7. Back semantics consistent
8. Arabic/English text scaling does not clip
9. dialog focus trap works
10. minimum target sizes appropriate for remote focus
11. high contrast against bright art
12. subtitles/player accessibility settings remain reachable
13. do not remove existing TalkBack/semantic labels while restyling
14. no auto-playing preview with uncontrolled loud audio
15. honor existing trailer mute policy

---

## 24. Performance / resource policy

The theme must remain viable on Android TV, including the TCL C6K and lower-memory tiers.

Required:
1. use existing AdaptiveResources as the sole device/resource policy
2. no second RAM detector
3. bound image decode/request sizes
4. one active preview video at most
5. cancel image/video work when offscreen
6. no full catalog precomposition
7. Lazy rows/grids remain virtualized
8. no unbounded bitmap retention
9. no per-card heavy shader in low-memory tier
10. no new animation dependency unless justified and approved
11. no redundant backdrop decode at 4K when a bounded decode is sufficient
12. use static fallback under constrained tier
13. test large catalogs
14. test rapid navigation
15. test repeated Home <-> Details navigation
16. test screensaver wake after theme use
17. hardware performance remains MANUAL-PENDING until TCL test
18. do not claim 4K "quality" merely because emulator dimensions pass

---

## 25. Data honesty rules

Never invent content facts for visual parity.

Production UI must omit rather than fabricate:
- match percentages
- rank
- awards
- "highly rewatched"
- "new season"
- expiry/leaving date
- runtime
- rating
- cast
- quality/HDR badge
- source availability

Fixture data may be synthetic only inside deterministic tests and must remain clearly test-only.

---

## 26. Screenshot / visual verification matrix

Current artifact capture has 17 images per locale at 1080p and a corresponding 4K matrix.

Required review surfaces:

1. Home hero/current focused title
2. Home rows
3. Focused card
4. Expanded card
5. Movie details
6. Series details
7. Episodes
8. Search
9. Navigation collapsed — remove/replace for Netflix theme after top-nav redesign
10. Navigation expanded — remove/replace for Netflix theme after top-nav redesign
11. Profiles
12. Player controls
13. Resume actions
14. Confirmation dialog
15. Empty My Netflix/List
16. Network error
17. Playback loading

For the redesigned current-generation theme, add explicit captures for:
18. Top navigation focused Home
19. Top navigation focused Search
20. Top navigation focused My Netflix
21. My Netflix hub
22. Contextual callout on a focused title
23. Low-memory static-preview fallback
24. Very long title
25. Missing logo/art fallback
26. Long Arabic title + mixed Latin metadata
27. Search error while keyboard remains usable
28. Return from Details preserving exact row/card focus

Each required surface:
- 1080p English
- 1080p Arabic
- 4K English
- 4K Arabic

The current workflow uses matching logical TV dp canvas at different physical pixel densities; still retain both resolutions to catch bitmap/decode/rendering issues.

---

## 27. Visual review rubric

For every screenshot, manually review and record PASS/FAIL for:

Layout:
- safe margins
- alignment
- balance
- dead space
- card density
- text width
- viewport clipping

Hierarchy:
- selected title dominance
- Play dominance
- metadata ordering
- row title ordering
- secondary action suppression

Focus:
- obvious focus
- no ambiguous two-focus state
- no clipped scale
- no row movement
- no focus loss

Typography:
- size
- weight
- line height
- ellipsis
- mixed-script behavior

Artwork:
- crop
- decode quality
- gradient readability
- fallback
- no stretch

Motion:
- entry
- exit
- dwell
- cancel
- restore
- repeated D-pad

RTL:
- mirrored placement
- logical token order
- focus direction
- button order
- search transitions

Do not replace manual visual review with screenshot existence.

---

## 28. CI / build gates before merge

Theme is not mergeable until all are true:

1. Governance validator green.
2. PR-scope checks green.
3. Full unit-suite baseline guard green: no new failures beyond registered debt.
4. assembleFullDebug green.
5. Netflix TV instrumentation green in English and Arabic.
6. Visual workflow green for all 4 matrix jobs.
7. No missing screenshots.
8. No unexpected screenshot dimensions.
9. Visual review record updated from PENDING with human-reviewed results.
10. Superfork Device Smoke green.
11. Signed Superfork Test Build produced from the exact reviewed head.
12. Theme install tested on TCL C6K.
13. D-pad/navigation feel tested on TCL C6K.
14. No regression in playback, HDR/DV/AFR/audio/subtitles/streams from presentation changes.
15. Do not close G14/feature 3/320 merely because the theme is green.

---

## 29. Current screenshots — audit observations

### Home hero / rows
- Good cinematic darkness and readable title block.
- Too much empty space compared with the current focus-driven Netflix hierarchy.
- Current navigation absent from the top because implementation is still left-rail based.
- Hero and rows are visually separated too strongly.
- Home hero and Home rows captures currently hash-identical in EN and AR, indicating the capture does not demonstrate a meaningful rows-state difference; update fixture/capture so the intended row state is visibly distinct.

### Focused card
- Current card scale/border demonstrates focus but feels generic.
- Needs current-generation richer focus presentation.
- Instrumentation failure confirms behavior is not stable yet.

### Expanded card
- Good real actions.
- Metadata foundation useful.
- Needs better current-style context/callout hierarchy and less box-like treatment.

### Movie/series details
- Strongest visual area together with Episodes.
- Keep architecture and refine spacing, density, metadata grouping and focus style.

### Episodes
- Strong structure.
- Refine focus box, season tabs, spacing and Arabic bidi.

### Search
- Good structural match for TV search.
- Needs current top-nav integration.
- Two current search/focus tests fail and must be fixed.

### Navigation
- Largest architectural mismatch.
- Left rail must be removed for Netflix theme in favor of current top navigation.

### Profiles
- Functional and clean, but generic.
- Needs stronger cinematic scale/focus while preserving Nuvio avatars.

### Player
- Current visual evidence invalid.
- Must be re-captured after test fix.

### Dialog
- Functional but generic Material-style surface.
- Needs Netflix-theme TV proportions and focus polish.

### Empty/error
- Too sparse/tiny for 10-foot UI.
- Add balanced layout and useful CTA where real.

### Loading
- Direction is usable.
- Improve hierarchy/visibility and ensure no fake progress.

### Arabic
- Overall mirroring is real and useful.
- Explicitly fix/test mixed LTR tokens and action order.

---

## 30. Implementation order — do not polish the wrong architecture

P0 order:
1. Fix the five failing interaction tests enough to establish a stable baseline.
2. Replace Netflix-theme left sidebar with top navigation.
3. Add/implement My Netflix hub using existing Nuvio data.
4. Refactor Home into current focus-driven discovery.
5. Make list/card/autoplay/trailer transitions stable under D-pad.
6. Fix Search focus restoration/error-state races.
7. Fix/re-capture Player controls.

P1 order:
8. Refine focus card/expanded card.
9. Add factual contextual callout slot.
10. Refine Movie/Series Details.
11. Refine Episodes.
12. Refine Profiles.
13. Refine dialogs/empty/error/loading.
14. Complete RTL polish.

P2:
15. Motion fine-tuning.
16. Performance tuning from hardware evidence.
17. Optional player experiment only if explicitly approved.

Do not spend time perfecting old left-rail pixels if the rail will be removed.

---

## 31. Done definition for the Netflix theme

The task is Done only when:

- It visibly follows the current Netflix TV generation documented by current official Netflix sources.
- Netflix-theme primary navigation is top navigation, not old left sidebar.
- My Netflix is a real Nuvio-backed hub.
- Home is focus-driven and information-rich.
- No fake facts are shown.
- Every genuine action still reaches the existing Nuvio owner.
- EN/AR and 1080p/4K matrices are green.
- All theme instrumentation is green with no skipped failures.
- Manual visual review is recorded.
- Exact-head full CI is green under the existing baseline debt policy.
- Exact-head signed Test Build exists.
- TCL C6K test is completed for focus/navigation/performance.
- Existing Nuvio themes are unchanged.
- Existing G14 governance and hardware blockers remain truthful.
- No Netflix proprietary asset/font/code is included.

Until then, status remains IN_PROGRESS.

---

## 32. Mandatory next-agent instruction

When an agent receives "اشتغل على نوفيو" and sees NETFLIX_THEME active/in progress:

1. Read this file in full.
2. Refresh feat/netflix-theme from the current integration branch without losing G14 fixes.
3. Re-check the official Netflix reference sources above for material UI changes.
4. Do not work from memory of the old Netflix sidebar UI.
5. Fix P0 architecture before cosmetic polishing.
6. Preserve existing Nuvio owners and all real playback/library/search actions.
7. Run the exact verification matrix.
8. Update this audit with evidence, not claims.
9. Leave screenshots/artifact/run IDs in docs.
10. Do not merge until the maintainer has reviewed the visual result.

