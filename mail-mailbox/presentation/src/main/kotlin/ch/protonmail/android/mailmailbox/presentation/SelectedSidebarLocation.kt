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

package ch.protonmail.android.mailmailbox.presentation

import ch.protonmail.android.mailmessage.domain.model.SidebarLocation
import ch.protonmail.android.mailmessage.domain.model.SidebarLocation.Inbox
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SelectedSidebarLocation @Inject constructor() {

    private val mutableCurrentLocation = MutableStateFlow<SidebarLocation>(Inbox)

    val location: StateFlow<SidebarLocation> = mutableCurrentLocation.asStateFlow()

    fun set(location: SidebarLocation) {
        mutableCurrentLocation.value = location
    }
}
