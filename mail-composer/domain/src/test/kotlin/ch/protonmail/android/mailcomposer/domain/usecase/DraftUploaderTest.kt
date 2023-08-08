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

import arrow.core.right
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailcomposer.domain.model.DraftAction
import ch.protonmail.android.mailcomposer.domain.repository.DraftRepository
import ch.protonmail.android.mailcomposer.domain.repository.DraftStateRepository
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import org.junit.Ignore
import org.junit.Test

class DraftUploaderTest {

    private val draftStateRepository = mockk<DraftStateRepository>()
    private val draftRepository = mockk<DraftRepository>()
    private val testDispatcher = UnconfinedTestDispatcher()
    private val coroutineScope = CoroutineScope(testDispatcher)

    private val draftUploader = DraftUploader(draftStateRepository, draftRepository, testDispatcher)

    @Test
    @Ignore("Test fails due to the infinite loop going OutOfMemory")
    fun `saves draft state as local when starting`() = runTest(testDispatcher) {
        // Given
        val userId = UserIdSample.Primary
        val messageId = MessageIdSample.RemoteDraft
        val action = DraftAction.Compose
        expectSaveLocalStateSuccess(userId, messageId, action)
        expectSyncDraft(userId, messageId)
        // When
        val job = launch { draftUploader.startContinuousUpload(userId, messageId, action, coroutineScope) }
        testDispatcher.scheduler.advanceTimeBy(1000)
        job.cancel()
        // Then
        coVerify { draftStateRepository.saveLocalState(userId, messageId, action) }
    }

    @Test
    @Ignore("Test fails due to the infinite loop going OutOfMemory")
    fun `keeps syncing the draft based on defined sync interval`() = runTest(testDispatcher) {
        // Given
        val userId = UserIdSample.Primary
        val messageId = MessageIdSample.RemoteDraft
        val action = DraftAction.Compose
        expectSaveLocalStateSuccess(userId, messageId, action)
        expectSyncDraft(userId, messageId)
        // When
        val job = launch { draftUploader.startContinuousUpload(userId, messageId, action, coroutineScope) }
        testDispatcher.scheduler.advanceTimeBy(1000)
        job.cancel()
        // Then
        coVerify { draftRepository.upload(userId, messageId) }
    }

    private fun expectSyncDraft(userId: UserId, messageId: MessageId) {
        coEvery { draftRepository.upload(userId, messageId) } returns Unit
    }

    private fun expectSaveLocalStateSuccess(
        userId: UserId,
        messageId: MessageId,
        action: DraftAction.Compose
    ) {
        coEvery { draftStateRepository.saveLocalState(userId, messageId, action) } returns Unit.right()
    }
}
