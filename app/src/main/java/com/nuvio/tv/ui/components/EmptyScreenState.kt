package com.nuvio.tv.ui.components

import com.nuvio.tv.ui.theme.NuvioTheme
import com.nuvio.tv.ui.theme.NetflixThemeTokens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import com.nuvio.tv.ui.util.contentTextDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun EmptyScreenState(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    height: Dp = 400.dp
) {
    if (NuvioTheme.isNetflix) {
        NetflixStatePanel(title, subtitle?.let { androidx.compose.ui.text.AnnotatedString(it) },
            modifier = modifier.fillMaxWidth().height(height), icon = icon)
        return
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(if (NuvioTheme.isNetflix) NetflixThemeTokens.State.iconSize else 80.dp),
                tint = NuvioTheme.colors.TextTertiary
            )
            Spacer(modifier = Modifier.height(NuvioTheme.spacing.xl))
        }

        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall.copy(textDirection = title.contentTextDirection()),
            color = NuvioTheme.colors.TextPrimary,
            textAlign = TextAlign.Center
        )

        if (subtitle != null) {
            Spacer(modifier = Modifier.height(NuvioTheme.spacing.sm))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium.copy(textDirection = subtitle.contentTextDirection()),
                color = NuvioTheme.colors.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = if (NuvioTheme.isNetflix) {
                    Modifier.widthIn(max = NetflixThemeTokens.State.maxTextWidth)
                        .padding(horizontal = NetflixThemeTokens.safeVerticalMargin)
                } else Modifier
            )
        }
    }
}
