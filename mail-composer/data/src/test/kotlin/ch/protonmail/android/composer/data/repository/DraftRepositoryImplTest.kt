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

import androidx.work.ExistingWorkPolicy
import ch.protonmail.android.composer.data.remote.UploadAttachmentsWorker
import ch.protonmail.android.composer.data.remote.UploadDraftWorker
import ch.protonmail.android.mailcommon.data.worker.Enqueuer
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class DraftRepositoryImplTest {

    private val enqueuer = mockk<Enqueuer>()

    private val draftRepository = DraftRepositoryImpl(enqueuer)

    @Test
    fun `upload enqueue upload draft work when not already enqueued`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val messageId = MessageIdSample.LocalDraft
        val expectedParams = UploadDraftWorker.params(userId, messageId)
        val expectedWorkerId = UploadDraftWorker.id(messageId)
        val expectedWorkPolicy = ExistingWorkPolicy.KEEP
        givenEnqueuerSucceeds(expectedWorkerId, expectedParams, expectedWorkPolicy)

        // When
        draftRepository.upload(userId, messageId)

        // Then
        verify {
            enqueuer.enqueueUniqueWork<UploadDraftWorker>(
                workerId = expectedWorkerId,
                params = expectedParams,
                existingWorkPolicy = expectedWorkPolicy
            )
        }
    }

    @Test
    fun `force upload enqueue upload draft work also if existing`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val messageId = MessageIdSample.LocalDraft
        val expectedParamsDraftWorker = UploadDraftWorker.params(userId, messageId)
        val expectedParamsAttachmentsWorker = UploadAttachmentsWorker.params(userId, messageId)
        val expectedWorkerId = UploadDraftWorker.id(messageId)
        // This work should happen independently on the outcome of the previous one
        val expectedWorkPolicy = ExistingWorkPolicy.APPEND_OR_REPLACE
        givenChainEnqueuerSucceeds(
            workId = expectedWorkerId,
            expectedParams1 = expectedParamsDraftWorker,
            expectedParams2 = expectedParamsAttachmentsWorker,
            existingWorkPolicy = expectedWorkPolicy
        )

        // When
        draftRepository.forceUpload(userId, messageId)

        // Then
        verify {
            enqueuer.enqueueInChain<UploadDraftWorker, UploadAttachmentsWorker>(
                uniqueWorkId = expectedWorkerId,
                params1 = expectedParamsDraftWorker,
                params2 = expectedParamsAttachmentsWorker,
                existingWorkPolicy = expectedWorkPolicy
            )
        }
    }

    private fun givenEnqueuerSucceeds(
        workId: String,
        expectedParams: Map<String, String>,
        existingWorkPolicy: ExistingWorkPolicy
    ) {
        every {
            enqueuer.enqueueUniqueWork<UploadDraftWorker>(
                workerId = workId,
                params = expectedParams,
                existingWorkPolicy = existingWorkPolicy
            )
        } returns Unit
    }

    private fun givenChainEnqueuerSucceeds(
        workId: String,
        expectedParams1: Map<String, String>,
        expectedParams2: Map<String, String>,
        existingWorkPolicy: ExistingWorkPolicy
    ) {
        every {
            enqueuer.enqueueInChain<UploadDraftWorker, UploadAttachmentsWorker>(
                uniqueWorkId = workId,
                params1 = expectedParams1,
                params2 = expectedParams2,
                existingWorkPolicy = existingWorkPolicy
            )
        } returns Unit
    }
}
