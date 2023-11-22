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

package ch.protonmail.android.composer.data.local

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.composer.data.sample.AttachmentStateEntitySample
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailcomposer.domain.sample.AttachmentStateSample
import ch.protonmail.android.mailmessage.data.local.dao.AttachmentStateDao
import ch.protonmail.android.mailmessage.data.local.entity.AttachmentStateEntity
import ch.protonmail.android.mailmessage.domain.model.AttachmentId
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import kotlin.test.Test
import kotlin.test.assertEquals

class AttachmentStateLocalDataSourceImplTest {

    private val attachmentStateDao = mockk<AttachmentStateDao>(relaxUnitFun = true)
    private val draftStateDatabase = mockk<DraftStateDatabase> {
        every { attachmentStateDao() } returns attachmentStateDao
    }

    private val localDataSource = AttachmentStateLocalDataSourceImpl(draftStateDatabase)

    @Test
    fun `get attachment state returns it when existing`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val messageId = MessageIdSample.EmptyDraft
        val attachmentId = AttachmentId("attachment_id")
        val expected = AttachmentStateSample.LocalAttachmentState
        val attachmentStateEntity = AttachmentStateEntitySample.LocalAttachmentState
        expectAttachmentStateDaoSuccess(userId, messageId, attachmentId, attachmentStateEntity)

        // When
        val actual = localDataSource.getAttachmentState(userId, messageId, attachmentId)

        // Then
        assertEquals(expected.right(), actual)
    }

    @Test
    fun `get attachment state returns no data when not existing`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val messageId = MessageIdSample.EmptyDraft
        val attachmentId = AttachmentId("attachment_id")
        val expected = DataError.Local.NoDataCached
        expectAttachmentStateDaoReturnsNull(userId, messageId, attachmentId)

        // When
        val actual = localDataSource.getAttachmentState(userId, messageId, attachmentId)

        // Then
        assertEquals(expected.left(), actual)
    }

    @Test
    fun `save attachment state returns Unit when succeeding`() = runTest {
        // Given
        val attachmentState = AttachmentStateSample.LocalAttachmentState
        val attachmentStateEntity = AttachmentStateEntitySample.LocalAttachmentState
        expectAttachmentStateDaoUpsertSuccess(attachmentStateEntity)

        // When
        val actual = localDataSource.createOrUpdate(attachmentState)

        // Then
        assertEquals(Unit.right(), actual)
        coVerify { attachmentStateDao.insertOrUpdate(attachmentStateEntity) }
    }

    @Test
    fun `save attachment states returns Unit when succeeding`() = runTest {
        // Given
        val attachmentStates = listOf(
            AttachmentStateSample.LocalAttachmentState.copy(attachmentId = AttachmentId("attachment_id_1")),
            AttachmentStateSample.LocalAttachmentState.copy(attachmentId = AttachmentId("attachment_id_2")),
            AttachmentStateSample.LocalAttachmentState.copy(attachmentId = AttachmentId("attachment_id_3"))
        )
        val attachmentStateEntities = listOf(
            AttachmentStateEntitySample.LocalAttachmentState.copy(attachmentId = AttachmentId("attachment_id_1")),
            AttachmentStateEntitySample.LocalAttachmentState.copy(attachmentId = AttachmentId("attachment_id_2")),
            AttachmentStateEntitySample.LocalAttachmentState.copy(attachmentId = AttachmentId("attachment_id_3"))
        )

        // When
        val actual = localDataSource.createOrUpdate(attachmentStates)

        // Then
        assertEquals(Unit.right(), actual)
        coVerify { attachmentStateDao.insertOrUpdate(*attachmentStateEntities.toTypedArray()) }
    }

    @Test
    fun `delete attachment state calls dao`() = runTest {
        val attachmentState = AttachmentStateSample.LocalAttachmentState
        val attachmentStateEntity = AttachmentStateEntitySample.LocalAttachmentState
        expectedAttachmentStateDaoDeleteSuccess(attachmentStateEntity)

        localDataSource.delete(attachmentState)

        coVerify { attachmentStateDao.delete(attachmentStateEntity) }
    }

    @Test
    fun `get all states of all attachments connected to user and message`() = runTest {
        val userId = UserIdSample.Primary
        val messageId = MessageIdSample.EmptyDraft
        val attachmentStateEntity = listOf(AttachmentStateEntitySample.LocalAttachmentState)
        val expected = listOf(AttachmentStateSample.LocalAttachmentState)
        expectGetAllAttachmentStatesForMessageSuccessful(userId, messageId, attachmentStateEntity)

        val actual = localDataSource.getAllAttachmentStatesForMessage(userId, messageId)

        assertEquals(expected, actual)
    }

    @Test
    fun `get all states of attachments for user and message returns empty list when no states are stored`() = runTest {
        val userId = UserIdSample.Primary
        val messageId = MessageIdSample.EmptyDraft
        val expectedStateEntities = listOf<AttachmentStateEntity>()
        expectGetAllAttachmentStatesForMessageSuccessful(userId, messageId, expectedStateEntities)

        val actual = localDataSource.getAllAttachmentStatesForMessage(userId, messageId)

        assertEquals(emptyList(), actual)
    }

    private fun expectAttachmentStateDaoSuccess(
        userId: UserId,
        messageId: MessageId,
        attachmentId: AttachmentId,
        attachmentStateEntity: AttachmentStateEntity
    ) {
        coEvery {
            attachmentStateDao.getAttachmentState(userId, messageId, attachmentId)
        } returns attachmentStateEntity
    }

    private fun expectAttachmentStateDaoReturnsNull(
        userId: UserId,
        messageId: MessageId,
        attachmentId: AttachmentId
    ) {
        coEvery {
            attachmentStateDao.getAttachmentState(userId, messageId, attachmentId)
        } returns null
    }

    private fun expectAttachmentStateDaoUpsertSuccess(attachmentStateEntity: AttachmentStateEntity) {
        coEvery { attachmentStateDao.insertOrUpdate(attachmentStateEntity) } returns Unit
    }

    private fun expectedAttachmentStateDaoDeleteSuccess(attachmentStateEntity: AttachmentStateEntity) {
        coEvery { attachmentStateDao.delete(attachmentStateEntity) } returns Unit
    }

    private fun expectGetAllAttachmentStatesForMessageSuccessful(
        userId: UserId,
        messageId: MessageId,
        attachmentStateEntities: List<AttachmentStateEntity>
    ) {
        coEvery {
            attachmentStateDao.getAllAttachmentStatesForMessage(userId, messageId)
        } returns attachmentStateEntities
    }

}
