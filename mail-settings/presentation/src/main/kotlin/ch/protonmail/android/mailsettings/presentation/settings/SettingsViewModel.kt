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

package ch.protonmail.android.mailsettings.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.mailcommon.domain.AppInformation
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUser
import ch.protonmail.android.mailsettings.domain.usecase.ObserveAppSettings
import ch.protonmail.android.mailsettings.presentation.settings.SettingsState.Data
import ch.protonmail.android.mailsettings.presentation.settings.SettingsState.Loading
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import me.proton.core.compose.viewmodel.stopTimeoutMillis
import me.proton.core.user.domain.entity.User
import me.proton.core.util.kotlin.takeIfNotEmpty
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    appInformation: AppInformation,
    observePrimaryUser: ObservePrimaryUser,
    observeAppSettings: ObserveAppSettings,
) : ViewModel() {

    val state = combine(
        observePrimaryUser(),
        observeAppSettings()
    ) { user, appSettings ->
        Data(
            buildAccountData(user),
            appSettings,
            appInformation,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(stopTimeoutMillis),
        Loading
    )

    private fun buildAccountData(user: User?) = user?.let {
        AccountInfo(
            name = getUsername(it),
            email = it.email.orEmpty()
        )
    }

    // For Mail product, we know that user.name will always be non-null, non-empty and non-blank.
    private fun getUsername(user: User) = user.displayName?.takeIfNotEmpty() ?: requireNotNull(user.name)

}
