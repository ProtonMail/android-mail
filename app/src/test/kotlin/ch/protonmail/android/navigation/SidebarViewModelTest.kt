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

import app.cash.turbine.test
import ch.protonmail.android.mailmailbox.domain.model.SidebarLocation
import ch.protonmail.android.mailmailbox.domain.model.SidebarLocation.Inbox
import ch.protonmail.android.mailmailbox.presentation.SelectedSidebarLocation
import ch.protonmail.android.sidebar.SidebarViewModel
import ch.protonmail.android.sidebar.SidebarViewModel.State.Enabled
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class SidebarViewModelTest {

    private val selectedSidebarLocation = mockk<SelectedSidebarLocation> {
        every { location } returns MutableStateFlow<SidebarLocation>(Inbox)
    }

    private lateinit var sidebarViewModel: SidebarViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())

        sidebarViewModel = SidebarViewModel(selectedSidebarLocation)
    }

    @Test
    fun emitsInitialSidebarState() = runTest {
        sidebarViewModel.state.test {
            val actual = awaitItem() as Enabled
            val expected = Inbox

            assertEquals(expected, actual.selectedLocation)
        }
    }
}
