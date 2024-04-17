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
import ch.protonmail.android.composer.data.extension.encryptAndSignText
import ch.protonmail.android.composer.data.remote.resource.SendMessagePackage
import ch.protonmail.android.composer.data.sample.SendMessageSample
import ch.protonmail.android.mailmessage.domain.model.AttachmentId
import ch.protonmail.android.mailmessage.domain.model.MessageAttachment
import ch.protonmail.android.mailmessage.domain.model.MimeType
import ch.protonmail.android.mailmessage.domain.sample.MessageAttachmentSample
import ch.protonmail.android.mailmessage.domain.sample.MessageWithBodySample
import ch.protonmail.android.test.utils.rule.LoggingTestRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import me.proton.core.crypto.common.context.CryptoContext
import me.proton.core.crypto.common.keystore.EncryptedByteArray
import me.proton.core.crypto.common.keystore.PlainByteArray
import me.proton.core.crypto.common.pgp.EncryptedMessage
import me.proton.core.crypto.common.pgp.PGPCrypto
import me.proton.core.crypto.common.pgp.dataPacket
import me.proton.core.crypto.common.pgp.decryptSessionKeyOrNull
import me.proton.core.crypto.common.pgp.exception.CryptoException
import me.proton.core.crypto.common.pgp.keyPacket
import me.proton.core.key.domain.entity.key.PrivateKey
import me.proton.core.key.domain.entity.key.PrivateKeyRing
import me.proton.core.key.domain.entity.key.PublicKeyRing
import me.proton.core.key.domain.entity.keyholder.KeyHolderContext
import me.proton.core.mailsettings.domain.entity.PackageType
import me.proton.core.user.domain.entity.UserAddress
import me.proton.core.user.domain.entity.UserAddressKey
import me.proton.core.util.kotlin.toInt
import org.junit.Rule
import org.junit.Test
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.assertTrue

@OptIn(ExperimentalEncodingApi::class)
class GenerateMessagePackagesTest {

    @get:Rule
    val loggingRule = LoggingTestRule()

    private val attachmentKeyPackets = Base64.encode("keyPackets".toByteArray())
    private val attachment = MessageAttachmentSample.document.copy(keyPackets = attachmentKeyPackets)
    private val attachmentFile = File.createTempFile("file", "txt")
    private val draft = MessageWithBodySample.RemoteDraftWith4RecipientTypes.copy(
        messageBody = MessageWithBodySample.RemoteDraftWith4RecipientTypes.messageBody.copy(
            attachments = listOf(attachment)
        )
    )

    private val generateSendMessagePackagesMock = mockk<GenerateSendMessagePackages>()
    private val generateMimeBodyMock = mockk<GenerateMimeBody>()

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
        every { getEncryptedPackets(draft.messageBody.body) } returns SendMessageSample.EncryptedPlaintextBodySplit
        every {
            getEncryptedPackets(SendMessageSample.PlaintextMimeBodyEncryptedAndSigned)
        } returns SendMessageSample.PlaintextMimeBodyEncryptedAndSignedSplit

        every { unlock(armoredPrivateKey, decryptedPassphrase.array) } returns mockk(relaxUnitFun = true) {
            every { value } returns unlockedPrivateKey
        }
        every { getPublicKey(armoredPrivateKey) } returns armoredPublicKey
        every { getBase64Decoded("keyPackets") } returns "keyPackets".encodeToByteArray()
        every { unlock(armoredPrivateKey, decryptedPassphrase.array) } returns mockk(relaxUnitFun = true) {
            every { value } returns unlockedPrivateKey
        }
        every {
            decryptSessionKey(Base64.decode(attachmentKeyPackets), unlockedPrivateKey)
        } returns SendMessageSample.AttachmentSessionKey
        every {
            decryptSessionKeyOrNull(SendMessageSample.EncryptedPlaintextBodySplit.keyPacket(), unlockedPrivateKey)
        } returns SendMessageSample.BodySessionKey
        every {
            decryptSessionKeyOrNull(
                SendMessageSample.PlaintextMimeBodyEncryptedAndSignedSplit.keyPacket(),
                unlockedPrivateKey
            )
        } returns SendMessageSample.MimeBodySessionKey
        every { decryptText(draft.messageBody.body, unlockedPrivateKey) } returns SendMessageSample.CleartextBody
        /* we pass any() here because we don't want to mock result of generateMimeBody() */
        every {
            encryptAndSignText(any(), armoredPublicKey, unlockedPrivateKey)
        } returns SendMessageSample.PlaintextMimeBodyEncryptedAndSigned
        every {
            encryptAndSignText(
                any(),
                SendMessageSample.SendPreferences.PgpMime.publicKey!!.key,
                unlockedPrivateKey
            )
        } returns SendMessageSample.PlaintextMimeBodyEncryptedAndSigned
    }

    private val cryptoContextMock = mockk<CryptoContext> {
        every { pgpCrypto } returns pgpCryptoMock
        every { keyStoreCrypto } returns mockk {
            every { decrypt(encryptedPassphrase) } returns decryptedPassphrase
        }
    }

    private val sut = GenerateMessagePackages(
        cryptoContextMock,
        generateSendMessagePackagesMock,
        generateMimeBodyMock
    )

    @Test
    fun `generates message packages for all provided SendPreferences`() = runTest {
        // Given
        val recipient1 = draft.message.toList.first().address
        val recipient2 = draft.message.ccList.first().address
        val recipient3 = draft.message.bccList.first().address
        val recipient4 = draft.message.bccList[1].address

        val sendPreferences = mapOf(
            recipient1 to SendMessageSample.SendPreferences.ProtonMail,
            recipient2 to SendMessageSample.SendPreferences.ClearMime,
            recipient3 to SendMessageSample.SendPreferences.Cleartext,
            recipient4 to SendMessageSample.SendPreferences.PgpMime
        )

        givenAllRecipients(recipient1, recipient2, recipient3, recipient4)

        expectGenerateMimeBody(
            SendMessageSample.CleartextBody,
            MimeType.PlainText,
            listOf(attachment),
            mapOf(attachment.attachmentId to attachmentFile),
            "generated MIME body"
        )

        // When
        val actual = sut(
            userAddress,
            draft,
            sendPreferences,
            mapOf(attachment.attachmentId to attachmentFile),
            SendMessageSample.MessagePassword,
            SendMessageSample.Modulus
        )

        // Then
        val emailsWithGeneratedPackages = actual.getOrNull()?.flatMap {
            it.addresses.keys
        }

        assertTrue(
            emailsWithGeneratedPackages!!.containsAll(
                listOf(
                    recipient1,
                    recipient2,
                    recipient3,
                    recipient4
                )
            )
        )

    }

    @Test
    fun `returns error when CryptoException is thrown`() = runTest {
        // Given
        val recipient1 = draft.message.toList.first().address
        val recipient2 = draft.message.ccList.first().address
        val recipient3 = draft.message.bccList.first().address
        val recipient4 = draft.message.bccList[1].address

        val sendPreferences = mapOf(
            recipient1 to SendMessageSample.SendPreferences.ProtonMail,
            recipient2 to SendMessageSample.SendPreferences.ClearMime,
            recipient3 to SendMessageSample.SendPreferences.Cleartext,
            recipient4 to SendMessageSample.SendPreferences.PgpMime
        )

        givenAllRecipients(recipient1, recipient2, recipient3, recipient4)

        every { pgpCryptoMock.decryptText(any(), any()) } throws CryptoException("crypto failed")

        // When
        val actual = sut(
            userAddress,
            draft,
            sendPreferences,
            mapOf(attachment.attachmentId to attachmentFile),
            SendMessageSample.MessagePassword,
            SendMessageSample.Modulus
        )

        // Then
        assertTrue(actual.isLeft())

    }

    @Test
    fun `returns error when GenerateSendMessagePackages returns error`() = runTest {
        // Given
        val recipient1 = draft.message.toList.first().address
        val recipient2 = draft.message.ccList.first().address
        val recipient3 = draft.message.bccList.first().address
        val recipient4 = draft.message.bccList[1].address

        val sendPreferences = mapOf(
            recipient1 to SendMessageSample.SendPreferences.ProtonMail,
            recipient2 to SendMessageSample.SendPreferences.ClearMime,
            recipient3 to SendMessageSample.SendPreferences.Cleartext,
            recipient4 to SendMessageSample.SendPreferences.PgpMime
        )

        givenAllRecipients(recipient1, recipient2, recipient3, recipient4)

        expectGenerateMimeBody(
            SendMessageSample.CleartextBody,
            MimeType.PlainText,
            listOf(attachment),
            mapOf(attachment.attachmentId to attachmentFile),
            "generated MIME body"
        )

        coEvery {
            generateSendMessagePackagesMock.invoke(
                any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any()
            )
        } returns GenerateSendMessagePackages.Error.ProtonMailAndCleartext("error").left()

        // When
        val actual = sut(
            userAddress,
            draft,
            sendPreferences,
            mapOf(attachment.attachmentId to attachmentFile),
            SendMessageSample.MessagePassword,
            SendMessageSample.Modulus
        )

        // Then
        assertTrue(actual.isLeft())

    }

    /**
     * Returns expected list of top-level Mail Packages for all recipients combined.
     */
    private fun givenAllRecipients(
        recipient1: String,
        recipient2: String,
        recipient3: String,
        recipient4: String
    ) {
        coEvery {
            generateSendMessagePackagesMock.invoke(
                mapOf(
                    recipient1 to SendMessageSample.SendPreferences.ProtonMail,
                    recipient2 to SendMessageSample.SendPreferences.ClearMime,
                    recipient3 to SendMessageSample.SendPreferences.Cleartext,
                    recipient4 to SendMessageSample.SendPreferences.PgpMime
                ),
                SendMessageSample.BodySessionKey,
                SendMessageSample.EncryptedPlaintextBodySplit.dataPacket(),
                SendMessageSample.MimeBodySessionKey,
                SendMessageSample.PlaintextMimeBodyEncryptedAndSignedSplit.dataPacket(),
                MimeType.PlainText,
                mapOf(
                    recipient4 to Pair(
                        SendMessageSample.PlaintextMimeBodyEncryptedAndSignedSplit.keyPacket(),
                        SendMessageSample.PlaintextMimeBodyEncryptedAndSignedSplit.dataPacket()
                    )
                ),
                mapOf(MessageAttachmentSample.document.attachmentId.id to SendMessageSample.AttachmentSessionKey),
                areAllAttachmentsSigned = false,
                messagePassword = SendMessageSample.MessagePassword,
                modulus = SendMessageSample.Modulus
            )
        } returns listOf(
            SendMessagePackage(
                addresses = mapOf(
                    recipient1 to SendMessagePackage.Address.Internal(
                        signature = true.toInt(),
                        bodyKeyPacket = Base64.encode(SendMessageSample.RecipientBodyKeyPacket),
                        attachmentKeyPackets = emptyMap()
                    ),
                    recipient3 to SendMessagePackage.Address.ExternalCleartext(
                        signature = false.toInt()
                    )
                ),
                mimeType = draft.messageBody.mimeType.value,
                body = Base64.encode(SendMessageSample.EncryptedBodyDataPacket),
                type = PackageType.ProtonMail.type + PackageType.Cleartext.type
            ),
            SendMessagePackage(
                addresses = mapOf(
                    recipient2 to SendMessagePackage.Address.ExternalSigned(
                        signature = true.toInt()
                    )
                ),
                mimeType = MimeType.MultipartMixed.value,
                body = Base64.encode(SendMessageSample.EncryptedMimeBodyDataPacket),
                type = PackageType.ClearMime.type
            ),
            SendMessagePackage(
                addresses = mapOf(
                    recipient4 to SendMessagePackage.Address.ExternalEncrypted(
                        signature = true.toInt(),
                        bodyKeyPacket = Base64.encode(SendMessageSample.EncryptedMimeBodyDataPacket)
                    )
                ),
                mimeType = MimeType.MultipartMixed.value,
                body = Base64.encode(SendMessageSample.EncryptedMimeBodyDataPacket),
                type = PackageType.PgpMime.type
            )
        ).right()
    }

    @Test
    fun `extension method KeyHolderContext#encryptAndSignText does not lock PrivateKey after using it`() = runTest {
        // Given
        val textToEncrypt = "text to encrypt"

        val privateKeyRingMock = PrivateKeyRing(cryptoContextMock, listOf(SendMessageSample.PrivateKey))
        val publicKeyRingMock = PublicKeyRing(listOf(SendMessageSample.PublicKey))

        val privateKeyDecryptedPassphrase = PlainByteArray("decrypted private key passphrase".encodeToByteArray())
        val unlockedPrivateKey = "unlocked PrivateKey".encodeToByteArray()

        every {
            cryptoContextMock.keyStoreCrypto.decrypt(SendMessageSample.PrivateKey.passphrase!!)
        } returns privateKeyDecryptedPassphrase

        every {
            pgpCryptoMock.unlock(SendMessageSample.PrivateKey.key, privateKeyDecryptedPassphrase.array)
        } returns mockk(relaxUnitFun = true) {
            every { value } returns unlockedPrivateKey
        }

        every {
            pgpCryptoMock.encryptAndSignText(textToEncrypt, any(), unlockedPrivateKey)
        } returns EncryptedMessage()

        val sut = KeyHolderContext(
            cryptoContextMock,
            privateKeyRingMock,
            publicKeyRingMock
        )

        // When
        sut.encryptAndSignText("text to encrypt", SendMessageSample.PublicKey)

        verify(exactly = 0) { sut.privateKeyRing.unlockedPrimaryKey.unlockedKey.close() }
    }

    private fun expectGenerateMimeBody(
        body: String,
        bodyContentType: MimeType,
        attachments: List<MessageAttachment>,
        attachmentFiles: Map<AttachmentId, File>,
        generatedMimeBody: String
    ) {
        every {
            generateMimeBodyMock(
                body,
                bodyContentType,
                attachments,
                attachmentFiles
            )
        } returns generatedMimeBody
    }

}
