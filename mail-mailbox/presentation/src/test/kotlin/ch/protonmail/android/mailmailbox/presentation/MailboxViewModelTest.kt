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

import android.util.Log
import androidx.paging.PagingData
import app.cash.turbine.test
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.MailFeatureId
import ch.protonmail.android.mailcommon.domain.model.Action
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailcommon.domain.model.PreferencesError
import ch.protonmail.android.mailcommon.domain.usecase.ObserveMailFeature
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.mapper.ActionUiModelMapper
import ch.protonmail.android.mailcommon.presentation.model.BottomBarEvent
import ch.protonmail.android.mailcommon.presentation.model.BottomBarState
import ch.protonmail.android.mailcommon.presentation.sample.ActionUiModelSample
import ch.protonmail.android.mailcontact.domain.usecase.GetContacts
import ch.protonmail.android.maillabel.domain.SelectedMailLabelId
import ch.protonmail.android.maillabel.domain.model.MailLabel
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.maillabel.domain.model.MailLabelId.System.Archive
import ch.protonmail.android.maillabel.domain.model.MailLabels
import ch.protonmail.android.maillabel.domain.usecase.ObserveMailLabels
import ch.protonmail.android.maillabel.presentation.text
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemId
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemType.Conversation
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemType.Message
import ch.protonmail.android.mailmailbox.domain.model.OpenMailboxItemRequest
import ch.protonmail.android.mailmailbox.domain.model.SpotlightPreference
import ch.protonmail.android.mailmailbox.domain.usecase.GetMailboxActions
import ch.protonmail.android.mailmailbox.domain.usecase.MarkConversationsAsRead
import ch.protonmail.android.mailmailbox.domain.usecase.MarkConversationsAsUnread
import ch.protonmail.android.mailmailbox.domain.usecase.MarkMessagesAsRead
import ch.protonmail.android.mailmailbox.domain.usecase.MarkMessagesAsUnread
import ch.protonmail.android.mailmailbox.domain.usecase.ObserveCurrentViewMode
import ch.protonmail.android.mailmailbox.domain.usecase.ObserveSpotlight
import ch.protonmail.android.mailmailbox.domain.usecase.ObserveUnreadCounters
import ch.protonmail.android.mailmailbox.domain.usecase.SaveSpotlight
import ch.protonmail.android.mailmailbox.presentation.helper.MailboxAsyncPagingDataDiffer
import ch.protonmail.android.mailmailbox.presentation.mailbox.MailboxViewModel
import ch.protonmail.android.mailmailbox.presentation.mailbox.mapper.MailboxItemUiModelMapper
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxEvent
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxItemUiModel
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxListState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxTopAppBarState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxViewAction
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.SpotlightState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.UnreadFilterState
import ch.protonmail.android.mailmailbox.presentation.mailbox.previewdata.MailboxStateSampleData
import ch.protonmail.android.mailmailbox.presentation.mailbox.reducer.MailboxReducer
import ch.protonmail.android.mailmailbox.presentation.paging.MailboxPagerFactory
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailsettings.domain.model.FolderColorSettings
import ch.protonmail.android.mailsettings.domain.usecase.ObserveFolderColorSettings
import ch.protonmail.android.testdata.contact.ContactTestData
import ch.protonmail.android.testdata.label.LabelTestData
import ch.protonmail.android.testdata.mailbox.MailboxItemUiModelTestData.buildMailboxUiModelItem
import ch.protonmail.android.testdata.mailbox.MailboxItemUiModelTestData.draftMailboxItemUiModel
import ch.protonmail.android.testdata.mailbox.MailboxItemUiModelTestData.readMailboxItemUiModel
import ch.protonmail.android.testdata.mailbox.MailboxItemUiModelTestData.unreadMailboxItemUiModel
import ch.protonmail.android.testdata.mailbox.MailboxItemUiModelTestData.unreadMailboxItemUiModelWithLabel
import ch.protonmail.android.testdata.mailbox.MailboxItemUiModelTestData.unreadMailboxItemUiModelWithLabelColored
import ch.protonmail.android.testdata.mailbox.MailboxTestData.readMailboxItem
import ch.protonmail.android.testdata.mailbox.MailboxTestData.unreadMailboxItem
import ch.protonmail.android.testdata.mailbox.MailboxTestData.unreadMailboxItemWithLabel
import ch.protonmail.android.testdata.mailbox.UnreadCountersTestData
import ch.protonmail.android.testdata.mailbox.UnreadCountersTestData.update
import ch.protonmail.android.testdata.maillabel.MailLabelTestData
import ch.protonmail.android.testdata.user.UserIdTestData.userId
import ch.protonmail.android.testdata.user.UserIdTestData.userId1
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifySequence
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import me.proton.core.domain.entity.UserId
import me.proton.core.featureflag.domain.entity.FeatureFlag
import me.proton.core.mailsettings.domain.entity.ViewMode
import me.proton.core.mailsettings.domain.entity.ViewMode.ConversationGrouping
import me.proton.core.mailsettings.domain.entity.ViewMode.NoConversationGrouping
import me.proton.core.test.kotlin.TestDispatcherProvider
import org.junit.Test
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.assertEquals
import ch.protonmail.android.mailconversation.domain.entity.Conversation as DomainConversation
import ch.protonmail.android.mailmessage.domain.model.Message as DomainMessage

class MailboxViewModelTest {

    private val defaultFolderColorSettings = FolderColorSettings()
    private val initialLocationMailLabelId = Archive
    private val actionUiModelMapper = ActionUiModelMapper()

    private val observePrimaryUserId = mockk<ObservePrimaryUserId> {
        every { this@mockk.invoke() } returns flowOf(userId)
    }

    private val selectedMailLabelId = mockk<SelectedMailLabelId> {
        every { this@mockk.flow } returns MutableStateFlow<MailLabelId>(initialLocationMailLabelId)
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

    private val observeCurrentViewMode = mockk<ObserveCurrentViewMode> {
        coEvery { this@mockk(userId = any()) } returns flowOf(NoConversationGrouping)
        coEvery { this@mockk(any(), any()) } returns flowOf(NoConversationGrouping)
    }

    private val observeUnreadCounters = mockk<ObserveUnreadCounters> {
        coEvery { this@mockk(userId = any()) } returns flowOf(UnreadCountersTestData.systemUnreadCounters)
    }
    private val observeFolderColorSettings = mockk<ObserveFolderColorSettings> {
        every { this@mockk.invoke(userId) } returns flowOf(defaultFolderColorSettings)
    }

    private val getContacts = mockk<GetContacts> {
        coEvery { this@mockk.invoke(userId) } returns Either.Right(ContactTestData.contacts)
    }

    private val pagerFactory = mockk<MailboxPagerFactory>()

    private val mailboxItemMapper = mockk<MailboxItemUiModelMapper>()

    private val mailboxReducer = mockk<MailboxReducer> {
        every { newStateFrom(any(), any()) } returns MailboxStateSampleData.Loading
    }

    private val observeMailFeature = mockk<ObserveMailFeature> {
        every { this@mockk(any(), any()) } returns flowOf()
    }

    private val observeMailboxActions = mockk<GetMailboxActions> {
        coEvery { this@mockk(any(), any()) } returns listOf(Action.Archive, Action.Trash).right()
    }

    private val observeSpotlight = mockk<ObserveSpotlight> {
        every { this@mockk() } returns flowOf(SpotlightPreference(display = false).right())
    }

    private val saveSpotlight: SaveSpotlight = mockk()

    private val markConversationsAsRead = mockk<MarkConversationsAsRead>()
    private val markConversationsAsUnread = mockk<MarkConversationsAsUnread>()
    private val markMessagesAsRead = mockk<MarkMessagesAsRead>()
    private val markMessagesAsUnread = mockk<MarkMessagesAsUnread>()

    private val mailboxViewModel by lazy {
        MailboxViewModel(
            mailboxPagerFactory = pagerFactory,
            observeCurrentViewMode = observeCurrentViewMode,
            observePrimaryUserId = observePrimaryUserId,
            observeMailLabels = observeMailLabels,
            selectedMailLabelId = selectedMailLabelId,
            observeUnreadCounters = observeUnreadCounters,
            observeFolderColorSettings = observeFolderColorSettings,
            getMailboxActions = observeMailboxActions,
            actionUiModelMapper = actionUiModelMapper,
            mailboxItemMapper = mailboxItemMapper,
            getContacts = getContacts,
            markConversationsAsRead = markConversationsAsRead,
            markConversationsAsUnread = markConversationsAsUnread,
            markMessagesAsRead = markMessagesAsRead,
            markMessagesAsUnread = markMessagesAsUnread,
            mailboxReducer = mailboxReducer,
            observeMailFeature = observeMailFeature,
            dispatchersProvider = TestDispatcherProvider(),
            observeSpotlight = observeSpotlight,
            saveSpotlight = saveSpotlight
        )
    }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        mockkStatic(Log::class)
        every { Log.isLoggable(any(), any()) } returns false
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkStatic(Log::class)
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
                mailboxListState = MailboxListState.Loading(selectionModeEnabled = false),
                topAppBarState = MailboxTopAppBarState.Loading,
                unreadFilterState = UnreadFilterState.Loading,
                bottomAppBarState = BottomBarState.Data.Hidden(emptyList()),
                spotlightState = SpotlightState.Hidden
            )

            assertEquals(expected, actual)

            verify { pagerFactory wasNot Called }
        }
    }

    @Test
    fun `when new location selected, new state is created and emitted`() = runTest {
        // Given
        val expectedMailLabel = MailLabel.System(MailLabelId.System.Spam)
        val expectedCount = UnreadCountersTestData.labelToCounterMap[expectedMailLabel.id.labelId]
        val expectedState = createMailboxDataState(
            selectedMailLabelId = expectedMailLabel.id,
            scrollToMailboxTop = Effect.of(expectedMailLabel.id)
        )
        val currentLocationFlow = MutableStateFlow<MailLabelId>(MailLabelId.System.Inbox)
        every { selectedMailLabelId.flow } returns currentLocationFlow
        every {
            mailboxReducer.newStateFrom(
                any(),
                MailboxEvent.NewLabelSelected(expectedMailLabel, expectedCount)
            )
        } returns expectedState
        returnExpectedStateForBottomBarEvent(expectedState = expectedState)

        mailboxViewModel.state.test {
            awaitItem()

            currentLocationFlow.emit(expectedMailLabel.id)

            // Then
            assertEquals(expectedState, awaitItem())
        }
    }

    @Test
    fun `when new location selected, new bottom bar state is created and emitted`() = runTest {
        // Given
        val expectedMailLabel = MailLabel.System(MailLabelId.System.Spam)
        val expectedCount = UnreadCountersTestData.labelToCounterMap[expectedMailLabel.id.labelId]
        val intermediateState = MailboxStateSampleData.createSelectionMode(
            currentMailLabel = expectedMailLabel,
            selectedMailboxItemUiModels = listOf(unreadMailboxItemUiModelWithLabel)
        )
        val expectedState = intermediateState.copy(
            bottomAppBarState = BottomBarState.Data.Shown(
                actions = listOf(
                    ActionUiModelSample.Archive,
                    ActionUiModelSample.Trash
                )
            )
        )
        val currentLocationFlow = MutableStateFlow<MailLabelId>(MailLabelId.System.Inbox)
        every { selectedMailLabelId.flow } returns currentLocationFlow
        every {
            mailboxReducer.newStateFrom(
                any(),
                MailboxEvent.NewLabelSelected(expectedMailLabel, expectedCount)
            )
        } returns intermediateState
        returnExpectedStateForBottomBarEvent(expectedState = expectedState)

        mailboxViewModel.state.test {
            awaitItem()

            // When
            currentLocationFlow.emit(expectedMailLabel.id)

            // Then
            assertEquals(intermediateState, awaitItem())
            assertEquals(expectedState, awaitItem())
            verifyOrder {
                mailboxReducer.newStateFrom(any(), MailboxEvent.NewLabelSelected(expectedMailLabel, expectedCount))
                mailboxReducer.newStateFrom(
                    currentState = intermediateState,
                    operation = MailboxEvent.MessageBottomBarEvent(
                        BottomBarEvent.ActionsData(listOf(ActionUiModelSample.Archive, ActionUiModelSample.Trash))
                    )
                )
            }
        }
    }

    @Test
    fun `when selected label changes, new state is created and emitted`() = runTest {
        // Given
        val initialMailLabel = MailLabelTestData.customLabelOne
        val modifiedMailLabel = initialMailLabel.copy(isExpanded = !MailLabelTestData.customLabelOne.isExpanded)
        val expectedState = MailboxStateSampleData.Loading.copy(
            mailboxListState = MailboxListState.Data.ViewMode(
                currentMailLabel = modifiedMailLabel,
                openItemEffect = Effect.empty(),
                scrollToMailboxTop = Effect.of(initialMailLabel.id),
                offlineEffect = Effect.empty(),
                refreshErrorEffect = Effect.empty(),
                refreshRequested = false,
                selectionModeEnabled = false
            )
        )
        val mailLabelsFlow = MutableStateFlow(
            MailLabels(
                systemLabels = LabelTestData.systemLabels,
                folders = emptyList(),
                labels = listOf(MailLabelTestData.customLabelOne)
            )
        )
        val currentLocationFlow = MutableStateFlow<MailLabelId>(initialMailLabel.id)
        every { observeMailLabels(userId) } returns mailLabelsFlow
        every { selectedMailLabelId.flow } returns currentLocationFlow
        every {
            mailboxReducer.newStateFrom(
                any(),
                MailboxEvent.SelectedLabelChanged(modifiedMailLabel)
            )
        } returns expectedState

        mailboxViewModel.state.test {
            awaitItem()

            mailLabelsFlow.emit(
                MailLabels(
                    systemLabels = LabelTestData.systemLabels,
                    folders = emptyList(),
                    labels = listOf(modifiedMailLabel)
                )
            )

            // Then
            assertEquals(expectedState, awaitItem())
        }
    }

    @Test
    fun `when counters for selected location change, new state is created and emitted`() = runTest {
        // Given
        val expectedCount = 42
        val expectedState = MailboxStateSampleData.Loading.copy(
            unreadFilterState = UnreadFilterState.Data(expectedCount, false)
        )
        val currentCountersFlow = MutableStateFlow(UnreadCountersTestData.systemUnreadCounters)
        val modifiedCounters = UnreadCountersTestData.systemUnreadCounters
            .update(initialLocationMailLabelId.labelId, expectedCount)
        every { observeUnreadCounters(userId) } returns currentCountersFlow
        every {
            mailboxReducer.newStateFrom(
                any(),
                MailboxEvent.SelectedLabelCountChanged(expectedCount)
            )
        } returns expectedState

        mailboxViewModel.state.test {
            awaitItem()

            currentCountersFlow.emit(modifiedCounters)

            // Then
            assertEquals(expectedState, awaitItem())
        }
    }

    @Test
    fun `when counters for a different location change, should not produce nor emit a new state`() = runTest {
        // Given
        val expectedCount = 42
        val expectedState = MailboxStateSampleData.Loading.copy(
            unreadFilterState = UnreadFilterState.Data(expectedCount, false)
        )
        val currentCountersFlow = MutableStateFlow(UnreadCountersTestData.systemUnreadCounters)
        val modifiedCounters = UnreadCountersTestData.systemUnreadCounters
            .update(MailLabelId.System.Spam.labelId, expectedCount)
        every { observeUnreadCounters(userId) } returns currentCountersFlow
        every {
            mailboxReducer.newStateFrom(
                any(),
                MailboxEvent.SelectedLabelCountChanged(expectedCount)
            )
        } returns expectedState

        mailboxViewModel.state.test {
            awaitItem()

            currentCountersFlow.emit(modifiedCounters)

            // Then
            verify(exactly = 0) {
                mailboxReducer.newStateFrom(any(), MailboxEvent.SelectedLabelCountChanged(expectedCount))
            }
            expectNoEvents()
        }
    }

    @Test
    fun `when long item click action is submitted and state is view mode, new state is created and emitted`() =
        runTest {
            // Given
            val item = readMailboxItemUiModel
            val intermediateState = createMailboxDataState()
            val expectedSelectionState = MailboxStateSampleData.createSelectionMode(listOf(item))
            val expectedBottomBarState = expectedSelectionState.copy(
                bottomAppBarState = BottomBarState.Data.Shown(
                    listOf(ActionUiModelSample.Archive, ActionUiModelSample.Trash)
                )
            )
            expectedSelectedLabelCountStateChange(intermediateState)
            returnExpectedStateWhenEnterSelectionMode(intermediateState, item, expectedSelectionState)
            returnExpectedStateForBottomBarEvent(expectedSelectionState, expectedBottomBarState)

            mailboxViewModel.state.test {
                // Given
                awaitItem() // First emission for selected user

                // When
                mailboxViewModel.submit(MailboxViewAction.OnItemLongClicked(item))

                // Then
                assertEquals(expectedSelectionState, awaitItem())
                assertEquals(expectedBottomBarState, awaitItem())
            }
        }

    @Test
    fun `when long item click action is submitted and state is not view mode, no new state is created nor emitted`() =
        runTest {
            // Given
            val item = readMailboxItemUiModel
            val intermediateState = createMailboxDataState()
            val expectedSelectionState = MailboxStateSampleData.createSelectionMode(listOf(item))
            val expectedBottomBarState = expectedSelectionState.copy(
                bottomAppBarState = BottomBarState.Data.Shown(
                    listOf(ActionUiModelSample.Archive, ActionUiModelSample.Trash)
                )
            )
            expectedSelectedLabelCountStateChange(intermediateState)
            returnExpectedStateWhenEnterSelectionMode(intermediateState, item, expectedSelectionState)
            returnExpectedStateForBottomBarEvent(expectedSelectionState, expectedBottomBarState)

            mailboxViewModel.state.test {
                // Given
                awaitItem() // First emission for selected user

                // When
                mailboxViewModel.submit(MailboxViewAction.OnItemLongClicked(item))

                // Then
                assertEquals(expectedSelectionState, awaitItem())
                assertEquals(expectedBottomBarState, awaitItem())

                // When
                mailboxViewModel.submit(MailboxViewAction.OnItemLongClicked(item))

                // Then
                verify(exactly = 1) { mailboxReducer.newStateFrom(any(), MailboxEvent.EnterSelectionMode(item)) }
            }
        }

    @Test
    fun `when avatar click action is submitted and state is view mode, new state is created and emitted`() = runTest {
        // Given
        val item = readMailboxItemUiModel
        val intermediateState = createMailboxDataState()
        val expectedState = MailboxStateSampleData.createSelectionMode(listOf(item))
        val expectedBottomBarActions = listOf(ActionUiModelSample.Archive, ActionUiModelSample.Trash)
        val expectedBottomBarState = MailboxStateSampleData.createSelectionMode(
            selectedMailboxItemUiModels = listOf(item),
            bottomBarAction = expectedBottomBarActions
        )
        expectedSelectedLabelCountStateChange(intermediateState)
        returnExpectedStateWhenEnterSelectionMode(intermediateState, item, expectedState)
        every {
            mailboxReducer.newStateFrom(
                currentState = expectedState,
                operation = MailboxEvent.MessageBottomBarEvent(BottomBarEvent.ActionsData(expectedBottomBarActions))
            )
        } returns expectedBottomBarState

        mailboxViewModel.state.test {
            // Given
            awaitItem() // First emission for selected user

            // When
            mailboxViewModel.submit(MailboxViewAction.OnItemAvatarClicked(item))

            // Then
            assertEquals(expectedState, awaitItem())
            verify(exactly = 1) {
                mailboxReducer.newStateFrom(intermediateState, MailboxEvent.EnterSelectionMode(item))
            }
            assertEquals(expectedBottomBarState, awaitItem())
        }
    }

    @Test
    fun `when avatar click action is submitted to add item to selection, new state is created and emitted`() = runTest {
        // Given
        val item = readMailboxItemUiModel
        val secondItem = draftMailboxItemUiModel
        val dataState = createMailboxDataState()
        val intermediateSelectionState = MailboxStateSampleData.createSelectionMode(listOf(item))
        val expectedSelectionState = MailboxStateSampleData.createSelectionMode(listOf(item, secondItem))

        val expectedBottomBarActions = listOf(ActionUiModelSample.Archive, ActionUiModelSample.Trash)

        val expectedBottomBarState = MailboxStateSampleData.createSelectionMode(
            selectedMailboxItemUiModels = listOf(item),
            bottomBarAction = expectedBottomBarActions
        )
        expectedSelectedLabelCountStateChange(dataState)
        returnExpectedStateWhenEnterSelectionMode(dataState, item, intermediateSelectionState)
        every {
            mailboxReducer.newStateFrom(
                currentState = intermediateSelectionState,
                operation = MailboxEvent.MessageBottomBarEvent(BottomBarEvent.ActionsData(expectedBottomBarActions))
            )
        } returns expectedBottomBarState
        every {
            mailboxReducer.newStateFrom(
                currentState = expectedBottomBarState,
                operation = MailboxEvent.ItemClicked.ItemAddedToSelection(secondItem)
            )
        } returns expectedSelectionState

        mailboxViewModel.state.test {
            // Given
            awaitItem() // First emission for selected user

            // When
            mailboxViewModel.submit(MailboxViewAction.OnItemAvatarClicked(item))

            // Then
            assertEquals(intermediateSelectionState, awaitItem())
            verify(exactly = 1) {
                mailboxReducer.newStateFrom(dataState, MailboxEvent.EnterSelectionMode(item))
            }
            assertEquals(expectedBottomBarState, awaitItem())

            // When
            mailboxViewModel.submit(MailboxViewAction.OnItemAvatarClicked(secondItem))

            // Then
            assertEquals(expectedSelectionState, awaitItem())
        }
    }

    @Test
    fun `when avatar click action is submitted to remove last item from selection, exit selection mode is triggered`() =
        runTest {
            // Given
            val item = readMailboxItemUiModel
            val initialState = createMailboxDataState()
            val intermediateState = MailboxStateSampleData.createSelectionMode(listOf(item))
            expectedSelectedLabelCountStateChange(initialState)
            returnExpectedStateWhenEnterSelectionMode(initialState, item, intermediateState)
            returnExpectedStateForBottomBarEvent(expectedState = intermediateState)
            every {
                mailboxReducer.newStateFrom(
                    intermediateState,
                    MailboxViewAction.ExitSelectionMode
                )
            } returns initialState

            mailboxViewModel.state.test {
                // Given
                awaitItem() // First emission for selected user

                // When
                mailboxViewModel.submit(MailboxViewAction.OnItemAvatarClicked(item))

                // Then
                assertEquals(intermediateState, awaitItem())
                verify(exactly = 1) {
                    mailboxReducer.newStateFrom(initialState, MailboxEvent.EnterSelectionMode(item))
                }

                // When
                mailboxViewModel.submit(MailboxViewAction.OnItemAvatarClicked(item))

                // Then
                assertEquals(initialState, awaitItem())
                verify(exactly = 1) {
                    mailboxReducer.newStateFrom(
                        currentState = intermediateState,
                        operation = MailboxViewAction.ExitSelectionMode
                    )
                }
            }
        }

    @Test
    fun `when avatar click action is submitted to remove item from selection, new state is created and emitted`() =
        runTest {
            // Given
            val item = readMailboxItemUiModel
            val secondItem = unreadMailboxItemUiModel
            val initialState = createMailboxDataState()
            val intermediateState = MailboxStateSampleData.createSelectionMode(listOf(item, secondItem))
            val expectedState = MailboxStateSampleData.createSelectionMode(listOf(secondItem))
            expectedSelectedLabelCountStateChange(initialState)
            returnExpectedStateWhenEnterSelectionMode(initialState, item, intermediateState)
            returnExpectedStateForBottomBarEvent(expectedState = intermediateState)
            every {
                mailboxReducer.newStateFrom(
                    intermediateState,
                    MailboxEvent.ItemClicked.ItemRemovedFromSelection(item)
                )
            } returns expectedState

            mailboxViewModel.state.test {
                // Given
                awaitItem() // First emission for selected user

                // When
                mailboxViewModel.submit(MailboxViewAction.OnItemAvatarClicked(item))

                // Then
                assertEquals(intermediateState, awaitItem())
                verify(exactly = 1) {
                    mailboxReducer.newStateFrom(initialState, MailboxEvent.EnterSelectionMode(item))
                }

                // When
                mailboxViewModel.submit(MailboxViewAction.OnItemAvatarClicked(item))

                // Then
                assertEquals(expectedState, awaitItem())
                verify(exactly = 1) {
                    mailboxReducer.newStateFrom(
                        currentState = intermediateState,
                        operation = MailboxEvent.ItemClicked.ItemRemovedFromSelection(item)
                    )
                }
            }
        }

    @Test
    fun `when exit selection mode action submitted, new state is created and emitted`() = runTest {
        // Given
        val expectedState = MailboxStateSampleData.Loading.copy(
            topAppBarState = MailboxTopAppBarState.Data.DefaultMode(
                currentLabelName = MailLabel.System(initialLocationMailLabelId).text()
            )
        )
        every {
            mailboxReducer.newStateFrom(MailboxStateSampleData.Loading, MailboxViewAction.ExitSelectionMode)
        } returns expectedState

        mailboxViewModel.state.test {
            // Given
            awaitItem() // First emission for selected user

            // When
            mailboxViewModel.submit(MailboxViewAction.ExitSelectionMode)

            // Then
            assertEquals(expectedState, awaitItem())
        }
    }

    @Test
    fun `mailbox items for the current location are requested when location changes`() = runTest {
        // Given
        val currentLocationFlow = MutableStateFlow<MailLabelId>(initialLocationMailLabelId)
        val initialMailboxState = createMailboxDataState()
        val expectedState = createMailboxDataState(selectedMailLabelId = MailLabelId.System.Spam)
        val userIds = listOf(userId)
        every { selectedMailLabelId.flow } returns currentLocationFlow
        every { pagerFactory.create(userIds, any(), false, Message) } returns mockk mockPager@{
            every { this@mockPager.flow } returns flowOf(PagingData.from(listOf(unreadMailboxItem)))
        }
        every { mailboxReducer.newStateFrom(any(), any()) } returns initialMailboxState
        every {
            mailboxReducer.newStateFrom(
                initialMailboxState,
                MailboxEvent.NewLabelSelected(
                    MailLabelId.System.Spam.toMailLabel(),
                    UnreadCountersTestData.labelToCounterMap[MailLabelId.System.Spam.labelId]!!
                )
            )
        } returns expectedState
        returnExpectedStateForBottomBarEvent(expectedState = expectedState)

        mailboxViewModel.items.test {
            // Then
            awaitItem()
            verify { pagerFactory.create(userIds, initialLocationMailLabelId, false, Message) }

            // When
            currentLocationFlow.emit(MailLabelId.System.Spam)

            // Then
            awaitItem()
            verify { pagerFactory.create(userIds, MailLabelId.System.Spam, false, Message) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `mailbox items are mapped to mailbox item ui models`() = runTest {
        // Given
        coEvery {
            mailboxItemMapper.toUiModel(unreadMailboxItem, ContactTestData.contacts, defaultFolderColorSettings)
        } returns unreadMailboxItemUiModel
        coEvery {
            mailboxItemMapper.toUiModel(readMailboxItem, ContactTestData.contacts, defaultFolderColorSettings)
        } returns readMailboxItemUiModel
        every { pagerFactory.create(any(), any(), any(), any()) } returns mockk {
            val pagingData = PagingData.from(listOf(unreadMailboxItem, readMailboxItem))
            every { this@mockk.flow } returns flowOf(pagingData)
        }
        every { mailboxReducer.newStateFrom(any(), any()) } returns createMailboxDataState()
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
    fun `mailbox items are mapped according to folder color setting`() = runTest {
        // Given
        val updatedFolderColorSetting = FolderColorSettings(useFolderColor = false)
        val folderColorFlow = MutableStateFlow(defaultFolderColorSettings)
        coEvery { observeFolderColorSettings(userId) } returns folderColorFlow
        coEvery {
            mailboxItemMapper.toUiModel(
                mailboxItem = unreadMailboxItemWithLabel,
                contacts = ContactTestData.contacts,
                folderColorSettings = defaultFolderColorSettings
            )
        } returns unreadMailboxItemUiModelWithLabelColored
        coEvery {
            mailboxItemMapper.toUiModel(
                unreadMailboxItemWithLabel,
                ContactTestData.contacts,
                updatedFolderColorSetting
            )
        } returns unreadMailboxItemUiModelWithLabel

        every { pagerFactory.create(listOf(userId), Archive, false, any()) } returns mockk {
            val pagingData = PagingData.from(listOf(unreadMailboxItemWithLabel))
            every { this@mockk.flow } returns flowOf(pagingData)
        }
        every { mailboxReducer.newStateFrom(any(), any()) } returns createMailboxDataState()
        val differ = MailboxAsyncPagingDataDiffer.differ

        // When
        mailboxViewModel.items.test {
            // Then
            val pagingData = awaitItem()
            differ.submitData(pagingData)

            val expected = listOf(unreadMailboxItemUiModelWithLabelColored)
            assertEquals(expected, differ.snapshot().items)

            // When
            folderColorFlow.emit(updatedFolderColorSetting)

            // Then
            val updatedPagingData = awaitItem()
            differ.submitData(updatedPagingData)

            val updatedExpected = listOf(unreadMailboxItemUiModelWithLabel)
            assertEquals(updatedExpected, differ.snapshot().items)
        }
    }

    @Test
    fun `user contacts are used to map mailbox items to ui models`() = runTest {
        // Given
        coEvery {
            mailboxItemMapper.toUiModel(unreadMailboxItem, ContactTestData.contacts, defaultFolderColorSettings)
        } returns unreadMailboxItemUiModel
        coEvery {
            mailboxItemMapper.toUiModel(readMailboxItem, ContactTestData.contacts, defaultFolderColorSettings)
        } returns readMailboxItemUiModel
        every { pagerFactory.create(any(), any(), any(), any()) } returns mockk {
            val pagingData = PagingData.from(listOf(unreadMailboxItem, readMailboxItem))
            every { this@mockk.flow } returns flowOf(pagingData)
        }
        every { mailboxReducer.newStateFrom(any(), any()) } returns createMailboxDataState()
        val differ = MailboxAsyncPagingDataDiffer.differ
        // When
        mailboxViewModel.items.test {
            // Then
            val pagingData = awaitItem()
            differ.submitData(pagingData)

            coVerify { mailboxItemMapper.toUiModel(any(), ContactTestData.contacts, defaultFolderColorSettings) }
        }
    }

    @Test
    fun `when open item action submitted in message mode, new state is produced and emitted`() = runTest {
        // Given
        val item = buildMailboxUiModelItem("id", Message)
        val intermediateState = createMailboxDataState()
        val expectedState = createMailboxDataState(
            openEffect = Effect.of(OpenMailboxItemRequest(MailboxItemId(item.id), Conversation, false))
        )
        expectViewMode(NoConversationGrouping)
        expectedSelectedLabelCountStateChange(intermediateState)
        every {
            mailboxReducer.newStateFrom(
                intermediateState,
                MailboxEvent.ItemClicked.ItemDetailsOpenedInViewMode(item, NoConversationGrouping)
            )
        } returns expectedState

        // When
        mailboxViewModel.submit(MailboxViewAction.ItemClicked(item))
        mailboxViewModel.state.test {
            // Then
            assertEquals(expectedState, awaitItem())
        }
    }

    @Test
    fun `when item action to open composer submitted in draft, new state is produced and emitted`() = runTest {
        // Given
        val item = buildMailboxUiModelItem("id", Message, shouldOpenInComposer = true)
        val intermediateState = createMailboxDataState(selectedMailLabelId = MailLabelId.System.Drafts)
        val expectedState = createMailboxDataState(
            selectedMailLabelId = MailLabelId.System.Drafts,
            openEffect = Effect.of(OpenMailboxItemRequest(MailboxItemId(item.id), Conversation, true))
        )
        expectViewMode(NoConversationGrouping)
        expectedSelectedLabelCountStateChange(intermediateState)
        every {
            mailboxReducer.newStateFrom(
                intermediateState,
                MailboxEvent.ItemClicked.OpenComposer(item)
            )
        } returns expectedState

        // When
        mailboxViewModel.submit(MailboxViewAction.ItemClicked(item))
        mailboxViewModel.state.test {
            // Then
            assertEquals(expectedState, awaitItem())
        }
    }

    @Test
    fun `when on offline with data is submitted, new state is produced and emitted`() = runTest {
        // Given
        val expectedState = MailboxStateSampleData.Loading.copy(
            mailboxListState = MailboxListState.Data.ViewMode(
                currentMailLabel = MailLabel.System(initialLocationMailLabelId),
                openItemEffect = Effect.empty(),
                scrollToMailboxTop = Effect.empty(),
                offlineEffect = Effect.of(Unit),
                refreshErrorEffect = Effect.empty(),
                refreshRequested = false,
                selectionModeEnabled = false
            )
        )
        every {
            mailboxReducer.newStateFrom(
                MailboxStateSampleData.Loading,
                MailboxViewAction.OnOfflineWithData
            )
        } returns expectedState

        // When
        mailboxViewModel.submit(MailboxViewAction.OnOfflineWithData)
        mailboxViewModel.state.test {
            // Then
            assertEquals(expectedState, awaitItem())
        }
    }

    @Test
    fun `when on error with data is submitted, new state is produced and emitted`() = runTest {
        // Given
        val expectedState = MailboxStateSampleData.Loading.copy(
            mailboxListState = MailboxListState.Data.ViewMode(
                currentMailLabel = MailLabel.System(initialLocationMailLabelId),
                openItemEffect = Effect.empty(),
                scrollToMailboxTop = Effect.empty(),
                offlineEffect = Effect.empty(),
                refreshErrorEffect = Effect.of(Unit),
                refreshRequested = false,
                selectionModeEnabled = false
            )
        )
        every {
            mailboxReducer.newStateFrom(
                MailboxStateSampleData.Loading,
                MailboxViewAction.OnErrorWithData
            )
        } returns expectedState

        // When
        mailboxViewModel.submit(MailboxViewAction.OnErrorWithData)
        mailboxViewModel.state.test {
            // Then
            assertEquals(expectedState, awaitItem())
        }
    }

    @Test
    fun `when refresh action is submitted, new state is produced and emitted`() = runTest {
        // Given
        val expectedState = MailboxStateSampleData.Loading.copy(
            mailboxListState = MailboxListState.Data.ViewMode(
                currentMailLabel = MailLabel.System(initialLocationMailLabelId),
                openItemEffect = Effect.empty(),
                scrollToMailboxTop = Effect.empty(),
                offlineEffect = Effect.of(Unit),
                refreshErrorEffect = Effect.empty(),
                refreshRequested = true,
                selectionModeEnabled = false
            )
        )
        every {
            mailboxReducer.newStateFrom(
                MailboxStateSampleData.Loading,
                MailboxViewAction.Refresh
            )
        } returns expectedState

        // When
        mailboxViewModel.submit(MailboxViewAction.Refresh)
        mailboxViewModel.state.test {
            // Then
            assertEquals(expectedState, awaitItem())
        }
    }

    @Test
    fun `when open item action submitted in conversation mode, new state is produced and emitted`() = runTest {
        // Given
        val item = buildMailboxUiModelItem("id", Conversation)
        val intermediateState = createMailboxDataState()
        val expectedState = createMailboxDataState(
            openEffect = Effect.of(OpenMailboxItemRequest(MailboxItemId(item.id), Conversation, false))
        )

        every { observeCurrentViewMode(userId = any()) } returns flowOf(ConversationGrouping)

        expectedSelectedLabelCountStateChange(intermediateState)
        every {
            mailboxReducer.newStateFrom(
                intermediateState,
                MailboxEvent.ItemClicked.ItemDetailsOpenedInViewMode(item, ConversationGrouping)
            )
        } returns expectedState

        mailboxViewModel.state.test {
            awaitItem() // await that label count gets emitted

            // When
            mailboxViewModel.submit(MailboxViewAction.ItemClicked(item))

            // Then
            assertEquals(expectedState, awaitItem())
        }
    }

    @Test
    fun `when enable unread filter action submitted, produces and emits a new state`() = runTest {
        // Given
        val expectedState = MailboxStateSampleData.Loading.copy(
            unreadFilterState = UnreadFilterState.Data(5, true)
        )
        every { mailboxReducer.newStateFrom(any(), MailboxViewAction.EnableUnreadFilter) } returns expectedState

        // When
        mailboxViewModel.submit(MailboxViewAction.EnableUnreadFilter)
        mailboxViewModel.state.test {
            // Then
            assertEquals(expectedState, awaitItem())
        }
    }

    @Test
    fun `when disable unread filter action submitted, produces and emits a new state`() = runTest {
        // Given
        val expectedState = MailboxStateSampleData.Loading.copy(
            unreadFilterState = UnreadFilterState.Data(5, false)
        )
        every {
            mailboxReducer.newStateFrom(MailboxStateSampleData.Loading, MailboxViewAction.DisableUnreadFilter)
        } returns expectedState

        // When
        mailboxViewModel.submit(MailboxViewAction.DisableUnreadFilter)
        mailboxViewModel.state.test {
            // Then
            assertEquals(expectedState, awaitItem())
        }
    }

    @Test
    fun `when selection mode enabled with a feature flag, produces and emits a new state`() = runTest {
        // Given
        val expectedState = MailboxStateSampleData.Loading.copy(
            topAppBarState = MailboxTopAppBarState.Data.DefaultMode(
                currentLabelName = MailLabel.System(initialLocationMailLabelId).text()
            ),
            mailboxListState = MailboxListState.Data.ViewMode(
                currentMailLabel = MailLabel.System(initialLocationMailLabelId),
                openItemEffect = Effect.empty(),
                scrollToMailboxTop = Effect.empty(),
                offlineEffect = Effect.empty(),
                refreshErrorEffect = Effect.empty(),
                refreshRequested = false,
                selectionModeEnabled = true
            )
        )
        every {
            mailboxReducer.newStateFrom(
                MailboxStateSampleData.Loading,
                MailboxEvent.SelectionModeEnabledChanged(selectionModeEnabled = true)
            )
        } returns expectedState
        val featureFlagFlow = MutableSharedFlow<FeatureFlag>()
        every { observeMailFeature(userId, MailFeatureId.SelectionMode) } returns featureFlagFlow

        // When
        mailboxViewModel.state.test {

            // The initial state
            skipItems(1)
            featureFlagFlow.emit(FeatureFlag.default(MailFeatureId.SelectionMode.id.id, defaultValue = true))

            // Then
            assertEquals(expectedState, awaitItem())
        }
    }

    @Test
    fun `mailbox pager is recreated when unread filter state changes`() = runTest {
        // Given
        val expectedMailBoxState = createMailboxDataState(unreadFilterState = false)
        every { mailboxReducer.newStateFrom(any(), any()) } returns expectedMailBoxState
        every { pagerFactory.create(listOf(userId), Archive, any(), Message) } returns mockk mockPager@{
            every { this@mockPager.flow } returns flowOf(PagingData.from(listOf(unreadMailboxItem)))
        }
        every { mailboxReducer.newStateFrom(expectedMailBoxState, MailboxViewAction.EnableUnreadFilter) } returns
            createMailboxDataState(unreadFilterState = true)

        mailboxViewModel.items.test {
            // When

            // Then
            awaitItem()
            verify(exactly = 1) { pagerFactory.create(listOf(userId), Archive, false, Message) }

            // When
            mailboxViewModel.submit(MailboxViewAction.EnableUnreadFilter)

            // Then
            awaitItem()
            verify(exactly = 1) { pagerFactory.create(listOf(userId), Archive, true, Message) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `mailbox pager is recreated when selected mail label state changes`() = runTest {
        // Given
        val expectedMailBoxState = createMailboxDataState(selectedMailLabelId = initialLocationMailLabelId)
        val inboxLabel = MailLabelId.System.Inbox
        val currentLocationFlow = MutableStateFlow<MailLabelId>(initialLocationMailLabelId)
        val expectedState = createMailboxDataState(selectedMailLabelId = inboxLabel)
        every { selectedMailLabelId.flow } returns currentLocationFlow
        every { mailboxReducer.newStateFrom(any(), any()) } returns expectedMailBoxState
        every { pagerFactory.create(any(), any(), any(), any()) } returns mockk mockPager@{
            every { this@mockPager.flow } returns flowOf(PagingData.from(listOf(unreadMailboxItem)))
        }
        every {
            mailboxReducer.newStateFrom(
                expectedMailBoxState,
                MailboxEvent.NewLabelSelected(
                    inboxLabel.toMailLabel(),
                    UnreadCountersTestData.labelToCounterMap[inboxLabel.labelId]!!
                )
            )
        } returns expectedState
        returnExpectedStateForBottomBarEvent(expectedState = expectedState)

        mailboxViewModel.items.test {
            // When

            // Then
            awaitItem()
            verify(exactly = 1) { pagerFactory.create(listOf(userId), Archive, false, Message) }

            // When
            currentLocationFlow.emit(inboxLabel)

            // Then
            awaitItem()
            // mailbox pager is recreated only once when view mode for the newly selected location does not change
            verify(exactly = 1) { pagerFactory.create(listOf(userId), inboxLabel, false, Message) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `pager is not recreated when any state beside selectedLabel, unreadFilter, viewMode or primaryUser changes`() =
        runTest {
            // Given
            val expectedMailBoxState = createMailboxDataState(Effect.empty())
            every { mailboxReducer.newStateFrom(any(), any()) } returns expectedMailBoxState
            every { pagerFactory.create(any(), any(), any(), any()) } returns mockk mockPager@{
                every { this@mockPager.flow } returns flowOf(PagingData.from(listOf(unreadMailboxItem)))
            }
            every {
                mailboxReducer.newStateFrom(
                    expectedMailBoxState,
                    MailboxEvent.ItemClicked.ItemDetailsOpenedInViewMode(
                        unreadMailboxItemUiModel,
                        NoConversationGrouping
                    )
                )
            } returns createMailboxDataState(
                Effect.of(
                    OpenMailboxItemRequest(
                        MailboxItemId(unreadMailboxItem.id),
                        unreadMailboxItem.type,
                        false
                    )
                )
            )

            mailboxViewModel.items.test {
                // When
                awaitItem()
                verify(exactly = 1) { pagerFactory.create(listOf(userId), Archive, false, Message) }

                mailboxViewModel.submit(MailboxViewAction.ItemClicked(unreadMailboxItemUiModel))

                // Then
                expectNoEvents()
                confirmVerified(pagerFactory)
            }
        }

    @Test
    fun `verify mapped paging data is cached`() = runTest {
        // Given
        every { mailboxReducer.newStateFrom(any(), any()) } returns createMailboxDataState()
        every { pagerFactory.create(any(), any(), any(), any()) } returns mockk mockPager@{
            every { this@mockPager.flow } returns flowOf(PagingData.from(listOf(unreadMailboxItem)))
        }

        mailboxViewModel.items.test {
            // When
            awaitItem()
            // Then
            verify(exactly = 1) { pagerFactory.create(listOf(userId), Archive, false, Message) }
        }

        mailboxViewModel.items.test {
            // When
            awaitItem()
            // Then
            confirmVerified(pagerFactory)
        }
    }

    @Test
    fun `when mark as read is triggered for conversation grouping then mark conversations as read is called`() =
        runTest {
            // Given
            val item = readMailboxItemUiModel
            val secondItem = unreadMailboxItemUiModel
            val initialState = createMailboxDataState()
            val intermediateState = MailboxStateSampleData.createSelectionMode(listOf(item, secondItem))
            val expectedState = MailboxStateSampleData.createSelectionMode(
                listOf(item.copy(isRead = true), secondItem.copy(isRead = true))
            )
            expectViewMode(ConversationGrouping)
            expectedSelectedLabelCountStateChange(initialState)
            returnExpectedStateWhenEnterSelectionMode(initialState, item, intermediateState)
            returnExpectedStateForBottomBarEvent(expectedState = intermediateState)
            returnExpectedStateForMarkAsRead(intermediateState, expectedState)
            expectMarkConversationsAsReadSucceeds(userId, listOf(item, secondItem))

            mailboxViewModel.state.test {
                // Given
                awaitItem() // First emission for selected user

                // When
                mailboxViewModel.submit(MailboxViewAction.OnItemAvatarClicked(item))

                // Then
                assertEquals(intermediateState, awaitItem())
                verify(exactly = 1) {
                    mailboxReducer.newStateFrom(initialState, MailboxEvent.EnterSelectionMode(item))
                }

                // When
                mailboxViewModel.submit(MailboxViewAction.MarkAsRead)

                // Then
                assertEquals(expectedState, awaitItem())
                coVerify(exactly = 1) {
                    markConversationsAsRead(userId, listOf(ConversationId(item.id), ConversationId(secondItem.id)))
                }
                coVerify { markMessagesAsRead wasNot Called }
            }
        }

    @Test
    fun `verify mark conversations read action triggers mark conversations read use case for each user id`() = runTest {
        // Given
        val item = readMailboxItemUiModel.copy(userId = userId)
        val secondItem = unreadMailboxItemUiModel.copy(userId = userId1)
        val initialState = createMailboxDataState()
        val intermediateState = MailboxStateSampleData.createSelectionMode(listOf(item, secondItem))
        val expectedState = MailboxStateSampleData.createSelectionMode(
            listOf(item.copy(isRead = true), secondItem.copy(isRead = true))
        )
        expectViewMode(ConversationGrouping)
        expectedSelectedLabelCountStateChange(initialState)
        returnExpectedStateWhenEnterSelectionMode(initialState, item, intermediateState)
        returnExpectedStateForBottomBarEvent(expectedState = intermediateState)
        returnExpectedStateForMarkAsRead(intermediateState, expectedState)
        expectMarkConversationsAsReadSucceeds(userId, listOf(item))
        expectMarkConversationsAsReadSucceeds(userId1, listOf(secondItem))

        mailboxViewModel.state.test {
            // Given
            awaitItem() // First emission for selected user

            // When
            mailboxViewModel.submit(MailboxViewAction.OnItemAvatarClicked(item))

            // Then
            assertEquals(intermediateState, awaitItem())
            verify(exactly = 1) {
                mailboxReducer.newStateFrom(initialState, MailboxEvent.EnterSelectionMode(item))
            }

            // When
            mailboxViewModel.submit(MailboxViewAction.MarkAsRead)

            // Then
            assertEquals(expectedState, awaitItem())
            coVerifySequence {
                markConversationsAsRead(userId, listOf(ConversationId(item.id)))
                markConversationsAsRead(userId1, listOf(ConversationId(secondItem.id)))
            }
        }
    }

    @Test
    fun `when mark as unread is triggered for conversation grouping then mark conversations as unread is called`() =
        runTest {
            // Given
            val item = readMailboxItemUiModel
            val secondItem = unreadMailboxItemUiModel
            val initialState = createMailboxDataState()
            val intermediateState = MailboxStateSampleData.createSelectionMode(listOf(item, secondItem))
            val expectedState = MailboxStateSampleData.createSelectionMode(
                listOf(item.copy(isRead = false), secondItem.copy(isRead = false))
            )
            expectViewMode(ConversationGrouping)
            expectedSelectedLabelCountStateChange(initialState)
            returnExpectedStateWhenEnterSelectionMode(initialState, item, intermediateState)
            returnExpectedStateForBottomBarEvent(expectedState = intermediateState)
            returnExpectedStateForMarkAsUnread(intermediateState, expectedState)
            expectMarkConversationsAsUnreadSucceeds(userId, listOf(item, secondItem))

            mailboxViewModel.state.test {
                // Given
                awaitItem() // First emission for selected user

                // When
                mailboxViewModel.submit(MailboxViewAction.OnItemAvatarClicked(item))

                // Then
                assertEquals(intermediateState, awaitItem())
                verify(exactly = 1) {
                    mailboxReducer.newStateFrom(initialState, MailboxEvent.EnterSelectionMode(item))
                }

                // When
                mailboxViewModel.submit(MailboxViewAction.MarkAsUnread)

                // Then
                assertEquals(expectedState, awaitItem())
                coVerify(exactly = 1) {
                    markConversationsAsUnread(userId, listOf(ConversationId(item.id), ConversationId(secondItem.id)))
                }
                coVerify { markMessagesAsUnread wasNot Called }
            }
        }

    @Test
    fun `verify mark conversations unread action triggers mark conversations unread use case for each user id`() =
        runTest {
            // Given
            val item = readMailboxItemUiModel.copy(userId = userId)
            val secondItem = unreadMailboxItemUiModel.copy(userId = userId1)
            val initialState = createMailboxDataState()
            val intermediateState = MailboxStateSampleData.createSelectionMode(listOf(item, secondItem))
            val expectedState = MailboxStateSampleData.createSelectionMode(
                listOf(item.copy(isRead = false), secondItem.copy(isRead = false))
            )
            expectViewMode(ConversationGrouping)
            expectedSelectedLabelCountStateChange(initialState)
            returnExpectedStateWhenEnterSelectionMode(initialState, item, intermediateState)
            returnExpectedStateForBottomBarEvent(expectedState = intermediateState)
            returnExpectedStateForMarkAsUnread(intermediateState, expectedState)
            expectMarkConversationsAsUnreadSucceeds(userId, listOf(item))
            expectMarkConversationsAsUnreadSucceeds(userId1, listOf(secondItem))

            mailboxViewModel.state.test {
                // Given
                awaitItem() // First emission for selected user

                // When
                mailboxViewModel.submit(MailboxViewAction.OnItemAvatarClicked(item))

                // Then
                assertEquals(intermediateState, awaitItem())
                verify(exactly = 1) {
                    mailboxReducer.newStateFrom(initialState, MailboxEvent.EnterSelectionMode(item))
                }

                // When
                mailboxViewModel.submit(MailboxViewAction.MarkAsUnread)

                // Then
                assertEquals(expectedState, awaitItem())
                coVerifySequence {
                    markConversationsAsUnread(userId, listOf(ConversationId(item.id)))
                    markConversationsAsUnread(userId1, listOf(ConversationId(secondItem.id)))
                }
            }
        }

    @Test
    fun `when mark as read is triggered for no conversation grouping then mark messages as read use case is called`() =
        runTest {
            // Given
            val item = readMailboxItemUiModel
            val secondItem = unreadMailboxItemUiModel
            val initialState = createMailboxDataState()
            val intermediateState = MailboxStateSampleData.createSelectionMode(listOf(item, secondItem))
            val expectedState = MailboxStateSampleData.createSelectionMode(
                listOf(item.copy(isRead = true), secondItem.copy(isRead = true))
            )
            expectViewMode(NoConversationGrouping)
            expectedSelectedLabelCountStateChange(initialState)
            returnExpectedStateWhenEnterSelectionMode(initialState, item, intermediateState)
            returnExpectedStateForBottomBarEvent(expectedState = intermediateState)
            returnExpectedStateForMarkAsRead(intermediateState, expectedState)
            expectMarkMessagesAsReadSucceeds(userId, listOf(item, secondItem))

            mailboxViewModel.state.test {
                // Given
                awaitItem() // First emission for selected user

                // When
                mailboxViewModel.submit(MailboxViewAction.OnItemAvatarClicked(item))

                // Then
                assertEquals(intermediateState, awaitItem())
                verify(exactly = 1) {
                    mailboxReducer.newStateFrom(initialState, MailboxEvent.EnterSelectionMode(item))
                }

                // When
                mailboxViewModel.submit(MailboxViewAction.MarkAsRead)

                // Then
                assertEquals(expectedState, awaitItem())
                coVerify(exactly = 1) {
                    markMessagesAsRead(userId, listOf(MessageId(item.id), MessageId(secondItem.id)))
                }
                coVerify { markConversationsAsRead wasNot Called }
            }
        }

    @Test
    fun `verify mark messages read action triggers mark messages read use case for each user id`() = runTest {
        // Given
        val item = readMailboxItemUiModel.copy(userId = userId)
        val secondItem = unreadMailboxItemUiModel.copy(userId = userId1)
        val initialState = createMailboxDataState()
        val intermediateState = MailboxStateSampleData.createSelectionMode(listOf(item, secondItem))
        val expectedState = MailboxStateSampleData.createSelectionMode(
            listOf(item.copy(isRead = true), secondItem.copy(isRead = true))
        )
        expectViewMode(NoConversationGrouping)
        expectedSelectedLabelCountStateChange(initialState)
        returnExpectedStateWhenEnterSelectionMode(initialState, item, intermediateState)
        returnExpectedStateForBottomBarEvent(expectedState = intermediateState)
        returnExpectedStateForMarkAsRead(intermediateState, expectedState)
        expectMarkMessagesAsReadSucceeds(userId, listOf(item))
        expectMarkMessagesAsReadSucceeds(userId1, listOf(secondItem))

        mailboxViewModel.state.test {
            // Given
            awaitItem() // First emission for selected user

            // When
            mailboxViewModel.submit(MailboxViewAction.OnItemAvatarClicked(item))

            // Then
            assertEquals(intermediateState, awaitItem())
            verify(exactly = 1) {
                mailboxReducer.newStateFrom(initialState, MailboxEvent.EnterSelectionMode(item))
            }

            // When
            mailboxViewModel.submit(MailboxViewAction.MarkAsRead)

            // Then
            assertEquals(expectedState, awaitItem())
            coVerifySequence {
                markMessagesAsRead(userId, listOf(MessageId(item.id)))
                markMessagesAsRead(userId1, listOf(MessageId(secondItem.id)))
            }
        }
    }

    @Test
    fun `when mark as unread is triggered for no conversation grouping then mark messages as unread is called`() =
        runTest {
            // Given
            val item = readMailboxItemUiModel
            val secondItem = unreadMailboxItemUiModel
            val initialState = createMailboxDataState()
            val intermediateState = MailboxStateSampleData.createSelectionMode(listOf(item, secondItem))
            val expectedState = MailboxStateSampleData.createSelectionMode(
                listOf(item.copy(isRead = true), secondItem.copy(isRead = true))
            )
            expectViewMode(NoConversationGrouping)
            expectedSelectedLabelCountStateChange(initialState)
            returnExpectedStateWhenEnterSelectionMode(initialState, item, intermediateState)
            returnExpectedStateForBottomBarEvent(expectedState = intermediateState)
            returnExpectedStateForMarkAsUnread(intermediateState, expectedState)
            expectMarkMessagesAsUnreadSucceeds(userId, listOf(item, secondItem))

            mailboxViewModel.state.test {
                // Given
                awaitItem() // First emission for selected user

                // When
                mailboxViewModel.submit(MailboxViewAction.OnItemAvatarClicked(item))

                // Then
                assertEquals(intermediateState, awaitItem())
                verify(exactly = 1) {
                    mailboxReducer.newStateFrom(initialState, MailboxEvent.EnterSelectionMode(item))
                }

                // When
                mailboxViewModel.submit(MailboxViewAction.MarkAsUnread)

                // Then
                assertEquals(expectedState, awaitItem())
                coVerify(exactly = 1) {
                    markMessagesAsUnread(userId, listOf(MessageId(item.id), MessageId(secondItem.id)))
                }
                coVerify { markConversationsAsUnread wasNot Called }
            }
        }

    @Test
    fun `verify mark messages unread action triggers mark messages unread use case for each user id`() = runTest {
        // Given
        val item = readMailboxItemUiModel.copy(userId = userId)
        val secondItem = unreadMailboxItemUiModel.copy(userId = userId1)
        val initialState = createMailboxDataState()
        val intermediateState = MailboxStateSampleData.createSelectionMode(listOf(item, secondItem))
        val expectedState = MailboxStateSampleData.createSelectionMode(
            listOf(item.copy(isRead = true), secondItem.copy(isRead = true))
        )
        expectViewMode(NoConversationGrouping)
        expectedSelectedLabelCountStateChange(initialState)
        returnExpectedStateWhenEnterSelectionMode(initialState, item, intermediateState)
        returnExpectedStateForBottomBarEvent(expectedState = intermediateState)
        returnExpectedStateForMarkAsUnread(intermediateState, expectedState)
        expectMarkMessagesAsUnreadSucceeds(userId, listOf(item))
        expectMarkMessagesAsUnreadSucceeds(userId1, listOf(secondItem))

        mailboxViewModel.state.test {
            // Given
            awaitItem() // First emission for selected user

            // When
            mailboxViewModel.submit(MailboxViewAction.OnItemAvatarClicked(item))

            // Then
            assertEquals(intermediateState, awaitItem())
            verify(exactly = 1) {
                mailboxReducer.newStateFrom(initialState, MailboxEvent.EnterSelectionMode(item))
            }

            // When
            mailboxViewModel.submit(MailboxViewAction.MarkAsUnread)

            // Then
            assertEquals(expectedState, awaitItem())
            coVerifySequence {
                markMessagesAsUnread(userId, listOf(MessageId(item.id)))
                markMessagesAsUnread(userId1, listOf(MessageId(secondItem.id)))
            }
        }
    }

    @Test
    fun `mailbox items are not requested when a user account is removed`() = runTest {
        // Given
        val currentLocationFlow = MutableStateFlow<MailLabelId>(initialLocationMailLabelId)
        val initialMailboxState = createMailboxDataState()
        val expectedState = createMailboxDataState()
        val userIds = listOf(userId)
        val primaryUserFlow = MutableStateFlow<UserId?>(userId)
        coEvery {
            observeCurrentViewMode(userId = userId, initialLocationMailLabelId)
        } returns flowOf(ConversationGrouping)
        every { observePrimaryUserId.invoke() } returns primaryUserFlow
        every { selectedMailLabelId.flow } returns currentLocationFlow
        every { pagerFactory.create(userIds, any(), false, Conversation) } returns mockk mockPager@{
            every { this@mockPager.flow } returns flowOf(PagingData.from(listOf(unreadMailboxItem)))
        }
        every { mailboxReducer.newStateFrom(any(), any()) } returns initialMailboxState
        returnExpectedStateForBottomBarEvent(expectedState = expectedState)

        mailboxViewModel.items.test {
            // Then
            awaitItem()
            verify { pagerFactory.create(userIds, initialLocationMailLabelId, false, Conversation) }

            // When
            primaryUserFlow.emit(null)

            // Then
            verify(exactly = 0) { pagerFactory.create(userIds, initialLocationMailLabelId, false, Message) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun createMailboxDataState(
        openEffect: Effect<OpenMailboxItemRequest> = Effect.empty(),
        scrollToMailboxTop: Effect<MailLabelId> = Effect.empty(),
        unreadFilterState: Boolean = false,
        selectedMailLabelId: MailLabelId.System = initialLocationMailLabelId
    ): MailboxState {
        return MailboxStateSampleData.Loading.copy(
            mailboxListState = MailboxListState.Data.ViewMode(
                currentMailLabel = MailLabel.System(selectedMailLabelId),
                openItemEffect = openEffect,
                scrollToMailboxTop = scrollToMailboxTop,
                offlineEffect = Effect.empty(),
                refreshErrorEffect = Effect.empty(),
                refreshRequested = false,
                selectionModeEnabled = false
            ),
            unreadFilterState = UnreadFilterState.Data(
                numUnread = UnreadCountersTestData.labelToCounterMap[initialLocationMailLabelId.labelId]!!,
                isFilterEnabled = unreadFilterState
            )
        )
    }

    @Test
    fun `spotlight state should be shown when repository emits the preference with value true`() = runTest {
        // When
        coEvery {
            observeSpotlight()
        } returns flowOf(SpotlightPreference(display = true).right())
        every {
            mailboxReducer.newStateFrom(
                any(),
                any()
            )
        } returns MailboxStateSampleData.SpotlightShown

        mailboxViewModel.state.test {
            val currentState = awaitItem()

            // Then
            verify(exactly = 1) {
                mailboxReducer.newStateFrom(any(), MailboxEvent.ShowSpotlight)
            }
            assertEquals(SpotlightState.Shown, currentState.spotlightState)
        }
    }

    @Test
    fun `spotlight state should be hidden when an error occurs while observing the preference`() = runTest {
        // Given
        coEvery {
            observeSpotlight()
        } returns flowOf(PreferencesError.left())
        val expectedSpotlightState = SpotlightState.Hidden

        // When
        mailboxViewModel.state.test {
            // Then
            assertEquals(expectedSpotlightState, awaitItem().spotlightState)
        }
    }

    @Test
    fun `should call repository save method when closing spotlight`() = runTest {
        // Given
        coEvery {
            saveSpotlight(display = false)
        } returns Unit.right()

        // When
        mailboxViewModel.submit(MailboxViewAction.CloseSpotlight)

        // When
        mailboxViewModel.state.test {
            // Then
            coVerify { saveSpotlight(display = false) }
            assertEquals(SpotlightState.Hidden, awaitItem().spotlightState)
        }
    }

    private fun returnExpectedStateForBottomBarEvent(
        intermediateState: MailboxState? = null,
        expectedState: MailboxState
    ) {
        every {
            mailboxReducer.newStateFrom(
                intermediateState ?: any(),
                MailboxEvent.MessageBottomBarEvent(
                    BottomBarEvent.ActionsData(
                        listOf(ActionUiModelSample.Archive, ActionUiModelSample.Trash)
                    )
                )
            )
        } returns expectedState
    }

    private fun returnExpectedStateForMarkAsUnread(
        intermediateState: MailboxState,
        expectedState: MailboxState
    ) {
        every {
            mailboxReducer.newStateFrom(intermediateState, MailboxViewAction.MarkAsUnread)
        } returns expectedState
    }

    private fun returnExpectedStateWhenEnterSelectionMode(
        initialState: MailboxState,
        item: MailboxItemUiModel,
        intermediateState: MailboxState
    ) {
        every {
            mailboxReducer.newStateFrom(initialState, MailboxEvent.EnterSelectionMode(item))
        } returns intermediateState
    }

    private fun expectedSelectedLabelCountStateChange(initialState: MailboxState) {
        every {
            mailboxReducer.newStateFrom(any(), MailboxEvent.SelectedLabelCountChanged(5))
        } returns initialState
    }

    private fun expectViewMode(viewMode: ViewMode) {
        every { observeCurrentViewMode(any()) } returns flowOf(viewMode)
    }

    private fun returnExpectedStateForMarkAsRead(
        intermediateState: MailboxState,
        expectedState: MailboxState
    ) {
        every { mailboxReducer.newStateFrom(intermediateState, MailboxViewAction.MarkAsRead) } returns expectedState
    }

    private fun expectMarkConversationsAsReadSucceeds(userId: UserId, items: List<MailboxItemUiModel>) {
        coEvery {
            markConversationsAsRead(userId, items.map { ConversationId(it.id) })
        } returns emptyList<DomainConversation>().right()
    }

    private fun expectMarkConversationsAsUnreadSucceeds(userId: UserId, items: List<MailboxItemUiModel>) {
        coEvery {
            markConversationsAsUnread(userId, items.map { ConversationId(it.id) })
        } returns emptyList<DomainConversation>().right()
    }

    private fun expectMarkMessagesAsReadSucceeds(userId: UserId, items: List<MailboxItemUiModel>) {
        coEvery {
            markMessagesAsRead(userId, items.map { MessageId(it.id) })
        } returns emptyList<DomainMessage>().right()
    }

    private fun expectMarkMessagesAsUnreadSucceeds(userId: UserId, items: List<MailboxItemUiModel>) {
        coEvery {
            markMessagesAsUnread(userId, items.map { MessageId(it.id) })
        } returns emptyList<DomainMessage>().right()
    }
}
