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
import ch.protonmail.android.mailsettings.domain.ObserveAppSettings
import ch.protonmail.android.mailsettings.domain.model.AppInformation
import ch.protonmail.android.mailsettings.domain.model.AppSettings
import ch.protonmail.android.mailsettings.presentation.State.Loading
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.compose.viewmodel.stopTimeoutMillis
import me.proton.core.domain.arch.mapSuccessValueOrNull
import me.proton.core.user.domain.UserManager
import me.proton.core.user.domain.entity.User
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    accountManager: AccountManager,
    userManager: UserManager,
    observeAppSettings: ObserveAppSettings,
    getAppInformation: GetAppInformation
) : ViewModel() {

    val state = combine(
        observePrimaryUser(accountManager, userManager),
        observeAppSettings()
    ) { user, appSettings ->
        State.Data(
            buildAccountData(user),
            appSettings,
            getAppInformation()
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(stopTimeoutMillis),
        Loading
    )

    private fun observePrimaryUser(
        accountManager: AccountManager,
        userManager: UserManager
    ) = accountManager.getPrimaryUserId().filterNotNull().flatMapLatest { userId ->
        userManager.getUserFlow(userId).mapSuccessValueOrNull()
    }

    private fun buildAccountData(user: User?) = if (user != null) {
        AccountInfo(user.displayName.orEmpty(), user.email.orEmpty())
    } else {
        null
    }

}

sealed class State {
    data class Data(
        val account: AccountInfo?,
        val appSettings: AppSettings,
        val appInformation: AppInformation
    ) : State()

    object Loading : State()
}

data class AccountInfo(
    val name: String,
    val email: String
)
