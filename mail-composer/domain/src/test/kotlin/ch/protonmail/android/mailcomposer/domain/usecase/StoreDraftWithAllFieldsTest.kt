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
import ch.protonmail.android.mailcomposer.domain.model.DraftAction
import ch.protonmail.android.mailcomposer.domain.model.DraftBody
import ch.protonmail.android.mailcomposer.domain.model.DraftFields
import ch.protonmail.android.mailcomposer.domain.model.RecipientsBcc
import ch.protonmail.android.mailcomposer.domain.model.RecipientsCc
import ch.protonmail.android.mailcomposer.domain.model.RecipientsTo
import ch.protonmail.android.mailcomposer.domain.model.SenderEmail
import ch.protonmail.android.mailcomposer.domain.model.Subject
import ch.protonmail.android.mailcomposer.domain.repository.DraftStateRepository
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.mailmessage.domain.sample.RecipientSample
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

    private val draftStateRepository = mockk<DraftStateRepository>()
    private val storeDraftWithSubjectMock = mockk<StoreDraftWithSubject>()
    private val storeDraftWithBodyMock = mockk<StoreDraftWithBody>()
    private val storeDraftWithRecipientsMock = mockk<StoreDraftWithRecipients>()

    private val storeDraftWithAllFields = StoreDraftWithAllFields(
        draftStateRepository,
        storeDraftWithSubjectMock,
        storeDraftWithBodyMock,
        storeDraftWithRecipientsMock
    )

    @Test
    fun `saves draft with subject body and sender`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val draftMessageId = MessageIdSample.build()
        val senderEmail = SenderEmail(UserAddressSample.PrimaryAddress.email)
        val subject = Subject("Subject of this email")
        val plaintextDraftBody = DraftBody("I am plaintext")
        val recipientsTo = RecipientsTo(listOf(RecipientSample.John))
        val recipientsCc = RecipientsCc(listOf(RecipientSample.John))
        val recipientsBcc = RecipientsBcc(listOf(RecipientSample.John))
        val draftFields = DraftFields(
            senderEmail,
            subject,
            plaintextDraftBody,
            recipientsTo,
            recipientsCc,
            recipientsBcc
        )
        expectStoreDraftBodySucceeds(draftMessageId, plaintextDraftBody, senderEmail, userId)
        expectStoreDraftSubjectSucceeds(userId, draftMessageId, senderEmail, subject)
        expectStoreDraftRecipientsSucceeds(
            userId,
            draftMessageId,
            senderEmail,
            Triple(recipientsTo, recipientsCc, recipientsBcc)
        )
        expectStoreDraftStateSucceeds(userId, draftMessageId)

        // When
        storeDraftWithAllFields(userId, draftMessageId, draftFields)

        // Then
        coVerify { storeDraftWithBodyMock(draftMessageId, plaintextDraftBody, senderEmail, userId) }
        coVerify { storeDraftWithSubjectMock(userId, draftMessageId, senderEmail, subject) }
        coVerify {
            storeDraftWithRecipientsMock(
                userId,
                draftMessageId,
                senderEmail,
                recipientsTo.value,
                recipientsCc.value,
                recipientsBcc.value
            )
        }
    }

    @Test
    fun `logs error when store draft with body fails`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val draftMessageId = MessageIdSample.build()
        val senderEmail = SenderEmail(UserAddressSample.PrimaryAddress.email)
        val subject = Subject("Subject of this email")
        val plaintextDraftBody = DraftBody("I am plaintext")
        val recipientsTo = RecipientsTo(listOf(RecipientSample.John))
        val recipientsCc = RecipientsCc(listOf(RecipientSample.John))
        val recipientsBcc = RecipientsBcc(listOf(RecipientSample.John))
        val draftFields = buildDraftFields(
            senderEmail,
            subject,
            plaintextDraftBody,
            recipientsTo,
            recipientsCc,
            recipientsBcc
        )
        val expectedError = StoreDraftWithBodyError.DraftReadError
        expectStoreDraftBodyFails(draftMessageId, plaintextDraftBody, senderEmail, userId) { expectedError }
        expectStoreDraftSubjectSucceeds(userId, draftMessageId, senderEmail, subject)
        expectStoreDraftRecipientsSucceeds(
            userId,
            draftMessageId,
            senderEmail,
            Triple(recipientsTo, recipientsCc, recipientsBcc)
        )
        expectStoreDraftStateSucceeds(userId, draftMessageId)

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
        val recipientsTo = RecipientsTo(listOf(RecipientSample.John))
        val recipientsCc = RecipientsCc(listOf(RecipientSample.John))
        val recipientsBcc = RecipientsBcc(listOf(RecipientSample.John))
        val draftFields = buildDraftFields(
            senderEmail,
            subject,
            plaintextDraftBody,
            recipientsTo,
            recipientsCc,
            recipientsBcc
        )
        val expectedError = StoreDraftWithSubject.Error.DraftSaveError
        expectStoreDraftBodySucceeds(draftMessageId, plaintextDraftBody, senderEmail, userId)
        expectStoreDraftSubjectFails(draftMessageId, senderEmail, userId, subject) { expectedError }
        expectStoreDraftStateSucceeds(userId, draftMessageId)
        expectStoreDraftRecipientsSucceeds(
            userId,
            draftMessageId,
            senderEmail,
            Triple(recipientsTo, recipientsCc, recipientsBcc)
        )

        // When
        storeDraftWithAllFields(userId, draftMessageId, draftFields)

        // Then
        val expectedLog = "Storing all draft fields failed due to $expectedError. \n Draft MessageId = $draftMessageId"
        loggingTestRule.assertErrorLogged(expectedLog)
    }

    @Test
    fun `logs error when store draft with recipients fails`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val draftMessageId = MessageIdSample.build()
        val senderEmail = SenderEmail(UserAddressSample.PrimaryAddress.email)
        val subject = Subject("Subject of this email")
        val plaintextDraftBody = DraftBody("I am plaintext")
        val recipientsTo = RecipientsTo(listOf(RecipientSample.John))
        val recipientsCc = RecipientsCc(listOf(RecipientSample.John))
        val recipientsBcc = RecipientsBcc(listOf(RecipientSample.John))
        val draftFields = buildDraftFields(
            senderEmail,
            subject,
            plaintextDraftBody,
            recipientsTo,
            recipientsCc,
            recipientsBcc
        )
        val expectedError = StoreDraftWithRecipients.Error.DraftSaveError
        expectStoreDraftBodySucceeds(draftMessageId, plaintextDraftBody, senderEmail, userId)
        expectStoreDraftSubjectSucceeds(userId, draftMessageId, senderEmail, subject)
        expectStoreDraftStateSucceeds(userId, draftMessageId)
        expectStoreDraftRecipientsFails(
            draftMessageId,
            senderEmail,
            userId,
            Triple(recipientsTo, recipientsCc, recipientsBcc)
        ) { expectedError }

        // When
        storeDraftWithAllFields(userId, draftMessageId, draftFields)

        // Then
        val expectedLog = "Storing all draft fields failed due to $expectedError. \n Draft MessageId = $draftMessageId"
        loggingTestRule.assertErrorLogged(expectedLog)
    }

    @Test
    fun `store draft state as Local when draft data is saved locally`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val draftMessageId = MessageIdSample.build()
        val senderEmail = SenderEmail(UserAddressSample.PrimaryAddress.email)
        val subject = Subject("Subject of this email")
        val plaintextDraftBody = DraftBody("I am plaintext")
        val draftFields = buildDraftFields(senderEmail, subject, plaintextDraftBody)
        val expectedAction = DraftAction.Compose
        expectStoreDraftBodySucceeds(draftMessageId, plaintextDraftBody, senderEmail, userId)
        expectStoreDraftSubjectSucceeds(userId, draftMessageId, senderEmail, subject)
        expectStoreDraftStateSucceeds(userId, draftMessageId, expectedAction)
        expectStoreDraftRecipientsSucceeds(
            userId,
            draftMessageId,
            senderEmail,
            Triple(RecipientsTo(emptyList()), RecipientsCc(emptyList()), RecipientsBcc(emptyList()))
        )

        // When
        storeDraftWithAllFields(userId, draftMessageId, draftFields)

        // Then
        coVerify { draftStateRepository.saveLocalState(userId, draftMessageId, expectedAction) }
    }

    private fun buildDraftFields(
        senderEmail: SenderEmail,
        subject: Subject,
        plaintextDraftBody: DraftBody,
        recipientsTo: RecipientsTo = RecipientsTo(emptyList()),
        recipientsCc: RecipientsCc = RecipientsCc(emptyList()),
        recipientsBcc: RecipientsBcc = RecipientsBcc(emptyList())
    ) = DraftFields(
        senderEmail,
        subject,
        plaintextDraftBody,
        recipientsTo,
        recipientsCc,
        recipientsBcc
    )

    private fun expectStoreDraftStateSucceeds(
        userId: UserId,
        draftMessageId: MessageId,
        expectedAction: DraftAction = DraftAction.Compose
    ) {
        coEvery { draftStateRepository.saveLocalState(userId, draftMessageId, expectedAction) } returns Unit.right()
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

    private fun expectStoreDraftRecipientsSucceeds(
        expectedUserId: UserId,
        expectedMessageId: MessageId,
        expectedSenderEmail: SenderEmail,
        recipients: Triple<RecipientsTo, RecipientsCc, RecipientsBcc>
    ) {
        coEvery {
            storeDraftWithRecipientsMock(
                expectedUserId,
                expectedMessageId,
                expectedSenderEmail,
                recipients.first.value,
                recipients.second.value,
                recipients.third.value
            )
        } returns Unit.right()
    }

    private fun expectStoreDraftRecipientsFails(
        expectedMessageId: MessageId,
        expectedSenderEmail: SenderEmail,
        expectedUserId: UserId,
        recipients: Triple<RecipientsTo, RecipientsCc, RecipientsBcc>,
        error: () -> StoreDraftWithRecipients.Error
    ) = error().also {
        coEvery {
            storeDraftWithRecipientsMock(
                expectedUserId,
                expectedMessageId,
                expectedSenderEmail,
                recipients.first.value,
                recipients.second.value,
                recipients.third.value
            )
        } returns it.left()
    }

}
