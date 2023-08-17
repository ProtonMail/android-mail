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
import ch.protonmail.android.mailcomposer.domain.model.SenderEmail
import ch.protonmail.android.mailcomposer.domain.model.Subject
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.MessageWithBody
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.mailmessage.domain.sample.MessageWithBodySample
import ch.protonmail.android.test.utils.FakeTransactor
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import org.junit.Test
import kotlin.test.assertEquals

class StoreDraftWithSubjectTest {

    private val saveDraftMock = mockk<SaveDraft>()
    private val getLocalDraftMock = mockk<GetLocalDraft>()
    private val fakeTransactor = FakeTransactor()

    private val storeDraftWithSubject = StoreDraftWithSubject(
        getLocalDraftMock,
        saveDraftMock,
        fakeTransactor
    )

    @Test
    fun `save draft with subject`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val draftMessageId = MessageIdSample.build()
        val senderEmail = SenderEmail(UserAddressSample.PrimaryAddress.email)
        val subject = Subject("Subject of this email")
        val draftWithBody = expectedGetLocalDraft(userId, draftMessageId, senderEmail) {
            MessageWithBodySample.EmptyDraft
        }
        val expectedSavedDraft = draftWithBody.copy(message = draftWithBody.message.copy(subject = subject.value))
        givenSaveDraftSucceeds(expectedSavedDraft, userId)

        // When
        val actualEither = storeDraftWithSubject(userId, draftMessageId, senderEmail, subject)

        // Then
        coVerify { saveDraftMock(expectedSavedDraft, userId) }
        assertEquals(Unit.right(), actualEither)
    }

    @Test
    fun `returns error when get local draft fails`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val draftMessageId = MessageIdSample.build()
        val senderEmail = SenderEmail(UserAddressSample.PrimaryAddress.email)
        val subject = Subject("Subject of this email")
        expectedGetLocalDraftFails(userId, draftMessageId, senderEmail) {
            GetLocalDraft.Error.ResolveUserAddressError
        }

        // When
        val actualEither = storeDraftWithSubject(userId, draftMessageId, senderEmail, subject)

        // Then
        coVerify { saveDraftMock wasNot Called }
        assertEquals(StoreDraftWithSubject.Error.DraftReadError.left(), actualEither)
    }

    @Test
    fun `returns error when save draft fails`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val draftMessageId = MessageIdSample.build()
        val senderEmail = SenderEmail(UserAddressSample.PrimaryAddress.email)
        val subject = Subject("Subject of this email")
        val draftWithBody = expectedGetLocalDraft(userId, draftMessageId, senderEmail) {
            MessageWithBodySample.EmptyDraft
        }
        val expectedSavedDraft = draftWithBody.copy(message = draftWithBody.message.copy(subject = subject.value))
        givenSaveDraftFails(expectedSavedDraft, userId)

        // When
        val actualEither = storeDraftWithSubject(userId, draftMessageId, senderEmail, subject)

        // Then
        assertEquals(StoreDraftWithSubject.Error.DraftSaveError.left(), actualEither)
    }

    private fun expectedGetLocalDraft(
        userId: UserId,
        messageId: MessageId,
        senderEmail: SenderEmail,
        localDraft: () -> MessageWithBody
    ): MessageWithBody = localDraft().also {
        coEvery { getLocalDraftMock.invoke(userId, messageId, senderEmail) } returns it.right()
    }

    private fun expectedGetLocalDraftFails(
        userId: UserId,
        messageId: MessageId,
        senderEmail: SenderEmail,
        error: () -> GetLocalDraft.Error
    ): GetLocalDraft.Error = error().also {
        coEvery { getLocalDraftMock.invoke(userId, messageId, senderEmail) } returns it.left()
    }

    private fun givenSaveDraftSucceeds(messageWithBody: MessageWithBody, userId: UserId) {
        coEvery { saveDraftMock(messageWithBody, userId) } returns true
    }

    private fun givenSaveDraftFails(messageWithBody: MessageWithBody, userId: UserId) {
        coEvery { saveDraftMock(messageWithBody, userId) } returns false
    }
}
