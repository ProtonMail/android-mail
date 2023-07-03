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

import ch.protonmail.android.mailcomposer.domain.model.DraftBody

sealed interface ComposerOperation

internal sealed interface ComposerAction : ComposerOperation {
    data class SenderChanged(val sender: SenderUiModel) : ComposerAction
    data class RecipientsToChanged(val recipients: List<RecipientUiModel>) : ComposerAction
    data class RecipientsCcChanged(val recipients: List<RecipientUiModel>) : ComposerAction
    data class RecipientsBccChanged(val recipients: List<RecipientUiModel>) : ComposerAction
    data class SubjectChanged(val subject: String) : ComposerAction
    data class DraftBodyChanged(val draftBody: DraftBody) : ComposerAction

    object OnChangeSender : ComposerAction
}

sealed interface ComposerEvent : ComposerOperation {
    data class DefaultSenderReceived(val sender: SenderUiModel) : ComposerEvent
    data class SenderAddressesReceived(val senders: List<SenderUiModel>) : ComposerEvent

    object GetDefaultSenderError : ComposerEvent
    object ChangeSenderFailed : ComposerEvent
    object UpgradeToChangeSender : ComposerEvent
    object ErrorGettingSubscriptionToChangeSender : ComposerEvent
    object ErrorSavingDraftBodyUnresolvedSender : ComposerEvent
    object ErrorSavingDraftSender : ComposerEvent

    object ErrorSavingDraftBodyDbFailure : ComposerEvent
    object ErrorSavingDraftBodyEncryptionFailure : ComposerEvent
}
