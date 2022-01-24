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

import app.cash.turbine.test
import ch.protonmail.android.mailmailbox.presentation.SelectedSidebarLocation
import ch.protonmail.android.mailmessage.domain.model.SidebarLocation
import ch.protonmail.android.mailmessage.domain.model.SidebarLocation.Archive
import ch.protonmail.android.mailmessage.domain.model.SidebarLocation.Drafts
import kotlinx.coroutines.test.runTest
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
    fun initialSelectedLocationIsInboxByDefault() = runTest {
        selectedSidebarLocation.location.test {
            assertEquals(SidebarLocation.Inbox, awaitItem())
        }
    }

    @Test
    fun emitsNewlySelectedLocationWhenItChanges() = runTest {
        selectedSidebarLocation.location.test {
            assertEquals(SidebarLocation.Inbox, awaitItem())

            selectedSidebarLocation.set(Drafts)

            assertEquals(Drafts, awaitItem())
        }
    }

    @Test
    fun doesNotEmitSameLocationTwice() = runTest {
        selectedSidebarLocation.location.test {
            assertEquals(SidebarLocation.Inbox, awaitItem())

            selectedSidebarLocation.set(Archive)
            selectedSidebarLocation.set(Archive)
            selectedSidebarLocation.set(SidebarLocation.CustomLabel("label"))

            assertEquals(Archive, awaitItem())
            assertEquals(SidebarLocation.CustomLabel("label"), awaitItem())
        }
    }
}
