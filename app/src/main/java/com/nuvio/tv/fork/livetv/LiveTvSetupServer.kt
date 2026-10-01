package com.nuvio.tv.fork.livetv

import android.content.Context
import com.nuvio.tv.R
import fi.iki.elonen.NanoHTTPD
import java.io.FilterInputStream
import java.io.InputStream
import kotlinx.coroutines.runBlocking

/**
 * Local web page for setting up Live TV from a phone (G10g, feature 213): typing a playlist link or
 * a provider login with a TV remote is slow, and many TVs have no file picker for an .m3u file.
 * FILE_PORT of Reshaped `reshaped/livetv/LiveTvSetupServer.kt` @ 0ccf049 with its checks in
 * [LiveTvSetupPolicy]. Every path carries a random per-session [token] (it is in the QR code), so
 * only someone who can see the TV screen can reach the page; a post from another site's page is
 * refused by its Origin; forms and files are capped before they are read. Credentials arrive only
 * in the POST body and go straight to [LiveTvRepository] (encrypted per profile); nothing is
 * logged. Runs only while the sources dialog is open and the app is in the foreground.
 */
internal class LiveTvSetupServer(
    private val context: Context,
    private val repository: LiveTvRepository,
    port: Int,
) : NanoHTTPD(port) {

    val token: String = LiveTvSetupPolicy.newToken()
    private val pagePath = "/$token/"
    private val sourcePath = "/$token/api/source"
    private val playlistPath = "/$token/api/playlist"

    override fun serve(session: IHTTPSession): Response = when {
        session.method == Method.GET && (session.uri == pagePath || session.uri == "/$token") ->
            newFixedLengthResponse(Response.Status.OK, "text/html; charset=utf-8", LiveTvSetupWebPage.html(context, sourcePath, playlistPath))
        session.method == Method.POST && session.uri == sourcePath -> guarded(session) { handleSource(session) }
        session.method == Method.POST && session.uri == playlistPath -> guarded(session) { handlePlaylist(session) }
        else -> newFixedLengthResponse(Response.Status.FORBIDDEN, MIME_PLAINTEXT, "Forbidden").apply { closeConnection(true) }
    }

    private fun guarded(session: IHTTPSession, handle: () -> Response): Response {
        if (!LiveTvSetupPolicy.originAllowed(session.headers["origin"], session.headers["host"])) {
            return json(Response.Status.FORBIDDEN, """{"error":"forbidden"}""").apply { closeConnection(true) }
        }
        return handle()
    }

    private fun handleSource(session: IHTTPSession): Response {
        val length = session.headers["content-length"]?.toLongOrNull()
        if (!LiveTvSetupPolicy.formLengthAllowed(length)) {
            // The unread request body would otherwise be parsed as the next request.
            return json(Response.Status.BAD_REQUEST, """{"error":"invalid"}""").apply { closeConnection(true) }
        }
        val body = BoundedInputStream(session.inputStream, length!!).readBytes().toString(Charsets.UTF_8)
        when (val request = LiveTvSetupPolicy.parseForm(body)) {
            is LiveTvSetupRequest.M3u -> repository.addM3uUrl(request.url)
            is LiveTvSetupRequest.Xtream -> repository.addXtream(request.settings)
            is LiveTvSetupRequest.Stalker -> repository.addStalker(request.settings)
            null -> return json(Response.Status.BAD_REQUEST, """{"error":"invalid"}""")
        }
        return json(Response.Status.OK, """{"status":"loading"}""")
    }

    private fun handlePlaylist(session: IHTTPSession): Response {
        val length = session.headers["content-length"]?.toLongOrNull()
        LiveTvSetupPolicy.playlistRefusal(length)?.let { refusal ->
            return json(Response.Status.BAD_REQUEST, """{"error":"${refusal.code}"}""").apply { closeConnection(true) }
        }
        val name = LiveTvSetupPolicy.playlistName(session.queryParameterString)
        val saved = runBlocking {
            repository.importPlaylist(name, BoundedInputStream(session.inputStream, length!!), LiveTvSetupPolicy.MAX_PLAYLIST_BYTES)
        }
        return if (saved) {
            json(Response.Status.OK, """{"status":"loading"}""")
        } else {
            json(Response.Status.BAD_REQUEST, """{"error":"invalid"}""").apply { closeConnection(true) }
        }
    }

    private fun json(status: Response.Status, body: String): Response =
        newFixedLengthResponse(status, "application/json; charset=utf-8", body)

    /** Reads at most [remaining] bytes: NanoHTTPD's stream would otherwise block past the body. */
    private class BoundedInputStream(input: InputStream, private var remaining: Long) : FilterInputStream(input) {
        override fun read(): Int {
            if (remaining <= 0) return -1
            val value = super.read()
            if (value >= 0) remaining--
            return value
        }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            if (remaining <= 0) return -1
            val read = super.read(buffer, offset, minOf(length.toLong(), remaining).toInt())
            if (read > 0) remaining -= read
            return read
        }

        override fun close() = Unit
    }

    companion object {
        private const val START_PORT = 8110
        private const val MAX_ATTEMPTS = 10

        fun startOnAvailablePort(context: Context, repository: LiveTvRepository): LiveTvSetupServer? {
            for (port in START_PORT until START_PORT + MAX_ATTEMPTS) {
                try {
                    val server = LiveTvSetupServer(context.applicationContext, repository, port)
                    server.start(SOCKET_READ_TIMEOUT, false)
                    return server
                } catch (_: Exception) {
                }
            }
            return null
        }
    }
}

private object LiveTvSetupWebPage {
    fun html(context: Context, sourcePath: String, playlistPath: String): String {
        fun text(id: Int): String = context.getString(id)
            .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            .replace("'", "&#39;").replace("\"", "&quot;")

        fun js(value: String): String = buildString {
            value.forEach { char ->
                when (char) {
                    '\\' -> append("\\\\")
                    '\'' -> append("\\'")
                    '"' -> append("\\\"")
                    '<' -> append("\\u003c")
                    '>' -> append("\\u003e")
                    '&' -> append("\\u0026")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\u2028' -> append("\\u2028")
                    '\u2029' -> append("\\u2029")
                    else -> if (char < ' ') append("\\u%04x".format(char.code)) else append(char)
                }
            }
        }
        fun jsText(id: Int): String = js(context.getString(id))

        val title = text(R.string.live_tv_web_title)
        val subtitle = text(R.string.live_tv_web_subtitle)
        val tabM3u = text(R.string.live_tv_source_m3u)
        val tabXtream = text(R.string.live_tv_source_xtream)
        val tabStalker = text(R.string.live_tv_source_stalker)
        val linkLabel = text(R.string.live_tv_web_m3u_link)
        val fileLabel = text(R.string.live_tv_web_m3u_file)
        val linkHint = text(R.string.live_tv_m3u_hint)
        val serverHint = text(R.string.live_tv_xtream_server_hint)
        val userHint = text(R.string.live_tv_username_hint)
        val passHint = text(R.string.live_tv_password_hint)
        val optUserHint = text(R.string.live_tv_optional_username_hint)
        val optPassHint = text(R.string.live_tv_optional_password_hint)
        val portalHint = text(R.string.live_tv_stalker_portal_hint)
        val macHint = text(R.string.live_tv_stalker_mac_hint)
        val send = text(R.string.live_tv_web_send)
        val sending = jsText(R.string.live_tv_web_sending)
        val sent = jsText(R.string.live_tv_web_sent)
        val chooseFile = jsText(R.string.live_tv_web_choose_file)
        val tooLarge = jsText(R.string.live_tv_web_too_large)
        val failed = jsText(R.string.live_tv_web_failed)
        val connection = jsText(R.string.live_tv_web_connection_error)
        val sourceApi = js(sourcePath)
        val playlistApi = js(playlistPath)
        val maxBytes = LiveTvSetupPolicy.MAX_PLAYLIST_BYTES
        // Page language/direction follow the app (Arabic renders right-to-left), as the G6b font page.
        val locale = context.resources.configuration.locales[0]
        val lang = js(locale.language.ifBlank { "en" })
        val dir = if (android.text.TextUtils.getLayoutDirectionFromLocale(locale) ==
            android.view.View.LAYOUT_DIRECTION_RTL
        ) "rtl" else "ltr"

        return """
<!DOCTYPE html>
<html lang="$lang" dir="$dir">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>$title</title>
<style>
*{box-sizing:border-box;margin:0;padding:0;-webkit-tap-highlight-color:transparent}
body{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;background:#000;color:#fff;
display:flex;justify-content:center;min-height:100vh;padding:16px}
.card{max-width:560px;width:100%;padding:32px 0}
h1{font-size:22px;font-weight:600;margin-bottom:6px}
.subtitle{font-size:13px;color:rgba(255,255,255,0.5);margin-bottom:24px;line-height:1.4}
.tabs{display:flex;gap:6px;padding:4px;border-radius:100px;background:rgba(255,255,255,0.06);
border:1px solid rgba(255,255,255,0.08);margin-bottom:20px}
.tab{flex:1;padding:10px 0;border:0;border-radius:100px;background:transparent;color:rgba(255,255,255,0.6);
font-size:14px;font-weight:500;cursor:pointer;transition:background .2s,color .2s}
.tab.on{background:#fff;color:#000}
.pane{display:none}.pane.on{display:block}
label{display:block;font-size:12px;color:rgba(255,255,255,0.5);margin:14px 0 6px}
input{width:100%;padding:13px 16px;border-radius:14px;border:1px solid rgba(255,255,255,0.14);
background:rgba(255,255,255,0.04);color:#fff;font-size:15px;outline:none}
input:focus{border-color:rgba(255,255,255,0.5)}
.send{width:100%;margin-top:20px;padding:14px;border:0;border-radius:100px;background:#fff;color:#000;
font-size:15px;font-weight:600;cursor:pointer}
.send:disabled{opacity:.4}
.status{margin-top:14px;font-size:13px;text-align:center;min-height:20px}
.status.ok{color:#4caf50}.status.err{color:#ef5350}
</style>
</head>
<body>
<div class="card">
  <h1>$title</h1>
  <p class="subtitle">$subtitle</p>
  <div class="tabs">
    <button class="tab on" data-t="m3u">$tabM3u</button>
    <button class="tab" data-t="xtream">$tabXtream</button>
    <button class="tab" data-t="stalker">$tabStalker</button>
  </div>
  <div class="pane on" id="m3u">
    <label for="url">$linkLabel</label><input id="url" type="url" autocapitalize="off" placeholder="$linkHint">
    <label for="file">$fileLabel</label><input id="file" type="file" accept=".m3u,.m3u8,audio/x-mpegurl,application/x-mpegurl,text/plain">
  </div>
  <div class="pane" id="xtream">
    <label for="xs">$serverHint</label><input id="xs" type="url" autocapitalize="off" placeholder="http://">
    <label for="xu">$userHint</label><input id="xu" autocapitalize="off">
    <label for="xp">$passHint</label><input id="xp" type="password">
  </div>
  <div class="pane" id="stalker">
    <label for="sp">$portalHint</label><input id="sp" type="url" autocapitalize="off" placeholder="http://">
    <label for="sm">$macHint</label><input id="sm" autocapitalize="characters" placeholder="00:1A:79:">
    <label for="su">$optUserHint</label><input id="su" autocapitalize="off">
    <label for="sw">$optPassHint</label><input id="sw" type="password">
  </div>
  <button class="send" id="send">$send</button>
  <div id="status" class="status"></div>
</div>
<script>
let tab='m3u';const st=document.getElementById('status'),btn=document.getElementById('send');
const v=id=>document.getElementById(id).value.trim();
document.querySelectorAll('.tab').forEach(b=>b.onclick=()=>{tab=b.dataset.t;
document.querySelectorAll('.tab').forEach(x=>x.classList.toggle('on',x===b));
document.querySelectorAll('.pane').forEach(p=>p.classList.toggle('on',p.id===tab))});
function msg(t,ok){st.textContent=t;st.className='status '+(ok?'ok':'err')}
async function post(url,body,type){const r=await fetch(url,{method:'POST',headers:{'Content-Type':type},body:body});
let d={};try{d=await r.json()}catch(e){}return {ok:r.ok,d:d}}
btn.onclick=async()=>{let res;btn.disabled=true;msg('$sending',true);
try{if(tab==='m3u'){const f=document.getElementById('file').files[0];
if(f){if(f.size>$maxBytes){msg('$tooLarge',false);return}
res=await post('$playlistApi?name='+encodeURIComponent(f.name),f,'application/octet-stream')}
else if(v('url')){res=await post('$sourceApi',JSON.stringify({type:'m3u',url:v('url')}),'application/json')}
else{msg('$chooseFile',false);return}}
else if(tab==='xtream'){res=await post('$sourceApi',JSON.stringify({type:'xtream',server:v('xs'),username:v('xu'),password:v('xp')}),'application/json')}
else{res=await post('$sourceApi',JSON.stringify({type:'stalker',portal:v('sp'),mac:v('sm'),username:v('su'),password:v('sw')}),'application/json')}
if(res.ok)msg('$sent',true);else msg(res.d.error==='too_large'?'$tooLarge':'$failed',false)}
catch(e){msg('$connection',false)}finally{btn.disabled=false}};
</script>
</body>
</html>
""".trimIndent()
    }
}
