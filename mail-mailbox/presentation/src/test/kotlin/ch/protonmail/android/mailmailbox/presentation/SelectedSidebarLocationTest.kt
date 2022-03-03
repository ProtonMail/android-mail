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

import app.cash.turbine.test
import ch.protonmail.android.mailmailbox.domain.model.SidebarLocation
import kotlinx.coroutines.test.runTest
import me.proton.core.label.domain.entity.LabelId
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class SelectedSidebarLocationTest {

    private lateinit var selectedSidebarLocation: SelectedSidebarLocation

    @Before
    fun setUp() {
        selectedSidebarLocation = SelectedSidebarLocation()
    }

    @Test
    fun `initial selected location is inbox by default`() = runTest {
        selectedSidebarLocation.location.test {
            assertEquals(SidebarLocation.Inbox, awaitItem())
        }
    }

    @Test
    fun `emits newly selected location when it changes`() = runTest {
        selectedSidebarLocation.location.test {
            assertEquals(SidebarLocation.Inbox, awaitItem())

            selectedSidebarLocation.set(SidebarLocation.Drafts)

            assertEquals(SidebarLocation.Drafts, awaitItem())
        }
    }

    @Test
    fun `does not emit same location twice`() = runTest {
        selectedSidebarLocation.location.test {
            assertEquals(SidebarLocation.Inbox, awaitItem())

            selectedSidebarLocation.set(SidebarLocation.Archive)
            selectedSidebarLocation.set(SidebarLocation.Archive)
            selectedSidebarLocation.set(SidebarLocation.CustomLabel(LabelId("lId1")))

            assertEquals(SidebarLocation.Archive, awaitItem())
            assertEquals(SidebarLocation.CustomLabel(LabelId("lId1")), awaitItem())
        }
    }
}
