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
import ch.protonmail.android.composer.data.local.dao.DraftStateDao
import ch.protonmail.android.mailmessage.data.local.entity.DraftStateEntity
import ch.protonmail.android.composer.data.sample.DraftStateEntitySample
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailcomposer.domain.sample.DraftStateSample
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import kotlin.test.Test
import kotlin.test.assertEquals

class DraftStateLocalDataSourceImplTest {

    private val draftStateDao = mockk<DraftStateDao>(relaxUnitFun = true)
    private val draftStateDatabase = mockk<DraftStateDatabase> {
        every { this@mockk.draftStateDao() } returns draftStateDao
    }

    private val localDataSource = DraftStateLocalDataSourceImpl(draftStateDatabase)

    @Test
    fun `observe draft state returns it when existing`() = runTest {
        val userId = UserIdSample.Primary
        val draftId = MessageIdSample.EmptyDraft
        val expected = DraftStateSample.NewDraftState
        val draftStateEntity = DraftStateEntitySample.NewDraftState
        expectDraftStateDaoSuccess(userId, draftId, draftStateEntity)

        val actual = localDataSource.observe(userId, draftId).first()

        assertEquals(expected.right(), actual)
    }

    @Test
    fun `observe draft state returns no data cached error when not existing`() = runTest {
        val userId = UserIdSample.Primary
        val draftId = MessageIdSample.EmptyDraft
        val expectedError = DataError.Local.NoDataCached
        expectDraftStateDaoReturnsNull(userId, draftId)

        val actual = localDataSource.observe(userId, draftId).first()

        assertEquals(expectedError.left(), actual)
    }

    @Test
    fun `save draft state returns Unit when succeeding`() = runTest {
        val draftState = DraftStateSample.RemoteDraftState
        val draftStateEntity = DraftStateEntitySample.RemoteDraft
        expectDraftStateDaoUpsertSuccess(draftStateEntity)

        val actual = localDataSource.save(draftState)

        assertEquals(Unit.right(), actual)
        coVerify { draftStateDao.insertOrUpdate(draftStateEntity) }
    }

    private fun expectDraftStateDaoUpsertSuccess(draftStateEntity: DraftStateEntity) {
        coEvery { draftStateDao.insertOrUpdate(draftStateEntity) } returns Unit
    }

    private fun expectDraftStateDaoSuccess(
        userId: UserId,
        draftId: MessageId,
        expected: DraftStateEntity
    ) {
        every { draftStateDao.observeDraftState(userId, draftId) } returns flowOf(expected)
    }

    private fun expectDraftStateDaoReturnsNull(userId: UserId, draftId: MessageId) {
        every { draftStateDao.observeDraftState(userId, draftId) } returns flowOf(null)
    }
}
