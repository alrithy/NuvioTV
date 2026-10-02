package com.nuvio.tv.ui.screens.livetv

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.nuvio.tv.ui.components.NuvioDialogButton
import com.nuvio.tv.ui.theme.NetflixThemeTokens
import com.nuvio.tv.ui.theme.NuvioTheme

/** Dialog presentation adapter; the Live TV screen and playback controls retain their owner. */
@Composable
internal fun LiveTvDialogButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    iconDescription: String? = null,
) {
    if (!NuvioTheme.isNetflix) {
        LiveTvPillButton(text, onClick, modifier, selected, enabled, icon, iconDescription)
        return
    }
    NuvioDialogButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        contentPadding = PaddingValues(
            horizontal = NetflixThemeTokens.actionGap,
            vertical = NetflixThemeTokens.metadataGap
        )
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = iconDescription,
                modifier = Modifier.size(NetflixThemeTokens.navigationIconSize))
        }
        if (text.isNotEmpty()) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = if (icon != null) Modifier.padding(start = NetflixThemeTokens.metadataGap) else Modifier
            )
        }
    }
}
