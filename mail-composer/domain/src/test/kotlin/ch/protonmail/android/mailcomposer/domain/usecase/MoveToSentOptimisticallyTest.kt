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
import ch.protonmail.android.mailcomposer.domain.repository.DraftStateRepository
import ch.protonmail.android.mailcomposer.domain.repository.MessageRepository
import ch.protonmail.android.mailcomposer.domain.sample.DraftStateSample
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class MoveToSentOptimisticallyTest {

    private val messageRepository: MessageRepository = mockk()
    private val draftStateRepository: DraftStateRepository = mockk()

    private val moveToSentOptimistically = MoveToSentOptimistically(
        messageRepository,
        draftStateRepository
    )

    @Test
    fun `moves message to draft folder using draft state's api message id when existing`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val messageId = MessageIdSample.PlainTextMessage
        val draftState = DraftStateSample.LocalDraftThatWasSyncedOnce
        every { draftStateRepository.observe(userId, messageId) } returns flowOf(draftState.right())
        coEvery {
            messageRepository.moveMessageFromDraftsToSent(userId, draftState.apiMessageId!!)
        } returns Unit.right()

        // When
        moveToSentOptimistically(userId, messageId)

        // Then
        val expectedMessageId = draftState.apiMessageId!!
        coVerify { messageRepository.moveMessageFromDraftsToSent(userId, expectedMessageId) }
    }

    @Test
    fun `moves message to draft folder using given message id when draft state has no api message id`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val messageId = MessageIdSample.PlainTextMessage
        val draftState = DraftStateSample.LocalDraftNeverSynced
        every { draftStateRepository.observe(userId, messageId) } returns flowOf(draftState.right())
        coEvery { messageRepository.moveMessageFromDraftsToSent(userId, messageId) } returns Unit.right()

        // When
        moveToSentOptimistically(userId, messageId)

        // Then
        coVerify { messageRepository.moveMessageFromDraftsToSent(userId, messageId) }
    }
}
