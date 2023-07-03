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
import ch.protonmail.android.mailcommon.domain.sample.UserAddressSample
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailmessage.domain.entity.MessageId
import ch.protonmail.android.mailmessage.domain.entity.MessageWithBody
import ch.protonmail.android.mailmessage.domain.entity.Sender
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.mailmessage.domain.sample.MessageWithBodySample
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import me.proton.core.user.domain.entity.UserAddress
import org.junit.Test
import kotlin.test.assertEquals

class StoreDraftWithSenderTest {

    private val createEmptyDraftMock = mockk<CreateEmptyDraft>()
    private val saveDraftMock = mockk<SaveDraft>()
    private val messageRepositoryMock = mockk<MessageRepository>()

    private val storeDraftWithSender = StoreDraftWithSender(
        createEmptyDraftMock,
        saveDraftMock,
        messageRepositoryMock
    )

    @Test
    fun `should save an existing draft with sender and address ID when draft already exists`() = runTest {
        // Given
        val senderAddress = UserAddressSample.AliasAddress
        val expectedUserId = UserIdSample.Primary
        val draftMessageId = MessageIdSample.build()
        val existingDraft = expectedExistingDraft(expectedUserId, draftMessageId) { MessageWithBodySample.EmptyDraft }
        val expectedSavedDraft = existingDraft.copy(
            message = existingDraft.message.copy(
                sender = Sender(senderAddress.email, senderAddress.displayName!!),
                addressId = senderAddress.addressId
            )
        )
        givenSaveDraftSucceeds(expectedSavedDraft, expectedUserId)

        // When
        val actualEither = storeDraftWithSender(draftMessageId, senderAddress, expectedUserId)

        // Then
        coVerify { saveDraftMock(expectedSavedDraft, expectedUserId) }
        assertEquals(Unit.right(), actualEither)
    }

    @Test
    fun `should save a new draft with sender and address ID when draft does not exist yet`() = runTest {
        // Given
        val senderAddress = UserAddressSample.AliasAddress
        val expectedUserId = UserIdSample.Primary
        val draftMessageId = MessageIdSample.build()
        val newDraft = expectedNewDraft(expectedUserId, draftMessageId, senderAddress) {
            MessageWithBodySample.EmptyDraft
        }
        val expectedSavedDraft = newDraft.copy(
            message = newDraft.message.copy(
                sender = Sender(senderAddress.email, senderAddress.displayName!!),
                addressId = senderAddress.addressId
            )
        )
        givenSaveDraftSucceeds(expectedSavedDraft, expectedUserId)

        // When
        val actualEither = storeDraftWithSender(draftMessageId, senderAddress, expectedUserId)

        // Then
        coVerify { saveDraftMock(expectedSavedDraft, expectedUserId) }
        assertEquals(Unit.right(), actualEither)
    }

    @Test
    fun `should return error when saving draft fails`() = runTest {
        // Given
        val senderAddress = UserAddressSample.AliasAddress
        val expectedUserId = UserIdSample.Primary
        val draftMessageId = MessageIdSample.build()
        val existingDraft = expectedExistingDraft(expectedUserId, draftMessageId) { MessageWithBodySample.EmptyDraft }
        val expectedSavedDraft = existingDraft.copy(
            message = existingDraft.message.copy(
                sender = Sender(senderAddress.email, senderAddress.displayName!!),
                addressId = senderAddress.addressId
            )
        )
        givenSaveDraftFails(expectedSavedDraft, expectedUserId)

        // When
        val actualEither = storeDraftWithSender(draftMessageId, senderAddress, expectedUserId)

        // Then
        assertEquals(StoreDraftWithSender.Error.DraftSaveError.left(), actualEither)
    }

    private fun expectedExistingDraft(
        userId: UserId,
        messageId: MessageId,
        existingDraft: () -> MessageWithBody
    ): MessageWithBody = existingDraft().also {
        coEvery { messageRepositoryMock.getLocalMessageWithBody(userId, messageId) } returns it
    }

    private fun expectedNewDraft(
        userId: UserId,
        messageId: MessageId,
        senderAddress: UserAddress,
        existingDraft: () -> MessageWithBody
    ): MessageWithBody = existingDraft().also {
        coEvery { messageRepositoryMock.getLocalMessageWithBody(userId, messageId) } returns null
        every { createEmptyDraftMock(messageId, userId, senderAddress) } returns it
    }

    private fun givenSaveDraftSucceeds(messageWithBody: MessageWithBody, userId: UserId) {
        coEvery { saveDraftMock(messageWithBody, userId) } returns true
    }

    private fun givenSaveDraftFails(messageWithBody: MessageWithBody, userId: UserId) {
        coEvery { saveDraftMock(messageWithBody, userId) } returns false
    }
}
