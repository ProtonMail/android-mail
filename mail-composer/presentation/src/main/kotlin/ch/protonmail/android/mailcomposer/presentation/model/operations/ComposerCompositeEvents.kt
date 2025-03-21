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

import ch.protonmail.android.mailcomposer.domain.model.QuotedHtmlContent
import ch.protonmail.android.mailcomposer.domain.model.SenderEmail
import ch.protonmail.android.mailcomposer.domain.usecase.ValidateSenderAddress.ValidationResult
import ch.protonmail.android.mailcomposer.presentation.model.ComposerState
import ch.protonmail.android.mailcomposer.presentation.model.SenderUiModel
import ch.protonmail.android.mailcomposer.presentation.reducer.modifications.AccessoriesStateModification
import ch.protonmail.android.mailcomposer.presentation.reducer.modifications.ComposerStateModifications
import ch.protonmail.android.mailcomposer.presentation.reducer.modifications.MainStateModification
import ch.protonmail.android.mailcomposer.presentation.reducer.modifications.effects.BottomSheetEffectsStateModification
import ch.protonmail.android.mailcomposer.presentation.reducer.modifications.effects.ConfirmationsEffectsStateModification
import ch.protonmail.android.mailcomposer.presentation.reducer.modifications.effects.ContentEffectsStateModifications
import kotlin.time.Duration

internal sealed interface CompositeEvent : ComposerStateEvent {

    override fun toStateModifications(): ComposerStateModifications = when (this) {
        is DraftContentReady -> ComposerStateModifications(
            mainModification = MainStateModification.OnDraftReady(
                senderEmail,
                quotedHtmlContent,
                shouldRestrictWebViewHeight
            ),
            effectsModification = ContentEffectsStateModifications.DraftContentReady(
                senderValidationResult,
                isDataRefreshed,
                forceBodyFocus
            )
        )

        is SenderAddressesListReady -> ComposerStateModifications(
            mainModification = MainStateModification.SendersListReady(sendersList),
            effectsModification = BottomSheetEffectsStateModification.ShowBottomSheet
        )

        is OnSendWithEmptySubject -> ComposerStateModifications(
            mainModification = MainStateModification.UpdateLoading(ComposerState.LoadingType.None),
            effectsModification = ConfirmationsEffectsStateModification.SendNoSubjectConfirmationRequested
        )

        is SetExpirationDismissed -> ComposerStateModifications(
            effectsModification = BottomSheetEffectsStateModification.HideBottomSheet,
            accessoriesModification = AccessoriesStateModification.MessageExpirationUpdated(expiration)
        )

        is UserChangedSender -> ComposerStateModifications(
            mainModification = MainStateModification.UpdateSender(newSender),
            effectsModification = BottomSheetEffectsStateModification.HideBottomSheet
        )
    }

    data class DraftContentReady(
        val senderEmail: String,
        val isDataRefreshed: Boolean,
        val senderValidationResult: ValidationResult,
        val quotedHtmlContent: QuotedHtmlContent?,
        val shouldRestrictWebViewHeight: Boolean,
        val forceBodyFocus: Boolean
    ) : CompositeEvent

    data class SenderAddressesListReady(val sendersList: List<SenderUiModel>) : CompositeEvent
    data class UserChangedSender(val newSender: SenderEmail) : CompositeEvent

    data class SetExpirationDismissed(val expiration: Duration) : CompositeEvent

    data object OnSendWithEmptySubject : CompositeEvent
}
