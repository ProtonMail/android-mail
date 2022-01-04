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

package ch.protonmail.android.navigation

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.BuildConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SidebarViewModel @Inject constructor(
) : ViewModel() {

    val state: StateFlow<State> = flowOf(State())
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = State()
        )

    data class State(
        val isAccountVisible: Boolean = true,
        val appName: String = "ProtonMail",
        val appVersion: String = BuildConfig.VERSION_NAME,
        val inboxCount: Int? = 1,
        val draftsCount: Int? = null,
        val sentCount: Int? = null,
        val starredCount: Int? = 1,
        val archiveCount: Int? = null,
        val spamCount: Int? = null,
        val trashCount: Int? = null,
        val allMailCount: Int? = 1,
        val folders: List<Folder> = listOf(
            Folder("1", "Folder 1", Color.Red)
        ),
        val labels: List<Label> = listOf(
            Label("1", "Label 1", Color.Cyan),
            Label("2", "Label 2", Color.Yellow)
        )
    )

    data class Folder(
        val id: String,
        val text: String,
        val color: Color
    )

    data class Label(
        val id: String,
        val text: String,
        val color: Color
    )
}
