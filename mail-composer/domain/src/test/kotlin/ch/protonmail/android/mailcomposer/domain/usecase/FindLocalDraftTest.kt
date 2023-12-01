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

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailmessage.domain.model.DraftState
import ch.protonmail.android.mailmessage.domain.repository.DraftStateRepository
import ch.protonmail.android.mailcomposer.domain.sample.DraftStateSample
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.MessageWithBody
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.mailmessage.domain.sample.MessageWithBodySample
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FindLocalDraftTest {

    private val messageRepository = mockk<MessageRepository>()
    private val draftStateRepository = mockk<DraftStateRepository>()

    private val findLocalDraft = FindLocalDraft(messageRepository, draftStateRepository)

    @Test
    fun `when message is found by messageId return it`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val messageId = MessageIdSample.Invoice
        val expectedMessage = MessageWithBodySample.Invoice
        val expectedDraftState = DraftStateSample.RemoteDraftState
        expectGetDraftStateSucceeds(userId, messageId, expectedDraftState)
        expectGetLocalMessageSucceeds(userId, messageId, expectedMessage)
        expectGetDraftStateFails(userId, messageId, DataError.Local.NoDataCached)

        // When
        val actual = findLocalDraft(userId, messageId)

        // Then
        assertEquals(expectedMessage, actual)
    }

    @Test
    fun `when message is not found by messageId but it is found by draft state apiMessageId return it`() = runTest {
        /*
         * This case would happen in case of concurrent executions of message creation (eg. typing a new draft while
         * fully offline results in two works for upload the draft being scheduled as we append when exiting composer).
         * First one succeeds, second fails as the first did update the ID of the message id DB.
         */
        // Given
        val userId = UserIdSample.Primary
        val messageId = MessageIdSample.LocalDraft
        val apiMessageId = MessageIdSample.RemoteDraft
        val expectedMessage = MessageWithBodySample.RemoteDraft
        val expectedDraftState = DraftStateSample.RemoteDraftState
        expectGetDraftStateSucceeds(userId, messageId, expectedDraftState)
        expectGetLocalMessageFails(userId, messageId)
        expectGetLocalMessageSucceeds(userId, apiMessageId, expectedMessage)

        // When
        val actual = findLocalDraft(userId, messageId)

        // Then
        assertEquals(expectedMessage, actual)
    }

    @Test
    fun `when message is not found by any id return null`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val messageId = MessageIdSample.LocalDraft
        val apiMessageId = MessageIdSample.RemoteDraft
        val expectedDraftState = DraftStateSample.RemoteDraftState
        expectGetDraftStateSucceeds(userId, messageId, expectedDraftState)
        expectGetLocalMessageFails(userId, messageId)
        expectGetLocalMessageFails(userId, apiMessageId)

        // When
        val actual = findLocalDraft(userId, messageId)

        // Then
        assertNull(actual)
    }

    private fun expectGetLocalMessageSucceeds(
        userId: UserId,
        messageId: MessageId,
        expectedMessage: MessageWithBody
    ) {
        coEvery { messageRepository.getLocalMessageWithBody(userId, messageId) } returns expectedMessage
    }

    private fun expectGetLocalMessageFails(userId: UserId, messageId: MessageId) {
        coEvery { messageRepository.getLocalMessageWithBody(userId, messageId) } returns null
    }

    private fun expectGetDraftStateSucceeds(
        userId: UserId,
        messageId: MessageId,
        expectedState: DraftState
    ) {
        coEvery { draftStateRepository.observe(userId, messageId) } returns flowOf(expectedState.right())
    }

    private fun expectGetDraftStateFails(
        userId: UserId,
        messageId: MessageId,
        error: DataError.Local
    ) {
        coEvery { draftStateRepository.observe(userId, messageId) } returns flowOf(error.left())
    }
}
