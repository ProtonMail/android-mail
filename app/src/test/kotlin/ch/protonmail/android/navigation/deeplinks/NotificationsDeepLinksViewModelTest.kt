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
import app.cash.turbine.test
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
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
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
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

        // When
        val notificationId = Random.nextInt()
        viewModel.navigateToInbox(notificationId)

        // Then
        viewModel.state.test {
            assertEquals(NavigateToInbox, awaitItem())
            verify { notificationsDeepLinkHelper.cancelNotification(notificationId) }
        }
    }

    @Test
    fun `Should emit navigate to conversation details when conversation model is enabled`() = runTest {
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
        } returns flowOf(
            ConversationSample.AlphaAppFeedback.right()
        )
        val viewModel = buildViewModel()

        // When
        viewModel.navigateToMessage(notificationId, messageId, userId)

        // Then
        viewModel.state.test {
            assertEquals(
                NavigateToConversation(
                    AlphaAppQAReport.conversationId,
                    false
                ),
                awaitItem()
            )
            verify { notificationsDeepLinkHelper.cancelNotification(notificationId) }
        }
    }

    @Test
    fun `Should emit navigate to message details when conversation model is not enabled`() = runTest {
        // Given
        val notificationId = Random.nextInt()
        val messageId = UUID.randomUUID().toString()
        val userId = UUID.randomUUID().toString()
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
                NavigateToMessageDetails(
                    AlphaAppQAReport.messageId,
                    false
                ),
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
            assertEquals(NavigateToInbox, awaitItem())
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
            assertEquals(NavigateToInbox, awaitItem())
            verify { notificationsDeepLinkHelper.cancelNotification(notificationId) }
        }
    }

    private fun buildViewModel() = NotificationsDeepLinksViewModel(
        networkManager = networkManager,
        messageRepository = messageRepository,
        conversationRepository = conversationRepository,
        mailSettingsRepository = mailSettingsRepository,
        notificationsDeepLinkHelper = notificationsDeepLinkHelper
    )
}
