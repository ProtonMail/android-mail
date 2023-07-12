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
import ch.protonmail.android.mailcomposer.domain.model.DraftBody
import ch.protonmail.android.mailcomposer.domain.model.DraftFields
import ch.protonmail.android.mailcomposer.domain.model.SenderEmail
import ch.protonmail.android.mailcomposer.domain.model.Subject
import ch.protonmail.android.mailmessage.domain.entity.MessageId
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.test.utils.rule.LoggingTestRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import org.junit.Rule
import org.junit.Test

class StoreDraftWithAllFieldsTest {

    @get:Rule
    val loggingTestRule = LoggingTestRule()

    private val storeDraftWithSubjectMock = mockk<StoreDraftWithSubject>()
    private val storeDraftWithBodyMock = mockk<StoreDraftWithBody>()

    private val storeDraftWithAllFields = StoreDraftWithAllFields(
        storeDraftWithSubjectMock,
        storeDraftWithBodyMock
    )

    @Test
    fun `saves draft with subject body and sender`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val draftMessageId = MessageIdSample.build()
        val senderEmail = SenderEmail(UserAddressSample.PrimaryAddress.email)
        val subject = Subject("Subject of this email")
        val plaintextDraftBody = DraftBody("I am plaintext")
        val draftFields = DraftFields(senderEmail, subject, plaintextDraftBody)
        expectStoreDraftBodySucceeds(draftMessageId, plaintextDraftBody, senderEmail, userId)
        expectStoreDraftSubjectSucceeds(userId, draftMessageId, senderEmail, subject)

        // When
        storeDraftWithAllFields(userId, draftMessageId, draftFields)

        // Then
        coVerify { storeDraftWithBodyMock(draftMessageId, plaintextDraftBody, senderEmail, userId) }
        coVerify { storeDraftWithSubjectMock(userId, draftMessageId, senderEmail, subject) }
    }

    @Test
    fun `logs error when store draft with body fails`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val draftMessageId = MessageIdSample.build()
        val senderEmail = SenderEmail(UserAddressSample.PrimaryAddress.email)
        val subject = Subject("Subject of this email")
        val plaintextDraftBody = DraftBody("I am plaintext")
        val draftFields = DraftFields(senderEmail, subject, plaintextDraftBody)
        val expectedError = StoreDraftWithBodyError.DraftReadError
        expectStoreDraftBodyFails(draftMessageId, plaintextDraftBody, senderEmail, userId) { expectedError }
        expectStoreDraftSubjectSucceeds(userId, draftMessageId, senderEmail, subject)

        // When
        storeDraftWithAllFields(userId, draftMessageId, draftFields)

        // Then
        val expectedLog = "Storing all draft fields failed due to $expectedError. \n Draft MessageId = $draftMessageId"
        loggingTestRule.assertErrorLogged(expectedLog)
    }

    @Test
    fun `logs error when store draft with subject fails`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val draftMessageId = MessageIdSample.build()
        val senderEmail = SenderEmail(UserAddressSample.PrimaryAddress.email)
        val subject = Subject("Subject of this email")
        val plaintextDraftBody = DraftBody("I am plaintext")
        val draftFields = DraftFields(senderEmail, subject, plaintextDraftBody)
        val expectedError = StoreDraftWithSubject.Error.DraftSaveError
        expectStoreDraftBodySucceeds(draftMessageId, plaintextDraftBody, senderEmail, userId)
        expectStoreDraftSubjectFails(draftMessageId, senderEmail, userId, subject) { expectedError }

        // When
        storeDraftWithAllFields(userId, draftMessageId, draftFields)

        // Then
        val expectedLog = "Storing all draft fields failed due to $expectedError. \n Draft MessageId = $draftMessageId"
        loggingTestRule.assertErrorLogged(expectedLog)
    }

    private fun expectStoreDraftBodySucceeds(
        expectedMessageId: MessageId,
        expectedDraftBody: DraftBody,
        expectedSenderEmail: SenderEmail,
        expectedUserId: UserId
    ) {
        coEvery {
            storeDraftWithBodyMock(
                expectedMessageId,
                expectedDraftBody,
                expectedSenderEmail,
                expectedUserId
            )
        } returns Unit.right()
    }

    private fun expectStoreDraftBodyFails(
        expectedMessageId: MessageId,
        expectedDraftBody: DraftBody,
        expectedSenderEmail: SenderEmail,
        expectedUserId: UserId,
        error: () -> StoreDraftWithBodyError
    ) = error().also {
        coEvery {
            storeDraftWithBodyMock(
                expectedMessageId,
                expectedDraftBody,
                expectedSenderEmail,
                expectedUserId
            )
        } returns it.left()
    }

    private fun expectStoreDraftSubjectSucceeds(
        expectedUserId: UserId,
        expectedMessageId: MessageId,
        expectedSenderEmail: SenderEmail,
        expectedSubject: Subject
    ) {
        coEvery {
            storeDraftWithSubjectMock(
                expectedUserId,
                expectedMessageId,
                expectedSenderEmail,
                expectedSubject
            )
        } returns Unit.right()
    }

    private fun expectStoreDraftSubjectFails(
        expectedMessageId: MessageId,
        expectedSenderEmail: SenderEmail,
        expectedUserId: UserId,
        expectedSubject: Subject,
        error: () -> StoreDraftWithSubject.Error
    ) = error().also {
        coEvery {
            storeDraftWithSubjectMock(
                expectedUserId,
                expectedMessageId,
                expectedSenderEmail,
                expectedSubject
            )
        } returns it.left()
    }

}
