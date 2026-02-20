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

package ch.protonmail.android.mailcomposer.presentation.model

import ch.protonmail.android.mailattachments.domain.model.AttachmentId
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcomposer.domain.model.DraftMimeType
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.presentation.model.attachment.AttachmentGroupUiModel
import ch.protonmail.android.mailmessage.presentation.model.attachment.NO_ATTACHMENT_LIMIT
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

data class ComposerStates(
    val main: ComposerState.Main,
    val attachments: ComposerState.Attachments,
    val accessories: ComposerState.Accessories,
    val effects: ComposerState.Effects
)

sealed interface ComposerState {

    enum class LoadingType {
        Initial,
        Save,
        None
    }

    data class Main(
        val sender: SenderUiModel,
        val draftType: DraftMimeType,
        val senderAddresses: ImmutableList<SenderUiModel>,
        val isSubmittable: Boolean,
        val loadingType: LoadingType,
        val initialFocusedField: FocusedFieldType
    ) {

        companion object {

            fun initial() = Main(
                sender = SenderUiModel(""),
                draftType = DraftMimeType.Html,
                senderAddresses = emptyList<SenderUiModel>().toImmutableList(),
                isSubmittable = false,
                loadingType = LoadingType.None,
                initialFocusedField = FocusedFieldType.TO
            )
        }
    }

    data class Attachments(
        val uiModel: AttachmentGroupUiModel
    ) {

        companion object {

            fun initial() = Attachments(
                uiModel = AttachmentGroupUiModel(
                    attachments = emptyList(),
                    limit = NO_ATTACHMENT_LIMIT
                )
            )
        }
    }

    data class Accessories(
        val isMessagePasswordSet: Boolean,
        val expirationTime: ExpirationTimeUiModel,
        val scheduleSendOptions: ScheduleSendOptionsUiModel
    ) {

        companion object {

            fun initial() = Accessories(
                isMessagePasswordSet = false,
                expirationTime = ExpirationTimeUiModel(ExpirationTimeOption.None),
                scheduleSendOptions = ScheduleSendOptionsUiModel.EmptyOptions
            )
        }
    }

    data class Effects(
        val error: Effect<TextUiModel>,
        val exitError: Effect<TextUiModel>,
        val premiumFeatureMessage: Effect<TextUiModel>,
        val recipientValidationError: Effect<TextUiModel>,
        val changeBottomSheetVisibility: Effect<Boolean>,
        val closeComposer: Effect<Unit>,
        val closeComposerWithDraftSaved: Effect<MessageId>,
        val closeComposerWithDraftDiscarded: Effect<Unit>,
        val closeComposerWithMessageSending: Effect<Unit>,
        val closeComposerWithMessageSendingOffline: Effect<Unit>,
        val closeComposerWithScheduleSending: Effect<Unit>,
        val closeComposerWithScheduleSendingOffline: Effect<Unit>,
        val confirmSendingWithoutSubject: Effect<Unit>,
        val changeFocusToField: Effect<FocusedFieldType>,
        val attachmentsFileSizeExceeded: Effect<List<AttachmentId>>,
        val attachmentsEncryptionFailed: Effect<Unit>,
        val warning: Effect<TextUiModel>,
        val focusTextBody: Effect<Unit> = Effect.empty(),
        val sendingErrorEffect: Effect<TextUiModel> = Effect.empty(),
        val senderChangedNotice: Effect<TextUiModel> = Effect.empty(),
        val confirmSendExpiringMessage: Effect<TextUiModel>,
        val openFilesPicker: Effect<Unit>,
        val openPhotosPicker: Effect<Unit>,
        val openCamera: Effect<Unit>,
        val confirmDiscardDraft: Effect<Unit>,
        val injectInlineAttachments: Effect<List<String>>,
        val stripInlineAttachment: Effect<String>,
        val refreshBody: Effect<DraftDisplayBodyUiModel>,
        val pickMessageExpiration: Effect<Unit>,
        val duplicateRemovalWarning: Effect<TextUiModel> = Effect.empty()
    ) {

        companion object {

            fun initial() = Effects(
                error = Effect.empty(),
                exitError = Effect.empty(),
                premiumFeatureMessage = Effect.empty(),
                recipientValidationError = Effect.empty(),
                changeBottomSheetVisibility = Effect.empty(),
                closeComposer = Effect.empty(),
                closeComposerWithDraftSaved = Effect.empty(),
                closeComposerWithDraftDiscarded = Effect.empty(),
                closeComposerWithMessageSending = Effect.empty(),
                closeComposerWithMessageSendingOffline = Effect.empty(),
                closeComposerWithScheduleSending = Effect.empty(),
                closeComposerWithScheduleSendingOffline = Effect.empty(),
                confirmSendingWithoutSubject = Effect.empty(),
                changeFocusToField = Effect.empty(),
                attachmentsFileSizeExceeded = Effect.empty(),
                attachmentsEncryptionFailed = Effect.empty(),
                warning = Effect.empty(),
                sendingErrorEffect = Effect.empty(),
                senderChangedNotice = Effect.empty(),
                confirmSendExpiringMessage = Effect.empty(),
                openFilesPicker = Effect.empty(),
                openPhotosPicker = Effect.empty(),
                openCamera = Effect.empty(),
                confirmDiscardDraft = Effect.empty(),
                injectInlineAttachments = Effect.empty(),
                stripInlineAttachment = Effect.empty(),
                refreshBody = Effect.empty(),
                pickMessageExpiration = Effect.empty()
            )
        }
    }
}
