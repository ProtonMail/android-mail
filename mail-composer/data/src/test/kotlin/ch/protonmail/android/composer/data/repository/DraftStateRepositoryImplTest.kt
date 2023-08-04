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

package ch.protonmail.android.composer.data.repository

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.composer.data.local.DraftStateLocalDataSourceImpl
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailcomposer.domain.model.DraftAction
import ch.protonmail.android.mailcomposer.domain.model.DraftState
import ch.protonmail.android.mailcomposer.domain.model.DraftSyncState
import ch.protonmail.android.mailcomposer.domain.sample.DraftStateSample
import ch.protonmail.android.mailmessage.domain.entity.MessageId
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import kotlin.test.Test
import kotlin.test.assertEquals

class DraftStateRepositoryImplTest {

    private val userId = UserIdSample.Primary

    private val draftStateLocalDataSource = mockk<DraftStateLocalDataSourceImpl>()

    private val repository = DraftStateRepositoryImpl(draftStateLocalDataSource)

    @Test
    fun `observe draft state returns it when existing`() = runTest {
        // Given
        val draftId = MessageIdSample.EmptyDraft
        val expected = DraftStateSample.NewDraftState
        expectDraftStateLocalDataSourceSuccess(userId, draftId, expected)

        // When
        val actual = repository.observe(userId, draftId).first()

        // Then
        assertEquals(expected.right(), actual)
    }

    @Test
    fun `observe draft state returns no data cached error when not existing`() = runTest {
        // Given
        val draftId = MessageIdSample.EmptyDraft
        val expectedError = DataError.Local.NoDataCached
        expectDraftStateLocalDataSourceFailure(userId, draftId, expectedError)

        // When
        val actual = repository.observe(userId, draftId).first()

        // Then
        assertEquals(expectedError.left(), actual)
    }

    @Test
    fun `store synced state does update api message id and draft sync state`() = runTest {
        // Given
        val draftId = MessageIdSample.EmptyDraft
        val remoteDraftId = MessageIdSample.RemoteDraft
        val existingState = DraftStateSample.NewDraftState
        val expectedDraftState = existingState.copy(
            apiMessageId = remoteDraftId,
            state = DraftSyncState.Synchronized
        )
        expectDraftStateLocalDataSourceSuccess(userId, draftId, existingState)
        expectLocalDataSourceUpsertSuccess(expectedDraftState)

        // When
        val actual = repository.saveSyncedState(userId, draftId, remoteDraftId)

        // Then
        assertEquals(Unit.right(), actual)
    }

    @Test
    fun `save local state stores new draft state when no state exists`() = runTest {
        // Given
        val draftId = MessageIdSample.EmptyDraft
        val expectedDraftState = DraftStateSample.NewDraftState.copy(
            messageId = draftId,
            state = DraftSyncState.Local
        )
        val expectedAction = DraftAction.Compose
        val expectedError = DataError.Local.NoDataCached
        expectDraftStateLocalDataSourceFailure(userId, draftId, expectedError)
        expectLocalDataSourceUpsertSuccess(expectedDraftState)

        // When
        val actual = repository.saveLocalState(userId, draftId, expectedAction)

        // Then
        coVerify { draftStateLocalDataSource.save(expectedDraftState) }
        assertEquals(Unit.right(), actual)
    }

    @Test
    fun `save local state updates draft state in data source when state exists`() = runTest {
        // Given
        val draftId = MessageIdSample.RemoteDraft
        val existingState = DraftStateSample.RemoteDraftState
        val expectedAction = DraftAction.Compose
        val expectedDraftState = existingState.copy(state = DraftSyncState.Local)
        expectDraftStateLocalDataSourceSuccess(userId, draftId, existingState)
        expectLocalDataSourceUpsertSuccess(expectedDraftState)

        // When
        val actual = repository.saveLocalState(userId, draftId, expectedAction)

        // Then
        coVerify { draftStateLocalDataSource.save(expectedDraftState) }
        assertEquals(Unit.right(), actual)
    }

    private fun expectDraftStateLocalDataSourceSuccess(
        userId: UserId,
        draftId: MessageId,
        expected: DraftState
    ) {
        coEvery { draftStateLocalDataSource.observe(userId, draftId) } returns flowOf(expected.right())
    }

    private fun expectLocalDataSourceUpsertSuccess(state: DraftState) {
        coEvery { draftStateLocalDataSource.save(state) } returns Unit.right()
    }

    private fun expectDraftStateLocalDataSourceFailure(
        userId: UserId,
        draftId: MessageId,
        error: DataError
    ) {
        coEvery { draftStateLocalDataSource.observe(userId, draftId) } returns flowOf(error.left())
    }

}
