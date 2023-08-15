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

package ch.protonmail.android.mailmessage.data.local.usecase

import java.io.File
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailmessage.domain.model.AttachmentId
import ch.protonmail.android.mailmessage.domain.model.MessageWithBody
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.testdata.message.MessageBodyTestData
import ch.protonmail.android.testdata.message.MessageTestData
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import me.proton.core.crypto.common.context.CryptoContext
import me.proton.core.crypto.common.keystore.EncryptedByteArray
import me.proton.core.crypto.common.keystore.PlainByteArray
import me.proton.core.crypto.common.pgp.PGPCrypto
import me.proton.core.crypto.common.pgp.SessionKey
import me.proton.core.crypto.common.pgp.decryptSessionKeyOrNull
import me.proton.core.crypto.common.pgp.exception.CryptoException
import me.proton.core.key.domain.entity.key.PrivateKey
import me.proton.core.user.domain.UserAddressManager
import me.proton.core.user.domain.entity.UserAddress
import me.proton.core.user.domain.entity.UserAddressKey
import org.junit.Test
import kotlin.test.assertEquals

class DecryptAttachmentByteArrayTest {

    private val userId = UserIdSample.Primary
    private val messageId = MessageIdSample.Invoice
    private val attachmentId = AttachmentId("invoice")

    private val armoredPrivateKey = "armoredPrivateKey"
    private val armoredPublicKey = "armoredPublicKey"
    private val encryptedPassphrase = EncryptedByteArray("encryptedPassphrase".encodeToByteArray())
    private val decryptedPassphrase = PlainByteArray("decryptedPassPhrase".encodeToByteArray())
    private val unlockedPrivateKey = "unlockedPrivateKey".encodeToByteArray()
    private val encodedKeyPackets = "keyPackets".encodeToByteArray()

    private val decryptedFile: ByteArray = run { File.createTempFile("decrypted", "test") }.readBytes()
    private val encryptedFile: ByteArray = run { File.createTempFile("encrypted", "test") }.readBytes()

    private val messageRepo = mockk<MessageRepository>()

    private val sessionKeyMocked: SessionKey = mockk(relaxed = true)
    private val pgpCryptoMock = mockk<PGPCrypto> {
        every { getPublicKey(armoredPrivateKey) } returns armoredPublicKey
        every { getBase64Decoded("keyPackets") } returns "keyPackets".encodeToByteArray()
        every { unlock(armoredPrivateKey, decryptedPassphrase.array) } returns mockk(relaxUnitFun = true) {
            every { value } returns unlockedPrivateKey
        }
        every { decryptSessionKeyOrNull(encodedKeyPackets, unlockedPrivateKey) } returns sessionKeyMocked
    }
    private val cryptoContext = mockk<CryptoContext> {
        every { pgpCrypto } returns pgpCryptoMock
        every { keyStoreCrypto } returns mockk {
            every { decrypt(encryptedPassphrase) } returns decryptedPassphrase
        }
    }
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
    private val mockUserAddressManager = mockk<UserAddressManager> {
        coEvery { getAddress(UserIdSample.Primary, MessageTestData.message.addressId) } returns userAddress
    }

    @Test
    fun `should return decrypted ByteArray when decrypting was successful`() = runTest {
        // Given
        mockDecryptionSuccessful()
        val decryptAttachmentFile = buildDecryptAttachmentFile()

        coEvery {
            messageRepo.getLocalMessageWithBody(userId, messageId)
        } returns MessageWithBody(MessageTestData.message, MessageBodyTestData.messageBodyWithAttachment)

        // When
        val result = decryptAttachmentFile(userId, messageId, attachmentId, encryptedFile)

        // Then
        assertEquals(decryptedFile.right(), result)
    }

    @Test
    fun `should throw crypto exception when decrypting failed`() = runTest {
        // Given
        mockDecryptionFails()
        val decryptAttachmentFile = buildDecryptAttachmentFile()

        coEvery {
            messageRepo.getLocalMessageWithBody(userId, messageId)
        } returns MessageWithBody(MessageTestData.message, MessageBodyTestData.messageBodyWithAttachment)

        // When
        val result = decryptAttachmentFile(userId, messageId, attachmentId, encryptedFile)

        // Then
        assertEquals(AttachmentDecryptionError.DecryptionFailed.left(), result)
    }

    @Test
    fun `should throw illegal argument exception when message is not found`() = runTest {
        // Given
        val decryptAttachmentFile = buildDecryptAttachmentFile()
        coEvery {
            messageRepo.getLocalMessageWithBody(userId, messageId)
        } returns null

        // When
        val result = decryptAttachmentFile(userId, messageId, attachmentId, encryptedFile)

        // Then
        assertEquals(AttachmentDecryptionError.MessageBodyNotFound.left(), result)
    }

    @Test
    fun `should throw illegal argument exception when user address is not found`() = runTest {
        // Given
        val decryptAttachmentFile = buildDecryptAttachmentFile()

        coEvery {
            messageRepo.getLocalMessageWithBody(userId, messageId)
        } returns MessageWithBody(MessageTestData.message, MessageBodyTestData.messageBodyWithAttachment)
        coEvery {
            mockUserAddressManager.getAddress(UserIdSample.Primary, MessageTestData.message.addressId)
        } returns null

        // When
        val result = decryptAttachmentFile(userId, messageId, attachmentId, encryptedFile)

        // Then
        assertEquals(AttachmentDecryptionError.UserAddressNotFound.left(), result)
    }

    @Test
    fun `should throw IllegalArgumentException when attachment is not found within the connected message`() = runTest {
        // Given
        val decryptAttachmentFile = buildDecryptAttachmentFile()

        coEvery {
            messageRepo.getLocalMessageWithBody(userId, messageId)
        } returns MessageWithBody(MessageTestData.message, MessageBodyTestData.messageBody)

        // When
        val result = decryptAttachmentFile(userId, messageId, attachmentId, encryptedFile)

        // Then
        assertEquals(AttachmentDecryptionError.MessageAttachmentNotFound.left(), result)
    }

    private fun mockDecryptionSuccessful() {
        every { pgpCryptoMock.decryptData(encryptedFile, sessionKeyMocked) } returns decryptedFile
    }

    private fun mockDecryptionFails() {
        every { pgpCryptoMock.decryptData(encryptedFile, sessionKeyMocked) } throws CryptoException()
    }

    private fun buildDecryptAttachmentFile() = DecryptAttachmentByteArray(
        messageRepo,
        cryptoContext,
        mockUserAddressManager,
        UnconfinedTestDispatcher()
    )

}
