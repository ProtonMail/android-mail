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
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.presentation.model.MessageBodyUiModel
import ch.protonmail.android.mailmessage.presentation.model.MimeTypeUiModel
import ch.protonmail.android.mailmessage.presentation.ui.MessageBodyWebView

@Composable
internal fun BodyHtmlQuote(value: String, modifier: Modifier = Modifier) {
    val uiModel = remember { buildFakeMessageBodyUiModel(value) }
    MessageBodyWebView(
        modifier = modifier,
        messageBodyUiModel = uiModel,
        actions = MessageBodyWebView.Actions(
            onMessageBodyLinkClicked = {},
            onShowAllAttachments = {},
            onAttachmentClicked = {},
            loadEmbeddedImage = { _, _ -> null }
        )
    )
}

private fun buildFakeMessageBodyUiModel(body: String) = MessageBodyUiModel(
    MessageId("fake-message-id-for-quoted-message-body"),
    body,
    MimeTypeUiModel.Html,
    shouldShowEmbeddedImages = false,
    shouldShowRemoteContent = false,
    shouldShowEmbeddedImagesBanner = false,
    shouldShowRemoteContentBanner = false,
    attachments = null
)
