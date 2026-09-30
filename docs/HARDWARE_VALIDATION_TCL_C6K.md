# Hardware Validation Checklist — TCL C6K (Android TV)

One checklist for every MANUAL-PENDING item from G1–G4. Nothing in this file has been run yet.
Record each result as **PASS**, **FAIL** or **MANUAL-PENDING** (not run), with the evidence asked
for. Never mark a row PASS from memory or expectation. Copy finished rows into
`docs/MANUAL_TEST_LOG.md` using its entry template. **This checklist does not block G5 or later development. It is part of the batched final hardware-certification campaign and must be completed before Stable release.**

> **ملخص بالعربي:** ثبّت نسخة fullDebug، فعّل ADB عبر الشبكة، ثم نفّذ الأقسام 1–6 بالترتيب.
> كل بند نتيجته PASS أو FAIL أو MANUAL-PENDING فقط، مع الدليل المطلوب (صورة للـHUD أو سطر من
> logcat أو رقم مقاس). القسم 5 (مقارنة A/B على نفس الملف) يبقى شرط التحقق النهائي من G4، لكنه لا يوقف تطوير الـGates التالية؛ سننفذ الاختبارات اليدوية كلها في حملة واحدة قبل الإصدار المستقر.

## 0. Setup (once)

1. **APK:** open the latest green *Superfork CI* run on `superfork/integration` (Actions tab), job
   *Superfork Full Debug CI*, artifact *APK*. Install it with `adb install -r <apk>`. The debug app
   id is `com.nuviodebug.com`, so it installs next to any release build.
2. **ADB over the network:** on the TV, go to Settings → System → About and press *Android TV OS
   build* 7 times. Then go to Settings → System → Developer options and turn on *USB debugging*
   (network ADB on Google TV). From a computer on the same network, run
   `adb connect <tv-ip>:5555` and accept the prompt on the TV.
3. **Log capture:** keep this running during every test and save it per test:
   ```
   adb logcat -v time PlayerViewModel:I PlayerMediaSource:I NuvioPrewarm:I ParallelRangeDS:I \
     MatroskaExtractor:W SeekReadAhead:I *:S | tee hv_<test-id>.log
   ```
   The logs contain hosts only (no tokens or full URLs), but check before sharing them.
4. **HUD:** go to Settings → Advanced and turn on *Playback stats overlay*. In the player, open
   stream info and use the stats button. The rows used below are `strategy`, `conn`, `rebuffer`,
   `underrun`, `load err`, `video`, `hdr`, `display`, `audio` and `output`.
5. **Test files.** Pick them once and reuse the same links for every run. Write the source,
   size and container of each in the log.

   | ID | File | Needed for |
   |---|---|---|
   | F1 | 4K MKV REMUX from an HTTP/debrid source, ≥ 20 GB or "REMUX" in the name | A/B, warm-up |
   | F2 | 1080p MKV, 2–10 GB, HTTP/debrid | Seek optimized ring |
   | F3 | Progressive MP4, ideally non-faststart (moov at the end) | MP4 session |
   | F4 | An HLS stream (.m3u8) | strategy fallback |
   | F5 | A title whose source list has a dead link (HTTP 404/410) above a working one | failover |
   | F6 | An MKV with missing Usenet articles (zero-filled holes), if you have one | MKV resync |

6. **Network:** use the same network for every run (wired or Wi-Fi, not both). Note it.

## 1. G1 — Diagnostics (IDs from G1a–G1c)

| Test | Steps | Expected | Result | Evidence |
|---|---|---|---|---|
| HV-G1-1 | Settings → Add-ons. Move the D-pad over each add-on. | The health badge (Healthy / Slow / Timeout / …) is readable, and focus never lands on or skips because of the badge. | MANUAL-PENDING | photo |
| HV-G1-2 | Play F1 with the HUD open. Compare `display`, `output` and `hdr` with the TV's own info (the TV info key, or Settings → Display). | The refresh rate matches the TV. `output` says passthrough only when the TV/AVR really receives bitstream. `hdr` says Dolby Vision only when the TV shows DV. | MANUAL-PENDING | HUD photo + TV info photo |
| HV-G1-3 | Settings → Advanced → *Device assessment*. Move focus through the card, choose *Apply recommended settings*, then *Revert assessment changes*. | Every row is focusable. Apply changes only the listed settings. Revert restores the previous values (check one value before and after). | MANUAL-PENDING | photos before / after / reverted |

## 2. G2 — Adaptive resources

| Test | Steps | Expected | Result | Evidence |
|---|---|---|---|---|
| HV-G2-1 | Cold start. Scroll the Home rows quickly for 2 minutes, open 5 detail pages, go back. | No crash, no out-of-memory restart, posters load. | MANUAL-PENDING | `adb shell dumpsys meminfo com.nuviodebug.com` TOTAL before/after |
| HV-G2-2 | Play F1 with the *Official* strategy. Measure time to first frame, then do 3 seeks (+10 min, −5 min, +30 min). | Plays and seeks without an out-of-memory crash. Note the first-frame time and each seek time. | MANUAL-PENDING | times + log |
| HV-G2-3 | Open a title with many sources. Scroll the source panel fast. Switch the app language to Arabic and repeat. | Focus moves smoothly. Arabic text and RTL layout are correct, with no clipped or mirrored numbers. | MANUAL-PENDING | photos EN + AR |

## 3. G3 — Playback strategies

| Test | Steps | Expected | Result | Evidence |
|---|---|---|---|---|
| HV-G3-1 | Settings → Advanced → *Playback strategy*. Move focus over all five options and select each one. | Every option is focusable, the selection sticks after leaving and re-entering, and each profile keeps its own choice. | MANUAL-PENDING | photos |
| HV-G3-2 | For each strategy (Official, REMUX / Throughput, Seek optimized, Low memory, Auto), play F1 and read the HUD `strategy` row and the `PLAYBACK_STRATEGY:` log line. | Official → `official`. REMUX → `remux`. Seek → `seek`. Low memory → `low_memory`. Auto on F1 → `auto → remux (large file)`; if the TV is low-RAM, `auto → low_memory (low RAM)`. | MANUAL-PENDING | HUD photo per strategy |
| HV-G3-3 | Choose REMUX / Throughput and play F4 (HLS). | HUD `strategy` shows `remux → official (stream type)`, and playback is normal. | MANUAL-PENDING | HUD photo |

## 4. G4 — Functional checks

| Test | Steps | Expected | Result | Evidence |
|---|---|---|---|---|
| HV-G4-1 dead-source failover (19) | Play the dead link from F5. | Instead of an error screen, the loading text shows "Source unavailable — trying next source (1/3)", and the working source plays. The log has `Dead source (...) - failing over to next source (1/3): host=...`. | MANUAL-PENDING | log lines + photo |
| HV-G4-2 failover cap | If you have a list with 4+ dead links in a row, play the first. | After 3 failovers the normal error screen appears (no endless loop). | MANUAL-PENDING | log |
| HV-G4-3 startup watchdog (20) | Play a stream that never starts (a very slow host, or a codec the TV cannot decode). Wait up to 60 s. | Within 20–60 s the spinner is replaced by a clear error, and the log has `STARTUP_WATCHDOG: ... surfacing error` with a reason. If a frame arrives late, the error disappears by itself. | MANUAL-PENDING | log + photo |
| HV-G4-4 warm-up (18) | Choose REMUX / Throughput. From the source list, press play on F1. | The log has `PREWARM host=... tail=...`, followed by `PREFETCH_WINDOW head hit`. With the Official strategy there is no `PREWARM` line. | MANUAL-PENDING | log |
| HV-G4-5 MKV resync (21) | Play F6 (any strategy) and watch through the damaged region. | Instead of stopping with a parser error, playback jumps briefly and continues. The log has `MKV_RESYNC: skipped malformed data near byte ...`. | MANUAL-PENDING (needs F6) | log |
| HV-G4-6 MP4 session (24) | Choose *Seek optimized* and play F3. Do 5 seeks, including one near the end. | The log has `SEEK_OPTIMIZED: MP4_SESSION`, seeks work, and nothing freezes. Note each seek time. | MANUAL-PENDING | log + times |
| HV-G4-7 read-ahead ring (25) | Choose *Seek optimized* and play F2. Let it run 3 min, then seek forward 1–2 min (inside the read-ahead) and back. | The log has `SEEK_OPTIMIZED: READ_AHEAD` and `SEEK_READ_AHEAD: on, capacity=...MB` (256 MB if the TV is in the constrained tier, 512 MB on standard, off on low-RAM). Seeks inside the ring are near-instant. | MANUAL-PENDING | log + times |
| HV-G4-8 ring cleanup | Exit the player after HV-G4-7, then run `adb shell run-as com.nuviodebug.com ls cache/nuvio_seek_read_ahead`. | The directory is empty or missing (the ring file is deleted on exit). | MANUAL-PENDING | command output |
| HV-G4-9 no stacking (D006) | Choose REMUX / Throughput and play F2. | The log has **no** `SEEK_READ_AHEAD` or `SEEK_OPTIMIZED` line. HUD `conn` shows several connections. | MANUAL-PENDING | log + HUD |

## 4b. G5 — Audio / DV / HDR / AFR

| Test | Steps | Expected | Result | Evidence |
|---|---|---|---|---|
| HV-G5-1 soft clip (48) | Settings → audio amplification to +10 dB. Play a loud action scene (explosions) with PCM output (not passthrough). Repeat with the amplification at 0. | At +10 dB peaks sound compressed, not crackly or distorted; at 0 dB the sound is unchanged from official. | MANUAL-PENDING | short note / recording |
| HV-G5-2 lossless default (37) | Settings → Audio → turn on *Prefer lossless audio*. Play a remux with TrueHD (or DTS-HD MA) and AC3 tracks in your language, never opened before. Then turn the setting off and play another such title. | On: the audio menu shows the TrueHD/DTS-HD track selected, and a commentary track is never picked. Picking another track by hand is remembered next time. Off: the same track official picks today. | MANUAL-PENDING | audio menu photo on / off |
| HV-G5-3 per-format passthrough (38) | Settings → Audio: leave every *Receiver decodes …* switch on and play a DTS-HD (or TrueHD) title; note the HUD `output` row and the TV/AVR format. Turn *Receiver decodes DTS-HD / DTS:X* off and play it again. Also try it with *Force optical passthrough* on. | All on: the same output as official (bitstream when the chain reports it). DTS-HD off: HUD `output` shows PCM, sound plays in 5.1/7.1 on the AVR (not folded to stereo), the log has `AUDIO_PASSTHROUGH_POLICY: decode DTS_HD active=true`; other formats still bitstream. With force-optical on the switch has no effect (`active=false`). | MANUAL-PENDING | HUD photos + log |
| HV-G5-4 audio diagnostics (39–42) | With the HUD open, play F1 (lossless track) and a stereo AAC title. Compare the `chain` row with Settings → Display & Sound → Audio output (surround mode and formats) and the AVR display. | `chain` lists the formats the TV reports, the surround mode, and the PCM channel count. `audio` shows the source channels (and bitrate only when the stream reports it). `output` shows passthrough, or PCM with the output channel count. None of the rows is invented, and a missing value is left out. | MANUAL-PENDING | HUD photos + TV settings photo |
| HV-G5-5 DV conversion fixes (52) | Settings → Dolby Vision: *Convert to DV8.1* with *Preserve DV mapping* on. Play a DV profile 7 remux (MKV) and, if you have one, a DV7 MP4. Watch 5 min and seek twice. | The TV shows Dolby Vision (not HDR10) and there is no colour flashing. The log has no `DV7_MKV: RPU conversion failed` or `DV7_NATIVE:` drop lines on a healthy file. If drops appear, the picture stays stable (HDR10 base layer) and the log counts them. | MANUAL-PENDING | TV info photo + log |
| HV-G5-6 HDR10 metadata on DV strip (54) | Settings → Dolby Vision: *Strip DV* (or HDR10 base layer), then turn on *Add HDR10 metadata when removing Dolby Vision*. Play a DV 8.1 MKV. Repeat with the switch off. | On: the TV reports HDR10, the HUD `dv` row ends with `HDR10 SEI added` (or the log says `skipped: base layer already carries HDR10 SEI`), and bright highlights look like the DV version. Off: official behavior. | MANUAL-PENDING | TV info photo + HUD + log |
| HV-G5-7 DV stream info (52, 53) | With the HUD open, play a DV profile 7 FEL remux, a DV7 MEL remux and a DV8 WEB-DL. | The `dv` row shows `FEL`/`MEL` for profile 7, and `MaxCLL … · MDL ~… nits` values that match MediaInfo for the file (a `-` means the file does not say). | MANUAL-PENDING | HUD photos + MediaInfo |
| HV-G5-8 display output (53, 55, 60) | With the HUD open, play F1 (4K HDR) with resolution matching on, then a 1080p SDR title. Turn *True-black letterbox* on and play a 2.39:1 HDR title. | `display` shows the refresh rate and the mode size the TV switched to (e.g. `23.976 Hz · 3840x2160`). `tv hdr` lists what the TV supports (DV/HDR10/HLG/HDR10+). With true-black letterbox on, the bars are true black (not grey) in HDR. | MANUAL-PENDING | HUD photos + photo of the bars |
| HV-G5-9 23.976 / 24 matching (56) | AFR on (Start). Play a 23.976 fps film, a true 24.000 fps title and a 25 fps title, each from the TV at 60 Hz. | The TV info shows 23.976 Hz, 24 Hz and 50 Hz respectively (or a clean multiple); HUD `display` matches. | MANUAL-PENDING | TV info photos |
| HV-G5-10 track-format AFR fallback (58, 59) | AFR on (Start), ExoPlayer. Play F3 (a non-faststart MP4 whose probe fails; the log has no `AFR preflight: ... FPS` detection). Then play it a second time. | First play: the log has `TRACK_AFR: raw=...` and, when the mode changed, `holding start 2000ms`; the picture starts after the switch, not during it, and never later than ~8 s. Second play: the official preflight hits the cache (`AFR preflight: cache hit`). A title under 20 fps (an error stub) never switches the panel. | MANUAL-PENDING | log + stopwatch |

## 4c. G6 — Subtitle Intelligence

| Test | Steps | Expected | Result | Evidence |
|---|---|---|---|---|
| HV-G6-1 stream subtitle reference (84) | Settings → Subtitles → *Auto Sync Subtitles* on. Play a title from a stream that ships its own subtitles but whose file has **no** embedded subtitle track; pick an add-on subtitle that is visibly out of sync. Repeat with a file that has embedded subtitles. | Without embedded subtitles the log has `AUTO_SYNC_V2 stream subtitle reference usable=1/…` (or 2) and the subtitle is shown "Auto synced" in time, or the official failure toast when no confident fit exists. With embedded subtitles the line is absent (official path). | MANUAL-PENDING | log + photo |
| HV-G6-2 Arabic AutoSync (94) | App language Arabic. Open Settings → Subtitles, turn Auto Sync on, then play an Arabic add-on subtitle against an English embedded track. | The Auto Sync settings, the failure toasts and the "Auto synced" label are in Arabic; the Arabic subtitle is retimed on timing alone. | MANUAL-PENDING | photo |
| HV-G6-3 custom subtitle font (100, 102, 104) | Settings → Subtitles → *Subtitle font* → *From URL*: import an HTTPS .ttf (for example an Arabic Naskh font). Play an SRT subtitle with ExoPlayer, then with MPV. Then import an `http://` link and a non-font file. Reset to default. | Both players show the imported font; the row shows its family name. The `http://` link and the non-font file are refused with a message and the current font stays. After reset both players use the official font. | MANUAL-PENDING | photo |
| HV-G6-4 phone upload via QR (101) | Open *Subtitle font*, scan the QR code with a phone on the same Wi-Fi, send a .otf. Then close the dialog and reload the page on the phone. | The TV shows "Font imported: <name>" and uses it. After the dialog closes the page no longer loads (server stopped); a request without the token path gets 403. | MANUAL-PENDING | photo + phone screenshot |
| HV-G6-5 Arabic cinema preset (105) | Settings → Subtitles → *Arabic cinema preset*. Play an Arabic subtitle over a bright and a dark scene. | Size, bold, outline and offset change once and show "Applied"; Arabic text with diacritics is readable on both scenes and no line is clipped at the bottom. Changing any value removes "Applied". | MANUAL-PENDING | photo |

## 4d. G7 — Seek Intelligence

| Test | Steps | Expected | Result | Evidence |
|---|---|---|---|---|
| HV-G7-1 on-device previews (107, 108) | ExoPlayer, *Generate previews on device* on, no Seekr key. Play F1 for 10 minutes, pause, then scrub back over the watched part and forward past it. | Over the watched part the thumbnail shows real frames; past it the nearest one appears blurred. Playback shows no judder while previews are collected; the log has no `NuvioLocalPreviews` errors. | MANUAL-PENDING | photo + log |
| HV-G7-2 keyframe-exact seek (109) | Scrub to a thumbnail over the watched part and press OK. | Playback resumes on the frame the thumbnail showed (within one keyframe), without a visible jump after the seek. | MANUAL-PENDING | video |
| HV-G7-3 Seekr fallback and Preview Sync (110, 112) | Settings → Playback → *Seekr API key*: enter an invalid key, then a valid one. Play a popular film not watched before, scrub ahead, open the Preview Sync button and nudge. | The invalid key is refused; the valid one is saved (row shows "Your own key"). Unwatched parts show Seekr thumbnails; Preview Sync moves them. | MANUAL-PENDING | photo |
| HV-G7-4 memory and disk bounds (115, 116) | With the HUD open, scrub through a 2-hour 4K remux, then play three more titles. Check Settings → Apps → Nuvio storage. | No playback stall or app restart; on a device under 3 GB the 4K stream gets no local previews (log `no software decoder` or size skip); cache stays under the tier cap. | MANUAL-PENDING | log + storage screenshot |
| HV-G7-5 automatic Seekr calibration (111, 113, 114) | Seekr key set. Play a release whose Seekr thumbnails are visibly early or late (e.g. an extended cut). Watch 15 minutes, then pause and scrub. Repeat with a dark, static title. | The log has `SeekrCalibration: ... accepted=true` and the Seekr thumbnails now match the frames; Preview Sync shows the applied offset. On the dark title the log shows `accepted=false` and the offset stays 0. A value set by hand in Preview Sync is never changed. | MANUAL-PENDING | log + photo |

## 4e. G8 — Stream Intelligence

| Test | Steps | Expected | Result | Evidence |
|---|---|---|---|---|
| HV-G8-1 progressive AIOStreams (147, 149, 150) | Install an AIOStreams add-on whose manifest URL has `client=nuvio-progressive` (an AIOStreams build with the Nuvio progressive endpoint). Open the streams of a popular film. | Streams appear in the AIOStreams group within seconds and the group grows in place as slower sources finish (no duplicate rows, no second AIOStreams group); the log has no `ProgressiveAioStreams` fallback line. | MANUAL-PENDING | video + log |
| HV-G8-2 progressive fallback (151) | Same add-on URL against an AIOStreams build without the endpoint (or with the network cut mid-load, then restored). | The log shows `progressive unavailable` or `progressive failed host=<host>` (host only, no path or query) and the ordinary AIOStreams list appears as in official. | MANUAL-PENDING | log |
| HV-G8-3 bounded add-on retry (289) | An add-on that answers 503 or times out once (e.g. a self-hosted add-on restarted during the request). | The log shows `Retrying stream request once host=<host>` at most once per add-on per load and never for a 4xx; a second failure shows the add-on error as in official. | MANUAL-PENDING | log |
| HV-G8-4 Best-quality autoplay and list order (155, 156, 158–161, 163, 164, 168) | Settings → Playback → stream selection: *Best quality*, then turn on *Sort streams by quality*. Open a popular film with cached 4K REMUX, 1080p WEB-DL and uncached results. | The list starts with cached 4K REMUX from a known group; uncached results are still listed, at the bottom. Autoplay starts the first stream of that list. With both options off the list and autoplay match official exactly. | MANUAL-PENDING | photo + video |
| HV-G8-5 DV ranking follows the display (162) | TCL C6K (DV display): open a title with DV and HDR10 releases of the same quality. Repeat on an HDR10-only display or with the TV's DV mode unavailable. | On the DV display the DV release is first; on the HDR10-only display the HDR10 (or DV + HDR) release is first and a DV-only release is at the bottom of its tier. | MANUAL-PENDING | photo |
| HV-G8-6 next episode with Best quality (168) | Best quality on; play episode 1 of a series to the end. | The next episode starts with the stream the ranked list shows first for it (same engine as the stream screen). | MANUAL-PENDING | video |
| HV-G8-7 connection fit (165) | Best quality and *Sort streams by quality* on. Play two different HTTP streams for 30 s each (learning). Then cap the router or TV to ~20 Mbps and open a title with a 60+ GB REMUX and a ~15 GB WEB-DL. | After learning, the REMUX the capped link cannot sustain sits below the WEB-DL in the list and autoplay picks the WEB-DL; the order does not change while the list is open. Uncapped, the REMUX is first again after the next two playbacks. | MANUAL-PENDING | photo + video |

## 4f. G9 — Skip / Recommendations / Discovery

| Test | Steps | Expected | Result | Evidence |
|---|---|---|---|---|
| HV-G9-1 extra skip sources (117, 124–126, 134) | Settings → Playback → skip: turn on SkipMe.db and TheIntroDB (and PublicMetaDB with a key). Play a popular series episode and a film; repeat one series episode opened from a TMDB-id catalog. | The skip button appears for intro / credits as before; with only the official sources on, times match official exactly. The TMDB-opened episode now gets skip segments. Keys never appear in the log. | MANUAL-PENDING | photo + log |
| HV-G9-2 slow source isolation (131) | With the extra sources on, block one provider host on the router (or cut the network for 5 s at start). | Skip segments from the remaining sources still appear within about 6 s; the log shows `ForkSkip: <provider> timed out`; playback never waits for skip data. | MANUAL-PENDING | log |
| HV-G9-3 previews, content warnings and mute (120–122, 127–129) | Turn on MovieHavenDB, VideoSkip and NotScare plus the Previews, Jump scares and Profanity categories. Play a horror film listed on notscare.me and a title with a profanity mute entry. | "Skip jump scare" appears a few seconds before each listed scare and does nothing unless pressed; a profanity mute segment silences only its span and the sound comes back (volume and amplification unchanged); no mute or warning segment reaches an external player. With the categories off nothing new appears. | MANUAL-PENDING | video + log |
| HV-G9-4 post-play sources, paging and trailer fallback (136, 140–144, 146) | Settings → Playback → Up next: leave the source on "Same as More like this" and finish a film; then pick Auto with Kurato AI (or BingeCat AI) installed, then MDBList watchlist with MDBList connected, and finish a film each time; page right through the cards. | Official: the same up to 4 cards as before. Fork sources: up to 20 cards, each loads when reached without stalling; watched titles never appear; with the add-on uninstalled or MDBList disconnected the official cards appear. A title with no TMDB trailer but an add-on trailer still offers Trailer. | MANUAL-PENDING | video + log |
| HV-G9-5 shuffle season, fallback and Mystery mode (171, 173–177) | On a series detail page open Random episode: turn on Only season N and Mystery mode, pick Unwatched, play; return; then turn on the fallback on a fully watched season and let two shuffle episodes play back to back. Check the home row and the stream screen too; restore the stream route after process death and try manual source selection / no auto-selectable source. | Every pick is from season N. Before playback no surface (preview, hero, home card, stream screen, up-next card) shows the pick's number, title, still or overview; with Manual auto-play the best stream starts without the list. With the fallback on, a fully watched season keeps shuffling; with it off, the official "Choose episodes" state appears. | MANUAL-PENDING | video |
| HV-G9-6 Calendar (178–187) | With several series in progress, in the library and on a signed-in tracker, open Calendar from the drawer; switch filters; mark an episode watched on another device (Nuvio Sync or the tracker) and come back; open an episode. | Days 30 back to 90 ahead appear, starting at today; an episode whose previous one is unwatched shows no title or still; after the remote watch, the next episode unlocks without a reload spinner; opening an episode lands on its show with that episode focused. With DISCOVERY_SKIP_RECOMMENDATIONS OFF there is no Calendar entry. | MANUAL-PENDING | video |
| HV-G9-7 App dimmer (206, 207) | Settings → Appearance → App dimmer 50 %; browse home, open a settings dialog and the player audio/stream-choice raw dialogs; start playback and use the player control to pick 80 % then Off. | Every screen, the player picture and dialog boxes darken evenly; D-pad focus and buttons keep working; the player level applies at once without interrupting playback; Off removes the layer; the level survives an app restart per profile. | MANUAL-PENDING | photo |

## 4g. G10 — Live TV

| Test | Steps | Expected | Result | Evidence |
|---|---|---|---|---|
| HV-G10-1 sources: M3U, Xtream, Stalker (208–212, 233) | Settings → Layout → sidebar: turn on Live TV; open Live TV from the menu. Add an M3U link, an Xtream login and a Stalker portal (real providers), then block one provider host on the router and press Refresh. | Each source lists its channels in one list with its categories and logos; the Xtream links use TS unless the account allows only HLS; Stalker channels play through per-play links; the blocked source shows its error while its earlier channels and the other sources stay; saved source rows show only the host. After an app restart the sources are back (stored encrypted). | MANUAL-PENDING | photo, log (host only) |
| HV-G10-2 list, favorites, categories, search (219–225) | On the Live TV screen: move through categories (the list follows focus), hold OK on a channel to favorite it, open Favorites, search with the remote, open Edit categories: hide one, hold OK and move another with ▲▼, rename one, open a category with ▶ and hide one channel. | Focus never gets stuck (text fields leave with the arrows); favorites show a star and stay when their category is hidden; hidden categories and channels leave All channels and search; order and names survive an app restart per profile; 10 000+ channel lists scroll without stutter. | MANUAL-PENDING | video |
| HV-G10-3 play a channel (208) | Pick a channel, watch 30 s, press Back; pick the Continue watching row. | The official player opens it as live (no progress bar or resume point is saved); Back returns to the list focused on that channel; the Continue watching row reopens it. | MANUAL-PENDING | photo |

## 5. G4 final validation — same-file A/B (required before Stable release)

Use **F1** for arms A and B, and **F2 and F3** for the seek arm. Same TV, same network, same time
window. Before every run, force-stop the app (`adb shell am force-stop com.nuviodebug.com`) and
wait 10 s. Do **3 runs per arm** and write down every run, not just the average.

| Arm | Strategy | File |
|---|---|---|
| A | Official | F1 |
| B | REMUX / Throughput | F1 |
| C | Official | F2, F3 |
| D | Seek optimized | F2, F3 |

Measure, per run:

| Metric | How |
|---|---|
| Startup (s) | Stopwatch from the play press to the first moving frame. Also copy the log timestamps of the press (`PREWARM` or `PLAYBACK_STRATEGY:`) and of the first frame. |
| Throughput | The HUD network/bitrate reading after 60 s of playback. |
| Buffer ahead | The HUD buffer reading after 60 s. |
| Rebuffers | The HUD `rebuffer` count after 10 min of playback, plus the total stall time (stopwatch). |
| Seek latency (s) | 5 seeks (+10 min, −5 min, +30 min, near the end, back to 0:00): time from the key press to moving picture. Also note wrong-position seeks. |
| RAM (MB) | `adb shell dumpsys meminfo com.nuviodebug.com` TOTAL PSS at 5 min. |
| Dropped frames / audio | The HUD `underrun` count and any visible stutter at 10 min. |
| Waste | MANUAL-PENDING unless a byte counter is available: note whether `load err` appears. |

Result template (copy one per run):

```
Run: A1 | Strategy: Official | File: F1 | Network: wired
Startup: __ s | Throughput: __ | Buffer ahead: __ | Rebuffers: __ (__ s)
Seeks: __ / __ / __ / __ / __ s | Wrong seeks: __ | RAM: __ MB | Underruns: __
Notes:
```

**Exit criterion:** B must not regress against A in startup, rebuffers, RAM or seek, and D must
not regress against C, beyond run-to-run noise. Any regression needs a written explanation and a
decision in `docs/DECISIONS.md`; otherwise record FAIL and open a follow-up. Only then is G4 hardware validation complete for Stable release.

## 6. After running

1. Put each test's result and evidence into `docs/MANUAL_TEST_LOG.md`, one entry per test ID.
2. Send the logs and the A/B table in the chat. The agent updates `integration/state.yaml`,
   `docs/HANDOFF.md` and the traceability evidence. A FAIL reopens the affected implementation; PASS resolves the G4 validation-pending entry.