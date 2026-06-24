/*
 * Copyright (c) 2026 Proton Technologies AG
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

package ch.protonmail.android.mailmailbox.presentation.mailbox.reducer

import ch.protonmail.android.mailcategory.presentation.mapper.CategoryViewUiModelMapper
import ch.protonmail.android.mailcategory.presentation.model.CategorySpotlightState
import ch.protonmail.android.mailcategory.presentation.model.CategoryViewState
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxEvent
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxOperation
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxViewAction
import javax.inject.Inject

class MailboxCategoryViewReducer @Inject constructor(
    private val categoryViewUiModelMapper: CategoryViewUiModelMapper
) {

    internal fun newStateFrom(
        currentState: CategoryViewState,
        operation: MailboxOperation.AffectingCategoryView
    ): CategoryViewState {
        return when (operation) {
            is MailboxEvent.CategoryViewStatusChanged -> {
                val newState = categoryViewUiModelMapper.toUiModel(operation.categoryViewStatus)

                if (newState is CategoryViewState.Available.Data && currentState is CategoryViewState.Available.Data) {
                    newState.copy(spotlightState = currentState.spotlightState)
                } else {
                    newState
                }
            }

            is MailboxEvent.CategorySpotlightStateChanged -> {
                if (currentState is CategoryViewState.Available.Data) {
                    currentState.copy(spotlightState = operation.categorySpotlightState)
                } else {
                    currentState
                }
            }

            MailboxViewAction.DismissCategorySpotlight -> {
                if (currentState is CategoryViewState.Available.Data) {
                    currentState.copy(spotlightState = CategorySpotlightState.Hidden)
                } else {
                    currentState
                }
            }

            MailboxEvent.PrimaryAccountChanged -> {
                when (currentState) {
                    is CategoryViewState.Available.Data -> {
                        currentState.copy(resetScrollEffect = Effect.of(Unit))
                    }

                    else -> currentState
                }
            }
        }
    }
}
