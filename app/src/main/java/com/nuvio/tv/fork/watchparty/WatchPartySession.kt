package com.nuvio.tv.fork.watchparty

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlin.math.abs
import kotlin.time.TimeSource

enum class WatchPartyRole { HOST, GUEST }

enum class WatchPartyStatus { IDLE, CONNECTING, CONNECTED, ERROR }

data class WatchPartyState(
    val status: WatchPartyStatus = WatchPartyStatus.IDLE,
    val role: WatchPartyRole? = null,
    val code: String? = null,
    val participants: List<String> = emptyList(),
    val hostPresent: Boolean = false,
    val error: WatchPartyError? = null,
    /** Host: the people who join get the open stream's link (allow-listed headers only). */
    val sharingConsented: Boolean = false,
    /** Guest: the host's stream was refused or did not open here (feature 244; IP-locked or local links). */
    val streamUnsupported: Boolean = false,
) {
    val isActive: Boolean get() = status == WatchPartyStatus.CONNECTING || status == WatchPartyStatus.CONNECTED
}

/**
 * A Watch Party room (G11a, D056; features 237–242, 245–248). FILE_PORT of AntoninoScardina/NuvioTV
 * `watchparty/WatchPartySession.kt` @ ff597b1: the host is the authority on playback and guests
 * follow. Anyone may play, pause or seek: a guest's command goes to the host (CMD), which applies it
 * and sends the state back (STATE). Small drift is caught up with a speed change of up to ±10 %, a
 * large one with a seek that learns how long a seek takes. Adapted: a host shares only after
 * consent; a guest accepts only what [WatchPartySharePolicy.acceptReceived] lets through; errors are
 * fixed codes; scope and clock are injectable for tests. Every method runs on the main thread.
 *
 * Corrections after review (G11 closeout): a guest follows one host — the first peer that speaks as
 * host — and ignores MEDIA / STATE from anyone else until that host leaves, which also ends every
 * correction based on it. Guests follow the host's chosen speed ([WatchPartyWire.rate]) and drift
 * correction works around it; the viewer's own speed comes back when the room ends.
 */
class WatchPartySession(
    private val transportFactory: () -> WatchPartyTransport,
    private val deviceName: () -> String,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
    private val clockMs: () -> Long = TimeSource.Monotonic.markNow().let { mark -> { mark.elapsedNow().inWholeMilliseconds } },
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false; explicitNulls = false }

    private val _state = MutableStateFlow(WatchPartyState())
    val state: StateFlow<WatchPartyState> = _state.asStateFlow()

    /** The stream a guest must open: the app opens the player and then calls [consumeMediaRequest]. */
    private val _mediaRequest = MutableStateFlow<WatchPartyMedia?>(null)
    val mediaRequest: StateFlow<WatchPartyMedia?> = _mediaRequest.asStateFlow()

    private var transport: WatchPartyTransport? = null
    private var tickerJob: Job? = null
    private val peerNames = linkedMapOf<String, String>()
    private var hostUuid: String? = null

    private var player: WatchPartyPlayer? = null
    private var playerMedia: WatchPartyMedia? = null
    private var hostMedia: WatchPartyMedia? = null

    private var lastPosition = 0L
    private var lastPlaying = false
    private var lastTickAt = 0L
    private var suppressUntil = 0L
    private var ignoreHostStateUntil = 0L
    private var lastStateSentAt = 0L

    private var hostPosition = 0L
    private var hostPlaying = false
    private var hostStateAt = -1L
    private var hostRate = 1f

    // Guest: after a seek, wait for the player to run again before correcting again, and measure
    // how long that took (seekLeadMs) to land a little further ahead next time.
    private var awaitingSettle = false
    private var lastSeekAt = 0L
    private var lastSeekTarget = 0L
    private var settledAt = 0L
    private var seekLeadMs = 0L
    /** The speed this session set on the player; null while the player runs at the viewer's own speed. */
    private var appliedSpeed: Float? = null

    private fun now(): Long = clockMs()

    /**
     * Opens a room as host. Only after the host agreed that the people who join get the open
     * stream's link (feature 243); without that nothing is created and null is returned.
     */
    fun createRoom(sharingConsented: Boolean): String? {
        if (!sharingConsented) return null
        val code = WatchPartyProtocol.generateCode()
        start(code, WatchPartyRole.HOST)
        _state.update { it.copy(sharingConsented = true) }
        return code
    }

    /** False when the code is not a valid one. */
    fun joinRoom(rawCode: String): Boolean {
        val code = WatchPartyProtocol.normalizeCode(rawCode) ?: return false
        start(code, WatchPartyRole.GUEST)
        return true
    }

    fun leaveRoom() {
        player?.let(::restoreSpeed)
        transport?.let { t ->
            runCatching { t.send(encode(WatchPartyWire(type = WatchPartyProtocol.BYE))) }
            t.leave()
        }
        transport = null
        tickerJob?.cancel()
        tickerJob = null
        peerNames.clear()
        forgetHost()
        seekLeadMs = 0L
        _mediaRequest.value = null
        WatchPartyReceivedStreams.clearTickets()
        _state.value = WatchPartyState()
    }

    /** Guest: the host's stream did not open on this device (the player failed or it was refused). */
    fun reportStreamUnsupported() {
        if (_state.value.role == WatchPartyRole.GUEST) _state.update { it.copy(streamUnsupported = true) }
    }

    fun consumeMediaRequest() {
        _mediaRequest.value = null
    }

    /** The player calls this once it is ready to play [media] (for a host: already share-filtered). */
    fun attachPlayer(player: WatchPartyPlayer, media: WatchPartyMedia) {
        this.player = player
        this.playerMedia = media
        appliedSpeed = null
        awaitingSettle = false
        resetBaseline(player)
        if (!_state.value.isActive) return
        when (_state.value.role) {
            WatchPartyRole.HOST -> broadcastMedia()
            WatchPartyRole.GUEST -> {
                if (sameStream(media, hostMedia)) applyHostState(force = true)
                sendToHost(WatchPartyWire(type = WatchPartyProtocol.REQUEST_STATE))
            }
            null -> Unit
        }
    }

    fun detachPlayer(player: WatchPartyPlayer) {
        if (this.player !== player) return
        restoreSpeed(player)
        this.player = null
        this.playerMedia = null
    }

    /** For the player's Watch Party button: the host creates the room from the open player. */
    val attachedMedia: WatchPartyMedia? get() = playerMedia

    private fun start(code: String, role: WatchPartyRole) {
        leaveRoom()
        _state.value = WatchPartyState(status = WatchPartyStatus.CONNECTING, role = role, code = code)
        val t = transportFactory()
        transport = t
        t.join(
            room = WatchPartyProtocol.roomFor(code),
            password = WatchPartyProtocol.passwordFor(code),
            label = WatchPartySharePolicy.peerName(deviceName()) ?: DEFAULT_NAME,
            listener = TransportListener(t),
        )
        tickerJob = scope.launch {
            while (isActive) {
                delay(TICK_MS)
                tick()
            }
        }
    }

    private inner class TransportListener(private val owner: WatchPartyTransport) : WatchPartyTransport.Listener {
        private val current get() = transport === owner

        override fun onJoined() {
            if (!current) return
            _state.update { it.copy(status = WatchPartyStatus.CONNECTED, error = null) }
        }

        override fun onPeerJoined(uuid: String) {
            if (!current) return
            peerNames.getOrPut(uuid) { "…" }
            publishParticipants()
            val isHost = _state.value.role == WatchPartyRole.HOST
            send(WatchPartyWire(type = WatchPartyProtocol.HELLO, name = WatchPartySharePolicy.peerName(deviceName()), host = isHost), uuid)
            if (isHost) sendMediaTo(uuid)
        }

        override fun onPeerLeft(uuid: String) {
            if (!current) return
            peerNames.remove(uuid)
            if (uuid == hostUuid) forgetHost()
            publishParticipants()
        }

        override fun onMessage(uuid: String, json: String) {
            if (!current) return
            val msg = runCatching { this@WatchPartySession.json.decodeFromString(WatchPartyWire.serializer(), json) }
                .getOrNull() ?: return
            handleMessage(uuid, msg)
        }

        override fun onError(error: WatchPartyError) {
            if (!current) return
            _state.update { it.copy(status = WatchPartyStatus.ERROR, error = error) }
        }
    }

    private fun handleMessage(uuid: String, msg: WatchPartyWire) {
        val role = _state.value.role ?: return
        when (msg.type) {
            WatchPartyProtocol.HELLO -> {
                peerNames[uuid] = WatchPartySharePolicy.peerName(msg.name) ?: DEFAULT_NAME
                if (msg.host == true && role == WatchPartyRole.GUEST && followsHost(uuid)) {
                    hostUuid = uuid
                    sendToHost(WatchPartyWire(type = WatchPartyProtocol.REQUEST_STATE))
                }
                publishParticipants()
            }
            WatchPartyProtocol.BYE -> {
                peerNames.remove(uuid)
                if (uuid == hostUuid) forgetHost()
                publishParticipants()
            }
            WatchPartyProtocol.REQUEST_STATE -> if (role == WatchPartyRole.HOST) sendMediaTo(uuid)
            WatchPartyProtocol.CMD -> if (role == WatchPartyRole.HOST) applyGuestCommand(msg)
            WatchPartyProtocol.MEDIA -> if (role == WatchPartyRole.GUEST && followsHost(uuid)) {
                hostUuid = uuid
                // Only a public HTTP(S) link with allow-listed headers is ever opened (D056).
                val media = msg.media?.let(WatchPartySharePolicy::acceptReceived)
                if (media == null) {
                    _state.update { it.copy(streamUnsupported = true) }
                    publishParticipants()
                    return
                }
                _state.update { it.copy(streamUnsupported = false) }
                hostMedia = media
                recordHostState(msg)
                if (!sameStream(media, playerMedia)) {
                    _mediaRequest.value = media
                } else {
                    applyHostState(force = true)
                }
                publishParticipants()
            }
            WatchPartyProtocol.STATE -> if (role == WatchPartyRole.GUEST && followsHost(uuid)) {
                hostUuid = uuid
                recordHostState(msg)
                if (now() >= ignoreHostStateUntil) applyHostState(force = false)
            }
        }
    }

    // ---- Host ----

    private fun sendMediaTo(uuid: String?) {
        if (!_state.value.sharingConsented) return
        val p = player
        val media = playerMedia ?: return
        send(
            WatchPartyWire(
                type = WatchPartyProtocol.MEDIA,
                media = media,
                positionMs = p?.positionMs,
                playing = p?.isPlaying,
                rate = p?.let(::sharedRate),
            ),
            uuid,
        )
    }

    private fun broadcastMedia() = sendMediaTo(null)

    private fun broadcastState() {
        val p = player ?: return
        if (playerMedia == null) return
        lastStateSentAt = now()
        send(WatchPartyWire(type = WatchPartyProtocol.STATE, positionMs = p.positionMs, playing = p.isPlaying, rate = sharedRate(p)))
    }

    /** The host's speed on the wire: only when it is not 1×, so normal messages stay protocol v1. */
    private fun sharedRate(p: WatchPartyPlayer): Float? = p.selectedSpeed.takeIf { it != 1f && it.isFinite() }

    private fun applyGuestCommand(msg: WatchPartyWire) {
        val p = player ?: return
        suppressUntil = now() + SUPPRESS_MS
        when (msg.action) {
            WatchPartyProtocol.ACTION_PLAY -> {
                msg.positionMs?.let { if (abs(it - p.positionMs) > SEEK_THRESHOLD_MS) p.seekTo(it) }
                p.play()
            }
            WatchPartyProtocol.ACTION_PAUSE -> {
                p.pause()
                msg.positionMs?.let { if (abs(it - p.positionMs) > SEEK_THRESHOLD_MS) p.seekTo(it) }
            }
            WatchPartyProtocol.ACTION_SEEK -> msg.positionMs?.let { p.seekTo(it) }
        }
        resetBaseline(p, expectedPosition = msg.positionMs, expectedPlaying = when (msg.action) {
            WatchPartyProtocol.ACTION_PLAY -> true
            WatchPartyProtocol.ACTION_PAUSE -> false
            else -> null
        })
        broadcastState()
    }

    // ---- Guest ----

    private fun recordHostState(msg: WatchPartyWire) {
        val position = msg.positionMs ?: return
        hostPosition = position
        hostPlaying = msg.playing ?: hostPlaying
        hostRate = msg.rate?.takeIf { it.isFinite() && it in WatchPartyProtocol.MIN_RATE..WatchPartyProtocol.MAX_RATE } ?: 1f
        hostStateAt = now()
    }

    private fun estimatedHostPosition(): Long =
        if (hostPlaying) hostPosition + ((now() - hostStateAt) * hostRate).toLong() else hostPosition

    /** A guest follows one host: the first peer that speaks as host, until that peer leaves. */
    private fun followsHost(uuid: String): Boolean = hostUuid == null || hostUuid == uuid

    /** The host left (or the room ended): nothing it said is applied any more. */
    private fun forgetHost() {
        hostUuid = null
        hostMedia = null
        hostStateAt = -1L
        hostRate = 1f
        hostPlaying = false
        awaitingSettle = false
        ignoreHostStateUntil = 0L
        if (_state.value.role == WatchPartyRole.GUEST) player?.let(::restoreSpeed)
    }

    private fun applyHostState(force: Boolean) {
        val p = player ?: return
        if (hostStateAt < 0 || !sameStream(playerMedia, hostMedia)) return
        val t = now()
        if (!force && (p.isBuffering || awaitingSettle || t - settledAt < SETTLE_MS)) return

        if (p.isPlaying != hostPlaying) {
            if (hostPlaying) p.play() else p.pause()
            suppressUntil = t + SUPPRESS_MS
            resetBaseline(p, expectedPlaying = hostPlaying)
        }

        val target = estimatedHostPosition()
        val drift = target - p.positionMs
        when {
            abs(drift) > SEEK_THRESHOLD_MS || ((force || !hostPlaying) && abs(drift) > SPEED_START_MS) -> {
                setSpeed(p, 1f)
                val seekTarget = target + if (hostPlaying) seekLeadMs else 0L
                p.seekTo(seekTarget)
                lastSeekAt = t
                lastSeekTarget = seekTarget
                awaitingSettle = true
                suppressUntil = t + SUPPRESS_MS
                resetBaseline(p, expectedPosition = seekTarget, expectedPlaying = hostPlaying)
            }
            hostPlaying && abs(drift) > SPEED_START_MS ->
                setSpeed(p, 1f + (drift / SPEED_GAIN_MS).coerceIn(-MAX_SPEED_DELTA, MAX_SPEED_DELTA))
            !hostPlaying || abs(drift) < SPEED_STOP_MS -> setSpeed(p, 1f)
        }
    }

    /** Guest: a seek has "settled" once the player runs again past where it landed. */
    private fun checkSeekSettled(p: WatchPartyPlayer, t: Long) {
        if (!awaitingSettle) return
        val elapsed = t - lastSeekAt
        val resumed = !p.isBuffering && (!hostPlaying || p.positionMs >= lastSeekTarget + 200)
        if (resumed && elapsed >= 500) {
            if (hostPlaying) {
                seekLeadMs = if (seekLeadMs == 0L) elapsed else (seekLeadMs + elapsed) / 2
                seekLeadMs = seekLeadMs.coerceAtMost(MAX_SEEK_LEAD_MS)
            }
            awaitingSettle = false
            settledAt = t
        } else if (elapsed > SETTLE_TIMEOUT_MS) {
            awaitingSettle = false
            settledAt = t
        }
    }

    /** Guest: plays at the host's speed times [factor] (1 = in step, up to ±10 % to catch up). */
    private fun setSpeed(p: WatchPartyPlayer, factor: Float) {
        val speed = hostRate * factor
        if (speed == appliedSpeed) return
        appliedSpeed = speed
        p.setPlaybackSpeed(speed)
    }

    /** Back to the speed the viewer picked. */
    private fun restoreSpeed(p: WatchPartyPlayer) {
        if (appliedSpeed == null) return
        appliedSpeed = null
        p.setPlaybackSpeed(p.selectedSpeed)
    }

    private fun sendToHost(msg: WatchPartyWire) {
        send(msg, hostUuid)
    }

    // ---- Local actions, found by polling (works the same for ExoPlayer and mpv) ----

    private fun tick() {
        val p = player ?: return
        if (!_state.value.isActive) return
        val t = now()
        val position = p.positionMs
        val playing = p.isPlaying
        val elapsed = t - lastTickAt

        if (_state.value.role == WatchPartyRole.GUEST) checkSeekSettled(p, t)

        if (t < suppressUntil || awaitingSettle) {
            resetBaseline(p)
        } else {
            val playChanged = playing != lastPlaying
            val expected = lastPosition + if (lastPlaying) elapsed else 0L
            val seeked = position < lastPosition - SEEK_DETECT_MS || position > expected + SEEK_DETECT_MS
            lastPosition = position
            lastPlaying = playing
            lastTickAt = t
            if (playChanged || seeked) onLocalAction(playChanged, seeked, position, playing)
        }

        when (_state.value.role) {
            WatchPartyRole.HOST -> if (t - lastStateSentAt >= STATE_INTERVAL_MS && peerNames.isNotEmpty()) broadcastState()
            WatchPartyRole.GUEST -> if (t >= ignoreHostStateUntil) applyHostState(force = false)
            null -> Unit
        }
    }

    private fun onLocalAction(playChanged: Boolean, seeked: Boolean, position: Long, playing: Boolean) {
        when (_state.value.role) {
            WatchPartyRole.HOST -> broadcastState()
            WatchPartyRole.GUEST -> {
                ignoreHostStateUntil = now() + GUEST_GRACE_MS
                val action = when {
                    playChanged && playing -> WatchPartyProtocol.ACTION_PLAY
                    playChanged -> WatchPartyProtocol.ACTION_PAUSE
                    else -> WatchPartyProtocol.ACTION_SEEK
                }
                sendToHost(WatchPartyWire(type = WatchPartyProtocol.CMD, action = action, positionMs = position))
                if (playChanged && seeked) {
                    sendToHost(WatchPartyWire(type = WatchPartyProtocol.CMD, action = WatchPartyProtocol.ACTION_SEEK, positionMs = position))
                }
            }
            null -> Unit
        }
    }

    private fun resetBaseline(p: WatchPartyPlayer, expectedPosition: Long? = null, expectedPlaying: Boolean? = null) {
        lastPosition = expectedPosition ?: p.positionMs
        lastPlaying = expectedPlaying ?: p.isPlaying
        lastTickAt = now()
    }

    // ---- Utility ----

    private fun publishParticipants() {
        val role = _state.value.role
        _state.update {
            it.copy(
                participants = peerNames.values.toList(),
                hostPresent = role == WatchPartyRole.HOST || (hostUuid != null && peerNames.containsKey(hostUuid)),
            )
        }
    }

    private fun send(msg: WatchPartyWire, targetUuid: String? = null) {
        transport?.send(encode(msg), targetUuid)
    }

    private fun encode(msg: WatchPartyWire): String = json.encodeToString(WatchPartyWire.serializer(), msg)

    private fun sameStream(a: WatchPartyMedia?, b: WatchPartyMedia?): Boolean =
        a != null && b != null && a.url == b.url

    private companion object {
        /** A peer that sends no usable name. */
        const val DEFAULT_NAME = "Nuvio"
        const val TICK_MS = 500L
        const val STATE_INTERVAL_MS = 2_000L
        const val SEEK_THRESHOLD_MS = 3_000L
        const val SPEED_START_MS = 400L
        const val SPEED_STOP_MS = 120L
        const val SPEED_GAIN_MS = 12_000f
        const val MAX_SPEED_DELTA = 0.10f
        const val SETTLE_MS = 1_500L
        const val SETTLE_TIMEOUT_MS = 20_000L
        const val MAX_SEEK_LEAD_MS = 8_000L
        const val SEEK_DETECT_MS = 2_500L
        const val SUPPRESS_MS = 1_500L
        const val GUEST_GRACE_MS = 2_500L
    }
}
