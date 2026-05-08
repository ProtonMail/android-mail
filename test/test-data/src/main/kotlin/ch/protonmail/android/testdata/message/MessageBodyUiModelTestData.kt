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

package ch.protonmail.android.testdata.message

import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.mailmessage.presentation.model.MessageBodyContent
import ch.protonmail.android.mailmessage.presentation.model.MessageBodyUiModel
import ch.protonmail.android.mailmessage.presentation.model.MimeTypeUiModel
import ch.protonmail.android.mailmessage.presentation.model.ViewModePreference
import ch.protonmail.android.mailmessage.presentation.model.attachment.AttachmentGroupUiModel
import ch.protonmail.android.mailmessage.presentation.sample.AttachmentMetadataUiModelSamples

object MessageBodyUiModelTestData {

    val plainTextMessageBodyUiModel = buildMessageBodyUiModel()

    val messageBodyWithAttachmentsUiModel = buildMessageBodyUiModel(
        attachments = AttachmentGroupUiModel(
            limit = 3,
            attachments = listOf(
                AttachmentMetadataUiModelSamples.Invoice,
                AttachmentMetadataUiModelSamples.Document,
                AttachmentMetadataUiModelSamples.DocumentWithMultipleDots,
                AttachmentMetadataUiModelSamples.Image
            )
        )
    )

    val messageBodyWithIcsAttachmentsUiModel = buildMessageBodyUiModel(
        attachments = AttachmentGroupUiModel(
            limit = 3,
            attachments = listOf(
                AttachmentMetadataUiModelSamples.Calendar
            )
        )
    )

    val htmlMessageBodyUiModel = buildMessageBodyUiModel(
        messageBody = """
            <div>
                <p>Dear Test,</p>
                <p>This is an HTML message body.</p>
                <p>Kind regards,<br>
                Developer</p>
            </div>
        """.trimIndent(),
        mimeType = MimeTypeUiModel.Html
    )

    val messageBodyWithRemoteContentBlocked = buildMessageBodyUiModel(
        messageBody = """
            <div>
                <p>Dear Test,</p>
                <p>This is an HTML message body.</p>
                <img src="http://remote-insecure-image" />
                <p>Kind regards,<br>
                Developer</p>
            </div>
        """.trimIndent(),
        mimeType = MimeTypeUiModel.Html,
        shouldShowRemoteContentBanner = true
    )

    val messageBodyWithRemoteContentLoaded = messageBodyWithRemoteContentBlocked.copy(
        shouldShowRemoteContentBanner = false
    )

    val messageBodyWithEmbeddedImagesBlocked = buildMessageBodyUiModel(
        messageBody = "MessageWithEmbeddedImages",
        shouldShowEmbeddedImagesBanner = true
    )

    val messageBodyWithEmbeddedImagesLoaded = messageBodyWithEmbeddedImagesBlocked.copy(
        shouldShowEmbeddedImagesBanner = false
    )

    val bodyWithRemoteAndEmbeddedContentBlocked = buildMessageBodyUiModel(
        messageBody = "MessageWithRemoteAndEmbeddedImages",
        shouldShowRemoteContentBanner = true,
        shouldShowEmbeddedImagesBanner = true
    )

    val bodyWithRemoteAndEmbeddedContentLoaded = bodyWithRemoteAndEmbeddedContentBlocked.copy(
        shouldShowRemoteContentBanner = false,
        shouldShowEmbeddedImagesBanner = false
    )


    fun buildMessageBodyUiModel(
        messageId: MessageId = MessageIdSample.build(),
        messageBody: String = MessageBodyTestData.messageBody.body,
        mimeType: MimeTypeUiModel = MimeTypeUiModel.PlainText,
        shouldShowEmbeddedImagesBanner: Boolean = false,
        shouldShowRemoteContentBanner: Boolean = false,
        attachments: AttachmentGroupUiModel? = null,
        viewModePreference: ViewModePreference = ViewModePreference.ThemeDefault,
        shouldRestrictWebViewHeight: Boolean = false
    ): MessageBodyUiModel {
        return MessageBodyUiModel(
            messageId = messageId,
            messageBody = MessageBodyContent.Text(messageBody),
            mimeType = mimeType,
            shouldShowEmbeddedImagesBanner = shouldShowEmbeddedImagesBanner,
            shouldShowRemoteContentBanner = shouldShowRemoteContentBanner,
            shouldShowImagesFailedToLoadBanner = false,
            shouldShowExpandCollapseButton = false,
            attachments = attachments,
            viewModePreference = viewModePreference,
            reloadMessageEffect = Effect.empty(),
            shouldRestrictWebViewHeight = shouldRestrictWebViewHeight
        )
    }
}
