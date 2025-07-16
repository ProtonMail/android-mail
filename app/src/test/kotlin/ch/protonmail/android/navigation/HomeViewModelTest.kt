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
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import app.cash.turbine.test
import ch.protonmail.android.mailcommon.data.file.getShareInfo
import ch.protonmail.android.mailcommon.domain.model.IntentShareInfo
import ch.protonmail.android.mailcommon.domain.sample.UserSample
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUser
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcomposer.domain.model.MessageSendingStatus
import ch.protonmail.android.mailcomposer.domain.usecase.DiscardDraft
import ch.protonmail.android.mailcomposer.domain.usecase.ObserveSendingMessagesStatus
import ch.protonmail.android.mailcomposer.domain.usecase.ResetSendingMessagesStatus
import ch.protonmail.android.maillabel.domain.SelectedMailLabelId
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.maillabel.domain.usecase.GetDraftLabelId
import ch.protonmail.android.mailmailbox.domain.usecase.RecordMailboxScreenView
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.mailnotifications.domain.model.telemetry.NotificationPermissionTelemetryEventType
import ch.protonmail.android.mailnotifications.domain.usecase.SavePermissionDialogTimestamp
import ch.protonmail.android.mailnotifications.domain.usecase.SaveShouldStopShowingPermissionDialog
import ch.protonmail.android.mailnotifications.domain.usecase.ShouldShowNotificationPermissionDialog
import ch.protonmail.android.mailnotifications.domain.usecase.TrackNotificationPermissionTelemetryEvent
import ch.protonmail.android.mailnotifications.presentation.model.NotificationPermissionDialogState
import ch.protonmail.android.mailnotifications.presentation.model.NotificationPermissionDialogType
import ch.protonmail.android.mailsettings.domain.usecase.autolock.ShouldPresentPinInsertionScreen
import ch.protonmail.android.navigation.model.Destination
import ch.protonmail.android.navigation.model.HomeState
import ch.protonmail.android.navigation.share.ShareIntentObserver
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import me.proton.core.network.domain.NetworkManager
import me.proton.core.network.domain.NetworkStatus
import me.proton.core.user.domain.entity.User
import org.junit.Assert.assertNull
import javax.inject.Provider
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class HomeViewModelTest {

    private val user = UserSample.Primary

    private val networkManager = mockk<NetworkManager>()

    private val observePrimaryUserMock = mockk<ObservePrimaryUser> {
        every { this@mockk() } returns MutableStateFlow<User?>(user)
    }

    private val observeSendingMessagesStatus = mockk<ObserveSendingMessagesStatus> {
        every { this@mockk.invoke(any()) } returns flowOf(MessageSendingStatus.None)
    }

    private val recordMailboxScreenView = mockk<RecordMailboxScreenView>(relaxUnitFun = true)

    private val resetSendingMessageStatus = mockk<ResetSendingMessagesStatus>(relaxUnitFun = true)

    private val selectedMailLabelId = mockk<SelectedMailLabelId>(relaxUnitFun = true)

    private val shouldPresentPinInsertionScreen = mockk<ShouldPresentPinInsertionScreen> {
        every { this@mockk.invoke() } returns flowOf(false)
    }

    private val shareIntentObserver = mockk<ShareIntentObserver>(relaxUnitFun = true) {
        every { this@mockk() } returns emptyFlow()
    }

    private val getDraftLabelId = mockk<GetDraftLabelId>()

    private val discardDraft = mockk<DiscardDraft>(relaxUnitFun = true)

    private val shouldShowNotificationPermissionDialog = mockk<ShouldShowNotificationPermissionDialog> {
        coEvery { this@mockk(currentTimeMillis = any(), isMessageSent = false) } returns false
    }
    private val savePermissionDialogTimestamp = mockk<SavePermissionDialogTimestamp>(relaxUnitFun = true)
    private val saveShouldStopShowingPermissionDialog = mockk<SaveShouldStopShowingPermissionDialog>(
        relaxUnitFun = true
    )
    private val trackNotificationPermissionTelemetry = mockk<TrackNotificationPermissionTelemetryEvent>(
        relaxUnitFun = true
    )

    private val isComposerV2Enabled = mockk<Provider<Boolean>> {
        every { this@mockk.get() } returns false
    }

    private val homeViewModel by lazy {
        HomeViewModel(
            networkManager,
            observeSendingMessagesStatus,
            recordMailboxScreenView,
            resetSendingMessageStatus,
            selectedMailLabelId,
            discardDraft,
            shouldShowNotificationPermissionDialog,
            savePermissionDialogTimestamp,
            saveShouldStopShowingPermissionDialog,
            trackNotificationPermissionTelemetry,
            isComposerV2Enabled.get(),
            getDraftLabelId,
            observePrimaryUserMock,
            shareIntentObserver
        )
    }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        mockkStatic(Uri::class)
    }

    @AfterTest
    fun teardown() {
        unmockkAll()
        unmockkStatic(Uri::class)
    }

    @Test
    fun `when initialized then emit initial state`() = runTest {
        // Given
        every { networkManager.observe() } returns emptyFlow()

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
            data = null
        )
        every { networkManager.observe() } returns emptyFlow()
        every { shareIntentObserver() } returns flowOf(mainIntent)

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
    fun `when the status is disconnected and is still disconnected after 5 seconds then emit disconnected status`() =
        runTest {
            // Given
            every { networkManager.observe() } returns flowOf(NetworkStatus.Disconnected)
            every { networkManager.networkStatus } returns NetworkStatus.Disconnected

            // When
            homeViewModel.state.test {
                awaitItem()
                advanceUntilIdle()
                val actualItem = awaitItem()
                val expectedItem = HomeState(
                    notificationPermissionDialogState = NotificationPermissionDialogState.Hidden,
                    networkStatusEffect = Effect.of(NetworkStatus.Disconnected),
                    messageSendingStatusEffect = Effect.empty(),
                    navigateToEffect = Effect.empty(),
                    startedFromLauncher = false
                )

                // Then
                assertEquals(expectedItem, actualItem)
            }
        }

    @Test
    fun `when the status is disconnected and is metered after 5 seconds then emit metered status`() = runTest {
        // Given
        every { networkManager.observe() } returns flowOf(NetworkStatus.Disconnected)
        every { networkManager.networkStatus } returns NetworkStatus.Metered

        // When
        homeViewModel.state.test {
            awaitItem()
            advanceUntilIdle()
            val actualItem = awaitItem()
            val expectedItem = HomeState(
                notificationPermissionDialogState = NotificationPermissionDialogState.Hidden,
                networkStatusEffect = Effect.of(NetworkStatus.Metered),
                messageSendingStatusEffect = Effect.empty(),
                navigateToEffect = Effect.empty(),
                startedFromLauncher = false
            )

            // Then
            assertEquals(expectedItem, actualItem)
        }
    }

    @Test
    fun `when the status is metered then emit metered status`() = runTest {
        // Given
        every { networkManager.observe() } returns flowOf(NetworkStatus.Metered)

        // When
        homeViewModel.state.test {
            val actualItem = awaitItem()
            val expectedItem = HomeState(
                notificationPermissionDialogState = NotificationPermissionDialogState.Hidden,
                networkStatusEffect = Effect.of(NetworkStatus.Metered),
                messageSendingStatusEffect = Effect.empty(),
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
        every { networkManager.observe() } returns flowOf(NetworkStatus.Metered)
        val sendingMessageStatusFlow = MutableStateFlow<MessageSendingStatus>(MessageSendingStatus.MessageSent)
        every { observeSendingMessagesStatus(user.userId) } returns sendingMessageStatusFlow
        coEvery { shouldShowNotificationPermissionDialog(any(), isMessageSent = true) } returns false

        // When
        homeViewModel.state.test {
            val actualItem = awaitItem()
            val expectedItem = HomeState(
                notificationPermissionDialogState = NotificationPermissionDialogState.Hidden,
                networkStatusEffect = Effect.of(NetworkStatus.Metered),
                messageSendingStatusEffect = Effect.of(MessageSendingStatus.MessageSent),
                navigateToEffect = Effect.empty(),
                startedFromLauncher = false
            )
            sendingMessageStatusFlow.emit(MessageSendingStatus.None)

            // Then
            assertEquals(expectedItem, actualItem)
        }
    }

    @Test
    fun `when observe sending message status emits error then emit effect and reset sending messages status`() =
        runTest {
            // Given
            every { networkManager.observe() } returns flowOf(NetworkStatus.Metered)
            every { observeSendingMessagesStatus(user.userId) } returns flowOf(
                MessageSendingStatus.SendMessageError(null)
            )

            // When
            homeViewModel.state.test {
                val actualItem = awaitItem()
                val expectedItem = HomeState(
                    notificationPermissionDialogState = NotificationPermissionDialogState.Hidden,
                    networkStatusEffect = Effect.of(NetworkStatus.Metered),
                    messageSendingStatusEffect = Effect.of(MessageSendingStatus.SendMessageError(null)),
                    navigateToEffect = Effect.empty(),
                    startedFromLauncher = false
                )

                // Then
                assertEquals(expectedItem, actualItem)
                coVerify { resetSendingMessageStatus(user.userId) }
            }
        }

    @Test
    fun `when pin lock screen needs to be shown, the effect is emitted accordingly`() = runTest {
        // Given
        every { networkManager.observe() } returns flowOf(NetworkStatus.Unmetered)
        every { shouldPresentPinInsertionScreen() } returns flowOf(true)

        // When + Then
        homeViewModel.state.test {
            val actualItem = awaitItem()
            val expectedItem = HomeState.Initial.copy(
                networkStatusEffect = Effect.of(NetworkStatus.Unmetered)
            )
            assertEquals(expectedItem, actualItem)
        }
    }

    @Test
    fun `should emit a new state with navigation effect when a share intent is received`() = runTest {
        // Given
        val fileUriStr = "content://media/1234"
        val fileUri = mockk<Uri>()
        val intentShareInfo = IntentShareInfo.Empty.copy(
            attachmentUris = listOf(fileUriStr)
        )
        val shareIntent = mockIntent(
            action = Intent.ACTION_SEND,
            data = fileUri
        )
        // Mock the extension function
        mockkStatic("ch.protonmail.android.mailcommon.data.file.IntentShareExtensionsKt")
        every { any<Intent>().getShareInfo() } returns intentShareInfo

        every { networkManager.observe() } returns flowOf()
        every { shouldPresentPinInsertionScreen() } returns flowOf()
        every { shareIntentObserver() } returns flowOf(shareIntent)

        // When + Then
        homeViewModel.state.test {
            val actualItem = awaitItem()
            assertNotNull(actualItem.navigateToEffect.consume())
        }
    }

    @Test
    fun `should not emit a new navigation state when file share info is empty`() = runTest {
        // Given
        val fileUri = mockk<Uri>()
        val shareIntent = mockIntent(
            action = Intent.ACTION_VIEW,
            data = fileUri
        )
        // Mock the extension function
        mockkStatic("ch.protonmail.android.mailcommon.data.file.IntentShareExtensionsKt")
        every { any<Intent>().getShareInfo() } returns IntentShareInfo.Empty

        every { networkManager.observe() } returns flowOf()
        every { shouldPresentPinInsertionScreen() } returns flowOf()
        every { shareIntentObserver() } returns flowOf(shareIntent)

        // When + Then
        homeViewModel.state.test {
            val actualItem = awaitItem()
            assertNull(actualItem.navigateToEffect.consume())
        }
    }

    @Test
    fun `should not emit a new navigation state when activity was started from launcher`() = runTest {
        // Given
        val fileUriStr = "content://media/1234"
        val fileUri = mockk<Uri>()
        val intentShareInfo = IntentShareInfo.Empty.copy(
            attachmentUris = listOf(fileUriStr)
        )
        val shareIntent = mockIntent(
            action = Intent.ACTION_SEND,
            data = fileUri
        )
        val mainIntent = mockIntent(
            action = Intent.ACTION_MAIN,
            data = null
        )
        // Mock the extension function
        mockkStatic("ch.protonmail.android.mailcommon.data.file.IntentShareExtensionsKt")
        every { any<Intent>().getShareInfo() } returns intentShareInfo

        every { networkManager.observe() } returns flowOf()
        every { shouldPresentPinInsertionScreen() } returns flowOf()
        every { shareIntentObserver() } returns flowOf(mainIntent, shareIntent)

        // When + Then
        homeViewModel.state.test {
            val actualItem = awaitItem()
            assertNull(actualItem.navigateToEffect.consume())
        }
    }

    @Test
    fun `should discard draft when discard draft is called`() = runTest {
        // Given
        val messageId = MessageIdSample.LocalDraft

        every { networkManager.observe() } returns flowOf()

        // When
        homeViewModel.discardDraft(messageId)

        // Then
        coVerify { discardDraft(user.userId, messageId) }
    }

    @Test
    fun `should call use case when recording mailbox screen view count`() {
        // Given
        every { networkManager.observe() } returns flowOf()

        // When
        homeViewModel.recordViewOfMailboxScreen()

        // Then
        verify { recordMailboxScreenView() }
    }

    @Test
    fun `should show notification permission dialog when initializing if use case return true`() = runTest {
        // Given
        every { networkManager.observe() } returns flowOf()
        coEvery { shouldShowNotificationPermissionDialog(any(), isMessageSent = false) } returns true

        // When
        homeViewModel.state.test {
            val item = awaitItem()

            // Then
            val expected = NotificationPermissionDialogState.Shown(
                type = NotificationPermissionDialogType.PostOnboarding
            )
            assertEquals(expected, item.notificationPermissionDialogState)
            coVerify { savePermissionDialogTimestamp(any()) }
            verify {
                trackNotificationPermissionTelemetry(
                    NotificationPermissionTelemetryEventType.NotificationPermissionDialogDisplayed(
                        NotificationPermissionDialogType.PostOnboarding
                    )
                )
            }
        }
    }

    @Test
    fun `should not show notification permission dialog when initializing if use case return false`() = runTest {
        // Given
        every { networkManager.observe() } returns flowOf()
        coEvery { shouldShowNotificationPermissionDialog(any(), isMessageSent = false) } returns false

        // When
        homeViewModel.state.test {
            val item = awaitItem()

            // Then
            val expected = NotificationPermissionDialogState.Hidden
            assertEquals(expected, item.notificationPermissionDialogState)
            verify(exactly = 0) { trackNotificationPermissionTelemetry(any()) }
        }
    }

    @Test
    fun `should show notification permission dialog when message was sent if use case returns true`() = runTest {
        // Given
        every { networkManager.observe() } returns flowOf()
        coEvery { shouldShowNotificationPermissionDialog(any(), isMessageSent = false) } returns false
        coEvery { shouldShowNotificationPermissionDialog(any(), isMessageSent = true) } returns true
        every { observeSendingMessagesStatus(user.userId) } returns flowOf(MessageSendingStatus.MessageSent)

        // When
        homeViewModel.state.test {
            val item = awaitItem()

            // Then
            val expected = NotificationPermissionDialogState.Shown(
                type = NotificationPermissionDialogType.PostSending
            )
            assertEquals(expected, item.notificationPermissionDialogState)
            coVerify { saveShouldStopShowingPermissionDialog() }
            verify {
                trackNotificationPermissionTelemetry(
                    NotificationPermissionTelemetryEventType.NotificationPermissionDialogDisplayed(
                        NotificationPermissionDialogType.PostSending
                    )
                )
            }
        }
    }

    @Test
    fun `should hide notification permission dialog when close method is called`() = runTest {
        // Given
        every { networkManager.observe() } returns flowOf()
        coEvery { shouldShowNotificationPermissionDialog(any(), isMessageSent = false) } returns true

        // When
        homeViewModel.state.test {
            skipItems(1)
            homeViewModel.closeNotificationPermissionDialog()
            val item = awaitItem()

            // Then
            val expected = NotificationPermissionDialogState.Hidden
            assertEquals(expected, item.notificationPermissionDialogState)
        }
    }

    @Test
    fun `should track telemetry event when the method is called`() = runTest {
        // Given
        every { networkManager.observe() } returns flowOf()
        coEvery { shouldShowNotificationPermissionDialog(any(), isMessageSent = false) } returns true

        // When
        homeViewModel.trackTelemetryEvent(
            NotificationPermissionTelemetryEventType.NotificationPermissionDialogDisplayed(
                NotificationPermissionDialogType.PostOnboarding
            )
        )

        // Then
        verify {
            trackNotificationPermissionTelemetry(
                NotificationPermissionTelemetryEventType.NotificationPermissionDialogDisplayed(
                    NotificationPermissionDialogType.PostOnboarding
                )
            )
        }
    }

    @Test
    fun `should set the draft label as returned by usecase`() = runTest {
        // Given
        val destination = mockk<NavDestination> {
            every { this@mockk.route } returns Destination.Screen.Mailbox.route
        }
        val navc = mockk<NavController> {
            every { this@mockk.currentDestination } returns destination
        }
        coEvery { getDraftLabelId.invoke(user.userId) } returns MailLabelId.System.AllDrafts

        every { networkManager.observe() } returns flowOf()

        // When
        homeViewModel.navigateToDrafts(navc)

        // Then
        verify {
            selectedMailLabelId.set(MailLabelId.System.AllDrafts)
        }
    }

    private fun mockIntent(action: String, data: Uri?): Intent {
        return mockk {
            every { this@mockk.action } returns action
            every { this@mockk.data } returns data
        }
    }
}
