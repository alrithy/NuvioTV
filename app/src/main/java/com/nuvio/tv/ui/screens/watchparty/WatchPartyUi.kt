package com.nuvio.tv.ui.screens.watchparty

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.nuvio.tv.R
import com.nuvio.tv.fork.watchparty.WatchPartyEntryPoint
import com.nuvio.tv.fork.watchparty.WatchPartyError
import com.nuvio.tv.fork.watchparty.WatchPartyReceivedStreams
import com.nuvio.tv.fork.watchparty.WatchPartyRole
import com.nuvio.tv.fork.watchparty.WatchPartySession
import com.nuvio.tv.fork.watchparty.WatchPartySharePolicy
import com.nuvio.tv.fork.watchparty.WatchPartyState
import com.nuvio.tv.fork.watchparty.WatchPartyStatus
import com.nuvio.tv.fork.watchparty.watchPartyEnabled
import com.nuvio.tv.ui.navigation.Screen
import com.nuvio.tv.ui.screens.settings.ForkTextEntryDialog
import com.nuvio.tv.ui.screens.settings.SettingsActionRow
import com.nuvio.tv.ui.screens.settings.SettingsSectionLabel
import dagger.hilt.android.EntryPointAccessors
import java.net.URI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/*
 * Watch Party outside the player (G11b, D056; features 237–240): the app's one session, opening the
 * host's stream on a guest, and joining from Settings. FILE_PORT of AntoninoScardina/NuvioTV
 * `ui/screens/watchparty/WatchPartyNavigationEffect.kt` and `WatchPartyJoinScreen.kt` @ ff597b1;
 * the join screen became a Playback settings row with a code dialog.
 */

/** The app's Watch Party session, or null while WATCH_PARTY is OFF (then nothing shows anywhere). */
@Composable
internal fun rememberWatchPartySession(): WatchPartySession? {
    val context = LocalContext.current.applicationContext
    return remember(context) {
        val entry = EntryPointAccessors.fromApplication(context, WatchPartyEntryPoint::class.java)
        entry.watchPartySession().takeIf { entry.featureRegistry().watchPartyEnabled() }
    }
}

/**
 * Guest: when the host starts (or changes) a stream, the player opens on the same link. The link is
 * remembered as received, so this device never saves it for reuse, and reaches the player through an
 * in-memory ticket, so it is never in navigation saved state.
 */
@Composable
fun WatchPartyNavigationEffect(navController: NavHostController) {
    val session = rememberWatchPartySession() ?: return
    val request by session.mediaRequest.collectAsStateWithLifecycle()
    LaunchedEffect(request) {
        val media = request ?: return@LaunchedEffect
        session.consumeMediaRequest()
        // A host could name a server on the guest's own network: open only names that resolve to
        // public addresses (D056).
        val host = runCatching { URI(media.url).host }.getOrNull()
        val public = host != null && withContext(Dispatchers.IO) { WatchPartySharePolicy.resolvesToPublic(host) }
        if (!public) {
            session.reportStreamUnsupported()
            return@LaunchedEffect
        }
        // The route carries a ticket, not the link or its headers: route arguments are saved state.
        val route = Screen.Player.createRoute(
            streamUrl = WatchPartyReceivedStreams.register(media),
            title = media.title ?: WATCH_PARTY_FALLBACK_TITLE,
            streamName = media.streamName,
            headers = null,
            contentId = media.contentId,
            contentType = media.contentType,
            contentName = media.title,
            poster = media.poster,
            backdrop = media.backdrop,
            logo = media.logo,
            videoId = media.videoId,
            season = media.season,
            episode = media.episode,
            episodeTitle = media.subtitle,
            startFromBeginning = true,
        )
        val playerOpen = navController.currentDestination?.route == Screen.Player.route
        navController.navigate(route) {
            if (playerOpen) popUpTo(Screen.Player.route) { inclusive = true }
        }
    }
}

/** "Watch Party" in Playback settings: join a room by code, see where it stands, leave it. */
@Composable
internal fun watchPartySettingsItems() {
    val session = rememberWatchPartySession() ?: return
    val state by session.state.collectAsStateWithLifecycle()
    var showJoin by remember { mutableStateOf(false) }
    var invalidCode by remember { mutableStateOf(false) }

    SettingsSectionLabel(text = stringResource(R.string.watch_party_title))
    val inRoom = state.role != null && (state.isActive || state.status == WatchPartyStatus.ERROR)
    if (!inRoom) {
        SettingsActionRow(
            title = stringResource(R.string.watch_party_join_title),
            subtitle = stringResource(if (invalidCode) R.string.watch_party_invalid_code else R.string.watch_party_join_description),
            onClick = { showJoin = true },
        )
    } else {
        SettingsActionRow(
            title = stringResource(R.string.watch_party_room, formatWatchPartyCode(state.code)),
            subtitle = watchPartyStatusText(state),
            value = stringResource(R.string.watch_party_leave),
            onClick = { session.leaveRoom() },
        )
    }

    if (showJoin) {
        ForkTextEntryDialog(
            title = stringResource(R.string.watch_party_join_title),
            description = stringResource(R.string.watch_party_join_description),
            placeholder = stringResource(R.string.watch_party_join_code_label),
            confirmText = stringResource(R.string.watch_party_join),
            keyboardType = KeyboardType.Ascii,
            onDismiss = { showJoin = false },
            onConfirm = { entered ->
                showJoin = false
                invalidCode = !session.joinRoom(entered)
            },
        )
    }
}

/** What a room is doing, for the settings row and the player panel. */
@Composable
internal fun watchPartyStatusText(state: WatchPartyState): String = when {
    state.status == WatchPartyStatus.ERROR -> watchPartyErrorText(state.error)
    state.status == WatchPartyStatus.CONNECTING -> stringResource(R.string.watch_party_connecting)
    state.role == WatchPartyRole.GUEST && state.streamUnsupported -> stringResource(R.string.watch_party_stream_unsupported)
    state.participants.isEmpty() -> stringResource(R.string.watch_party_waiting)
    state.role == WatchPartyRole.GUEST && !state.hostPresent -> stringResource(R.string.watch_party_waiting_host)
    else -> stringResource(R.string.watch_party_participants, state.participants.joinToString(", "))
}

@Composable
internal fun watchPartyErrorText(error: WatchPartyError?): String = stringResource(
    when (error) {
        WatchPartyError.WEBVIEW_UNAVAILABLE -> R.string.watch_party_error_webview
        WatchPartyError.CONNECTION_FAILED, null -> R.string.watch_party_error_connection
    },
)

/** "ABC234" as "ABC 234", easier to read across a room. */
internal fun formatWatchPartyCode(code: String?): String = code.orEmpty().chunked(3).joinToString(" ")

private const val WATCH_PARTY_FALLBACK_TITLE = "Watch Party"
