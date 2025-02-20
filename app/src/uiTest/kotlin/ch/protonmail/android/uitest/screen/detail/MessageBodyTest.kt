/*
 * Copyright (c) 2022 Proton Technologies AG
 * This file is part of Proton Technologies AG and Proton Mail.
 *
 * Proton Mail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Mail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Mail. If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.uitest.screen.detail

import java.util.UUID
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.onNodeWithTag
import ch.protonmail.android.mailcommon.domain.system.DeviceCapabilities.Capabilities
import ch.protonmail.android.mailcommon.presentation.system.LocalDeviceCapabilitiesProvider
import ch.protonmail.android.maildetail.presentation.sample.MessageDetailBodyUiModelSample
import ch.protonmail.android.maildetail.presentation.ui.MessageBody
import ch.protonmail.android.maildetail.presentation.ui.MessageBodyTestTags
import ch.protonmail.android.mailmessage.presentation.model.MessageBodyExpandCollapseMode
import ch.protonmail.android.mailmessage.presentation.ui.MessageBodyWebViewTestTags
import ch.protonmail.android.test.annotations.suite.RegressionTest
import ch.protonmail.android.uitest.util.HiltInstrumentedTest
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test

@RegressionTest
@HiltAndroidTest
class MessageBodyTest : HiltInstrumentedTest() {

    @Test
    fun shouldDisplayWebViewIfAvailable() {
        // given
        val messageContent = UUID.randomUUID().toString()
        val state = MessageDetailBodyUiModelSample.build(messageContent)

        // when
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDeviceCapabilitiesProvider provides Capabilities(hasWebView = true)) {
                MessageBody(
                    modifier = Modifier,
                    messageBodyUiModel = state,
                    actions = EmptyActions,
                    expandCollapseMode = MessageBodyExpandCollapseMode.NotApplicable
                )
            }
        }

        // then
        composeTestRule.onNodeWithTag(MessageBodyWebViewTestTags.WebView).assertExists()
        composeTestRule.onNodeWithTag(MessageBodyTestTags.WebViewAlternative).assertDoesNotExist()
    }

    @Test
    fun shouldDisplayWebViewAlternativeWhenWebViewNotAvailable() {
        // given
        val messageContent = UUID.randomUUID().toString()
        val state = MessageDetailBodyUiModelSample.build(messageContent)

        // when
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDeviceCapabilitiesProvider provides Capabilities(hasWebView = false)) {
                MessageBody(
                    modifier = Modifier,
                    messageBodyUiModel = state,
                    actions = EmptyActions,
                    expandCollapseMode = MessageBodyExpandCollapseMode.NotApplicable
                )
            }
        }

        // then
        composeTestRule.onNodeWithTag(MessageBodyWebViewTestTags.WebView).assertDoesNotExist()
        composeTestRule.onNodeWithTag(MessageBodyTestTags.WebViewAlternative).assertExists()
    }
}

private val EmptyActions = MessageBody.Actions(
    onExpandCollapseButtonClicked = {},
    onMessageBodyLinkClicked = {},
    onShowAllAttachments = {},
    onAttachmentClicked = {},
    loadEmbeddedImage = { _, _ -> null },
    onReply = {},
    onReplyAll = {},
    onForward = {},
    onEffectConsumed = { _, _ -> },
    onLoadRemoteContent = {},
    onLoadEmbeddedImages = {},
    onLoadRemoteAndEmbeddedContent = {},
    onOpenInProtonCalendar = {},
    onPrint = {},
    onViewEntireMessageClicked = { _, _, _, _ -> },
)
