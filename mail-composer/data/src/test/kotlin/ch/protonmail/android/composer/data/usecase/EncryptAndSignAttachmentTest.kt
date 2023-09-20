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

package ch.protonmail.android.composer.data.usecase

import java.io.File
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.test.utils.rule.LoggingTestRule
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
import me.proton.core.user.domain.entity.UserAddress
import me.proton.core.user.domain.entity.UserAddressKey
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals

class EncryptAndSignAttachmentTest {

    @get:Rule
    val loggingRule = LoggingTestRule()

    private val mockedSessionKey = SessionKey("mockedSessionKey".encodeToByteArray())
    private val mockedKeyPacket = "mockedKeyPacket".encodeToByteArray()

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
        every { encryptSessionKey(mockedSessionKey, armoredPublicKey) } returns mockedKeyPacket
        every { decryptSessionKey(mockedKeyPacket, unlockedPrivateKey) } returns mockedSessionKey
    }
    private val cryptoContextMock = mockk<CryptoContext> {
        every { pgpCrypto } returns pgpCryptoMock
        every { keyStoreCrypto } returns mockk {
            every { decrypt(encryptedPassphrase) } returns decryptedPassphrase
        }
    }

    val encryptAndSignAttachment by lazy { EncryptAndSignAttachment(cryptoContextMock) }

    @Test
    fun `when encrypting and signing attachment is successful EncryptedAttachmentResult is returned`() = runTest {
        // Given
        val decryptedFile = File.createTempFile("decrypted", "file")
        val encryptedFile = File.createTempFile("encrypted", "file")
        val unArmoredSignature = "unArmoredSignature".encodeToByteArray()
        expectEncryptingFileSucceeds(encryptedFile)
        expectSigningFileSucceeds(decryptedFile, unArmoredSignature)


        val expected = EncryptedAttachmentResult(
            keyPacket = mockedKeyPacket,
            encryptedAttachment = encryptedFile,
            signature = unArmoredSignature
        )

        // When
        val actual = encryptAndSignAttachment(userAddress, decryptedFile)

        // Then
        assertEquals(expected.right(), actual)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `when encrypting and signing of attachment fails to generate session key then FailedToGenerateSessionKey is returned`() =
        runTest {
            // Given
            val exception = Exception("Failed to generate session key")
            every { pgpCryptoMock.generateNewSessionKey() } throws exception
            val expected = AttachmentEncryptionError.FailedToGenerateSessionKey(exception)

            // When
            val actual = encryptAndSignAttachment(userAddress, File.createTempFile("decrypted", "file"))

            // Then
            assertEquals(expected.left(), actual)
        }

    @Test
    fun `when encrypting and signing fails to encrypt session key then FailedToEncryptSessionKey is returned`() =
        runTest {
            // Given
            val exception = CryptoException("Failed to encrypt session key")
            expectEncryptingSessionKeyFails(exception)
            val expected = AttachmentEncryptionError.FailedToEncryptSessionKey(exception)

            // When
            val actual = encryptAndSignAttachment(userAddress, File.createTempFile("decrypted", "file"))

            // Then
            assertEquals(expected.left(), actual)
        }

    @Test
    fun `when encrypting of attachment fails due then FailedToEncryptAttachment is returned`() = runTest {
        // Given
        val exception = CryptoException("Failed to encrypt attachment")
        val expected = AttachmentEncryptionError.FailedToEncryptAttachment(exception)

        expectEncryptingFileFails(exception)

        // When
        val actual = encryptAndSignAttachment(userAddress, File.createTempFile("decrypted", "file"))

        // Then
        assertEquals(expected.left(), actual)
    }

    @Test
    fun `when signing of attachment fails then AttachmentEncryptionError_FailedToSignAttachment is returned`() =
        runTest {
            // Given
            val decryptedFile = File.createTempFile("encrypted", "file")
            val exception = CryptoException("Failed to sign attachment")

            val expected = AttachmentEncryptionError.FailedToSignAttachment(exception)

            expectEncryptingFileSucceeds(decryptedFile)
            expectSigningFails(decryptedFile, exception)

            // When
            val actual = encryptAndSignAttachment(userAddress, decryptedFile)

            // Then
            assertEquals(expected.left(), actual)
        }

    private fun expectEncryptingFileSucceeds(encryptedFile: File) {
        every { pgpCryptoMock.encryptFile(any(), any(), mockedSessionKey) } returns encryptedFile
    }

    private fun expectSigningFileSucceeds(decryptedFile: File, unArmoredSignature: ByteArray) {
        val signature = "signature"
        every { pgpCryptoMock.signFile(decryptedFile, unlockedPrivateKey) } returns signature
        every { pgpCryptoMock.getUnarmored(signature) } returns unArmoredSignature
    }

    private fun expectEncryptingSessionKeyFails(exception: Exception) {
        every { pgpCryptoMock.encryptSessionKey(mockedSessionKey, armoredPublicKey) } throws exception
    }

    private fun expectEncryptingFileFails(exception: CryptoException) {
        every { pgpCryptoMock.encryptFile(any(), any(), mockedSessionKey) } throws exception
    }

    private fun expectSigningFails(decryptedFile: File, exception: CryptoException) {
        every { pgpCryptoMock.signFile(decryptedFile, unlockedPrivateKey) } throws exception
    }
}
