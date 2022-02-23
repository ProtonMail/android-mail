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

package ch.protonmail.android.mailsettings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.mailsettings.presentation.State.Loading
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.compose.viewmodel.stopTimeoutMillis
import me.proton.core.domain.arch.mapSuccessValueOrNull
import me.proton.core.user.domain.UserManager
import me.proton.core.user.domain.entity.User
import javax.inject.Inject

@HiltViewModel
class MainSettingsViewModel @Inject constructor(
    accountManager: AccountManager,
    userManager: UserManager
) : ViewModel() {

    val state = accountManager.getPrimaryUserId().filterNotNull().flatMapLatest { userId ->
        userManager.getUserFlow(userId).mapSuccessValueOrNull().mapLatest {
            State.Data(buildAccountData(it))
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(stopTimeoutMillis),
        Loading
    )

    private fun buildAccountData(user: User?) = if (user != null) {
        AccountData(user.displayName.orEmpty(), user.email.orEmpty())
    } else {
        null
    }

}

sealed class State {
    data class Data(
        val account: AccountData?
    ) : State()

    object Loading : State()
}

data class AccountData(
    val name: String,
    val email: String
)
