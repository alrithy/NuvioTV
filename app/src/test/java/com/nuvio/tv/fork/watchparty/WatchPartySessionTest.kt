package com.nuvio.tv.fork.watchparty

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** G11a: consent, received media, play / pause / seek sync, soft drift and hard seek, lifecycle; G11 corrections. */
@OptIn(ExperimentalCoroutinesApi::class)
class WatchPartySessionTest {
    private val json = Json { ignoreUnknownKeys = true }

    private class FakeTransport : WatchPartyTransport {
        var room: String? = null
        var password: String? = null
        var listener: WatchPartyTransport.Listener? = null
        val sent = mutableListOf<Pair<String, String?>>()
        var left = false

        override fun join(room: String, password: String, label: String, listener: WatchPartyTransport.Listener) {
            this.room = room
            this.password = password
            this.listener = listener
        }

        override fun send(json: String, targetUuid: String?) {
            sent += json to targetUuid
        }

        override fun leave() {
            left = true
        }
    }

    /** A player whose position runs with the test clock while it plays. */
    private class FakePlayer(override val selectedSpeed: Float = 1f, private val clock: () -> Long) : WatchPartyPlayer {
        private var base = 0L
        private var since = 0L
        var playing = true
            private set
        var speed = selectedSpeed
            private set
        val seeks = mutableListOf<Long>()
        override var isBuffering = false

        override val positionMs: Long
            get() = if (playing) base + ((clock() - since) * speed).toLong() else base

        override val isPlaying: Boolean get() = playing

        private fun rebase() {
            base = positionMs
            since = clock()
        }

        override fun play() { rebase(); playing = true }
        override fun pause() { rebase(); playing = false }
        override fun seekTo(positionMs: Long) { seeks += positionMs; base = positionMs; since = clock() }
        override fun setPlaybackSpeed(speed: Float) { rebase(); this.speed = speed }
    }

    private val media = WatchPartyMedia(url = "https://cdn.example/film.mkv", headers = mapOf("Referer" to "https://site.example/"), title = "Film")

    private fun TestScope.session(transports: MutableList<FakeTransport>) = WatchPartySession(
        transportFactory = { FakeTransport().also(transports::add) },
        deviceName = { "Living room TV" },
        scope = backgroundScope,
        clockMs = { testScheduler.currentTime },
    )

    private fun FakeTransport.messages(): List<Pair<WatchPartyWire, String?>> =
        sent.map { (text, target) -> json.decodeFromString(WatchPartyWire.serializer(), text) to target }

    private fun wire(msg: WatchPartyWire) = json.encodeToString(WatchPartyWire.serializer(), msg)

    @Test
    fun noRoomIsCreatedWithoutConsentAndAHostSharesOnlyAfterIt() = runTest {
        val transports = mutableListOf<FakeTransport>()
        val party = session(transports)
        assertNull(party.createRoom(sharingConsented = false))
        assertTrue(transports.isEmpty())

        val code = party.createRoom(sharingConsented = true)!!
        val t = transports.single()
        assertEquals(WatchPartyProtocol.roomFor(code), t.room)
        assertEquals(WatchPartyProtocol.passwordFor(code), t.password)
        assertEquals(WatchPartyStatus.CONNECTING, party.state.value.status)
        t.listener!!.onJoined()
        assertEquals(WatchPartyStatus.CONNECTED, party.state.value.status)

        party.attachPlayer(FakePlayer { testScheduler.currentTime }, media)
        t.listener!!.onPeerJoined("guest-1")
        val toGuest = t.messages().filter { it.second == "guest-1" }.map { it.first }
        assertEquals(listOf(WatchPartyProtocol.HELLO, WatchPartyProtocol.MEDIA), toGuest.map { it.type })
        assertEquals(media.url, toGuest[1].media!!.url)
        assertEquals("Living room TV", toGuest[0].name)
        party.leaveRoom()
    }

    @Test
    fun aGuestOpensOnlyAcceptableMediaWithAllowListedHeaders() = runTest {
        val transports = mutableListOf<FakeTransport>()
        val party = session(transports)
        assertFalse(party.joinRoom("bad"))
        assertTrue(party.joinRoom("abc 234"))
        val l = transports.single().listener!!
        l.onMessage("host", wire(WatchPartyWire(type = WatchPartyProtocol.MEDIA, media = media.copy(url = "http://127.0.0.1:11470/x"), positionMs = 0)))
        assertNull(party.mediaRequest.value)
        l.onMessage("host", wire(WatchPartyWire(type = WatchPartyProtocol.MEDIA, media = media.copy(headers = media.headers + ("Cookie" to "sid")), positionMs = 0, playing = true)))
        assertEquals(mapOf("Referer" to "https://site.example/"), party.mediaRequest.value!!.headers)
        party.consumeMediaRequest()
        assertNull(party.mediaRequest.value)
        party.leaveRoom()
    }

    @Test
    fun guestsSeekOnLargeDriftAndChangeSpeedOnSmallDrift() = runTest {
        val transports = mutableListOf<FakeTransport>()
        val party = session(transports)
        party.joinRoom("ABC234")
        val l = transports.single().listener!!
        val player = FakePlayer { testScheduler.currentTime }
        party.attachPlayer(player, media)
        // Host at the same position: nothing to correct.
        l.onMessage("host", wire(WatchPartyWire(type = WatchPartyProtocol.MEDIA, media = media, positionMs = 0, playing = true)))
        assertTrue(player.seeks.isEmpty())
        advanceTimeBy(2_000); runCurrent()
        assertEquals(1f, player.speed)

        // One second behind while playing: catch up a little faster, no jump (247).
        l.onMessage("host", wire(WatchPartyWire(type = WatchPartyProtocol.STATE, positionMs = player.positionMs + 1_000, playing = true)))
        assertTrue(player.speed > 1f && player.speed <= 1.1f)
        assertTrue(player.seeks.isEmpty())

        // Five seconds behind: a seek (248).
        l.onMessage("host", wire(WatchPartyWire(type = WatchPartyProtocol.STATE, positionMs = player.positionMs + 5_000, playing = true)))
        assertEquals(1, player.seeks.size)
        assertEquals(1f, player.speed)
        party.leaveRoom()
    }

    @Test
    fun theHostFollowsGuestCommandsAndGuestsFollowTheHostsPause() = runTest {
        val transports = mutableListOf<FakeTransport>()
        val host = session(transports)
        host.createRoom(sharingConsented = true)
        val hostTransport = transports.single()
        val hostPlayer = FakePlayer { testScheduler.currentTime }
        host.attachPlayer(hostPlayer, media)
        hostTransport.listener!!.onPeerJoined("guest-1")
        hostTransport.sent.clear()

        hostTransport.listener!!.onMessage("guest-1", wire(WatchPartyWire(type = WatchPartyProtocol.CMD, action = WatchPartyProtocol.ACTION_PAUSE, positionMs = hostPlayer.positionMs)))
        assertFalse(hostPlayer.playing)
        hostTransport.listener!!.onMessage("guest-1", wire(WatchPartyWire(type = WatchPartyProtocol.CMD, action = WatchPartyProtocol.ACTION_SEEK, positionMs = 60_000)))
        assertEquals(listOf(60_000L), hostPlayer.seeks)
        assertTrue(hostTransport.messages().any { it.first.type == WatchPartyProtocol.STATE && it.first.positionMs == 60_000L })
        host.leaveRoom()

        // A guest pauses when the host's state says paused (241).
        val guestTransports = mutableListOf<FakeTransport>()
        val guest = session(guestTransports)
        guest.joinRoom("ABC234")
        val guestPlayer = FakePlayer { testScheduler.currentTime }
        guest.attachPlayer(guestPlayer, media)
        guestTransports.single().listener!!.onMessage("host", wire(WatchPartyWire(type = WatchPartyProtocol.MEDIA, media = media, positionMs = 0, playing = false)))
        assertFalse(guestPlayer.playing)
        guest.leaveRoom()
    }

    @Test
    fun aGuestsOwnPauseGoesToTheHostAsACommand() = runTest {
        val transports = mutableListOf<FakeTransport>()
        val guest = session(transports)
        guest.joinRoom("ABC234")
        val t = transports.single()
        val player = FakePlayer { testScheduler.currentTime }
        guest.attachPlayer(player, media)
        t.listener!!.onMessage("host", wire(WatchPartyWire(type = WatchPartyProtocol.MEDIA, media = media, positionMs = 0, playing = true)))
        advanceTimeBy(3_000); runCurrent()
        t.sent.clear()
        player.pause()
        advanceTimeBy(600); runCurrent()
        val cmd = t.messages().map { it.first }.first { it.type == WatchPartyProtocol.CMD }
        assertEquals(WatchPartyProtocol.ACTION_PAUSE, cmd.action)
        assertEquals("host", t.messages().first { it.first.type == WatchPartyProtocol.CMD }.second)
        guest.leaveRoom()
    }

    @Test
    fun leavingSaysGoodbyeClearsTheRoomAndIgnoresTheOldTransport() = runTest {
        val transports = mutableListOf<FakeTransport>()
        val party = session(transports)
        party.joinRoom("ABC234")
        val old = transports.single()
        val oldListener = old.listener!!
        party.leaveRoom()
        assertTrue(old.left)
        assertEquals(WatchPartyProtocol.BYE, old.messages().last().first.type)
        assertEquals(WatchPartyState(), party.state.value)

        oldListener.onJoined()
        oldListener.onMessage("host", wire(WatchPartyWire(type = WatchPartyProtocol.MEDIA, media = media, positionMs = 0)))
        assertEquals(WatchPartyState(), party.state.value)
        assertNull(party.mediaRequest.value)

        party.joinRoom("ABC234")
        transports.last().listener!!.onError(WatchPartyError.CONNECTION_FAILED)
        assertEquals(WatchPartyStatus.ERROR, party.state.value.status)
        assertEquals(WatchPartyError.CONNECTION_FAILED, party.state.value.error)
        party.leaveRoom()
    }

    @Test
    fun aGuestFollowsOnlyItsHostAndAnotherPeerCannotTakeOver() = runTest {
        val transports = mutableListOf<FakeTransport>()
        val guest = session(transports)
        guest.joinRoom("ABC234")
        val l = transports.single().listener!!
        val player = FakePlayer { testScheduler.currentTime }
        guest.attachPlayer(player, media)
        l.onMessage("host", wire(WatchPartyWire(type = WatchPartyProtocol.HELLO, name = "Host", host = true)))
        l.onMessage("host", wire(WatchPartyWire(type = WatchPartyProtocol.MEDIA, media = media, positionMs = 0, playing = true)))
        // Another participant claims to be host: its stream is not opened and its state is not applied.
        l.onMessage("mallory", wire(WatchPartyWire(type = WatchPartyProtocol.HELLO, name = "M", host = true)))
        l.onMessage("mallory", wire(WatchPartyWire(type = WatchPartyProtocol.MEDIA, media = media.copy(url = "https://evil.example/x"), positionMs = 0)))
        assertNull(guest.mediaRequest.value)
        l.onMessage("mallory", wire(WatchPartyWire(type = WatchPartyProtocol.STATE, positionMs = 600_000, playing = false)))
        assertTrue(player.seeks.isEmpty())
        assertTrue(player.playing)
        guest.leaveRoom()
    }

    @Test
    fun whenTheHostLeavesNothingItSaidIsAppliedAnyMore() = runTest {
        val transports = mutableListOf<FakeTransport>()
        val guest = session(transports)
        guest.joinRoom("ABC234")
        val l = transports.single().listener!!
        val player = FakePlayer { testScheduler.currentTime }
        guest.attachPlayer(player, media)
        l.onPeerJoined("host")
        l.onMessage("host", wire(WatchPartyWire(type = WatchPartyProtocol.MEDIA, media = media, positionMs = 0, playing = true)))
        advanceTimeBy(2_000); runCurrent()
        l.onMessage("host", wire(WatchPartyWire(type = WatchPartyProtocol.STATE, positionMs = player.positionMs + 1_000, playing = true)))
        assertTrue(player.speed > 1f)
        l.onPeerLeft("host")
        assertEquals(1f, player.speed)
        assertFalse(guest.state.value.hostPresent)
        // The guest pauses on its own; with no host it is not forced back into playback or moved.
        player.pause()
        advanceTimeBy(10_000); runCurrent()
        assertFalse(player.playing)
        assertTrue(player.seeks.isEmpty())
        // A new host may then lead.
        l.onMessage("host-2", wire(WatchPartyWire(type = WatchPartyProtocol.STATE, positionMs = 120_000, playing = false)))
        assertNull(guest.mediaRequest.value)
        guest.leaveRoom()
    }

    @Test
    fun guestsFollowTheHostsSpeedAndGetTheirOwnBackWhenTheRoomEnds() = runTest {
        val hostTransports = mutableListOf<FakeTransport>()
        val host = session(hostTransports)
        host.createRoom(sharingConsented = true)
        host.attachPlayer(FakePlayer(selectedSpeed = 1.5f) { testScheduler.currentTime }, media)
        hostTransports.single().listener!!.onPeerJoined("guest-1")
        val sentMedia = hostTransports.single().messages().first { it.first.type == WatchPartyProtocol.MEDIA }.first
        assertEquals(1.5f, sentMedia.rate)
        host.leaveRoom()

        // At 1x the wire stays protocol v1: no rate field at all.
        val plainTransports = mutableListOf<FakeTransport>()
        val plain = session(plainTransports)
        plain.createRoom(sharingConsented = true)
        plain.attachPlayer(FakePlayer { testScheduler.currentTime }, media)
        plainTransports.single().listener!!.onPeerJoined("guest-1")
        assertTrue(plainTransports.single().sent.none { "rate" in it.first })
        plain.leaveRoom()

        val transports = mutableListOf<FakeTransport>()
        val guest = session(transports)
        guest.joinRoom("ABC234")
        val l = transports.single().listener!!
        val player = FakePlayer(selectedSpeed = 1.25f) { testScheduler.currentTime }
        guest.attachPlayer(player, media)
        l.onMessage("host", wire(WatchPartyWire(type = WatchPartyProtocol.MEDIA, media = media, positionMs = 0, playing = true, rate = 1.5f)))
        advanceTimeBy(1_000); runCurrent()
        assertEquals(1.5f, player.speed)
        // In step at 1.5x for a while: no seeks, the estimate runs at the host's speed.
        advanceTimeBy(20_000); runCurrent()
        assertTrue(player.seeks.isEmpty())
        // An out-of-range rate is ignored (treated as 1x).
        l.onMessage("host", wire(WatchPartyWire(type = WatchPartyProtocol.STATE, positionMs = player.positionMs, playing = true, rate = 50f)))
        assertEquals(1f, player.speed)
        guest.leaveRoom()
        assertEquals(1.25f, player.speed)
    }

    @Test
    fun aGuestIsToldWhenTheHostsStreamCannotOpenHere() = runTest {
        val transports = mutableListOf<FakeTransport>()
        val guest = session(transports)
        guest.joinRoom("ABC234")
        val l = transports.single().listener!!
        l.onJoined()
        l.onMessage("host", wire(WatchPartyWire(type = WatchPartyProtocol.MEDIA, media = media.copy(url = "http://192.168.1.10/x.mkv"), positionMs = 0)))
        assertTrue(guest.state.value.streamUnsupported)
        assertNull(guest.mediaRequest.value)
        l.onMessage("host", wire(WatchPartyWire(type = WatchPartyProtocol.MEDIA, media = media, positionMs = 0)))
        assertFalse(guest.state.value.streamUnsupported)
        guest.reportStreamUnsupported()
        assertTrue(guest.state.value.streamUnsupported)
        guest.leaveRoom()
        assertFalse(guest.state.value.streamUnsupported)

        // A host is never marked.
        val host = session(mutableListOf())
        host.createRoom(sharingConsented = true)
        host.reportStreamUnsupported()
        assertFalse(host.state.value.streamUnsupported)
        host.leaveRoom()
    }
}
