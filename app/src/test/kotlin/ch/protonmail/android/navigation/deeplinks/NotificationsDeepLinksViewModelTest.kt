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
import ch.protonmail.android.mailcommon.domain.sample.AccountSample
import ch.protonmail.android.mailcommon.domain.sample.UserAddressSample
import ch.protonmail.android.mailcommon.domain.usecase.GetPrimaryAddress
import ch.protonmail.android.mailconversation.domain.repository.ConversationRepository
import ch.protonmail.android.mailconversation.domain.sample.ConversationSample
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import ch.protonmail.android.mailmessage.domain.sample.MessageSample.AlphaAppQAReport
import ch.protonmail.android.navigation.deeplinks.NotificationsDeepLinksViewModel.State.NavigateToConversation
import ch.protonmail.android.navigation.deeplinks.NotificationsDeepLinksViewModel.State.NavigateToInbox
import ch.protonmail.android.navigation.deeplinks.NotificationsDeepLinksViewModel.State.NavigateToMessageDetails
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
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
    private val getPrimaryAddress: GetPrimaryAddress = mockk()

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
        viewModel.navigateToInbox(userId)

        // Then
        viewModel.state.test {
            assertEquals(NavigateToInbox.ActiveUser, awaitItem())
        }
    }

    @Test
    fun `Should emit navigate to conversation details when conversation mode is enabled`() = runTest {
        // Given
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
        viewModel.navigateToMessage(messageId, userId)

        // Then
        viewModel.state.test {
            assertEquals(
                NavigateToConversation(AlphaAppQAReport.conversationId),
                awaitItem()
            )
        }
    }

    @Test
    fun `Should emit navigate to message details when conversation mode is not enabled`() = runTest {
        // Given
        val messageId = UUID.randomUUID().toString()
        val userId = UUID.randomUUID().toString()
        coEvery { accountManager.getPrimaryUserId() } returns flowOf(UserId(userId))
        coEvery { mailSettings.viewMode } returns IntEnum(ViewMode.NoConversationGrouping.value, null)
        coEvery { messageRepository.observeCachedMessage(UserId(userId), MessageId(messageId)) } returns flowOf(
            AlphaAppQAReport.right()
        )
        val viewModel = buildViewModel()

        // When
        viewModel.navigateToMessage(messageId, userId)

        // Then
        viewModel.state.test {
            assertEquals(
                NavigateToMessageDetails(AlphaAppQAReport.messageId),
                awaitItem()
            )
        }
    }

    @Test
    fun `Should emit navigate to inbox when the user is offline and taps in a message deeplink`() = runTest {
        // Given
        val messageId = UUID.randomUUID().toString()
        val userId = UUID.randomUUID().toString()
        coEvery { networkManager.networkStatus } returns NetworkStatus.Disconnected
        val viewModel = buildViewModel()

        // When
        viewModel.navigateToMessage(messageId, userId)

        // Then
        viewModel.state.test {
            assertEquals(NavigateToInbox.ActiveUser, awaitItem())
        }
    }

    @Test
    fun `Should navigate to the inbox if there is an error retrieving the local messages`() = runTest {
        // Given
        val messageId = UUID.randomUUID().toString()
        val userId = UUID.randomUUID().toString()
        coEvery { mailSettings.viewMode } returns IntEnum(ViewMode.NoConversationGrouping.value, null)
        coEvery { messageRepository.observeCachedMessage(UserId(userId), MessageId(messageId)) } returns flowOf(
            DataError.Local.Unknown.left()
        )
        val viewModel = buildViewModel()

        // When
        viewModel.navigateToMessage(messageId, userId)

        // Then
        viewModel.state.test {
            assertEquals(NavigateToInbox.ActiveUser, awaitItem())
        }
    }

    @Test
    fun `Should navigate to inbox if conversation mode is enabled but the conversation can not be read`() = runTest {
        // Given
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
        viewModel.navigateToMessage(messageId, userId)

        // Then
        viewModel.state.test {
            assertEquals(NavigateToInbox.ActiveUser, awaitItem())
        }
    }

    @Test
    fun `Should switch account and emit switched for inbox notification to an active non primary account`() = runTest {
        // Given
        val activeAccount = AccountSample.Primary.copy(email = "test@email.com")
        val notificationUserId = UserId(UUID.randomUUID().toString())
        val secondaryAccount = AccountSample.Primary.copy(userId = notificationUserId)
        val viewModel = buildViewModel()
        coEvery { accountManager.getPrimaryUserId() } returns flowOf(activeAccount.userId)
        coEvery { accountManager.getAccounts() } returns flowOf(listOf(activeAccount, secondaryAccount))
        coEvery { getPrimaryAddress.invoke(notificationUserId) } returns UserAddressSample.PrimaryAddress.right()

        // When
        viewModel.navigateToInbox(notificationUserId.id)

        // Then
        viewModel.state.test {
            assertEquals(NavigateToInbox.ActiveUserSwitched(secondaryAccount.email!!), awaitItem())
            coVerify { accountManager.setAsPrimary(secondaryAccount.userId) }
        }
    }

    @Test
    fun `Should switch account and emit switched for message notification to active non primary account`() = runTest {
        // Given
        val activeAccount = AccountSample.Primary.copy(email = "test@email.com")
        val notificationUserId = UserId(UUID.randomUUID().toString())
        val secondaryAccount = AccountSample.Primary.copy(userId = notificationUserId)
        val messageId = UUID.randomUUID().toString()
        val viewModel = buildViewModel()
        coEvery { accountManager.getPrimaryUserId() } returns flowOf(activeAccount.userId)
        coEvery { getPrimaryAddress.invoke(secondaryAccount.userId) } returns UserAddressSample.PrimaryAddress.right()
        coEvery { accountManager.getAccounts() } returns flowOf(listOf(activeAccount, secondaryAccount))
        coEvery { mailSettings.viewMode } returns IntEnum(ViewMode.ConversationGrouping.value, null)
        coEvery {
            messageRepository.observeCachedMessage(secondaryAccount.userId, any())
        } returns flowOf(AlphaAppQAReport.right())
        coEvery {
            conversationRepository.observeConversation(
                secondaryAccount.userId,
                AlphaAppQAReport.conversationId,
                true
            )
        } returns flowOf(
            ConversationSample.AlphaAppFeedback.right()
        )

        // When
        viewModel.navigateToMessage(messageId, secondaryAccount.userId.id)

        // Then
        viewModel.state.test {
            assertEquals(
                NavigateToConversation(
                    conversationId = AlphaAppQAReport.conversationId,
                    userSwitchedEmail = AccountSample.Primary.email
                ),
                awaitItem()
            )
            coVerify { accountManager.setAsPrimary(secondaryAccount.userId) }
        }
    }

    private fun buildViewModel() = NotificationsDeepLinksViewModel(
        networkManager = networkManager,
        accountManager = accountManager,
        messageRepository = messageRepository,
        conversationRepository = conversationRepository,
        mailSettingsRepository = mailSettingsRepository,
        getPrimaryAddress = getPrimaryAddress
    )
}
