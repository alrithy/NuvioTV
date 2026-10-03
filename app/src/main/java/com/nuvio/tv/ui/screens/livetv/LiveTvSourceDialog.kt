@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.nuvio.tv.ui.screens.livetv

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.nuvio.tv.R
import com.nuvio.tv.core.qr.QrCodeGenerator
import com.nuvio.tv.core.server.DeviceIpAddress
import com.nuvio.tv.fork.livetv.LiveTvRepository
import com.nuvio.tv.fork.livetv.LiveTvSetupServer
import com.nuvio.tv.fork.livetv.LiveTvSource
import com.nuvio.tv.fork.livetv.LiveTvSourceType
import com.nuvio.tv.fork.livetv.LiveTvStalkerSettings
import com.nuvio.tv.fork.livetv.LiveTvXtreamSettings
import com.nuvio.tv.ui.components.NuvioDialog
import com.nuvio.tv.ui.theme.NuvioTheme
import com.nuvio.tv.ui.theme.NetflixThemeTokens

/**
 * Where the channels come from (G10b, features 209–212): the saved sources and a form to add one.
 * FILE_PORT of Reshaped `LiveTvSourceDialog` @ 0ccf049; the phone QR setup beside the form arrives
 * with G10g. After an add it shows the list again (and closes when that was the first source).
 * Saved sources show only their host or file name, never the link, user or password.
 */
@Composable
internal fun LiveTvSourceDialog(repository: LiveTvRepository, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val state by repository.state.collectAsStateWithLifecycle()
    var adding by rememberSaveable { mutableStateOf(!state.hasSource) }
    var tab by rememberSaveable { mutableStateOf(LiveTvSourceType.M3u) }
    var m3uUrl by rememberSaveable { mutableStateOf("") }
    var xtreamServer by rememberSaveable { mutableStateOf("") }
    var xtreamUser by rememberSaveable { mutableStateOf("") }
    // Passwords are not kept in saved instance state.
    var xtreamPassword by remember { mutableStateOf("") }
    var stalkerPortal by rememberSaveable { mutableStateOf("") }
    var stalkerMac by rememberSaveable { mutableStateOf("") }
    var stalkerUser by rememberSaveable { mutableStateOf("") }
    var stalkerPassword by remember { mutableStateOf("") }
    val firstFocus = remember { FocusRequester() }

    // Back to the list once a source was added (or entered again); the first source closes it.
    var seenAdds by remember { mutableIntStateOf(state.addedCount) }
    val openedEmpty = remember { state.sources.isEmpty() }
    LaunchedEffect(state.addedCount) {
        if (state.addedCount == seenAdds) return@LaunchedEffect
        seenAdds = state.addedCount
        if (openedEmpty && state.sources.size == 1) {
            onDismiss()
        } else {
            adding = false
            m3uUrl = ""; xtreamServer = ""; xtreamUser = ""; xtreamPassword = ""
            stalkerPortal = ""; stalkerMac = ""; stalkerUser = ""; stalkerPassword = ""
        }
    }
    // G10g: the phone page runs only while this dialog is open and the app is in the foreground
    // (a restart gets a new token, so a new QR code).
    var serverState by remember { mutableStateOf<LiveTvSetupServerState?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, repository) {
        var running: LiveTvSetupServer? = null
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> if (running == null) {
                    val started = startLiveTvSetupServer(context, repository)
                    running = started.server
                    serverState = started
                }
                Lifecycle.Event.ON_STOP -> {
                    running?.stop()
                    running = null
                }
                else -> Unit
            }
        }
        // Replays ON_START right away when the screen is already started.
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            running?.stop()
        }
    }
    // Removing the last source goes to the form; a removed row's focus moves to the first row.
    LaunchedEffect(adding, state.sources.size) {
        if (!state.hasSource) adding = true
        withFrameNanos { }
        runCatching { firstFocus.requestFocus() }
    }

    NuvioDialog(
        onDismiss = onDismiss,
        title = stringResource(if (adding) R.string.live_tv_source_add_title else R.string.live_tv_sources_title),
        subtitle = stringResource(if (adding) R.string.live_tv_source_description else R.string.live_tv_sources_description),
        width = 860.dp,
        usePlatformDefaultWidth = false,
        contentSpacing = NuvioTheme.spacing.md,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.xl),
        ) {
            LiveTvSetupQrColumn(serverState)
            // Scrolls when the screen is short; moving focus down brings each field into view.
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.sm),
            ) {
                if (!adding) {
                    var confirmRemoveId by remember { mutableStateOf<String?>(null) }
                    state.sources.forEachIndexed { index, source ->
                        LiveTvSourceRow(
                            source = source,
                            channelCount = state.sourceCounts[source.id] ?: 0,
                            error = state.sourceErrors[source.id]?.message(context),
                            confirmingRemove = confirmRemoveId == source.id,
                            onRemove = {
                                if (confirmRemoveId == source.id) {
                                    confirmRemoveId = null
                                    repository.removeSource(source.id)
                                } else {
                                    confirmRemoveId = source.id
                                }
                            },
                            modifier = if (index == 0) Modifier.focusRequester(firstFocus) else Modifier,
                        )
                    }
                    if (state.isLoading) {
                        Text(
                            text = stringResource(R.string.live_tv_loading),
                            style = MaterialTheme.typography.bodySmall,
                            color = NuvioTheme.colors.TextSecondary,
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = NuvioTheme.spacing.sm),
                        horizontalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.sm, Alignment.End),
                    ) {
                        LiveTvDialogButton(text = stringResource(R.string.live_tv_close), onClick = onDismiss)
                        LiveTvDialogButton(text = stringResource(R.string.live_tv_add_source_button), onClick = { adding = true })
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.sm)) {
                        LiveTvDialogButton(
                            text = stringResource(R.string.live_tv_source_m3u),
                            selected = tab == LiveTvSourceType.M3u,
                            onClick = { tab = LiveTvSourceType.M3u },
                            modifier = Modifier.focusRequester(firstFocus),
                        )
                        LiveTvDialogButton(
                            text = stringResource(R.string.live_tv_source_xtream),
                            selected = tab == LiveTvSourceType.Xtream,
                            onClick = { tab = LiveTvSourceType.Xtream },
                        )
                        LiveTvDialogButton(
                            text = stringResource(R.string.live_tv_source_stalker),
                            selected = tab == LiveTvSourceType.Stalker,
                            onClick = { tab = LiveTvSourceType.Stalker },
                        )
                    }

                    when (tab) {
                        LiveTvSourceType.M3u -> {
                            LiveTvTextField(m3uUrl, { m3uUrl = it }, stringResource(R.string.live_tv_m3u_hint))
                        }
                        LiveTvSourceType.Xtream -> {
                            LiveTvTextField(xtreamServer, { xtreamServer = it }, stringResource(R.string.live_tv_xtream_server_hint))
                            Row(horizontalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.sm)) {
                                LiveTvTextField(
                                    xtreamUser, { xtreamUser = it }, stringResource(R.string.live_tv_username_hint), Modifier.weight(1f),
                                    keyboardType = KeyboardType.Text,
                                )
                                LiveTvTextField(
                                    xtreamPassword, { xtreamPassword = it }, stringResource(R.string.live_tv_password_hint), Modifier.weight(1f),
                                    password = true,
                                )
                            }
                        }
                        LiveTvSourceType.Stalker -> {
                            LiveTvTextField(stalkerPortal, { stalkerPortal = it }, stringResource(R.string.live_tv_stalker_portal_hint))
                            LiveTvTextField(
                                stalkerMac, { stalkerMac = it }, stringResource(R.string.live_tv_stalker_mac_hint),
                                keyboardType = KeyboardType.Ascii,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.sm)) {
                                LiveTvTextField(
                                    stalkerUser, { stalkerUser = it }, stringResource(R.string.live_tv_optional_username_hint), Modifier.weight(1f),
                                    keyboardType = KeyboardType.Text,
                                )
                                LiveTvTextField(
                                    stalkerPassword, { stalkerPassword = it }, stringResource(R.string.live_tv_optional_password_hint),
                                    Modifier.weight(1f), password = true,
                                )
                            }
                        }
                    }

                    val status = when {
                        state.isLoading -> stringResource(R.string.live_tv_loading)
                        state.error != null -> state.error?.message(context)
                        else -> null
                    }
                    status?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (state.error != null && !state.isLoading) NuvioTheme.colors.Error else NuvioTheme.colors.TextSecondary,
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.sm, Alignment.End),
                    ) {
                        if (state.hasSource) {
                            LiveTvDialogButton(text = stringResource(R.string.live_tv_back), onClick = { adding = false })
                        } else {
                            LiveTvDialogButton(text = stringResource(R.string.live_tv_close), onClick = onDismiss)
                        }
                        LiveTvDialogButton(
                            text = stringResource(R.string.live_tv_load),
                            enabled = !state.isLoading,
                            onClick = {
                                when (tab) {
                                    LiveTvSourceType.M3u -> repository.addM3uUrl(m3uUrl)
                                    LiveTvSourceType.Xtream -> repository.addXtream(LiveTvXtreamSettings(xtreamServer, xtreamUser, xtreamPassword))
                                    LiveTvSourceType.Stalker -> repository.addStalker(
                                        LiveTvStalkerSettings(stalkerPortal, stalkerMac, stalkerUser, stalkerPassword),
                                    )
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

/** The phone setup page's QR code and link, or why it could not start (G10g). */
private class LiveTvSetupServerState(val server: LiveTvSetupServer?, val url: String?, val qr: Bitmap?, val error: String?)

private fun startLiveTvSetupServer(context: android.content.Context, repository: LiveTvRepository): LiveTvSetupServerState {
    val ip = DeviceIpAddress.get(context)
        ?: return LiveTvSetupServerState(null, null, null, context.getString(R.string.error_network_required))
    val server = LiveTvSetupServer.startOnAvailablePort(context, repository)
        ?: return LiveTvSetupServerState(null, null, null, context.getString(R.string.error_server_ports_unavailable))
    val url = "http://$ip:${server.listeningPort}/${server.token}/"
    return LiveTvSetupServerState(server, url, QrCodeGenerator.generate(url, 400), null)
}

@Composable
private fun LiveTvSetupQrColumn(serverState: LiveTvSetupServerState?) {
    Column(
        modifier = Modifier.width(200.dp),
        verticalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        serverState?.qr?.let { qr ->
            Image(
                bitmap = remember(qr) { qr.asImageBitmap() },
                contentDescription = stringResource(R.string.cd_qr_code),
                modifier = Modifier.size(170.dp),
                contentScale = ContentScale.Fit,
            )
        }
        Text(
            text = serverState?.error ?: stringResource(R.string.live_tv_phone_instruction),
            style = MaterialTheme.typography.bodySmall,
            color = NuvioTheme.colors.TextSecondary,
        )
        serverState?.url?.let { url ->
            Text(text = url, style = MaterialTheme.typography.labelSmall, color = NuvioTheme.colors.TextTertiary)
        }
    }
}

/** A saved source: its host or file name, kind and channel count (or why it failed), and a two-press Remove. */
@Composable
private fun LiveTvSourceRow(
    source: LiveTvSource,
    channelCount: Int,
    error: String?,
    confirmingRemove: Boolean,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val kind = stringResource(
        when (source.type) {
            LiveTvSourceType.M3u -> R.string.live_tv_source_m3u
            LiveTvSourceType.Xtream -> R.string.live_tv_source_xtream
            LiveTvSourceType.Stalker -> R.string.live_tv_source_stalker
        },
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (NuvioTheme.isNetflix) Modifier.background(NetflixThemeTokens.surfaceRaised, NetflixThemeTokens.buttonShape)
                else Modifier.clip(RoundedCornerShape(14.dp))
                    .background(NuvioTheme.colors.BackgroundElevated.copy(alpha = 0.6f))
            )
            .padding(start = NuvioTheme.spacing.lg, end = NuvioTheme.spacing.sm, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = source.label,
                style = MaterialTheme.typography.bodyLarge,
                color = NuvioTheme.colors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = error ?: stringResource(R.string.live_tv_source_summary, kind, channelCount),
                style = MaterialTheme.typography.bodySmall,
                color = if (error != null) NuvioTheme.colors.Error else NuvioTheme.colors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        LiveTvDialogButton(
            text = stringResource(if (confirmingRemove) R.string.live_tv_remove_confirm else R.string.live_tv_remove_source),
            onClick = onRemove,
            modifier = modifier,
        )
    }
}
