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
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcomposer.domain.usecase.StoreDraftWithAttachmentError
import ch.protonmail.android.mailcomposer.presentation.R
import ch.protonmail.android.mailcomposer.presentation.model.ComposerState

internal sealed class LoadingError(@StringRes val resId: Int) : EffectsStateModification {

    override fun apply(state: ComposerState.Effects): ComposerState.Effects =
        state.copy(error = Effect.of(TextUiModel(resId)))

    data object DraftContent : LoadingError(R.string.composer_error_loading_draft)
}

internal sealed class UnrecoverableError(@StringRes val resId: Int) : EffectsStateModification {

    override fun apply(state: ComposerState.Effects): ComposerState.Effects =
        state.copy(exitError = Effect.of(TextUiModel(resId)))

    data object InvalidSenderAddress : UnrecoverableError(R.string.composer_error_invalid_sender)
    data object ParentMessageMetadata : UnrecoverableError(R.string.composer_error_loading_parent_message)
}

internal sealed interface RecoverableError : EffectsStateModification {

    sealed class SenderChange(@StringRes val resId: Int) : RecoverableError {

        override fun apply(state: ComposerState.Effects): ComposerState.Effects = when (this) {
            FreeUser -> state.copy(premiumFeatureMessage = Effect.of(TextUiModel(resId)))
            UnknownPermissions -> state.copy(error = Effect.of(TextUiModel(resId)))
        }

        data object FreeUser : SenderChange(R.string.composer_change_sender_paid_feature)
        data object UnknownPermissions :
            SenderChange(R.string.composer_error_change_sender_failed_getting_subscription)
    }

    data class AttachmentsStore(val error: StoreDraftWithAttachmentError) : RecoverableError {

        override fun apply(state: ComposerState.Effects): ComposerState.Effects = when (error) {
            StoreDraftWithAttachmentError.AttachmentsMissing,
            StoreDraftWithAttachmentError.AttachmentFileMissing ->
                state.copy(error = Effect.of(TextUiModel.TextRes(R.string.composer_attachment_not_found)))

            StoreDraftWithAttachmentError.FailedReceivingDraft ->
                state.copy(error = Effect.of(TextUiModel.TextRes(R.string.composer_attachment_error_draft_loading)))

            StoreDraftWithAttachmentError.FailedToStoreAttachments ->
                state.copy(
                    error = Effect.of(TextUiModel.TextRes(R.string.composer_attachment_error_saving_attachment))
                )

            StoreDraftWithAttachmentError.FileSizeExceedsLimit ->
                state.copy(attachmentsFileSizeExceeded = Effect.of(Unit))
        }
    }

    data object ReEncryptAttachment : RecoverableError {

        override fun apply(state: ComposerState.Effects): ComposerState.Effects = state.copy(
            attachmentsReEncryptionFailed = Effect.of(
                TextUiModel(R.string.composer_attachment_reencryption_failed_message)
            )
        )
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
}
