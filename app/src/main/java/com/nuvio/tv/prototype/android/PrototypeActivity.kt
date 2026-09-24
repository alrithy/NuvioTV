package com.nuvio.tv.prototype.android

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import com.nuvio.tv.prototype.hub.PrototypeRoot
import com.nuvio.tv.prototype.hub.PrototypeState
import com.nuvio.tv.prototype.shared.ProtoEnv
import com.nuvio.tv.prototype.shared.ProtoLang
import com.nuvio.tv.prototype.shared.ProtoScreen

/**
 * Entry point for the isolated design-prototype system ("Nuvio Prototypes" on the TV launcher).
 *
 * Nothing in the production app references this activity. To remove the prototypes, delete the
 * `prototype` package, the `proto_*` fonts in res/font, `prototype_strings.xml` and the
 * activity entry in AndroidManifest.xml.
 *
 * Optional launch extras (handy over adb):
 *   --ei concept 1..10   --es screen HOME|FOCUSED|DETAILS|EPISODES|SEARCH|LIBRARY|STREAMS|PLAYER|SUBTITLES|AUDIO|PROFILE
 *   --es lang EN|AR      --ez illustrated true
 */
class PrototypeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val concept = intent.getIntExtra("concept", 0).takeIf { it in 1..10 }
        val screen = intent.getStringExtra("screen")
            ?.let { name -> ProtoScreen.entries.firstOrNull { it.name.equals(name, ignoreCase = true) } }
            ?: ProtoScreen.HOME
        val lang = if (intent.getStringExtra("lang").equals("AR", ignoreCase = true)) ProtoLang.AR else ProtoLang.EN
        val illustrated = intent.getBooleanExtra("illustrated", false)

        setContent {
            val fonts = remember { androidProtoFonts() }
            val env = remember { ProtoEnv(remoteArtwork = true, supportsBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S, frozen = false) }
            val state = remember { PrototypeState(concept, screen, lang, initialRemoteArt = !illustrated) }
            // Fallback for back presses that reach the activity without a focused Compose node.
            BackHandler(enabled = state.concept != null) { state.toHub() }
            PrototypeRoot(fonts = fonts, env = env, onExit = { finish() }, state = state)
        }
    }
}
