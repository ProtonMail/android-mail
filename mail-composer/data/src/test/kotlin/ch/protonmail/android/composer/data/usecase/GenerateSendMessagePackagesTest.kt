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
import ch.protonmail.android.mailmessage.domain.model.MimeType
import ch.protonmail.android.test.utils.rule.LoggingTestRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.test.runTest
import me.proton.core.crypto.common.context.CryptoContext
import me.proton.core.crypto.common.pgp.PGPCrypto
import me.proton.core.crypto.common.srp.Auth
import me.proton.core.crypto.common.srp.SrpCrypto
import me.proton.core.key.domain.encryptSessionKey
import me.proton.core.key.domain.entity.key.PublicKey
import me.proton.core.mailsettings.domain.entity.PackageType
import me.proton.core.util.kotlin.toInt
import org.junit.Assert.assertEquals
import org.junit.Rule
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalEncodingApi::class)
class GenerateSendMessagePackagesTest {

    @get:Rule
    val loggingRule = LoggingTestRule()

    private val pgpCryptoMock = mockk<PGPCrypto>()
    private val srpCryptoMock = mockk<SrpCrypto>()
    private val cryptoContextMock = mockk<CryptoContext> {
        every { pgpCrypto } returns pgpCryptoMock
        every { srpCrypto } returns srpCryptoMock
    }

    private val sut = GenerateSendMessagePackages(cryptoContextMock)

    @Test
    fun `using invalid SendPreferences without PublicKey returns empty list`() = runTest {
        // Given
        val sendPreferencesProtonMailButNoPublicKey = SendMessageSample.SendPreferences.ProtonMailWithEmptyPublicKey

        // When
        val actual = sut(
            mapOf(SendMessageSample.RecipientEmail to sendPreferencesProtonMailButNoPublicKey),
            SendMessageSample.BodySessionKey,
            SendMessageSample.EncryptedBodyDataPacket,
            SendMessageSample.MimeBodySessionKey,
            SendMessageSample.EncryptedMimeBodyDataPacket,
            MimeType.PlainText,
            mapOf(SendMessageSample.RecipientEmail to SendMessageSample.SignedEncryptedMimeBody),
            emptyMap(),
            areAllAttachmentsSigned = true,
            messagePassword = null,
            modulus = null
        ).getOrNull()

        // Then
        assertEquals(emptyList<SendMessagePackage>(), actual)
        loggingRule.assertErrorLogged(
            "GenerateSendMessagePackages: publicKey for" +
                " ${sendPreferencesProtonMailButNoPublicKey.pgpScheme.name} was null"
        )
    }

    @Test
    fun `providing no signedEncryptedMimeBody for PgpMime returns empty list`() = runTest {
        // Given
        val sendPreferences = SendMessageSample.SendPreferences.PgpMime

        // When
        val actual = sut(
            mapOf(SendMessageSample.RecipientEmail to sendPreferences),
            SendMessageSample.BodySessionKey,
            SendMessageSample.EncryptedBodyDataPacket,
            SendMessageSample.MimeBodySessionKey,
            SendMessageSample.EncryptedMimeBodyDataPacket,
            MimeType.PlainText,
            emptyMap(),
            emptyMap(),
            areAllAttachmentsSigned = true,
            messagePassword = null,
            modulus = null
        ).getOrNull()

        // Then
        assertEquals(emptyList<SendMessagePackage>(), actual)
        loggingRule.assertErrorLogged("GenerateSendMessagePackages: signedEncryptedMimeBody was null")
    }

    @Test
    fun `generate package for ProtonMail, no attachments`() = runTest {
        // Given
        val sendPreferences = SendMessageSample.SendPreferences.ProtonMail.copy(
            publicKey = expectPublicKeyEncryptSessionKey()
        )

        // When
        val actual = sut(
            mapOf(SendMessageSample.RecipientEmail to sendPreferences),
            SendMessageSample.BodySessionKey,
            SendMessageSample.EncryptedBodyDataPacket,
            SendMessageSample.MimeBodySessionKey,
            SendMessageSample.EncryptedMimeBodyDataPacket,
            MimeType.PlainText,
            mapOf(SendMessageSample.RecipientEmail to SendMessageSample.SignedEncryptedMimeBody),
            emptyMap(),
            areAllAttachmentsSigned = true,
            messagePassword = null,
            modulus = null
        ).getOrNull()

        // Then
        val expected = SendMessagePackage(
            addresses = mapOf(
                SendMessageSample.RecipientEmail to SendMessagePackage.Address.Internal(
                    signature = true.toInt(),
                    bodyKeyPacket = Base64.encode(SendMessageSample.RecipientBodyKeyPacket),
                    attachmentKeyPackets = emptyMap()
                )
            ),
            mimeType = sendPreferences.mimeType.value,
            body = Base64.encode(SendMessageSample.EncryptedBodyDataPacket),
            type = PackageType.ProtonMail.type
        )

        assertNotNull(actual)
        assertEquals(listOf(expected), actual)

        // make sure we don't leak keys, because everything should be encrypted
        assertEquals(null, actual.first().attachmentKeys)
        assertEquals(null, actual.first().bodyKey)
    }

    @Test
    fun `generate package for PgpMime, no attachments`() = runTest {
        // Given
        val sendPreferences = SendMessageSample.SendPreferences.PgpMime

        // When
        val actual = sut(
            mapOf(SendMessageSample.RecipientEmail to sendPreferences),
            SendMessageSample.BodySessionKey,
            SendMessageSample.EncryptedBodyDataPacket,
            SendMessageSample.MimeBodySessionKey,
            SendMessageSample.EncryptedMimeBodyDataPacket,
            MimeType.PlainText,
            mapOf(SendMessageSample.RecipientEmail to SendMessageSample.SignedEncryptedMimeBody),
            emptyMap(),
            areAllAttachmentsSigned = true,
            messagePassword = null,
            modulus = null
        ).getOrNull()

        // Then
        val expected = SendMessagePackage(
            addresses = mapOf(
                SendMessageSample.RecipientEmail to SendMessagePackage.Address.ExternalEncrypted(
                    signature = true.toInt(),
                    bodyKeyPacket = Base64.encode(SendMessageSample.SignedEncryptedMimeBody.first)
                )
            ),
            mimeType = me.proton.core.mailsettings.domain.entity.MimeType.Mixed.value, // forced multipart
            body = Base64.encode(SendMessageSample.SignedEncryptedMimeBody.second),
            type = PackageType.PgpMime.type
        )

        assertNotNull(actual)
        assertEquals(listOf(expected), actual)

        // make sure we don't leak keys, because everything should be encrypted
        assertEquals(null, actual.first().attachmentKeys)
        assertEquals(null, actual.first().bodyKey)
    }

    @Test
    fun `generate package for ClearMime, no attachments`() = runTest {
        // Given
        val sendPreferences = SendMessageSample.SendPreferences.ClearMime

        // When
        val actual = sut(
            mapOf(SendMessageSample.RecipientEmail to sendPreferences),
            SendMessageSample.BodySessionKey,
            SendMessageSample.EncryptedBodyDataPacket,
            SendMessageSample.MimeBodySessionKey,
            SendMessageSample.EncryptedMimeBodyDataPacket,
            MimeType.PlainText,
            mapOf(SendMessageSample.RecipientEmail to SendMessageSample.SignedEncryptedMimeBody),
            emptyMap(),
            areAllAttachmentsSigned = true,
            messagePassword = null,
            modulus = null
        ).getOrNull()

        // Then
        val expected = SendMessagePackage(
            addresses = mapOf(
                SendMessageSample.RecipientEmail to SendMessagePackage.Address.ExternalSigned(
                    signature = true.toInt()
                )
            ),
            mimeType = me.proton.core.mailsettings.domain.entity.MimeType.Mixed.value, // forced multipart
            body = Base64.encode(SendMessageSample.EncryptedMimeBodyDataPacket),
            type = PackageType.ClearMime.type,
            bodyKey = SendMessageSample.CleartextMimeBodyKey
        )

        assertEquals(listOf(expected), actual)
    }

    @Test
    fun `generate package for Cleartext, no attachments`() = runTest {
        // Given
        val sendPreferences = SendMessageSample.SendPreferences.Cleartext

        // When
        val actual = sut(
            mapOf(SendMessageSample.RecipientEmail to sendPreferences),
            SendMessageSample.BodySessionKey,
            SendMessageSample.EncryptedBodyDataPacket,
            SendMessageSample.MimeBodySessionKey,
            SendMessageSample.EncryptedMimeBodyDataPacket,
            MimeType.PlainText,
            mapOf(SendMessageSample.RecipientEmail to SendMessageSample.SignedEncryptedMimeBody),
            emptyMap(),
            areAllAttachmentsSigned = true,
            messagePassword = null,
            modulus = null
        ).getOrNull()

        // Then
        val expected = SendMessagePackage(
            addresses = mapOf(
                SendMessageSample.RecipientEmail to SendMessagePackage.Address.ExternalCleartext(
                    signature = false.toInt()
                )
            ),
            mimeType = MimeType.PlainText.value,
            body = Base64.encode(SendMessageSample.EncryptedBodyDataPacket),
            type = PackageType.Cleartext.type,
            bodyKey = SendMessageSample.CleartextBodyKey,
            attachmentKeys = emptyMap()
        )

        assertEquals(listOf(expected), actual)
    }

    @Test
    fun `generate package for Cleartext, no attachments, override SendPreferences body MimeType with HTML`() = runTest {
        // Given
        val sendPreferences = SendMessageSample.SendPreferences.Cleartext

        // When
        val actual = sut(
            mapOf(SendMessageSample.RecipientEmail to sendPreferences),
            SendMessageSample.BodySessionKey,
            SendMessageSample.EncryptedBodyDataPacket,
            SendMessageSample.MimeBodySessionKey,
            SendMessageSample.EncryptedMimeBodyDataPacket,
            MimeType.Html,
            mapOf(SendMessageSample.RecipientEmail to SendMessageSample.SignedEncryptedMimeBody),
            emptyMap(),
            areAllAttachmentsSigned = true,
            messagePassword = null,
            modulus = null
        ).getOrNull()

        // Then
        val expected = SendMessagePackage(
            addresses = mapOf(
                SendMessageSample.RecipientEmail to SendMessagePackage.Address.ExternalCleartext(
                    signature = false.toInt()
                )
            ),
            mimeType = MimeType.Html.value,
            body = Base64.encode(SendMessageSample.EncryptedBodyDataPacket),
            type = PackageType.Cleartext.type,
            bodyKey = SendMessageSample.CleartextBodyKey,
            attachmentKeys = emptyMap()
        )

        assertEquals(listOf(expected), actual)
    }

    @Test
    fun `generate package for ProtonMail with 'sign' disabled when there are unsigned attachments`() = runTest {
        // Given
        val sendPreferences = SendMessageSample.SendPreferences.ProtonMail.copy(
            publicKey = expectPublicKeyEncryptSessionKey()
        )

        // When
        val actual = sut(
            mapOf(SendMessageSample.RecipientEmail to sendPreferences),
            SendMessageSample.BodySessionKey,
            SendMessageSample.EncryptedBodyDataPacket,
            SendMessageSample.MimeBodySessionKey,
            SendMessageSample.EncryptedMimeBodyDataPacket,
            MimeType.PlainText,
            mapOf(SendMessageSample.RecipientEmail to SendMessageSample.SignedEncryptedMimeBody),
            emptyMap(),
            areAllAttachmentsSigned = false,
            messagePassword = null,
            modulus = null
        ).getOrNull()

        // Then
        val expected = SendMessagePackage(
            addresses = mapOf(
                SendMessageSample.RecipientEmail to SendMessagePackage.Address.Internal(
                    signature = false.toInt(),
                    bodyKeyPacket = Base64.encode(SendMessageSample.RecipientBodyKeyPacket),
                    attachmentKeyPackets = emptyMap()
                )
            ),
            mimeType = sendPreferences.mimeType.value,
            body = Base64.encode(SendMessageSample.EncryptedBodyDataPacket),
            type = PackageType.ProtonMail.type
        )

        assertEquals(listOf(expected), actual)
    }

    @Test
    fun `generate packages for Internal`() = runTest {
        // Given
        val sendPreferencesInternal = SendMessageSample.SendPreferences.ProtonMail.copy(
            publicKey = expectPublicKeyEncryptSessionKey()
        )

        // When
        val actual = sut(
            mapOf(
                SendMessageSample.RecipientEmail to sendPreferencesInternal
            ),
            SendMessageSample.BodySessionKey,
            SendMessageSample.EncryptedBodyDataPacket,
            SendMessageSample.MimeBodySessionKey,
            SendMessageSample.EncryptedMimeBodyDataPacket,
            MimeType.PlainText,
            emptyMap(),
            emptyMap(),
            areAllAttachmentsSigned = true,
            messagePassword = null,
            modulus = null
        ).getOrNull()

        // Then

        assertNotNull(actual)
        assertTrue(actual.size == 1)
        assertTrue(actual.first().addresses.size == 1)
        assertTrue {
            actual.first()
                .addresses[SendMessageSample.RecipientEmail] is SendMessagePackage.Address.Internal
        }
        assertEquals(actual.first().type, PackageType.ProtonMail.type)
    }

    @Test
    fun `generate packages for Cleartext`() = runTest {
        // Given
        val sendPreferencesCleartext = SendMessageSample.SendPreferences.Cleartext

        // When
        val actual = sut(
            mapOf(
                SendMessageSample.RecipientAliasEmail to sendPreferencesCleartext
            ),
            SendMessageSample.BodySessionKey,
            SendMessageSample.EncryptedBodyDataPacket,
            SendMessageSample.MimeBodySessionKey,
            SendMessageSample.EncryptedMimeBodyDataPacket,
            MimeType.PlainText,
            emptyMap(),
            emptyMap(),
            areAllAttachmentsSigned = true,
            messagePassword = null,
            modulus = null
        ).getOrNull()

        // Then

        assertNotNull(actual)
        assertTrue(actual.size == 1)
        assertTrue(actual.first().addresses.size == 1)
        assertTrue {
            actual.first()
                .addresses[SendMessageSample.RecipientAliasEmail] is SendMessagePackage.Address.ExternalCleartext
        }
        assertEquals(actual.first().type, PackageType.Cleartext.type)
    }

    @Test
    fun `generate packages for ExternalSigned creates 1 shared package`() = runTest {

        // Given
        val sendPreferences = SendMessageSample.SendPreferences.ClearMime

        // When
        val actual = sut(
            mapOf(
                SendMessageSample.RecipientEmail to sendPreferences,
                SendMessageSample.RecipientAliasEmail to sendPreferences
            ),
            SendMessageSample.BodySessionKey,
            SendMessageSample.EncryptedBodyDataPacket,
            SendMessageSample.MimeBodySessionKey,
            SendMessageSample.EncryptedMimeBodyDataPacket,
            MimeType.PlainText,
            emptyMap(),
            emptyMap(),
            areAllAttachmentsSigned = true,
            messagePassword = null,
            modulus = null
        ).getOrNull()

        // Then

        assertNotNull(actual)
        assertTrue(actual.size == 1)
        assertTrue(actual.first().addresses.size == 2)
        assertTrue {
            actual.first()
                .addresses[SendMessageSample.RecipientEmail] is SendMessagePackage.Address.ExternalSigned
        }
        assertTrue {
            actual.first()
                .addresses[SendMessageSample.RecipientAliasEmail] is SendMessagePackage.Address.ExternalSigned
        }
        kotlin.test.assertEquals(actual.first().type, PackageType.ClearMime.type)
    }

    @Test
    fun `generate package for ProtonMail always uses PlainText MimeType when body is plain text`() = runTest {
        // Given
        val sendPreferences = SendMessageSample.SendPreferences.ProtonMailWithHtmlMime.copy(
            publicKey = expectPublicKeyEncryptSessionKey()
        )

        // When
        val actual = sut(
            mapOf(SendMessageSample.RecipientEmail to sendPreferences),
            SendMessageSample.BodySessionKey,
            SendMessageSample.EncryptedBodyDataPacket,
            SendMessageSample.MimeBodySessionKey,
            SendMessageSample.EncryptedMimeBodyDataPacket,
            MimeType.PlainText,
            mapOf(SendMessageSample.RecipientEmail to SendMessageSample.SignedEncryptedMimeBody),
            emptyMap(),
            areAllAttachmentsSigned = true,
            messagePassword = null,
            modulus = null
        ).getOrNull()

        // Then
        val expected = SendMessagePackage(
            addresses = mapOf(
                SendMessageSample.RecipientEmail to SendMessagePackage.Address.Internal(
                    signature = true.toInt(),
                    bodyKeyPacket = Base64.encode(SendMessageSample.RecipientBodyKeyPacket),
                    attachmentKeyPackets = emptyMap()
                )
            ),
            mimeType = MimeType.PlainText.value,
            body = Base64.encode(SendMessageSample.EncryptedBodyDataPacket),
            type = PackageType.ProtonMail.type
        )

        assertNotNull(actual)
        assertEquals(listOf(expected), actual)

        // make sure we don't leak keys, because everything should be encrypted
        assertEquals(null, actual.first().attachmentKeys)
        assertEquals(null, actual.first().bodyKey)
    }

    @Test
    fun `generate package for EncryptedOutside`() = runTest {
        // Given
        val sendPreferences = SendMessageSample.SendPreferences.ClearMime
        expectEncryptBodySessionKeyWithPassword()
        expectEncryptAttachmentSessionKeyWithPassword()
        expectGenerateRandomBytes()
        expectEncryptTextWithPassword()
        expectCalculatePasswordVerifier()

        // When
        val actual = sut(
            mapOf(SendMessageSample.RecipientEmail to sendPreferences),
            SendMessageSample.BodySessionKey,
            SendMessageSample.EncryptedBodyDataPacket,
            SendMessageSample.MimeBodySessionKey,
            SendMessageSample.EncryptedMimeBodyDataPacket,
            MimeType.PlainText,
            mapOf(SendMessageSample.RecipientEmail to SendMessageSample.SignedEncryptedMimeBody),
            mapOf(SendMessageSample.AttachmentId to SendMessageSample.AttachmentSessionKey),
            areAllAttachmentsSigned = true,
            messagePassword = SendMessageSample.MessagePassword,
            modulus = SendMessageSample.Modulus
        ).getOrNull()

        // Then
        val expected = SendMessagePackage(
            addresses = mapOf(
                SendMessageSample.RecipientEmail to SendMessagePackage.Address.EncryptedOutside(
                    bodyKeyPacket = Base64.encode(SendMessageSample.RecipientBodyKeyPacket),
                    attachmentKeyPackets = mapOf(
                        SendMessageSample.AttachmentId to Base64.encode(SendMessageSample.EncryptedAttachmentSessionKey)
                    ),
                    token = SendMessageSample.Token,
                    encToken = SendMessageSample.EncryptedToken,
                    auth = SendMessageSample.Auth,
                    passwordHint = SendMessageSample.MessagePassword.passwordHint,
                    signature = true.toInt()
                )
            ),
            mimeType = MimeType.PlainText.value,
            body = Base64.encode(SendMessageSample.EncryptedBodyDataPacket),
            type = 2
        )

        // Then
        assertNotNull(actual)
        assertEquals(listOf(expected), actual)
        // make sure we don't leak keys, because everything should be encrypted
        assertEquals(null, actual.first().attachmentKeys)
        assertEquals(null, actual.first().bodyKey)
    }

    @Test
    fun `generate a package for EncryptedOutside when pgp scheme is PGP MIME but encrypt setting is false`() = runTest {
        // Given
        val sendPreferences = SendMessageSample.SendPreferences.PgpMimeEncryptFalse
        expectEncryptBodySessionKeyWithPassword()
        expectEncryptAttachmentSessionKeyWithPassword()
        expectGenerateRandomBytes()
        expectEncryptTextWithPassword()
        expectCalculatePasswordVerifier()

        // When
        val actual = sut(
            mapOf(SendMessageSample.RecipientEmail to sendPreferences),
            SendMessageSample.BodySessionKey,
            SendMessageSample.EncryptedBodyDataPacket,
            SendMessageSample.MimeBodySessionKey,
            SendMessageSample.EncryptedMimeBodyDataPacket,
            MimeType.PlainText,
            mapOf(SendMessageSample.RecipientEmail to SendMessageSample.SignedEncryptedMimeBody),
            mapOf(SendMessageSample.AttachmentId to SendMessageSample.AttachmentSessionKey),
            areAllAttachmentsSigned = true,
            messagePassword = SendMessageSample.MessagePassword,
            modulus = SendMessageSample.Modulus
        ).getOrNull()

        // Then
        val expected = SendMessagePackage(
            addresses = mapOf(
                SendMessageSample.RecipientEmail to SendMessagePackage.Address.EncryptedOutside(
                    bodyKeyPacket = Base64.encode(SendMessageSample.RecipientBodyKeyPacket),
                    attachmentKeyPackets = mapOf(
                        SendMessageSample.AttachmentId to Base64.encode(SendMessageSample.EncryptedAttachmentSessionKey)
                    ),
                    token = SendMessageSample.Token,
                    encToken = SendMessageSample.EncryptedToken,
                    auth = SendMessageSample.Auth,
                    passwordHint = SendMessageSample.MessagePassword.passwordHint,
                    signature = true.toInt()
                )
            ),
            mimeType = MimeType.PlainText.value,
            body = Base64.encode(SendMessageSample.EncryptedBodyDataPacket),
            type = 2
        )

        // Then
        assertNotNull(actual)
        assertEquals(listOf(expected), actual)
        // make sure we don't leak keys, because everything should be encrypted
        assertEquals(null, actual.first().attachmentKeys)
        assertEquals(null, actual.first().bodyKey)
    }

    @Test
    fun `fallback to PgpMime when contact send preferences is pgpInline`() = runTest {
        // Given
        val sendPreferences = SendMessageSample.SendPreferences.PgpInline

        // When
        val actual = sut(
            mapOf(SendMessageSample.RecipientEmail to sendPreferences),
            SendMessageSample.BodySessionKey,
            SendMessageSample.EncryptedBodyDataPacket,
            SendMessageSample.MimeBodySessionKey,
            SendMessageSample.EncryptedMimeBodyDataPacket,
            MimeType.PlainText,
            mapOf(SendMessageSample.RecipientEmail to SendMessageSample.SignedEncryptedMimeBody),
            emptyMap(),
            areAllAttachmentsSigned = true,
            messagePassword = null,
            modulus = null
        ).getOrNull()

        // Then
        val expected = SendMessagePackage(
            addresses = mapOf(
                SendMessageSample.RecipientEmail to SendMessagePackage.Address.ExternalEncrypted(
                    signature = true.toInt(),
                    bodyKeyPacket = Base64.encode(SendMessageSample.SignedEncryptedMimeBody.first)
                )
            ),
            mimeType = me.proton.core.mailsettings.domain.entity.MimeType.Mixed.value, // forced multipart
            body = Base64.encode(SendMessageSample.SignedEncryptedMimeBody.second),
            type = PackageType.PgpMime.type
        )

        assertNotNull(actual)
        assertEquals(listOf(expected), actual)

        // make sure we don't leak keys, because everything should be encrypted
        assertNull(actual.first().attachmentKeys)
        assertNull(actual.first().bodyKey)
    }

    private fun expectPublicKeyEncryptSessionKey(): PublicKey {
        mockkStatic(PublicKey::encryptSessionKey)
        return mockk {
            every {
                encryptSessionKey(cryptoContextMock, SendMessageSample.BodySessionKey)
            } returns SendMessageSample.RecipientBodyKeyPacket
        }
    }

    private fun expectEncryptBodySessionKeyWithPassword() {
        every {
            pgpCryptoMock.encryptSessionKeyWithPassword(
                SendMessageSample.BodySessionKey,
                SendMessageSample.PasswordByteArray
            )
        } returns SendMessageSample.RecipientBodyKeyPacket
    }

    private fun expectEncryptAttachmentSessionKeyWithPassword() {
        every {
            pgpCryptoMock.encryptSessionKeyWithPassword(
                SendMessageSample.AttachmentSessionKey,
                SendMessageSample.PasswordByteArray
            )
        } returns SendMessageSample.EncryptedAttachmentSessionKey
    }

    private fun expectGenerateRandomBytes() {
        every { pgpCryptoMock.generateRandomBytes(size = 32) } returns SendMessageSample.TokenByteArray
    }

    private fun expectEncryptTextWithPassword() {
        every {
            pgpCryptoMock.encryptTextWithPassword(
                SendMessageSample.Token,
                SendMessageSample.PasswordByteArray
            )
        } returns SendMessageSample.EncryptedToken
    }

    private fun expectCalculatePasswordVerifier() {
        coEvery {
            srpCryptoMock.calculatePasswordVerifier(
                "",
                SendMessageSample.PasswordByteArray,
                SendMessageSample.Modulus.modulusId,
                SendMessageSample.Modulus.modulus
            )
        } returns Auth(
            version = SendMessageSample.Auth.version,
            modulusId = SendMessageSample.Auth.modulusId,
            salt = SendMessageSample.Auth.salt,
            verifier = SendMessageSample.Auth.verifier
        )
    }

}
