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

import ch.protonmail.android.mailcomposer.domain.usecase.StoreDraftWithAttachmentError
import ch.protonmail.android.mailcomposer.presentation.reducer.modifications.ComposerStateModifications
import ch.protonmail.android.mailcomposer.presentation.reducer.modifications.effects.BottomSheetEffectsStateModification
import ch.protonmail.android.mailcomposer.presentation.reducer.modifications.effects.CompletionEffectsStateModification
import ch.protonmail.android.mailcomposer.presentation.reducer.modifications.effects.ConfirmationsEffectsStateModification
import ch.protonmail.android.mailcomposer.presentation.reducer.modifications.effects.ContentEffectsStateModifications
import ch.protonmail.android.mailcomposer.presentation.reducer.modifications.effects.LoadingError
import ch.protonmail.android.mailcomposer.presentation.reducer.modifications.effects.RecoverableError
import ch.protonmail.android.mailcomposer.presentation.reducer.modifications.effects.UnrecoverableError
import ch.protonmail.android.mailmessage.domain.model.Recipient

internal sealed interface EffectsEvent : ComposerStateEvent {

    override fun toStateModifications(): ComposerStateModifications

    sealed interface DraftEvent : EffectsEvent {

        override fun toStateModifications(): ComposerStateModifications = ComposerStateModifications(
            effectsModification = when (this) {
                is OnDraftLoadingFailed -> LoadingError.DraftContent
            }
        )

        data object OnDraftLoadingFailed : DraftEvent
    }

    sealed interface LoadingEvent : EffectsEvent {

        override fun toStateModifications(): ComposerStateModifications = ComposerStateModifications(
            effectsModification = when (this) {
                OnParentLoadingFailed -> UnrecoverableError.ParentMessageMetadata
                OnSenderAddressLoadingFailed -> UnrecoverableError.InvalidSenderAddress
            }
        )

        data object OnParentLoadingFailed : LoadingEvent
        data object OnSenderAddressLoadingFailed : LoadingEvent
    }

    sealed interface AttachmentEvent : EffectsEvent {

        override fun toStateModifications(): ComposerStateModifications = ComposerStateModifications(
            effectsModification = when (this) {
                is Error -> RecoverableError.AttachmentsStore(error)
                is ReEncryptError -> RecoverableError.ReEncryptAttachment
                is OnAddRequest -> ContentEffectsStateModifications.OnAddAttachmentRequested
            }
        )

        data class Error(val error: StoreDraftWithAttachmentError) : AttachmentEvent
        data object ReEncryptError : AttachmentEvent
        data object OnAddRequest : AttachmentEvent
    }

    sealed interface ComposerControlEvent : EffectsEvent {

        override fun toStateModifications(): ComposerStateModifications = ComposerStateModifications(
            effectsModification = when (this) {
                is OnCloseRequest -> CompletionEffectsStateModification.CloseComposer(hasDraftSaved)
                is OnComposerRestored -> CompletionEffectsStateModification.CloseComposer(false)
            }
        )

        data class OnCloseRequest(val hasDraftSaved: Boolean) : ComposerControlEvent
        data object OnComposerRestored : ComposerControlEvent
    }

    sealed interface ErrorEvent : EffectsEvent {

        override fun toStateModifications(): ComposerStateModifications = ComposerStateModifications(
            effectsModification = when (this) {
                OnSenderChangeFreeUserError -> RecoverableError.SenderChange.FreeUser
                OnSenderChangePermissionsError -> RecoverableError.SenderChange.UnknownPermissions
                OnSetExpirationError -> RecoverableError.Expiration
            }
        )

        data object OnSenderChangeFreeUserError : ErrorEvent
        data object OnSenderChangePermissionsError : ErrorEvent
        data object OnSetExpirationError : ErrorEvent
    }

    sealed interface SendEvent : EffectsEvent {

        override fun toStateModifications(): ComposerStateModifications = ComposerStateModifications(
            effectsModification = when (this) {
                is OnCancelSendNoSubject -> ConfirmationsEffectsStateModification.CancelSendNoSubject
                is OnSendExpiringToExternalRecipients ->
                    ConfirmationsEffectsStateModification.ShowExternalExpiringRecipients(externalRecipients)

                is OnSendMessage -> CompletionEffectsStateModification.SendMessage.SendAndExit
                is OnOfflineSendMessage -> CompletionEffectsStateModification.SendMessage.SendAndExitOffline
                is OnSendingError -> RecoverableError.SendingFailed(message)
            }
        )

        data object OnSendMessage : SendEvent
        data object OnOfflineSendMessage : SendEvent

        data object OnCancelSendNoSubject : SendEvent
        data class OnSendExpiringToExternalRecipients(val externalRecipients: List<Recipient>) : SendEvent
        data class OnSendingError(val message: String) : SendEvent
    }

    data object SetExpirationReady : EffectsEvent {

        override fun toStateModifications(): ComposerStateModifications = ComposerStateModifications(
            effectsModification = BottomSheetEffectsStateModification.ShowBottomSheet
        )
    }
}
