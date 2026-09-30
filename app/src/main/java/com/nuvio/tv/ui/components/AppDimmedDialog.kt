package com.nuvio.tv.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

/** G9f: raw dialogs have their own window and need the same input-transparent draw layer. */
@Composable
fun AppDimmedDialog(
    onDismissRequest: () -> Unit,
    properties: DialogProperties = DialogProperties(),
    content: @Composable () -> Unit,
) {
    Dialog(onDismissRequest = onDismissRequest, properties = properties) {
        DimmedWindowContent(content)
    }
}

/** Add-on/plugin QR and confirmation popups are separate windows too. */
@Composable
fun AppDimmedPopup(properties: PopupProperties = PopupProperties(), content: @Composable () -> Unit) {
    Popup(properties = properties) { DimmedWindowContent(content) }
}

@Composable
private fun DimmedWindowContent(content: @Composable () -> Unit) {
    Box(propagateMinConstraints = true) {
        content()
        // matchParentSize keeps the overlay from changing the window's measured size.
        AppDimmerOverlay(LocalAppDimPercent.current, Modifier.matchParentSize())
    }
}
