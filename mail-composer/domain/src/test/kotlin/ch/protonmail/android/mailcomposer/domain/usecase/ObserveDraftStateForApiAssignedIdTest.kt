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

package ch.protonmail.android.mailcomposer.domain.usecase

import app.cash.turbine.test
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailcomposer.domain.model.DraftState
import ch.protonmail.android.mailcomposer.domain.model.DraftSyncState
import ch.protonmail.android.mailcomposer.domain.repository.DraftStateRepository
import ch.protonmail.android.mailcomposer.domain.sample.DraftStateSample
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import kotlin.test.Test
import kotlin.test.assertEquals

class ObserveDraftStateForApiAssignedIdTest {

    private val draftStateRepository = mockk<DraftStateRepository>()

    private val observeDraftStateForApiAssignedId = ObserveDraftStateForApiAssignedId(draftStateRepository)

    @Test
    fun `emit api assigned id when not null on draft state`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val messageId = MessageIdSample.LocalDraft
        val expectedApiAssignedId = MessageIdSample.RemoteDraft
        val expected = DraftStateSample.RemoteDraftState
        expectDraftState(userId, messageId, expected)

        // When
        observeDraftStateForApiAssignedId(userId, messageId).test {
            // Then
            assertEquals(expectedApiAssignedId, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `ignore changes of draft state that are not the api assigned id`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val messageId = MessageIdSample.LocalDraft
        val expectedApiAssignedId = MessageIdSample.RemoteDraft
        val expectedDraftStates = flowOf(
            DraftStateSample.RemoteDraftState.right(), // draft receiving remote id
            DraftStateSample.RemoteDraftState.copy(state = DraftSyncState.Local).right() // draft being updated again
        )
        expectDraftStateEmits(userId, messageId, expectedDraftStates)

        // When
        observeDraftStateForApiAssignedId(userId, messageId).test {
            // Then
            assertEquals(expectedApiAssignedId, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `emit nothing when api assigned id is null on draft state`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val messageId = MessageIdSample.LocalDraft
        val expected = DraftStateSample.NewDraftState
        expectDraftState(userId, messageId, expected)

        // When
        observeDraftStateForApiAssignedId(userId, messageId).test {
            // Then
            awaitComplete()
        }
    }

    @Test
    fun `emit nothing when there is no draft state for the given messageId`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val messageId = MessageIdSample.LocalDraft
        expectDraftStateNotExisting(userId, messageId)

        // When
        observeDraftStateForApiAssignedId(userId, messageId).test {
            // Then
            awaitComplete()
        }
    }

    private fun expectDraftStateNotExisting(userId: UserId, messageId: MessageId) {
        coEvery { draftStateRepository.observe(userId, messageId) } returns flowOf(DataError.Local.NoDataCached.left())
    }
    private fun expectDraftState(
        userId: UserId,
        messageId: MessageId,
        expected: DraftState
    ) {
        expectDraftStateEmits(userId, messageId, flowOf(expected.right()))
    }

    private fun expectDraftStateEmits(
        userId: UserId,
        messageId: MessageId,
        states: Flow<Either<DataError, DraftState>>
    ) {
        coEvery { draftStateRepository.observe(userId, messageId) } returns states
    }
}
