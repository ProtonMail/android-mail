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
import ch.protonmail.android.mailmailbox.presentation.SelectedMailboxLocation
import ch.protonmail.android.mailmessage.domain.model.MailLocation
import ch.protonmail.android.mailmessage.domain.model.MailLocation.Inbox
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import me.proton.core.compose.viewmodel.stopTimeoutMillis
import javax.inject.Inject

@HiltViewModel
class SidebarViewModel @Inject constructor(
    private val selectedMailboxLocation: SelectedMailboxLocation
) : ViewModel() {

    val state: Flow<State> = selectedMailboxLocation.location.mapLatest {
        State.Enabled(it)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(stopTimeoutMillis),
        State.Enabled(Inbox)
    )

    fun onLocationSelected(location: MailLocation) {
        selectedMailboxLocation.set(location)
    }

    sealed class State {
        data class Enabled(val selectedLocation: MailLocation) : State()
        object Disabled : State()
    }
}

