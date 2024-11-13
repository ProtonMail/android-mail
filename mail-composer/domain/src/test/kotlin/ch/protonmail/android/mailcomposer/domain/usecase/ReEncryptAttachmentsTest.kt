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

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.sample.UserAddressSample
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailcommon.domain.usecase.ResolveUserAddress
import ch.protonmail.android.mailcomposer.domain.model.SenderEmail
import ch.protonmail.android.mailmessage.domain.model.MessageWithBody
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.mailmessage.domain.sample.MessageWithBodySample
import ch.protonmail.android.test.utils.FakeTransactor
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.crypto.common.context.CryptoContext
import me.proton.core.crypto.common.keystore.EncryptedByteArray
import me.proton.core.crypto.common.keystore.PlainByteArray
import me.proton.core.crypto.common.pgp.PGPCrypto
import me.proton.core.crypto.common.pgp.SessionKey
import me.proton.core.crypto.common.pgp.exception.CryptoException
import me.proton.core.key.domain.entity.key.PrivateKey
import me.proton.core.user.domain.entity.AddressId
import me.proton.core.user.domain.entity.UserAddress
import me.proton.core.user.domain.entity.UserAddressKey
import org.junit.Test
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.assertEquals

@ExperimentalEncodingApi
class ReEncryptAttachmentsTest {

    private val userId = UserIdSample.Primary
    private val messageId = MessageIdSample.Invoice
    private val previousSender = SenderEmail(UserAddressSample.PrimaryAddress.email)
    private val newSender = SenderEmail(UserAddressSample.AliasAddress.email)

    private val mockedSessionKey = SessionKey("mockedSessionKey".encodeToByteArray())
    private val mockedKeyPacket = "encryptedKeyPackets".toByteArray()
    private val newMockedKeyPacket = "newEncryptedKeyPackets".toByteArray()

    private val armoredPrivateKey = "armoredPrivateKey"
    private val armoredPublicKey = "armoredPublicKey"
    private val encryptedPassphrase = EncryptedByteArray("encryptedPassphrase".encodeToByteArray())
    private val decryptedPassphrase = PlainByteArray("decryptedPassPhrase".encodeToByteArray())
    private val unlockedPrivateKey = "unlockedPrivateKey".encodeToByteArray()

    private val userAddressKey = mockk<UserAddressKey> {
        every { privateKey } returns PrivateKey(
            key = armoredPrivateKey,
            isPrimary = true,
            isActive = true,
            canEncrypt = true,
            canVerify = true,
            passphrase = encryptedPassphrase
        )
    }

    private val userAddress = mockk<UserAddress> {
        every { keys } returns listOf(userAddressKey)
    }

    private val pgpCryptoMock = mockk<PGPCrypto> {
        every { unlock(armoredPrivateKey, decryptedPassphrase.array) } returns mockk(relaxUnitFun = true) {
            every { value } returns unlockedPrivateKey
        }
        every { getPublicKey(armoredPrivateKey) } returns armoredPublicKey
        every { generateNewSessionKey() } returns mockedSessionKey
        every { encryptSessionKey(mockedSessionKey, armoredPublicKey) } returns newMockedKeyPacket
        every { decryptSessionKey(mockedKeyPacket, unlockedPrivateKey) } returns mockedSessionKey
    }
    private val cryptoContextMock = mockk<CryptoContext> {
        every { pgpCrypto } returns pgpCryptoMock
        every { keyStoreCrypto } returns mockk {
            every { decrypt(encryptedPassphrase) } returns decryptedPassphrase
        }
    }

    private val getLocalDraft = mockk<GetLocalDraft>()
    private val messageRepository = mockk<MessageRepository>()
    private val resolveUserAddress = mockk<ResolveUserAddress>()
    private val cryptoContext = cryptoContextMock
    private val transactor = FakeTransactor()

    private val reEncryptAttachments by lazy {
        ReEncryptAttachments(
            getLocalDraft,
            messageRepository,
            resolveUserAddress,
            cryptoContext,
            transactor
        )
    }

    @Test
    fun `when the reencryption is successful then the message is updated with the new keypackets`() = runTest {
        // Given
        val initialMessageWithBody = MessageWithBodySample.MessageWithEncryptedAttachments.copy(
            message = MessageWithBodySample.MessageWithEncryptedAttachments.message.copy(
                addressId = AddressId(newSender.value)
            )
        )
        val attachmentSize = initialMessageWithBody.messageBody.attachments.size
        val updatedMessageWithBody = initialMessageWithBody.copy(
            messageBody = initialMessageWithBody.messageBody.copy(
                attachments = initialMessageWithBody.messageBody.attachments.map {
                    it.copy(keyPackets = Base64.encode(newMockedKeyPacket))
                }
            )
        )

        expectGetLocalDraftSucceeds(initialMessageWithBody)
        expectGetMessageWithBodySucceeds(initialMessageWithBody)
        expectResolveUserAddressSucceeds(previousSender)
        expectResolveAddressIdSucceeds(AddressId(newSender.value))
        expectUpsertMessageSucceeds(updatedMessageWithBody)

        // When
        val actual = reEncryptAttachments(userId, messageId, previousSender, newSender)

        // Then
        assertEquals(Unit.right(), actual)
        coVerify(exactly = 1) { getLocalDraft(userId, messageId, newSender) }
        coVerify(exactly = 1) { messageRepository.upsertMessageWithBody(userId, updatedMessageWithBody) }
        coVerify(exactly = attachmentSize) { pgpCryptoMock.decryptSessionKey(any(), unlockedPrivateKey) }
        coVerify(exactly = attachmentSize) { pgpCryptoMock.encryptSessionKey(mockedSessionKey, armoredPublicKey) }
    }

    @Test
    fun `given the local draft is not found then return draft not found error`() = runTest {
        // Given
        val expectedDraftError = GetLocalDraft.Error.ResolveUserAddressError.left()
        val expectedError = AttachmentReEncryptionError.DraftNotFound.left()
        expectGetLocalDraftFails(expectedDraftError)

        // When
        val actual = reEncryptAttachments(userId, messageId, previousSender, newSender)

        // Then
        assertEquals(expectedError, actual)
        coVerify { messageRepository wasNot Called }
    }

    @Test
    fun `given the previous sender cannot be resolved then return previous failed to resolve error`() = runTest {
        // Given
        val expectedError = AttachmentReEncryptionError.FailedToResolvePreviousUserAddress.left()
        val expectedMessage = MessageWithBodySample.MessageWithEncryptedAttachments
        expectGetLocalDraftSucceeds(expectedMessage)
        expectGetMessageWithBodySucceeds(expectedMessage)
        expectResolveUserAddressFails()

        // When
        val actual = reEncryptAttachments(userId, messageId, previousSender, newSender)

        // Then
        assertEquals(expectedError, actual)
    }

    @Test
    fun `given the new sender cannot be resolved then return new failed to resolve error`() = runTest {
        // Given
        val expectedError = AttachmentReEncryptionError.FailedToResolveNewUserAddress.left()
        val initialMessageWithBody = MessageWithBodySample.MessageWithEncryptedAttachments.copy(
            message = MessageWithBodySample.MessageWithEncryptedAttachments.message.copy(
                addressId = AddressId(newSender.value)
            )
        )

        expectGetLocalDraftSucceeds(initialMessageWithBody)
        expectGetMessageWithBodySucceeds(initialMessageWithBody)
        expectResolveUserAddressSucceeds(previousSender)
        expectResolveAddressIdFails(AddressId(newSender.value))

        // When
        val actual = reEncryptAttachments(userId, messageId, previousSender, newSender)

        // Then
        assertEquals(expectedError, actual)
    }

    @Test
    fun `given the attachment keyPackets cannot be decrypted then return failed to decrypt error`() = runTest {
        // Given
        val expectedError = AttachmentReEncryptionError.FailedToDecryptAttachmentKeyPackets.left()
        val initialMessageWithBody = MessageWithBodySample.MessageWithEncryptedAttachments.copy(
            message = MessageWithBodySample.MessageWithEncryptedAttachments.message.copy(
                addressId = AddressId(newSender.value)
            )
        )

        expectGetLocalDraftSucceeds(initialMessageWithBody)
        expectGetMessageWithBodySucceeds(initialMessageWithBody)
        expectResolveUserAddressSucceeds(previousSender)
        expectResolveAddressIdSucceeds(AddressId(newSender.value))
        expectDecryptSessionKeyFails()

        // When
        val actual = reEncryptAttachments(userId, messageId, previousSender, newSender)

        // Then
        assertEquals(expectedError, actual)
        coVerify(exactly = 1) { pgpCryptoMock.decryptSessionKey(any(), unlockedPrivateKey) }
    }

    @Test
    fun `given the attachment keyPackets cannot be encrypted then return failed to encrypt error`() = runTest {
        // Given
        val expectedError = AttachmentReEncryptionError.FailedToEncryptAttachmentKeyPackets.left()
        val initialMessageWithBody = MessageWithBodySample.MessageWithEncryptedAttachments.copy(
            message = MessageWithBodySample.MessageWithEncryptedAttachments.message.copy(
                addressId = AddressId(newSender.value)
            )
        )

        expectGetLocalDraftSucceeds(initialMessageWithBody)
        expectGetMessageWithBodySucceeds(initialMessageWithBody)
        expectResolveUserAddressSucceeds(previousSender)
        expectResolveAddressIdSucceeds(AddressId(newSender.value))
        expectEncryptSessionKeyFails()

        // When
        val actual = reEncryptAttachments(userId, messageId, previousSender, newSender)

        // Then
        assertEquals(expectedError, actual)
        coVerify(exactly = 1) { pgpCryptoMock.encryptSessionKey(mockedSessionKey, armoredPublicKey) }
    }

    @Test
    fun `given the attachment keyPackets cannot be updated then return failed to update error`() = runTest {
        // Given
        val expectedError = AttachmentReEncryptionError.FailedToUpdateAttachmentKeyPackets.left()
        val initialMessageWithBody = MessageWithBodySample.MessageWithEncryptedAttachments.copy(
            message = MessageWithBodySample.MessageWithEncryptedAttachments.message.copy(
                addressId = AddressId(newSender.value)
            )
        )

        expectGetLocalDraftSucceeds(initialMessageWithBody)
        expectGetMessageWithBodySucceeds(initialMessageWithBody)
        expectResolveUserAddressSucceeds(previousSender)
        expectResolveAddressIdSucceeds(AddressId(newSender.value))
        expectUpsertMessageFails(
            initialMessageWithBody.copy(
                messageBody = initialMessageWithBody.messageBody.copy(
                    attachments = initialMessageWithBody.messageBody.attachments.map {
                        it.copy(keyPackets = Base64.encode(newMockedKeyPacket))
                    }
                )
            )
        )

        // When
        val actual = reEncryptAttachments(userId, messageId, previousSender, newSender)

        // Then
        assertEquals(expectedError, actual)
    }

    private fun expectGetLocalDraftSucceeds(expectedMessageWithBody: MessageWithBody) {
        coEvery { getLocalDraft(userId, messageId, newSender) } returns expectedMessageWithBody.copy(
            message = expectedMessageWithBody.message.copy(
                addressId = AddressId(newSender.value)
            )
        ).right()
    }

    private fun expectGetLocalDraftFails(
        expectedDraftError: Either<GetLocalDraft.Error.ResolveUserAddressError, Nothing>
    ) {
        coEvery { getLocalDraft(userId, messageId, newSender) } returns expectedDraftError
    }

    private fun expectGetMessageWithBodySucceeds(expectedMessageWithBody: MessageWithBody) {
        coEvery {
            messageRepository.getMessageWithBody(userId, expectedMessageWithBody.message.messageId)
        } returns expectedMessageWithBody.right()
    }

    private fun expectResolveUserAddressSucceeds(senderEmail: SenderEmail) {
        coEvery { resolveUserAddress(userId, senderEmail.value) } returns userAddress.right()
    }

    private fun expectResolveUserAddressFails() {
        coEvery {
            resolveUserAddress(userId, previousSender.value)
        } returns ResolveUserAddress.Error.UserAddressNotFound.left()
    }

    private fun expectResolveAddressIdSucceeds(addressId: AddressId) {
        coEvery { resolveUserAddress(userId, addressId) } returns userAddress.right()
    }

    private fun expectResolveAddressIdFails(addressId: AddressId) {
        coEvery { resolveUserAddress(userId, addressId) } returns ResolveUserAddress.Error.UserAddressNotFound.left()
    }

    private fun expectUpsertMessageSucceeds(messageWithBody: MessageWithBody) {
        coEvery { messageRepository.upsertMessageWithBody(userId, messageWithBody) } returns true
    }

    private fun expectUpsertMessageFails(messageWithBody: MessageWithBody) {
        coEvery { messageRepository.upsertMessageWithBody(userId, messageWithBody) } returns false
    }

    private fun expectDecryptSessionKeyFails() {
        every { pgpCryptoMock.decryptSessionKey(any(), unlockedPrivateKey) } throws CryptoException("Failed to decrypt")
    }

    private fun expectEncryptSessionKeyFails() {
        every {
            pgpCryptoMock.encryptSessionKey(mockedSessionKey, armoredPublicKey)
        } throws CryptoException("Failed to encrypt")
    }
}
