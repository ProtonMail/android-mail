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
import ch.protonmail.android.mailcomposer.domain.repository.MessageRepository
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.mailmessage.domain.sample.MessageWithBodySample
import ch.protonmail.android.test.utils.rule.LoggingTestRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class MoveToSentOptimisticallyTest {

    @get:Rule
    val loggerRule = LoggingTestRule()

    private val messageRepository: MessageRepository = mockk()
    private val findLocalDraft = mockk<FindLocalDraft>()

    private val moveToSentOptimistically = MoveToSentOptimistically(
        messageRepository,
        findLocalDraft
    )

    @Test
    fun `moves message to draft folder using local draft's message id`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val messageId = MessageIdSample.PlainTextMessage
        val expectedLocalDraft = MessageWithBodySample.RemoteDraft
        coEvery { findLocalDraft(userId, messageId) } returns expectedLocalDraft
        coEvery {
            messageRepository.moveMessageFromDraftsToSent(userId, expectedLocalDraft.message.messageId)
        } returns Unit.right()

        // When
        moveToSentOptimistically(userId, messageId)

        // Then
        coVerify { messageRepository.moveMessageFromDraftsToSent(userId, expectedLocalDraft.message.messageId) }
    }

    @Test
    fun `moves message to draft folder using given message id when local draft is not found exist`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val messageId = MessageIdSample.PlainTextMessage
        coEvery { findLocalDraft(userId, messageId) } returns null
        coEvery { messageRepository.moveMessageFromDraftsToSent(userId, messageId) } returns Unit.right()

        // When
        moveToSentOptimistically(userId, messageId)

        // Then
        coVerify { messageRepository.moveMessageFromDraftsToSent(userId, messageId) }
        loggerRule.assertErrorLogged("Local draft not found while trying to move sending message to sent $messageId")
    }
}
