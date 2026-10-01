# G12 audit — UI Styles and screensaver

Evidence for `tasks/G12_UI_STYLES.md` (IDs 188–205, 292). Official is the accepted baseline
`56aaba2` (D050). Official `dev` is at `5c1d9b0`, and its 22 changed files since `56aaba2` include
the home layouts: a global landscape poster mode touching `ClassicHomeContent`, `GridHomeContent`,
`ModernHomeRows`, `ContentCard`, `GridContentCard` and `LayoutHomeSettings`, plus `MainActivity`.

Pinned sources (SOURCE_MAP):
- xnucade/NuvioGlass `dev` @ `84098b7`, commits `09ade45` (Glass layout) and `5b1ab68` (glass hero
  badges);
- DavidVamaiotu/NuvioTV-Reshaped `0ccf049` (`ui/reshaped/pillnav`);
- Cxsmo-ai/NuvioTV-Custom `3e0d0fa` (`TopNavigation.kt`, `ScreensaverController`).

Legacy PRs #1 ("Cinema View") and #2 (design prototypes) are reference-only: ideas may be
harvested, but no code is merged from them. This audit changes no code.

## What official already has at `56aaba2`
- **Layouts (188–192):**
  - `HomeLayout` has CLASSIC, GRID and MODERN, picked on first run (`LayoutSelectionScreen`) and in
    Settings → Layout, per profile (`LayoutPreferenceDataStore.selectedLayout`; default MODERN).
  - An unknown stored name falls back safely.
  - The three layouts are kept unchanged. "Original" is official's own presentation as shipped,
    which this fork never removes.
- **Navigation (195):**
  - The legacy sidebar (`LegacySidebarScaffold`) and the modern sidebar (`ModernSidebarScaffold`,
    `modern_sidebar_enabled`).
  - The modern sidebar collapses to an icon pill (`CollapsedSidebarPill`, `isFloatingPillIconOnly`)
    and uses a Haze blur on Android 12+ when `modern_sidebar_blur_enabled` is on.
  - The profile avatar is in the sidebar (`ProfileAvatarCircle`).
  - `NuvioTopBar.kt` exists but nothing uses it.
- **Hero (201–203):**
  - `HeroCarousel` rotates hero items automatically: the first advance after 20 s, then at a fixed
    interval, used by Classic, Grid and Modern.
  - Modern has a hero trailer (`ModernHomeHero` + `TrailerPlayer`) and a full-screen hero backdrop
    setting (`modern_hero_full_screen_backdrop`).
- **Effects:**
  - Haze 1.7.2 is already a dependency (sidebar, stream screen).
  - Blur is gated by API level, but not by device memory or tier.
- **Missing:** no top navigation, no Glass layout, no clock, no screensaver, no liquid-glass effect.
- **Fork owners that touch this:**
  - `FeatureId.UI_STYLES` exists but is unregistered, so it is OFF.
  - G2's `AdaptiveResources` owns device tiers.
  - G9f's per-profile App Dimmer draws over the app.

## What the pinned forks add

| Source | What | Size |
|---|---|---|
| NuvioGlass `09ade45` | `HomeLayout.GLASS` with `usesModernPipeline` (Glass reskins Modern: same hero, rows and enrichment); `GlassScaffold` replaces the sidebar with top chrome that auto-hides on Home and returns on Up (`LocalGlassChromeReveal`, called by `ModernHomeRowsList` on its first row); `GlassNavPill` (frosted pill with a sliding indicator); `GlassClockPill`; `GlassSurface` (Haze blur, tint and bevel edge, falling back to an opaque tint without a haze state, with blur off, or below Android 12; blur off over video); `GlassBadge`; `GlassTokens` | about 900 lines of UI plus about 70 lines of hooks |
| Reshaped `0ccf049` | Top pill menu replacing the sidebar (`PillNavScaffold`, `PillNavigationBar`: sliding highlight, Up to reach it, Back to jump to it); AGSL liquid-glass refraction (`PillGlassShader`, `RuntimeShader`) only on Android 13+ with memory to spare and the blur setting on (`PillGlassBackdrop`) | 1294 lines |
| Cxsmo `3e0d0fa` | `TopNavigation.kt`: a full-width top bar with profile name / avatar and profile switching; `ScreensaverController`: an OLED idle dim overlay on a 1 Hz idle check, never during playback, a paused-to-playing transition wakes it, Compose dialogs pause it, and engaging it stops hero trailers; `ScreensaverOverlay` | 579 + 91 + 43 lines |
| Legacy #1 (reference) | Cinema View ideas: full-screen artwork of the focused title, trailer after a delay, ambient idle slideshow, clock | ideas only |

**Not inherited:**
- NuvioGlass fork identity: `applicationId`, updater, signing, About, build-root and `settings.gradle`
  changes.
- NuvioGlass's Calendar tab: G9e already owns Calendar.
- Reshaped's app-wide SharedPreferences flag: the setting becomes per profile.
- A second top bar implementation per source.
- Legacy #1 / #2 code.

## Decision: one UI-style owner, official layouts kept (D058)
- **Owner:** `fork/uistyle`. Glass tokens and surface, and one top-chrome scaffold with three looks
  (BAR, PILL, GLASS) instead of three top bars. The clock and the profile button are slots in that
  scaffold.
- **Sources:**
  - NuvioGlass FILE_PORT: surface, nav pill, clock, scaffold and focus reveal.
  - Reshaped FILE_PORT: pill behaviour and the AGSL refraction effect.
  - Cxsmo ALGORITHM_PORT: top-bar profile access, implemented on the same scaffold.
- **Official layouts kept:** Classic, Grid and Modern and both sidebars stay exactly as official
  ships them. The default stays Modern with the official sidebar.
- **Navigation style:** a new per-profile setting for Classic / Grid / Modern: Sidebar (official,
  default), Top bar or Pill. Glass layouts always use Glass top chrome.
- **New layouts:** `HomeLayout.GLASS` and `HomeLayout.CINEMATIC_GLASS`, both on the Modern
  pipeline (NuvioGlass's `usesModernPipeline`).
  - **Amended by D060 (G12b):** Glass (and Cinematic Glass) are navigation styles over official's
    Modern home instead of new `HomeLayout` values, so official layout code is not edited.
  - Cinematic Glass is Glass with the focused title's artwork full screen (official
    `modern_hero_full_screen_backdrop`) and the official hero trailer on. Those are the Cinema View
    ideas, built on official owners.
- **Effects, gated by capability through AdaptiveResources (199, 200):**
  - Liquid-glass refraction only on Android 13+ when the device tier allows it.
  - Haze blur on Android 12+.
  - A flat tinted surface otherwise, and always over video.
  - "Lightweight effects" is a setting that forces the flat surface on any device.
- **Screensaver (292):** Cxsmo's controller, off by default, per profile. It dims only when idle,
  never during playback, and the first key press only wakes the screen.
- **Flag:** `UI_STYLES` becomes AUTO with the first code slice. OFF hides every new entry, treats a
  Glass layout as Modern, the navigation style as Sidebar and the screensaver as off, and touches no
  official preference.
- **Focus (D-pad is correctness):**
  - The top chrome is reachable with Up from the first row and returns focus there with Down.
  - Back on a root screen focuses the chrome before exiting, as official does with the sidebar.
  - RTL mirrors the chrome.
  - The clock follows the device's 12 / 24-hour setting.
- **Pre-step:** official `dev` changed the home layout files G12 touches, so a dedicated upstream
  sync PR to `5c1d9b0` comes first (the D050 / #40 pattern).

## Feature map

| ID | Feature | Official | Decision |
|---|---|---|---|
| 188 | Multiple Home Layouts | Classic / Grid / Modern, per profile | verified_official |
| 189 | Original layout | official's layouts as shipped, never removed | verified_official |
| 190 | Classic layout | `ClassicHomeContent` | verified_official |
| 191 | Grid layout | `GridHomeContent` | verified_official |
| 192 | Modern layout | `ModernHomeContent` | verified_official |
| 193 | Glass layout | missing | `HomeLayout.GLASS` on the Modern pipeline → G12b |
| 194 | Cinematic Glass layout | missing | `HomeLayout.CINEMATIC_GLASS`: Glass with full-screen artwork and hero trailer → G12b |
| 195 | Modern sidebar navigation | `ModernSidebarScaffold`, blur toggle | verified_official |
| 196 | Top navigation | missing (`NuvioTopBar` unused) | top-chrome BAR look, navigation style setting → G12a |
| 197 | Glass top navigation | missing | top-chrome GLASS look (Glass layouts) → G12a |
| 198 | Pill navigation | collapsed-sidebar icon pill only | top-chrome PILL look (Reshaped) → G12a |
| 199 | Liquid-glass effect on capable devices | Haze blur on Android 12+ only | AGSL refraction on Android 13+, tier-gated → G12a |
| 200 | Lightweight fallback on weaker devices | API gate only | flat surface by tier, over video and by setting → G12a |
| 201 | Full-screen Hero | `modern_hero_full_screen_backdrop` | verified_official |
| 202 | Rotating Hero artwork | `HeroCarousel` auto-advance | verified_official |
| 203 | Hero trailer support | Modern hero `TrailerPlayer` | verified_official |
| 204 | Clock in top navigation | missing | clock slot in top chrome → G12a |
| 205 | Profile access in top navigation | profile avatar in the sidebar only | profile slot in top chrome → G12a |
| 292 | Optional screensaver | missing | Cxsmo controller, off by default → G12c |

## Slices
- **Sync:** an upstream sync PR to official `dev` `5c1d9b0` (D050 pattern), before any G12 code.
- **G12a** (196–200, 204, 205):
  - `fork/uistyle`: tokens, glass surface, effect policy (AdaptiveResources tier, API level,
    setting, over-video);
  - the top-chrome scaffold with the BAR / PILL / GLASS looks, clock and profile slots, and the Up /
    Back focus contract;
  - the per-profile navigation style and lightweight-effects settings; `UI_STYLES` AUTO.
  - Tests: effect policy across API and tier, navigation-style resolution with the flag on and off,
    clock format, focus contract helpers.
- **G12b** (193, 194):
  - `HomeLayout.GLASS` and `CINEMATIC_GLASS` with `usesModernPipeline`, chooser and settings
    entries, and the Cinematic preset;
  - the flag-OFF and unknown-name fallback to Modern.
  - Tests: pipeline selection, fallback and settings migration.
- **G12c** (292): the screensaver controller and overlay, per-profile setting and timeout; engaging
  it stops hero trailers.
  - Tests: idle, playback-active, dialog-focus and wake transitions.

D-pad focus on real remotes, effect cost on weak boxes, RTL on device and screensaver wake are
device checks (HV-G12-1..HV-G12-4, G14). No device run is claimed.
