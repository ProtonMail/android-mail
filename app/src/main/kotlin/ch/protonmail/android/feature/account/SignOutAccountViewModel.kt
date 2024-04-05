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

package ch.protonmail.android.feature.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.mailcommon.data.worker.Enqueuer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

@HiltViewModel
class SignOutAccountViewModel @Inject constructor(
    private val accountManager: AccountManager,
    private val enqueuer: Enqueuer
) : ViewModel() {

    private val mutableState = MutableStateFlow<State>(State.Initial)
    val state = mutableState.asStateFlow()

    fun signOut(userId: UserId? = null, removeAccount: Boolean = false) = viewModelScope.launch {
        val resolvedUserId = requireNotNull(userId ?: getPrimaryUserIdOrNull())
        enqueuer.cancelAllWork(resolvedUserId)

        if (removeAccount) {
            mutableState.emit(State.Removing)
            accountManager.removeAccount(resolvedUserId)
            mutableState.emit(State.Removed)
        } else {
            mutableState.emit(State.SigningOut)
            accountManager.disableAccount(resolvedUserId)
            mutableState.emit(State.SignedOut)
        }
    }

    private suspend fun getPrimaryUserIdOrNull() = accountManager.getPrimaryUserId().firstOrNull()

    sealed class State {
        object Initial : State()
        object SigningOut : State()
        object SignedOut : State()
        object Removing : State()
        object Removed : State()
    }
}
