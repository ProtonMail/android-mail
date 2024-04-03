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

package ch.protonmail.android.mailmessage.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.mailmessage.presentation.model.webview.MessageBodyWebViewOperation.MessageBodyWebViewAction
import ch.protonmail.android.mailmessage.presentation.model.webview.MessageBodyWebViewOperation.MessageBodyWebViewEvent
import ch.protonmail.android.mailmessage.presentation.model.webview.MessageBodyWebViewState
import ch.protonmail.android.mailmessage.presentation.reducer.MessageBodyWebViewReducer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MessageBodyWebViewViewModel @Inject constructor(
    private val reducer: MessageBodyWebViewReducer
) : ViewModel() {

    private val mutableState = MutableStateFlow(MessageBodyWebViewState.Initial)
    val state = mutableState.asStateFlow()

    fun submit(action: MessageBodyWebViewAction) {
        viewModelScope.launch {
            when (action) {
                is MessageBodyWebViewAction.LongClickLink ->
                    emitNewStateFrom(MessageBodyWebViewEvent.LinkLongClicked(action.uri))
            }
        }
    }

    private suspend fun emitNewStateFrom(operation: MessageBodyWebViewEvent) {
        val updatedState = reducer.newStateFrom(state.value, operation)
        mutableState.emit(updatedState)
    }
}
