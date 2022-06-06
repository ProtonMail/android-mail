/*
 * Copyright (c) 2022 Proton Technologies AG
 * This file is part of Proton Technologies AG and Proton Mail.
 *
 * Proton Mail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Mail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Mail. If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.mailmailbox.presentation

import app.cash.turbine.test
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailconversation.domain.entity.ConversationId
import ch.protonmail.android.maillabel.domain.SelectedMailLabelId
import ch.protonmail.android.maillabel.domain.model.MailLabel
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.maillabel.domain.model.MailLabelId.System.Archive
import ch.protonmail.android.maillabel.domain.model.MailLabels
import ch.protonmail.android.maillabel.domain.usecase.ObserveMailLabels
import ch.protonmail.android.mailmailbox.domain.model.MailboxItem
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemId
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemType
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemType.Conversation
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemType.Message
import ch.protonmail.android.mailmailbox.domain.model.OpenMailboxItemRequest
import ch.protonmail.android.mailmailbox.domain.usecase.MarkAsStaleMailboxItems
import ch.protonmail.android.mailmailbox.domain.usecase.ObserveCurrentViewMode
import ch.protonmail.android.mailmailbox.presentation.MailboxState.Data
import ch.protonmail.android.mailmailbox.presentation.MailboxState.Loading
import ch.protonmail.android.mailmailbox.presentation.MailboxViewModel.Action
import ch.protonmail.android.mailmailbox.presentation.model.MailboxTopAppBarState
import ch.protonmail.android.mailmailbox.presentation.paging.MailboxItemPagingSourceFactory
import ch.protonmail.android.testdata.user.UserIdTestData
import ch.protonmail.android.testdata.user.UserIdTestData.userId
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import me.proton.core.domain.entity.UserId
import me.proton.core.mailsettings.domain.entity.ViewMode.ConversationGrouping
import me.proton.core.mailsettings.domain.entity.ViewMode.NoConversationGrouping
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class MailboxViewModelTest {

    private val userId = UserIdTestData.userId
    private val userIdFlow = MutableStateFlow<UserId?>(null)
    private val observePrimaryUserId = mockk<ObservePrimaryUserId> {
        every { this@mockk.invoke() } returns userIdFlow
    }

    private val selectedMailLabelId = mockk<SelectedMailLabelId> {
        every { this@mockk.flow } returns MutableStateFlow<MailLabelId>(Archive)
    }

    private val mailLabels = MutableStateFlow(
        MailLabels(
            systems = listOf(MailLabel.System(Archive)),
            folders = emptyList(),
            labels = emptyList(),
        )
    )
    private val observeMailLabels = mockk<ObserveMailLabels> {
        every { this@mockk.invoke(any()) } returns mailLabels
    }

    private val markAsStaleMailboxItems = mockk<MarkAsStaleMailboxItems> {
        coEvery { this@mockk(any(), any(), any()) } returns Unit
    }

    private val observeCurrentViewMode = mockk<ObserveCurrentViewMode> {
        coEvery { this@mockk(userId = any()) } returns flowOf(NoConversationGrouping)
        coEvery { this@mockk(any(), any()) } returns flowOf(NoConversationGrouping)
    }

    private val pagingSourceFactory = mockk<MailboxItemPagingSourceFactory>(relaxed = true)

    private val mailboxViewModel by lazy {
        MailboxViewModel(
            markAsStaleMailboxItems,
            pagingSourceFactory,
            observeCurrentViewMode,
            observePrimaryUserId,
            observeMailLabels,
            selectedMailLabelId,
        )
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @Test
    fun `emits initial mailbox state when initialized`() = runTest {
        // Given
        givenUserNotLoggedIn()

        // When
        mailboxViewModel.state.test {

            // Then
            val actual = awaitItem()
            val expected = Loading

            assertEquals(expected, actual)

            verify { pagingSourceFactory wasNot Called }
        }
    }

    @Test
    fun `emits default TopAppBar state as soon as the label name is available`() = runTest {
        mailboxViewModel.state.test {
            assertEquals(MailboxState.Loading, awaitItem())

            // Given
            givenUserLoggedIn()

            // Then
            val expected = MailboxTopAppBarState.Data.DefaultMode(MailLabel.System(Archive))
            val actual = awaitItem()
            assertIs<Data>(actual)
            assertEquals(expected, actual.topAppBar)
        }
    }

    @Test
    fun `emits mailbox state with current mail label`() = runTest {
        mailboxViewModel.state.test {
            assertEquals(MailboxState.Loading, awaitItem())

            // Given
            givenUserLoggedIn()

            // Then
            assertEquals(MailLabel.System(Archive), awaitItem().currentMailLabel)
        }
    }

    @Test
    fun `when selection mode is not open and the right Action is submitted, selection mode is opened`() = runTest {
        mailboxViewModel.state.test {
            assertEquals(MailboxState.Loading, awaitItem())

            // Given
            givenUserLoggedIn()
            awaitItem() // First emission for selected user

            // When
            mailboxViewModel.submit(Action.EnterSelectionMode)

            // Then
            val expected = MailboxTopAppBarState.Data.SelectionMode(MailLabel.System(Archive), selectedCount = 0)
            val actual = awaitItem()
            assertIs<Data>(actual)
            assertEquals(expected, actual.topAppBar)
        }
    }

    @Test
    fun `when selection mode is open and the right Action is submitted, selection mode is closed`() = runTest {
        // Given
        mailboxViewModel.state.test {
            assertEquals(MailboxState.Loading, awaitItem())

            // Given
            givenUserLoggedIn()
            awaitItem() // First emission for selected user

            mailboxViewModel.submit(Action.EnterSelectionMode)
            awaitItem() // Selection Mode has been opened

            // When
            mailboxViewModel.submit(Action.ExitSelectionMode)

            // Then
            val expected = MailboxTopAppBarState.Data.DefaultMode(MailLabel.System(Archive))
            val actual = awaitItem()
            assertIs<Data>(actual)
            assertEquals(expected, actual.topAppBar)
        }
    }

    @Test
    fun `emits mailbox state with current location`() = runTest {
        mailboxViewModel.state.test {

            // Then
            val actual = awaitItem()
            assertIs<Data>(actual)
            assertEquals(Archive, actual.selectedLocation)
        }
    }

    @Test
    fun `emits mailbox items`() = runTest {
        mailboxViewModel.items.test {
            awaitItem() // Initial item

            // Given
            givenUserLoggedIn()

            // Then
            awaitItem()
            verify { pagingSourceFactory.create(listOf(userId), Archive, Message) }
        }
    }

    @Test
    fun `onRefresh call markAsStaleMailboxItems`() = runTest {
        // Given
        givenUserLoggedIn()

        // When
        mailboxViewModel.submit(Action.Refresh)

        // Then
        coVerify { markAsStaleMailboxItems.invoke(listOf(userId), Message, Archive.labelId) }
    }

    @Test
    fun `open item details actions generates a request to open Message details for a Message while in Message mode`() =
        runTest {

            // Given
            givenUserLoggedIn()
            val item = buildMailboxItem(Message)
            every { observeCurrentViewMode(userId) } returns flowOf(NoConversationGrouping)

            // When
            mailboxViewModel.submit(Action.OpenItemDetails(item))
            mailboxViewModel.state.test {

                // Then
                val expected = OpenMailboxItemRequest(MailboxItemId(item.id), Message)
                val actual = awaitItem()
                assertIs<Data>(actual)
                assertEquals(expected, actual.openItemEffect.consume())
            }
        }

    @Test
    fun `open item details actions generates a request to open Conversation details for a Conversation while in Conversation mode`() =
        runTest {

            // Given
            givenUserLoggedIn()
            val item = buildMailboxItem(Conversation)
            every { observeCurrentViewMode(userId) } returns flowOf(ConversationGrouping)

            // When
            mailboxViewModel.submit(Action.OpenItemDetails(item))
            mailboxViewModel.state.test {

                // Then
                val expected = OpenMailboxItemRequest(MailboxItemId(item.id), Conversation)
                val actual = awaitItem()
                assertIs<Data>(actual)
                assertEquals(expected, actual.openItemEffect.consume())
            }
        }

    @Test
    fun `open item details actions generates a request to open Conversation details for a Message while in Conversation mode`() =
        runTest {

            // Given
            givenUserLoggedIn()
            val item = buildMailboxItem(Message)
            every { observeCurrentViewMode(userId) } returns flowOf(ConversationGrouping)

            // When
            mailboxViewModel.submit(Action.OpenItemDetails(item))
            mailboxViewModel.state.test {

                // Then
                val expected = OpenMailboxItemRequest(MailboxItemId(item.id), Conversation)
                val actual = awaitItem()
                assertIs<Data>(actual)
                assertEquals(expected, actual.openItemEffect.consume())
            }
        }

    private suspend fun givenUserLoggedIn() {
        userIdFlow.emit(userId)
    }

    private suspend fun givenUserNotLoggedIn() {
        userIdFlow.emit(null)
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
