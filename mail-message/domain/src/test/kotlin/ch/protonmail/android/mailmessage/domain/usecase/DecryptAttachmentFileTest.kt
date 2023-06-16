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

package ch.protonmail.android.mailmessage.domain.usecase

import java.io.File
import java.io.IOException
import android.content.Context
import android.os.ParcelFileDescriptor
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailcommon.domain.system.ParcelFileDescriptorProvider
import ch.protonmail.android.mailmessage.domain.entity.AttachmentId
import ch.protonmail.android.mailmessage.domain.entity.AttachmentWorkerStatus
import ch.protonmail.android.mailmessage.domain.entity.MessageAttachmentMetadata
import ch.protonmail.android.mailmessage.domain.entity.MessageWithBody
import ch.protonmail.android.mailmessage.domain.repository.AttachmentRepository
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.testdata.message.MessageBodyTestData
import ch.protonmail.android.testdata.message.MessageTestData
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
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
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertFailsWith

class DecryptAttachmentFileTest {

    private val userId = UserIdSample.Primary
    private val messageId = MessageIdSample.Invoice
    private val attachmentId = AttachmentId("invoice")
    private val attachmentHash = "attachmentHash"

    private val armoredPrivateKey = "armoredPrivateKey"
    private val armoredPublicKey = "armoredPublicKey"
    private val encryptedPassphrase = EncryptedByteArray("encryptedPassphrase".encodeToByteArray())
    private val decryptedPassphrase = PlainByteArray("decryptedPassPhrase".encodeToByteArray())
    private val unlockedPrivateKey = "unlockedPrivateKey".encodeToByteArray()
    private val encodedKeyPackets = "keyPackets".encodeToByteArray()

    private val decryptedFile = run { File.createTempFile("decryted", "test") }
    private val encryptedFile = run { File.createTempFile("encrypted", "test") }

    private val attachmentMetadata = MessageAttachmentMetadata(
        userId = userId,
        messageId = messageId,
        attachmentId = attachmentId,
        hash = attachmentHash,
        path = encryptedFile.path,
        status = AttachmentWorkerStatus.Success
    )

    private val context = mockk<Context>(relaxed = true) { every { cacheDir } returns File("cacheDir") }
    private val attachmentRepo = mockk<AttachmentRepository>()
    private val messageRepo = mockk<MessageRepository>()

    private val parcelFileDescriptor = mockk<ParcelFileDescriptor>()
    private val mockParcelFileDescriptorProvider = mockk<ParcelFileDescriptorProvider> {
        every { provideParcelFileDescriptor(decryptedFile) } returns parcelFileDescriptor
    }

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

    @Before
    fun setUp() {
        mockkStatic(File::class)
        every { File.createTempFile(attachmentId.id, "pdf", context.cacheDir) } returns decryptedFile
    }

    @After
    fun tearDown() {
        unmockkStatic(File::class)
    }

    @Test
    fun `should return ParcelFileDescriptor when decrypting was successful`() = runTest {
        // Given
        mockDecryptionSuccessFul()
        val decryptAttachmentFile = buildDecryptAttachmentFile()

        coEvery { attachmentRepo.getAttachmentMetadataByHash(attachmentHash) } returns attachmentMetadata.right()
        coEvery {
            messageRepo.getMessageWithBody(userId, messageId)
        } returns MessageWithBody(MessageTestData.message, MessageBodyTestData.messageBodyWithAttachment).right()

        // When
        val result = decryptAttachmentFile("attachmentHash")

        // Then
        assert(result == parcelFileDescriptor)
    }

    @Test
    fun `should throw crypto exception when decrypting failed`() = runTest {
        // Given
        mockDecryptionFails()
        val attachmentHash = "attachmentHash"
        val decryptAttachmentFile = buildDecryptAttachmentFile()

        coEvery { attachmentRepo.getAttachmentMetadataByHash(attachmentHash) } returns attachmentMetadata.right()
        coEvery {
            messageRepo.getMessageWithBody(userId, messageId)
        } returns MessageWithBody(MessageTestData.message, MessageBodyTestData.messageBodyWithAttachment).right()

        // When - Then
        assertFailsWith<CryptoException> { decryptAttachmentFile("attachmentHash") }
    }

    @Test
    fun `should throw illegal argument exception when attachment metadata is not found`() = runTest {
        // Given
        val attachmentHash = "attachmentHash"
        val decryptAttachmentFile = buildDecryptAttachmentFile()

        coEvery {
            attachmentRepo.getAttachmentMetadataByHash(attachmentHash)
        } returns DataError.Local.NoDataCached.left()

        // When - Then
        assertFailsWith<IllegalArgumentException> { decryptAttachmentFile("attachmentHash") }
    }

    @Test
    fun `should throw illegal argument exception when message is not found`() = runTest {
        // Given
        val attachmentHash = "attachmentHash"
        val decryptAttachmentFile = buildDecryptAttachmentFile()

        coEvery { attachmentRepo.getAttachmentMetadataByHash(attachmentHash) } returns attachmentMetadata.right()
        coEvery {
            messageRepo.getMessageWithBody(userId, messageId)
        } returns DataError.Local.NoDataCached.left()

        // When - Then
        assertFailsWith<IllegalArgumentException> { decryptAttachmentFile("attachmentHash") }
    }

    @Test
    fun `should throw illegal argument exception when user address is not found`() = runTest {
        // Given
        val attachmentHash = "attachmentHash"
        val decryptAttachmentFile = buildDecryptAttachmentFile()

        coEvery { attachmentRepo.getAttachmentMetadataByHash(attachmentHash) } returns attachmentMetadata.right()
        coEvery {
            messageRepo.getMessageWithBody(userId, messageId)
        } returns MessageWithBody(MessageTestData.message, MessageBodyTestData.messageBodyWithAttachment).right()
        coEvery {
            mockUserAddressManager.getAddress(UserIdSample.Primary, MessageTestData.message.addressId)
        } returns null

        // When - Then
        assertFailsWith<IllegalArgumentException> { decryptAttachmentFile("attachmentHash") }
    }

    @Test
    fun `should throw IllegalArgumentException when attachment is not found within the connected message`() = runTest {
        // Given
        val attachmentHash = "attachmentHash"
        val decryptAttachmentFile = buildDecryptAttachmentFile()

        coEvery { attachmentRepo.getAttachmentMetadataByHash(attachmentHash) } returns attachmentMetadata.right()
        coEvery {
            messageRepo.getMessageWithBody(userId, messageId)
        } returns MessageWithBody(MessageTestData.message, MessageBodyTestData.messageBody).right()

        // When - Then
        assertFailsWith<IllegalArgumentException> { decryptAttachmentFile("attachmentHash") }
    }

    @Test
    fun `should throw IOException when creating temp file fails`() = runTest {
        // Given
        val attachmentHash = "attachmentHash"
        val decryptAttachmentFile = buildDecryptAttachmentFile()

        coEvery { attachmentRepo.getAttachmentMetadataByHash(attachmentHash) } returns attachmentMetadata.right()
        coEvery {
            messageRepo.getMessageWithBody(userId, messageId)
        } returns MessageWithBody(MessageTestData.message, MessageBodyTestData.messageBodyWithAttachment).right()

        every { File.createTempFile(attachmentId.id, "pdf", context.cacheDir) } throws IOException()

        // When - Then
        assertFailsWith<IOException> { decryptAttachmentFile("attachmentHash") }
    }

    private fun mockDecryptionSuccessFul() {
        every { pgpCryptoMock.decryptFile(encryptedFile, decryptedFile, sessionKeyMocked) } returns mockk()
    }

    private fun mockDecryptionFails() {
        every { pgpCryptoMock.decryptFile(encryptedFile, decryptedFile, sessionKeyMocked) } throws CryptoException()
    }

    private fun buildDecryptAttachmentFile() = DecryptAttachmentFile(
        context,
        attachmentRepo,
        messageRepo,
        cryptoContext,
        mockUserAddressManager,
        mockParcelFileDescriptorProvider,
        UnconfinedTestDispatcher()
    )

}
