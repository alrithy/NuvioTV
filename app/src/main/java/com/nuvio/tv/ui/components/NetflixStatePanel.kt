@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.nuvio.tv.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.nuvio.tv.ui.theme.NetflixThemeTokens
import com.nuvio.tv.ui.util.contentTextDirection
import com.nuvio.tv.ui.theme.withNuvioDescenderRoom

/**
 * NETFLIX_THEME empty / error composition (parity audit §18): an intentional centred block — icon,
 * clear title, one short line, and a real action when one exists — instead of a small text island.
 */
@Composable
internal fun NetflixStatePanel(
    title: String,
    body: AnnotatedString?,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    actionModifier: Modifier = Modifier,
) {
    val tokens = NetflixThemeTokens
    val state = NetflixThemeTokens.State
    Column(
        modifier = modifier.padding(horizontal = tokens.safeMargin),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(state.gap, Alignment.CenterVertically),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = tokens.textSecondary, modifier = Modifier.size(state.iconSize))
        }
        Text(
            title,
            style = TextStyle(fontFamily = tokens.fontFamily, fontSize = state.titleSize, fontWeight = FontWeight.Bold,
                textDirection = title.contentTextDirection()).withNuvioDescenderRoom(),
            color = tokens.textPrimary, textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = state.maxTextWidth),
        )
        if (body != null && body.isNotBlank()) {
            Text(
                body,
                style = TextStyle(fontFamily = tokens.fontFamily, fontSize = state.bodySize, lineHeight = tokens.descriptionLineHeight,
                    textDirection = body.text.contentTextDirection()).withNuvioDescenderRoom(),
                color = tokens.textSecondary, textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = state.maxTextWidth),
            )
        }
        if (actionLabel != null && onAction != null) {
            Button(
                onClick = onAction,
                modifier = actionModifier.padding(top = tokens.metadataGap).height(tokens.buttonHeight).widthIn(min = state.actionMinWidth),
                shape = ButtonDefaults.shape(tokens.buttonShape),
                colors = ButtonDefaults.colors(
                    containerColor = tokens.focus.copy(alpha = tokens.secondaryActionFillAlpha), contentColor = tokens.textPrimary,
                    focusedContainerColor = tokens.focus, focusedContentColor = tokens.focusContent,
                ),
                scale = ButtonDefaults.scale(focusedScale = tokens.episodeFocusScale),
                contentPadding = PaddingValues(horizontal = tokens.previewPadding * 2),
            ) {
                Text(actionLabel, style = TextStyle(fontFamily = tokens.fontFamily, fontSize = tokens.buttonText, fontWeight = FontWeight.Medium).withNuvioDescenderRoom())
            }
        }
    }
}

private fun AnnotatedString.isNotBlank() = text.isNotBlank()
