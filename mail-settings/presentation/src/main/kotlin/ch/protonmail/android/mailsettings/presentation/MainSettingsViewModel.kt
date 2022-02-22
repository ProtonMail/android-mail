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
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.accountmanager.domain.getPrimaryAccount
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MainSettingsViewModel @Inject constructor(
    accountManager: AccountManager
) : ViewModel() {

    val state = accountManager.getPrimaryAccount()
        .filterNotNull()
        .mapLatest { account ->
            Timber.d("Loaded primary account ${account.username} email ${account.email}")
            State.Data(account.username, account.email)
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(),
            Loading
        )

}

sealed class State {
    data class Data(
        val name: String,
        val email: String?
    ) : State()

    object Loading : State()
}
