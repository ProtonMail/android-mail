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
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcontact.domain.usecase.GetContacts
import ch.protonmail.android.maillabel.domain.SelectedMailLabelId
import ch.protonmail.android.maillabel.domain.model.MailLabel
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.maillabel.domain.model.MailLabelId.System.Archive
import ch.protonmail.android.maillabel.domain.model.MailLabelId.System.Sent
import ch.protonmail.android.maillabel.domain.model.MailLabels
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.maillabel.domain.usecase.ObserveMailLabels
import ch.protonmail.android.maillabel.presentation.text
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemId
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemType.Conversation
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemType.Message
import ch.protonmail.android.mailmailbox.domain.model.OpenMailboxItemRequest
import ch.protonmail.android.mailmailbox.domain.model.UnreadCounter
import ch.protonmail.android.mailmailbox.domain.usecase.MarkAsStaleMailboxItems
import ch.protonmail.android.mailmailbox.domain.usecase.ObserveCurrentViewMode
import ch.protonmail.android.mailmailbox.domain.usecase.ObserveUnreadCounters
import ch.protonmail.android.mailmailbox.presentation.helper.MailboxAsyncPagingDataDiffer
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxEvent
import ch.protonmail.android.mailmailbox.presentation.mailbox.MailboxPagerFactory
import ch.protonmail.android.mailmailbox.presentation.mailbox.MailboxViewModel
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxViewAction
import ch.protonmail.android.mailmailbox.presentation.mailbox.mapper.MailboxItemUiModelMapper
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxListState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxOperation
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxTopAppBarState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.UnreadFilterState
import ch.protonmail.android.mailmailbox.presentation.mailbox.reducer.MailboxListReducer
import ch.protonmail.android.mailmailbox.presentation.mailbox.reducer.MailboxTopAppBarReducer
import ch.protonmail.android.mailmailbox.presentation.mailbox.reducer.MailboxUnreadFilterReducer
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
                labels = listOf(MailLabelTestData.customLabelOne)
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

    private val mailboxTopAppBarReducer = mockk<MailboxTopAppBarReducer> {
        every { newStateFrom(any(), any()) } returns MailboxTopAppBarState.Loading
    }

    private val unreadFilterReducer = mockk<MailboxUnreadFilterReducer> {
        every { newStateFrom(any(), any()) } answers {
            val operation = secondArg<MailboxOperation.AffectingUnreadFilter>()
            if (operation is MailboxEvent.SelectedLabelCountChanged) {
                UnreadFilterState.Data(operation.selectedLabelCount, false)
            } else {
                UnreadFilterState.Loading
            }
        }
    }

    private val mailboxListReducer = mockk<MailboxListReducer> {
        every { newStateFrom(any(), any()) } returns MailboxListState.Loading
    }

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
            getContacts = getContacts,
            mailboxTopAppBarReducer = mailboxTopAppBarReducer,
            unreadFilterReducer = unreadFilterReducer,
            mailboxListReducer = mailboxListReducer
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
        // Given
        val expectedMailLabel = MailLabel.System(Archive)
        val expectedCount = UnreadCountersTestData.labelToCounterMap[SystemLabelId.Archive.labelId]!!
        val expectedState = MailboxTopAppBarState.Data.DefaultMode(expectedMailLabel.text())
        every {
            mailboxTopAppBarReducer.newStateFrom(
                any(),
                MailboxEvent.NewLabelSelected(expectedMailLabel, expectedCount)
            )
        } returns expectedState

        mailboxViewModel.state.test {

            // Then
            val actual = awaitItem()
            assertEquals(expectedState, actual.topAppBarState)
        }
    }

    @Test
    fun `emits mailbox state with current mail label`() = runTest {
        // Given
        val expectedMailLabel = MailLabel.System(Archive)
        val expectedCount = UnreadCountersTestData.labelToCounterMap[SystemLabelId.Archive.labelId]!!
        every {
            mailboxListReducer.newStateFrom(
                any(),
                MailboxEvent.NewLabelSelected(expectedMailLabel, expectedCount)
            )
        } returns MailboxListState.Data(
            currentMailLabel = expectedMailLabel,
            openItemEffect = Effect.empty(),
            scrollToMailboxTop = Effect.of(expectedMailLabel.id)
        )

        mailboxViewModel.state.test {

            // Then
            val mailboxListState = assertIs<MailboxListState.Data>(awaitItem().mailboxListState)
            assertEquals(MailLabel.System(Archive), mailboxListState.currentMailLabel)
        }
    }

    @Test
    fun `when selection mode is not open and enter selection mode is submitted, selection mode is opened`() = runTest {
        // Given
        val expectedState = MailboxTopAppBarState.Data.SelectionMode(
            MailLabel.System(Archive).text(),
            selectedCount = 0
        )
        every {
            mailboxTopAppBarReducer.newStateFrom(any(), MailboxViewAction.EnterSelectionMode)
        } returns expectedState

        mailboxViewModel.state.test {

            // Given
            awaitItem() // First emission for selected user

            // When
            mailboxViewModel.submit(MailboxViewAction.EnterSelectionMode)

            // Then
            val actual = awaitItem()
            assertEquals(expectedState, actual.topAppBarState)
        }
    }

    @Test
    fun `when selection mode is open and exit selection mode is submitted, selection mode is closed`() = runTest {
        // Given
        val expectedState = MailboxTopAppBarState.Data.DefaultMode(MailLabel.System(Archive).text())
        every { mailboxTopAppBarReducer.newStateFrom(any(), MailboxViewAction.ExitSelectionMode) } returns expectedState

        mailboxViewModel.state.test {

            // When
            mailboxViewModel.submit(MailboxViewAction.EnterSelectionMode)
            awaitItem() // Selection Mode has been opened

            mailboxViewModel.submit(MailboxViewAction.ExitSelectionMode)

            // Then
            val actual = awaitItem()
            assertEquals(expectedState, actual.topAppBarState)
        }
    }

    @Test
    fun `emits mailbox state with unread filter state based on current location`() = runTest {
        // Given
        val currentLocation = MutableStateFlow<MailLabelId>(Sent)
        val sentCount = UnreadCountersTestData.labelToCounterMap[Sent.labelId]!!
        val archiveCount = UnreadCountersTestData.labelToCounterMap[Archive.labelId]!!
        every { selectedMailLabelId.flow } returns currentLocation
        every {
            unreadFilterReducer.newStateFrom(
                any(),
                MailboxEvent.NewLabelSelected(Sent.toMailLabel(), sentCount)
            )
        } returns UnreadFilterState.Data(sentCount, false)
        every {
            unreadFilterReducer.newStateFrom(
                any(),
                MailboxEvent.NewLabelSelected(Archive.toMailLabel(), archiveCount)
            )
        } returns UnreadFilterState.Data(archiveCount, false)

        mailboxViewModel.state.test {
            // Then
            val firstItem = assertIs<UnreadFilterState.Data>(awaitItem().unreadFilterState)
            assertEquals(sentCount, firstItem.numUnread)
            // When
            currentLocation.emit(Archive)
            // Then
            val secondItem = assertIs<UnreadFilterState.Data>(awaitItem().unreadFilterState)
            assertEquals(archiveCount, secondItem.numUnread)
        }
    }

    @Test
    fun `emits mailbox state with loading unread filter state when no unread counters for current location`() =
        runTest {
            // Given
            val currentLocation = MutableStateFlow<MailLabelId>(MailLabelId.System.Inbox)
            every { selectedMailLabelId.flow } returns currentLocation
            every {
                unreadFilterReducer.newStateFrom(
                    any(),
                    MailboxEvent.NewLabelSelected(MailLabelTestData.customLabelOne, selectedLabelCount = null)
                )
            } returns UnreadFilterState.Loading

            mailboxViewModel.state.test {
                // Then
                awaitItem()
                // When
                currentLocation.emit(MailLabelTestData.customLabelOne.id)
                // Then
                assertIs<UnreadFilterState.Loading>(awaitItem().unreadFilterState)
            }
        }

    @Test
    fun `top bar state is updated when current location changes`() = runTest {
        // Given
        val currentLocationFlow = MutableStateFlow<MailLabelId>(MailLabelId.System.Inbox)
        val inboxLabel = MailLabel.System(MailLabelId.System.Inbox)
        val inboxCount = UnreadCountersTestData.labelToCounterMap[SystemLabelId.Inbox.labelId]!!
        val starredCount = UnreadCountersTestData.labelToCounterMap[SystemLabelId.Starred.labelId]!!
        val starredLabel = MailLabel.System(MailLabelId.System.Starred)
        every {
            mailboxTopAppBarReducer.newStateFrom(
                any(),
                MailboxEvent.NewLabelSelected(inboxLabel, inboxCount)
            )
        } returns MailboxTopAppBarState.Data.DefaultMode(inboxLabel.text())
        every {
            mailboxTopAppBarReducer.newStateFrom(
                any(),
                MailboxEvent.NewLabelSelected(starredLabel, starredCount)
            )
        } returns MailboxTopAppBarState.Data.DefaultMode(starredLabel.text())
        every { selectedMailLabelId.flow } returns currentLocationFlow

        mailboxViewModel.state.test {
            // Then
            val firstUnreadFilterState = assertIs<MailboxTopAppBarState.Data>(awaitItem().topAppBarState)
            assertEquals(inboxLabel.text(), firstUnreadFilterState.currentLabelName)

            // When
            currentLocationFlow.emit(MailLabelId.System.Starred)

            // Then
            val secondUnreadFilterState = assertIs<MailboxTopAppBarState.Data>(awaitItem().topAppBarState)
            assertEquals(MailLabel.System(MailLabelId.System.Starred).text(), secondUnreadFilterState.currentLabelName)
        }
    }

    @Test
    fun `does request to scroll to top on current location changes`() = runTest {
        // Given
        val currentLocationFlow = MutableStateFlow<MailLabelId>(MailLabelId.System.AllMail)
        every { selectedMailLabelId.flow } returns currentLocationFlow
        every {
            mailboxListReducer.newStateFrom(any(), any())
        } answers {
            val operation = secondArg<MailboxOperation.AffectingMailboxList>()
            if (operation is MailboxEvent.NewLabelSelected) {
                MailboxListState.Data(
                    currentMailLabel = operation.selectedLabel,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.of(operation.selectedLabel.id)
                )
            } else {
                MailboxListState.Loading
            }
        }

        mailboxViewModel.state.test {
            // Then
            val firstState = assertIs<MailboxListState.Data>(awaitItem().mailboxListState)
            assertEquals(MailLabelId.System.AllMail, firstState.scrollToMailboxTop.consume())

            // When
            currentLocationFlow.emit(MailLabelId.System.Trash)

            // Then
            val secondState = assertIs<MailboxListState.Data>(awaitItem().mailboxListState)
            assertEquals(MailLabelId.System.Trash, secondState.scrollToMailboxTop.consume())

            // When
            currentLocationFlow.emit(MailLabelId.System.AllMail)

            // Then
            val thirdState = assertIs<MailboxListState.Data>(awaitItem().mailboxListState)
            assertEquals(MailLabelId.System.AllMail, thirdState.scrollToMailboxTop.consume())
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
        every {
            mailboxListReducer.newStateFrom(
                any(),
                MailboxEvent.NewLabelSelected(
                    MailLabelId.System.Spam.toMailLabel(),
                    UnreadCountersTestData.labelToCounterMap[MailLabelId.System.Spam.labelId]!!
                )
            )
        } returns MailboxListState.Data(
            currentMailLabel = MailLabel.System(MailLabelId.System.Spam),
            openItemEffect = Effect.empty(),
            scrollToMailboxTop = Effect.of(MailLabelId.System.Spam.toMailLabel().id)
        )

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
        mailboxViewModel.submit(MailboxViewAction.Refresh)

        // Then
        coVerify { markAsStaleMailboxItems.invoke(listOf(userId), Message, Archive.labelId) }
    }

    @Test
    fun `open item details action generates a request to open Message details for a Message while in Message mode`() =
        runTest {

            // Given
            val item = buildMailboxUiModelItem("id", Message)
            val expectedOpenItemRequest = OpenMailboxItemRequest(MailboxItemId(item.id), Message)
            every { observeCurrentViewMode(userId) } returns flowOf(NoConversationGrouping)
            every {
                mailboxListReducer.newStateFrom(
                    any(),
                    MailboxEvent.ItemDetailsOpenedInViewMode(
                        item,
                        NoConversationGrouping
                    )
                )
            } returns MailboxListState.Data(
                currentMailLabel = MailLabel.System(Archive),
                openItemEffect = Effect.of(expectedOpenItemRequest),
                scrollToMailboxTop = Effect.empty()
            )

            // When
            mailboxViewModel.submit(MailboxViewAction.OpenItemDetails(item))
            mailboxViewModel.state.test {

                // Then
                val mailboxListState = assertIs<MailboxListState.Data>(awaitItem().mailboxListState)
                assertEquals(expectedOpenItemRequest, mailboxListState.openItemEffect.consume())
            }
        }

    @Test
    fun `open item action generates a request to open Conversation for a Conversation while in Conversation mode`() =
        runTest {

            // Given
            val item = buildMailboxUiModelItem("id", Conversation)
            val expectedOpenItemRequest = OpenMailboxItemRequest(MailboxItemId(item.id), Conversation)
            every { observeCurrentViewMode(userId) } returns flowOf(ConversationGrouping)
            every {
                mailboxListReducer.newStateFrom(
                    any(),
                    MailboxEvent.ItemDetailsOpenedInViewMode(
                        item,
                        ConversationGrouping
                    )
                )
            } returns MailboxListState.Data(
                currentMailLabel = MailLabel.System(Archive),
                openItemEffect = Effect.of(expectedOpenItemRequest),
                scrollToMailboxTop = Effect.empty()
            )

            // When
            mailboxViewModel.submit(MailboxViewAction.OpenItemDetails(item))
            mailboxViewModel.state.test {

                // Then
                val mailboxListState = assertIs<MailboxListState.Data>(awaitItem().mailboxListState)
                assertEquals(expectedOpenItemRequest, mailboxListState.openItemEffect.consume())
            }
        }

    @Test
    fun `open item action generates a request to open Conversation for a Message while in Conversation mode`() =
        runTest {

            // Given
            val item = buildMailboxUiModelItem("id", Message)
            every { observeCurrentViewMode(userId = any()) } returns flowOf(ConversationGrouping)
            val expectedOpenItemRequest = OpenMailboxItemRequest(MailboxItemId(item.id), Conversation)
            every {
                mailboxListReducer.newStateFrom(
                    any(),
                    MailboxEvent.ItemDetailsOpenedInViewMode(
                        item,
                        ConversationGrouping
                    )
                )
            } returns MailboxListState.Data(
                currentMailLabel = MailLabel.System(Archive),
                openItemEffect = Effect.of(expectedOpenItemRequest),
                scrollToMailboxTop = Effect.empty()
            )

            // When
            mailboxViewModel.submit(MailboxViewAction.OpenItemDetails(item))
            mailboxViewModel.state.test {

                // Then
                val mailboxListState = assertIs<MailboxListState.Data>(awaitItem().mailboxListState)
                assertEquals(expectedOpenItemRequest, mailboxListState.openItemEffect.consume())
            }
        }

    @Test
    fun `enable unread filter action emits a new state with unread filter state enabled`() = runTest {
        // Given
        val expectedState = UnreadFilterState.Data(5, true)
        every { unreadFilterReducer.newStateFrom(any(), MailboxViewAction.EnableUnreadFilter) } returns expectedState

        // When
        mailboxViewModel.submit(MailboxViewAction.EnableUnreadFilter)
        mailboxViewModel.state.test {

            // Then
            val actual = assertIs<UnreadFilterState.Data>(awaitItem().unreadFilterState)
            assertEquals(expectedState, actual)
        }
    }

    @Test
    fun `disable unread filter action emits a new state with unread filter state disabled`() = runTest {
        // Given
        val expectedState = UnreadFilterState.Data(5, false)
        every { unreadFilterReducer.newStateFrom(any(), MailboxViewAction.DisableUnreadFilter) } returns expectedState

        // When
        mailboxViewModel.submit(MailboxViewAction.DisableUnreadFilter)
        mailboxViewModel.state.test {

            // Then
            val actual = assertIs<UnreadFilterState.Data>(awaitItem().unreadFilterState)
            assertEquals(expectedState, actual)
        }
    }

    @Test
    fun `mailbox is not scrolled to top when something changes but location didn't change`() = runTest {
        // Given
        every { selectedMailLabelId.flow } returns MutableStateFlow<MailLabelId>(MailLabelId.System.AllMail)
        val unreadCountersFlow = MutableStateFlow(UnreadCountersTestData.systemUnreadCounters)
        val changedCount = 1
        coEvery { observeUnreadCounters(userId = any()) } returns unreadCountersFlow
        every {
            unreadFilterReducer.newStateFrom(
                any(),
                MailboxEvent.SelectedLabelCountChanged(changedCount)
            )
        } returns UnreadFilterState.Data(changedCount, false)
        every {
            mailboxListReducer.newStateFrom(
                any(),
                MailboxEvent.NewLabelSelected(
                    MailLabelId.System.AllMail.toMailLabel(),
                    UnreadCountersTestData.labelToCounterMap[MailLabelId.System.AllMail.labelId]!!
                )
            )
        } returns MailboxListState.Data(
            currentMailLabel = MailLabelId.System.AllMail.toMailLabel(),
            openItemEffect = Effect.empty(),
            scrollToMailboxTop = Effect.of(MailLabelId.System.AllMail.toMailLabel().id)
        )

        mailboxViewModel.state.test {
            // Then
            val initialState = assertIs<MailboxListState.Data>(awaitItem().mailboxListState)
            assertEquals(MailLabelId.System.AllMail, initialState.scrollToMailboxTop.consume())

            // When
            unreadCountersFlow.emit(listOf(UnreadCounter(SystemLabelId.AllMail.labelId, changedCount)))

            // Then
            val actual = assertIs<MailboxListState.Data>(awaitItem().mailboxListState)
            assertEquals(null, actual.scrollToMailboxTop.consume())
        }
    }
}
