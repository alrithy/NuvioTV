# Master Feature Inventory

This is the authoritative product-scope list for the Nuvio Superfork. It captures the complete feature set agreed for the project. Numbers are stable IDs and should not be renumbered.

Status convention:
- OFFICIAL = already available in current official Nuvio baseline; keep upstream implementation.
- IMPORT = expected to be imported/ported from a pinned fork.
- NEW_GLUE = integration/coordination code we own.
- EXPERIMENTAL = optional/off by default.
- TARGET = desired end-state behavior composed from multiple sources.

## Base / Upstream
1. Build on the latest official NuvioTV rather than an old fork.
2. Keep continuous upstream sync capability without losing fork changes.
3. Automatic Playback Strategy selection by device/file/network.
4. Parallel Range Downloading for high-throughput playback.
5. Adaptive Connection Count.
6. Adaptive Chunk Size.
7. Deep buffering for large 4K REMUX files.
8. Avoid re-downloading already fetched data.
9. CDN throttle recovery.
10. Intelligent HTTP 429/503 handling.
11. Detect stalled connections and reopen a fresh one.
12. Off-heap/network buffer strategy where beneficial.
13. Speed testing against the actual stream URL.
14. Device Settings Assessment.
15. Apply Recommended Settings action.
16. Revert recommended settings.
17. Pre-resolve stream before playback.
18. Warm connection before playback to reduce startup delay.
19. Automatic source failover mid-playback.
20. Startup watchdog for stuck playback.
21. Recover from partially malformed MKV when possible.
22. Better truncated MKV handling.
23. Nested MKV SeekHead support/fixes.
24. Better seeking for non-faststart/poorly interleaved MP4.
25. Disk-based seek buffer.
26. Adaptive MPV demux cache by RAM/device tier.
27. Standard Playback mode.
28. REMUX / Throughput playback mode.
29. Seek Optimized playback mode.
30. Low-RAM playback mode.
31. Auto playback mode.

## Audio
32. TrueHD passthrough.
33. Dolby Atmos passthrough.
34. DTS-HD MA passthrough.
35. DTS:X passthrough.
36. Dolby Digital / DD+ passthrough.
37. Automatically select best lossless audio track.
38. Per-format passthrough controls.
39. Detect real audio device / TV / AVR capabilities.
40. Audio output diagnostics.
41. Display actual audio bitrate.
42. Display source channels vs output channels.
43. Improved FFmpeg downmix.
44. Software decode fallback when required.
45. Experimental Kodi-style MAT / IEC61937 path.
46. Reduce cold-start delay for TrueHD / DTS-HD.
47. Volume boost up to 200%.
48. Soft clipping for boosted volume.

## Dolby Vision / HDR / Frame Rate
49. libdovi integration.
50. Dolby Vision Profile 7 -> 8.1 conversion when required.
51. Dolby Vision Profile 5 -> 8.1 fallback when required.
52. Better handling of DV enhancement layers / single-track cases.
53. Detect actual DV/HDR playback state.
54. Improved HDR10 fallback.
55. True-black HDR letterboxing.
56. Accurate 23.976 / 24 Hz matching.
57. Automatic Frame Rate matching.
58. Seamless frame-rate-switch detection.
59. HDMI/eARC settle/resume handling after display mode changes.
60. Display source resolution vs output resolution.

## Unified Diagnostics / Stats for Nerds
61. Unified Stats for Nerds HUD.
62. Video codec.
63. Actual video bitrate.
64. Source FPS.
65. Display refresh rate.
66. HDR / Dolby Vision profile/status.
67. Audio codec.
68. Atmos / DTS:X status.
69. Real passthrough status.
70. Live network throughput.
71. Required bitrate vs available throughput.
72. Buffer ahead.
73. Connection count and chunk size.
74. Rebuffer count.
75. Dropped frames.
76. Audio underruns.
77. Audio clock jitter.
78. RAM and player-buffer usage.
79. SoC / CPU status.
80. Thermal-throttling indication where measurable.
81. Current playback strategy.

## Subtitle Intelligence
82. Subtitle AutoSync.
83. Embedded subtitle reference.
84. Same-release subtitle reference.
85. Hash-matched subtitle reference.
86. Cue-rhythm alignment across different languages.
87. Automatic subtitle offset estimation.
88. Clock-scale correction for gradual drift.
89. Piecewise drift correction.
90. Confidence scoring before applying automatic sync.
91. Fail closed if sync confidence is low.
92. Audio-based subtitle sync fallback.
93. On-device speech recognition where practical.
94. Arabic AutoSync.
95. Language-independent AutoSync.
96. Secondary subtitle language.
97. Automatic secondary-language fallback.
98. Auto-Synced indicator.
99. Subtitle sync notifications.
100. Custom subtitle fonts.
101. Upload TTF/OTF from phone via QR.
102. Download font from URL.
103. Validate font before use.
104. Fall back to default font if custom font fails.
105. Arabic cinema subtitle preset.
106. Avoid video reload when switching sidecar subtitles.

## Seek Intelligence / Preview
107. Hybrid Seek Preview engine.
108. Local preview frames from the current video.
109. Use real keyframes where available.
110. Seekr thumbnail fallback.
111. Seekr auto-calibration against real rendered frames.
112. Manual preview-sync adjustment.
113. Confidence-based Seekr calibration.
114. Reject weak calibration.
115. Bound Seek Preview RAM usage.
116. Bounded disk cache for seek previews.

## Skip Engine
117. Multi-provider Skip Intro engine.
118. Skip Recap.
119. Skip Credits / Outro.
120. Skip Preview.
121. Content-warning segments.
122. Mute segments where metadata supports it.
123. IntroDB.
124. SkipMe.db.
125. TheIntroDB.
126. PublicMetaDB.
127. MovieHavenDB.
128. VideoSkip.
129. NotScare public-page integration.
130. Parallel provider requests.
131. Independent timeout per provider.
132. Merge overlapping evidence.
133. Confidence-weighted skip timing.
134. TMDB -> IMDb normalization where required.
135. Encrypt provider credentials per profile.

## Post-play / Recommendations
136. Configurable post-play recommendation engine.
137. Trakt recommendations.
138. TMDB recommendations.
139. Simkl recommendations.
140. MDBList recommendations.
141. Kurato AI recommendations.
142. BingeCat AI recommendations.
143. Auto recommendation-provider mode.
144. Full pagination.
145. Lazy metadata loading.
146. Multi-source trailer fallback.

## Progressive AIOStreams
147. Progressive AIOStreams results.
148. Show results while slower addons continue.
149. Cumulative NDJSON snapshots.
150. Deduplicate progressive results.
151. Fall back to normal AIOStreams endpoint.
152. Instant scrape timeout mode.
153. Bounded scrape timeout mode.
154. Unlimited scrape mode with safeguards.

## Stream Intelligence / Ranking
155. Improved stream ranking.
156. Real-Debrid cached first.
157. Keep uncached results visible.
158. 4K above 1080p when configured.
159. REMUX preference.
160. Release-group quality ranking, TRaSH-style where appropriate.
161. Codec ranking.
162. HDR / DV ranking.
163. Lossless-audio ranking.
164. Bitrate ranking.
165. Connection-fit ranking.
166. Source-reliability ranking.
167. Preserve addon order where possible.
168. Auto-play best stream using the same ranking engine.

## Discovery / Random / Mystery
169. Random Episode picker.
170. Random across all seasons.
171. Random within current season.
172. Unwatched-only mode.
173. Fallback after all episodes watched.
174. Mystery Episode mode.
175. Hide episode title in Mystery Mode.
176. Hide episode number.
177. Hide image/overview to avoid spoilers.

## Calendar
178. Native in-app Calendar.
179. Upcoming episodes.
180. Recently released episodes.
181. Combine Library + metadata-addon dates.
182. Nuvio Sync integration.
183. Trakt integration.
184. Simkl integration.
185. MDBList integration.
186. Real-time watched-state updates.
187. Spoiler-safe Calendar.

## UI / Layout
188. Multiple Home Layouts rather than forcing one.
189. Original layout.
190. Classic layout.
191. Grid layout.
192. Modern layout.
193. Glass layout.
194. Cinematic Glass layout.
195. Modern sidebar navigation.
196. Top navigation.
197. Glass top navigation.
198. Pill navigation.
199. Liquid-glass effect on capable devices.
200. Lightweight fallback on weaker devices.
201. Full-screen Hero.
202. Rotating Hero artwork.
203. Hero trailer support.
204. Clock in top navigation.
205. Profile access in top navigation.
206. App Dimmer.
207. Player-accessible dimmer control.

## Live TV
208. Live TV module.
209. M3U playlists.
210. Xtream Codes.
211. Stalker Portal.
212. Multiple IPTV sources.
213. Add Live TV source from phone via QR.
214. EPG.
215. Now Playing information.
216. Next Program information.
217. Program progress bar.
218. Time remaining.
219. Favorites.
220. Categories.
221. Reorder categories.
222. Hide categories.
223. Hide channels.
224. Channel search.
225. Channel logos.
226. Channel preview.
227. Low-resolution preview on constrained devices.
228. Zap Up/Down.
229. CH+/CH- support.
230. Channel list inside player.
231. Category panel inside player.
232. Now/Next card on OK.
233. Improved Xtream TS/HLS handling.
234. Channel-start retry.
235. Faster MPEG-TS startup from first useful I-frame.
236. Live-TV-aware AFR behavior.

## Watch Party
237. Watch Party.
238. Six-character room code.
239. TV <-> TV Watch Party.
240. TV <-> Android phone Watch Party.
241. Play/Pause sync.
242. Seek sync.
243. Share the resolved stream URL only with explicit permission.
244. Share only required headers and handle them securely.
245. WebRTC P2P communication.
246. Encrypted Watch Party data channel.
247. Soft drift correction for small drift.
248. Hard seek for large drift.
249. Never log sensitive stream URLs/headers.

## AI Media Providers — Experimental
250. AI Media Provider platform.
251. AI-generated subtitles.
252. AI speech-to-text.
253. AI subtitle translation.
254. AI voice translation / voice overlay.
255. BYOK.
256. Multiple AI vendors.
257. Provider APK architecture.
258. Keep AI providers isolated from the core app.
259. SHA-256 verification for provider APKs.
260. Signer verification.
261. Encrypt API keys using Android Keystore.

## Adaptive Resource Manager
262. Adaptive Resource Manager.
263. Detect physical RAM.
264. Low-RAM tier.
265. Constrained-device tier.
266. Buffer size by RAM tier.
267. Parallel connection count by RAM tier.
268. Chunk size by RAM tier.
269. Bound addon request concurrency.
270. Reduce poster cache on weak devices.
271. RGB565 posters only when required.
272. Reduce/disable animated posters on weak devices.
273. Reduce post-play prefetch on weak devices.
274. Bounded metadata caches.
275. Bounded rating cache.
276. Bounded offline sync queue.
277. Reduce stream-list recompositions.
278. Optimize Bidi/RTL rendering work.
279. Memory-safe Seekr handling.
280. Low-RAM MPV cache policy.

## Add-on Reliability / Health
281. Add-on Health system.
282. Healthy state.
283. Slow state.
284. Timeout state.
285. Authentication-error state.
286. Manifest-error state.
287. No-streams state.
288. Do not permanently hide an addon for a temporary manifest failure.
289. Addon retry policy.
290. Isolate a failing addon from other sources.
291. Add-on Health screen.

## Other Platform / Infrastructure
292. Optional screensaver.
293. Official self-hosted Nuvio server support.
294. /.well-known/nuvio discovery.
295. Custom backend switching.
296. Custom Supabase configuration.
297. Server trust confirmation.
298. Easy return to official Nuvio server.
299. Android Keystore for sensitive credentials.
300. Profile-scoped secrets.
301. Do not send stream credentials to subtitle-sync services.
302. Do not forward Cookies/Authorization to unrelated subtitle hosts.
303. QR local-server session tokens.
304. Cross-origin protection for local QR pages.
305. Do not log API keys or sensitive URLs.
306. SHA-256 checks for distributed APK releases.
307. ABI-specific APKs: arm64 / armeabi-v7a / universal.
308. In-app updater for the fork.
309. Stable release channel.
310. Beta release channel.
311. GitHub Actions CI for builds.
312. Tests before APK release.
313. Every major feature is isolated behind a module and/or feature flag where practical.
314. Avoid invasive core changes unless necessary for maintainability/upstream sync.
315. One feature/gate per coherent commit/PR.
316. Automated upstream sync checks.
317. Experimental features disabled by default.
318. Simple settings for normal users plus Advanced controls for power users.
319. Stability takes priority over blindly enabling every feature.
320. End goal: one Nuvio distribution combining the strongest playback, subtitle, Live TV, UI, diagnostics, discovery and community features from the important forks.

## Product-priority defaults

These are product preferences, not immutable implementation details:
- Real-Debrid cached results rank before uncached results.
- Uncached results remain visible.
- Prefer 4K over 1080p when the user chooses quality-first behavior.
- Prefer REMUX; keep WEB-DL as fallback.
- Prefer Dolby Vision/HDR when device capability allows.
- Prefer TrueHD Atmos / lossless audio where supported.
- Connection-fit must prevent a theoretically higher-quality source from causing an obviously worse playback experience.
- Preserve addon ordering when the ranker has no strong reason to override it.

## Scope rule
This file defines desired end-state scope. It does not authorize merging all features at once. docs/ROADMAP.md controls implementation order.
