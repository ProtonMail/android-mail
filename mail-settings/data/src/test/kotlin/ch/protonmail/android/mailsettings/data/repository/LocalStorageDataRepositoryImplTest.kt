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

package ch.protonmail.android.mailsettings.data.repository

import androidx.work.Constraints
import app.cash.turbine.test
import ch.protonmail.android.mailcommon.data.worker.Enqueuer
import ch.protonmail.android.mailmessage.data.local.AttachmentLocalDataSource
import ch.protonmail.android.mailmessage.data.local.MessageLocalDataSource
import ch.protonmail.android.mailsettings.data.local.ClearLocalDataWorker
import ch.protonmail.android.mailsettings.domain.model.ClearDataAction
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

internal class LocalStorageDataRepositoryImplTest {

    private val enqueuer = mockk<Enqueuer>()
    private val attachmentLocalDataSource = mockk<AttachmentLocalDataSource>()
    private val messageLocalDataSource = mockk<MessageLocalDataSource>()
    private val localDataRepository = LocalStorageDataRepositoryImpl(
        messageLocalDataSource,
        attachmentLocalDataSource,
        enqueuer
    )

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `should propagate data correctly when message data size is observed`() = runTest {
        // Given
        every { messageLocalDataSource.observeCachedMessagesTotalSize() } returns flowOf(BaseSize)

        // When + Then
        localDataRepository.observeMessageDataTotalRawSize().test {
            assertEquals(BaseSize, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `should propagate attachment size 0 when the attachments folder does not exist`() = runTest {
        // Given
        coEvery { attachmentLocalDataSource.getAttachmentFolderForUserId(BaseUserId) } returns null

        // When
        val actual = localDataRepository.getAttachmentDataSizeForUserId(BaseUserId)

        // Then
        assertEquals(0L, actual)
    }

    @Test
    fun `should propagate attachment size 0 when the attachments folder is empty`() = runTest {
        // Given
        coEvery { attachmentLocalDataSource.getAttachmentFolderForUserId(BaseUserId)?.isDirectory } returns true
        coEvery { attachmentLocalDataSource.getAttachmentFolderForUserId(BaseUserId)?.list() } returns emptyArray()

        // When
        val actual = localDataRepository.getAttachmentDataSizeForUserId(BaseUserId)

        // Then
        assertEquals(0L, actual)
    }

    @Test
    fun `should propagate data correctly when attachments data size is fetched`() = runTest {
        // Given
        val expectedList = arrayOf("1", "2")
        coEvery { attachmentLocalDataSource.getAttachmentFolderForUserId(BaseUserId)?.isDirectory } returns false
        coEvery { attachmentLocalDataSource.getAttachmentFolderForUserId(BaseUserId)?.list() } returns expectedList
        coEvery { attachmentLocalDataSource.getAttachmentFolderForUserId(BaseUserId)?.length() } returns BaseSize

        // When
        val expected = localDataRepository.getAttachmentDataSizeForUserId(BaseUserId)

        // Then
        assertEquals(BaseSize, expected)
    }

    @Test
    fun `should enqueue a worker with the correct params when perform clear data is invoked`() {
        // Given
        every {
            enqueuer.enqueueUniqueWork<ClearLocalDataWorker>(
                userId = BaseUserId,
                workerId = BaseWorkerId,
                params = BaseParams,
                constraints = BaseConstraints
            )
        } returns Unit

        // When
        localDataRepository.performClearData(BaseUserId, BaseClearAction)

        // Then
        verify(exactly = 1) {
            enqueuer.enqueueUniqueWork<ClearLocalDataWorker>(
                userId = BaseUserId,
                workerId = BaseWorkerId,
                params = BaseParams,
                constraints = BaseConstraints
            )
        }
    }

    private companion object {

        const val BaseSize: Long = 10L
        val BaseUserId = UserId("userId")
        val BaseClearAction = ClearDataAction.ClearAll
        val BaseWorkerId = "ClearLocalDataWorker-${BaseClearAction.hashCode()}"
        val BaseConstraints = Constraints.Builder().build()
        val BaseParams = ClearLocalDataWorker.params(BaseClearAction)
    }
}
