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

package ch.protonmail.android.composer.data.usecase

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.composer.data.remote.DraftRemoteDataSource
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.model.ProtonError
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailcomposer.domain.model.DraftAction
import ch.protonmail.android.mailcomposer.domain.model.DraftState
import ch.protonmail.android.mailcomposer.domain.repository.DraftStateRepository
import ch.protonmail.android.mailcomposer.domain.sample.DraftStateSample
import ch.protonmail.android.mailmessage.domain.entity.MessageId
import ch.protonmail.android.mailmessage.domain.entity.MessageWithBody
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.mailmessage.domain.sample.MessageWithBodySample
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import org.junit.Assert.assertEquals
import org.junit.Test

class SyncDraftTest {

    private val userId = UserIdSample.Primary

    private val messageRepository = mockk<MessageRepository>()
    private val draftRemoteDataSource = mockk<DraftRemoteDataSource>()
    private val draftStateRepository = mockk<DraftStateRepository>()

    private val draftRepository = SyncDraft(
        messageRepository,
        draftStateRepository,
        draftRemoteDataSource
    )

    @Test
    fun `returns success when remote data source succeeds`() = runTest {
        // Given
        val messageId = MessageIdSample.LocalDraft
        val expectedDraft = MessageWithBodySample.Invoice
        val expectedAction = DraftAction.Compose
        val expectedResponse = MessageWithBodySample.EmptyDraft
        val expectedDraftState = DraftStateSample.NewDraftState
        expectGetDraftStateSucceeds(userId, messageId, expectedDraftState)
        expectGetLocalMessageSucceeds(userId, messageId, expectedDraft)
        expectRemoteDataSourceCreateSuccess(userId, expectedDraft, expectedAction, expectedResponse)
        expectStoreCreatedStateSuccess(userId, messageId, expectedResponse.message.messageId)

        // When
        val actual = draftRepository(userId, messageId)

        // Then
        assertEquals(Unit.right(), actual)
        coVerify(exactly = 0) { draftRemoteDataSource.update(any(), any()) }
    }

    @Test
    fun `update draft state with api message id when create call succeeds`() = runTest {
        // Given
        val messageId = MessageIdSample.LocalDraft
        val remoteDraftId = MessageIdSample.RemoteDraft
        val expectedDraft = MessageWithBodySample.Invoice
        val expectedAction = DraftAction.Compose
        val expectedResponse = MessageWithBodySample.RemoteDraft
        val expectedDraftState = DraftStateSample.NewDraftState
        expectGetDraftStateSucceeds(userId, messageId, expectedDraftState)
        expectGetLocalMessageSucceeds(userId, messageId, expectedDraft)
        expectRemoteDataSourceCreateSuccess(userId, expectedDraft, expectedAction, expectedResponse)
        expectStoreCreatedStateSuccess(userId, messageId, remoteDraftId)

        // When
        val actual = draftRepository(userId, messageId)

        // Then
        assertEquals(Unit.right(), actual)
        coVerify { draftStateRepository.saveCreatedState(userId, messageId, remoteDraftId) }
    }

    @Test
    fun `returns local failure when reading the message from DB fails`() = runTest {
        // Given
        val messageId = MessageIdSample.Invoice
        val expectedError = DataError.Local.NoDataCached
        expectGetLocalMessageFails(userId, messageId, expectedError)

        // When
        val actual = draftRepository(userId, messageId)

        // Then
        assertEquals(expectedError.left(), actual)
    }

    @Test
    fun `returns local failure when reading the draft state from DB fails`() = runTest {
        // Given
        val messageId = MessageIdSample.Invoice
        val expectedDraft = MessageWithBodySample.Invoice
        val expectedError = DataError.Local.NoDataCached
        expectGetLocalMessageSucceeds(userId, messageId, expectedDraft)
        expectGetDraftStateFails(userId, messageId, expectedError)

        // When
        val actual = draftRepository(userId, messageId)

        // Then
        assertEquals(expectedError.left(), actual)
    }

    @Test
    fun `returns remote failure when remote data source fails`() = runTest {
        // Given
        val messageId = MessageIdSample.LocalDraft
        val expectedDraft = MessageWithBodySample.Invoice
        val expectedAction = DraftAction.Compose
        val expectedError = DataError.Remote.Proton(ProtonError.MessageUpdateDraftNotDraft)
        val expectedDraftState = DraftStateSample.NewDraftState
        expectGetDraftStateSucceeds(userId, messageId, expectedDraftState)
        expectGetLocalMessageSucceeds(userId, messageId, expectedDraft)
        expectRemoteDataSourceFailure(userId, expectedDraft, expectedAction, expectedError)

        // When
        val actual = draftRepository(userId, messageId)

        // Then
        assertEquals(expectedError.left(), actual)
    }

    @Test
    fun `sync performs update on remote data source when draft is already known to the API`() = runTest {
        // Given
        val messageId = MessageIdSample.RemoteDraft
        val expectedDraft = MessageWithBodySample.RemoteDraft
        val expectedResponse = MessageWithBodySample.RemoteDraft
        val expectedDraftState = DraftStateSample.RemoteDraftState
        expectGetDraftStateSucceeds(userId, messageId, expectedDraftState)
        expectGetLocalMessageSucceeds(userId, messageId, expectedDraft)
        expectRemoteDataSourceUpdateSuccess(userId, expectedDraft, expectedResponse)

        // When
        val actual = draftRepository(userId, messageId)

        // Then
        assertEquals(Unit.right(), actual)
        coVerify(exactly = 0) { draftRemoteDataSource.create(any(), any(), any()) }
    }

    private fun expectStoreCreatedStateSuccess(
        userId: UserId,
        messageId: MessageId,
        remoteDraftId: MessageId
    ) {
        coEvery { draftStateRepository.saveCreatedState(userId, messageId, remoteDraftId) } returns Unit.right()
    }

    private fun expectRemoteDataSourceCreateSuccess(
        userId: UserId,
        messageWithBody: MessageWithBody,
        action: DraftAction,
        response: MessageWithBody
    ) {
        coEvery { draftRemoteDataSource.create(userId, messageWithBody, action) } returns response.right()
    }

    private fun expectRemoteDataSourceUpdateSuccess(
        userId: UserId,
        messageWithBody: MessageWithBody,
        response: MessageWithBody
    ) {
        coEvery { draftRemoteDataSource.update(userId, messageWithBody) } returns response.right()
    }


    private fun expectRemoteDataSourceFailure(
        userId: UserId,
        messageWithBody: MessageWithBody,
        action: DraftAction,
        error: DataError.Remote
    ) {
        coEvery { draftRemoteDataSource.create(userId, messageWithBody, action) } returns error.left()
    }

    private fun expectGetLocalMessageSucceeds(
        userId: UserId,
        messageId: MessageId,
        expectedMessage: MessageWithBody
    ) {
        every { messageRepository.observeMessageWithBody(userId, messageId) } returns flowOf(expectedMessage.right())
    }

    private fun expectGetLocalMessageFails(
        userId: UserId,
        messageId: MessageId,
        error: DataError.Local
    ) {
        every { messageRepository.observeMessageWithBody(userId, messageId) } returns flowOf(error.left())
    }

    private fun expectGetDraftStateSucceeds(
        userId: UserId,
        messageId: MessageId,
        expectedState: DraftState
    ) {
        coEvery { draftStateRepository.observe(userId, messageId) } returns flowOf(expectedState.right())
    }

    private fun expectGetDraftStateFails(
        userId: UserId,
        messageId: MessageId,
        error: DataError.Local
    ) {
        coEvery { draftStateRepository.observe(userId, messageId) } returns flowOf(error.left())
    }
}
