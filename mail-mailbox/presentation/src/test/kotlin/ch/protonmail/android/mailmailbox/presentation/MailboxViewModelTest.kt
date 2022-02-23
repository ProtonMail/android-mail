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
import ch.protonmail.android.mailmailbox.domain.model.SidebarLocation.Archive
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.domain.entity.UserId
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class MailboxViewModelTest {

    private val userIdFlow = MutableSharedFlow<UserId?>()
    private val accountManager = mockk<AccountManager> {
        every { this@mockk.getPrimaryUserId() } returns userIdFlow
    }

    private val selectedSidebarLocation = mockk<SelectedSidebarLocation> {
        every { this@mockk.location } returns MutableStateFlow<SidebarLocation>(Archive)
    }

    private lateinit var mailboxViewModel: MailboxViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())

        mailboxViewModel = MailboxViewModel(
            accountManager,
            selectedSidebarLocation
        )
    }

    @Test
    fun emitsInitialLoadingMailboxStateWhenInitialized() = runTest {
        // When
        mailboxViewModel.state.test {
            // Then
            val actual = awaitItem()
            val expected = MailboxState(loading = true)

            assertEquals(expected, actual)
        }
    }

    @Test
    fun emitsMailboxStateWithCurrentLocation() = runTest {
        mailboxViewModel.state.test {
            awaitItem() // Initial item

            // When
            userIdFlow.emit(UserId("userId"))

            // Then
            val actual = awaitItem()
            assertEquals(false, actual.loading)
            assertEquals(setOf(Archive), actual.filteredLocations)
        }
    }

}
