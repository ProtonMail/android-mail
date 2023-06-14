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

package ch.protonmail.android.mailcomposer.presentation.viewmodel

import androidx.lifecycle.ViewModel
import ch.protonmail.android.mailcomposer.presentation.model.ComposerAction
import ch.protonmail.android.mailcomposer.domain.usecase.ProvideNewDraftId
import ch.protonmail.android.mailcomposer.presentation.model.ComposerDraftState
import ch.protonmail.android.mailcomposer.presentation.model.RecipientUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ComposerViewModel @Inject constructor(
    provideNewDraftId: ProvideNewDraftId
) : ViewModel() {

    private val mutableState = MutableStateFlow(ComposerDraftState.empty(provideNewDraftId()))
    val state: StateFlow<ComposerDraftState> = mutableState

    init {
        Timber.d(state.value.toString())
    }

    internal fun submit(action: ComposerAction) {
        when (action) {
            is ComposerAction.RecipientsBccChanged -> updateRecipientsBcc(action.recipients)
            is ComposerAction.RecipientsCcChanged -> updateRecipientsCc(action.recipients)
            is ComposerAction.RecipientsToChanged -> updateRecipientsTo(action.recipients)
        }
    }

    private fun updateRecipientsTo(recipients: List<RecipientUiModel>) {
        val currentState = state.value
        mutableState.value = when (currentState) {
            is ComposerDraftState.Submittable -> currentState.copy(to = recipients)
        }
    }

    private fun updateRecipientsCc(recipients: List<RecipientUiModel>) {
        val currentState = state.value
        mutableState.value = when (currentState) {
            is ComposerDraftState.Submittable -> currentState.copy(cc = recipients)
        }
    }

    private fun updateRecipientsBcc(recipients: List<RecipientUiModel>) {
        val currentState = state.value
        mutableState.value = when (currentState) {
            is ComposerDraftState.Submittable -> currentState.copy(bcc = recipients)
        }
    }
}
