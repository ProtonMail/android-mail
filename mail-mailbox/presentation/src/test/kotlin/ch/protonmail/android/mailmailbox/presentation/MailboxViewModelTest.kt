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
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemType.Message
import ch.protonmail.android.mailmailbox.domain.model.SidebarLocation
import ch.protonmail.android.mailmailbox.domain.model.SidebarLocation.Archive
import ch.protonmail.android.mailmailbox.domain.usecase.MarkAsStaleMailboxItems
import ch.protonmail.android.mailmailbox.domain.usecase.ObserveMailboxItemType
import ch.protonmail.android.mailmailbox.presentation.model.MailboxTopAppBarState
import ch.protonmail.android.mailmailbox.presentation.paging.MailboxItemPagingSourceFactory
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.domain.entity.UserId
import org.junit.Before
import org.junit.Test
import kotlin.test.*

class MailboxViewModelTest {

    private val userId = UserId("userId")
    private val userIdFlow = MutableSharedFlow<UserId?>()
    private val accountManager = mockk<AccountManager> {
        every { this@mockk.getPrimaryUserId() } returns userIdFlow
    }

    private val selectedSidebarLocation = mockk<SelectedSidebarLocation> {
        every { this@mockk.location } returns MutableStateFlow<SidebarLocation>(Archive)
    }

    private val markAsStaleMailboxItems = mockk<MarkAsStaleMailboxItems> {
        coEvery { this@mockk.invoke(any(), any(), any()) } returns Unit
    }
    private val observeMailboxItemType = mockk<ObserveMailboxItemType> {
        coEvery { this@mockk.invoke() } returns flowOf(Message)
    }
    private val pagingSourceFactory = mockk<MailboxItemPagingSourceFactory>(relaxed = true)

    private lateinit var mailboxViewModel: MailboxViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())

        mailboxViewModel = MailboxViewModel(
            accountManager,
            selectedSidebarLocation,
            markAsStaleMailboxItems,
            observeMailboxItemType,
            pagingSourceFactory
        )
    }

    @Test
    fun `emits initial mailbox state when initialized`() = runTest {
        // When
        mailboxViewModel.state.test {
            // Then
            val actual = awaitItem()
            val expected = MailboxState.Loading

            assertEquals(expected, actual)

            verify { pagingSourceFactory wasNot Called }
        }
    }

    @Test
    fun `emits default TopAppBar state as soon as the label name is available`() = runTest {
        // Given
        val expected = MailboxTopAppBarState.Data.DefaultMode(Archive::class.simpleName!!)

        // When
        mailboxViewModel.state.test {
            assertEquals(MailboxState.Loading, awaitItem())
            userIdFlow.emit(userId)

            // Then
            assertEquals(expected, awaitItem().topAppBar)
        }
    }

    @Test
    fun `emits mailbox state with current location`() = runTest {
        mailboxViewModel.state.test {
            awaitItem() // Initial item

            // When
            userIdFlow.emit(userId)

            // Then
            val actual = awaitItem()
            assertEquals(Archive, actual.selectedLocation)
        }
    }

    @Test
    fun `emits mailbox items`() = runTest {
        mailboxViewModel.items.test {
            awaitItem() // Initial item

            // When
            userIdFlow.emit(userId)

            // Then
            awaitItem()

            verify { pagingSourceFactory.create(listOf(userId), Archive, Message) }
        }
    }

    @Test
    fun `onRefresh call markAsStaleMailboxItems`() = runTest {
        // Given
        userIdFlow.emit(userId)

        // When
        mailboxViewModel.submit(MailboxViewModel.Action.Refresh)

        // Then
        coVerify { markAsStaleMailboxItems.invoke(listOf(userId), Message, Archive.labelId) }
    }
}
