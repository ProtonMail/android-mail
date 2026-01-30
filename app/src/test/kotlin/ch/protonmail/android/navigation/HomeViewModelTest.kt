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

package ch.protonmail.android.navigation

import android.content.Intent
import android.net.Uri
import androidx.navigation.NavOptions
import app.cash.turbine.test
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.data.file.IntentExtraKeys
import ch.protonmail.android.mailcommon.data.file.getShareInfo
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.model.IntentShareInfo
import ch.protonmail.android.mailcommon.domain.model.UndoSendError
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailcommon.domain.sample.UserSample
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.usecase.FormatFullDate
import ch.protonmail.android.mailcomposer.domain.model.MessageSendingStatus
import ch.protonmail.android.mailcomposer.domain.model.SendErrorReason
import ch.protonmail.android.mailcomposer.domain.usecase.DiscardDraft
import ch.protonmail.android.mailcomposer.domain.usecase.MarkMessageSendingStatusesAsSeen
import ch.protonmail.android.mailcomposer.domain.usecase.ObserveSendingMessagesStatus
import ch.protonmail.android.mailcomposer.domain.usecase.UndoSendMessage
import ch.protonmail.android.mailmailbox.domain.usecase.RecordMailboxScreenView
import ch.protonmail.android.mailmessage.domain.model.PreviousScheduleSendTime
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.mailmessage.domain.usecase.CancelScheduleSendMessage
import ch.protonmail.android.mailpinlock.domain.usecase.ShouldPresentPinInsertionScreen
import ch.protonmail.android.mailsession.domain.eventloop.EventLoopErrorSignal
import ch.protonmail.android.mailsession.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.navigation.mapper.IntentMapper
import ch.protonmail.android.navigation.model.Destination
import ch.protonmail.android.navigation.model.HomeState
import ch.protonmail.android.navigation.model.NavigationEffect
import ch.protonmail.android.navigation.reducer.HomeNavigationEventsReducer
import ch.protonmail.android.navigation.share.NewIntentObserver
import ch.protonmail.android.test.utils.rule.MainDispatcherRule
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import org.junit.Assert.assertNull
import org.junit.Rule
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.DurationUnit
import kotlin.time.Instant
import kotlin.time.toDuration

class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val user = UserSample.Primary
    private val userId = UserIdSample.Primary
    private val messageId = MessageIdSample.LocalDraft


    private val observePrimaryUserId = mockk<ObservePrimaryUserId> {
        every { this@mockk() } returns MutableStateFlow<UserId?>(userId)
    }

    private val observeSendingMessagesStatus = mockk<ObserveSendingMessagesStatus> {
        coEvery { this@mockk.invoke(any()) } returns flowOf(MessageSendingStatus.NoStatus(messageId))
    }

    private val recordMailboxScreenView = mockk<RecordMailboxScreenView>(relaxUnitFun = true)

    private val shouldPresentPinInsertionScreen = mockk<ShouldPresentPinInsertionScreen> {
        every { this@mockk.invoke() } returns flowOf(false)
    }

    private val newIntentObserver = mockk<NewIntentObserver>(relaxUnitFun = true) {
        every { this@mockk() } returns emptyFlow()
    }

    private val discardDraft = mockk<DiscardDraft>(relaxUnitFun = true)

    private val undoSendMessage = mockk<UndoSendMessage>(relaxUnitFun = true)
    private val cancelScheduleSendMessage = mockk<CancelScheduleSendMessage>(relaxUnitFun = true)
    private val markMessageSendingStatusesAsSeen = mockk<MarkMessageSendingStatusesAsSeen>(relaxUnitFun = true)

    private val eventLoopErrorSignal = mockk<EventLoopErrorSignal> {
        every { this@mockk.observeEventLoopErrors() } returns flowOf()
    }

    private val formatFullDate = mockk<FormatFullDate>()
    private val intentMapper = IntentMapper()
    private val navigationEventsReducer = HomeNavigationEventsReducer()

    private val homeViewModel by lazy {
        HomeViewModel(
            observeSendingMessagesStatus,
            recordMailboxScreenView,
            discardDraft,
            undoSendMessage,
            markMessageSendingStatusesAsSeen,
            formatFullDate,
            cancelScheduleSendMessage,
            eventLoopErrorSignal,
            observePrimaryUserId,
            newIntentObserver,
            intentMapper,
            navigationEventsReducer
        )
    }

    @BeforeTest
    fun setUp() {
        mockkStatic(Uri::class)
    }

    @AfterTest
    fun teardown() {
        unmockkAll()
        unmockkStatic(Uri::class)
    }

    @Test
    fun `when initialized then emit initial state`() = runTest {
        // When
        homeViewModel.state.test {
            val actualItem = awaitItem()
            val expectedItem = HomeState.Initial

            // Then
            assertEquals(expectedItem, actualItem)
        }
    }

    @Test
    fun `should emit a new state with started from launcher set when intent with main action is received`() = runTest {
        // Given
        val mainIntent = mockIntent(
            action = Intent.ACTION_MAIN,
            data = null,
            externalBoolean = false,
            categories = setOf(Intent.CATEGORY_LAUNCHER)
        )
        every { newIntentObserver() } returns flowOf(mainIntent)

        // When
        homeViewModel.state.test {
            val actualItem = awaitItem()
            val expectedItem = HomeState.Initial.copy(
                startedFromLauncher = true
            )

            // Then
            assertEquals(expectedItem, actualItem)
        }
    }

    @Test
    fun `when sending message status changes, it should update state correctly`() = runTest {
        // Given
        val sendingMessageStatusFlow = MutableStateFlow<MessageSendingStatus>(
            MessageSendingStatus.MessageSentUndoable(messageId, 5000L.toDuration(DurationUnit.MILLISECONDS))
        )
        coEvery { observeSendingMessagesStatus(userId) } returns sendingMessageStatusFlow

        // When
        homeViewModel.state.test {
            val actualItem = awaitItem()
            val expectedItem = HomeState(
                messageSendingStatusEffect = Effect.of(
                    MessageSendingStatus.MessageSentUndoable(
                        messageId,
                        5000L.toDuration(DurationUnit.MILLISECONDS)
                    )
                ),
                navigateToEffect = Effect.empty(),
                startedFromLauncher = false
            )

            // Then
            assertEquals(expectedItem, actualItem)
        }
    }

    @Test
    fun `when observe sending message status emits Send and then None then emit only send effect`() = runTest {
        // Given
        val sendingMessageStatusFlow = MutableStateFlow<MessageSendingStatus>(
            MessageSendingStatus.MessageSentFinal(messageId)
        )
        coEvery { observeSendingMessagesStatus(user.userId) } returns sendingMessageStatusFlow

        // When
        homeViewModel.state.test {
            val actualItem = awaitItem()
            val expectedItem = HomeState(
                messageSendingStatusEffect = Effect.of(MessageSendingStatus.MessageSentFinal(messageId)),
                navigateToEffect = Effect.empty(),
                startedFromLauncher = false
            )
            sendingMessageStatusFlow.emit(MessageSendingStatus.NoStatus(messageId))

            // Then
            assertEquals(expectedItem, actualItem)
        }
    }

    @Test
    fun `should call undo send message use case when undo is triggered`() = runTest {
        // Given
        val messageId = MessageIdSample.LocalDraft

        coEvery { undoSendMessage(userId, messageId) } returns Unit.right()

        // When
        homeViewModel.undoSendMessage(messageId)

        // Then
        coVerify { undoSendMessage(userId, messageId) }
    }

    @Test
    fun `when observe sending message status emits error then emit effect and reset sending messages status`() =
        runTest {
            // Given
            coEvery { observeSendingMessagesStatus(user.userId) } returns flowOf(
                MessageSendingStatus.SendMessageError(
                    messageId, SendErrorReason.OtherDataError(DataError.Local.NoDataCached)
                )
            )

            // When
            homeViewModel.state.test {
                val actualItem = awaitItem()
                val expectedItem = HomeState(
                    messageSendingStatusEffect = Effect.of(
                        MessageSendingStatus.SendMessageError(
                            messageId, SendErrorReason.OtherDataError(DataError.Local.NoDataCached)
                        )
                    ),
                    navigateToEffect = Effect.empty(),
                    startedFromLauncher = false
                )

                // Then
                assertEquals(expectedItem, actualItem)
            }
        }

    @Test
    fun `when pin lock screen needs to be shown, the effect is emitted accordingly`() = runTest {
        // Given
        every { shouldPresentPinInsertionScreen() } returns flowOf(true)

        // When + Then
        homeViewModel.state.test {
            val actualItem = awaitItem()
            val expectedItem = HomeState.Initial
            assertEquals(expectedItem, actualItem)
        }
    }

    @Test
    fun `should emit a new state with navigation effect when a share intent is received`() = runTest {
        // Given
        val fileUriStr = "content://media/1234"
        val fileUri = mockk<Uri> {
            every { this@mockk.host } returns null
        }
        val intentShareInfo = IntentShareInfo.Empty.copy(
            attachmentUris = listOf(fileUriStr)
        )
        val shareIntent = mockIntent(
            action = Intent.ACTION_SEND,
            data = fileUri,
            externalBoolean = true
        )
        every { shareIntent.scheme } returns ""
        // Mock the extension function
        mockkStatic("ch.protonmail.android.mailcommon.data.file.IntentShareExtensionsKt")
        every { any<Intent>().getShareInfo() } returns intentShareInfo

        every { shouldPresentPinInsertionScreen() } returns flowOf()
        every { newIntentObserver() } returns flowOf(shareIntent)

        // When + Then
        homeViewModel.state.test {
            val actualItem = awaitItem()
            assertNotNull(actualItem.navigateToEffect.consume())
        }
    }

    @Test
    fun `should not emit a new navigation state when file share info is empty`() = runTest {
        // Given
        val fileUri = mockk<Uri> {
            every { this@mockk.host } returns null
        }
        val shareIntent = mockIntent(
            action = Intent.ACTION_VIEW,
            data = fileUri,
            externalBoolean = true
        )
        every { shareIntent.scheme } returns ""
        // Mock the extension function
        mockkStatic("ch.protonmail.android.mailcommon.data.file.IntentShareExtensionsKt")
        every { any<Intent>().getShareInfo() } returns IntentShareInfo.Empty

        every { shouldPresentPinInsertionScreen() } returns flowOf()
        every { newIntentObserver() } returns flowOf(shareIntent)

        // When + Then
        homeViewModel.state.test {
            val actualItem = awaitItem()
            assertNull(actualItem.navigateToEffect.consume())
        }
    }

    @Test
    fun `should emit a new navigation state when app was started from launcher and external intent received`() =
        runTest {
            // Given
            val fileUriStr = "content://media/1234"
            val fileUri = mockk<Uri> {
                every { this@mockk.host } returns null
            }
            val intentShareInfo = IntentShareInfo.Empty.copy(
                attachmentUris = listOf(fileUriStr)
            )
            val shareIntent = mockIntent(
                action = Intent.ACTION_SEND,
                data = fileUri,
                externalBoolean = true
            )
            val mainIntent = mockIntent(
                action = Intent.ACTION_MAIN,
                data = null,
                externalBoolean = false,
                categories = setOf(Intent.CATEGORY_LAUNCHER)
            )
            every { shareIntent.scheme } returns ""
            // Mock the extension function
            mockkStatic("ch.protonmail.android.mailcommon.data.file.IntentShareExtensionsKt")
            every { any<Intent>().getShareInfo() } returns intentShareInfo

            every { shouldPresentPinInsertionScreen() } returns flowOf()
            every { newIntentObserver() } returns flowOf(mainIntent, shareIntent)

            // When + Then
            homeViewModel.state.test {
                val effect = awaitItem().navigateToEffect.consume()
                assertTrue { effect is NavigationEffect.NavigateTo }
            }
        }

    @Test
    fun `should emit a new navigation state when app was started from launcher and internal intent`() = runTest {
        // Given
        val fileUriStr = "content://media/1234"
        val fileUri = mockk<Uri> {
            every { this@mockk.host } returns null
        }
        val intentShareInfo = IntentShareInfo.Empty.copy(
            attachmentUris = listOf(fileUriStr)
        )
        val shareIntent = mockIntent(
            action = Intent.ACTION_SEND,
            data = fileUri,
            externalBoolean = false
        )
        val mainIntent = mockIntent(
            action = Intent.ACTION_MAIN,
            data = null,
            externalBoolean = false,
            categories = setOf(Intent.CATEGORY_LAUNCHER)
        )
        every { shareIntent.scheme } returns ""
        // Mock the extension function
        mockkStatic("ch.protonmail.android.mailcommon.data.file.IntentShareExtensionsKt")
        every { any<Intent>().getShareInfo() } returns intentShareInfo

        every { shouldPresentPinInsertionScreen() } returns flowOf()
        every { newIntentObserver() } returns flowOf(mainIntent, shareIntent)

        // When + Then
        homeViewModel.state.test {
            val effect = awaitItem().navigateToEffect.consume()
            assertTrue { effect is NavigationEffect.NavigateTo }
        }
    }

    @Test
    fun `should discard draft when discard draft is called`() = runTest {
        // Given
        val messageId = MessageIdSample.LocalDraft

        coEvery { discardDraft(user.userId, messageId) } returns Unit.right()

        // When
        homeViewModel.discardDraft(messageId)

        // Then
        coVerify { discardDraft(user.userId, messageId) }
    }

    @Test
    fun `should call use case when recording mailbox screen view count`() {
        // When
        homeViewModel.recordViewOfMailboxScreen()

        // Then
        verify { recordMailboxScreenView() }
    }

    private fun mockIntent(
        action: String,
        data: Uri?,
        externalBoolean: Boolean,
        categories: Set<String> = emptySet()
    ): Intent {
        return mockk {
            every { this@mockk.action } returns action
            every { this@mockk.data } returns data
            every { this@mockk.getBooleanExtra(IntentExtraKeys.EXTRA_EXTERNAL_SHARE, false) } returns externalBoolean
            every { this@mockk.categories } returns categories
        }
    }

    @Test
    fun `confirmMessageAsSeen calls mark as seen and delete send result when user is available`() = runTest {
        // Given
        val messageId = MessageIdSample.LocalDraft
        coEvery { markMessageSendingStatusesAsSeen(userId, listOf(messageId)) } just Runs

        // When
        homeViewModel.confirmMessageAsSeen(messageId)

        // Then
        coVerify { markMessageSendingStatusesAsSeen(userId, listOf(messageId)) }
    }

    @Test
    fun `confirmMessageAsSeen fails when primary user is unavailable`() = runTest {
        // Given
        coEvery { observePrimaryUserId() } returns MutableStateFlow(null)

        // When
        homeViewModel.confirmMessageAsSeen(MessageIdSample.LocalDraft)

        // Then
        coVerify(exactly = 0) { markMessageSendingStatusesAsSeen(any(), any()) }
    }

    @Test
    fun `should call cancel schedule send message use case when undo schedule send is triggered`() = runTest {
        // Given
        val messageId = MessageIdSample.LocalDraft
        val previousScheduleTime = PreviousScheduleSendTime(Instant.DISTANT_FUTURE)

        coEvery { cancelScheduleSendMessage(userId, messageId) } returns previousScheduleTime.right()

        // When
        homeViewModel.undoScheduleSendMessage(messageId)

        // Then
        coVerify { cancelScheduleSendMessage(userId, messageId) }
    }

    @Test
    fun `navigate to draft when cancel schedule send message succeeds`() = runTest {
        // Given
        val messageId = MessageIdSample.LocalDraft
        val previousScheduleTime = PreviousScheduleSendTime(Instant.DISTANT_FUTURE)

        coEvery { cancelScheduleSendMessage(userId, messageId) } returns previousScheduleTime.right()

        // When
        homeViewModel.undoScheduleSendMessage(messageId)

        // Then
        homeViewModel.state.test {
            val expected = Effect.of(
                NavigationEffect.NavigateTo(
                    route = Destination.Screen.EditDraftComposer(messageId),
                    navOptions = NavOptions.Builder()
                        .setPopUpTo(route = Destination.Screen.Mailbox.route, inclusive = false, saveState = false)
                        .build()
                )
            )
            assertEquals(expected, awaitItem().navigateToEffect)
        }
    }

    @Test
    fun `navigate to draft when undo send message succeeds`() = runTest {
        // Given
        coEvery { undoSendMessage(userId, messageId) } returns Unit.right()

        // When
        homeViewModel.undoSendMessage(messageId)

        // Then
        homeViewModel.state.test {
            val expected = Effect.of(
                NavigationEffect.NavigateTo(
                    route = Destination.Screen.EditDraftComposer(messageId),
                    navOptions = NavOptions.Builder()
                        .setPopUpTo(route = Destination.Screen.Mailbox.route, inclusive = false, saveState = false)
                        .build()
                )
            )
            assertEquals(expected, awaitItem().navigateToEffect)
        }
    }

    @Test
    fun `show undo send error when cancel schedule send message fails`() = runTest {
        // Given
        val messageId = MessageIdSample.LocalDraft

        coEvery { cancelScheduleSendMessage(userId, messageId) } returns UndoSendError.UndoSendFailed.left()

        // When
        homeViewModel.undoScheduleSendMessage(messageId)

        // Then
        homeViewModel.state.test {
            val expected: Effect<MessageSendingStatus> = Effect.of(MessageSendingStatus.UndoSendError(messageId))
            assertEquals(expected, awaitItem().messageSendingStatusEffect)
        }
    }

    @Test
    fun `show undo send error when undo send message fails`() = runTest {
        // Given
        coEvery { undoSendMessage(userId, messageId) } returns UndoSendError.UndoSendFailed.left()

        // When
        homeViewModel.undoSendMessage(messageId)

        // Then
        homeViewModel.state.test {
            val expected: Effect<MessageSendingStatus> = Effect.of(MessageSendingStatus.UndoSendError(messageId))
            assertEquals(expected, awaitItem().messageSendingStatusEffect)
        }
    }

    @Test
    fun `shows 'cancelling' snackbar when cancel schedule send message is triggered`() = runTest {
        // Given
        val messageId = MessageIdSample.LocalDraft
        val previousScheduleTime = PreviousScheduleSendTime(Instant.DISTANT_FUTURE)

        coEvery { cancelScheduleSendMessage(userId, messageId) } returns previousScheduleTime.right()

        // When
        homeViewModel.undoScheduleSendMessage(messageId)

        // Then
        homeViewModel.state.test {
            val expected: Effect<MessageSendingStatus> = Effect.of(
                MessageSendingStatus.CancellingScheduleSend(messageId)
            )
            val actual = awaitItem()
            assertEquals(expected, actual.messageSendingStatusEffect)
            assertNotNull(actual.navigateToEffect.consume())
        }
    }

}
