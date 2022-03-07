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

package ch.protonmail.android.mailsettings.presentation.accountsettings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.mailsettings.domain.ObserveMailSettings
import ch.protonmail.android.mailsettings.domain.ObservePrimaryUser
import ch.protonmail.android.mailsettings.domain.ObservePrimaryUserSettings
import ch.protonmail.android.mailsettings.presentation.accountsettings.AccountSettingsState.Data
import ch.protonmail.android.mailsettings.presentation.accountsettings.AccountSettingsState.Loading
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import me.proton.core.compose.viewmodel.stopTimeoutMillis
import me.proton.core.mailsettings.domain.entity.ViewMode.ConversationGrouping
import me.proton.core.usersettings.domain.entity.UserSettings
import javax.inject.Inject

@HiltViewModel
class AccountSettingsViewModel @Inject constructor(
    observePrimaryUser: ObservePrimaryUser,
    observePrimaryUserSettings: ObservePrimaryUserSettings,
    observeMailSettings: ObserveMailSettings
) : ViewModel() {

    val state = combine(
        observePrimaryUser(),
        observePrimaryUserSettings(),
        observeMailSettings()
    ) { user, userSettings, mailSettings ->
        Data(
            null,
            getRecoveryEmail(userSettings),
            user?.maxSpace,
            user?.usedSpace,
            user?.email,
            mailSettings?.viewMode?.enum?.let { it == ConversationGrouping }
        )

    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(stopTimeoutMillis),
        Loading
    )

    private fun getRecoveryEmail(userSettings: UserSettings?) = userSettings?.email?.value.let {
        if (it.isNullOrBlank()) null else it
    }

}
