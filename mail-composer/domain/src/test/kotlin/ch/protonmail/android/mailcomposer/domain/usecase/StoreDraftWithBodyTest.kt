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
import ch.protonmail.android.mailcomposer.domain.model.SenderEmail
import ch.protonmail.android.mailmessage.domain.model.MessageWithBody
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.mailmessage.domain.sample.MessageWithBodySample
import ch.protonmail.android.test.utils.FakeTransactor
import ch.protonmail.android.test.utils.rule.LoggingTestRule
import io.mockk.called
import io.mockk.coEvery
import io.mockk.coVerifySequence
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals

internal class StoreDraftWithBodyTest {

    @get:Rule
    val loggingTestRule = LoggingTestRule()

    private val saveDraftMock = mockk<SaveDraft>()
    private val prepareAndEncryptDraftBody = mockk<PrepareAndEncryptDraftBody>()
    private val fakeTransactor = FakeTransactor()

    private val storeDraftWithBody = StoreDraftWithBody(
        prepareAndEncryptDraftBody,
        saveDraftMock,
        fakeTransactor
    )

    @Test
    fun `should save a draft with the proper message and user Id`() = runTest {
        // Given
        val plaintextDraftBody = DraftBody("I am plaintext")
        val senderAddress = UserAddressSample.build()
        val senderEmail = SenderEmail(senderAddress.email)
        val expectedUserId = UserIdSample.Primary
        val draftMessageId = MessageIdSample.build()
        val expectedDraft = expectedMessageWithBody(MessageWithBodySample.NewDraftWithSubject)

        givenSaveDraftSucceeds(expectedDraft, expectedUserId)

        // When
        val actualEither = storeDraftWithBody(
            expectedUserId, draftMessageId, plaintextDraftBody, NoQuotedHtmlBody, senderEmail
        )

        // Then
        coVerifySequence {
            prepareAndEncryptDraftBody(
                expectedUserId,
                draftMessageId,
                plaintextDraftBody,
                NoQuotedHtmlBody,
                senderEmail
            )
            saveDraftMock(expectedDraft, expectedUserId)
        }
        assertEquals(Unit.right(), actualEither)
    }

    @Test
    fun `should return error when saving fails`() = runTest {
        // Given
        val plaintextDraftBody = DraftBody("I am plaintext")
        val senderAddress = UserAddressSample.build()
        val senderEmail = SenderEmail(senderAddress.email)
        val expectedUserId = UserIdSample.Primary
        val draftMessageId = MessageIdSample.build()
        val expectedDraft = expectedMessageWithBody(MessageWithBodySample.NewDraftWithSubject)

        givenSaveDraftFails(expectedDraft, expectedUserId)

        // When
        val actualEither = storeDraftWithBody(
            expectedUserId, draftMessageId, plaintextDraftBody, NoQuotedHtmlBody, senderEmail
        )

        // Then
        assertEquals(StoreDraftWithBodyError.DraftSaveError.left(), actualEither)
        coVerifySequence {
            prepareAndEncryptDraftBody(
                expectedUserId,
                draftMessageId,
                plaintextDraftBody,
                NoQuotedHtmlBody,
                senderEmail
            )
            saveDraftMock(expectedDraft, expectedUserId)
        }
        loggingTestRule.assertErrorLogged("Store draft $draftMessageId body to local DB failed")
    }

    @Test
    fun `should return error when preparing the draft body fails`() = runTest {
        // Given
        val plaintextDraftBody = DraftBody("I am plaintext")
        val senderAddress = UserAddressSample.build()
        val senderEmail = SenderEmail(senderAddress.email)
        val expectedUserId = UserIdSample.Primary
        val draftMessageId = MessageIdSample.build()

        expectBodyPreparationFailure()

        // When
        val actualEither = storeDraftWithBody(
            expectedUserId, draftMessageId, plaintextDraftBody, NoQuotedHtmlBody, senderEmail
        )

        // Then
        assertEquals(StoreDraftWithBodyError.DraftSaveError.left(), actualEither)
        coVerifySequence {
            prepareAndEncryptDraftBody(
                expectedUserId,
                draftMessageId,
                plaintextDraftBody,
                NoQuotedHtmlBody,
                senderEmail
            )
            saveDraftMock wasNot called
        }
        loggingTestRule.assertErrorLogged("Prepare encrypted $draftMessageId body failed")
    }

    private fun expectedMessageWithBody(localDraft: MessageWithBody): MessageWithBody {
        coEvery {
            prepareAndEncryptDraftBody(any(), any(), any(), any(), any())
        } returns localDraft.right()

        return localDraft
    }

    private fun expectBodyPreparationFailure() {
        coEvery {
            prepareAndEncryptDraftBody(any(), any(), any(), any(), any())
        } returns PrepareDraftBodyError.DraftBodyEncryptionError.left()
    }

    private fun givenSaveDraftSucceeds(messageWithBody: MessageWithBody, userId: UserId) {
        coEvery { saveDraftMock(messageWithBody, userId) } returns true
    }

    private fun givenSaveDraftFails(messageWithBody: MessageWithBody, userId: UserId) {
        coEvery { saveDraftMock(messageWithBody, userId) } returns false
    }

    companion object {

        private val NoQuotedHtmlBody = null
    }
}
