package com.nuvio.tv.fork.watchparty

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject

/**
 * The VDO.Ninja transport (G11a, D056; features 245, 246). FILE_PORT of AntoninoScardina/NuvioTV
 * `watchparty/WebViewWatchPartyTransport.kt` @ ff597b1: a hidden WebView loads the bundled, unmodified
 * VDO.Ninja SDK (MPL-2.0, assets/watchparty/) and uses only its WebRTC data channel; signaling is
 * encrypted with the room password and peer data travels over DTLS. The made-up https origin gives
 * the page a secure context (crypto.subtle, which the SDK needs). Adapted (feature 249): the page's
 * console is never logged, errors are fixed codes, file and content access are off and the page
 * cannot navigate anywhere.
 */
class WebViewWatchPartyTransport(context: Context) : WatchPartyTransport {
    private val appContext = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private val json = Json { ignoreUnknownKeys = true }

    private var webView: WebView? = null
    private var listener: WatchPartyTransport.Listener? = null
    private var ready = false
    private val pending = mutableListOf<String>()

    @SuppressLint("SetJavaScriptEnabled")
    override fun join(room: String, password: String, label: String, listener: WatchPartyTransport.Listener) {
        this.listener = listener
        val view = runCatching { WebView(appContext) }.getOrElse {
            listener.onError(WatchPartyError.WEBVIEW_UNAVAILABLE)
            return
        }
        webView = view
        view.settings.javaScriptEnabled = true
        view.settings.domStorageEnabled = true
        view.settings.allowFileAccess = false
        view.settings.allowContentAccess = false
        view.webViewClient = object : WebViewClient() {
            // The page is ours and loaded from memory: it never goes anywhere else.
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean = true
        }
        view.webChromeClient = object : WebChromeClient() {
            // The SDK logs signaling detail; none of it is written anywhere.
            override fun onConsoleMessage(message: ConsoleMessage): Boolean = true
        }
        view.addJavascriptInterface(Bridge(), BRIDGE_NAME)
        pending += "window.wp.join(${quote(room)}, ${quote(password)}, ${quote(label)});"
        val html = runCatching { buildHtml() }.getOrElse {
            listener.onError(WatchPartyError.WEBVIEW_UNAVAILABLE)
            return
        }
        view.loadDataWithBaseURL(BASE_URL, html, "text/html", "utf-8", null)
    }

    override fun send(json: String, targetUuid: String?) {
        val target = targetUuid?.let(::quote) ?: "null"
        run("window.wp.send(${quote(json)}, $target);")
    }

    override fun leave() {
        val view = webView ?: return
        webView = null
        listener = null
        ready = false
        pending.clear()
        view.evaluateJavascript("window.wp && window.wp.leave();", null)
        // Gives the SDK a moment to say goodbye to the other peers before the WebView goes.
        main.postDelayed({
            view.removeJavascriptInterface(BRIDGE_NAME)
            view.destroy()
        }, LEAVE_GRACE_MS)
    }

    private fun run(script: String) {
        val view = webView ?: return
        if (ready) view.evaluateJavascript(script, null) else pending += script
    }

    private fun onEvent(raw: String) {
        val event = runCatching { json.parseToJsonElement(raw).jsonObject }.getOrNull() ?: return
        val l = listener ?: return
        fun str(key: String) = (event[key] as? JsonPrimitive)?.contentOrNull
        when (str("type")) {
            "ready" -> {
                ready = true
                val view = webView ?: return
                pending.forEach { view.evaluateJavascript(it, null) }
                pending.clear()
            }
            "joined" -> l.onJoined()
            "peerJoined" -> str("uuid")?.let(l::onPeerJoined)
            "peerLeft" -> str("uuid")?.let(l::onPeerLeft)
            "message" -> {
                val uuid = str("uuid") ?: return
                val data = event["data"] as? JsonObject ?: return
                l.onMessage(uuid, data.toString())
            }
            "error" -> l.onError(WatchPartyError.CONNECTION_FAILED)
            // A signaling drop the SDK will retry needs no action; one it gave up on ends the room.
            "signaling" -> {
                val terminal = str("state") == "disconnected" &&
                    str("willReconnect") == "false" && str("intentional") != "true"
                if (terminal) l.onError(WatchPartyError.CONNECTION_FAILED)
            }
        }
    }

    private inner class Bridge {
        @JavascriptInterface
        fun onEvent(raw: String) {
            main.post { this@WebViewWatchPartyTransport.onEvent(raw) }
        }
    }

    private fun buildHtml(): String {
        val sdk = asset("watchparty/vdoninja-sdk.js")
        val bridge = asset("watchparty/watchparty-bridge.js")
        return "<!doctype html><html><head><meta charset=\"utf-8\"></head><body>" +
            "<script>$sdk</script><script>$bridge</script></body></html>"
    }

    private fun asset(path: String): String =
        appContext.assets.open(path).bufferedReader().use { it.readText() }

    private fun quote(value: String): String = JsonPrimitive(value).toString()

    private companion object {
        const val BRIDGE_NAME = "NuvioWatchParty"
        const val BASE_URL = "https://watchparty.nuvio.app/"
        const val LEAVE_GRACE_MS = 1_500L
    }
}
