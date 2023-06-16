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
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailcomposer.domain.usecase.HandleDraftBodyChange
import ch.protonmail.android.mailcomposer.domain.usecase.IsValidEmailAddress
import ch.protonmail.android.mailcomposer.presentation.model.ComposerAction
import ch.protonmail.android.mailcomposer.domain.usecase.ProvideNewDraftId
import ch.protonmail.android.mailmessage.domain.entity.MessageId
import ch.protonmail.android.mailcomposer.presentation.model.ComposerDraftState
import ch.protonmail.android.mailcomposer.presentation.reducer.ComposerReducer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import me.proton.core.user.domain.UserAddressManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import me.proton.core.user.domain.entity.UserAddress
import javax.inject.Inject

@HiltViewModel
class ComposerViewModel @Inject constructor(
    private val reducer: ComposerReducer,
    private val isValidEmailAddress: IsValidEmailAddress,
    private val handleEditedDraftBodyChange: HandleDraftBodyChange,
    private val observePrimaryUserId: ObservePrimaryUserId,
    private val userAddressManager: UserAddressManager,
    provideNewDraftId: ProvideNewDraftId
) : ViewModel() {

    private val messageId = MessageId(provideNewDraftId().id)

    private val mutableState = MutableStateFlow(ComposerDraftState.empty(provideNewDraftId()))
    val state: StateFlow<ComposerDraftState> = mutableState

    internal fun submit(action: ComposerAction) {
        if (action is ComposerAction.DraftBodyChanged) {
            viewModelScope.launch {
                handleEditedDraftBodyChange(messageId, action.draftBody, senderAddress())
            }
        }
        val currentState = state.value
        mutableState.value = reducer.newStateFrom(currentState, action)
    }

    fun validateEmailAddress(emailAddress: String): Boolean = isValidEmailAddress(emailAddress)

    // This is a temp code till we implement senders properly
    private suspend fun senderAddress(): UserAddress = observePrimaryUserId()
        .filterNotNull()
        .flatMapLatest { userId -> userAddressManager.observeAddresses(userId) }
        .filterNotNull()
        .first()
        .first()
}
