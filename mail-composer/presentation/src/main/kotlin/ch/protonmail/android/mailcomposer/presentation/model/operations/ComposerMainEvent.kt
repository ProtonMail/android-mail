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

import ch.protonmail.android.mailcomposer.domain.model.SenderEmail
import ch.protonmail.android.mailcomposer.presentation.model.ComposerState
import ch.protonmail.android.mailcomposer.presentation.reducer.modifications.ComposerStateModifications
import ch.protonmail.android.mailcomposer.presentation.reducer.modifications.MainStateModification.RemoveHtmlQuotedText
import ch.protonmail.android.mailcomposer.presentation.reducer.modifications.MainStateModification.UpdateLoading
import ch.protonmail.android.mailcomposer.presentation.reducer.modifications.MainStateModification.UpdateSender
import ch.protonmail.android.mailcomposer.presentation.reducer.modifications.MainStateModification.UpdateSubmittable

internal sealed interface MainEvent : ComposerStateEvent {

    override fun toStateModifications(): ComposerStateModifications = ComposerStateModifications(
        mainModification = when (this) {
            is InitialLoadingToggled -> UpdateLoading(ComposerState.LoadingType.Initial)
            is CoreLoadingToggled -> UpdateLoading(ComposerState.LoadingType.Save)
            is LoadingDismissed -> UpdateLoading(ComposerState.LoadingType.None)
            is SenderChanged -> UpdateSender(newSender)
            is RecipientsChanged -> UpdateSubmittable(areSubmittable)
            is OnQuotedHtmlRemoved -> RemoveHtmlQuotedText
        }
    )

    data object InitialLoadingToggled : MainEvent
    data object CoreLoadingToggled : MainEvent
    data object LoadingDismissed : MainEvent
    data class RecipientsChanged(val areSubmittable: Boolean) : MainEvent
    data class SenderChanged(val newSender: SenderEmail) : MainEvent
    data object OnQuotedHtmlRemoved : MainEvent
}
