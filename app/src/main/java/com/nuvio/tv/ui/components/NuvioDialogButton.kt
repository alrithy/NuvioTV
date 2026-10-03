package com.nuvio.tv.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonColors
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ButtonScale
import androidx.tv.material3.ButtonShape
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.OutlinedButtonDefaults
import com.nuvio.tv.ui.theme.NetflixThemeTokens
import com.nuvio.tv.ui.theme.NuvioTheme

/** Shared dialog action styling, with the existing TV button's focus/key/click behavior. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun NuvioDialogButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.colors(),
    shape: ButtonShape = ButtonDefaults.shape(),
    scale: ButtonScale = ButtonDefaults.scale(),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        // NETFLIX_THEME: TV-sized remote targets; other themes keep their own sizing.
        modifier = if (NuvioTheme.isNetflix) modifier
            .heightIn(min = NetflixThemeTokens.Dialog.buttonHeight)
            .widthIn(min = NetflixThemeTokens.Dialog.buttonMinWidth) else modifier,
        enabled = enabled,
        colors = if (NuvioTheme.isNetflix) ButtonDefaults.colors(
            containerColor = NetflixThemeTokens.surfaceRaised,
            contentColor = NetflixThemeTokens.textPrimary,
            focusedContainerColor = NetflixThemeTokens.focus,
            focusedContentColor = NetflixThemeTokens.focusContent,
            disabledContainerColor = NetflixThemeTokens.surfaceRaised.copy(alpha = .5f),
            disabledContentColor = NetflixThemeTokens.textMuted
        ) else colors,
        shape = if (NuvioTheme.isNetflix) ButtonDefaults.shape(NetflixThemeTokens.buttonShape) else shape,
        scale = if (NuvioTheme.isNetflix) ButtonDefaults.scale(focusedScale = NetflixThemeTokens.episodeFocusScale) else scale,
        contentPadding = contentPadding,
        content = content
    )
}

/** Outlined actions retain their original TV Material defaults outside the Netflix theme. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun NuvioDialogOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = OutlinedButtonDefaults.colors(),
    shape: ButtonShape = OutlinedButtonDefaults.shape(),
    scale: ButtonScale = OutlinedButtonDefaults.scale(),
    contentPadding: PaddingValues = OutlinedButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit
) {
    if (NuvioTheme.isNetflix) {
        NuvioDialogButton(onClick, modifier, enabled, contentPadding = contentPadding, content = content)
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            colors = colors,
            shape = shape,
            scale = scale,
            contentPadding = contentPadding,
            content = content
        )
    }
}
