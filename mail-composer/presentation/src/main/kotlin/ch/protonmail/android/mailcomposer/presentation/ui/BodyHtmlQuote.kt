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

package ch.protonmail.android.mailcomposer.presentation.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.presentation.model.MessageBodyExpandCollapseMode
import ch.protonmail.android.mailmessage.presentation.model.MessageBodyUiModel
import ch.protonmail.android.mailmessage.presentation.model.MimeTypeUiModel
import ch.protonmail.android.mailmessage.presentation.model.ViewModePreference
import ch.protonmail.android.mailmessage.presentation.ui.MessageBodyWebView

@Composable
internal fun BodyHtmlQuote(
    value: String,
    shouldRestrictWebViewHeight: Boolean,
    modifier: Modifier = Modifier
) {
    val uiModel = remember { buildFakeMessageBodyUiModel(value, shouldRestrictWebViewHeight) }
    MessageBodyWebView(
        modifier = modifier,
        messageBodyUiModel = uiModel,
        bodyDisplayMode = MessageBodyExpandCollapseMode.NotApplicable,
        shouldAllowViewingEntireMessage = false,
        webViewActions = MessageBodyWebView.Actions(
            onMessageBodyLinkClicked = {},
            onMessageBodyLinkLongClicked = {},
            onExpandCollapseButtonCLicked = {},
            loadEmbeddedImage = { _, _ -> null },
            onPrint = {},
            onViewEntireMessageClicked = { _, _, _, _ -> },
            onReply = {},
            onReplyAll = {},
            onForward = {}
        )
    )
}

private fun buildFakeMessageBodyUiModel(body: String, shouldRestrictWebViewHeight: Boolean) = MessageBodyUiModel(
    MessageId("fake-message-id-for-quoted-message-body"),
    body,
    messageBodyWithoutQuote = body,
    MimeTypeUiModel.Html,
    shouldShowEmbeddedImages = false,
    shouldShowRemoteContent = false,
    shouldShowEmbeddedImagesBanner = false,
    shouldShowRemoteContentBanner = false,
    shouldShowExpandCollapseButton = false,
    shouldShowOpenInProtonCalendar = false,
    attachments = null,
    userAddress = null,
    viewModePreference = ViewModePreference.ThemeDefault,
    printEffect = Effect.empty(),
    shouldRestrictWebViewHeight = shouldRestrictWebViewHeight,
    replyEffect = Effect.empty(),
    replyAllEffect = Effect.empty(),
    forwardEffect = Effect.empty()
)
