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

import androidx.paging.PagingData
import app.cash.turbine.test
import arrow.core.Either
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailcontact.domain.usecase.GetContacts
import ch.protonmail.android.maillabel.domain.SelectedMailLabelId
import ch.protonmail.android.maillabel.domain.model.MailLabel
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.maillabel.domain.model.MailLabelId.System.Archive
import ch.protonmail.android.maillabel.domain.model.MailLabelId.System.Sent
import ch.protonmail.android.maillabel.domain.model.MailLabels
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.maillabel.domain.usecase.ObserveMailLabels
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemId
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemType.Conversation
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemType.Message
import ch.protonmail.android.mailmailbox.domain.model.OpenMailboxItemRequest
import ch.protonmail.android.mailmailbox.domain.model.UnreadCounter
import ch.protonmail.android.mailmailbox.domain.usecase.MarkAsStaleMailboxItems
import ch.protonmail.android.mailmailbox.domain.usecase.ObserveCurrentViewMode
import ch.protonmail.android.mailmailbox.domain.usecase.ObserveUnreadCounters
import ch.protonmail.android.mailmailbox.presentation.helper.MailboxAsyncPagingDataDiffer
import ch.protonmail.android.mailmailbox.presentation.mailbox.MailboxPagerFactory
import ch.protonmail.android.mailmailbox.presentation.mailbox.MailboxViewModel
import ch.protonmail.android.mailmailbox.presentation.mailbox.MailboxViewModel.Action
import ch.protonmail.android.mailmailbox.presentation.mailbox.mapper.MailboxItemUiModelMapper
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxListState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxTopAppBarState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.UnreadFilterState
import ch.protonmail.android.testdata.contact.ContactTestData
import ch.protonmail.android.testdata.label.LabelTestData
import ch.protonmail.android.testdata.mailbox.MailboxItemUiModelTestData.buildMailboxUiModelItem
import ch.protonmail.android.testdata.mailbox.MailboxItemUiModelTestData.readMailboxItemUiModel
import ch.protonmail.android.testdata.mailbox.MailboxItemUiModelTestData.unreadMailboxItemUiModel
import ch.protonmail.android.testdata.mailbox.MailboxTestData.readMailboxItem
import ch.protonmail.android.testdata.mailbox.MailboxTestData.unreadMailboxItem
import ch.protonmail.android.testdata.mailbox.UnreadCountersTestData
import ch.protonmail.android.testdata.maillabel.MailLabelTestData
import ch.protonmail.android.testdata.user.UserIdTestData.userId
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import me.proton.core.mailsettings.domain.entity.ViewMode.ConversationGrouping
import me.proton.core.mailsettings.domain.entity.ViewMode.NoConversationGrouping
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class MailboxViewModelTest {

    private val observePrimaryUserId = mockk<ObservePrimaryUserId> {
        every { this@mockk.invoke() } returns flowOf(userId)
    }

    private val selectedMailLabelId = mockk<SelectedMailLabelId> {
        every { this@mockk.flow } returns MutableStateFlow<MailLabelId>(Archive)
    }

    private val observeMailLabels = mockk<ObserveMailLabels> {
        every { this@mockk.invoke(any()) } returns MutableStateFlow(
            MailLabels(
                systemLabels = LabelTestData.systemLabels,
                folders = emptyList(),
                labels = listOf(MailLabelTestData.customLabel)
            )
        )
    }

    private val markAsStaleMailboxItems = mockk<MarkAsStaleMailboxItems> {
        coEvery { this@mockk(any(), any(), any()) } returns Unit
    }

    private val observeCurrentViewMode = mockk<ObserveCurrentViewMode> {
        coEvery { this@mockk(userId = any()) } returns flowOf(NoConversationGrouping)
        coEvery { this@mockk(any(), any()) } returns flowOf(NoConversationGrouping)
    }

    private val observeUnreadCounters = mockk<ObserveUnreadCounters> {
        coEvery { this@mockk(userId = any()) } returns flowOf(UnreadCountersTestData.systemUnreadCounters)
    }

    private val getContacts = mockk<GetContacts> {
        coEvery { this@mockk.invoke(userId) } returns Either.Right(ContactTestData.contacts)
    }

    private val pagerFactory = mockk<MailboxPagerFactory>()

    private val mailboxItemMapper = mockk<MailboxItemUiModelMapper>()

    private val mailboxViewModel by lazy {
        MailboxViewModel(
            markAsStaleMailboxItems = markAsStaleMailboxItems,
            mailboxPagerFactory = pagerFactory,
            observeCurrentViewMode = observeCurrentViewMode,
            observePrimaryUserId = observePrimaryUserId,
            observeMailLabels = observeMailLabels,
            selectedMailLabelId = selectedMailLabelId,
            observeUnreadCounters = observeUnreadCounters,
            mailboxItemMapper = mailboxItemMapper,
            getContacts = getContacts
        )
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @Test
    fun `emits initial mailbox state when initialized`() = runTest {
        // Given
        coEvery { observeUnreadCounters(userId = any()) } returns emptyFlow()
        coEvery { observeMailLabels(userId = any()) } returns emptyFlow()

        // When
        mailboxViewModel.state.test {

            // Then
            val actual = awaitItem()
            val expected = MailboxState(
                mailboxListState = MailboxListState.Loading,
                topAppBarState = MailboxTopAppBarState.Loading,
                unreadFilterState = UnreadFilterState.Loading
            )

            assertEquals(expected, actual)

            verify { pagerFactory wasNot Called }
        }
    }

    @Test
    fun `emits default top app bar state as soon as the label name is available`() = runTest {
        mailboxViewModel.state.test {

            // Then
            val expected = MailboxTopAppBarState.Data.DefaultMode(MailLabel.System(Archive))
            val actual = awaitItem()
            assertEquals(expected, actual.topAppBarState)
        }
    }

    @Test
    fun `emits mailbox state with current mail label`() = runTest {
        mailboxViewModel.state.test {

            // Then
            val mailboxListState = assertIs<MailboxListState.Data>(awaitItem().mailboxListState)
            assertEquals(MailLabel.System(Archive), mailboxListState.currentMailLabel)
        }
    }

    @Test
    fun `when selection mode is not open and enter selection mode is submitted, selection mode is opened`() = runTest {
        mailboxViewModel.state.test {

            // Given
            awaitItem() // First emission for selected user

            // When
            mailboxViewModel.submit(Action.EnterSelectionMode)

            // Then
            val expected = MailboxTopAppBarState.Data.SelectionMode(MailLabel.System(Archive), selectedCount = 0)
            val actual = awaitItem()
            assertEquals(expected, actual.topAppBarState)
        }
    }

    @Test
    fun `when selection mode is open and exit selection mode is submitted, selection mode is closed`() = runTest {
        // Given
        mailboxViewModel.state.test {

            awaitItem() // First emission for selected user

            mailboxViewModel.submit(Action.EnterSelectionMode)
            awaitItem() // Selection Mode has been opened

            // When
            mailboxViewModel.submit(Action.ExitSelectionMode)

            // Then
            val expected = MailboxTopAppBarState.Data.DefaultMode(MailLabel.System(Archive))
            val actual = awaitItem()
            assertEquals(expected, actual.topAppBarState)
        }
    }

    @Test
    fun `emits mailbox state with current location`() = runTest {

        mailboxViewModel.state.test {

            // Then
            val mailboxListState = assertIs<MailboxListState.Data>(awaitItem().mailboxListState)
            assertEquals(Archive.toMailLabel(), mailboxListState.currentMailLabel)
        }
    }

    @Test
    fun `emits mailbox state with unread filter state based on current location`() = runTest {
        // Given
        val currentLocation = MutableStateFlow<MailLabelId>(Sent)
        every { selectedMailLabelId.flow } returns currentLocation

        mailboxViewModel.state.test {
            // Then
            val firstItem = assertIs<UnreadFilterState.Data>(awaitItem().unreadFilterState)
            assertEquals(3, firstItem.numUnread)
            // When
            currentLocation.emit(Archive)
            // Then
            val secondItem = assertIs<UnreadFilterState.Data>(awaitItem().unreadFilterState)
            assertEquals(5, secondItem.numUnread)
        }
    }

    @Test
    fun `emits mailbox state with loading unread filter state when no unread counters for current location`() =
        runTest {
            // Given
            val currentLocation = MutableStateFlow<MailLabelId>(MailLabelId.System.Inbox)
            every { selectedMailLabelId.flow } returns currentLocation

            mailboxViewModel.state.test {
                // Then
                val firstItem = assertIs<UnreadFilterState.Data>(awaitItem().unreadFilterState)
                assertEquals(1, firstItem.numUnread)
                // When
                currentLocation.emit(MailLabelTestData.customLabel.id)
                // Then
                assertIs<UnreadFilterState.Loading>(awaitItem().unreadFilterState)
            }
        }

    @Test
    fun `top bar state is updated when current location changes`() = runTest {
        // Given
        val currentLocationFlow = MutableStateFlow<MailLabelId>(MailLabelId.System.Inbox)
        every { selectedMailLabelId.flow } returns currentLocationFlow

        mailboxViewModel.state.test {
            // Then
            val firstUnreadFilterState = assertIs<MailboxTopAppBarState.Data>(awaitItem().topAppBarState)
            assertEquals(MailLabel.System(MailLabelId.System.Inbox), firstUnreadFilterState.currentMailLabel)

            // When
            currentLocationFlow.emit(MailLabelId.System.Starred)

            // Then
            val secondUnreadFilterState = assertIs<MailboxTopAppBarState.Data>(awaitItem().topAppBarState)
            assertEquals(MailLabel.System(MailLabelId.System.Starred), secondUnreadFilterState.currentMailLabel)
        }
    }

    @Test
    fun `mailbox items for the current location are requested when location changes`() = runTest {
        // Given
        val currentLocationFlow = MutableStateFlow<MailLabelId>(MailLabelId.System.Inbox)
        every { selectedMailLabelId.flow } returns currentLocationFlow
        every { pagerFactory.create(any(), any(), any(), any()) } returns mockk mockPager@{
            every { this@mockPager.flow } returns flowOf(PagingData.from(listOf(unreadMailboxItem)))
        }

        mailboxViewModel.items.test {
            // Then
            awaitItem()
            verify { pagerFactory.create(listOf(userId), MailLabelId.System.Inbox, false, Message) }

            // When
            currentLocationFlow.emit(MailLabelId.System.Spam)

            // Then
            awaitItem()
            verify { pagerFactory.create(listOf(userId), MailLabelId.System.Spam, false, Message) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `mailbox items are mapped to mailbox item ui models`() = runTest {
        // Given
        every {
            mailboxItemMapper.toUiModel(unreadMailboxItem, ContactTestData.contacts)
        } returns unreadMailboxItemUiModel
        every { mailboxItemMapper.toUiModel(readMailboxItem, ContactTestData.contacts) } returns readMailboxItemUiModel
        every { pagerFactory.create(any(), any(), any(), any()) } returns mockk {
            val pagingData = PagingData.from(listOf(unreadMailboxItem, readMailboxItem))
            every { this@mockk.flow } returns flowOf(pagingData)
        }
        val differ = MailboxAsyncPagingDataDiffer.differ

        // When
        mailboxViewModel.items.test {
            // Then
            val pagingData = awaitItem()
            differ.submitData(pagingData)

            val expected = listOf(unreadMailboxItemUiModel, readMailboxItemUiModel)
            assertEquals(expected, differ.snapshot().items)
        }
    }

    @Test
    fun `user contacts are used to map mailbox items to ui models`() = runTest {
        // Given
        every { pagerFactory.create(any(), any(), any(), any()) } returns mockk {
            val pagingData = PagingData.from(listOf(unreadMailboxItem, readMailboxItem))
            every { this@mockk.flow } returns flowOf(pagingData)
        }
        val differ = MailboxAsyncPagingDataDiffer.differ
        // When
        mailboxViewModel.items.test {
            // Then
            val pagingData = awaitItem()
            differ.submitData(pagingData)

            verify { mailboxItemMapper.toUiModel(any(), ContactTestData.contacts) }
        }
    }

    @Test
    fun `on refresh call mark as stale mailbox items`() = runTest {

        // When
        mailboxViewModel.submit(Action.Refresh)

        // Then
        coVerify { markAsStaleMailboxItems.invoke(listOf(userId), Message, Archive.labelId) }
    }

    @Test
    fun `open item details action generates a request to open Message details for a Message while in Message mode`() =
        runTest {

            // Given
            val item = buildMailboxUiModelItem("id", Message)
            every { observeCurrentViewMode(userId) } returns flowOf(NoConversationGrouping)

            // When
            mailboxViewModel.submit(Action.OpenItemDetails(item))
            mailboxViewModel.state.test {

                // Then
                val expected = OpenMailboxItemRequest(MailboxItemId(item.id), Message)
                val mailboxListState = assertIs<MailboxListState.Data>(awaitItem().mailboxListState)
                assertEquals(expected, mailboxListState.openItemEffect.consume())
            }
        }

    @Test
    fun `open item action generates a request to open Conversation for a Conversation while in Conversation mode`() =
        runTest {

            // Given
            val item = buildMailboxUiModelItem("id", Conversation)
            every { observeCurrentViewMode(userId) } returns flowOf(ConversationGrouping)

            // When
            mailboxViewModel.submit(Action.OpenItemDetails(item))
            mailboxViewModel.state.test {

                // Then
                val expected = OpenMailboxItemRequest(MailboxItemId(item.id), Conversation)
                val mailboxListState = assertIs<MailboxListState.Data>(awaitItem().mailboxListState)
                assertEquals(expected, mailboxListState.openItemEffect.consume())
            }
        }

    @Test
    fun `open item action generates a request to open Conversation for a Message while in Conversation mode`() =
        runTest {

            // Given
            val item = buildMailboxUiModelItem("id", Message)
            every { observeCurrentViewMode(userId = any()) } returns flowOf(ConversationGrouping)

            // When
            mailboxViewModel.submit(Action.OpenItemDetails(item))
            mailboxViewModel.state.test {

                // Then
                val expected = OpenMailboxItemRequest(MailboxItemId(item.id), Conversation)
                val mailboxListState = assertIs<MailboxListState.Data>(awaitItem().mailboxListState)
                assertEquals(expected, mailboxListState.openItemEffect.consume())
            }
        }

    @Test
    fun `enable unread filter action emits a new state with unread filter state enabled`() = runTest {
        // When
        mailboxViewModel.submit(Action.EnableUnreadFilter)
        mailboxViewModel.state.test {

            // Then
            val expected = UnreadFilterState.Data(5, true)
            val actual = assertIs<UnreadFilterState.Data>(awaitItem().unreadFilterState)
            assertEquals(expected, actual)
        }
    }

    @Test
    fun `disable unread filter action emits a new state with unread filter state disabled`() = runTest {
        mailboxViewModel.state.test {
            // When
            mailboxViewModel.submit(Action.DisableUnreadFilter)

            // Then
            val expected = UnreadFilterState.Data(5, false)
            val actual = assertIs<UnreadFilterState.Data>(awaitItem().unreadFilterState)
            assertEquals(expected, actual)
        }
    }

    @Test
    fun `mailbox is scrolled to top when location changes`() = runTest {
        // Given
        val currentLocationFlow = MutableStateFlow<MailLabelId>(MailLabelId.System.Inbox)
        every { selectedMailLabelId.flow } returns currentLocationFlow

        mailboxViewModel.state.test {
            // Then
            val initialState = assertIs<MailboxListState.Data>(awaitItem().mailboxListState)
            assertEquals(MailLabelId.System.Inbox, initialState.scrollToMailboxTop.consume())

            // When
            currentLocationFlow.emit(MailLabelId.System.Starred)

            // Then
            val actual = assertIs<MailboxListState.Data>(awaitItem().mailboxListState)
            assertEquals(MailLabelId.System.Starred, actual.scrollToMailboxTop.consume())
        }
    }

    @Test
    fun `mailbox is not scrolled to top when something changes but location didn't change`() = runTest {
        // Given
        every { selectedMailLabelId.flow } returns MutableStateFlow<MailLabelId>(MailLabelId.System.AllMail)
        val unreadCountersFlow = MutableStateFlow(UnreadCountersTestData.systemUnreadCounters)
        coEvery { observeUnreadCounters(userId = any()) } returns unreadCountersFlow

        mailboxViewModel.state.test {
            // Then
            val initialState = assertIs<MailboxListState.Data>(awaitItem().mailboxListState)
            assertEquals(MailLabelId.System.AllMail, initialState.scrollToMailboxTop.consume())

            // When
            unreadCountersFlow.emit(listOf(UnreadCounter(SystemLabelId.AllMail.labelId, 1)))

            // Then
            val actual = assertIs<MailboxListState.Data>(awaitItem().mailboxListState)
            assertEquals(null, actual.scrollToMailboxTop.consume())
        }
    }
}
