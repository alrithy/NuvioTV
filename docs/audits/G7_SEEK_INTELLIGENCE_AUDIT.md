# G7 audit — Seek Intelligence

Evidence for `tasks/G7_SEEK_INTELLIGENCE.md` (IDs 107–116) and G2's deferred 279 (memory-safe
Seekr). Official = accepted `56aaba2` (D050; official `dev` unchanged since). Sources (SOURCE_MAP
pins): DavidVamaiotu/NuvioTV-Reshaped `subtitle-autosync` @ `0ccf049d2789600835f3f7a75423e9149ea416ba`
(`ui/screens/player/seekpreview/*`, itself built on the Seekr author's AKhalil609/NuvioTV fork),
Cxsmo-ai/NuvioTV-Custom `main` @ `3e0d0fad60a2721adec133b88640b49c0183883f` (`Seekr*` in
`ui/screens/player`, `data/local/SeekrCredentialsStore`). This audit changes no code.

## What official has at `56aaba2`
- **No seek preview.** The scrubber shows position/time only; no thumbnails, keyframe index use or
  preview sync. (`seekRequest*` names in the trailer player are unrelated.)
- G4d (ID 25, our fork): `seekbuffer/SeekReadAhead` disk ring under the *Seek optimized* strategy.
  G7 reuses it and does not take it over (task packet).
- Official AutoSync already wraps the ExoPlayer extractors (`AutoSyncExtractorsFactory`,
  output forwarded unchanged) — the seam Reshaped's keyframe tap also uses.

## Seekr service and SDK
- Seekr (`seekr.tv`) serves sprite-sheet thumbnails per title; it needs an API key (free tier:
  40 distinct titles/day per key). SDK `tv.seekr:seekr-core|android|compose:0.2.0`
  (github.com/AKhalil609/seekr-android-sdk), **Apache-2.0**, on Maven Central (GPL-3.0 compatible).
- The SDK decodes every sprite sheet at playback start and keeps them as full bitmaps for the
  session (its `maxCachedSheets` is ignored): >100 MB on a long film. Reshaped `BoundedSeekrTrack`
  keeps sheets compressed and decodes only the current sheet and its neighbours (279).

## Source comparison
| Area | Reshaped | Cxsmo |
|---|---|---|
| Local previews | `seekpreview/local/*`: `VideoKeyframeTap` copies the keyframes playback already downloads (extractor wrapper, no extra requests), `KeyframeThumbnailDecoder` (one reused **software** codec, ByteBuffer mode, YUV-plane downscale, never holds the hardware decoder), `LocalPreviewTrack` (per-slot JPEG thumbnails, decoded only while paused/scrubbing, spool file while playing, memory + per-title disk cache), `LocalPreviewSources` (register/unregister per ExoPlayer; MPV gets none). Bounds: 10 s slots, 48 decoded thumbnails in memory, 200 MB per-title disk cache, 96 MB keyframe spool, stale spool dropped after 12 h, ≥ 3 GB RAM treated as not low-memory | none |
| Real keyframe seek | `SeekPreviewTrack.keyframeNear` from the file's own index; `seekPreviewCommitSeekParameters` snaps a commit onto a nearby keyframe | `CLOSEST_SYNC` only during calibration capture |
| Seekr | `BoundedSeekrTrack` (memory-bounded), `SeekrPreviewTrack`, `HybridSeekPreviewTrack` (local first, Seekr fills gaps) | SDK tracks directly (unbounded), three-frame host |
| Calibration | manual *Preview Sync* only | `SeekrFrameCalibrator`: per-cue best local frame by luma perceptual similarity, rejects weak (<0.56) and ambiguous (margin <0.035) matches, median + MAD over inliers, confidence, ±240 s cap; frames from `ExoSeekrFrameCapture`, which **seeks the live player** behind a cover and copies the surface |
| Manual sync | `SeekPreviewSyncOverlay` / `SeekPreviewSyncConfig` | `SeekPreviewSyncOverlay` / `SeekPreviewSyncConfig` (250 ms / 2 s steps) |
| Key storage | `SeekrKeyPreferences`: plaintext SharedPreferences, falls back to a key built into the APK (`BuildConfig.SEEKR_API_KEY`) | `SeekrCredentialsStore`: profile-scoped DataStore value **AES-GCM encrypted with an Android Keystore key** |

## Decision: one preview engine (D052)
One `SeekPreviewTrack` pipeline in `fork/seek`, in the task-packet order:
**local real keyframes → calibrated Seekr → normal seek**.
- Base: Reshaped's hybrid structure (FILE_PORT): keyframe tap + software thumbnail decoder + local
  track, Seekr only fills slots the local track does not have, bounded Seekr sheets (279).
- Calibration: ALGORITHM_PORT of Cxsmo's `SeekrFrameCalibrator` (similarity, weak/ambiguous
  rejection, median + MAD, confidence), but fed with **local keyframe thumbnails of the playing
  file** instead of Cxsmo's surface capture: no seeking of the live player, no extra network. Local
  frames are real decoded frames of the same file, so they are a valid ground truth.
- Key: Cxsmo's Keystore-encrypted, profile-scoped store; **no built-in key** (a key compiled into
  the APK is extractable and shared). Without a user key there is no Seekr track; local previews
  work regardless.
- Never two preview systems side by side; Cxsmo's direct SDK host and surface capture are not
  imported.
- Default: previews only show while scrubbing, which is a user action; local generation follows
  the resource tier (AdaptiveResourceManager owns the memory/disk budget; off on constrained
  devices unless the user enables it). FeatureId SEEK_INTELLIGENCE OFF = official scrubber.

## Features
| ID | Behavior | Class | Result / plan |
|---|---|---|---|
| 107 | Hybrid Seek Preview engine | missing delta | one `SeekPreviewTrack` pipeline (local → Seekr → normal seek) → G7a/G7b |
| 108 | Local preview frames from the current video | missing delta | Reshaped keyframe tap + software decoder + local track → G7a |
| 109 | Use real keyframes where available | missing delta | thumbnails are real keyframes; commit snaps to a nearby keyframe from the file index → G7a |
| 110 | Seekr thumbnail fallback | missing delta | bounded Seekr track fills slots without a local frame, user key only → G7b |
| 111 | Seekr auto-calibration against real frames | missing delta | Cxsmo estimator on local keyframe thumbnails → G7c |
| 112 | Manual preview-sync adjustment | missing delta | Preview Sync overlay (Reshaped/Cxsmo share the design) → G7b |
| 113 | Confidence-based Seekr calibration | missing delta | estimator confidence + anchor count gate → G7c |
| 114 | Reject weak calibration | missing delta | weak / ambiguous / high-MAD results keep offset 0 (or the manual value) → G7c |
| 115 | Bound Seek Preview RAM usage | missing delta | JPEG slots (Reshaped keeps 48 decoded) + bounded Seekr sheets; the decoded count comes from the AdaptiveResources tier instead of Reshaped's fixed 3 GB threshold → G7a/G7b |
| 116 | Bounded disk cache for seek previews | missing delta | Reshaped per-title cache (200 MB) and spool (96 MB, 12 h stale cleanup) with caps scaled down by tier for TV storage, oldest title evicted first → G7a |
| 279 | Memory-safe Seekr handling (G2) | missing delta | Reshaped `BoundedSeekrTrack` → G7b |

## Slice plan
- G7a (107, 108, 109, 115, 116): local keyframe previews + scrubber thumbnail + keyframe snap,
  ExoPlayer only (MPV keeps the official scrubber), memory/disk bounds from AdaptiveResources.
- G7b (110, 112, 279): Seekr fallback with the encrypted per-profile key and bounded sheets,
  manual Preview Sync; Seekr requests carry the user key only to `api.seekr.tv`, never logged.
- G7c (111, 113, 114): automatic Seekr calibration against local keyframe frames with
  confidence gating and weak-match rejection.

Every slice keeps device checks MANUAL-PENDING in `docs/HARDWARE_VALIDATION_TCL_C6K.md` (G14, D047).
