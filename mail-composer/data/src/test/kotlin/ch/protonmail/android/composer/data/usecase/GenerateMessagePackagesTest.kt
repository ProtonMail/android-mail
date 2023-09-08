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

import ch.protonmail.android.composer.data.remote.resource.SendMessagePackage
import ch.protonmail.android.composer.data.sample.SendMessageSample
import ch.protonmail.android.mailmessage.domain.sample.MessageWithBodySample
import ch.protonmail.android.test.utils.rule.LoggingTestRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.crypto.common.context.CryptoContext
import me.proton.core.crypto.common.keystore.EncryptedByteArray
import me.proton.core.crypto.common.keystore.PlainByteArray
import me.proton.core.crypto.common.pgp.PGPCrypto
import me.proton.core.crypto.common.pgp.dataPacket
import me.proton.core.crypto.common.pgp.decryptSessionKeyOrNull
import me.proton.core.crypto.common.pgp.keyPacket
import me.proton.core.key.domain.entity.key.PrivateKey
import me.proton.core.mailsettings.domain.entity.MimeType
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

    private val draft = MessageWithBodySample.RemoteDraftWith4RecipientTypes

    private val generateSendMessagePackageMock = mockk<GenerateSendMessagePackage>()

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
    }

    private val cryptoContextMock = mockk<CryptoContext> {
        every { pgpCrypto } returns pgpCryptoMock
        every { keyStoreCrypto } returns mockk {
            every { decrypt(encryptedPassphrase) } returns decryptedPassphrase
        }
    }

    private val sut = GenerateMessagePackages(
        cryptoContextMock,
        generateSendMessagePackageMock
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

        givenRecipientProtonMail(recipient1)
        givenRecipientClearMime(recipient2)
        givenRecipientCleartext(recipient3)
        givenRecipientPgpMime(recipient4)

        // When
        val actual = sut(
            userAddress,
            draft,
            sendPreferences
        )

        // Then
        val emailsWithGeneratedPackages = actual.getOrNull()?.map {
            it.addresses.keys.first()
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

    private fun givenRecipientPgpMime(recipient4: String) {
        every {
            pgpCryptoMock.encryptAndSignText(
                any(),
                SendMessageSample.SendPreferences.PgpMime.publicKey!!.key,
                unlockedPrivateKey
            )
        } returns SendMessageSample.PlaintextMimeBodyEncryptedAndSigned

        every {
            generateSendMessagePackageMock.invoke(
                recipient4,
                SendMessageSample.SendPreferences.PgpMime,
                SendMessageSample.BodySessionKey,
                SendMessageSample.EncryptedPlaintextBodySplit.dataPacket(),
                SendMessageSample.MimeBodySessionKey,
                SendMessageSample.PlaintextMimeBodyEncryptedAndSignedSplit.dataPacket(),
                Pair(
                    SendMessageSample.PlaintextMimeBodyEncryptedAndSignedSplit.keyPacket(),
                    SendMessageSample.PlaintextMimeBodyEncryptedAndSignedSplit.dataPacket()
                ),
                emptyList()
            )
        } returns SendMessagePackage(
            addresses = mapOf(
                recipient4 to SendMessagePackage.Address.ExternalEncrypted(
                    signature = true.toInt(),
                    bodyKeyPacket = Base64.encode(SendMessageSample.EncryptedMimeBodyDataPacket)
                )
            ),
            mimeType = MimeType.Mixed.value,
            body = Base64.encode(SendMessageSample.EncryptedMimeBodyDataPacket),
            type = PackageType.PgpMime.type
        )
    }

    private fun givenRecipientCleartext(recipient3: String) {
        every {
            generateSendMessagePackageMock.invoke(
                recipient3,
                SendMessageSample.SendPreferences.Cleartext,
                SendMessageSample.BodySessionKey,
                SendMessageSample.EncryptedPlaintextBodySplit.dataPacket(),
                SendMessageSample.MimeBodySessionKey,
                SendMessageSample.PlaintextMimeBodyEncryptedAndSignedSplit.dataPacket(),
                null, // Cleartext package type doesn't need this
                emptyList()
            )
        } returns SendMessagePackage(
            addresses = mapOf(
                recipient3 to SendMessagePackage.Address.ExternalCleartext(
                    signature = false.toInt()
                )
            ),
            mimeType = SendMessageSample.SendPreferences.Cleartext.mimeType.value,
            body = Base64.encode(SendMessageSample.EncryptedBodyDataPacket),
            type = PackageType.Cleartext.type
        )
    }

    private fun givenRecipientClearMime(recipient2: String) {
        every {
            generateSendMessagePackageMock.invoke(
                recipient2,
                SendMessageSample.SendPreferences.ClearMime,
                SendMessageSample.BodySessionKey,
                SendMessageSample.EncryptedPlaintextBodySplit.dataPacket(),
                SendMessageSample.MimeBodySessionKey,
                SendMessageSample.PlaintextMimeBodyEncryptedAndSignedSplit.dataPacket(),
                null, // ClearMime package type doesn't need this
                emptyList()
            )
        } returns SendMessagePackage(
            addresses = mapOf(
                recipient2 to SendMessagePackage.Address.ExternalSigned(
                    signature = true.toInt()
                )
            ),
            mimeType = MimeType.Mixed.value,
            body = Base64.encode(SendMessageSample.EncryptedMimeBodyDataPacket),
            type = PackageType.ClearMime.type
        )
    }

    private fun givenRecipientProtonMail(recipient1: String) {
        every {
            generateSendMessagePackageMock.invoke(
                recipient1,
                SendMessageSample.SendPreferences.ProtonMail,
                SendMessageSample.BodySessionKey,
                SendMessageSample.EncryptedPlaintextBodySplit.dataPacket(),
                SendMessageSample.MimeBodySessionKey,
                SendMessageSample.PlaintextMimeBodyEncryptedAndSignedSplit.dataPacket(),
                null, // ProtonMail package type doesn't need this
                emptyList()
            )
        } returns SendMessagePackage(
            addresses = mapOf(
                recipient1 to SendMessagePackage.Address.Internal(
                    signature = true.toInt(),
                    bodyKeyPacket = Base64.encode(SendMessageSample.RecipientBodyKeyPacket),
                    attachmentKeyPackets = emptyList()
                )
            ),
            mimeType = SendMessageSample.SendPreferences.ProtonMail.mimeType.value,
            body = Base64.encode(SendMessageSample.EncryptedBodyDataPacket),
            type = PackageType.ProtonMail.type
        )
    }

}
