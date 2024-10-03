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

import android.app.Activity
import android.util.Log
import androidx.paging.PagingData
import app.cash.turbine.test
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.Action
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailcommon.domain.model.PreferencesError
import ch.protonmail.android.mailcommon.domain.sample.ConversationIdSample
import ch.protonmail.android.mailcommon.domain.sample.LabelIdSample
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.mapper.ActionUiModelMapper
import ch.protonmail.android.mailcommon.presentation.model.ActionUiModel
import ch.protonmail.android.mailcommon.presentation.model.BottomBarEvent
import ch.protonmail.android.mailcommon.presentation.model.BottomBarState
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.sample.ActionUiModelSample
import ch.protonmail.android.mailcommon.presentation.ui.delete.DeleteDialogState
import ch.protonmail.android.mailcontact.domain.usecase.GetContacts
import ch.protonmail.android.mailconversation.domain.entity.ConversationWithLabels
import ch.protonmail.android.mailconversation.domain.sample.ConversationSample
import ch.protonmail.android.mailconversation.domain.usecase.DeleteConversations
import ch.protonmail.android.mailconversation.domain.usecase.GetConversationsWithLabels
import ch.protonmail.android.mailconversation.domain.usecase.MarkConversationsAsRead
import ch.protonmail.android.mailconversation.domain.usecase.MarkConversationsAsUnread
import ch.protonmail.android.mailconversation.domain.usecase.MoveConversations
import ch.protonmail.android.mailconversation.domain.usecase.ObserveClearConversationOperation
import ch.protonmail.android.mailconversation.domain.usecase.StarConversations
import ch.protonmail.android.mailconversation.domain.usecase.UnStarConversations
import ch.protonmail.android.maillabel.domain.SelectedMailLabelId
import ch.protonmail.android.maillabel.domain.model.MailLabel
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.maillabel.domain.model.MailLabelId.System.Archive
import ch.protonmail.android.maillabel.domain.model.MailLabels
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.maillabel.domain.usecase.ObserveCustomMailLabels
import ch.protonmail.android.maillabel.domain.usecase.ObserveExclusiveDestinationMailLabels
import ch.protonmail.android.maillabel.domain.usecase.ObserveMailLabels
import ch.protonmail.android.maillabel.presentation.MailLabelUiModel
import ch.protonmail.android.maillabel.presentation.model.LabelSelectedState
import ch.protonmail.android.maillabel.presentation.model.LabelUiModelWithSelectedState
import ch.protonmail.android.maillabel.presentation.text
import ch.protonmail.android.maillabel.presentation.toCustomUiModel
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemId
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemType.Conversation
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemType.Message
import ch.protonmail.android.mailmailbox.domain.model.OpenMailboxItemRequest
import ch.protonmail.android.mailmailbox.domain.model.StorageLimitPreference
import ch.protonmail.android.mailmailbox.domain.model.UserAccountStorageStatus
import ch.protonmail.android.mailmailbox.domain.usecase.GetMailboxActions
import ch.protonmail.android.mailmailbox.domain.usecase.ObserveCurrentViewMode
import ch.protonmail.android.mailmailbox.domain.usecase.ObservePrimaryUserAccountStorageStatus
import ch.protonmail.android.mailmailbox.domain.usecase.ObserveStorageLimitPreference
import ch.protonmail.android.mailmailbox.domain.usecase.ObserveUnreadCounters
import ch.protonmail.android.mailmailbox.domain.usecase.RecordRatingBoosterTriggered
import ch.protonmail.android.mailmailbox.domain.usecase.RelabelConversations
import ch.protonmail.android.mailmailbox.domain.usecase.RelabelMessages
import ch.protonmail.android.mailmailbox.domain.usecase.SaveStorageLimitPreference
import ch.protonmail.android.mailmailbox.domain.usecase.ShouldShowRatingBooster
import ch.protonmail.android.mailmailbox.presentation.helper.MailboxAsyncPagingDataDiffer
import ch.protonmail.android.mailmailbox.presentation.mailbox.MailboxViewModel
import ch.protonmail.android.mailmailbox.presentation.mailbox.mapper.MailboxItemUiModelMapper
import ch.protonmail.android.mailmailbox.presentation.mailbox.mapper.SwipeActionsMapper
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxEvent
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxItemUiModel
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxListState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxOperation
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxTopAppBarState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxViewAction
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.StorageLimitState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.SwipeActionsUiModel
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.UnreadFilterState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.UpgradeStorageState
import ch.protonmail.android.mailmailbox.presentation.mailbox.previewdata.MailboxSearchStateSampleData
import ch.protonmail.android.mailmailbox.presentation.mailbox.previewdata.MailboxStateSampleData
import ch.protonmail.android.mailmailbox.presentation.mailbox.previewdata.SwipeUiModelSampleData
import ch.protonmail.android.mailmailbox.presentation.mailbox.reducer.MailboxReducer
import ch.protonmail.android.mailmailbox.presentation.mailbox.usecase.ShowRatingBooster
import ch.protonmail.android.mailmailbox.presentation.paging.MailboxPagerFactory
import ch.protonmail.android.mailmessage.domain.model.LabelSelectionList
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.MessageWithLabels
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.mailmessage.domain.sample.MessageSample
import ch.protonmail.android.mailmessage.domain.usecase.DeleteMessages
import ch.protonmail.android.mailmessage.domain.usecase.DeleteSearchResults
import ch.protonmail.android.mailmessage.domain.usecase.GetMessagesWithLabels
import ch.protonmail.android.mailmessage.domain.usecase.MarkMessagesAsRead
import ch.protonmail.android.mailmessage.domain.usecase.MarkMessagesAsUnread
import ch.protonmail.android.mailmessage.domain.usecase.MoveMessages
import ch.protonmail.android.mailmessage.domain.usecase.ObserveClearMessageOperation
import ch.protonmail.android.mailmessage.domain.usecase.StarMessages
import ch.protonmail.android.mailmessage.domain.usecase.UnStarMessages
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.BottomSheetState
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.LabelAsBottomSheetState
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.MailboxMoreActionsBottomSheetState
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.MoveToBottomSheetState
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.UpsellingBottomSheetState
import ch.protonmail.android.mailonboarding.domain.model.OnboardingPreference
import ch.protonmail.android.mailonboarding.domain.usecase.ObserveOnboarding
import ch.protonmail.android.mailonboarding.domain.usecase.SaveOnboarding
import ch.protonmail.android.mailonboarding.presentation.model.OnboardingState
import ch.protonmail.android.mailpagination.presentation.paging.EmptyLabelId
import ch.protonmail.android.mailpagination.presentation.paging.EmptyLabelInProgressSignal
import ch.protonmail.android.mailsettings.domain.model.FolderColorSettings
import ch.protonmail.android.mailsettings.domain.model.SwipeActionsPreference
import ch.protonmail.android.mailsettings.domain.usecase.ObserveFolderColorSettings
import ch.protonmail.android.mailsettings.domain.usecase.ObserveSwipeActionsPreference
import ch.protonmail.android.mailupselling.presentation.usecase.ObserveUpsellingVisibility
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
import ch.protonmail.android.testdata.maillabel.MailLabelUiModelTestData
import ch.protonmail.android.testdata.user.UserIdTestData.userId
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.coVerifySequence
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkStatic
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId
import me.proton.core.mailsettings.domain.entity.SwipeAction
import me.proton.core.mailsettings.domain.entity.ViewMode
import me.proton.core.mailsettings.domain.entity.ViewMode.ConversationGrouping
import me.proton.core.mailsettings.domain.entity.ViewMode.NoConversationGrouping
import me.proton.core.plan.presentation.compose.usecase.ShouldUpgradeStorage
import me.proton.core.test.kotlin.TestDispatcherProvider
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import ch.protonmail.android.mailconversation.domain.entity.Conversation as DomainConversation
import ch.protonmail.android.mailmessage.domain.model.Message as DomainMessage

class MailboxViewModelTest {

    private val defaultFolderColorSettings = FolderColorSettings()
    private val initialLocationMailLabelId = Archive
    private val actionUiModelMapper = ActionUiModelMapper()
    private val swipeActionsMapper = SwipeActionsMapper()

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
    private val observeCustomMailLabels = mockk<ObserveCustomMailLabels>()
    private val observeDestinationMailLabels = mockk<ObserveExclusiveDestinationMailLabels>()
    private val observeSwipeActionsPreference = mockk<ObserveSwipeActionsPreference> {
        every { this@mockk(userId) } returns flowOf(SwipeActionsPreference(SwipeAction.MarkRead, SwipeAction.Archive))
    }
    private val observeConversationClearOperation = mockk<ObserveClearConversationOperation> {
        every { this@mockk(userId, any()) } returns flowOf(false)
    }
    private val observeMessageClearOperation = mockk<ObserveClearMessageOperation> {
        every { this@mockk(userId, any()) } returns flowOf(false)
    }
    private val getMessagesWithLabels = mockk<GetMessagesWithLabels>()
    private val getConversationsWithLabels = mockk<GetConversationsWithLabels>()

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

    private val observeMailboxActions = mockk<GetMailboxActions> {
        coEvery { this@mockk(any(), any()) } returns listOf(Action.Archive, Action.Trash).right()
    }

    private val observeOnboarding = mockk<ObserveOnboarding> {
        every { this@mockk() } returns flowOf(OnboardingPreference(display = false).right())
    }

    private val saveOnboarding: SaveOnboarding = mockk()

    private val markConversationsAsRead = mockk<MarkConversationsAsRead>()
    private val markConversationsAsUnread = mockk<MarkConversationsAsUnread>()
    private val markMessagesAsRead = mockk<MarkMessagesAsRead>()
    private val markMessagesAsUnread = mockk<MarkMessagesAsUnread>()
    private val moveConversations = mockk<MoveConversations>()
    private val moveMessages = mockk<MoveMessages>()
    private val deleteConversations = mockk<DeleteConversations>()
    private val deleteMessages = mockk<DeleteMessages>()
    private val relabelMessages = mockk<RelabelMessages>()
    private val relabelConversations = mockk<RelabelConversations>()
    private val starMessages = mockk<StarMessages>()
    private val starConversations = mockk<StarConversations>()
    private val unStarMessages = mockk<UnStarMessages>()
    private val unStarConversations = mockk<UnStarConversations>()
    private val deleteSearchResults = mockk<DeleteSearchResults>()
    private val observePrimaryUserAccountStorageStatus = mockk<ObservePrimaryUserAccountStorageStatus> {
        every { this@mockk() } returns flowOf()
    }
    private val observeStorageLimitPreference = mockk<ObserveStorageLimitPreference> {
        every { this@mockk() } returns flowOf()
    }
    private val saveStorageLimitPreference = mockk<SaveStorageLimitPreference> {
        coEvery { saveFirstLimitWarningPreference(any()) } returns Unit.right()
        coEvery { saveSecondLimitWarningPreference(any()) } returns Unit.right()
    }
    private val shouldUpgradeStorage = mockk<ShouldUpgradeStorage> {
        every { this@mockk() } returns flowOf()
    }
    private val shouldShowRatingBooster = mockk<ShouldShowRatingBooster> {
        every { this@mockk(userId) } returns flowOf(false)
    }
    private val showRatingBooster = mockk<ShowRatingBooster>(relaxUnitFun = true)
    private val recordRatingBoosterTriggered = mockk<RecordRatingBoosterTriggered>(relaxUnitFun = true)
    private val observeUpsellingVisibility = mockk<ObserveUpsellingVisibility> {
        every { this@mockk.invoke(any()) } returns flowOf(false)
    }
    private val emptyLabelInProgressSignal = mockk<EmptyLabelInProgressSignal>()

    private val mailboxViewModel by lazy {
        MailboxViewModel(
            mailboxPagerFactory = pagerFactory,
            observeCurrentViewMode = observeCurrentViewMode,
            observePrimaryUserId = observePrimaryUserId,
            observeMailLabels = observeMailLabels,
            observeCustomMailLabels = observeCustomMailLabels,
            observeDestinationMailLabels = observeDestinationMailLabels,
            observeSwipeActionsPreference = observeSwipeActionsPreference,
            observeClearMessageOperation = observeMessageClearOperation,
            observeClearConversationOperation = observeConversationClearOperation,
            selectedMailLabelId = selectedMailLabelId,
            observeUnreadCounters = observeUnreadCounters,
            observeFolderColorSettings = observeFolderColorSettings,
            getMessagesWithLabels = getMessagesWithLabels,
            getConversationsWithLabels = getConversationsWithLabels,
            getMailboxActions = observeMailboxActions,
            actionUiModelMapper = actionUiModelMapper,
            mailboxItemMapper = mailboxItemMapper,
            swipeActionsMapper = swipeActionsMapper,
            getContacts = getContacts,
            markConversationsAsRead = markConversationsAsRead,
            markConversationsAsUnread = markConversationsAsUnread,
            markMessagesAsRead = markMessagesAsRead,
            markMessagesAsUnread = markMessagesAsUnread,
            relabelMessages = relabelMessages,
            relabelConversations = relabelConversations,
            moveConversations = moveConversations,
            moveMessages = moveMessages,
            deleteConversations = deleteConversations,
            deleteMessages = deleteMessages,
            starMessages = starMessages,
            starConversations = starConversations,
            unStarMessages = unStarMessages,
            unStarConversations = unStarConversations,
            mailboxReducer = mailboxReducer,
            dispatchersProvider = TestDispatcherProvider(),
            observeOnboarding = observeOnboarding,
            saveOnboarding = saveOnboarding,
            deleteSearchResults = deleteSearchResults,
            observePrimaryUserAccountStorageStatus = observePrimaryUserAccountStorageStatus,
            observeStorageLimitPreference = observeStorageLimitPreference,
            saveStorageLimitPreference = saveStorageLimitPreference,
            shouldUpgradeStorage = shouldUpgradeStorage,
            shouldShowRatingBooster = shouldShowRatingBooster,
            showRatingBooster = showRatingBooster,
            recordRatingBoosterTriggered = recordRatingBoosterTriggered,
            observeUpsellingVisibility = observeUpsellingVisibility,
            emptyLabelInProgressSignal = emptyLabelInProgressSignal
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
                mailboxListState = MailboxListState.Loading,
                topAppBarState = MailboxTopAppBarState.Loading,
                upgradeStorageState = UpgradeStorageState(false),
                unreadFilterState = UnreadFilterState.Loading,
                bottomAppBarState = BottomBarState.Data.Hidden(emptyList<ActionUiModel>().toImmutableList()),
                onboardingState = OnboardingState.Hidden,
                deleteDialogState = DeleteDialogState.Hidden,
                deleteAllDialogState = DeleteDialogState.Hidden,
                storageLimitState = StorageLimitState.None,
                bottomSheetState = null,
                actionResult = Effect.empty(),
                error = Effect.empty(),
                showRatingBooster = Effect.empty(),
                showOnboardingUpselling = Effect.empty()
            )

            assertEquals(expected, actual)

            verify { pagerFactory wasNot Called }
        }
    }

    @Test
    fun `emits storage limit state when storage status and limit preferences are read`() = runTest {
        // Given
        val expectedStorageLimitState = MailboxStateSampleData.Loading.storageLimitState
        val userAccountStorageStatus = UserAccountStorageStatus(5_000L, 10_000L)
        val storageLimitPreference = StorageLimitPreference(
            firstLimitWarningConfirmed = false,
            secondLimitWarningConfirmed = false
        )
        coEvery { observePrimaryUserAccountStorageStatus() } returns flowOf(userAccountStorageStatus)
        coEvery { observeStorageLimitPreference() } returns flowOf(storageLimitPreference.right())

        // When
        mailboxViewModel.state.test {
            // Then
            val actual = awaitItem()
            assertEquals(expectedStorageLimitState, actual.storageLimitState)
        }
    }

    @Test
    fun `emits new storage limit state when user confirms first limit with do not remind again`() = runTest {
        // Given
        val initialState = MailboxStateSampleData.Loading.copy(
            storageLimitState = StorageLimitState.Notifiable.FirstLimitOver(false)
        )
        val expectedState = MailboxStateSampleData.Loading.copy(
            storageLimitState = StorageLimitState.Notifiable.FirstLimitOver(true)
        )
        val userAccountStorageStatus = UserAccountStorageStatus(8_001L, 10_000L)
        val storageLimitPreference = StorageLimitPreference(
            firstLimitWarningConfirmed = false,
            secondLimitWarningConfirmed = false
        )
        coEvery { observePrimaryUserAccountStorageStatus() } returns flowOf(userAccountStorageStatus)
        coEvery { observeStorageLimitPreference() } returns flowOf(storageLimitPreference.right())
        coEvery { saveStorageLimitPreference.saveFirstLimitWarningPreference(true) } returns Unit.right()
        coEvery { saveStorageLimitPreference.saveSecondLimitWarningPreference(any()) } returns Unit.right()

        every {
            mailboxReducer.newStateFrom(
                any(),
                MailboxEvent.StorageLimitStatusChanged(userAccountStorageStatus, storageLimitPreference)
            )
        } returns initialState
        every {
            mailboxReducer.newStateFrom(
                any(),
                MailboxViewAction.StorageLimitDoNotRemind
            )
        } returns expectedState
        // When
        mailboxViewModel.state.test {
            // Then
            assertEquals(initialState, awaitItem())

            // When
            mailboxViewModel.submit(MailboxViewAction.StorageLimitDoNotRemind)

            // Then
            assertEquals(expectedState, awaitItem())
            coVerify { saveStorageLimitPreference.saveFirstLimitWarningPreference(true) }
            coVerify(exactly = 0) { saveStorageLimitPreference.saveSecondLimitWarningPreference(any()) }
        }
    }

    @Test
    fun `emits new storage limit state when user confirms second limit with do not remind again`() = runTest {
        // Given
        val initialState = MailboxStateSampleData.Loading.copy(
            storageLimitState = StorageLimitState.Notifiable.SecondLimitOver(false)
        )
        val expectedState = MailboxStateSampleData.Loading.copy(
            storageLimitState = StorageLimitState.Notifiable.SecondLimitOver(true)
        )
        val userAccountStorageStatus = UserAccountStorageStatus(9_001L, 10_000L)
        val storageLimitPreference = StorageLimitPreference(
            firstLimitWarningConfirmed = false,
            secondLimitWarningConfirmed = false
        )
        coEvery { observePrimaryUserAccountStorageStatus() } returns flowOf(userAccountStorageStatus)
        coEvery { observeStorageLimitPreference() } returns flowOf(storageLimitPreference.right())
        coEvery { saveStorageLimitPreference.saveFirstLimitWarningPreference(any()) } returns Unit.right()
        coEvery { saveStorageLimitPreference.saveSecondLimitWarningPreference(true) } returns Unit.right()

        every {
            mailboxReducer.newStateFrom(
                any(),
                MailboxEvent.StorageLimitStatusChanged(userAccountStorageStatus, storageLimitPreference)
            )
        } returns initialState
        every {
            mailboxReducer.newStateFrom(
                any(),
                MailboxViewAction.StorageLimitDoNotRemind
            )
        } returns expectedState
        // When
        mailboxViewModel.state.test {
            // Then
            assertEquals(initialState, awaitItem())

            // When
            mailboxViewModel.submit(MailboxViewAction.StorageLimitDoNotRemind)

            // Then
            assertEquals(expectedState, awaitItem())
            coVerify { saveStorageLimitPreference.saveSecondLimitWarningPreference(true) }
            coVerify(exactly = 0) { saveStorageLimitPreference.saveFirstLimitWarningPreference(any()) }
        }
    }

    @Test
    fun `emits new storage limit state when user confirms with ok button`() = runTest {
        // Given
        val initialState = MailboxStateSampleData.Loading.copy(
            storageLimitState = StorageLimitState.Notifiable.QuotaOver(false)
        )
        val expectedState = MailboxStateSampleData.Loading.copy(
            storageLimitState = StorageLimitState.Notifiable.QuotaOver(true)
        )
        val userAccountStorageStatus = UserAccountStorageStatus(10_001L, 10_000L)
        val storageLimitPreference = StorageLimitPreference(
            firstLimitWarningConfirmed = false,
            secondLimitWarningConfirmed = false
        )
        coEvery { observePrimaryUserAccountStorageStatus() } returns flowOf(userAccountStorageStatus)
        coEvery { observeStorageLimitPreference() } returns flowOf(storageLimitPreference.right())
        every {
            mailboxReducer.newStateFrom(
                any(),
                MailboxEvent.StorageLimitStatusChanged(userAccountStorageStatus, storageLimitPreference)
            )
        } returns initialState
        every {
            mailboxReducer.newStateFrom(
                any(),
                MailboxViewAction.StorageLimitConfirmed
            )
        } returns expectedState
        // When
        mailboxViewModel.state.test {
            // Then
            assertEquals(initialState, awaitItem())

            // When
            mailboxViewModel.submit(MailboxViewAction.StorageLimitConfirmed)

            // Then
            assertEquals(expectedState, awaitItem())
        }
    }


    @Test
    fun `revokes first storage limit confirmation when storage status is below first limit`() = runTest {
        // Given
        val initialState = MailboxStateSampleData.Loading.copy(
            storageLimitState = StorageLimitState.Notifiable.FirstLimitOver(true)
        )
        val userAccountStorageStatus = UserAccountStorageStatus(5_000L, 10_000L)
        val storageLimitPreference = StorageLimitPreference(
            firstLimitWarningConfirmed = true,
            secondLimitWarningConfirmed = false
        )
        coEvery { observePrimaryUserAccountStorageStatus() } returns flowOf(userAccountStorageStatus)
        coEvery { observeStorageLimitPreference() } returns flowOf(storageLimitPreference.right())
        coEvery { saveStorageLimitPreference.saveFirstLimitWarningPreference(false) } returns Unit.right()

        every {
            mailboxReducer.newStateFrom(
                any(),
                MailboxEvent.StorageLimitStatusChanged(userAccountStorageStatus, storageLimitPreference)
            )
        } returns initialState

        // When
        mailboxViewModel.state.test {
            // Then
            assertEquals(initialState, awaitItem())
            coVerify { saveStorageLimitPreference.saveFirstLimitWarningPreference(false) }
        }
    }


    @Test
    fun `revokes second storage limit confirmation when storage status is below second limit`() = runTest {
        // Given
        val initialState = MailboxStateSampleData.Loading.copy(
            storageLimitState = StorageLimitState.Notifiable.SecondLimitOver(true)
        )
        val userAccountStorageStatus = UserAccountStorageStatus(5_000L, 10_000L)
        val storageLimitPreference = StorageLimitPreference(
            firstLimitWarningConfirmed = true,
            secondLimitWarningConfirmed = true
        )
        coEvery { observePrimaryUserAccountStorageStatus() } returns flowOf(userAccountStorageStatus)
        coEvery { observeStorageLimitPreference() } returns flowOf(storageLimitPreference.right())
        coEvery { saveStorageLimitPreference.saveSecondLimitWarningPreference(false) } returns Unit.right()

        every {
            mailboxReducer.newStateFrom(
                any(),
                MailboxEvent.StorageLimitStatusChanged(userAccountStorageStatus, storageLimitPreference)
            )
        } returns initialState

        // When
        mailboxViewModel.state.test {
            // Then
            assertEquals(initialState, awaitItem())
            coVerify { saveStorageLimitPreference.saveSecondLimitWarningPreference(false) }
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
            awaitItem() // swipe gestures
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
                ).toImmutableList()
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

            awaitItem() // swipe gestures

            verifyOrder {
                mailboxReducer.newStateFrom(any(), MailboxEvent.NewLabelSelected(expectedMailLabel, expectedCount))
                mailboxReducer.newStateFrom(
                    currentState = intermediateState,
                    operation = MailboxEvent.MessageBottomBarEvent(
                        BottomBarEvent.ActionsData(
                            listOf(ActionUiModelSample.Archive, ActionUiModelSample.Trash)
                                .toImmutableList()
                        )
                    )
                )
            }
        }
    }

    @Test
    fun `when selected label changes, new state is created and emitted`() = runTest {
        // Given
        val initialMailLabel = MailLabelTestData.customLabelOne
        val modifiedMailLabel = MailLabelTestData.customLabelTwo
        val expectedState = MailboxStateSampleData.Loading.copy(
            mailboxListState = MailboxListState.Data.ViewMode(
                currentMailLabel = modifiedMailLabel,
                openItemEffect = Effect.empty(),
                scrollToMailboxTop = Effect.of(initialMailLabel.id),
                offlineEffect = Effect.empty(),
                refreshErrorEffect = Effect.empty(),
                refreshRequested = false,
                swipeActions = null,
                searchState = MailboxSearchStateSampleData.NotSearching,
                showOnboardingUpselling = Effect.empty(),
                clearState = MailboxListState.Data.ClearState.Hidden
            )
        )
        val expectedSwipeActions = SwipeActionsUiModel(
            end = SwipeUiModelSampleData.MarkRead,
            start = SwipeUiModelSampleData.Archive
        )
        val expectedStateWithSwipeGestures = expectedState.copy(
            mailboxListState = MailboxListState.Data.ViewMode(
                currentMailLabel = modifiedMailLabel,
                openItemEffect = Effect.empty(),
                scrollToMailboxTop = Effect.of(initialMailLabel.id),
                offlineEffect = Effect.empty(),
                refreshErrorEffect = Effect.empty(),
                refreshRequested = false,
                swipeActions = expectedSwipeActions,
                searchState = MailboxSearchStateSampleData.NotSearching,
                showOnboardingUpselling = Effect.empty(),
                clearState = MailboxListState.Data.ClearState.Hidden
            )
        )
        val expectedStateAfterClearAllStatus = expectedStateWithSwipeGestures.copy(
            mailboxListState = MailboxListState.Data.ViewMode(
                currentMailLabel = modifiedMailLabel,
                openItemEffect = Effect.empty(),
                scrollToMailboxTop = Effect.of(initialMailLabel.id),
                offlineEffect = Effect.empty(),
                refreshErrorEffect = Effect.empty(),
                refreshRequested = false,
                swipeActions = expectedSwipeActions,
                searchState = MailboxSearchStateSampleData.NotSearching,
                showOnboardingUpselling = Effect.empty(),
                clearState = MailboxListState.Data.ClearState.Visible.Button(TextUiModel("Clear All"))
            )
        )
        val mailLabelsFlow = MutableStateFlow(
            MailLabels(
                systemLabels = LabelTestData.systemLabels,
                folders = emptyList(),
                labels = listOf(
                    MailLabelTestData.customLabelOne,
                    MailLabelTestData.customLabelTwo
                )
            )
        )
        val currentLocationFlow = MutableStateFlow<MailLabelId>(initialMailLabel.id)
        every { observeMailLabels(userId) } returns mailLabelsFlow
        every { selectedMailLabelId.flow } returns currentLocationFlow

        every {
            mailboxReducer.newStateFrom(any(), MailboxEvent.NewLabelSelected(modifiedMailLabel, null))
        } returns expectedState
        every {
            mailboxReducer.newStateFrom(expectedState, MailboxEvent.SwipeActionsChanged(expectedSwipeActions))
        } returns expectedStateWithSwipeGestures
        every {
            mailboxReducer.newStateFrom(expectedStateWithSwipeGestures, MailboxEvent.ClearAllOperationStatus(false))
        } returns expectedStateAfterClearAllStatus

        mailboxViewModel.state.test {
            awaitItem()

            // When
            currentLocationFlow.emit(modifiedMailLabel.id)

            // Then
            assertEquals(expectedState, awaitItem())
            assertEquals(expectedStateWithSwipeGestures, awaitItem())
            assertEquals(expectedStateAfterClearAllStatus, awaitItem())


        }
    }

    @Test
    fun `when selected label is deleted, new state is created and emitted`() = runTest {
        // Given
        val initialMailLabel = MailLabelTestData.customLabelOne
        val expectedState = MailboxStateSampleData.Loading.copy(
            mailboxListState = MailboxListState.Data.ViewMode(
                currentMailLabel = MailLabelId.System.Inbox.toMailLabel(),
                openItemEffect = Effect.empty(),
                scrollToMailboxTop = Effect.of(initialMailLabel.id),
                offlineEffect = Effect.empty(),
                refreshErrorEffect = Effect.empty(),
                refreshRequested = false,
                swipeActions = null,
                searchState = MailboxSearchStateSampleData.NotSearching,
                showOnboardingUpselling = Effect.empty(),
                clearState = MailboxListState.Data.ClearState.Hidden
            )
        )
        val mailLabelsFlow = MutableStateFlow(
            MailLabels(
                systemLabels = LabelTestData.systemLabels,
                folders = emptyList(),
                labels = listOf(MailLabelTestData.customLabelOne, MailLabelTestData.customLabelTwo)
            )
        )
        val currentLocationFlow = MutableStateFlow<MailLabelId>(initialMailLabel.id)
        every { observeMailLabels(userId) } returns mailLabelsFlow
        every { selectedMailLabelId.flow } returns currentLocationFlow
        every {
            mailboxReducer.newStateFrom(
                any(),
                MailboxEvent.SelectedLabelChanged(MailLabelId.System.Inbox.toMailLabel())
            )
        } returns expectedState

        mailboxViewModel.state.test {
            awaitItem()

            mailLabelsFlow.emit(
                MailLabels(
                    systemLabels = LabelTestData.systemLabels,
                    folders = emptyList(),
                    labels = listOf(MailLabelTestData.customLabelTwo)
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
        coEvery { observeUnreadCounters(userId) } returns currentCountersFlow
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
        coEvery { observeUnreadCounters(userId) } returns currentCountersFlow
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
                    listOf(ActionUiModelSample.Archive, ActionUiModelSample.Trash).toImmutableList()
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
                    listOf(ActionUiModelSample.Archive, ActionUiModelSample.Trash).toImmutableList()
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
        val expectedBottomBarActions = listOf(ActionUiModelSample.Archive, ActionUiModelSample.Trash).toImmutableList()
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

        val expectedBottomBarActions = listOf(ActionUiModelSample.Archive, ActionUiModelSample.Trash).toImmutableList()

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
        every {
            pagerFactory.create(
                userIds,
                any(),
                false,
                Message,
                any(),
                emptyLabelInProgressSignal
            )
        } returns mockk mockPager@{
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
            verify {
                pagerFactory.create(
                    userIds,
                    initialLocationMailLabelId,
                    false,
                    Message,
                    any(),
                    emptyLabelInProgressSignal
                )
            }

            // When
            currentLocationFlow.emit(MailLabelId.System.Spam)

            // Then
            awaitItem()
            verify {
                pagerFactory.create(
                    userIds,
                    MailLabelId.System.Spam,
                    false,
                    Message,
                    any(),
                    emptyLabelInProgressSignal
                )
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `mailbox items are mapped to mailbox item ui models`() = runTest {
        // Given
        coEvery {
            mailboxItemMapper.toUiModel(unreadMailboxItem, ContactTestData.contacts, defaultFolderColorSettings, false)
        } returns unreadMailboxItemUiModel
        coEvery {
            mailboxItemMapper.toUiModel(readMailboxItem, ContactTestData.contacts, defaultFolderColorSettings, false)
        } returns readMailboxItemUiModel
        every { pagerFactory.create(any(), any(), any(), any(), any(), emptyLabelInProgressSignal) } returns mockk {
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
                folderColorSettings = defaultFolderColorSettings,
                isShowingSearchResults = false
            )
        } returns unreadMailboxItemUiModelWithLabelColored
        coEvery {
            mailboxItemMapper.toUiModel(
                unreadMailboxItemWithLabel,
                ContactTestData.contacts,
                updatedFolderColorSetting,
                false
            )
        } returns unreadMailboxItemUiModelWithLabel

        every {
            pagerFactory.create(
                listOf(userId),
                Archive,
                false,
                any(),
                any(),
                emptyLabelInProgressSignal
            )
        } returns mockk {
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
            mailboxItemMapper.toUiModel(unreadMailboxItem, ContactTestData.contacts, defaultFolderColorSettings, false)
        } returns unreadMailboxItemUiModel
        coEvery {
            mailboxItemMapper.toUiModel(readMailboxItem, ContactTestData.contacts, defaultFolderColorSettings, false)
        } returns readMailboxItemUiModel
        every { pagerFactory.create(any(), any(), any(), any(), any(), emptyLabelInProgressSignal) } returns mockk {
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

            coVerify { mailboxItemMapper.toUiModel(any(), ContactTestData.contacts, defaultFolderColorSettings, false) }
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
                swipeActions = null,
                searchState = MailboxSearchStateSampleData.NotSearching,
                showOnboardingUpselling = Effect.empty(),
                clearState = MailboxListState.Data.ClearState.Hidden
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
                swipeActions = null,
                searchState = MailboxSearchStateSampleData.NotSearching,
                showOnboardingUpselling = Effect.empty(),
                clearState = MailboxListState.Data.ClearState.Hidden
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
                swipeActions = null,
                searchState = MailboxSearchStateSampleData.NotSearching,
                showOnboardingUpselling = Effect.empty(),
                clearState = MailboxListState.Data.ClearState.Hidden
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
    fun `mailbox pager is recreated when unread filter state changes`() = runTest {
        // Given
        val expectedMailBoxState = createMailboxDataState(unreadFilterState = false)
        every { mailboxReducer.newStateFrom(any(), any()) } returns expectedMailBoxState
        every {
            pagerFactory.create(
                listOf(userId),
                Archive,
                any(),
                Message,
                any(),
                emptyLabelInProgressSignal
            )
        } returns mockk mockPager@{
            every { this@mockPager.flow } returns flowOf(PagingData.from(listOf(unreadMailboxItem)))
        }
        every { mailboxReducer.newStateFrom(expectedMailBoxState, MailboxViewAction.EnableUnreadFilter) } returns
            createMailboxDataState(unreadFilterState = true)

        mailboxViewModel.items.test {
            // When

            // Then
            awaitItem()
            verify(exactly = 1) {
                pagerFactory.create(
                    listOf(userId),
                    Archive,
                    false,
                    Message,
                    any(),
                    emptyLabelInProgressSignal
                )
            }

            // When
            mailboxViewModel.submit(MailboxViewAction.EnableUnreadFilter)

            // Then
            awaitItem()
            verify(exactly = 1) {
                pagerFactory.create(
                    listOf(userId),
                    Archive,
                    true,
                    Message,
                    any(),
                    emptyLabelInProgressSignal
                )
            }
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
        every {
            pagerFactory.create(
                any(),
                any(),
                any(),
                any(),
                any(),
                emptyLabelInProgressSignal
            )
        } returns mockk mockPager@{
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
            verify(exactly = 1) {
                pagerFactory.create(
                    listOf(userId),
                    Archive,
                    false,
                    Message,
                    any(),
                    emptyLabelInProgressSignal
                )
            }

            // When
            currentLocationFlow.emit(inboxLabel)

            // Then
            awaitItem()
            // mailbox pager is recreated only once when view mode for the newly selected location does not change
            verify(exactly = 1) {
                pagerFactory.create(
                    listOf(userId),
                    inboxLabel,
                    false,
                    Message,
                    any(),
                    emptyLabelInProgressSignal
                )
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `pager is not recreated when any state beside selectedLabel, unreadFilter, viewMode or primaryUser changes`() =
        runTest {
            // Given
            val expectedMailBoxState = createMailboxDataState(Effect.empty())
            every { mailboxReducer.newStateFrom(any(), any()) } returns expectedMailBoxState
            every {
                pagerFactory.create(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    emptyLabelInProgressSignal
                )
            } returns mockk mockPager@{
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
                verify(exactly = 1) {
                    pagerFactory.create(
                        listOf(userId),
                        Archive,
                        false,
                        Message,
                        any(),
                        emptyLabelInProgressSignal
                    )
                }

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
        every {
            pagerFactory.create(
                any(),
                any(),
                any(),
                any(),
                any(),
                emptyLabelInProgressSignal
            )
        } returns mockk mockPager@{
            every { this@mockPager.flow } returns flowOf(PagingData.from(listOf(unreadMailboxItem)))
        }

        mailboxViewModel.items.test {
            // When
            awaitItem()
            // Then
            verify(exactly = 1) {
                pagerFactory.create(
                    listOf(userId),
                    Archive,
                    false,
                    Message,
                    any(),
                    emptyLabelInProgressSignal
                )
            }
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
            expectViewModeForCurrentLocation(ConversationGrouping)
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
            expectViewModeForCurrentLocation(ConversationGrouping)
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
            expectViewModeForCurrentLocation(NoConversationGrouping)
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
            expectViewModeForCurrentLocation(NoConversationGrouping)
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
    fun `when trash is triggered for conversation grouping then move conversations is called`() = runTest {
        // Given
        val item = readMailboxItemUiModel
        val secondItem = unreadMailboxItemUiModel
        val initialState = createMailboxDataState()
        val intermediateState = MailboxStateSampleData.createSelectionMode(listOf(item, secondItem))
        expectViewModeForCurrentLocation(ConversationGrouping)
        expectedSelectedLabelCountStateChange(initialState)
        returnExpectedStateWhenEnterSelectionMode(initialState, item, intermediateState)
        returnExpectedStateForBottomBarEvent(expectedState = intermediateState)
        returnExpectedStateForTrash(intermediateState, initialState, 2)
        expectMoveConversationsSucceeds(userId, listOf(item, secondItem), SystemLabelId.Trash.labelId)

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
            mailboxViewModel.submit(MailboxViewAction.Trash)

            // Then
            assertEquals(initialState, awaitItem())
            coVerify(exactly = 1) {
                moveConversations(
                    userId,
                    listOf(ConversationId(item.id), ConversationId(secondItem.id)),
                    SystemLabelId.Trash.labelId
                )
            }
            coVerify { moveMessages wasNot Called }
        }
    }

    @Test
    fun `when trash is triggered for no conversation grouping then move messages is called `() = runTest {
        val item = readMailboxItemUiModel
        val secondItem = unreadMailboxItemUiModel
        val initialState = createMailboxDataState()
        val intermediateState = MailboxStateSampleData.createSelectionMode(listOf(item, secondItem))
        expectViewModeForCurrentLocation(NoConversationGrouping)
        expectedSelectedLabelCountStateChange(initialState)
        returnExpectedStateWhenEnterSelectionMode(initialState, item, intermediateState)
        returnExpectedStateForBottomBarEvent(expectedState = intermediateState)
        returnExpectedStateForTrash(intermediateState, initialState, 2)
        expectMoveMessagesSucceeds(userId, listOf(item, secondItem), SystemLabelId.Trash.labelId)

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
            mailboxViewModel.submit(MailboxViewAction.Trash)

            // Then
            assertEquals(initialState, awaitItem())
            coVerify(exactly = 1) {
                moveMessages(userId, listOf(MessageId(item.id), MessageId(secondItem.id)), SystemLabelId.Trash.labelId)
            }
            coVerify { moveConversations wasNot Called }
        }
    }

    @Test
    fun `when delete is triggered for conversation grouping then delete conversations is called `() = runTest {
        // Given
        val item = readMailboxItemUiModel
        val secondItem = unreadMailboxItemUiModel
        val initialState = createMailboxDataState()
        val intermediateState = MailboxStateSampleData.createSelectionMode(
            listOf(item, secondItem),
            currentMailLabel = MailLabel.System(MailLabelId.System.Trash)
        )
        expectViewModeForCurrentLocation(ConversationGrouping)
        expectedSelectedLabelCountStateChange(initialState)
        returnExpectedStateWhenEnterSelectionMode(initialState, item, intermediateState)
        returnExpectedStateForBottomBarEvent(expectedState = intermediateState)
        returnExpectedStateForDeleteConfirmed(intermediateState, initialState, ConversationGrouping, 2)
        expectDeleteConversationsSucceeds(userId, listOf(item, secondItem), SystemLabelId.Trash.labelId)

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
            mailboxViewModel.submit(MailboxViewAction.DeleteConfirmed)

            // Then
            assertEquals(initialState, awaitItem())
            coVerify(exactly = 1) {
                deleteConversations(
                    userId,
                    listOf(ConversationId(item.id), ConversationId(secondItem.id)),
                    SystemLabelId.Trash.labelId
                )
            }
            coVerify { deleteMessages wasNot Called }
        }
    }

    @Test
    fun `when delete is triggered for no conversation grouping then delete messages is called`() = runTest {
        val item = readMailboxItemUiModel.copy(type = Message)
        val secondItem = unreadMailboxItemUiModel.copy(type = Message)
        val initialState = createMailboxDataState()
        val intermediateState = MailboxStateSampleData.createSelectionMode(
            listOf(item, secondItem),
            currentMailLabel = MailLabel.System(MailLabelId.System.Trash)
        )
        expectedSelectedLabelCountStateChange(initialState)
        returnExpectedStateWhenEnterSelectionMode(initialState, item, intermediateState)
        returnExpectedStateForBottomBarEvent(expectedState = intermediateState)
        returnExpectedStateForDeleteConfirmed(intermediateState, initialState, NoConversationGrouping, 2)
        expectDeleteMessagesSucceeds(userId, listOf(item, secondItem), SystemLabelId.Trash.labelId)

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
            mailboxViewModel.submit(MailboxViewAction.DeleteConfirmed)

            // Then
            assertEquals(initialState, awaitItem())
            coVerify(exactly = 1) {
                deleteMessages(
                    userId,
                    listOf(MessageId(item.id), MessageId(secondItem.id)),
                    SystemLabelId.Trash.labelId
                )
            }
            coVerify { deleteConversations wasNot Called }
        }
    }

    @Test
    fun `when delete all is triggered from trash for no conversation grouping, then reducer is called`() = runTest {
        // Given
        val currentMailLabel = MailLabelId.System.Trash
        val initialState = createMailboxDataState(selectedMailLabelId = currentMailLabel)
        every { selectedMailLabelId.flow } returns MutableStateFlow(currentMailLabel)
        expectedSelectedLabelCountStateChange(initialState)
        expectViewModeForCurrentLocation(NoConversationGrouping)

        mailboxViewModel.state.test {
            awaitItem() // First emission for selected user

            // When
            mailboxViewModel.submit(MailboxViewAction.DeleteAll)

            // Then
            coVerify {
                mailboxReducer.newStateFrom(
                    any(),
                    MailboxEvent.DeleteAll(NoConversationGrouping, currentMailLabel.labelId)
                )
                deleteMessages wasNot Called
                deleteConversations wasNot Called
            }
        }
    }

    @Test
    fun `when delete all is triggered from spam for no conversation grouping, then reducer is called`() = runTest {
        // Given
        val currentMailLabel = MailLabelId.System.Spam
        val initialState = createMailboxDataState(selectedMailLabelId = currentMailLabel)
        every { selectedMailLabelId.flow } returns MutableStateFlow(currentMailLabel)
        expectedSelectedLabelCountStateChange(initialState)
        expectViewModeForCurrentLocation(NoConversationGrouping)

        mailboxViewModel.state.test {
            awaitItem() // First emission for selected user

            // When
            mailboxViewModel.submit(MailboxViewAction.DeleteAll)

            // Then
            coVerify {
                mailboxReducer.newStateFrom(
                    any(),
                    MailboxEvent.DeleteAll(NoConversationGrouping, currentMailLabel.labelId)
                )
                deleteMessages wasNot Called
                deleteConversations wasNot Called
            }
        }
    }

    @Test
    fun `when delete all is triggered from non spam or trash location, then no new state is emitted`() = runTest {
        // Given
        val initialState = createMailboxDataState(selectedMailLabelId = MailLabelId.System.Inbox)
        every { selectedMailLabelId.flow } returns MutableStateFlow(MailLabelId.System.Inbox)
        expectedSelectedLabelCountStateChange(initialState)

        mailboxViewModel.state.test {
            awaitItem() // First emission for selected user

            // When
            mailboxViewModel.submit(MailboxViewAction.DeleteAll)

            // Then
            coVerify(exactly = 0) { mailboxReducer.newStateFrom(any(), MailboxViewAction.DeleteAll) }
            coVerify {
                deleteMessages wasNot Called
                deleteConversations wasNot Called
            }
        }
    }

    @Test
    fun `when delete all is triggered for no conversation grouping then delete messages is called with label`() =
        runTest {
            // Given
            val initialState = createMailboxDataState(selectedMailLabelId = MailLabelId.System.Trash)
            val emptyLabelId = EmptyLabelId(SystemLabelId.Trash.labelId.id)
            every { selectedMailLabelId.flow } returns MutableStateFlow(MailLabelId.System.Trash)
            coEvery { emptyLabelInProgressSignal.emitOperationSignal(emptyLabelId) } just runs
            expectedSelectedLabelCountStateChange(initialState)
            expectDeleteMessagesSucceeds(userId, SystemLabelId.Trash.labelId)
            expectViewModeForCurrentLocation(NoConversationGrouping)
            returnExpectedStateForDeleteAllConfirmed(initialState, NoConversationGrouping)

            mailboxViewModel.state.test {
                awaitItem() // First emission for selected user

                // When
                mailboxViewModel.submit(MailboxViewAction.DeleteAllConfirmed)

                // Then
                assertEquals(initialState, awaitItem())
                coVerify(exactly = 1) {
                    deleteMessages(userId, SystemLabelId.Trash.labelId)
                }
                coVerify { deleteConversations wasNot Called }
                coVerify(exactly = 1) { emptyLabelInProgressSignal.emitOperationSignal(emptyLabelId) }
                confirmVerified(emptyLabelInProgressSignal)
            }
        }

    @Test
    fun `when delete all is triggered for conversation grouping then delete conversation is called with label`() =
        runTest {
            // Given
            val initialState = createMailboxDataState(selectedMailLabelId = MailLabelId.System.Trash)
            val emptyLabelId = EmptyLabelId(SystemLabelId.Trash.labelId.id)
            every { selectedMailLabelId.flow } returns MutableStateFlow(MailLabelId.System.Trash)
            coEvery { emptyLabelInProgressSignal.emitOperationSignal(emptyLabelId) } just runs
            expectedSelectedLabelCountStateChange(initialState)
            expectDeleteConversationsSucceeds(userId, SystemLabelId.Trash.labelId)
            expectViewModeForCurrentLocation(ConversationGrouping)
            returnExpectedStateForDeleteAllConfirmed(initialState, ConversationGrouping)

            mailboxViewModel.state.test {
                awaitItem() // First emission for selected user

                // When
                mailboxViewModel.submit(MailboxViewAction.DeleteAllConfirmed)

                // Then
                assertEquals(initialState, awaitItem())
                coVerify(exactly = 1) {
                    deleteConversations(userId, SystemLabelId.Trash.labelId)
                }
                coVerify { deleteMessages wasNot Called }
                coVerify(exactly = 1) { emptyLabelInProgressSignal.emitOperationSignal(emptyLabelId) }
                confirmVerified(emptyLabelInProgressSignal)
            }
        }

    @Test
    fun `when delete all is triggered from non spam and trash location no action is performed`() = runTest {
        // Given
        val initialState = createMailboxDataState(selectedMailLabelId = MailLabelId.System.Inbox)
        every { selectedMailLabelId.flow } returns MutableStateFlow(MailLabelId.System.Inbox)
        expectedSelectedLabelCountStateChange(initialState)
        returnExpectedStateForDeleteAllDismissed(initialState)


        mailboxViewModel.state.test {
            awaitItem() // First emission for selected user

            // When
            mailboxViewModel.submit(MailboxViewAction.DeleteAllConfirmed)

            // Then
            assertEquals(initialState, awaitItem())
            coVerify {
                mailboxReducer.newStateFrom(any(), MailboxViewAction.DeleteAllDialogDismissed)
                deleteMessages wasNot Called
                deleteConversations wasNot Called
            }
        }
    }

    @Test
    fun `when bottom sheet dismissal is triggered then the label as bottom sheet is dismissed `() = runTest {
        // Given
        val item = readMailboxItemUiModel.copy(id = MessageIdSample.Invoice.id)
        val selectedItemsList = listOf(item)

        val expectedCustomLabels = MailLabelTestData.listOfCustomLabels
        val expectedCustomUiLabels = MailLabelUiModelTestData.customLabelList

        val initialState = createMailboxDataState()
        val bottomSheetShownState = createMailboxStateWithLabelAsBottomSheet(selectedItemsList, false)
        val intermediateState = MailboxStateSampleData.createSelectionMode(
            listOf(item),
            currentMailLabel = MailLabel.System(MailLabelId.System.Trash)
        )
        expectViewModeForCurrentLocation(NoConversationGrouping)
        expectedSelectedLabelCountStateChange(initialState)
        returnExpectedStateWhenEnterSelectionMode(initialState, item, intermediateState)
        returnExpectedStateForBottomBarEvent(expectedState = intermediateState)
        expectObserveCustomMailLabelSucceeds(expectedCustomLabels)
        expectGetMessagesWithLabelsSucceeds(selectedItemsList.map { MessageId(it.id) })
        expectedLabelAsBottomSheetRequestedStateChange(expectedCustomUiLabels, bottomSheetShownState)
        expectedLabelAsBottomSheetDismissed(initialState)

        mailboxViewModel.state.test {
            awaitItem() // First emission for selected user

            // When
            mailboxViewModel.submit(MailboxViewAction.OnItemAvatarClicked(item))
            assertEquals(intermediateState, awaitItem())
            mailboxViewModel.submit(MailboxViewAction.RequestLabelAsBottomSheet)
            assertEquals(bottomSheetShownState, awaitItem())
            mailboxViewModel.submit(MailboxViewAction.DismissBottomSheet)

            // Then
            assertEquals(initialState, awaitItem())

            coVerify {
                relabelMessages wasNot Called
                relabelConversations wasNot Called
            }
        }
    }


    @Test
    fun `when label as is triggered for no conversation grouping then relabel messages is called`() = runTest {
        // Given
        val item = readMailboxItemUiModel.copy(id = MessageIdSample.Invoice.id)
        val secondItem = unreadMailboxItemUiModel.copy(id = MessageIdSample.AlphaAppQAReport.id)
        val selectedItemsList = listOf(item, secondItem)

        val expectedCustomLabels = MailLabelTestData.listOfCustomLabels
        val expectedCustomUiLabels = MailLabelUiModelTestData.customLabelList

        val expectedCurrentLabelList = LabelSelectionList(
            partiallySelectionLabels = emptyList(),
            selectedLabels = emptyList()
        )
        val expectedUpdatedLabelList = LabelSelectionList(
            partiallySelectionLabels = emptyList(),
            selectedLabels = listOf(
                MailLabelTestData.customLabelOne.id.labelId,
                MailLabelTestData.customLabelTwo.id.labelId
            )
        )

        val initialState = createMailboxDataState()
        val bottomSheetShownState = createMailboxStateWithLabelAsBottomSheet(selectedItemsList, false)
        val intermediateState = MailboxStateSampleData.createSelectionMode(
            listOf(item, secondItem),
            currentMailLabel = MailLabel.System(MailLabelId.System.Trash)
        )
        expectViewModeForCurrentLocation(NoConversationGrouping)
        expectedSelectedLabelCountStateChange(initialState)
        returnExpectedStateWhenEnterSelectionMode(initialState, item, intermediateState)
        returnExpectedStateForBottomBarEvent(expectedState = intermediateState)
        expectObserveCustomMailLabelSucceeds(expectedCustomLabels)
        expectGetMessagesWithLabelsSucceeds(selectedItemsList.map { MessageId(it.id) })
        expectedLabelAsBottomSheetRequestedStateChange(expectedCustomUiLabels, bottomSheetShownState)
        expectedLabelAsStateChange(selectedItemsList, LabelIdSample.Label2022)
        expectedLabelAsConfirmed(intermediateState)
        expectRelabelMessagesSucceeds(
            selectedItemsList.map { MessageId(it.id) },
            expectedCurrentLabelList,
            expectedUpdatedLabelList
        )

        mailboxViewModel.state.test {
            awaitItem() // First emission for selected user

            // When
            mailboxViewModel.submit(MailboxViewAction.OnItemAvatarClicked(item))
            assertEquals(intermediateState, awaitItem())
            mailboxViewModel.submit(MailboxViewAction.RequestLabelAsBottomSheet)
            assertEquals(bottomSheetShownState, awaitItem())
            mailboxViewModel.submit(MailboxViewAction.LabelAsToggleAction(LabelIdSample.Label2022))
            assertEquals(createMailboxStateWithLabelAsBottomSheet(selectedItemsList, true), awaitItem())
            mailboxViewModel.submit(MailboxViewAction.LabelAsConfirmed(false))

            // Then
            assertEquals(intermediateState, awaitItem())
            coVerify(exactly = 1) {
                relabelMessages(
                    userId,
                    selectedItemsList.map { MessageId(it.id) },
                    expectedCurrentLabelList,
                    expectedUpdatedLabelList
                )
            }
            coVerify { relabelConversations wasNot Called }
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `when label as is triggered with archive selected for no conversation grouping then move and relabel messages is called`() =
        runTest {
            // Given
            val item = readMailboxItemUiModel.copy(id = MessageIdSample.Invoice.id)
            val secondItem = unreadMailboxItemUiModel.copy(id = MessageIdSample.AlphaAppQAReport.id)
            val selectedItemsList = listOf(item, secondItem)

            val expectedCustomLabels = MailLabelTestData.listOfCustomLabels
            val expectedCustomUiLabels = MailLabelUiModelTestData.customLabelList

            val expectedCurrentLabelList = LabelSelectionList(
                partiallySelectionLabels = emptyList(),
                selectedLabels = emptyList()
            )
            val expectedUpdatedLabelList = LabelSelectionList(
                partiallySelectionLabels = emptyList(),
                selectedLabels = listOf(
                    MailLabelTestData.customLabelOne.id.labelId,
                    MailLabelTestData.customLabelTwo.id.labelId
                )
            )

            val initialState = createMailboxDataState()
            val bottomSheetShownState =
                createMailboxStateWithLabelAsBottomSheet(selectedItemsList, false)
            val intermediateState = MailboxStateSampleData.createSelectionMode(
                listOf(item, secondItem),
                currentMailLabel = MailLabel.System(MailLabelId.System.Trash)
            )
            expectViewModeForCurrentLocation(NoConversationGrouping)
            expectedSelectedLabelCountStateChange(initialState)
            returnExpectedStateWhenEnterSelectionMode(initialState, item, intermediateState)
            returnExpectedStateForBottomBarEvent(expectedState = intermediateState)
            expectObserveCustomMailLabelSucceeds(expectedCustomLabels)
            expectGetMessagesWithLabelsSucceeds(selectedItemsList.map { MessageId(it.id) })
            expectedLabelAsBottomSheetRequestedStateChange(expectedCustomUiLabels, bottomSheetShownState)
            expectedLabelAsStateChange(selectedItemsList, LabelIdSample.Label2022)
            expectedLabelAsConfirmed(intermediateState, true)
            expectRelabelMessagesSucceeds(
                selectedItemsList.map { MessageId(it.id) },
                expectedCurrentLabelList,
                expectedUpdatedLabelList
            )
            expectMoveMessagesSucceeds(userId, selectedItemsList, SystemLabelId.Archive.labelId)

            mailboxViewModel.state.test {
                awaitItem() // First emission for selected user

                // When
                mailboxViewModel.submit(MailboxViewAction.OnItemAvatarClicked(item))
                assertEquals(intermediateState, awaitItem())
                mailboxViewModel.submit(MailboxViewAction.RequestLabelAsBottomSheet)
                assertEquals(bottomSheetShownState, awaitItem())
                mailboxViewModel.submit(MailboxViewAction.LabelAsToggleAction(LabelIdSample.Label2022))
                assertEquals(
                    createMailboxStateWithLabelAsBottomSheet(selectedItemsList, true),
                    awaitItem()
                )
                mailboxViewModel.submit(MailboxViewAction.LabelAsConfirmed(true))

                // Then
                assertEquals(intermediateState, awaitItem())
                coVerifySequence {
                    moveMessages(userId, selectedItemsList.map { MessageId(it.id) }, SystemLabelId.Archive.labelId)
                    relabelMessages(
                        userId,
                        selectedItemsList.map { MessageId(it.id) },
                        expectedCurrentLabelList,
                        expectedUpdatedLabelList
                    )
                }
                coVerify { relabelConversations wasNot Called }
                coVerify { moveConversations wasNot Called }
            }
        }

    @Test
    fun `when label as is triggered for conversation grouping then relabel conversation is called`() = runTest {
        // Given
        val item = readMailboxItemUiModel.copy(id = MessageIdSample.Invoice.id)
        val secondItem = unreadMailboxItemUiModel.copy(id = MessageIdSample.AlphaAppQAReport.id)
        val selectedItemsList = listOf(item, secondItem)

        val expectedCustomLabels = MailLabelTestData.listOfCustomLabels
        val expectedCustomUiLabels = MailLabelUiModelTestData.customLabelList

        val expectedCurrentLabelList = LabelSelectionList(
            partiallySelectionLabels = emptyList(),
            selectedLabels = emptyList()
        )
        val expectedUpdatedLabelList = LabelSelectionList(
            partiallySelectionLabels = emptyList(),
            selectedLabels = listOf(
                MailLabelTestData.customLabelOne.id.labelId,
                MailLabelTestData.customLabelTwo.id.labelId
            )
        )

        val initialState = createMailboxDataState()
        val bottomSheetShownState =
            createMailboxStateWithLabelAsBottomSheet(selectedItemsList, false)
        val intermediateState = MailboxStateSampleData.createSelectionMode(
            listOf(item, secondItem),
            currentMailLabel = MailLabel.System(MailLabelId.System.Trash)
        )
        expectViewModeForCurrentLocation(ConversationGrouping)
        expectedSelectedLabelCountStateChange(initialState)
        returnExpectedStateWhenEnterSelectionMode(initialState, item, intermediateState)
        returnExpectedStateForBottomBarEvent(expectedState = intermediateState)
        expectObserveCustomMailLabelSucceeds(expectedCustomLabels)
        expectGetConversationMessagesWithLabelsSucceeds(selectedItemsList.map { ConversationId(it.id) })
        expectedLabelAsBottomSheetRequestedStateChange(expectedCustomUiLabels, bottomSheetShownState)
        expectedLabelAsStateChange(selectedItemsList, LabelIdSample.Label2022)
        expectedLabelAsConfirmed(intermediateState)
        expectRelabelConversationSucceeds(
            selectedItemsList.map { ConversationId(it.id) },
            expectedCurrentLabelList,
            expectedUpdatedLabelList
        )

        mailboxViewModel.state.test {
            awaitItem() // First emission for selected user

            // When
            mailboxViewModel.submit(MailboxViewAction.OnItemAvatarClicked(item))
            assertEquals(intermediateState, awaitItem())
            mailboxViewModel.submit(MailboxViewAction.RequestLabelAsBottomSheet)
            assertEquals(bottomSheetShownState, awaitItem())
            mailboxViewModel.submit(MailboxViewAction.LabelAsToggleAction(LabelIdSample.Label2022))
            assertEquals(
                createMailboxStateWithLabelAsBottomSheet(selectedItemsList, true),
                awaitItem()
            )
            mailboxViewModel.submit(MailboxViewAction.LabelAsConfirmed(false))

            // Then
            assertEquals(intermediateState, awaitItem())
            coVerify(exactly = 1) {
                relabelConversations(
                    userId,
                    selectedItemsList.map { ConversationId(it.id) },
                    expectedCurrentLabelList,
                    expectedUpdatedLabelList
                )
            }
            coVerify { relabelMessages wasNot Called }
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `when label as is triggered with archive selected for conversation grouping then move and relabel conversation is called`() =
        runTest {
            // Given
            val item = readMailboxItemUiModel.copy(id = MessageIdSample.Invoice.id)
            val secondItem = unreadMailboxItemUiModel.copy(id = MessageIdSample.AlphaAppQAReport.id)
            val selectedItemsList = listOf(item, secondItem)

            val expectedCustomLabels = MailLabelTestData.listOfCustomLabels
            val expectedCustomUiLabels = MailLabelUiModelTestData.customLabelList

            val expectedCurrentLabelList = LabelSelectionList(
                partiallySelectionLabels = emptyList(),
                selectedLabels = emptyList()
            )
            val expectedUpdatedLabelList = LabelSelectionList(
                partiallySelectionLabels = emptyList(),
                selectedLabels = listOf(
                    MailLabelTestData.customLabelOne.id.labelId,
                    MailLabelTestData.customLabelTwo.id.labelId
                )
            )

            val initialState = createMailboxDataState()
            val bottomSheetShownState =
                createMailboxStateWithLabelAsBottomSheet(selectedItemsList, false)
            val intermediateState = MailboxStateSampleData.createSelectionMode(
                listOf(item, secondItem),
                currentMailLabel = MailLabel.System(MailLabelId.System.Trash)
            )
            expectViewModeForCurrentLocation(ConversationGrouping)
            expectedSelectedLabelCountStateChange(initialState)
            returnExpectedStateWhenEnterSelectionMode(initialState, item, intermediateState)
            returnExpectedStateForBottomBarEvent(expectedState = intermediateState)
            expectObserveCustomMailLabelSucceeds(expectedCustomLabels)
            expectGetConversationMessagesWithLabelsSucceeds(selectedItemsList.map { ConversationId(it.id) })
            expectedLabelAsBottomSheetRequestedStateChange(expectedCustomUiLabels, bottomSheetShownState)
            expectedLabelAsStateChange(selectedItemsList, LabelIdSample.Label2022)
            expectedLabelAsConfirmed(intermediateState, true)
            expectRelabelConversationSucceeds(
                selectedItemsList.map { ConversationId(it.id) },
                expectedCurrentLabelList,
                expectedUpdatedLabelList
            )
            expectMoveConversationsSucceeds(userId, selectedItemsList, SystemLabelId.Archive.labelId)

            mailboxViewModel.state.test {
                awaitItem() // First emission for selected user

                // When
                mailboxViewModel.submit(MailboxViewAction.OnItemAvatarClicked(item))
                assertEquals(intermediateState, awaitItem())
                mailboxViewModel.submit(MailboxViewAction.RequestLabelAsBottomSheet)
                assertEquals(bottomSheetShownState, awaitItem())
                mailboxViewModel.submit(MailboxViewAction.LabelAsToggleAction(LabelIdSample.Label2022))
                assertEquals(
                    createMailboxStateWithLabelAsBottomSheet(selectedItemsList, true),
                    awaitItem()
                )
                mailboxViewModel.submit(MailboxViewAction.LabelAsConfirmed(true))

                // Then
                assertEquals(intermediateState, awaitItem())
                coVerifySequence {
                    moveConversations(
                        userId,
                        selectedItemsList.map { ConversationId(it.id) },
                        SystemLabelId.Archive.labelId
                    )
                    relabelConversations(
                        userId,
                        selectedItemsList.map { ConversationId(it.id) },
                        expectedCurrentLabelList,
                        expectedUpdatedLabelList
                    )
                }
                coVerify { relabelMessages wasNot Called }
                coVerify { moveMessages wasNot Called }
            }
        }

    @Test
    fun `when move to is triggered for no conversation grouping then move messages is called`() = runTest {
        // Given
        val item = readMailboxItemUiModel.copy(id = MessageIdSample.Invoice.id)
        val secondItem = unreadMailboxItemUiModel.copy(id = MessageIdSample.AlphaAppQAReport.id)
        val selectedItemsList = listOf(item, secondItem)

        val expectedMailLabels = MailLabels(
            systemLabels = emptyList(),
            folders = MailLabelTestData.listOfCustomLabels,
            labels = emptyList()
        )
        val expectedMailLabelUiModel = MailLabelUiModelTestData.customLabelList

        val initialState = createMailboxDataState()
        val bottomSheetShownState = createMailboxStateWithMoveToBottomSheet(selectedItemsList)
        val bottomSheetShownStateWithSelectedItem = createMailboxStateWithMoveToBottomSheet(
            selectedItemsList,
            MailLabelUiModelTestData.spamFolder
        )
        val intermediateState = MailboxStateSampleData.createSelectionMode(
            listOf(item, secondItem),
            currentMailLabel = MailLabel.System(MailLabelId.System.Trash)
        )
        expectViewModeForCurrentLocation(NoConversationGrouping)
        expectedSelectedLabelCountStateChange(initialState)
        returnExpectedStateWhenEnterSelectionMode(initialState, item, intermediateState)
        returnExpectedStateForBottomBarEvent(expectedState = intermediateState)
        expectDestinationMailLabelsSucceeds(expectedMailLabels)
        expectedMoveToBottomSheetRequestedStateChange(expectedMailLabelUiModel, bottomSheetShownState)
        expectedMoveToStateChange(MailLabelId.System.Spam, bottomSheetShownStateWithSelectedItem)
        expectedMoveToConfirmed(initialState)
        expectMoveMessagesSucceeds(userId, selectedItemsList, MailLabelId.System.Spam.labelId)

        mailboxViewModel.state.test {
            awaitItem() // First emission for selected user

            // When + Then
            mailboxViewModel.submit(MailboxViewAction.OnItemAvatarClicked(item))
            assertEquals(intermediateState, awaitItem())
            mailboxViewModel.submit(MailboxViewAction.RequestMoveToBottomSheet)
            assertEquals(bottomSheetShownState, awaitItem())
            mailboxViewModel.submit(MailboxViewAction.MoveToDestinationSelected(MailLabelId.System.Spam))
            assertEquals(bottomSheetShownStateWithSelectedItem, awaitItem())
            mailboxViewModel.submit(MailboxViewAction.MoveToConfirmed)

            assertEquals(initialState, awaitItem())
            coVerify(exactly = 1) {
                moveMessages(userId, selectedItemsList.map { MessageId(it.id) }, MailLabelId.System.Spam.labelId)
            }
            coVerify { moveConversations wasNot Called }
        }
    }

    @Test
    fun `when move to is triggered for conversation grouping then move conversation is called`() = runTest {
        // Given
        val item = readMailboxItemUiModel.copy(id = ConversationIdSample.Invoices.id)
        val secondItem = unreadMailboxItemUiModel.copy(id = ConversationIdSample.AlphaAppFeedback.id)
        val selectedItemsList = listOf(item, secondItem)

        val expectedMailLabels = MailLabels(
            systemLabels = emptyList(),
            folders = MailLabelTestData.listOfCustomLabels,
            labels = emptyList()
        )
        val expectedMailLabelUiModel = MailLabelUiModelTestData.customLabelList

        val initialState = createMailboxDataState()
        val bottomSheetShownState = createMailboxStateWithMoveToBottomSheet(selectedItemsList)
        val bottomSheetShownStateWithSelectedItem = createMailboxStateWithMoveToBottomSheet(
            selectedItemsList,
            MailLabelUiModelTestData.spamFolder
        )
        val intermediateState = MailboxStateSampleData.createSelectionMode(
            listOf(item, secondItem),
            currentMailLabel = MailLabel.System(MailLabelId.System.Trash)
        )
        expectViewModeForCurrentLocation(ConversationGrouping)
        expectedSelectedLabelCountStateChange(initialState)
        returnExpectedStateWhenEnterSelectionMode(initialState, item, intermediateState)
        returnExpectedStateForBottomBarEvent(expectedState = intermediateState)
        expectDestinationMailLabelsSucceeds(expectedMailLabels)
        expectedMoveToBottomSheetRequestedStateChange(expectedMailLabelUiModel, bottomSheetShownState)
        expectedMoveToStateChange(MailLabelId.System.Spam, bottomSheetShownStateWithSelectedItem)
        expectedMoveToConfirmed(initialState)
        expectMoveConversationsSucceeds(userId, selectedItemsList, MailLabelId.System.Spam.labelId)

        mailboxViewModel.state.test {
            awaitItem() // First emission for selected user

            // When + Then
            mailboxViewModel.submit(MailboxViewAction.OnItemAvatarClicked(item))
            assertEquals(intermediateState, awaitItem())
            mailboxViewModel.submit(MailboxViewAction.RequestMoveToBottomSheet)
            assertEquals(bottomSheetShownState, awaitItem())
            mailboxViewModel.submit(MailboxViewAction.MoveToDestinationSelected(MailLabelId.System.Spam))
            assertEquals(bottomSheetShownStateWithSelectedItem, awaitItem())
            mailboxViewModel.submit(MailboxViewAction.MoveToConfirmed)

            assertEquals(initialState, awaitItem())
            coVerify(exactly = 1) {
                moveConversations(
                    userId = userId,
                    conversationIds = selectedItemsList.map { ConversationId(it.id) },
                    labelId = MailLabelId.System.Spam.labelId
                )
            }
            coVerify { moveMessages wasNot Called }
        }
    }

    @Test
    fun `show error retrieving folder error when observing mail labels failed`() = runTest {
        // Given
        val item = readMailboxItemUiModel.copy(id = ConversationIdSample.Invoices.id)
        val secondItem = unreadMailboxItemUiModel.copy(id = ConversationIdSample.AlphaAppFeedback.id)
        val selectedItemsList = listOf(item, secondItem)

        val initialState = createMailboxDataState()
        val intermediateState = MailboxStateSampleData.createSelectionMode(
            selectedItemsList,
            currentMailLabel = MailLabel.System(MailLabelId.System.Trash)
        )
        val expectedState = MailboxStateSampleData.createSelectionMode(
            selectedItemsList,
            currentMailLabel = MailLabel.System(MailLabelId.System.Trash),
            error = Effect.of(TextUiModel(R.string.mailbox_action_move_messages_failed_retrieving_folders))
        )
        expectViewMode(ConversationGrouping)
        expectedSelectedLabelCountStateChange(initialState)
        returnExpectedStateWhenEnterSelectionMode(initialState, item, intermediateState)
        returnExpectedStateForBottomBarEvent(expectedState = intermediateState)
        expectedReducerResult(MailboxEvent.ErrorRetrievingDestinationMailFolders, expectedState)
        expectDestinationMailLabelsFails()

        mailboxViewModel.state.test {
            awaitItem() // First emission for selected user

            // When + Then
            mailboxViewModel.submit(MailboxViewAction.OnItemAvatarClicked(item))
            assertEquals(intermediateState, awaitItem())
            mailboxViewModel.submit(MailboxViewAction.RequestMoveToBottomSheet)
            assertEquals(expectedState, awaitItem())

            coVerify { moveConversations wasNot Called }
            coVerify { moveMessages wasNot Called }
        }
    }

    @Test
    fun `show error retrieving color setting when observing color settings failed`() = runTest {
        // Given
        val item = readMailboxItemUiModel.copy(id = ConversationIdSample.Invoices.id)
        val secondItem = unreadMailboxItemUiModel.copy(id = ConversationIdSample.AlphaAppFeedback.id)
        val selectedItemsList = listOf(item, secondItem)

        val expectedMailLabels = MailLabels(
            systemLabels = emptyList(),
            folders = MailLabelTestData.listOfCustomLabels,
            labels = emptyList()
        )

        val initialState = createMailboxDataState()
        val intermediateState = MailboxStateSampleData.createSelectionMode(
            selectedItemsList,
            currentMailLabel = MailLabel.System(MailLabelId.System.Trash)
        )
        val expectedState = MailboxStateSampleData.createSelectionMode(
            selectedItemsList,
            currentMailLabel = MailLabel.System(MailLabelId.System.Trash),
            error = Effect.of(TextUiModel(R.string.mailbox_action_move_messages_failed_retrieving_folders))
        )
        expectViewMode(ConversationGrouping)
        expectedSelectedLabelCountStateChange(initialState)
        returnExpectedStateWhenEnterSelectionMode(initialState, item, intermediateState)
        returnExpectedStateForBottomBarEvent(expectedState = intermediateState)
        expectedReducerResult(MailboxEvent.ErrorRetrievingFolderColorSettings, expectedState)
        expectDestinationMailLabelsSucceeds(expectedMailLabels)
        expectFolderColorSettingsFails()

        mailboxViewModel.state.test {
            awaitItem() // First emission for selected user

            // When + Then
            mailboxViewModel.submit(MailboxViewAction.OnItemAvatarClicked(item))
            assertEquals(intermediateState, awaitItem())
            mailboxViewModel.submit(MailboxViewAction.RequestMoveToBottomSheet)
            assertEquals(expectedState, awaitItem())

            coVerify { moveConversations wasNot Called }
            coVerify { moveMessages wasNot Called }
        }
    }

    @Test
    fun `when more action bottom sheet is requested with non-starred items then star action is shown`() = runTest {
        // Given
        val item = readMailboxItemUiModel.copy(id = MessageIdSample.Invoice.id, showStar = true)
        val secondItem = unreadMailboxItemUiModel.copy(id = MessageIdSample.AlphaAppQAReport.id, showStar = false)
        val selectedItemsList = listOf(item, secondItem)

        val initialState = createMailboxDataState()
        val expectedActionItems = listOf(
            ActionUiModelSample.build(Action.Star),
            ActionUiModelSample.build(Action.Archive),
            ActionUiModelSample.build(Action.Spam)
        )
        val expectedBottomSheetContent = MailboxMoreActionsBottomSheetState.Data(
            actionUiModels = expectedActionItems.toImmutableList()
        )
        val bottomSheetShownState =
            createMailboxStateWithMoreActionBottomSheet(selectedItemsList, expectedBottomSheetContent)
        val intermediateState = MailboxStateSampleData.createSelectionMode(
            listOf(item, secondItem),
            currentMailLabel = MailLabel.System(MailLabelId.System.Trash)
        )
        expectViewModeForCurrentLocation(NoConversationGrouping)
        expectedSelectedLabelCountStateChange(initialState)
        returnExpectedStateForBottomBarEvent(expectedState = intermediateState)
        returnExpectedStateWhenEnterSelectionMode(initialState, item, intermediateState)
        expectedMoreActionBottomSheetRequestedStateChange(expectedActionItems, bottomSheetShownState)


        mailboxViewModel.state.test {
            awaitItem() // First emission for selected user

            // When + Then
            mailboxViewModel.submit(MailboxViewAction.OnItemAvatarClicked(item))
            assertEquals(intermediateState, awaitItem())
            mailboxViewModel.submit(MailboxViewAction.RequestMoreActionsBottomSheet)
            assertEquals(bottomSheetShownState, awaitItem())
        }
    }

    @Test
    fun `when more action bottom sheet is requested with all items starred then unstar action is shown`() = runTest {
        // Given
        val item = readMailboxItemUiModel.copy(id = MessageIdSample.Invoice.id, showStar = true)
        val secondItem = unreadMailboxItemUiModel.copy(id = MessageIdSample.AlphaAppQAReport.id, showStar = true)
        val selectedItemsList = listOf(item, secondItem)

        val initialState = createMailboxDataState()
        val expectedActionItems = listOf(
            ActionUiModelSample.build(Action.Unstar),
            ActionUiModelSample.build(Action.Archive),
            ActionUiModelSample.build(Action.Spam)
        )
        val expectedBottomSheetContent = MailboxMoreActionsBottomSheetState.Data(
            actionUiModels = expectedActionItems.toImmutableList()
        )
        val bottomSheetShownState =
            createMailboxStateWithMoreActionBottomSheet(selectedItemsList, expectedBottomSheetContent)
        val intermediateState = MailboxStateSampleData.createSelectionMode(
            listOf(item, secondItem),
            currentMailLabel = MailLabel.System(MailLabelId.System.Trash)
        )
        expectViewModeForCurrentLocation(NoConversationGrouping)
        expectedSelectedLabelCountStateChange(initialState)
        returnExpectedStateForBottomBarEvent(expectedState = intermediateState)
        returnExpectedStateWhenEnterSelectionMode(initialState, item, intermediateState)
        expectedMoreActionBottomSheetRequestedStateChange(expectedActionItems, bottomSheetShownState)


        mailboxViewModel.state.test {
            awaitItem() // First emission for selected user

            // When + Then
            mailboxViewModel.submit(MailboxViewAction.OnItemAvatarClicked(item))
            assertEquals(intermediateState, awaitItem())
            mailboxViewModel.submit(MailboxViewAction.RequestMoreActionsBottomSheet)
            assertEquals(bottomSheetShownState, awaitItem())
        }
    }

    @Test
    fun `when star action is triggered for no-conversation grouping then star messages is called`() = runTest {
        // Given
        val item = readMailboxItemUiModel.copy(id = MessageIdSample.Invoice.id, showStar = true)
        val secondItem = unreadMailboxItemUiModel.copy(id = MessageIdSample.AlphaAppQAReport.id, showStar = false)
        val selectedItemsList = listOf(item, secondItem)

        val initialState = createMailboxDataState()
        val expectedActionItems = listOf(
            ActionUiModelSample.build(Action.Star),
            ActionUiModelSample.build(Action.Archive),
            ActionUiModelSample.build(Action.Spam)
        )
        val expectedBottomSheetContent = MailboxMoreActionsBottomSheetState.Data(
            actionUiModels = expectedActionItems.toImmutableList()
        )
        val bottomSheetShownState =
            createMailboxStateWithMoreActionBottomSheet(selectedItemsList, expectedBottomSheetContent)
        val intermediateState = MailboxStateSampleData.createSelectionMode(
            listOf(item, secondItem),
            currentMailLabel = MailLabel.System(MailLabelId.System.Trash)
        )
        expectViewModeForCurrentLocation(NoConversationGrouping)
        expectedSelectedLabelCountStateChange(initialState)
        returnExpectedStateForBottomBarEvent(expectedState = intermediateState)
        returnExpectedStateWhenEnterSelectionMode(initialState, item, intermediateState)
        expectedMoreActionBottomSheetRequestedStateChange(expectedActionItems, bottomSheetShownState)
        expectedStarMessagesSucceeds(userId, selectedItemsList)
        returnExpectedStateWhenStarringSucceeds(intermediateState)


        mailboxViewModel.state.test {
            awaitItem() // First emission for selected user

            // When + Then
            mailboxViewModel.submit(MailboxViewAction.OnItemAvatarClicked(item))
            assertEquals(intermediateState, awaitItem())
            mailboxViewModel.submit(MailboxViewAction.RequestMoreActionsBottomSheet)
            assertEquals(bottomSheetShownState, awaitItem())
            mailboxViewModel.submit(MailboxViewAction.Star)
            assertEquals(intermediateState, awaitItem())
        }
        coVerify(exactly = 1) { starMessages(userId, selectedItemsList.map { MessageId(it.id) }) }
        verify { starConversations wasNot Called }
    }

//    @Test
//    fun `when unstar action is triggered for conversation grouping then unstar conversations is called`() = runTest {
//        // Given
//        val item = readMailboxItemUiModel.copy(id = MessageIdSample.Invoice.id, showStar = true)
//        val secondItem = unreadMailboxItemUiModel.copy(id = MessageIdSample.AlphaAppQAReport.id, showStar = true)
//        val selectedItemsList = listOf(item, secondItem)
//
//        val initialState = createMailboxDataState()
//        val expectedActionItems = listOf(
//            ActionUiModelSample.build(Action.Unstar),
//            ActionUiModelSample.build(Action.Archive),
//            ActionUiModelSample.build(Action.Spam)
//        )
//        val expectedBottomSheetContent = MoreActionsBottomSheetState.Data(
//            actionUiModels = expectedActionItems.toImmutableList()
//        )
//        val bottomSheetShownState =
//            createMailboxStateWithMoreActionBottomSheet(selectedItemsList, expectedBottomSheetContent)
//        val intermediateState = MailboxStateSampleData.createSelectionMode(
//            listOf(item, secondItem),
//            currentMailLabel = MailLabel.System(MailLabelId.System.Trash)
//        )
//        expectViewModeForCurrentLocation(ConversationGrouping)
//        expectedSelectedLabelCountStateChange(initialState)
//        returnExpectedStateForBottomBarEvent(expectedState = intermediateState)
//        returnExpectedStateWhenEnterSelectionMode(initialState, item, intermediateState)
//        expectedMoreActionBottomSheetRequestedStateChange(expectedActionItems, bottomSheetShownState)
//        expectedUnStarConversationsSucceeds(userId, selectedItemsList)
//        returnExpectedStateWhenUnStarringSucceeds(intermediateState)
//
//
//        mailboxViewModel.state.test {
//            awaitItem() // First emission for selected user
//
//            // When + Then
//            mailboxViewModel.submit(MailboxViewAction.OnItemAvatarClicked(item))
//            assertEquals(intermediateState, awaitItem())
//            mailboxViewModel.submit(MailboxViewAction.RequestMoreActionsBottomSheet)
//            assertEquals(bottomSheetShownState, awaitItem())
//            mailboxViewModel.submit(MailboxViewAction.UnStar)
//            assertEquals(intermediateState, awaitItem())
//        }
//        coVerify(exactly = 1) { unStarConversations(userId, selectedItemsList.map { ConversationId(it.id) }) }
//        verify { unStarMessages wasNot Called }
//    }

    @Test
    fun `when unstar action is triggered for no-conversation grouping then unstar messages is called`() = runTest {
        // Given
        val item = readMailboxItemUiModel.copy(id = MessageIdSample.Invoice.id, showStar = true)
        val secondItem = unreadMailboxItemUiModel.copy(id = MessageIdSample.AlphaAppQAReport.id, showStar = true)
        val selectedItemsList = listOf(item, secondItem)

        val initialState = createMailboxDataState()
        val expectedActionItems = listOf(
            ActionUiModelSample.build(Action.Unstar),
            ActionUiModelSample.build(Action.Archive),
            ActionUiModelSample.build(Action.Spam)
        )
        val expectedBottomSheetContent = MailboxMoreActionsBottomSheetState.Data(
            actionUiModels = expectedActionItems.toImmutableList()
        )
        val bottomSheetShownState =
            createMailboxStateWithMoreActionBottomSheet(selectedItemsList, expectedBottomSheetContent)
        val intermediateState = MailboxStateSampleData.createSelectionMode(
            listOf(item, secondItem),
            currentMailLabel = MailLabel.System(MailLabelId.System.Trash)
        )
        expectViewModeForCurrentLocation(NoConversationGrouping)
        expectedSelectedLabelCountStateChange(initialState)
        returnExpectedStateForBottomBarEvent(expectedState = intermediateState)
        returnExpectedStateWhenEnterSelectionMode(initialState, item, intermediateState)
        expectedMoreActionBottomSheetRequestedStateChange(expectedActionItems, bottomSheetShownState)
        expectedUnStarMessagesSucceeds(userId, selectedItemsList)
        returnExpectedStateWhenUnStarringSucceeds(intermediateState)


        mailboxViewModel.state.test {
            awaitItem() // First emission for selected user

            // When + Then
            mailboxViewModel.submit(MailboxViewAction.OnItemAvatarClicked(item))
            assertEquals(intermediateState, awaitItem())
            mailboxViewModel.submit(MailboxViewAction.RequestMoreActionsBottomSheet)
            assertEquals(bottomSheetShownState, awaitItem())
            mailboxViewModel.submit(MailboxViewAction.UnStar)
            assertEquals(intermediateState, awaitItem())
        }
        coVerify(exactly = 1) { unStarMessages(userId, selectedItemsList.map { MessageId(it.id) }) }
        verify { unStarConversations wasNot Called }
    }

//    @Test
//    fun `when star action is triggered for conversation grouping then star conversations is called`() = runTest {
//        // Given
//        val item = readMailboxItemUiModel.copy(id = MessageIdSample.Invoice.id, showStar = true)
//        val secondItem = unreadMailboxItemUiModel.copy(id = MessageIdSample.AlphaAppQAReport.id, showStar = false)
//        val selectedItemsList = listOf(item, secondItem)
//
//        val initialState = createMailboxDataState()
//        val expectedActionItems = listOf(
//            ActionUiModelSample.build(Action.Archive),
//            ActionUiModelSample.build(Action.Spam)
//        )
//        val expectedBottomSheetContent = MoreActionsBottomSheetState.Data(
//            actionUiModels = expectedActionItems.toImmutableList()
//        )
//        val bottomSheetShownState =
//            createMailboxStateWithMoreActionBottomSheet(selectedItemsList, expectedBottomSheetContent)
//        val intermediateState = MailboxStateSampleData.createSelectionMode(
//            listOf(item, secondItem),
//            currentMailLabel = MailLabel.System(MailLabelId.System.Trash)
//        )
//        expectViewModeForCurrentLocation(ConversationGrouping)
//        expectedSelectedLabelCountStateChange(initialState)
//        returnExpectedStateForBottomBarEvent(expectedState = intermediateState)
//        returnExpectedStateWhenEnterSelectionMode(initialState, item, intermediateState)
//        expectedMoreActionBottomSheetRequestedStateChange(expectedActionItems, bottomSheetShownState)
//        expectedStarConversationsSucceeds(userId, selectedItemsList)
//        returnExpectedStateWhenStarringSucceeds(intermediateState)
//
//
//        mailboxViewModel.state.test {
//            awaitItem() // First emission for selected user
//
//            // When + Then
//            mailboxViewModel.submit(MailboxViewAction.OnItemAvatarClicked(item))
//            assertEquals(intermediateState, awaitItem())
//            mailboxViewModel.submit(MailboxViewAction.RequestMoreActionsBottomSheet)
//            assertEquals(bottomSheetShownState, awaitItem())
//            mailboxViewModel.submit(MailboxViewAction.Star)
//            assertEquals(intermediateState, awaitItem())
//            coVerify(exactly = 1) { starConversations(userId, selectedItemsList.map { ConversationId(it.id) }) }
//            verify { starMessages wasNot Called }
//        }
//    }

    @Test
    fun `when move to archive is triggered for no-conversation grouping then move messages is called`() = runTest {
        // Given
        val item = readMailboxItemUiModel.copy(id = MessageIdSample.Invoice.id, showStar = true)
        val secondItem = unreadMailboxItemUiModel.copy(id = MessageIdSample.AlphaAppQAReport.id, showStar = true)
        val selectedItemsList = listOf(item, secondItem)

        val initialState = createMailboxDataState()
        val expectedActionItems = listOf(
            ActionUiModelSample.build(Action.Unstar),
            ActionUiModelSample.build(Action.Archive),
            ActionUiModelSample.build(Action.Spam)
        )
        val expectedBottomSheetContent = MailboxMoreActionsBottomSheetState.Data(
            actionUiModels = expectedActionItems.toImmutableList()
        )
        val bottomSheetShownState =
            createMailboxStateWithMoreActionBottomSheet(selectedItemsList, expectedBottomSheetContent)
        val intermediateState = MailboxStateSampleData.createSelectionMode(
            listOf(item, secondItem),
            currentMailLabel = MailLabel.System(MailLabelId.System.Trash)
        )
        expectViewModeForCurrentLocation(NoConversationGrouping)
        expectedSelectedLabelCountStateChange(initialState)
        returnExpectedStateForBottomBarEvent(expectedState = intermediateState)
        returnExpectedStateWhenEnterSelectionMode(initialState, item, intermediateState)
        expectedMoreActionBottomSheetRequestedStateChange(expectedActionItems, bottomSheetShownState)
        expectMoveMessagesSucceeds(userId, selectedItemsList, SystemLabelId.Archive.labelId)
        expectedReducerResult(MailboxViewAction.MoveToConfirmed, initialState)


        mailboxViewModel.state.test {
            awaitItem() // First emission for selected user

            // When + Then
            mailboxViewModel.submit(MailboxViewAction.OnItemAvatarClicked(item))
            assertEquals(intermediateState, awaitItem())
            mailboxViewModel.submit(MailboxViewAction.RequestMoreActionsBottomSheet)
            assertEquals(bottomSheetShownState, awaitItem())
            mailboxViewModel.submit(MailboxViewAction.MoveToArchive)
            assertEquals(initialState, awaitItem())
        }
        coVerify { moveMessages(userId, selectedItemsList.map { MessageId(it.id) }, SystemLabelId.Archive.labelId) }
        coVerify { moveConversations wasNot Called }
    }

    @Test
    fun `when move to archive is triggered for conversation grouping then move conversation is called`() = runTest {
        // Given
        val item = readMailboxItemUiModel.copy(id = MessageIdSample.Invoice.id, showStar = true)
        val secondItem = unreadMailboxItemUiModel.copy(id = MessageIdSample.AlphaAppQAReport.id, showStar = true)
        val selectedItemsList = listOf(item, secondItem)

        val initialState = createMailboxDataState()
        val expectedActionItems = listOf(
            ActionUiModelSample.build(Action.Archive),
            ActionUiModelSample.build(Action.Spam)
        )
        val expectedBottomSheetContent = MailboxMoreActionsBottomSheetState.Data(
            actionUiModels = expectedActionItems.toImmutableList()
        )
        val bottomSheetShownState =
            createMailboxStateWithMoreActionBottomSheet(
                selectedItemsList,
                expectedBottomSheetContent
            )
        val intermediateState = MailboxStateSampleData.createSelectionMode(
            listOf(item, secondItem),
            currentMailLabel = MailLabel.System(MailLabelId.System.Trash)
        )
        expectViewMode(ConversationGrouping)
        expectViewModeForCurrentLocation(ConversationGrouping)
        expectedSelectedLabelCountStateChange(initialState)
        returnExpectedStateForBottomBarEvent(expectedState = intermediateState)
        returnExpectedStateWhenEnterSelectionMode(initialState, item, intermediateState)
        expectedMoreActionBottomSheetRequestedStateChange(expectedActionItems, bottomSheetShownState)
        expectMoveConversationsSucceeds(userId, selectedItemsList, SystemLabelId.Archive.labelId)
        expectedReducerResult(MailboxViewAction.MoveToConfirmed, initialState)


        mailboxViewModel.state.test {
            awaitItem() // First emission for selected user

            // When + Then
            mailboxViewModel.submit(MailboxViewAction.OnItemAvatarClicked(item))
            assertEquals(intermediateState, awaitItem())
            mailboxViewModel.submit(MailboxViewAction.RequestMoreActionsBottomSheet)
            assertEquals(bottomSheetShownState, awaitItem())
            mailboxViewModel.submit(MailboxViewAction.MoveToArchive)
            assertEquals(initialState, awaitItem())
        }
        coVerify {
            moveConversations(userId, selectedItemsList.map { ConversationId(it.id) }, SystemLabelId.Archive.labelId)
        }
        coVerify { moveMessages wasNot Called }
    }

    @Test
    fun `when move to spam is triggered for no-conversation grouping then move messages is called`() = runTest {
        // Given
        val item = readMailboxItemUiModel.copy(id = MessageIdSample.Invoice.id, showStar = true)
        val secondItem = unreadMailboxItemUiModel.copy(id = MessageIdSample.AlphaAppQAReport.id, showStar = true)
        val selectedItemsList = listOf(item, secondItem)

        val initialState = createMailboxDataState()
        val expectedActionItems = listOf(
            ActionUiModelSample.build(Action.Unstar),
            ActionUiModelSample.build(Action.Archive),
            ActionUiModelSample.build(Action.Spam)
        )
        val expectedBottomSheetContent = MailboxMoreActionsBottomSheetState.Data(
            actionUiModels = expectedActionItems.toImmutableList()
        )
        val bottomSheetShownState =
            createMailboxStateWithMoreActionBottomSheet(selectedItemsList, expectedBottomSheetContent)
        val intermediateState = MailboxStateSampleData.createSelectionMode(
            listOf(item, secondItem),
            currentMailLabel = MailLabel.System(MailLabelId.System.Trash)
        )
        expectViewModeForCurrentLocation(NoConversationGrouping)
        expectedSelectedLabelCountStateChange(initialState)
        returnExpectedStateForBottomBarEvent(expectedState = intermediateState)
        returnExpectedStateWhenEnterSelectionMode(initialState, item, intermediateState)
        expectedMoreActionBottomSheetRequestedStateChange(expectedActionItems, bottomSheetShownState)
        expectMoveMessagesSucceeds(userId, selectedItemsList, SystemLabelId.Spam.labelId)
        expectedReducerResult(MailboxViewAction.MoveToConfirmed, initialState)


        mailboxViewModel.state.test {
            awaitItem() // First emission for selected user

            // When + Then
            mailboxViewModel.submit(MailboxViewAction.OnItemAvatarClicked(item))
            assertEquals(intermediateState, awaitItem())
            mailboxViewModel.submit(MailboxViewAction.RequestMoreActionsBottomSheet)
            assertEquals(bottomSheetShownState, awaitItem())
            mailboxViewModel.submit(MailboxViewAction.MoveToSpam)
            assertEquals(initialState, awaitItem())
        }
        coVerify { moveMessages(userId, selectedItemsList.map { MessageId(it.id) }, SystemLabelId.Spam.labelId) }
        coVerify { moveConversations wasNot Called }
    }

    @Test
    fun `when move to spam is triggered for conversation grouping then move conversation is called`() = runTest {
        // Given
        val item = readMailboxItemUiModel.copy(id = MessageIdSample.Invoice.id, showStar = true)
        val secondItem = unreadMailboxItemUiModel.copy(id = MessageIdSample.AlphaAppQAReport.id, showStar = true)
        val selectedItemsList = listOf(item, secondItem)

        val initialState = createMailboxDataState()
        val expectedActionItems = listOf(
            ActionUiModelSample.build(Action.Archive),
            ActionUiModelSample.build(Action.Spam)
        )
        val expectedBottomSheetContent = MailboxMoreActionsBottomSheetState.Data(
            actionUiModels = expectedActionItems.toImmutableList()
        )
        val bottomSheetShownState =
            createMailboxStateWithMoreActionBottomSheet(
                selectedItemsList,
                expectedBottomSheetContent
            )
        val intermediateState = MailboxStateSampleData.createSelectionMode(
            listOf(item, secondItem),
            currentMailLabel = MailLabel.System(MailLabelId.System.Trash)
        )
        expectViewMode(ConversationGrouping)
        expectViewModeForCurrentLocation(ConversationGrouping)
        expectedSelectedLabelCountStateChange(initialState)
        returnExpectedStateForBottomBarEvent(expectedState = intermediateState)
        returnExpectedStateWhenEnterSelectionMode(initialState, item, intermediateState)
        expectedMoreActionBottomSheetRequestedStateChange(expectedActionItems, bottomSheetShownState)
        expectMoveConversationsSucceeds(userId, selectedItemsList, SystemLabelId.Spam.labelId)
        expectedReducerResult(MailboxViewAction.MoveToConfirmed, initialState)


        mailboxViewModel.state.test {
            awaitItem() // First emission for selected user

            // When + Then
            mailboxViewModel.submit(MailboxViewAction.OnItemAvatarClicked(item))
            assertEquals(intermediateState, awaitItem())
            mailboxViewModel.submit(MailboxViewAction.RequestMoreActionsBottomSheet)
            assertEquals(bottomSheetShownState, awaitItem())
            mailboxViewModel.submit(MailboxViewAction.MoveToSpam)
            assertEquals(initialState, awaitItem())
        }
        coVerify {
            moveConversations(userId, selectedItemsList.map { ConversationId(it.id) }, SystemLabelId.Spam.labelId)
        }
        coVerify { moveMessages wasNot Called }
    }

    @Test
    fun `verify dismiss delete dialog calls reducer`() = runTest {
        // Given
        val initialState = createMailboxDataState()
        expectViewMode(NoConversationGrouping)
        expectedSelectedLabelCountStateChange(initialState)
        returnExpectedStateForDeleteDismissed(initialState, initialState)

        mailboxViewModel.state.test {
            // Given
            awaitItem() // First emission for selected user

            // When
            mailboxViewModel.submit(MailboxViewAction.DeleteDialogDismissed)

            // Then
            verify(exactly = 1) {
                mailboxReducer.newStateFrom(initialState, MailboxViewAction.DeleteDialogDismissed)
            }
        }
    }

    @Test
    fun `verify dismiss bottom sheet calls reducer`() = runTest {
        // Given
        val initialState = createMailboxDataState()
        expectViewMode(NoConversationGrouping)
        expectedSelectedLabelCountStateChange(initialState)
        returnExpectedStateForDeleteDismissed(initialState, initialState)

        mailboxViewModel.state.test {
            // Given
            awaitItem() // First emission for selected user

            // When
            mailboxViewModel.submit(MailboxViewAction.DeleteDialogDismissed)

            // Then
            verify(exactly = 1) {
                mailboxReducer.newStateFrom(initialState, MailboxViewAction.DeleteDialogDismissed)
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
        every {
            pagerFactory.create(
                userIds,
                any(),
                false,
                Conversation,
                any(),
                emptyLabelInProgressSignal
            )
        } returns mockk mockPager@{
            every { this@mockPager.flow } returns flowOf(PagingData.from(listOf(unreadMailboxItem)))
        }
        every { mailboxReducer.newStateFrom(any(), any()) } returns initialMailboxState
        returnExpectedStateForBottomBarEvent(expectedState = expectedState)

        mailboxViewModel.items.test {
            // Then
            awaitItem()
            verify {
                pagerFactory.create(
                    userIds,
                    initialLocationMailLabelId,
                    false,
                    Conversation,
                    any(),
                    emptyLabelInProgressSignal
                )
            }

            // When
            primaryUserFlow.emit(null)

            // Then
            verify(exactly = 0) {
                pagerFactory.create(
                    userIds,
                    initialLocationMailLabelId,
                    false,
                    Message,
                    any(),
                    emptyLabelInProgressSignal
                )
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `when swipe read is called for no conversation grouping and item is read then mark as unread is called`() {
        // Given
        val expectedItemId = "itemId"
        expectViewMode(NoConversationGrouping)
        expectMarkMessagesAsUnreadSucceeds(userId, listOf(buildMailboxUiModelItem(id = expectedItemId)))
        val expectedViewAction = MailboxViewAction.SwipeReadAction(userId, expectedItemId, true)

        // When
        mailboxViewModel.submit(expectedViewAction)

        // Then
        coVerifyOrder {
            markMessagesAsUnread(userId, listOf(MessageId(expectedItemId)))
            mailboxReducer.newStateFrom(any(), expectedViewAction)
        }
        coVerify {
            markMessagesAsRead wasNot Called
            markConversationsAsRead wasNot Called
            markConversationsAsUnread wasNot Called
        }
    }

    @Test
    fun `when swipe read is called for conversation grouping and item is read then mark as unread is called`() {
        // Given
        val expectedItemId = "itemId"
        expectViewModeForCurrentLocation(ConversationGrouping)
        expectMarkConversationsAsUnreadSucceeds(userId, listOf(buildMailboxUiModelItem(id = expectedItemId)))
        val expectedViewAction = MailboxViewAction.SwipeReadAction(userId, expectedItemId, true)

        // When
        mailboxViewModel.submit(expectedViewAction)

        // Then
        coVerifyOrder {
            markConversationsAsUnread(userId, listOf(ConversationId(expectedItemId)))
            mailboxReducer.newStateFrom(any(), expectedViewAction)
        }
        coVerify {
            markMessagesAsRead wasNot Called
            markMessagesAsUnread wasNot Called
            markConversationsAsRead wasNot Called
        }
    }

    @Test
    fun `when swipe read is called for no conversation grouping and item is unread then mark as read is called`() {
        // Given
        val expectedItemId = "itemId"
        expectViewMode(NoConversationGrouping)
        expectMarkMessagesAsReadSucceeds(userId, listOf(buildMailboxUiModelItem(id = expectedItemId)))
        val expectedViewAction = MailboxViewAction.SwipeReadAction(userId, expectedItemId, false)

        // When
        mailboxViewModel.submit(expectedViewAction)

        // Then
        coVerifyOrder {
            markMessagesAsRead(userId, listOf(MessageId(expectedItemId)))
            mailboxReducer.newStateFrom(any(), expectedViewAction)
        }
        coVerify {
            markMessagesAsUnread wasNot Called
            markConversationsAsRead wasNot Called
            markConversationsAsUnread wasNot Called
        }
    }

    @Test
    fun `when swipe read is called for conversation grouping and item is unread then mark as read is called`() {
        // Given
        val expectedItemId = "itemId"
        expectViewModeForCurrentLocation(ConversationGrouping)
        expectMarkConversationsAsReadSucceeds(userId, listOf(buildMailboxUiModelItem(id = expectedItemId)))
        val expectedViewAction = MailboxViewAction.SwipeReadAction(userId, expectedItemId, false)

        // When
        mailboxViewModel.submit(expectedViewAction)

        // Then
        coVerify {
            markConversationsAsRead(userId, listOf(ConversationId(expectedItemId)))
            mailboxReducer.newStateFrom(any(), expectedViewAction)
        }
        coVerify {
            markMessagesAsRead wasNot Called
            markMessagesAsUnread wasNot Called
            markConversationsAsUnread wasNot Called
        }
    }

    @Test
    fun `when swipe archive is called for no conversation grouping then move is called`() = runTest {
        // Given
        val initialState = createMailboxDataState(selectedMailLabelId = MailLabelId.System.Inbox)
        val itemId = "itemId"
        val expectedLabelId = SystemLabelId.Archive.labelId
        val expectedViewAction = MailboxViewAction.SwipeArchiveAction(userId, itemId)

        expectedSelectedLabelCountStateChange(initialState)
        expectMoveMessagesSucceeds(userId, listOf(buildMailboxUiModelItem(id = itemId)), expectedLabelId)

        mailboxViewModel.state.test {
            advanceUntilIdle()

            // When
            mailboxViewModel.submit(expectedViewAction)

            // Then
            coVerify { moveMessages(userId, listOf(MessageId(itemId)), expectedLabelId) }
            coVerify { moveConversations wasNot Called }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `when swipe archive is called for conversation grouping then move is called`() = runTest {
        // Given
        val initialState = createMailboxDataState(selectedMailLabelId = MailLabelId.System.Inbox)
        val expectedItemId = "itemId"
        val expectedLabelId = SystemLabelId.Archive.labelId
        val expectedViewAction = MailboxViewAction.SwipeArchiveAction(userId, expectedItemId)

        expectViewModeForCurrentLocation(ConversationGrouping)
        expectedSelectedLabelCountStateChange(initialState)
        expectMoveConversationsSucceeds(userId, listOf(buildMailboxUiModelItem(id = expectedItemId)), expectedLabelId)

        mailboxViewModel.state.test {
            advanceUntilIdle()

            // When
            mailboxViewModel.submit(expectedViewAction)

            // Then
            coVerify { moveConversations(userId, listOf(ConversationId(expectedItemId)), expectedLabelId) }
            coVerify { moveMessages wasNot Called }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `when swipe archive is called when current label is archive, no action is performed`() = runTest {
        // Given
        val initialState = createMailboxDataState(selectedMailLabelId = MailLabelId.System.Archive)
        val expectedItemId = "itemId"
        val expectedViewAction = MailboxViewAction.SwipeArchiveAction(userId, expectedItemId)

        expectedSelectedLabelCountStateChange(initialState)

        mailboxViewModel.state.test {
            // When
            mailboxViewModel.submit(expectedViewAction)

            // Then
            coVerify { moveConversations wasNot Called }
            coVerify { moveMessages wasNot Called }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `when swipe spam is called for no conversation grouping then move is called`() = runTest {
        // Given
        val initialState = createMailboxDataState(selectedMailLabelId = MailLabelId.System.Inbox)
        val itemId = "itemId"
        val expectedLabelId = SystemLabelId.Spam.labelId
        val expectedViewAction = MailboxViewAction.SwipeSpamAction(userId, itemId)

        expectedSelectedLabelCountStateChange(initialState)
        expectMoveMessagesSucceeds(userId, listOf(buildMailboxUiModelItem(id = itemId)), expectedLabelId)

        mailboxViewModel.state.test {
            advanceUntilIdle()

            // When
            mailboxViewModel.submit(expectedViewAction)

            // Then
            coVerify { moveMessages(userId, listOf(MessageId(itemId)), expectedLabelId) }
            coVerify { moveConversations wasNot Called }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `when swipe spam is called for conversation grouping then move is called`() = runTest {
        // Given
        val initialState = createMailboxDataState(selectedMailLabelId = MailLabelId.System.Inbox)
        val expectedItemId = "itemId"
        val expectedLabelId = SystemLabelId.Spam.labelId
        val expectedViewAction = MailboxViewAction.SwipeSpamAction(userId, expectedItemId)

        expectViewModeForCurrentLocation(ConversationGrouping)
        expectedSelectedLabelCountStateChange(initialState)
        expectMoveConversationsSucceeds(userId, listOf(buildMailboxUiModelItem(id = expectedItemId)), expectedLabelId)

        mailboxViewModel.state.test {
            // When
            mailboxViewModel.submit(expectedViewAction)

            // Then
            coVerify { moveConversations(userId, listOf(ConversationId(expectedItemId)), expectedLabelId) }
            coVerify { moveMessages wasNot Called }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `when swipe spam is called when current label is spam, no action is performed`() = runTest {
        // Given
        val initialState = createMailboxDataState(selectedMailLabelId = MailLabelId.System.Spam)
        val expectedItemId = "itemId"
        val expectedViewAction = MailboxViewAction.SwipeSpamAction(userId, expectedItemId)

        expectedSelectedLabelCountStateChange(initialState)

        mailboxViewModel.state.test {
            // When
            mailboxViewModel.submit(expectedViewAction)

            // Then
            coVerify { moveConversations wasNot Called }
            coVerify { moveMessages wasNot Called }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `when swipe trash is called for no conversation grouping then move is called`() = runTest {
        // Given
        val initialState = createMailboxDataState(selectedMailLabelId = MailLabelId.System.Inbox)
        val itemId = "itemId"
        val expectedLabelId = SystemLabelId.Trash.labelId
        val expectedViewAction = MailboxViewAction.SwipeTrashAction(userId, itemId)

        expectedSelectedLabelCountStateChange(initialState)
        expectMoveMessagesSucceeds(userId, listOf(buildMailboxUiModelItem(id = itemId)), expectedLabelId)


        mailboxViewModel.state.test {
            // When
            mailboxViewModel.submit(expectedViewAction)

            // Then
            coVerify { moveMessages(userId, listOf(MessageId(itemId)), expectedLabelId) }
            coVerify { moveConversations wasNot Called }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `when swipe trash is called for conversation grouping then move is called`() = runTest {
        // Given
        val initialState = createMailboxDataState(selectedMailLabelId = MailLabelId.System.Inbox)
        val expectedItemId = "itemId"
        val expectedLabelId = SystemLabelId.Trash.labelId
        val expectedViewAction = MailboxViewAction.SwipeTrashAction(userId, expectedItemId)

        expectViewModeForCurrentLocation(ConversationGrouping)
        expectedSelectedLabelCountStateChange(initialState)
        expectMoveConversationsSucceeds(userId, listOf(buildMailboxUiModelItem(id = expectedItemId)), expectedLabelId)

        mailboxViewModel.state.test {
            // When
            mailboxViewModel.submit(expectedViewAction)

            // Then
            coVerify { moveConversations(userId, listOf(ConversationId(expectedItemId)), expectedLabelId) }
            coVerify { moveMessages wasNot Called }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `when swipe trash is called when current label is trash, no action is performed`() = runTest {
        // Given
        val initialState = createMailboxDataState(selectedMailLabelId = MailLabelId.System.Trash)
        val expectedItemId = "itemId"
        val expectedViewAction = MailboxViewAction.SwipeTrashAction(userId, expectedItemId)

        expectedSelectedLabelCountStateChange(initialState)

        mailboxViewModel.state.test {
            // When
            mailboxViewModel.submit(expectedViewAction)

            // Then
            coVerify { moveConversations wasNot Called }
            coVerify { moveMessages wasNot Called }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `when swipe star is called for no conversation grouping and item is starred then unstar is called`() {
        // Given
        val expectedItemId = "itemId"
        expectViewMode(NoConversationGrouping)
        expectedUnStarMessagesSucceeds(userId, listOf(buildMailboxUiModelItem(id = expectedItemId)))
        val expectedViewAction = MailboxViewAction.SwipeStarAction(userId, expectedItemId, true)

        // When
        mailboxViewModel.submit(expectedViewAction)

        // Then
        coVerifyOrder {
            unStarMessages(userId, listOf(MessageId(expectedItemId)))
            mailboxReducer.newStateFrom(any(), expectedViewAction)
        }
        coVerify {
            starMessages wasNot Called
            starConversations wasNot Called
            unStarConversations wasNot Called
        }
    }

    @Test
    fun `when swipe star is called for conversation grouping and item is starred then unstar is called`() {
        // Given
        val expectedItemId = "itemId"
        expectViewMode(ConversationGrouping)
        expectedUnStarConversationsSucceeds(userId, listOf(buildMailboxUiModelItem(id = expectedItemId)))
        expectViewModeForCurrentLocation(ConversationGrouping)
        val expectedViewAction = MailboxViewAction.SwipeStarAction(userId, expectedItemId, true)

        // When
        mailboxViewModel.submit(expectedViewAction)

        // Then
        coVerifyOrder {
            unStarConversations(userId, listOf(ConversationId(expectedItemId)))
            mailboxReducer.newStateFrom(any(), expectedViewAction)
        }
        coVerify {
            starConversations wasNot Called
            starMessages wasNot Called
            unStarMessages wasNot Called
        }
    }

    @Test
    fun `when swipe star is called for no conversation grouping and item is not starred then star is called`() {
        // Given
        val expectedItemId = "itemId"
        expectViewMode(NoConversationGrouping)
        expectedStarMessagesSucceeds(userId, listOf(buildMailboxUiModelItem(id = expectedItemId)))
        val expectedViewAction = MailboxViewAction.SwipeStarAction(userId, expectedItemId, false)

        // When
        mailboxViewModel.submit(expectedViewAction)

        // Then
        coVerifyOrder {
            starMessages(userId, listOf(MessageId(expectedItemId)))
            mailboxReducer.newStateFrom(any(), expectedViewAction)
        }
        coVerify {
            unStarMessages wasNot Called
            unStarConversations wasNot Called
            starConversations wasNot Called
        }
    }

    @Test
    fun `when swipe star is called for conversation grouping and item is not starred then star is called`() {
        // Given
        val expectedItemId = "itemId"
        expectViewMode(ConversationGrouping)
        expectedStarConversationsSucceeds(userId, listOf(buildMailboxUiModelItem(id = expectedItemId)))
        expectViewModeForCurrentLocation(ConversationGrouping)
        val expectedViewAction = MailboxViewAction.SwipeStarAction(userId, expectedItemId, false)

        // When
        mailboxViewModel.submit(expectedViewAction)

        // Then
        coVerifyOrder {
            starConversations(userId, listOf(ConversationId(expectedItemId)))
            mailboxReducer.newStateFrom(any(), expectedViewAction)
        }
        coVerify {
            unStarConversations wasNot Called
            unStarMessages wasNot Called
            starMessages wasNot Called
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
                swipeActions = null,
                searchState = MailboxSearchStateSampleData.NotSearching,
                showOnboardingUpselling = Effect.empty(),
                clearState = MailboxListState.Data.ClearState.Hidden
            ),
            unreadFilterState = UnreadFilterState.Data(
                numUnread = UnreadCountersTestData.labelToCounterMap[initialLocationMailLabelId.labelId]!!,
                isFilterEnabled = unreadFilterState
            )
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `onboarding state should be shown with upselling OFF when repository emits the preference with value true`() {
        runTest {
            // When
            coEvery {
                observeOnboarding()
            } returns flowOf(OnboardingPreference(display = true).right())
            every {
                mailboxReducer.newStateFrom(
                    any(),
                    any()
                )
            } returns MailboxStateSampleData.OnboardingShown
            val expectedUpsellingVisible = false
            expectObserveUpsellingVisibility(expectedUpsellingVisible)

            mailboxViewModel.state.test {
                val currentState = awaitItem()

                // Then
                verify(exactly = 1) {
                    mailboxReducer.newStateFrom(any(), MailboxEvent.ShowOnboarding(expectedUpsellingVisible))
                }
                assertEquals(OnboardingState.Shown.UpsellingOff, currentState.onboardingState)
            }
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `onboarding state should be shown with upselling ON when repository emits the preference with value true`() {
        runTest {
            // When
            coEvery {
                observeOnboarding()
            } returns flowOf(OnboardingPreference(display = true).right())
            every {
                mailboxReducer.newStateFrom(
                    any(),
                    any()
                )
            } returns MailboxStateSampleData.OnboardingShown.copy(
                onboardingState = OnboardingState.Shown.UpsellingOn
            )
            val expectedUpsellingVisible = true
            expectObserveUpsellingVisibility(expectedUpsellingVisible)

            mailboxViewModel.state.test {
                val currentState = awaitItem()

                // Then
                verify(exactly = 1) {
                    mailboxReducer.newStateFrom(any(), MailboxEvent.ShowOnboarding(expectedUpsellingVisible))
                }
                assertEquals(OnboardingState.Shown.UpsellingOn, currentState.onboardingState)
            }
        }
    }

    @Test
    fun `onboarding state should be hidden when an error occurs while observing the preference`() = runTest {
        // Given
        coEvery {
            observeOnboarding()
        } returns flowOf(PreferencesError.left())
        val expectedOnboardingState = OnboardingState.Hidden

        // When
        mailboxViewModel.state.test {
            // Then
            assertEquals(expectedOnboardingState, awaitItem().onboardingState)
        }
    }

    @Test
    fun `should call repository save method when closing onboarding`() = runTest {
        // Given
        coEvery {
            saveOnboarding(display = false)
        } returns Unit.right()

        // When
        mailboxViewModel.submit(MailboxViewAction.CloseOnboarding)

        // When
        mailboxViewModel.state.test {
            // Then
            coVerify { saveOnboarding(display = false) }
            assertEquals(OnboardingState.Hidden, awaitItem().onboardingState)
        }
    }

    @Test
    fun `should emit ShowOnboardingUpselling when closing onboarding`() = runTest {
        // Given
        coEvery {
            saveOnboarding(display = false)
        } returns Unit.right()

        val initialMailboxState = createMailboxDataState().copy(
            onboardingState = OnboardingState.Shown.UpsellingOn
        )
        val expectedState = initialMailboxState.copy(
            showOnboardingUpselling = Effect.of(Unit)
        )

        every { mailboxReducer.newStateFrom(any(), any()) } returns initialMailboxState
        every {
            mailboxReducer.newStateFrom(
                initialMailboxState,
                MailboxViewAction.CloseOnboarding
            )
        } returns expectedState

        // When
        mailboxViewModel.submit(MailboxViewAction.CloseOnboarding)

        // When
        mailboxViewModel.state.test {
            // Then
            val actual = awaitItem()

            assertEquals(Effect.of(Unit), actual.showOnboardingUpselling)
        }
    }

    @Test
    fun `when enter search mode action is submitted, search mode is updated and emitted`() = runTest {
        // Given
        val expectedState = MailboxStateSampleData.Inbox.copy(
            mailboxListState = MailboxListState.Data.ViewMode(
                currentMailLabel = MailLabel.System(MailLabelId.System.AllMail),
                openItemEffect = Effect.empty(),
                scrollToMailboxTop = Effect.empty(),
                offlineEffect = Effect.empty(),
                refreshErrorEffect = Effect.of(Unit),
                refreshRequested = false,
                swipeActions = null,
                searchState = MailboxSearchStateSampleData.NewSearch,
                showOnboardingUpselling = Effect.empty(),
                clearState = MailboxListState.Data.ClearState.Hidden
            )
        )
        every {
            mailboxReducer.newStateFrom(
                MailboxStateSampleData.Loading,
                MailboxViewAction.EnterSearchMode
            )
        } returns expectedState

        // When
        mailboxViewModel.submit(MailboxViewAction.EnterSearchMode)
        mailboxViewModel.state.test {

            // Then
            assertEquals(expectedState, awaitItem())
        }
    }

    @Test
    fun `when exit search mode action is submitted, search mode is updated and emitted`() = runTest {
        // Given
        val expectedState = MailboxStateSampleData.Inbox.copy(
            mailboxListState = MailboxListState.Data.ViewMode(
                currentMailLabel = MailLabel.System(MailLabelId.System.Inbox),
                openItemEffect = Effect.empty(),
                scrollToMailboxTop = Effect.empty(),
                offlineEffect = Effect.empty(),
                refreshErrorEffect = Effect.of(Unit),
                refreshRequested = false,
                swipeActions = null,
                searchState = MailboxSearchStateSampleData.NotSearching,
                showOnboardingUpselling = Effect.empty(),
                clearState = MailboxListState.Data.ClearState.Hidden
            )
        )
        every {
            mailboxReducer.newStateFrom(
                MailboxStateSampleData.Loading,
                MailboxViewAction.ExitSearchMode
            )
        } returns expectedState
        coEvery { deleteSearchResults.invoke(any(), any()) } just runs

        // When
        mailboxViewModel.submit(MailboxViewAction.ExitSearchMode)
        mailboxViewModel.state.test {

            // Then
            assertEquals(expectedState, awaitItem())
            coVerify { deleteSearchResults.invoke(any(), any()) }
        }
    }

    @Test
    fun `when search query action is submitted, search mode is updated and emitted`() = runTest {
        // Given
        val queryText = "query"
        val expectedState = MailboxStateSampleData.Loading.copy(
            mailboxListState = MailboxListState.Data.ViewMode(
                currentMailLabel = MailLabel.System(initialLocationMailLabelId),
                openItemEffect = Effect.empty(),
                scrollToMailboxTop = Effect.empty(),
                offlineEffect = Effect.empty(),
                refreshErrorEffect = Effect.of(Unit),
                refreshRequested = false,
                swipeActions = null,
                searchState = MailboxSearchStateSampleData.SearchLoading,
                showOnboardingUpselling = Effect.empty(),
                clearState = MailboxListState.Data.ClearState.Hidden
            )
        )
        every {
            mailboxReducer.newStateFrom(
                MailboxStateSampleData.Loading,
                MailboxViewAction.SearchQuery(queryText)
            )
        } returns expectedState

        // When
        mailboxViewModel.submit(MailboxViewAction.SearchQuery(queryText))
        mailboxViewModel.state.test {

            // Then
            assertEquals(expectedState, awaitItem())
        }
    }

    @Test
    fun `when search result action is submitted, search mode is updated and emitted`() = runTest {
        // Given
        val expectedState = MailboxStateSampleData.Loading.copy(
            mailboxListState = MailboxListState.Data.ViewMode(
                currentMailLabel = MailLabel.System(initialLocationMailLabelId),
                openItemEffect = Effect.empty(),
                scrollToMailboxTop = Effect.empty(),
                offlineEffect = Effect.empty(),
                refreshErrorEffect = Effect.of(Unit),
                refreshRequested = false,
                swipeActions = null,
                searchState = MailboxSearchStateSampleData.SearchData,
                showOnboardingUpselling = Effect.empty(),
                clearState = MailboxListState.Data.ClearState.Hidden
            )
        )
        every {
            mailboxReducer.newStateFrom(
                MailboxStateSampleData.Loading,
                MailboxViewAction.SearchResult
            )
        } returns expectedState

        // When
        mailboxViewModel.submit(MailboxViewAction.SearchResult)
        mailboxViewModel.state.test {

            // Then
            assertEquals(expectedState, awaitItem())
        }
    }

    @Test
    fun `search will be performed in AllMail label independent from current label`() = runTest {
        // Given
        val queryText = MailboxSearchStateSampleData.QueryString
        val currentLocationFlow = MutableStateFlow<MailLabelId>(initialLocationMailLabelId)
        val initialMailboxState = createMailboxDataState()
        val mailboxSearchQueryState = initialMailboxState.copy(
            mailboxListState = (initialMailboxState.mailboxListState as MailboxListState.Data.ViewMode).copy(
                searchState = MailboxSearchStateSampleData.SearchLoading
            )
        )
        val userIds = listOf(userId)
        every { selectedMailLabelId.flow } returns currentLocationFlow
        every {
            pagerFactory.create(
                userIds,
                any(),
                false,
                any(),
                any(),
                emptyLabelInProgressSignal
            )
        } returns mockk mockPager@{
            every { this@mockPager.flow } returns flowOf(PagingData.from(listOf(unreadMailboxItem)))
        }
        every { selectedMailLabelId.set(any()) } returns Unit
        every { mailboxReducer.newStateFrom(any(), any()) } returns initialMailboxState
        every {
            mailboxReducer.newStateFrom(
                any(),
                MailboxViewAction.SearchQuery(queryText)
            )
        } returns mailboxSearchQueryState
        coEvery { deleteSearchResults.invoke(any(), any()) } just runs

        mailboxViewModel.items.test {
            // Then
            verify {
                pagerFactory.create(
                    userIds,
                    initialLocationMailLabelId,
                    false,
                    any(),
                    any(),
                    emptyLabelInProgressSignal
                )
            }

            // When
            mailboxViewModel.submit(MailboxViewAction.SearchQuery(queryText))

            // Then
            awaitItem()
            verify {
                pagerFactory.create(
                    userIds,
                    MailLabelId.System.AllMail,
                    false,
                    any(),
                    queryText,
                    emptyLabelInProgressSignal
                )
            }

            // When
            mailboxViewModel.submit(MailboxViewAction.ExitSearchMode)

            // Then
            awaitItem()
            verify {
                pagerFactory.create(
                    userIds,
                    initialLocationMailLabelId,
                    false,
                    any(),
                    any(),
                    emptyLabelInProgressSignal
                )
            }

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `should emit upselling data event when showing the upselling bottom sheet was requested`() = runTest {
        // Given
        val initialState = createMailboxDataState()
        expectViewMode(NoConversationGrouping)
        expectedSelectedLabelCountStateChange(initialState)
        expectRequestUpsellingBottomSheet(initialState)
        expectUpsellingBottomSheetDataLoaded(initialState)

        mailboxViewModel.state.test {
            awaitItem() // First emission for selected user

            // When
            mailboxViewModel.submit(MailboxViewAction.RequestUpsellingBottomSheet)

            // Then
            verify(exactly = 1) {
                mailboxReducer.newStateFrom(initialState, MailboxViewAction.RequestUpsellingBottomSheet)
            }
            verify(exactly = 1) {
                mailboxReducer.newStateFrom(
                    initialState,
                    MailboxEvent.MailboxBottomSheetEvent(UpsellingBottomSheetState.UpsellingBottomSheetEvent.Ready)
                )
            }
        }
    }

    @Test
    fun `navigate to inbox label will trigger selected mail label use case`() = runTest {
        // Given
        coJustRun { selectedMailLabelId.set(MailLabelId.System.Inbox) }

        // When
        mailboxViewModel.submit(MailboxViewAction.NavigateToInboxLabel)

        // Then
        coVerify { selectedMailLabelId.set(MailLabelId.System.Inbox) }
    }

    @Test
    fun `should create new state for showing the rating booster when it should be shown`() = runTest {
        // Given
        every { shouldShowRatingBooster(userId) } returns flowOf(true)

        // When
        mailboxViewModel.state.test {
            awaitItem()

            // Then
            verify(exactly = 1) {
                mailboxReducer.newStateFrom(any(), MailboxEvent.ShowRatingBooster)
            }
            coVerify { recordRatingBoosterTriggered(userId) }
        }
    }

    @Test
    fun `should call use case for showing the rating booster when it should be shown`() = runTest {
        // Given
        val context = mockk<Activity>()

        // When
        mailboxViewModel.submit(MailboxViewAction.ShowRatingBooster(context))

        // Then
        verify { showRatingBooster(context) }
    }

    @Test
    fun `should create new state for showing the Onboarding Upselling when it should be shown`() = runTest {
        // Given
        val initialMailboxState = createMailboxDataState()
        val expectedState = initialMailboxState.copy(
            showOnboardingUpselling = Effect.of(Unit)
        )

        every { mailboxReducer.newStateFrom(any(), any()) } returns initialMailboxState
        every {
            mailboxReducer.newStateFrom(
                initialMailboxState,
                MailboxViewAction.ShowOnboardingUpselling
            )
        } returns expectedState

        // When
        mailboxViewModel.state.test {
            // Then

            awaitItem()

            mailboxViewModel.submit(MailboxViewAction.ShowOnboardingUpselling)

            val actual = awaitItem()

            assertEquals(Effect.of(Unit), actual.showOnboardingUpselling)
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
                        listOf(ActionUiModelSample.Archive, ActionUiModelSample.Trash).toImmutableList()
                    )
                )
            )
        } returns expectedState
    }

    private fun returnExpectedStateForMarkAsUnread(intermediateState: MailboxState, expectedState: MailboxState) {
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

    private fun expectedLabelAsBottomSheetRequestedStateChange(
        expectedCustomLabels: List<MailLabelUiModel.Custom>,
        bottomSheetState: MailboxState
    ) {
        val selectedLabels = listOf<LabelId>()
        every {
            mailboxReducer.newStateFrom(
                currentState = any(),
                operation = MailboxEvent.MailboxBottomSheetEvent(
                    LabelAsBottomSheetState.LabelAsBottomSheetEvent.ActionData(
                        customLabelList = expectedCustomLabels.toImmutableList(),
                        selectedLabels = selectedLabels.toImmutableList(),
                        partiallySelectedLabels = selectedLabels.toImmutableList()
                    )
                )
            )
        } returns bottomSheetState
    }

    private fun expectedMoveToBottomSheetRequestedStateChange(
        mailLabelUiModel: List<MailLabelUiModel>,
        bottomSheetState: MailboxState
    ) {
        every {
            mailboxReducer.newStateFrom(
                currentState = any(),
                operation = MailboxEvent.MailboxBottomSheetEvent(
                    MoveToBottomSheetState.MoveToBottomSheetEvent.ActionData(mailLabelUiModel.toImmutableList())
                )
            )
        } returns bottomSheetState
    }

    private fun expectedMoreActionBottomSheetRequestedStateChange(
        actionUiModels: List<ActionUiModel>,
        bottomSheetState: MailboxState
    ) {
        every {
            mailboxReducer.newStateFrom(
                currentState = any(),
                operation = MailboxEvent.MailboxBottomSheetEvent(
                    MailboxMoreActionsBottomSheetState.MailboxMoreActionsBottomSheetEvent.ActionData(
                        actionUiModels.toImmutableList()
                    )
                )
            )
        } returns bottomSheetState
    }

    private fun expectedLabelAsStateChange(selectedItems: List<MailboxItemUiModel>, labelId: LabelId) {
        every {
            mailboxReducer.newStateFrom(any(), MailboxViewAction.LabelAsToggleAction(labelId))
        } returns createMailboxStateWithLabelAsBottomSheet(selectedItems, true)
    }

    private fun expectedMoveToStateChange(selectedItem: MailLabelId, expectedState: MailboxState) {
        every {
            mailboxReducer.newStateFrom(any(), MailboxViewAction.MoveToDestinationSelected(selectedItem))
        } returns expectedState
    }

    private fun expectedLabelAsConfirmed(expectedState: MailboxState, archiveSelected: Boolean = false) {
        every {
            mailboxReducer.newStateFrom(any(), MailboxViewAction.LabelAsConfirmed(archiveSelected))
        } returns expectedState
    }

    private fun expectedMoveToConfirmed(expectedState: MailboxState) {
        every { mailboxReducer.newStateFrom(any(), MailboxViewAction.MoveToConfirmed) } returns expectedState
    }

    private fun returnExpectedStateWhenStarringSucceeds(expectedState: MailboxState) {
        every { mailboxReducer.newStateFrom(any(), MailboxViewAction.Star) } returns expectedState
    }

    private fun returnExpectedStateWhenUnStarringSucceeds(expectedState: MailboxState) {
        every { mailboxReducer.newStateFrom(any(), MailboxViewAction.UnStar) } returns expectedState
    }

    private fun expectedReducerResult(operation: MailboxOperation, expectedState: MailboxState) {
        every { mailboxReducer.newStateFrom(any(), operation) } returns expectedState
    }

    private fun expectedLabelAsBottomSheetDismissed(expectedState: MailboxState) {
        every { mailboxReducer.newStateFrom(any(), MailboxViewAction.DismissBottomSheet) } returns expectedState
    }

    private fun expectRelabelMessagesSucceeds(
        selectedMessages: List<MessageId>,
        currentSelections: LabelSelectionList,
        updatedSelections: LabelSelectionList
    ) {
        coEvery {
            relabelMessages(userId, selectedMessages, currentSelections, updatedSelections)
        } returns listOf<DomainMessage>().right()
    }

    private fun expectRelabelConversationSucceeds(
        selectedConversation: List<ConversationId>,
        currentSelections: LabelSelectionList,
        updatedSelections: LabelSelectionList
    ) {
        coEvery {
            relabelConversations(userId, selectedConversation, currentSelections, updatedSelections)
        } returns listOf<DomainConversation>().right()
    }

    private fun expectViewMode(viewMode: ViewMode) {
        every { observeCurrentViewMode(any()) } returns flowOf(viewMode)
    }

    private fun expectViewModeForCurrentLocation(viewMode: ViewMode) {
        every { observeCurrentViewMode(any(), any()) } returns flowOf(viewMode)
    }

    private fun expectObserveCustomMailLabelSucceeds(expectedLabels: List<MailLabel.Custom>) {
        every { observeCustomMailLabels(userId) } returns flowOf(expectedLabels.right())
    }

    private fun expectDestinationMailLabelsSucceeds(expectedMailLabels: MailLabels) {
        every { observeDestinationMailLabels(userId) } returns flowOf(expectedMailLabels)
    }

    private fun expectDestinationMailLabelsFails() {
        every { observeDestinationMailLabels(userId) } returns flowOf()
    }

    private fun expectFolderColorSettingsFails() {
        every { observeFolderColorSettings(userId) } returns flowOf()
    }

    private fun expectGetMessagesWithLabelsSucceeds(messageIds: List<MessageId>) {
        coEvery { getMessagesWithLabels(userId, messageIds) } returns listOf(
            MessageWithLabels(
                message = MessageSample.Invoice,
                labels = listOf()
            ),
            MessageWithLabels(
                message = MessageSample.AlphaAppQAReport,
                labels = listOf()
            )
        ).right()
    }

    private fun expectGetConversationMessagesWithLabelsSucceeds(conversationIds: List<ConversationId>) {
        coEvery { getConversationsWithLabels(userId, conversationIds) } returns listOf(
            ConversationWithLabels(
                conversation = ConversationSample.AlphaAppFeedback,
                labels = listOf()
            ),
            ConversationWithLabels(
                conversation = ConversationSample.WeatherForecast,
                labels = listOf()
            )
        ).right()
    }

    private fun returnExpectedStateForMarkAsRead(intermediateState: MailboxState, expectedState: MailboxState) {
        every { mailboxReducer.newStateFrom(intermediateState, MailboxViewAction.MarkAsRead) } returns expectedState
    }

    private fun returnExpectedStateForTrash(
        intermediateState: MailboxState,
        expectedState: MailboxState,
        expectedItemCount: Int
    ) {
        every {
            mailboxReducer.newStateFrom(intermediateState, MailboxEvent.Trash(expectedItemCount))
        } returns expectedState
    }

    private fun returnExpectedStateForDeleteConfirmed(
        intermediateState: MailboxState,
        expectedState: MailboxState,
        viewMode: ViewMode,
        expectedItemCount: Int
    ) {
        every {
            mailboxReducer.newStateFrom(intermediateState, MailboxEvent.DeleteConfirmed(viewMode, expectedItemCount))
        } returns expectedState
    }

    private fun returnExpectedStateForDeleteAllConfirmed(expectedState: MailboxState, viewMode: ViewMode) {
        every {
            mailboxReducer.newStateFrom(any(), MailboxEvent.DeleteAllConfirmed(viewMode))
        } returns expectedState
    }

    private fun returnExpectedStateForDeleteAllDismissed(expectedState: MailboxState) {
        every { mailboxReducer.newStateFrom(any(), MailboxViewAction.DeleteAllDialogDismissed) } returns expectedState
    }

    private fun returnExpectedStateForDeleteDismissed(intermediateState: MailboxState, expectedState: MailboxState) {
        every {
            mailboxReducer.newStateFrom(intermediateState, MailboxViewAction.DeleteDialogDismissed)
        } returns expectedState
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

    private fun expectMoveConversationsSucceeds(
        userId: UserId,
        items: List<MailboxItemUiModel>,
        labelId: LabelId
    ) {
        coEvery { moveConversations(userId, items.map { ConversationId(it.id) }, labelId) } returns Unit.right()
    }

    private fun expectMoveMessagesSucceeds(
        userId: UserId,
        items: List<MailboxItemUiModel>,
        labelId: LabelId
    ) {
        coEvery { moveMessages(userId, items.map { MessageId(it.id) }, labelId) } returns Unit.right()
    }

    private fun expectDeleteConversationsSucceeds(
        userId: UserId,
        items: List<MailboxItemUiModel>,
        labelId: LabelId
    ) {
        coJustRun { deleteConversations(userId, items.map { ConversationId(it.id) }, labelId) }
    }

    private fun expectDeleteConversationsSucceeds(userId: UserId, labelId: LabelId) {
        coJustRun { deleteConversations(userId, labelId) }
    }

    private fun expectDeleteMessagesSucceeds(
        userId: UserId,
        items: List<MailboxItemUiModel>,
        labelId: LabelId
    ) {
        coJustRun { deleteMessages(userId, items.map { MessageId(it.id) }, labelId) }
    }

    private fun expectDeleteMessagesSucceeds(userId: UserId, labelId: LabelId) {
        coJustRun { deleteMessages(userId, labelId) }
    }

    private fun expectedStarMessagesSucceeds(userId: UserId, items: List<MailboxItemUiModel>) {
        coJustRun { starMessages(userId, items.map { MessageId(it.id) }) }
    }

    private fun expectedUnStarMessagesSucceeds(userId: UserId, items: List<MailboxItemUiModel>) {
        coJustRun { unStarMessages(userId, items.map { MessageId(it.id) }) }
    }

    private fun expectedStarConversationsSucceeds(userId: UserId, items: List<MailboxItemUiModel>) {
        coJustRun { starConversations(userId, items.map { ConversationId(it.id) }) }
    }

    private fun expectedUnStarConversationsSucceeds(userId: UserId, items: List<MailboxItemUiModel>) {
        coJustRun { unStarConversations(userId, items.map { ConversationId(it.id) }) }
    }

    private fun expectRequestUpsellingBottomSheet(initialState: MailboxState) {
        every {
            mailboxReducer.newStateFrom(initialState, MailboxViewAction.RequestUpsellingBottomSheet)
        } returns initialState
    }

    private fun expectObserveUpsellingVisibility(visible: Boolean) {
        every { observeUpsellingVisibility.invoke(any()) } returns flowOf(visible)
    }

    private fun expectUpsellingBottomSheetDataLoaded(initialState: MailboxState) {
        every {
            mailboxReducer.newStateFrom(
                initialState,
                MailboxEvent.MailboxBottomSheetEvent(UpsellingBottomSheetState.UpsellingBottomSheetEvent.Ready)
            )
        } returns initialState
    }

    private fun createMailboxStateWithLabelAsBottomSheet(
        selectedMailboxItems: List<MailboxItemUiModel>,
        selected: Boolean
    ) = MailboxStateSampleData.createSelectionMode(
        selectedMailboxItemUiModels = selectedMailboxItems,
        currentMailLabel = MailLabel.System(MailLabelId.System.Trash),
        bottomSheetState = BottomSheetState(
            LabelAsBottomSheetState.Data(
                listOf(
                    LabelUiModelWithSelectedState(
                        labelUiModel = MailLabelTestData.customLabelOne.toCustomUiModel(
                            defaultFolderColorSettings, emptyMap(), null
                        ),
                        selectedState = when {
                            selected -> LabelSelectedState.Selected
                            else -> LabelSelectedState.NotSelected
                        }
                    ),
                    LabelUiModelWithSelectedState(
                        labelUiModel = MailLabelTestData.customLabelTwo.toCustomUiModel(
                            defaultFolderColorSettings, emptyMap(), null
                        ),
                        selectedState = when {
                            selected -> LabelSelectedState.Selected
                            else -> LabelSelectedState.NotSelected
                        }
                    )
                ).toPersistentList(),
                messageIdInConversation = null
            )
        )
    )

    private fun createMailboxStateWithMoveToBottomSheet(
        selectedMailboxItems: List<MailboxItemUiModel>,
        selectedItem: MailLabelUiModel? = null
    ) = MailboxStateSampleData.createSelectionMode(
        selectedMailboxItemUiModels = selectedMailboxItems,
        currentMailLabel = MailLabel.System(MailLabelId.System.Trash),
        bottomSheetState = BottomSheetState(
            MoveToBottomSheetState.Data(MailLabelUiModelTestData.customLabelList, selectedItem, null)
        )
    )

    private fun createMailboxStateWithMoreActionBottomSheet(
        selectedMailboxItems: List<MailboxItemUiModel>,
        expectedBottomSheetContent: MailboxMoreActionsBottomSheetState
    ) = MailboxStateSampleData.createSelectionMode(
        selectedMailboxItemUiModels = selectedMailboxItems,
        currentMailLabel = MailLabel.System(MailLabelId.System.Trash),
        bottomSheetState = BottomSheetState(expectedBottomSheetContent)
    )
}
