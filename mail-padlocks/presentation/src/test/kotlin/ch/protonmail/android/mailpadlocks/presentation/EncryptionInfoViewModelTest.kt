/*
 * Copyright (c) 2026 Proton Technologies AG
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

package ch.protonmail.android.mailpadlocks.presentation

import app.cash.turbine.test
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailpadlocks.domain.PrivacyLock
import ch.protonmail.android.mailpadlocks.domain.PrivacyLockColor
import ch.protonmail.android.mailpadlocks.domain.PrivacyLockIcon
import ch.protonmail.android.mailpadlocks.domain.PrivacyLockTooltip
import ch.protonmail.android.mailpadlocks.domain.usecase.GetPrivacyLockForMessage
import ch.protonmail.android.mailpadlocks.presentation.model.EncryptionInfoState
import ch.protonmail.android.mailpadlocks.presentation.model.EncryptionInfoUiModel
import ch.protonmail.android.mailsession.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.test.utils.rule.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertTrue

internal class EncryptionInfoViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val testUserId = UserId("test-user-id")
    private val testMessageId = MessageId("123")

    private val mockGetPrivacyLockForMessage = mockk<GetPrivacyLockForMessage>()
    private val mockObservePrimaryUserId = mockk<ObservePrimaryUserId>()

    @Test
    fun `state emits Enabled with WithLock when use case returns PrivacyLock Value`() = runTest {
        // Given
        val privacyLock = PrivacyLock.Value(
            icon = PrivacyLockIcon.ClosedLockWithTick,
            color = PrivacyLockColor.Green,
            tooltip = PrivacyLockTooltip.ReceiveE2eVerifiedRecipient
        )
        every { mockObservePrimaryUserId() } returns flowOf(testUserId)
        coEvery { mockGetPrivacyLockForMessage(testUserId, testMessageId) } returns privacyLock.right()

        val viewModel = EncryptionInfoViewModel(
            observePrimaryUserId = mockObservePrimaryUserId,
            getPrivacyLockForMessage = mockGetPrivacyLockForMessage,
            messageId = testMessageId
        )

        // When/Then
        viewModel.state.test {
            val state = awaitItem()
            assertTrue(state is EncryptionInfoState.Enabled)
            assertTrue(state.uiModel is EncryptionInfoUiModel.WithLock)
        }
    }

    @Test
    fun `state emits Enabled with NoLock when use case returns PrivacyLock None`() = runTest {
        // Given
        every { mockObservePrimaryUserId() } returns flowOf(testUserId)
        coEvery { mockGetPrivacyLockForMessage(testUserId, testMessageId) } returns PrivacyLock.None.right()

        val viewModel = EncryptionInfoViewModel(
            observePrimaryUserId = mockObservePrimaryUserId,
            getPrivacyLockForMessage = mockGetPrivacyLockForMessage,
            messageId = testMessageId
        )

        // When/Then
        viewModel.state.test {
            val state = awaitItem()
            assertTrue(state is EncryptionInfoState.Enabled)
            assertTrue(state.uiModel is EncryptionInfoUiModel.NoLock)
        }
    }

    @Test
    fun `state emits Disabled when use case returns error`() = runTest {
        // Given
        every { mockObservePrimaryUserId() } returns flowOf(testUserId)
        coEvery { mockGetPrivacyLockForMessage(testUserId, testMessageId) } returns DataError.Remote.NoNetwork.left()

        val viewModel = EncryptionInfoViewModel(
            observePrimaryUserId = mockObservePrimaryUserId,
            getPrivacyLockForMessage = mockGetPrivacyLockForMessage,
            messageId = testMessageId
        )

        // When/Then
        viewModel.state.test {
            val state = awaitItem()
            assertTrue(state is EncryptionInfoState.Disabled)
        }
    }
}
