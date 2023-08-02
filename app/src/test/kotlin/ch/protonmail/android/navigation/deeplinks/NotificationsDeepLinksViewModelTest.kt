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

package ch.protonmail.android.navigation.deeplinks

import java.util.UUID
import androidx.lifecycle.viewmodel.compose.viewModel
import app.cash.turbine.test
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.sample.AccountSample
import ch.protonmail.android.mailconversation.domain.repository.ConversationRepository
import ch.protonmail.android.mailconversation.domain.sample.ConversationSample
import ch.protonmail.android.mailmessage.domain.entity.MessageId
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import ch.protonmail.android.mailmessage.domain.sample.MessageSample.AlphaAppQAReport
import ch.protonmail.android.mailnotifications.domain.NotificationsDeepLinkHelper
import ch.protonmail.android.navigation.deeplinks.NotificationsDeepLinksViewModel.State.NavigateToConversation
import ch.protonmail.android.navigation.deeplinks.NotificationsDeepLinksViewModel.State.NavigateToInbox
import ch.protonmail.android.navigation.deeplinks.NotificationsDeepLinksViewModel.State.NavigateToMessageDetails
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.domain.entity.UserId
import me.proton.core.domain.type.IntEnum
import me.proton.core.mailsettings.domain.entity.MailSettings
import me.proton.core.mailsettings.domain.entity.ViewMode
import me.proton.core.mailsettings.domain.repository.MailSettingsRepository
import me.proton.core.network.domain.NetworkManager
import me.proton.core.network.domain.NetworkStatus
import org.junit.Before
import org.junit.Test
import kotlin.random.Random
import kotlin.test.assertEquals

class NotificationsDeepLinksViewModelTest {

    private val networkManager: NetworkManager = mockk {
        coEvery { networkStatus } returns NetworkStatus.Unmetered
    }
    private val accountManager: AccountManager = mockk(relaxed = true)
    private val messageRepository: MessageRepository = mockk()
    private val conversationRepository: ConversationRepository = mockk()
    private val mailSettings: MailSettings = mockk()
    private val mailSettingsRepository: MailSettingsRepository = mockk {
        coEvery { getMailSettings(any(), any()) } returns mailSettings
    }
    private val notificationsDeepLinkHelper: NotificationsDeepLinkHelper = mockk(relaxed = true)

    @Before
    fun before() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @Test
    fun `Should emit navigate to inbox and cancel the group notification`() = runTest {
        // Given
        val viewModel = buildViewModel()
        val userId = UUID.randomUUID().toString()

        // When
        val notificationId = Random.nextInt()
        viewModel.navigateToInbox(notificationId, userId)

        // Then
        viewModel.state.test {
            assertEquals(NavigateToInbox.ActiveUser, awaitItem())
            verify { notificationsDeepLinkHelper.cancelNotification(notificationId) }
        }
    }

    @Test
    fun `Should emit navigate to conversation details when conversation mode is enabled`() = runTest {
        // Given
        val notificationId = Random.nextInt()
        val messageId = UUID.randomUUID().toString()
        val userId = UUID.randomUUID().toString()
        coEvery { accountManager.getPrimaryUserId() } returns flowOf(UserId(userId))
        coEvery { mailSettings.viewMode } returns IntEnum(ViewMode.ConversationGrouping.value, null)
        coEvery { messageRepository.observeCachedMessage(UserId(userId), MessageId(messageId)) } returns flowOf(
            AlphaAppQAReport.right()
        )
        coEvery {
            conversationRepository.observeConversation(
                UserId(userId),
                AlphaAppQAReport.conversationId,
                true
            )
        } returns flowOf(
            ConversationSample.AlphaAppFeedback.right()
        )
        val viewModel = buildViewModel()

        // When
        viewModel.navigateToMessage(notificationId, messageId, userId)

        // Then
        viewModel.state.test {
            assertEquals(
                NavigateToConversation(AlphaAppQAReport.conversationId),
                awaitItem()
            )
            verify { notificationsDeepLinkHelper.cancelNotification(notificationId) }
        }
    }

    @Test
    fun `Should emit navigate to message details when conversation mode is not enabled`() = runTest {
        // Given
        val notificationId = Random.nextInt()
        val messageId = UUID.randomUUID().toString()
        val userId = UUID.randomUUID().toString()
        coEvery { accountManager.getPrimaryUserId() } returns flowOf(UserId(userId))
        coEvery { mailSettings.viewMode } returns IntEnum(ViewMode.NoConversationGrouping.value, null)
        coEvery { messageRepository.observeCachedMessage(UserId(userId), MessageId(messageId)) } returns flowOf(
            AlphaAppQAReport.right()
        )
        val viewModel = buildViewModel()

        // When
        viewModel.navigateToMessage(notificationId, messageId, userId)

        // Then
        viewModel.state.test {
            assertEquals(
                NavigateToMessageDetails(AlphaAppQAReport.messageId),
                awaitItem()
            )
            verify { notificationsDeepLinkHelper.cancelNotification(notificationId) }
        }
    }

    @Test
    fun `Should emit navigate to inbox when the user is offline and taps in a message deeplink`() = runTest {
        // Given
        val notificationId = Random.nextInt()
        val messageId = UUID.randomUUID().toString()
        val userId = UUID.randomUUID().toString()
        coEvery { networkManager.networkStatus } returns NetworkStatus.Disconnected
        val viewModel = buildViewModel()

        // When
        viewModel.navigateToMessage(notificationId, messageId, userId)

        // Then
        viewModel.state.test {
            assertEquals(NavigateToInbox.ActiveUser, awaitItem())
            verify { notificationsDeepLinkHelper.cancelNotification(notificationId) }
        }
    }

    @Test
    fun `Should navigate to the inbox if there is an error retrieving the local messages`() = runTest {
        // Given
        val notificationId = Random.nextInt()
        val messageId = UUID.randomUUID().toString()
        val userId = UUID.randomUUID().toString()
        coEvery { mailSettings.viewMode } returns IntEnum(ViewMode.NoConversationGrouping.value, null)
        coEvery { messageRepository.observeCachedMessage(UserId(userId), MessageId(messageId)) } returns flowOf(
            DataError.Local.Unknown.left()
        )
        val viewModel = buildViewModel()

        // When
        viewModel.navigateToMessage(notificationId, messageId, userId)

        // Then
        viewModel.state.test {
            assertEquals(NavigateToInbox.ActiveUser, awaitItem())
            verify { notificationsDeepLinkHelper.cancelNotification(notificationId) }
        }
    }

    @Test
    fun `Should navigate to inbox if conversation mode is enabled but the conversation can not be read`() = runTest {
        // Given
        val notificationId = Random.nextInt()
        val messageId = UUID.randomUUID().toString()
        val userId = UUID.randomUUID().toString()
        coEvery { mailSettings.viewMode } returns IntEnum(ViewMode.ConversationGrouping.value, null)
        coEvery { messageRepository.observeCachedMessage(UserId(userId), MessageId(messageId)) } returns flowOf(
            AlphaAppQAReport.right()
        )
        coEvery {
            conversationRepository.observeConversation(
                UserId(userId),
                AlphaAppQAReport.conversationId,
                true
            )
        } returns flowOf(DataError.Local.Unknown.left())
        val viewModel = buildViewModel()

        // When
        viewModel.navigateToMessage(notificationId, messageId, userId)

        // Then
        viewModel.state.test {
            assertEquals(NavigateToInbox.ActiveUser, awaitItem())
            verify { notificationsDeepLinkHelper.cancelNotification(notificationId) }
        }
    }

    @Test
    fun `Should switch account and emit switched for inbox notification to an active non primary account`() = runTest {
        // Given
        val notificationId = Random.nextInt()
        val notificationUserId = UUID.randomUUID().toString()
        val activeUserId = UUID.randomUUID().toString()
        val viewModel = buildViewModel()
        coEvery { accountManager.getPrimaryUserId() } returns flowOf(UserId(activeUserId))
        coEvery { accountManager.getAccount(UserId(notificationUserId)) } returns flowOf(AccountSample.Primary)

        // When
        viewModel.navigateToInbox(notificationId, notificationUserId)

        // Then
        viewModel.state.test {
            assertEquals(NavigateToInbox.ActiveUserSwitched(AccountSample.Primary.email!!), awaitItem())
            verify { notificationsDeepLinkHelper.cancelNotification(notificationId) }
            coVerify { accountManager.setAsPrimary(UserId(notificationUserId)) }
        }
    }

    @Test
    fun `Should switch account and emit switched for message notification to active non primary account`() = runTest {
        // Given
        val notificationId = Random.nextInt()
        val notificationTargetAccount = AccountSample.Primary
        val activeUserId = UUID.randomUUID().toString()
        val messageId = UUID.randomUUID().toString()
        val viewModel = buildViewModel()
        coEvery { accountManager.getPrimaryUserId() } returns flowOf(UserId(activeUserId))
        coEvery {
            accountManager.getAccount(notificationTargetAccount.userId)
        } returns flowOf(notificationTargetAccount)
        coEvery { mailSettings.viewMode } returns IntEnum(ViewMode.ConversationGrouping.value, null)
        coEvery {
            messageRepository.observeCachedMessage(notificationTargetAccount.userId, any())
        } returns flowOf(AlphaAppQAReport.right())
        coEvery {
            conversationRepository.observeConversation(
                notificationTargetAccount.userId,
                AlphaAppQAReport.conversationId,
                true
            )
        } returns flowOf(
            ConversationSample.AlphaAppFeedback.right()
        )

        // When
        viewModel.navigateToMessage(notificationId, messageId, notificationTargetAccount.userId.id)

        // Then
        viewModel.state.test {
            assertEquals(
                NavigateToConversation(AlphaAppQAReport.conversationId, AccountSample.Primary.email),
                awaitItem()
            )
            verify { notificationsDeepLinkHelper.cancelNotification(notificationId) }
            coVerify { accountManager.setAsPrimary(notificationTargetAccount.userId) }
        }
    }

    private fun buildViewModel() = NotificationsDeepLinksViewModel(
        networkManager = networkManager,
        accountManager = accountManager,
        messageRepository = messageRepository,
        conversationRepository = conversationRepository,
        mailSettingsRepository = mailSettingsRepository,
        notificationsDeepLinkHelper = notificationsDeepLinkHelper
    )
}
