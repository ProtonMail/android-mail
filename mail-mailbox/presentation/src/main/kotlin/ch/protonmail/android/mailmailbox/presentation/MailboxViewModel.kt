/*
 * Copyright (c) 2021 Proton Technologies AG
 * This file is part of Proton Technologies AG and ProtonMail.
 *
 * ProtonMail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonMail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonMail.  If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.mailmailbox.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.compose.Constants
import ch.protonmail.android.mailconversation.domain.Conversation
import ch.protonmail.android.mailconversation.domain.ConversationId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MailboxViewModel @Inject constructor(
    accountManager: AccountManager,
) : ViewModel() {

    @SuppressWarnings("UseIfInsteadOfWhen")
    val viewState: Flow<State> = accountManager.getPrimaryUserId()
        .flatMapLatest { userId ->
            when (userId) {
                null -> flowOf(State.initialState)
                else -> observeState(userId)
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(Constants.VIEW_MODEL_STOP_STATE_EMISSION_DELAY),
            State.initialState
        )

    private fun observeState(userId: UserId): Flow<State> =
        observeConversations(userId = userId).mapLatest { messages ->
            State(
                loading = false,
                mailboxItems = messages
            )
        }

    private fun observeConversations(userId: UserId): Flow<List<Conversation>> {
        Timber.d("Faking getting messages for userId $userId")
        return flowOf(
            listOf(
                Conversation(ConversationId("1"), "First message"),
                Conversation(ConversationId("2"), "Second message"),
                Conversation(ConversationId("3"), "Third message"),
                Conversation(ConversationId("4"), "Fourth message"),
                Conversation(ConversationId("5"), "Fifth message"),
                Conversation(ConversationId("6"), "Sixth message"),
            )
        )
    }

    data class State(
        val loading: Boolean,
        val mailboxItems: List<Conversation>
    ) {
        companion object {
            val initialState = State(
                loading = true,
                mailboxItems = emptyList()
            )
        }
    }
}
