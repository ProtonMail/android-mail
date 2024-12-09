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
import ch.protonmail.android.mailcomposer.domain.model.OriginalHtmlQuote
import ch.protonmail.android.mailcomposer.domain.model.RecipientsBcc
import ch.protonmail.android.mailcomposer.domain.model.RecipientsCc
import ch.protonmail.android.mailcomposer.domain.model.RecipientsTo
import ch.protonmail.android.mailcomposer.domain.model.SenderEmail
import ch.protonmail.android.mailcomposer.domain.model.Subject
import ch.protonmail.android.mailmessage.domain.model.DraftAction
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.MessageWithBody
import ch.protonmail.android.mailmessage.domain.repository.DraftStateRepository
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.mailmessage.domain.sample.MessageWithBodySample
import ch.protonmail.android.mailmessage.domain.sample.RecipientSample
import ch.protonmail.android.test.utils.FakeTransactor
import ch.protonmail.android.test.utils.rule.LoggingTestRule
import io.mockk.called
import io.mockk.coEvery
import io.mockk.coVerifySequence
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertTrue

class StoreDraftWithAllFieldsTest {

    @get:Rule
    val loggingTestRule = LoggingTestRule()

    private val draftStateRepository = mockk<DraftStateRepository>()
    private val prepareAndEncryptDraftBody = mockk<PrepareAndEncryptDraftBody>()
    private val saveDraft = mockk<SaveDraft>()
    private val fakeTransactor = FakeTransactor()

    private val storeDraftWithAllFields = StoreDraftWithAllFields(
        draftStateRepository,
        prepareAndEncryptDraftBody,
        saveDraft,
        fakeTransactor
    )

    @Test
    fun `saves draft with all fields`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val draftMessageId = MessageIdSample.build()
        val senderEmail = SenderEmail(UserAddressSample.PrimaryAddress.email)
        val subject = Subject("Subject of this email")
        val plaintextDraftBody = DraftBody("I am plaintext")
        val quotedHtmlBody = OriginalHtmlQuote("<div>Input quoted html body</div>")
        val recipientsTo = RecipientsTo(listOf(RecipientSample.John))
        val recipientsCc = RecipientsCc(listOf(RecipientSample.Doe))
        val recipientsBcc = RecipientsBcc(listOf(RecipientSample.Bob))
        val expectedAction = DraftAction.Compose
        val draftFields = DraftFields(
            senderEmail,
            subject,
            plaintextDraftBody,
            recipientsTo,
            recipientsCc,
            recipientsBcc,
            quotedHtmlBody
        )
        val draftBody = expectDraftBodyWithText(userId, draftMessageId, plaintextDraftBody, quotedHtmlBody, senderEmail)
        val expectedDraftUpdated = draftBody.copy(
            message = draftBody.message.copy(
                subject = draftFields.subject.value,
                toList = draftFields.recipientsTo.value,
                ccList = draftFields.recipientsCc.value,
                bccList = draftFields.recipientsBcc.value
            )
        )
        expectStoreDraftStateSucceeds(userId, draftMessageId)
        expectSaveDraftSuccess(userId, expectedDraftUpdated)

        // When
        val result = storeDraftWithAllFields(userId, draftMessageId, draftFields)

        // Then
        assertTrue(result.isRight())
        coVerifySequence {
            prepareAndEncryptDraftBody(userId, draftMessageId, plaintextDraftBody, quotedHtmlBody, senderEmail)
            saveDraft(expectedDraftUpdated, userId)
            draftStateRepository.createOrUpdateLocalState(userId, draftMessageId, expectedAction)
        }
    }

    @Test
    fun `should error when prepare draft body fails`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val draftMessageId = MessageIdSample.build()
        val senderEmail = SenderEmail(UserAddressSample.PrimaryAddress.email)
        val subject = Subject("Subject of this email")
        val plaintextDraftBody = DraftBody("I am plaintext")
        val recipientsTo = RecipientsTo(listOf(RecipientSample.John))
        val recipientsCc = RecipientsCc(listOf(RecipientSample.Doe))
        val recipientsBcc = RecipientsBcc(listOf(RecipientSample.Bob))
        val draftFields = buildDraftFields(
            senderEmail,
            subject,
            plaintextDraftBody,
            recipientsTo,
            recipientsCc,
            recipientsBcc
        )
        expectDraftBodyError(userId, draftMessageId, plaintextDraftBody, null, senderEmail)

        // When
        val result = storeDraftWithAllFields(userId, draftMessageId, draftFields)

        // Then
        assertTrue(result.isLeft())

        coVerifySequence {
            prepareAndEncryptDraftBody(userId, draftMessageId, plaintextDraftBody, null, senderEmail)
            saveDraft wasNot called
            draftStateRepository wasNot called
        }
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
        recipientsBcc,
        null
    )

    private fun expectStoreDraftStateSucceeds(
        userId: UserId,
        draftMessageId: MessageId,
        expectedAction: DraftAction = DraftAction.Compose
    ) {
        coEvery {
            draftStateRepository.createOrUpdateLocalState(userId, draftMessageId, expectedAction)
        } returns Unit.right()
    }

    private fun expectDraftBodyWithText(
        userId: UserId,
        messageId: MessageId,
        draftBody: DraftBody,
        originalHtmlQuote: OriginalHtmlQuote?,
        senderEmail: SenderEmail
    ) = MessageWithBodySample.EmptyDraft.also {
        coEvery { prepareAndEncryptDraftBody(userId, messageId, draftBody, originalHtmlQuote, senderEmail) } returns
            it.right()
    }

    private fun expectDraftBodyError(
        userId: UserId,
        messageId: MessageId,
        draftBody: DraftBody,
        originalHtmlQuote: OriginalHtmlQuote?,
        senderEmail: SenderEmail
    ) {
        coEvery { prepareAndEncryptDraftBody(userId, messageId, draftBody, originalHtmlQuote, senderEmail) } returns
            PrepareDraftBodyError.DraftReadError.left()
    }

    private fun expectSaveDraftSuccess(userId: UserId, messageWithBody: MessageWithBody) {
        coEvery { saveDraft(messageWithBody, userId) } returns true
    }
}
