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

package ch.protonmail.android.mailcomposer.presentation.reducer.modifications.effects

import androidx.annotation.StringRes
import ch.protonmail.android.mailattachments.domain.model.AddAttachmentError
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcomposer.domain.model.AttachmentAddErrorWithList
import ch.protonmail.android.mailcomposer.domain.model.AttachmentDeleteError
import ch.protonmail.android.mailcomposer.domain.model.SaveDraftError
import ch.protonmail.android.mailcomposer.domain.model.SendDraftError
import ch.protonmail.android.mailcomposer.presentation.R
import ch.protonmail.android.mailcomposer.presentation.mapper.SaveDraftErrorMapper
import ch.protonmail.android.mailcomposer.presentation.mapper.SendDraftErrorMapper
import ch.protonmail.android.mailcomposer.presentation.model.ComposerState

internal sealed class UnrecoverableError(@StringRes val resId: Int) : EffectsStateModification {

    override fun apply(state: ComposerState.Effects): ComposerState.Effects =
        state.copy(exitError = Effect.of(TextUiModel(resId)))

    data object NewDraftContentUnavailable : UnrecoverableError(R.string.composer_error_create_draft)
    data object ExistingDraftContentUnavailable : UnrecoverableError(R.string.composer_error_loading_draft)
    data object ParentMessageMetadata : UnrecoverableError(R.string.composer_error_loading_parent_message)
}

internal sealed interface RecoverableError : EffectsStateModification {

    sealed class SenderChange(@StringRes val resId: Int) : RecoverableError {

        override fun apply(state: ComposerState.Effects): ComposerState.Effects = when (this) {
            FreeUser -> state.copy(premiumFeatureMessage = Effect.of(TextUiModel(resId)))
            AddressCanNotSend,
            ChangeSenderFailure,
            RefreshBodyFailure,
            GetAddressesError -> state.copy(
                error = Effect.of(TextUiModel(resId)),
                changeBottomSheetVisibility = Effect.of(false)
            )
        }

        data object FreeUser : SenderChange(R.string.composer_change_sender_paid_feature)
        data object GetAddressesError :
            SenderChange(R.string.composer_error_change_sender_failed_getting_addresses)

        data object AddressCanNotSend : SenderChange(R.string.composer_change_sender_invalid_address)
        data object ChangeSenderFailure : SenderChange(R.string.composer_change_sender_unexpected_failure)
        data object RefreshBodyFailure : SenderChange(R.string.composer_change_sender_error_refreshing_body)
    }

    data class AttachmentsListChangedWithError(
        val attachmentAddErrorWithList: AttachmentAddErrorWithList
    ) : RecoverableError {

        override fun apply(state: ComposerState.Effects): ComposerState.Effects =
            when (attachmentAddErrorWithList.error) {
                AddAttachmentError.AttachmentTooLarge,
                AddAttachmentError.TooManyAttachments -> state.copy(
                    attachmentsFileSizeExceeded = Effect.of(
                        attachmentAddErrorWithList.failedAttachments.map {
                            it.attachmentMetadata.attachmentId
                        }
                    )
                )
                AddAttachmentError.UploadTimeout,
                AddAttachmentError.InvalidState -> state.copy(
                    warning = Effect.of(TextUiModel(R.string.composer_attachment_upload_error_retry))
                )

                AddAttachmentError.EncryptionError,
                is AddAttachmentError.Other,
                AddAttachmentError.InvalidDraftMessage ->
                    state.copy(error = Effect.of(TextUiModel(R.string.composer_unexpected_attachments_error)))

                AddAttachmentError.StorageQuotaExceeded ->
                    state.copy(error = Effect.of(TextUiModel(R.string.composer_storage_quota_exceeded_error)))
            }
    }

    data class AttachmentsStore(val error: AddAttachmentError) : RecoverableError {

        override fun apply(state: ComposerState.Effects): ComposerState.Effects = when (error) {
            AddAttachmentError.AttachmentTooLarge -> state.copy(attachmentsFileSizeExceeded = Effect.of(emptyList()))
            AddAttachmentError.EncryptionError -> state.copy(attachmentsEncryptionFailed = Effect.of(Unit))
            AddAttachmentError.TooManyAttachments ->
                state.copy(error = Effect.of(TextUiModel(R.string.composer_too_many_attachments_error)))

            AddAttachmentError.UploadTimeout,
            AddAttachmentError.InvalidState ->
                state.copy(error = Effect.of(TextUiModel(R.string.composer_attachment_upload_error_retry)))

            is AddAttachmentError.Other,
            AddAttachmentError.InvalidDraftMessage ->
                state.copy(error = Effect.of(TextUiModel(R.string.composer_unexpected_attachments_error)))

            AddAttachmentError.StorageQuotaExceeded ->
                state.copy(error = Effect.of(TextUiModel(R.string.composer_storage_quota_exceeded_error)))
        }
    }

    data class AttachmentRemove(val error: AttachmentDeleteError) : RecoverableError {

        override fun apply(state: ComposerState.Effects): ComposerState.Effects = when (error) {
            AttachmentDeleteError.FailedToDeleteFile,
            AttachmentDeleteError.MessageAlreadySent,
            AttachmentDeleteError.MessageDoesNotExist,
            is AttachmentDeleteError.Other,
            AttachmentDeleteError.RetriableError,
            AttachmentDeleteError.Unknown ->
                state.copy(error = Effect.of(TextUiModel(R.string.composer_delete_attachment_error)))

        }
    }

    data object Expiration : RecoverableError {

        override fun apply(state: ComposerState.Effects): ComposerState.Effects = state.copy(
            error = Effect.of(TextUiModel(R.string.composer_error_setting_expiration_time)),
            changeBottomSheetVisibility = Effect.of(false)
        )
    }

    data class SendingFailed(val reason: String) : RecoverableError {

        override fun apply(state: ComposerState.Effects): ComposerState.Effects =
            state.copy(sendingErrorEffect = Effect.of(TextUiModel.Text(reason)))
    }

    data object DiscardDraftFailed : RecoverableError {

        override fun apply(state: ComposerState.Effects): ComposerState.Effects =
            state.copy(error = Effect.of(TextUiModel.TextRes(R.string.discard_draft_error)))
    }

    data class SaveBodyFailed(val saveDraftError: SaveDraftError) : RecoverableError {

        override fun apply(state: ComposerState.Effects): ComposerState.Effects =
            state.copy(error = Effect.of(SaveDraftErrorMapper.toTextUiModel(saveDraftError)))
    }

    data class SaveRecipientFailed(val saveDraftError: SaveDraftError) : RecoverableError {

        override fun apply(state: ComposerState.Effects): ComposerState.Effects =
            state.copy(error = Effect.of(SaveDraftErrorMapper.toTextUiModel(saveDraftError)))
    }

    data class SaveSubjectFailed(val saveDraftError: SaveDraftError) : RecoverableError {

        override fun apply(state: ComposerState.Effects): ComposerState.Effects =
            state.copy(error = Effect.of(SaveDraftErrorMapper.toTextUiModel(saveDraftError)))
    }

    data class FinalSaveError(val saveDraftError: SaveDraftError) : RecoverableError {

        override fun apply(state: ComposerState.Effects): ComposerState.Effects =
            state.copy(error = Effect.of(SaveDraftErrorMapper.toTextUiModel(saveDraftError)))
    }

    data class SendMessageFailed(val error: SendDraftError) : RecoverableError {

        override fun apply(state: ComposerState.Effects): ComposerState.Effects =
            state.copy(error = Effect.of(SendDraftErrorMapper.toTextUiModel(error)))
    }

    data object GetScheduleSendOptionsFailed : RecoverableError {

        override fun apply(state: ComposerState.Effects): ComposerState.Effects =
            state.copy(error = Effect.of(TextUiModel(R.string.composer_error_retrieving_schedule_send_options)))
    }

    data object LoadingAttachmentsFailed : RecoverableError {

        override fun apply(state: ComposerState.Effects): ComposerState.Effects =
            state.copy(error = Effect.of(TextUiModel(R.string.composer_loading_attachments_error)))
    }

}
