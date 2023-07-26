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

import ch.protonmail.android.composer.data.remote.SyncDraftWorker
import ch.protonmail.android.mailcommon.data.worker.Enqueuer
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class DraftRepositoryImplTest {

    private val enqueuer = mockk<Enqueuer>()

    private val draftRepository = DraftRepositoryImpl(enqueuer)

    @Test
    fun `enqueue sync draft work`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val messageId = MessageIdSample.LocalDraft
        val expectedParams = SyncDraftWorker.params(userId, messageId)
        val expectedWorkerId = SyncDraftWorker.id(messageId)
        givenEnqueuerSucceeds(expectedWorkerId, expectedParams)
        givenRemoveUnstartedWorkSucceeds(expectedWorkerId)

        // When
        draftRepository.sync(userId, messageId)

        // Then
        verify { enqueuer.enqueueUniqueWork<SyncDraftWorker>(expectedWorkerId, expectedParams) }
    }

    @Test
    fun `cancel any un-started existing work before enqueuing the new one`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val messageId = MessageIdSample.LocalDraft
        val expectedParams = SyncDraftWorker.params(userId, messageId)
        val expectedWorkerId = SyncDraftWorker.id(messageId)
        givenEnqueuerSucceeds(expectedWorkerId, expectedParams)
        givenRemoveUnstartedWorkSucceeds(expectedWorkerId)

        // When
        draftRepository.sync(userId, messageId)

        // Then
        coVerify { enqueuer.removeUnStartedExistingWork(expectedWorkerId) }
        verify { enqueuer.enqueueUniqueWork<SyncDraftWorker>(expectedWorkerId, expectedParams) }
    }

    private fun givenRemoveUnstartedWorkSucceeds(expectedWorkerId: String) {
        coEvery { enqueuer.removeUnStartedExistingWork(expectedWorkerId) } returns Unit
    }

    private fun givenEnqueuerSucceeds(workId: String, expectedParams: Map<String, String>) {
        every { enqueuer.enqueueUniqueWork<SyncDraftWorker>(workId, expectedParams) } returns Unit
    }
}
