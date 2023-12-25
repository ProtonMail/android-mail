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

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailcomposer.domain.usecase.SaveMessagePassword
import ch.protonmail.android.mailcomposer.presentation.ui.SetMessagePasswordScreen
import ch.protonmail.android.mailmessage.domain.model.MessageId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SetMessagePasswordViewModel @Inject constructor(
    private val saveMessagePassword: SaveMessagePassword,
    observePrimaryUserId: ObservePrimaryUserId,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val primaryUserId = observePrimaryUserId().filterNotNull()
    private val messageId = savedStateHandle.get<String>(SetMessagePasswordScreen.DraftMessageIdKey)

    fun submit(action: MessagePasswordAction) = when (action) {
        is MessagePasswordAction.ApplyPassword -> onApplyPassword(action.password, action.passwordHint)
    }

    private fun onApplyPassword(password: String, passwordHint: String?) {
        viewModelScope.launch {
            messageId?.let {
                saveMessagePassword(primaryUserId.first(), MessageId(it), password, passwordHint)
            }
        }
    }
}

sealed interface MessagePasswordAction {
    data class ApplyPassword(val password: String, val passwordHint: String?) : MessagePasswordAction
}
