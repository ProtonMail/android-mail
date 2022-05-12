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
import ch.protonmail.android.mailconversation.domain.entity.ConversationId
import ch.protonmail.android.mailmailbox.domain.model.MailboxItem
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemId
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemType
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemType.Conversation
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemType.Message
import ch.protonmail.android.mailmailbox.domain.model.OpenMailboxItemRequest
import ch.protonmail.android.mailmailbox.domain.model.SidebarLocation
import ch.protonmail.android.mailmailbox.domain.model.SidebarLocation.Archive
import ch.protonmail.android.mailmailbox.domain.usecase.MarkAsStaleMailboxItems
import ch.protonmail.android.mailmailbox.domain.usecase.ObserveMailboxItemType
import ch.protonmail.android.mailmailbox.presentation.MailboxViewModel.Action
import ch.protonmail.android.mailmailbox.presentation.model.MailboxTopAppBarState
import ch.protonmail.android.mailmailbox.presentation.paging.MailboxItemPagingSourceFactory
import ch.protonmail.android.testdata.user.UserIdTestData.userId
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
import kotlin.test.Ignore
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MailboxViewModelTest {

    private val userIdFlow = MutableSharedFlow<UserId?>()
    private val accountManager = mockk<AccountManager> {
        every { getPrimaryUserId() } returns flowOf(userId)
    }

    private val selectedSidebarLocation = mockk<SelectedSidebarLocation> {
        every { this@mockk.location } returns MutableStateFlow<SidebarLocation>(Archive)
    }

    private val markAsStaleMailboxItems = mockk<MarkAsStaleMailboxItems> {
        coEvery { this@mockk.invoke(any(), any(), any()) } returns Unit
    }
    private val observeMailboxItemType = mockk<ObserveMailboxItemType> {
        coEvery { this@mockk.invoke() } returns flowOf(Message)
        coEvery { this@mockk.invoke(any()) } returns flowOf(Message)
    }
    private val pagingSourceFactory = mockk<MailboxItemPagingSourceFactory>(relaxed = true)

    private val mailboxViewModel by lazy {
        MailboxViewModel(
            accountManager,
            selectedSidebarLocation,
            markAsStaleMailboxItems,
            observeMailboxItemType,
            pagingSourceFactory
        )
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @Test
    fun `emits initial mailbox state when initialized`() = runTest {
        // given
        givenUserNotLoggedIn()

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
        // When
        mailboxViewModel.state.test {

            // Then
            val expected = MailboxTopAppBarState.Data.DefaultMode(Archive::class.simpleName!!)
            assertEquals(expected, awaitItem().topAppBar)
        }
    }

    @Test
    fun `when selection mode is not open and the right Action is submitted, selection mode is opened`() = runTest {
        // given
        mailboxViewModel.state.test {
            awaitItem() // First emission for selected user

            // When
            mailboxViewModel.submit(Action.EnterSelectionMode)

            // Then
            val expected = MailboxTopAppBarState.Data.SelectionMode(Archive::class.simpleName!!, selectedCount = 0)
            assertEquals(expected, awaitItem().topAppBar)
        }
    }

    @Test
    fun `when selection mode is open and the right Action is submitted, selection mode is closed`() = runTest {
        // given
        mailboxViewModel.state.test {
            awaitItem() // First emission for selected user

            mailboxViewModel.submit(Action.EnterSelectionMode)
            awaitItem() // Selection Mode has been opened

            // When
            mailboxViewModel.submit(Action.ExitSelectionMode)

            // Then
            val expected = MailboxTopAppBarState.Data.DefaultMode(Archive::class.simpleName!!)
            assertEquals(expected, awaitItem().topAppBar)
        }
    }

    @Test
    fun `emits mailbox state with current location`() = runTest {
        mailboxViewModel.state.test {

            // Then
            val actual = awaitItem()
            assertEquals(Archive, actual.selectedLocation)
        }
    }

    @Test
    fun `emits mailbox items`() = runTest {
        mailboxViewModel.items.test {

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
        mailboxViewModel.submit(Action.Refresh)

        // Then
        coVerify { markAsStaleMailboxItems.invoke(listOf(userId), Message, Archive.labelId) }
    }

    @Test
    fun `open item details actions generates a request to open Message details for a Message while in Message mode`() =
        runTest {

            // given
            val item = buildMailboxItem(Message)
            every { observeMailboxItemType() } returns flowOf(Message)

            // when
            mailboxViewModel.submit(Action.OpenItemDetails(item))
            mailboxViewModel.state.test {

                // then
                val expected = OpenMailboxItemRequest(MailboxItemId(item.id), Message)
                assertEquals(expected, awaitItem().openItemEffect.consume())
            }
        }

    @Test
    fun `open item details actions generates a request to open Conversation details for a Conversation while in Conversation mode`() =
        runTest {

            // given
            val item = buildMailboxItem(Conversation)
            every { observeMailboxItemType() } returns flowOf(Conversation)

            // when
            mailboxViewModel.submit(Action.OpenItemDetails(item))
            mailboxViewModel.state.test {

                // then
                val expected = OpenMailboxItemRequest(MailboxItemId(item.id), Conversation)
                assertEquals(expected, awaitItem().openItemEffect.consume())
            }
        }

    @Test
    fun `open item details actions generates a request to open Conversation details for a Message while in Conversation mode`() =
        runTest {

            // given
            val item = buildMailboxItem(Message)
            every { observeMailboxItemType() } returns flowOf(Conversation)

            // when
            mailboxViewModel.submit(Action.OpenItemDetails(item))
            mailboxViewModel.state.test {

                // then
                val expected = OpenMailboxItemRequest(MailboxItemId(item.id), Conversation)
                assertEquals(expected, awaitItem().openItemEffect.consume())
            }
        }

    @Test
    @Ignore("Failing to catch the exception")
    fun `open item details actions throws an exception when trying to open details for a Conversation while in Message mode`() =
        runTest {

            // given
            val item = buildMailboxItem(Conversation)
            every { observeMailboxItemType() } returns flowOf(Message)

            // when - then
            val expectedMessage = "Item type is $Conversation, but mailbox type is $Message"
            assertFailsWith<IllegalStateException>(expectedMessage) {
                mailboxViewModel.submit(Action.OpenItemDetails(item))
            }
        }

    private fun givenUserNotLoggedIn() {
        every { accountManager.getPrimaryUserId() } returns userIdFlow
    }

    private companion object TestData {

        fun buildMailboxItem(type: MailboxItemType) = MailboxItem(
            type = type,
            id = "id",
            userId = userId,
            time = 0,
            size = 0,
            order = 0,
            read = false,
            conversationId = ConversationId("id"),
            labels = emptyList(),
            subject = "subject",
            senders = emptyList(),
            recipients = emptyList(),
        )
    }
}
