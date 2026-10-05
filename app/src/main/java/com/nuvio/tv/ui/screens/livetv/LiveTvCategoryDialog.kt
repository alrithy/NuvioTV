@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.nuvio.tv.ui.screens.livetv

import android.view.KeyEvent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.nuvio.tv.R
import com.nuvio.tv.fork.livetv.LiveTvChannel
import com.nuvio.tv.fork.livetv.LiveTvRepository
import com.nuvio.tv.fork.livetv.LiveTvState
import com.nuvio.tv.ui.components.NuvioDialog
import com.nuvio.tv.ui.theme.NuvioTheme
import com.nuvio.tv.ui.theme.NetflixThemeTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Picks the categories the list shows, and their order (G10b, features 220–223). FILE_PORT of
 * Reshaped `LiveTvCategoryDialog` @ 0ccf049, keyed by channel key; the repository is passed in. For people who only watch a few: Hide
 * all, then turn on the ones they want. Hidden categories leave the list, search and channel
 * switching; favorites always stay. Holding OK picks a category up, ▲▼ move it, OK drops it.
 * ▶ opens a category's channels to hide single ones.
 */
@Composable
internal fun LiveTvCategoryDialog(repository: LiveTvRepository, onDismiss: () -> Unit) {
    val uiState by repository.state.collectAsStateWithLifecycle()
    val firstFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { firstFocus.requestFocus() } }
    var moving by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    var openGroup by remember { mutableStateOf<String?>(null) }
    // Coming back from a category's channels focuses that category again.
    var returnTo by remember { mutableStateOf<String?>(null) }
    val group = openGroup

    NuvioDialog(
        onDismiss = onDismiss,
        title = group?.let { liveTvGroupLabel(it, uiState.library.groupNames) } ?: stringResource(R.string.live_tv_categories_title),
        subtitle = stringResource(if (group != null) R.string.live_tv_category_channels_description else R.string.live_tv_categories_description),
        width = 640.dp,
        usePlatformDefaultWidth = false,
        contentSpacing = NuvioTheme.spacing.md,
    ) {
        if (group != null) {
            LiveTvCategoryChannels(
                repository = repository,
                group = group,
                uiState = uiState,
                onBack = { openGroup = null },
            )
            return@NuvioDialog
        }
        val shown = uiState.groups.count { it !in uiState.library.hiddenGroups }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.sm),
        ) {
            Text(
                text = stringResource(R.string.live_tv_categories_shown, shown, uiState.groups.size),
                style = MaterialTheme.typography.bodySmall,
                color = NuvioTheme.colors.TextSecondary,
                modifier = Modifier.weight(1f),
            )
            LiveTvDialogButton(text = stringResource(R.string.live_tv_categories_show_all), onClick = { repository.setAllGroupsHidden(false) })
            LiveTvDialogButton(text = stringResource(R.string.live_tv_categories_hide_all), onClick = { repository.setAllGroupsHidden(true) })
            LiveTvDialogButton(text = stringResource(R.string.live_tv_categories_sort), onClick = { repository.resetGroupOrder() })
            LiveTvDialogButton(text = stringResource(R.string.live_tv_done), onClick = onDismiss)
        }
        if (uiState.groups.isEmpty()) {
            Text(
                text = stringResource(R.string.live_tv_categories_none),
                style = MaterialTheme.typography.bodyMedium,
                color = NuvioTheme.colors.TextSecondary,
            )
        } else {
            val returnFocus = remember { FocusRequester() }
            LaunchedEffect(Unit) {
                if (returnTo == null) return@LaunchedEffect
                withFrameNanos { }
                runCatching { returnFocus.requestFocus() }
            }
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp),
                contentPadding = PaddingValues(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                itemsIndexed(uiState.groups, key = { _, name -> name }) { index, name ->
                    val visible = name !in uiState.library.hiddenGroups
                    val isMoving = moving == name
                    LiveTvCategoryToggle(
                        label = liveTvGroupLabel(name, uiState.library.groupNames),
                        count = (uiState.groupCounts[name] ?: 0).toString(),
                        visible = visible,
                        moving = isMoving,
                        onToggle = { if (isMoving) moving = null else repository.setGroupHidden(name, visible) },
                        onPickUp = { moving = if (isMoving) null else name },
                        onMove = { step ->
                            val target = index + step
                            repository.moveGroup(name, step)
                            // Keep the moving row in view, a little away from the edge.
                            val shownRows = listState.layoutInfo.visibleItemsInfo
                            val first = shownRows.firstOrNull()?.index ?: 0
                            val last = shownRows.lastOrNull()?.index ?: 0
                            if (target <= first || target >= last) {
                                scope.launch { listState.scrollToItem((target - if (step < 0) 1 else shownRows.size - 2).coerceAtLeast(0)) }
                            }
                        },
                        onDrop = { moving = null },
                        onOpen = {
                            returnTo = name
                            openGroup = name
                        },
                        modifier = when (name) {
                            returnTo -> Modifier.focusRequester(returnFocus)
                            uiState.groups.firstOrNull() -> Modifier.focusRequester(firstFocus)
                            else -> Modifier
                        },
                    )
                }
            }
        }
    }
}

/** One category's channels, each shown or hidden on its own. Back returns to the categories. */
@Composable
private fun LiveTvCategoryChannels(repository: LiveTvRepository, group: String, uiState: LiveTvState, onBack: () -> Unit) {
    val channels by produceState(emptyList<LiveTvChannel>(), uiState.channels, group) {
        value = withContext(Dispatchers.Default) { uiState.channels.filter { it.group == group } }
    }
    val hidden = uiState.library.hiddenChannels
    val firstFocus = remember { FocusRequester() }
    LaunchedEffect(channels.isNotEmpty()) {
        if (channels.isEmpty()) return@LaunchedEffect
        withFrameNanos { }
        runCatching { firstFocus.requestFocus() }
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.md),
        modifier = Modifier.onPreviewKeyEvent { event ->
            val native = event.nativeKeyEvent
            val back = native.keyCode == KeyEvent.KEYCODE_BACK || native.keyCode == KeyEvent.KEYCODE_ESCAPE
            if (back && native.action == KeyEvent.ACTION_UP) onBack()
            back
        },
    ) {
        // The name the list shows; empty goes back to the playlist's own.
        LiveTvTextField(
            value = uiState.library.groupNames[group].orEmpty(),
            onValueChange = { repository.renameGroup(group, it) },
            placeholder = stringResource(R.string.live_tv_category_rename_hint, liveTvGroupLabel(group)),
            keyboardType = KeyboardType.Text,
            modifier = Modifier.fillMaxWidth(),
        )
        val shown = channels.count { it.key !in hidden }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.sm),
        ) {
            Text(
                text = stringResource(R.string.live_tv_categories_shown, shown, channels.size),
                style = MaterialTheme.typography.bodySmall,
                color = NuvioTheme.colors.TextSecondary,
                modifier = Modifier.weight(1f),
            )
            LiveTvDialogButton(
                text = stringResource(R.string.live_tv_categories_show_all),
                onClick = { repository.setChannelsHidden(channels, hidden = false) },
            )
            LiveTvDialogButton(
                text = stringResource(R.string.live_tv_categories_hide_all),
                onClick = { repository.setChannelsHidden(channels, hidden = true) },
            )
            LiveTvDialogButton(text = stringResource(R.string.live_tv_back), onClick = onBack)
        }
        LazyColumn(
            modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp),
            contentPadding = PaddingValues(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            itemsIndexed(channels, key = { _, channel -> channel.id }) { index, channel ->
                val visible = channel.key !in hidden
                LiveTvCategoryToggle(
                    label = channel.name,
                    count = null,
                    visible = visible,
                    onToggle = { repository.setChannelsHidden(listOf(channel), hidden = visible) },
                    modifier = if (index == 0) Modifier.focusRequester(firstFocus) else Modifier,
                )
            }
        }
    }
}

@Composable
private fun LiveTvCategoryToggle(
    label: String,
    count: String?,
    visible: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    moving: Boolean = false,
    onPickUp: (() -> Unit)? = null,
    onMove: (step: Int) -> Unit = {},
    onDrop: () -> Unit = {},
    onOpen: (() -> Unit)? = null,
) {
    var focused by remember { mutableStateOf(false) }
    val forwardKey = if (LocalLayoutDirection.current == LayoutDirection.Rtl) {
        KeyEvent.KEYCODE_DPAD_LEFT
    } else {
        KeyEvent.KEYCODE_DPAD_RIGHT
    }
    // A picked-up row lifts a little, like a card taken off the stack.
    val lift by animateFloatAsState(
        if (moving) 1.04f else 1f,
        if (NuvioTheme.isNetflix) tween(NetflixThemeTokens.focusDurationMillis)
        else spring(dampingRatio = 0.6f, stiffness = 500f),
        label = "liveTvCategoryLift"
    )
    val content = when {
        focused -> Color.Black
        visible -> NuvioTheme.colors.TextPrimary
        else -> NuvioTheme.colors.TextTertiary
    }
    Card(
        onClick = onToggle,
        onLongClick = onPickUp,
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = lift
                scaleY = lift
            }
            .onFocusChanged {
                focused = it.isFocused
                if (!it.isFocused && moving) onDrop()
            }
            .onPreviewKeyEvent { event ->
                val native = event.nativeKeyEvent
                if (!moving) {
                    // Forward opens a category's channels in either reading direction.
                    if (onOpen == null || native.keyCode != forwardKey) return@onPreviewKeyEvent false
                    if (native.action == KeyEvent.ACTION_DOWN && native.repeatCount == 0) onOpen()
                    return@onPreviewKeyEvent true
                }
                when (native.keyCode) {
                    KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN -> {
                        if (native.action == KeyEvent.ACTION_DOWN) onMove(if (native.keyCode == KeyEvent.KEYCODE_DPAD_UP) -1 else 1)
                        true
                    }
                    KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> {
                        if (native.action == KeyEvent.ACTION_UP) onDrop()
                        true
                    }
                    // Sideways would leave the row while carrying it.
                    KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT -> true
                    else -> false
                }
            },
        shape = CardDefaults.shape(if (NuvioTheme.isNetflix) NetflixThemeTokens.buttonShape else RoundedCornerShape(12.dp)),
        colors = CardDefaults.colors(
            containerColor = if (visible) NuvioTheme.colors.TextPrimary.copy(alpha = 0.08f) else Color.Transparent,
            focusedContainerColor = NuvioTheme.colors.TextPrimary,
        ),
        scale = CardDefaults.scale(focusedScale = if (NuvioTheme.isNetflix) NetflixThemeTokens.episodeFocusScale else 1.02f),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = content,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (count != null) {
                Text(
                    text = count,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (focused) Color.Black.copy(alpha = 0.55f) else NuvioTheme.colors.TextTertiary,
                    modifier = Modifier.padding(horizontal = NuvioTheme.spacing.sm),
                )
            }
            // A check for shown categories (arrows while it is being moved); empty for hidden ones.
            Icon(
                imageVector = if (moving) Icons.Filled.UnfoldMore else Icons.Filled.Check,
                contentDescription = null,
                tint = if (visible || moving) content else Color.Transparent,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
