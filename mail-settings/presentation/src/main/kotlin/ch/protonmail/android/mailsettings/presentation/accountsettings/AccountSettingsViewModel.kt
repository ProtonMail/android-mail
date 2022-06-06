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

package ch.protonmail.android.mailsettings.presentation.accountsettings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.mailcommon.domain.usecase.ObserveUser
import ch.protonmail.android.mailsettings.domain.usecase.ObserveMailSettings
import ch.protonmail.android.mailsettings.domain.usecase.ObserveUserSettings
import ch.protonmail.android.mailsettings.presentation.accountsettings.AccountSettingsState.Data
import ch.protonmail.android.mailsettings.presentation.accountsettings.AccountSettingsState.Loading
import ch.protonmail.android.mailsettings.presentation.accountsettings.AccountSettingsState.NotLoggedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.compose.viewmodel.stopTimeoutMillis
import me.proton.core.mailsettings.domain.entity.ViewMode.ConversationGrouping
import me.proton.core.usersettings.domain.entity.UserSettings
import me.proton.core.util.kotlin.takeIfNotBlank
import javax.inject.Inject

@HiltViewModel
class AccountSettingsViewModel @Inject constructor(
    accountManager: AccountManager,
    private val observeUser: ObserveUser,
    private val observeUserSettings: ObserveUserSettings,
    private val observeMailSettings: ObserveMailSettings
) : ViewModel() {

    val state: StateFlow<AccountSettingsState> = accountManager.getPrimaryUserId().flatMapLatest { _userId ->
        val userId = _userId
            ?: return@flatMapLatest flowOf(NotLoggedIn)

        combine(
            observeUser(userId),
            observeUserSettings(userId),
            observeMailSettings(userId)
        ) { user, userSettings, mailSettings ->
            Data(
                getRecoveryEmail(userSettings),
                user?.maxSpace,
                user?.usedSpace,
                user?.email,
                mailSettings?.viewMode?.enum?.let { it == ConversationGrouping }
            )
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(stopTimeoutMillis),
        Loading
    )

    private fun getRecoveryEmail(userSettings: UserSettings?) =
        userSettings?.email?.value?.takeIfNotBlank()

}
