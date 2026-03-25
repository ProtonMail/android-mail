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

package ch.protonmail.android.mailcomposer.presentation.model.operations

import ch.protonmail.android.mailattachments.domain.model.ConvertAttachmentError
import ch.protonmail.android.mailcomposer.domain.model.AttachmentDeleteError
import ch.protonmail.android.mailcomposer.domain.model.DraftSenderValidationError
import ch.protonmail.android.mailcomposer.domain.model.SaveDraftError
import ch.protonmail.android.mailcomposer.domain.model.SendDraftError
import ch.protonmail.android.mailcomposer.presentation.reducer.modifications.ComposerStateModifications
import ch.protonmail.android.mailcomposer.presentation.reducer.modifications.effects.BottomSheetEffectsStateModification
import ch.protonmail.android.mailcomposer.presentation.reducer.modifications.effects.CompletionEffectsStateModification
import ch.protonmail.android.mailcomposer.presentation.reducer.modifications.effects.ConfirmationsEffectsStateModification
import ch.protonmail.android.mailcomposer.presentation.reducer.modifications.effects.ContentEffectsStateModifications
import ch.protonmail.android.mailcomposer.presentation.reducer.modifications.effects.ContentEffectsStateModifications.OnAddAttachmentCameraRequested
import ch.protonmail.android.mailcomposer.presentation.reducer.modifications.effects.ContentEffectsStateModifications.OnAddAttachmentFileRequested
import ch.protonmail.android.mailcomposer.presentation.reducer.modifications.effects.ContentEffectsStateModifications.OnAddAttachmentPhotosRequested
import ch.protonmail.android.mailcomposer.presentation.reducer.modifications.effects.ContentEffectsStateModifications.OnInlineAttachmentRemoved
import ch.protonmail.android.mailcomposer.presentation.reducer.modifications.effects.ContentEffectsStateModifications.OnInlineAttachmentsAdded
import ch.protonmail.android.mailcomposer.presentation.reducer.modifications.effects.RecoverableError
import ch.protonmail.android.mailcomposer.presentation.reducer.modifications.effects.RecoverableError.AttachmentConversion
import ch.protonmail.android.mailcomposer.presentation.reducer.modifications.effects.RecoverableError.AttachmentRemove
import ch.protonmail.android.mailcomposer.presentation.reducer.modifications.effects.RecoverableError.AttachmentsStore
import ch.protonmail.android.mailcomposer.presentation.reducer.modifications.effects.RecoverableError.LoadingAttachmentsFailed
import ch.protonmail.android.mailcomposer.presentation.reducer.modifications.effects.UnrecoverableError
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailattachments.domain.model.AddAttachmentError as DomainAddAttachmentError

internal sealed interface EffectsEvent : ComposerStateEvent {

    override fun toStateModifications(): ComposerStateModifications

    sealed interface DraftEvent : EffectsEvent {

        override fun toStateModifications(): ComposerStateModifications = ComposerStateModifications(
            effectsModification = when (this) {
                is OnDraftCreationFailed -> UnrecoverableError.NewDraftContentUnavailable
                is OnDraftLoadingFailed -> UnrecoverableError.ExistingDraftContentUnavailable
                is OnDiscardDraftRequested -> ConfirmationsEffectsStateModification.DiscardDraftConfirmationRequested
                is OnSenderValidationError -> ContentEffectsStateModifications.OnDraftSenderValidationError(this.error)
            }
        )

        data object OnDraftCreationFailed : DraftEvent
        data object OnDraftLoadingFailed : DraftEvent
        data object OnDiscardDraftRequested : DraftEvent
        data class OnSenderValidationError(val error: DraftSenderValidationError) : DraftEvent
    }

    sealed interface AttachmentEvent : EffectsEvent {

        override fun toStateModifications(): ComposerStateModifications = ComposerStateModifications(
            effectsModification = when (this) {
                is AddAttachmentError -> AttachmentsStore(error)
                is OnAddFileRequest -> OnAddAttachmentFileRequested
                is OnAddFromCameraRequest -> OnAddAttachmentCameraRequested
                is OnAddMediaRequest -> OnAddAttachmentPhotosRequested
                is InlineAttachmentsAdded -> OnInlineAttachmentsAdded(contentIds)
                is StripInlineAttachmentFromBody -> OnInlineAttachmentRemoved(contentId)
                is OnAttachFromOptionsRequest -> BottomSheetEffectsStateModification.ShowBottomSheet
                is OnInlineImageActionsRequested -> BottomSheetEffectsStateModification.ShowBottomSheet
                is RemoveAttachmentError -> AttachmentRemove(error)
                is OnLoadAttachmentsFailed -> LoadingAttachmentsFailed
                is InlineAttachmentConversionFailed -> AttachmentConversion(error)
            }
        )

        data class RemoveAttachmentError(val error: AttachmentDeleteError) : AttachmentEvent
        data class AddAttachmentError(val error: DomainAddAttachmentError) : AttachmentEvent
        data class InlineAttachmentsAdded(val contentIds: List<String>) : AttachmentEvent
        data class StripInlineAttachmentFromBody(val contentId: String) : AttachmentEvent
        data class InlineAttachmentConversionFailed(val error: ConvertAttachmentError) : AttachmentEvent

        data object OnInlineImageActionsRequested : AttachmentEvent
        data object OnAttachFromOptionsRequest : AttachmentEvent
        data object OnAddFileRequest : AttachmentEvent
        data object OnAddMediaRequest : AttachmentEvent
        data object OnAddFromCameraRequest : AttachmentEvent
        data object OnLoadAttachmentsFailed : AttachmentEvent
    }

    sealed interface ComposerControlEvent : EffectsEvent {

        override fun toStateModifications(): ComposerStateModifications = ComposerStateModifications(
            effectsModification = when (this) {
                is OnCloseRequest -> CompletionEffectsStateModification.CloseComposer.CloseComposerNoDraft
                is OnComposerRestored -> CompletionEffectsStateModification.CloseComposer.CloseComposerNoDraft
                is OnCloseRequestWithDraft ->
                    CompletionEffectsStateModification.CloseComposer.CloseComposerDraftSaved(this.draftId)

                is OnCloseRequestWithDraftDiscarded ->
                    CompletionEffectsStateModification.CloseComposer.CloseComposerDraftDiscarded
            }
        )

        data object OnCloseRequest : ComposerControlEvent
        data class OnCloseRequestWithDraft(val draftId: MessageId) : ComposerControlEvent
        data object OnCloseRequestWithDraftDiscarded : ComposerControlEvent
        data object OnComposerRestored : ComposerControlEvent
    }

    sealed interface ErrorEvent : EffectsEvent {

        override fun toStateModifications(): ComposerStateModifications = ComposerStateModifications(
            effectsModification = when (this) {
                OnSenderChangeFreeUserError -> RecoverableError.SenderChange.FreeUser
                OnGetAddressesError -> RecoverableError.SenderChange.GetAddressesError
                OnSetExpirationError -> RecoverableError.Expiration
                OnDiscardDraftError -> RecoverableError.DiscardDraftFailed
                is OnStoreBodyError -> RecoverableError.SaveBodyFailed(draftError)
                is OnStoreRecipientError -> RecoverableError.SaveRecipientFailed(draftError)
                is OnStoreSubjectError -> RecoverableError.SaveSubjectFailed(draftError)
                is OnFinalSaveError -> RecoverableError.FinalSaveError(draftError)
                is OnSendMessageError -> RecoverableError.SendMessageFailed(sendError)
                OnGetScheduleSendOptionsError -> RecoverableError.GetScheduleSendOptionsFailed
                OnAddressNotValidForSending -> RecoverableError.SenderChange.AddressCanNotSend
                OnChangeSenderFailure -> RecoverableError.SenderChange.ChangeSenderFailure
                OnRefreshBodyFailed -> RecoverableError.SenderChange.RefreshBodyFailure
            }
        )

        data object OnSenderChangeFreeUserError : ErrorEvent
        data object OnGetAddressesError : ErrorEvent
        data object OnSetExpirationError : ErrorEvent
        data object OnDiscardDraftError : ErrorEvent
        data class OnStoreBodyError(val draftError: SaveDraftError) : ErrorEvent
        data class OnStoreSubjectError(val draftError: SaveDraftError) : ErrorEvent
        data class OnStoreRecipientError(val draftError: SaveDraftError) : ErrorEvent
        data class OnFinalSaveError(val draftError: SaveDraftError) : ErrorEvent
        data class OnSendMessageError(val sendError: SendDraftError) : ErrorEvent
        data object OnGetScheduleSendOptionsError : ErrorEvent
        data object OnAddressNotValidForSending : ErrorEvent
        data object OnRefreshBodyFailed : ErrorEvent
        data object OnChangeSenderFailure : ErrorEvent
    }

    sealed interface SendEvent : EffectsEvent {

        override fun toStateModifications(): ComposerStateModifications = ComposerStateModifications(
            effectsModification = when (this) {
                is OnCancelSendNoSubject -> ConfirmationsEffectsStateModification.CancelSendNoSubject

                is OnSendMessage -> CompletionEffectsStateModification.SendMessage.SendAndExit
                is OnOfflineSendMessage -> CompletionEffectsStateModification.SendMessage.SendAndExitOffline
                is OnSendingError -> RecoverableError.SendingFailed(message)
                is OnScheduleSendMessage -> CompletionEffectsStateModification.ScheduleMessage.ScheduleAndExit
                is OnOfflineScheduleSendMessage ->
                    CompletionEffectsStateModification.ScheduleMessage.ScheduleAndExitOffline
            }
        )

        data object OnSendMessage : SendEvent
        data object OnOfflineSendMessage : SendEvent

        data object OnScheduleSendMessage : SendEvent
        data object OnOfflineScheduleSendMessage : SendEvent

        data object OnCancelSendNoSubject : SendEvent
        data class OnSendingError(val message: String) : SendEvent
    }

    data object SetExpirationReady : EffectsEvent {

        override fun toStateModifications(): ComposerStateModifications = ComposerStateModifications(
            effectsModification = ContentEffectsStateModifications.OnPickMessageExpirationTimeRequested
        )
    }
}
