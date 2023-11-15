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
import ch.protonmail.android.test.utils.FakeTransactor
import ch.protonmail.android.test.utils.rule.LoggingTestRule
import io.mockk.called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import me.proton.core.user.domain.entity.UserAddress
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals

class StoreDraftWithBodyTest {

    @get:Rule
    val loggingTestRule = LoggingTestRule()

    private val encryptDraftBodyMock = mockk<EncryptDraftBody>()
    private val saveDraftMock = mockk<SaveDraft>()
    private val getLocalDraftMock = mockk<GetLocalDraft>()
    private val resolveUserAddressMock = mockk<ResolveUserAddress>()
    private val fakeTransactor = FakeTransactor()

    private val storeDraftWithBody = StoreDraftWithBody(
        getLocalDraftMock,
        encryptDraftBodyMock,
        saveDraftMock,
        resolveUserAddressMock,
        fakeTransactor
    )

    @Test
    fun `should save a draft with encrypted body and sender details`() = runTest {
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
        val expectedSavedDraft = existingDraft.copy(
            message = existingDraft.message.copy(
                sender = Sender(senderAddress.email, senderAddress.displayName!!),
                addressId = senderAddress.addressId
            ),
            messageBody = existingDraft.messageBody.copy(
                body = expectedEncryptedDraftBody.value
            )
        )
        expectedResolvedUserAddress(expectedUserId, senderEmail) { senderAddress }
        givenSaveDraftSucceeds(expectedSavedDraft, expectedUserId)

        // When
        val actualEither = storeDraftWithBody(
            draftMessageId, plaintextDraftBody, NoQuotedHtmlBody, senderEmail, expectedUserId
        )

        // Then
        coVerify { saveDraftMock(expectedSavedDraft, expectedUserId) }
        assertEquals(Unit.right(), actualEither)
    }

    @Test
    fun `should wrap draft text in 'pre', append quoted html body and set mime type to html when any quote exists`() =
        runTest {
            // Given
            val plaintextDraftBody = DraftBody("I am plaintext")
            val quotedHtmlBody = OriginalHtmlQuote("<div> I am quoted html </div>")
            val preformattedDraftText = "<pre>${plaintextDraftBody.value}</pre>"
            val expectedMergedBody = DraftBody("$preformattedDraftText${quotedHtmlBody.value}")
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
            val expectedSavedDraft = existingDraft.copy(
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
            givenSaveDraftSucceeds(expectedSavedDraft, expectedUserId)

            // When
            val actualEither = storeDraftWithBody(
                draftMessageId, plaintextDraftBody, quotedHtmlBody, senderEmail, expectedUserId
            )

            // Then
            coVerify { saveDraftMock(expectedSavedDraft, expectedUserId) }
            assertEquals(Unit.right(), actualEither)
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
        val actualEither = storeDraftWithBody(
            draftMessageId, plaintextDraftBody, NoQuotedHtmlBody, senderEmail, userId
        )

        // Then
        coVerify { saveDraftMock wasNot called }
        assertEquals(StoreDraftWithBodyError.DraftBodyEncryptionError.left(), actualEither)
        loggingTestRule.assertErrorLogged("Encrypt draft $draftMessageId body to store to local DB failed")
    }

    @Test
    fun `should return error when saving fails`() = runTest {
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
        val expectedSavedDraft = existingDraft.copy(
            message = existingDraft.message.copy(
                sender = Sender(senderAddress.email, senderAddress.displayName!!),
                addressId = senderAddress.addressId
            ),
            messageBody = existingDraft.messageBody.copy(
                body = expectedEncryptedDraftBody.value
            )
        )
        expectedResolvedUserAddress(expectedUserId, senderEmail) { senderAddress }
        givenSaveDraftFails(expectedSavedDraft, expectedUserId)

        // When
        val actualEither = storeDraftWithBody(
            draftMessageId, plaintextDraftBody, NoQuotedHtmlBody, senderEmail, expectedUserId
        )

        // Then
        assertEquals(StoreDraftWithBodyError.DraftSaveError.left(), actualEither)
        loggingTestRule.assertErrorLogged("Store draft $draftMessageId body to local DB failed")
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
        val actualEither = storeDraftWithBody(
            draftMessageId, expectedDraftBody, NoQuotedHtmlBody, expectedSenderEmail, expectedUserId
        )

        // Then
        assertEquals(StoreDraftWithBodyError.DraftResolveUserAddressError.left(), actualEither)
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
        val actualEither = storeDraftWithBody(
            draftMessageId, expectedDraftBody, NoQuotedHtmlBody, expectedSenderEmail, expectedUserId
        )

        // Then
        assertEquals(StoreDraftWithBodyError.DraftReadError.left(), actualEither)
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

    private fun givenSaveDraftSucceeds(messageWithBody: MessageWithBody, userId: UserId) {
        coEvery { saveDraftMock(messageWithBody, userId) } returns true
    }

    private fun givenSaveDraftFails(messageWithBody: MessageWithBody, userId: UserId) {
        coEvery { saveDraftMock(messageWithBody, userId) } returns false
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
