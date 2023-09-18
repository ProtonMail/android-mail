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

import ch.protonmail.android.mailmessage.presentation.model.MessageBodyUiModel
import ch.protonmail.android.mailmessage.presentation.model.MimeTypeUiModel
import ch.protonmail.android.mailmessage.presentation.model.AttachmentGroupUiModel
import ch.protonmail.android.mailmessage.domain.model.MessageId

object MessageDetailBodyUiModelSample {

    fun build(
        messageBody: String,
        messageId: MessageId = MessageId("sample message id"),
        mimeType: MimeTypeUiModel = MimeTypeUiModel.Html,
        shouldShowEmbeddedImages: Boolean = false,
        shouldShowRemoteContent: Boolean = false,
        shouldShowEmbeddedImagesBanner: Boolean = false,
        shouldShowRemoteContentBanner: Boolean = false,
        attachments: AttachmentGroupUiModel? = null
    ) = MessageBodyUiModel(
        messageBody = messageBody,
        messageId = messageId,
        mimeType = mimeType,
        shouldShowEmbeddedImages = shouldShowEmbeddedImages,
        shouldShowRemoteContent = shouldShowRemoteContent,
        shouldShowEmbeddedImagesBanner = shouldShowEmbeddedImagesBanner,
        shouldShowRemoteContentBanner = shouldShowRemoteContentBanner,
        attachments = attachments
    )
}
