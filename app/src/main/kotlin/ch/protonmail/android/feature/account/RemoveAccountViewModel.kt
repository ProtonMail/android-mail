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
import ch.protonmail.android.feature.account.RemoveAccountViewModel.State.Initial
import ch.protonmail.android.feature.account.RemoveAccountViewModel.State.Removed
import ch.protonmail.android.feature.account.RemoveAccountViewModel.State.Removing
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
class RemoveAccountViewModel @Inject constructor(
    private val accountManager: AccountManager,
    private val enqueuer: Enqueuer
) : ViewModel() {

    private val mutableState = MutableStateFlow<State>(Initial)

    val state = mutableState.asStateFlow()

    fun remove(userId: UserId? = null) = viewModelScope.launch {
        mutableState.emit(Removing)

        val resolvedUserId = requireNotNull(userId ?: getPrimaryUserIdOrNull())
        enqueuer.cancelAllWork(resolvedUserId)
        accountManager.removeAccount(resolvedUserId)

        mutableState.emit(Removed)
    }

    private suspend fun getPrimaryUserIdOrNull() = accountManager.getPrimaryUserId().firstOrNull()

    sealed class State {
        object Initial : State()
        object Removing : State()
        object Removed : State()
    }
}
