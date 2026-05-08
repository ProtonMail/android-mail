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

package ch.protonmail.android.maildetail.presentation.mapper

import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcrashrecord.domain.usecase.HasMessageBodyWebViewCrashed
import ch.protonmail.android.mailfeatureflags.domain.annotation.IsRestrictMessageWebViewHeightEnabled
import ch.protonmail.android.mailfeatureflags.domain.model.FeatureFlag
import ch.protonmail.android.mailmessage.domain.model.AttachmentListExpandCollapseMode
import ch.protonmail.android.mailmessage.domain.model.DecryptedMessageBody
import ch.protonmail.android.mailmessage.domain.model.GetMessageBodyError
import ch.protonmail.android.mailmessage.domain.model.MessageBanner
import ch.protonmail.android.mailmessage.domain.model.MimeType
import ch.protonmail.android.mailmessage.presentation.mapper.AttachmentGroupUiModelMapper
import ch.protonmail.android.mailmessage.presentation.model.MessageBodyContent
import ch.protonmail.android.mailmessage.presentation.model.MessageBodyUiModel
import ch.protonmail.android.mailmessage.presentation.model.MessageBodyWithType
import ch.protonmail.android.mailmessage.presentation.model.MimeTypeUiModel
import ch.protonmail.android.mailmessage.presentation.model.ViewModePreference
import javax.inject.Inject

class MessageBodyUiModelMapper @Inject constructor(
    private val attachmentGroupUiModelMapper: AttachmentGroupUiModelMapper,
    private val hasMessageBodyWebViewCrashed: HasMessageBodyWebViewCrashed,
    private val bodyContentUiModelMapper: MessageBodyContentUiModelMapper,
    @IsRestrictMessageWebViewHeightEnabled private val restrictMessageWebViewHeightEnabled: FeatureFlag<Boolean>
) {

    suspend fun toUiModel(
        decryptedMessageBody: DecryptedMessageBody,
        attachmentListExpandCollapseMode: AttachmentListExpandCollapseMode?,
        existingMessageBodyUiModel: MessageBodyUiModel? = null
    ): MessageBodyUiModel {
        val shouldRestrictWebViewHeight =
            hasMessageBodyWebViewCrashed() && restrictMessageWebViewHeightEnabled.get()

        val decryptedMessageBodyWithType = MessageBodyWithType(
            decryptedMessageBody.value,
            decryptedMessageBody.mimeType.toMimeTypeUiModel()
        )
        val messageBody = bodyContentUiModelMapper.toUiContent(
            decryptedMessageBodyWithType.messageBody,
            decryptedMessageBody.messageId,
            shouldRestrictWebViewHeight
        )

        val hasRemoteContentBlocked = decryptedMessageBody.banners.contains(MessageBanner.RemoteContent)
        val hasEmbeddedImagesBlocked = decryptedMessageBody.banners.contains(MessageBanner.EmbeddedImages)
        val hasExpandCollapseButton = existingMessageBodyUiModel?.shouldShowExpandCollapseButton == true ||
            decryptedMessageBody.hasQuotedText

        val viewModePreference =
            decryptedMessageBody.transformations.messageThemeOptions?.themeOverride.toViewModePreference()

        return MessageBodyUiModel(
            messageId = decryptedMessageBody.messageId,
            messageBody = messageBody,
            mimeType = decryptedMessageBody.mimeType.toMimeTypeUiModel(),
            shouldShowEmbeddedImagesBanner = hasEmbeddedImagesBlocked,
            shouldShowRemoteContentBanner = hasRemoteContentBlocked,
            shouldShowImagesFailedToLoadBanner = false,
            shouldShowExpandCollapseButton = hasExpandCollapseButton,
            attachments = if (decryptedMessageBody.attachments.isNotEmpty()) {
                attachmentGroupUiModelMapper.toUiModel(
                    decryptedMessageBody.attachments,
                    attachmentListExpandCollapseMode
                )
            } else null,
            viewModePreference = viewModePreference,
            reloadMessageEffect = Effect.empty(),
            shouldRestrictWebViewHeight = shouldRestrictWebViewHeight
        )
    }

    fun toUiModel(decryptionError: GetMessageBodyError.Decryption) = MessageBodyUiModel(
        messageId = decryptionError.messageId,
        messageBody = MessageBodyContent.Text(decryptionError.encryptedMessageBody),
        mimeType = MimeTypeUiModel.PlainText,
        shouldShowEmbeddedImagesBanner = false,
        shouldShowRemoteContentBanner = false,
        shouldShowImagesFailedToLoadBanner = false,
        shouldShowExpandCollapseButton = false,
        attachments = null,
        viewModePreference = ViewModePreference.ThemeDefault,
        reloadMessageEffect = Effect.empty(),
        shouldRestrictWebViewHeight = false
    )

    private fun MimeType.toMimeTypeUiModel() = when (this) {
        MimeType.PlainText -> MimeTypeUiModel.PlainText
        MimeType.Html, MimeType.MultipartMixed -> MimeTypeUiModel.Html
    }
}
