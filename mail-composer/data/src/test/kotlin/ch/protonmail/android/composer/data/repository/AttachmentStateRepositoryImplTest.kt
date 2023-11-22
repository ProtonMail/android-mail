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
import ch.protonmail.android.composer.data.local.AttachmentStateLocalDataSourceImpl
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailcomposer.domain.sample.AttachmentStateSample
import ch.protonmail.android.mailmessage.domain.model.AttachmentId
import ch.protonmail.android.mailmessage.domain.model.AttachmentState
import ch.protonmail.android.mailmessage.domain.model.AttachmentSyncState
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import kotlin.test.Test
import kotlin.test.assertEquals

class AttachmentStateRepositoryImplTest {

    private val userId = UserIdSample.Primary
    private val attachmentId = AttachmentId("attachment_id")

    private val attachmentStateLocalDataSource = mockk<AttachmentStateLocalDataSourceImpl>()

    private val repository = AttachmentStateRepositoryImpl(attachmentStateLocalDataSource)

    @Test
    fun `get attachment state returns it when existing`() = runTest {
        // Given
        val draftId = MessageIdSample.EmptyDraft
        val expected = AttachmentStateSample.LocalAttachmentState
        expectAttachmentStateLocalDataSourceSuccess(userId, draftId, attachmentId, expected)

        // When
        val actual = repository.getAttachmentState(userId, draftId, attachmentId)

        // Then
        assertEquals(expected.right(), actual)
    }

    @Test
    fun `get attachment state returns no data cached error when not existing`() = runTest {
        // Given
        val draftId = MessageIdSample.EmptyDraft
        val expected = DataError.Local.NoDataCached.left()
        expectAttachmentStateLocalDataSourceNoData(userId, draftId, attachmentId)

        // When
        val actual = repository.getAttachmentState(userId, draftId, attachmentId)

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `get all attachment states for message returns them when existing`() = runTest {
        // Given
        val draftId = MessageIdSample.EmptyDraft
        val expected = listOf(AttachmentStateSample.LocalAttachmentState)
        expectGetAllAttachmentStatesReturnsListWithEntries(draftId, expected)

        // When
        val actual = repository.getAllAttachmentStatesForMessage(userId, draftId)

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `get all attachment states for message returns empty list when not existing`() = runTest {
        // Given
        val draftId = MessageIdSample.EmptyDraft
        val expected = emptyList<AttachmentState>()
        expectGetAllAttachmentStatesReturnsListWithEntries(draftId, expected)

        // When
        val actual = repository.getAllAttachmentStatesForMessage(userId, draftId)

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `set attachment upload state does update attachment sync state`() = runTest {
        // Given
        val draftId = MessageIdSample.RemoteDraft
        val existingAttachmentState = AttachmentStateSample.LocalAttachmentState
        val expectedAttachmentState = existingAttachmentState.copy(state = AttachmentSyncState.Uploaded)
        expectAttachmentStateLocalDataSourceSuccess(userId, draftId, attachmentId, existingAttachmentState)
        expectLocalDataSourceUpsertSuccess(expectedAttachmentState)

        // When
        val actual = repository.setAttachmentToUploadState(
            userId,
            draftId,
            expectedAttachmentState.attachmentId
        )

        // Then
        assertEquals(Unit.right(), actual)
    }

    @Test
    fun `store synced state calls attachment state local data source`() = runTest {
        // Given
        val expectedAttachmentState = AttachmentStateSample.LocalAttachmentState
        val messageId = MessageIdSample.RemoteDraft
        expectLocalDataSourceUpsertSuccess(expectedAttachmentState)

        // When
        val actual = repository.createOrUpdateLocalState(userId, messageId, attachmentId)

        // Then
        coVerify { attachmentStateLocalDataSource.createOrUpdate(expectedAttachmentState) }
        assertEquals(Unit.right(), actual)
    }

    @Test
    fun `store synced states calls attachment state local data source for multiple states`() = runTest {
        // Given
        val attachmentId1 = AttachmentId("1")
        val attachmentId2 = AttachmentId("2")
        val expectedAttachmentState = AttachmentStateSample.LocalAttachmentState.copy(attachmentId = attachmentId1)
        val expectedAttachmentState2 = AttachmentStateSample.LocalAttachmentState.copy(attachmentId = attachmentId2)
        val messageId = MessageIdSample.RemoteDraft
        expectLocalDataSourceUpsertSuccess(listOf(expectedAttachmentState, expectedAttachmentState2))

        // When
        val actual = repository.createOrUpdateLocalStates(
            userId = userId,
            messageId = messageId,
            attachmentIds = listOf(attachmentId1, attachmentId2),
            syncState = AttachmentSyncState.Local
        )

        // Then
        coVerify {
            attachmentStateLocalDataSource.createOrUpdate(listOf(expectedAttachmentState, expectedAttachmentState2))
        }
        assertEquals(Unit.right(), actual)
    }

    @Test
    fun `store synced state stores updates attachment id in attachment sync state`() = runTest {
        // Given
        val localAttachmentState = AttachmentStateSample.LocalAttachmentState
        val expectedAttachmentState = AttachmentStateSample.RemoteAttachmentState
        val messageId = MessageIdSample.RemoteDraft
        expectAttachmentStateLocalDataSourceSuccess(userId, messageId, attachmentId, localAttachmentState)
        expectLocalDataSourceUpsertSuccess(expectedAttachmentState)

        // When
        val actual = repository.setAttachmentToUploadState(
            userId = userId,
            messageId = messageId,
            attachmentId = expectedAttachmentState.attachmentId
        )

        // Then
        coVerify { attachmentStateLocalDataSource.createOrUpdate(expectedAttachmentState) }
        assertEquals(Unit.right(), actual)
    }

    @Test
    fun `should delete attachment state when existing`() = runTest {
        // Given
        val messageId = MessageIdSample.RemoteDraft
        val expectedAttachmentState = AttachmentStateSample.LocalAttachmentState
        expectAttachmentStateLocalDataSourceSuccess(userId, messageId, attachmentId, expectedAttachmentState)
        expectedDeleteSuccess(expectedAttachmentState)

        // When
        val actual = repository.deleteAttachmentState(userId, messageId, attachmentId)

        // Then
        coVerify { attachmentStateLocalDataSource.delete(expectedAttachmentState) }
        assertEquals(Unit.right(), actual)
    }

    private fun expectAttachmentStateLocalDataSourceSuccess(
        userId: UserId,
        messageId: MessageId,
        attachmentId: AttachmentId,
        expected: AttachmentState
    ) {
        coEvery {
            attachmentStateLocalDataSource.getAttachmentState(userId, messageId, attachmentId)
        } returns expected.right()
    }

    private fun expectAttachmentStateLocalDataSourceNoData(
        userId: UserId,
        messageId: MessageId,
        attachmentId: AttachmentId
    ) {
        coEvery {
            attachmentStateLocalDataSource.getAttachmentState(userId, messageId, attachmentId)
        } returns DataError.Local.NoDataCached.left()
    }

    private fun expectGetAllAttachmentStatesReturnsListWithEntries(
        draftId: MessageId,
        expected: List<AttachmentState>
    ) {
        coEvery { attachmentStateLocalDataSource.getAllAttachmentStatesForMessage(userId, draftId) } returns expected
    }

    private fun expectLocalDataSourceUpsertSuccess(state: AttachmentState) {
        coEvery { attachmentStateLocalDataSource.createOrUpdate(state) } returns Unit.right()
    }

    private fun expectLocalDataSourceUpsertSuccess(states: List<AttachmentState>) {
        coEvery { attachmentStateLocalDataSource.createOrUpdate(states) } returns Unit.right()
    }

    private fun expectedDeleteSuccess(state: AttachmentState) {
        coJustRun { attachmentStateLocalDataSource.delete(state) }
    }

}
