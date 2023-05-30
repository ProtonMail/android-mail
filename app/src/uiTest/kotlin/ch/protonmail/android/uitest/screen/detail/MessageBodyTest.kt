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
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithTag
import ch.protonmail.android.mailcommon.domain.system.DeviceCapabilities.Capabilities
import ch.protonmail.android.mailcommon.presentation.system.LocalDeviceCapabilitiesProvider
import ch.protonmail.android.maildetail.presentation.sample.MessageDetailBodyUiModelSample
import ch.protonmail.android.maildetail.presentation.ui.MessageBody
import ch.protonmail.android.maildetail.presentation.ui.MessageBodyTestTags
import ch.protonmail.android.uitest.util.ComposeTestRuleHolder
import org.junit.Rule
import org.junit.Test

class MessageBodyTest {

    @get:Rule
    val rule: ComposeContentTestRule = ComposeTestRuleHolder.createAndGetComposeRule()

    @Test
    fun shouldDisplayWebViewIfAvailable() {
        // given
        val messageContent = UUID.randomUUID().toString()
        val state = MessageDetailBodyUiModelSample.build(messageContent)

        // when
        rule.setContent {
            CompositionLocalProvider(LocalDeviceCapabilitiesProvider provides Capabilities(hasWebView = true)) {
                MessageBody(
                    modifier = Modifier,
                    messageBodyUiModel = state,
                    actions = MessageBody.Actions({}, {}),
                )
            }
        }

        // then
        rule.onNodeWithTag(MessageBodyTestTags.WebView).assertExists()
        rule.onNodeWithTag(MessageBodyTestTags.WebViewAlternative).assertDoesNotExist()
    }

    @Test
    fun shouldDisplayWebViewAlternativeWhenWebViewNotAvailable() {
        // given
        val messageContent = UUID.randomUUID().toString()
        val state = MessageDetailBodyUiModelSample.build(messageContent)

        // when
        rule.setContent {
            CompositionLocalProvider(LocalDeviceCapabilitiesProvider provides Capabilities(hasWebView = false)) {
                MessageBody(
                    modifier = Modifier,
                    messageBodyUiModel = state,
                    actions = MessageBody.Actions({}, {}),
                )
            }
        }

        // then
        rule.onNodeWithTag(MessageBodyTestTags.WebView).assertDoesNotExist()
        rule.onNodeWithTag(MessageBodyTestTags.WebViewAlternative).assertExists()
    }
}
