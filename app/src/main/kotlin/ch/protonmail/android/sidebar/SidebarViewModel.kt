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

package ch.protonmail.android.sidebar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.MailFeatureFlags.ShowSettings
import ch.protonmail.android.mailmailbox.domain.model.SidebarLocation
import ch.protonmail.android.mailmailbox.domain.model.SidebarLocation.Inbox
import ch.protonmail.android.mailmailbox.presentation.SelectedSidebarLocation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.compose.viewmodel.stopTimeoutMillis
import me.proton.core.featureflag.domain.FeatureFlagManager
import javax.inject.Inject

@HiltViewModel
class SidebarViewModel @Inject constructor(
    private val selectedSidebarLocation: SelectedSidebarLocation,
    private val featureFlagManager: FeatureFlagManager,
    private val accountManager: AccountManager
) : ViewModel() {

    val state: Flow<State> = combine(
        selectedSidebarLocation.location,
        observePrimaryUserFeatureFlag()
    ) { location, settingsFeature ->
        State.Enabled(
            selectedLocation = location,
            isSettingsEnabled = settingsFeature?.value ?: ShowSettings.defaultLocalValue
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(stopTimeoutMillis),
        State.Enabled(Inbox, ShowSettings.defaultLocalValue)
    )

    fun onSidebarItemSelected(location: SidebarLocation) {
        selectedSidebarLocation.set(location)
    }

    private fun observePrimaryUserFeatureFlag() = accountManager.getPrimaryUserId()
        .flatMapLatest { userId ->
            featureFlagManager.observe(userId, ShowSettings.id)
        }

    sealed class State {
        data class Enabled(
            val selectedLocation: SidebarLocation,
            val isSettingsEnabled: Boolean
        ) : State()

        object Disabled : State()
    }
}

