# Nuvio design prototypes

This folder holds ten separate high-fidelity UI directions for Nuvio TV. They are built on one shared mock
catalog and one playback-intelligence engine. The prototype code is fully isolated from the production app:
no production code references it, and it references nothing in production.

## Launching

- **On the TV:** a second launcher tile, **Nuvio Prototypes**, appears next to Nuvio. It opens the Prototype Hub.
- **Over adb** (full debug build), you can jump straight to any concept, screen or language:

  ```
  adb shell am start -n com.nuviodebug.com/com.nuvio.tv.prototype.android.PrototypeActivity \
      --ei concept 10 --es screen PLAYER --es lang AR
  ```

  | Extra | Values |
  |---|---|
  | `concept` | `1`–`10`. If you leave it out, the Hub opens. |
  | `screen` | `HOME`, `FOCUSED`, `DETAILS`, `EPISODES`, `SEARCH`, `LIBRARY`, `STREAMS`, `PLAYER`, `SUBTITLES`, `AUDIO`, `PROFILE` |
  | `lang` | `EN` or `AR` (Arabic runs right-to-left) |
  | `illustrated` | `true` uses the built-in illustrated artwork only, so no network is needed |

## Remote shortcuts inside a concept

| Key | Action |
|---|---|
| Back (at a concept's first screen) | Return to the Hub |
| Menu / Info / Guide / Red | Open the quick switcher (pick another concept or screen) |
| `1`–`9`, `0` | Jump to concept 1–9, or 10 |
| Channel +/− or Page Up/Down | Next or previous concept |
| Green | Switch between English and Arabic |

## Structure

```
prototype/
  hub/        Prototype Hub, quick switcher, concept registry, hub thumbnails
  shared/     Language (Bi, RTL), focus and back helpers, navigation stack, icons, effects,
              illustrated artwork, mock catalog, StreamIntelligence, mock player state
  android/    Android-only glue: PrototypeActivity, fonts, Coil image loading, scroll pivot
  concept01/  … concept10/   One package per concept: Theme (tokens and primitives) plus screens
```

Every concept implements all eleven states: Home, Focused title, Details, Episodes, Search, Library,
Stream picker, Player, Subtitles, Audio and Profile. Every state works in both English and Arabic.

## Removing the prototypes

1. Delete `app/src/main/java/com/nuvio/tv/prototype/`.
2. Delete `app/src/main/res/font/proto_*.ttf`. This saves about 5 MB of bundled fonts.
3. Delete `app/src/main/res/values/prototype_strings.xml`.
4. Remove the `PrototypeActivity` entry from `AndroidManifest.xml`. It is marked with a comment.

No other file changed. The prototypes add no Gradle dependencies.

## Notes

- **Artwork:** loaded live from the Metahub image CDN by IMDb ID. Each title also has a procedural
  illustration that works as the placeholder and as the offline mode.
- **Blur:** used only on Android 12 and later. Older devices get tinted translucent panes instead.
- **Playback:** mocked. Positions tick in real time, and stream lists are generated from realistic source
  templates, so every stream picker and quality HUD exercises the same ranking logic
  (`shared/data/StreamIntelligence.kt`).
