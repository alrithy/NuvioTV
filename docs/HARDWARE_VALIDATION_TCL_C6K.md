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