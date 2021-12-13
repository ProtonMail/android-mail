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

package ch.protonmail.android.navigation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.navigation.viewmodel.SignOutViewModel.State.Initial
import ch.protonmail.android.navigation.viewmodel.SignOutViewModel.State.SignedOut
import ch.protonmail.android.navigation.viewmodel.SignOutViewModel.State.SigningOut
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

@HiltViewModel
class SignOutViewModel @Inject constructor(
    val accountManager: AccountManager
) : ViewModel() {

    private val mutableState = MutableStateFlow<State>(Initial)

    val state = mutableState.asStateFlow()

    fun signOut(userId: UserId? = null) = viewModelScope.launch {
        mutableState.emit(SigningOut)
        accountManager.disableAccount(requireNotNull(userId ?: getPrimaryUserIdOrNull()))
        mutableState.emit(SignedOut)
    }

    private suspend fun getPrimaryUserIdOrNull() = accountManager.getPrimaryUserId().firstOrNull()

    sealed class State {
        object Initial : State()
        object SigningOut : State()
        object SignedOut : State()
    }
}
