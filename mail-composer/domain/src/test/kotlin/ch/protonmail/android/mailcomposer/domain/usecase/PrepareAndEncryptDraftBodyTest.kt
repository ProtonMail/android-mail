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
import ch.protonmail.android.mailcommon.domain.usecase.ResolveUserAddress
import ch.protonmail.android.mailcomposer.domain.model.DraftBody
import ch.protonmail.android.mailcomposer.domain.model.OriginalHtmlQuote
import ch.protonmail.android.mailcomposer.domain.model.SenderEmail
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.MessageWithBody
import ch.protonmail.android.mailmessage.domain.model.MimeType
import ch.protonmail.android.mailmessage.domain.model.Sender
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.mailmessage.domain.sample.MessageWithBodySample
import ch.protonmail.android.mailmessage.domain.usecase.ConvertPlainTextIntoHtml
import ch.protonmail.android.test.utils.rule.LoggingTestRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import me.proton.core.user.domain.entity.UserAddress
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals

internal class PrepareAndEncryptDraftBodyTest {

    @get:Rule
    val loggingTestRule = LoggingTestRule()

    private val encryptDraftBodyMock = mockk<EncryptDraftBody>()
    private val getLocalDraftMock = mockk<GetLocalDraft>()
    private val resolveUserAddressMock = mockk<ResolveUserAddress>()
    private val convertPlainTextIntoHtml = mockk<ConvertPlainTextIntoHtml>()

    private val prepareAndEncryptDraftBody = PrepareAndEncryptDraftBody(
        getLocalDraftMock,
        resolveUserAddressMock,
        convertPlainTextIntoHtml,
        encryptDraftBodyMock
    )

    @Test
    fun `should prepare a draft with encrypted body and sender details`() = runTest {
        // Given
        val plaintextDraftBody = DraftBody("I am plaintext")
        val senderAddress = UserAddressSample.build()
        val senderEmail = SenderEmail(senderAddress.email)
        val expectedUserId = UserIdSample.Primary
        val draftMessageId = MessageIdSample.build()
        val existingDraft = expectedGetLocalDraft(expectedUserId, draftMessageId, senderEmail) {
            MessageWithBodySample.EmptyDraft
        }
        val expectedEncryptedDraftBody = expectedEncryptedDraftBody(plaintextDraftBody, senderAddress) {
            DraftBody("I am encrypted")
        }
        val expectedUpdatedDraft = existingDraft.copy(
            message = existingDraft.message.copy(
                sender = Sender(senderAddress.email, senderAddress.displayName!!),
                addressId = senderAddress.addressId
            ),
            messageBody = existingDraft.messageBody.copy(
                body = expectedEncryptedDraftBody.value
            )
        )
        expectedResolvedUserAddress(expectedUserId, senderEmail) { senderAddress }

        // When
        val actualEither = prepareAndEncryptDraftBody(
            expectedUserId, draftMessageId, plaintextDraftBody, NoQuotedHtmlBody, senderEmail
        )

        // Then
        assertEquals(expectedUpdatedDraft, actualEither.getOrNull())
    }

    @Test
    fun `should wrap draft text in 'pre', append quoted html body and set mime type to html when any quote exists`() =
        runTest {
            // Given
            val plaintextDraftBody = DraftBody("I am plaintext")
            val quotedHtmlBody = OriginalHtmlQuote("<div> I am quoted html </div>")
            val htmlDraftBody = expectedConvertedText(plaintextDraftBody.value, plaintextDraftBody.value)
            val expectedMergedBody = DraftBody("$htmlDraftBody${quotedHtmlBody.value}")
            val senderAddress = UserAddressSample.build()
            val senderEmail = SenderEmail(senderAddress.email)
            val expectedUserId = UserIdSample.Primary
            val draftMessageId = MessageIdSample.build()
            val existingDraft = expectedGetLocalDraft(expectedUserId, draftMessageId, senderEmail) {
                MessageWithBodySample.EmptyDraft
            }
            val expectedEncryptedDraftBody = expectedEncryptedDraftBody(expectedMergedBody, senderAddress) {
                DraftBody("I am encrypted with the quoted html included")
            }
            val expectedUpdatedDraft = existingDraft.copy(
                message = existingDraft.message.copy(
                    sender = Sender(senderAddress.email, senderAddress.displayName!!),
                    addressId = senderAddress.addressId
                ),
                messageBody = existingDraft.messageBody.copy(
                    body = expectedEncryptedDraftBody.value,
                    mimeType = MimeType.Html
                )
            )
            expectedResolvedUserAddress(expectedUserId, senderEmail) { senderAddress }

            // When
            val actualEither = prepareAndEncryptDraftBody(
                expectedUserId, draftMessageId, plaintextDraftBody, quotedHtmlBody, senderEmail
            )

            // Then
            assertEquals(expectedUpdatedDraft, actualEither.getOrNull())
        }

    @Test
    fun `should return error and not save draft when encryption fails`() = runTest {
        // Given
        val plaintextDraftBody = DraftBody("I am plaintext")
        val senderAddress = UserAddressSample.build()
        val senderEmail = SenderEmail(senderAddress.email)
        val userId = UserIdSample.Primary
        val draftMessageId = MessageIdSample.build()
        expectedGetLocalDraft(userId, draftMessageId, senderEmail) { MessageWithBodySample.EmptyDraft }
        expectedResolvedUserAddress(userId, senderEmail) { senderAddress }
        givenDraftBodyEncryptionFails()

        // When
        val actualEither = prepareAndEncryptDraftBody(
            userId, draftMessageId, plaintextDraftBody, NoQuotedHtmlBody, senderEmail
        )

        // Then
        assertEquals(PrepareDraftBodyError.DraftBodyEncryptionError.left(), actualEither)
        loggingTestRule.assertErrorLogged("Encrypt draft $draftMessageId body to store to local DB failed")
    }

    @Test
    fun `should return error when resolving the draft sender address fails`() = runTest {
        // Given
        val expectedSenderEmail = SenderEmail("unresolvable@sender.email")
        val expectedUserId = UserIdSample.Primary
        val expectedDraftBody = DraftBody("I am plaintext")
        val draftMessageId = MessageIdSample.build()
        expectedGetLocalDraft(expectedUserId, draftMessageId, expectedSenderEmail) { MessageWithBodySample.EmptyDraft }
        expectResolveUserAddressFailure(expectedUserId, expectedSenderEmail)

        // When
        val actualEither = prepareAndEncryptDraftBody(
            expectedUserId, draftMessageId, expectedDraftBody, NoQuotedHtmlBody, expectedSenderEmail
        )

        // Then
        assertEquals(PrepareDraftBodyError.DraftResolveUserAddressError.left(), actualEither)
    }

    @Test
    fun `should return error when reading the local draft fails`() = runTest {
        // Given
        val expectedSenderEmail = SenderEmail("unresolvable@sender.email")
        val expectedUserId = UserIdSample.Primary
        val expectedDraftBody = DraftBody("I am plaintext")
        val draftMessageId = MessageIdSample.build()
        expectedResolvedUserAddress(expectedUserId, expectedSenderEmail) { UserAddressSample.PrimaryAddress }
        expectedGetLocalDraftFails(expectedUserId, draftMessageId, expectedSenderEmail) {
            GetLocalDraft.Error.ResolveUserAddressError
        }

        // When
        val actualEither = prepareAndEncryptDraftBody(
            expectedUserId, draftMessageId, expectedDraftBody, NoQuotedHtmlBody, expectedSenderEmail
        )

        // Then
        assertEquals(PrepareDraftBodyError.DraftReadError.left(), actualEither)
    }

    private fun expectedConvertedText(original: String, expected: String): String {
        every { convertPlainTextIntoHtml(original) } returns expected
        return expected
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

    private fun expectedEncryptedDraftBody(
        plaintextDraftBody: DraftBody,
        senderAddress: UserAddress,
        expectedEncryptedDraftBody: () -> DraftBody
    ): DraftBody = expectedEncryptedDraftBody().also {
        coEvery { encryptDraftBodyMock(plaintextDraftBody, senderAddress) } returns it.right()
    }

    private fun givenDraftBodyEncryptionFails() {
        coEvery { encryptDraftBodyMock(any(), any()) } returns Unit.left()
    }

    private fun expectedResolvedUserAddress(
        userId: UserId,
        email: SenderEmail,
        address: () -> UserAddress
    ) = address().also { coEvery { resolveUserAddressMock(userId, email.value) } returns it.right() }

    private fun expectResolveUserAddressFailure(userId: UserId, email: SenderEmail) {
        coEvery {
            resolveUserAddressMock(
                userId,
                email.value
            )
        } returns ResolveUserAddress.Error.UserAddressNotFound.left()
    }

    companion object {

        private val NoQuotedHtmlBody = null
    }
}
