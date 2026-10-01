package com.nuvio.tv.fork.watchparty

/**
 * The peer channel (G11a). FILE_PORT of AntoninoScardina/NuvioTV `watchparty/WatchPartyTransport.kt`
 * @ ff597b1. On Android it is a hidden WebView running the VDO.Ninja SDK (signaling through
 * VDO.Ninja, data over a WebRTC data channel). Every listener call arrives on the main thread.
 */
interface WatchPartyTransport {
    fun join(room: String, password: String, label: String, listener: Listener)
    fun send(json: String, targetUuid: String? = null)
    fun leave()

    interface Listener {
        fun onJoined()
        fun onPeerJoined(uuid: String)
        fun onPeerLeft(uuid: String)
        fun onMessage(uuid: String, json: String)
        fun onError(error: WatchPartyError)
    }
}

/** Why a room failed, as a fixed code: transport text can carry hosts or links and is never shown or logged. */
enum class WatchPartyError { WEBVIEW_UNAVAILABLE, CONNECTION_FAILED }

/** The open player, as the Watch Party sees it. Called on the main thread only. */
interface WatchPartyPlayer {
    val positionMs: Long
    /** The intent to play (play-when-ready): stays true while buffering. */
    val isPlaying: Boolean
    val isBuffering: Boolean
    fun play()
    fun pause()
    fun seekTo(positionMs: Long)
    /** Guests catch up small drifts without a jump (1.0 = normal); pitch is kept. */
    fun setPlaybackSpeed(speed: Float)
}
