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
import ch.protonmail.android.mailmessage.domain.model.DraftAction
import ch.protonmail.android.mailcomposer.domain.repository.DraftRepository
import ch.protonmail.android.mailmessage.domain.repository.DraftStateRepository
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds

class DraftUploaderTest {

    private val draftStateRepository = mockk<DraftStateRepository>()
    private val draftRepository = mockk<DraftRepository>()
    private val testDispatcher = StandardTestDispatcher()
    private val coroutineScope = CoroutineScope(testDispatcher)

    private val draftUploader = DraftUploader(draftStateRepository, draftRepository, testDispatcher)

    @Test
    fun `saves draft state as local when starting`() = runTest(testDispatcher) {
        // Given
        val userId = UserIdSample.Primary
        val messageId = MessageIdSample.RemoteDraft
        val action = DraftAction.Compose
        expectSaveLocalStateSuccess(userId, messageId, action)
        expectSyncDraft(userId, messageId)

        // When
        draftUploader.startContinuousUpload(userId, messageId, action, coroutineScope)
        testDispatcher.scheduler.advanceTimeBy(1000)
        draftUploader.stopContinuousUpload()

        // Then
        coVerify { draftStateRepository.createOrUpdateLocalState(userId, messageId, action) }
    }

    @Test
    fun `keeps syncing the draft based on defined sync interval`() = runTest(testDispatcher) {
        // Given
        val userId = UserIdSample.Primary
        val messageId = MessageIdSample.RemoteDraft
        val action = DraftAction.Compose
        expectSaveLocalStateSuccess(userId, messageId, action)
        expectSyncDraft(userId, messageId)

        // When
        draftUploader.startContinuousUpload(userId, messageId, action, coroutineScope)
        // Advance time by just a bit more than 2xSyncInterval so that 2 loops are executed
        val advanceTimeMillis = DraftUploader.SyncInterval.times(2).plus(100.milliseconds).inWholeMilliseconds
        testDispatcher.scheduler.advanceTimeBy(advanceTimeMillis)
        draftUploader.stopContinuousUpload()

        // Then
        coVerify(exactly = 2) { draftRepository.upload(userId, messageId) }
    }

    @Test
    fun `upload calls draft repository force upload`() = runTest(testDispatcher) {
        // Given
        val userId = UserIdSample.Primary
        val messageId = MessageIdSample.RemoteDraft
        expectForceUploadDraft(userId, messageId)

        // When
        draftUploader.upload(userId, messageId)

        // Then
        coVerify(exactly = 1) { draftRepository.forceUpload(userId, messageId) }
    }

    private fun expectForceUploadDraft(userId: UserId, messageId: MessageId) {
        coEvery { draftRepository.forceUpload(userId, messageId) } returns Unit
    }

    private fun expectSyncDraft(userId: UserId, messageId: MessageId) {
        coEvery { draftRepository.upload(userId, messageId) } returns Unit
    }

    private fun expectSaveLocalStateSuccess(
        userId: UserId,
        messageId: MessageId,
        action: DraftAction.Compose
    ) {
        coEvery { draftStateRepository.createOrUpdateLocalState(userId, messageId, action) } returns Unit.right()
    }
}
