package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import com.example.ui.components.BottomChatComposer
import com.example.ui.theme.VoxelStudioTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun verifyStudioStringsAndResources() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("MC Mod Studio", context.getString(R.string.app_name))
        assertEquals("Minecraft AI Mod Maker Studio", context.getString(R.string.studio_title))
    }

    @Test
    fun bottomChatComposer_rendersHintAttachAndSendControls() {
        composeRule.setContent {
            VoxelStudioTheme {
                BottomChatComposer(
                    inputText = "",
                    onInputChange = {},
                    pendingAttachment = null,
                    onClearAttachment = {},
                    onPickImage = {},
                    onPickFile = {},
                    isGenerating = false,
                    onSend = {}
                )
            }
        }
        composeRule.onNodeWithText("Describe your Minecraft Mod idea...").assertIsDisplayed()
        composeRule.onNodeWithTag("attach_button").assertIsDisplayed()
        composeRule.onNodeWithTag("send_button").assertIsDisplayed()
        composeRule.onNodeWithTag("chat_input_field").assertIsDisplayed()
    }
}
