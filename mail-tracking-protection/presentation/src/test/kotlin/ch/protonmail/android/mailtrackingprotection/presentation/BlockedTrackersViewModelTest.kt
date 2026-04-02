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

package ch.protonmail.android.mailtrackingprotection.presentation

import app.cash.turbine.test
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailsession.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailtrackingprotection.domain.model.BlockedPrivacyItems
import ch.protonmail.android.mailtrackingprotection.domain.model.BlockedTracker
import ch.protonmail.android.mailtrackingprotection.domain.model.CleanedLink
import ch.protonmail.android.mailtrackingprotection.domain.model.PrivacyItemsResult
import ch.protonmail.android.mailtrackingprotection.domain.repository.PrivacyInfoRepository
import ch.protonmail.android.mailtrackingprotection.presentation.model.BlockedElementsState
import ch.protonmail.android.test.utils.rule.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class BlockedTrackersViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val testUserId = UserId("test-user-id")
    private val testMessageId = MessageId("message-123")

    private val mockRepository = mockk<PrivacyInfoRepository>()
    private val mockObservePrimaryUserId = mockk<ObservePrimaryUserId>()

    @Test
    fun `state emits NoTrackersBlocked when repository returns empty list`() = runTest {
        // Given
        every { mockObservePrimaryUserId() } returns flowOf(testUserId)
        every {
            mockRepository.observePrivacyItemsForMessage(testUserId, testMessageId)
        } returns flowOf(PrivacyItemsResult.Detected(BlockedPrivacyItems(emptyList(), emptyList())).right())

        val viewModel = BlockedTrackersViewModel(
            privacyInfoRepository = mockRepository,
            observePrimaryUserId = mockObservePrimaryUserId,
            messageId = testMessageId
        )

        // When/Then
        viewModel.state.test {
            val state = awaitItem()
            assertTrue(state is BlockedElementsState.NoBlockedElements)
        }
    }

    @Test
    fun `state emits BlockedElements when repository returns items`() = runTest {
        // Given
        val items = BlockedPrivacyItems(
            trackers = listOf(
                BlockedTracker("tracker1.com", listOf("https://tracker1.com/pixel")),
                BlockedTracker("tracker2.com", listOf("https://tracker2.com/pixel"))
            ),
            urls = listOf(
                CleanedLink("original.com/utm=123", "original.com")
            )
        )
        every { mockObservePrimaryUserId() } returns flowOf(testUserId)
        every {
            mockRepository.observePrivacyItemsForMessage(testUserId, testMessageId)
        } returns flowOf(PrivacyItemsResult.Detected(items).right())

        val viewModel = BlockedTrackersViewModel(
            privacyInfoRepository = mockRepository,
            observePrimaryUserId = mockObservePrimaryUserId,
            messageId = testMessageId
        )

        // When/Then
        viewModel.state.test {
            val state = awaitItem()
            assertTrue(state is BlockedElementsState.BlockedElements)
            assertEquals(2, state.uiModel.trackers.items.size)
            assertEquals(1, state.uiModel.links.items.size)
        }
    }

    @Test
    fun `state emits Disabled when repository returns error`() = runTest {
        // Given
        every { mockObservePrimaryUserId() } returns flowOf(testUserId)
        every {
            mockRepository.observePrivacyItemsForMessage(testUserId, testMessageId)
        } returns flowOf(DataError.Remote.NoNetwork.left())

        val viewModel = BlockedTrackersViewModel(
            privacyInfoRepository = mockRepository,
            observePrimaryUserId = mockObservePrimaryUserId,
            messageId = testMessageId
        )

        // When/Then
        viewModel.state.test {
            val state = awaitItem()
            assertTrue(state is BlockedElementsState.Disabled)
        }
    }

    @Test
    fun `state emits Loading when repository returns Pending`() = runTest {
        // Given
        every { mockObservePrimaryUserId() } returns flowOf(testUserId)
        every {
            mockRepository.observePrivacyItemsForMessage(testUserId, testMessageId)
        } returns flowOf(PrivacyItemsResult.Pending.right())

        val viewModel = BlockedTrackersViewModel(
            privacyInfoRepository = mockRepository,
            observePrimaryUserId = mockObservePrimaryUserId,
            messageId = testMessageId
        )

        // When/Then
        viewModel.state.test {
            val state = awaitItem()
            assertTrue(state is BlockedElementsState.Loading)
        }
    }

    @Test
    fun `state emits Disabled when repository returns Disabled`() = runTest {
        // Given
        every { mockObservePrimaryUserId() } returns flowOf(testUserId)
        every {
            mockRepository.observePrivacyItemsForMessage(testUserId, testMessageId)
        } returns flowOf(PrivacyItemsResult.Disabled.right())

        val viewModel = BlockedTrackersViewModel(
            privacyInfoRepository = mockRepository,
            observePrimaryUserId = mockObservePrimaryUserId,
            messageId = testMessageId
        )

        // When/Then
        viewModel.state.test {
            val state = awaitItem()
            assertTrue(state is BlockedElementsState.Disabled)
        }
    }
}
