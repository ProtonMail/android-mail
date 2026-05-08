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

package ch.protonmail.android.maildetail.presentation.sample

import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.presentation.model.MessageBodyContent
import ch.protonmail.android.mailmessage.presentation.model.MessageBodyUiModel
import ch.protonmail.android.mailmessage.presentation.model.MimeTypeUiModel
import ch.protonmail.android.mailmessage.presentation.model.ViewModePreference
import ch.protonmail.android.mailmessage.presentation.model.attachment.AttachmentGroupUiModel

object MessageDetailBodyUiModelSample {

    val withBlockedRemoteContent = build(
        "A message body with blocked remote content",
        shouldShowRemoteContentBanner = true
    )

    val withAllowedRemoteContent = build(
        "A message body with blocked remote content",
        shouldShowRemoteContentBanner = false
    )

    val withBlockedEmbeddedImages = build(
        "A message body with embedded images",
        shouldShowEmbeddedImagesBanner = true
    )

    val withAllowedEmbeddedImages = build(
        "A message body with embedded images",
        shouldShowEmbeddedImagesBanner = false
    )

    val withBlockedContent = build(
        "A message body with both remote and embedded images",
        shouldShowRemoteContentBanner = true,
        shouldShowEmbeddedImagesBanner = true
    )

    val withAllowedContent = build(
        "A message body with both remote and embedded images",
        shouldShowRemoteContentBanner = false,
        shouldShowEmbeddedImagesBanner = false
    )

    fun build(
        messageBody: String,
        messageId: MessageId = MessageId("sample message id"),
        mimeType: MimeTypeUiModel = MimeTypeUiModel.Html,
        shouldShowEmbeddedImagesBanner: Boolean = false,
        shouldShowRemoteContentBanner: Boolean = false,
        attachments: AttachmentGroupUiModel? = null
    ) = MessageBodyUiModel(
        messageId = messageId,
        messageBody = MessageBodyContent.Text(messageBody),
        mimeType = mimeType,
        shouldShowEmbeddedImagesBanner = shouldShowEmbeddedImagesBanner,
        shouldShowRemoteContentBanner = shouldShowRemoteContentBanner,
        shouldShowImagesFailedToLoadBanner = false,
        shouldShowExpandCollapseButton = false,
        attachments = attachments,
        viewModePreference = ViewModePreference.ThemeDefault,
        reloadMessageEffect = Effect.empty(),
        shouldRestrictWebViewHeight = false
    )
}
