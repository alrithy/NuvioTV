@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.nuvio.tv.ui.theme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.Text
import com.nuvio.tv.domain.model.AppTheme
import com.nuvio.tv.ui.components.NuvioDialogButton
import com.nuvio.tv.ui.components.NuvioDialogOutlinedButton
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NuvioDialogButtonThemeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun netflixDialogActionHasWhiteFocusAndBlackInheritedTextAndAcceptsRemoteClick() {
        var inheritedContentColor = Color.Unspecified
        var clicks = 0
        compose.setContent {
            NuvioTheme(appTheme = AppTheme.NETFLIX) {
                Box(Modifier.padding(32.dp)) {
                    NuvioDialogButton(onClick = { clicks++ }, modifier = Modifier.testTag("action")) {
                        val color = LocalContentColor.current
                        SideEffect { inheritedContentColor = color }
                        Text("Cancel")
                    }
                }
            }
        }
        val action = compose.onNodeWithTag("action")
        action.performSemanticsAction(SemanticsActions.RequestFocus) { it() }.assertIsFocused()
        compose.runOnIdle { assertEquals(Color.Black, inheritedContentColor) }
        val image = action.captureToImage().toPixelMap()
        // A point inside the leading edge avoids text and the rounded corner.
        assertEquals(Color.White, image[8, image.height / 2])
        action.performKeyInput { pressKey(Key.DirectionCenter) }
        compose.runOnIdle { assertEquals(1, clicks) }
    }

    @Test fun netflixOutlinedActionAlsoInheritsBlackTextOnFocus() {
        var inheritedContentColor = Color.Unspecified
        compose.setContent {
            NuvioTheme(appTheme = AppTheme.NETFLIX) {
                Box(Modifier.padding(32.dp)) {
                    NuvioDialogOutlinedButton(onClick = {}, modifier = Modifier.testTag("outlined")) {
                        val color = LocalContentColor.current
                        SideEffect { inheritedContentColor = color }
                        Text("More Info")
                    }
                }
            }
        }
        compose.onNodeWithTag("outlined")
            .performSemanticsAction(SemanticsActions.RequestFocus) { it() }.assertIsFocused()
        compose.runOnIdle { assertEquals(Color.Black, inheritedContentColor) }
    }

    @Test fun otherThemesKeepTheCallersShapePaddingAndColors() {
        compose.setContent {
            NuvioTheme(appTheme = AppTheme.OCEAN) {
                val colors = ButtonDefaults.colors(disabledContainerColor = Color.Magenta, disabledContentColor = Color.Black)
                val shape = ButtonDefaults.shape(RectangleShape)
                val padding = PaddingValues(horizontal = 32.dp, vertical = 24.dp)
                Row(Modifier.padding(32.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Button(onClick = {}, enabled = false, colors = colors, shape = shape, contentPadding = padding,
                        modifier = Modifier.testTag("original")) { Text("Settings") }
                    NuvioDialogButton(onClick = {}, enabled = false, colors = colors, shape = shape, contentPadding = padding,
                        modifier = Modifier.testTag("adapted")) { Text("Settings") }
                }
            }
        }
        val original = compose.onNodeWithTag("original")
        val adapted = compose.onNodeWithTag("adapted")
        val originalBounds = original.getUnclippedBoundsInRoot()
        val adaptedBounds = adapted.getUnclippedBoundsInRoot()
        assertEquals(originalBounds.width, adaptedBounds.width)
        assertEquals(originalBounds.height, adaptedBounds.height)
        val expected = original.captureToImage().toPixelMap()
        val actual = adapted.captureToImage().toPixelMap()
        assertEquals(Color.Magenta, actual[0, 0])
        assertEquals(expected[0, 0], actual[0, 0])
    }
}
