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
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.test.runTest
import me.proton.core.crypto.common.context.CryptoContext
import me.proton.core.key.domain.encryptSessionKey
import me.proton.core.key.domain.entity.key.PublicKey
import me.proton.core.mailsettings.domain.entity.PackageType
import me.proton.core.util.kotlin.toInt
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.assertTrue

@OptIn(ExperimentalEncodingApi::class)
class GenerateSendMessagePackagesTest {

    @get:Rule
    val loggingRule = LoggingTestRule()

    private val cryptoContextMock = mockk<CryptoContext>()

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
            areAllAttachmentsSigned = true
        )

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
            areAllAttachmentsSigned = true
        )

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
            areAllAttachmentsSigned = true
        )

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
            areAllAttachmentsSigned = true
        )

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
            areAllAttachmentsSigned = true
        )

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
            areAllAttachmentsSigned = true
        )

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
            areAllAttachmentsSigned = true
        )

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
            areAllAttachmentsSigned = false
        )

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
    fun `generate packages for Internal and Cleartext creates 1 shared package`() = runTest {
        // Given
        val sendPreferencesInternal = SendMessageSample.SendPreferences.ProtonMail.copy(
            publicKey = expectPublicKeyEncryptSessionKey()
        )

        val sendPreferencesCleartext = SendMessageSample.SendPreferences.Cleartext

        // When
        val actual = sut(
            mapOf(
                SendMessageSample.RecipientEmail to sendPreferencesInternal,
                SendMessageSample.RecipientAliasEmail to sendPreferencesCleartext
            ),
            SendMessageSample.BodySessionKey,
            SendMessageSample.EncryptedBodyDataPacket,
            SendMessageSample.MimeBodySessionKey,
            SendMessageSample.EncryptedMimeBodyDataPacket,
            MimeType.PlainText,
            emptyMap(),
            emptyMap(),
            areAllAttachmentsSigned = true
        )

        // Then

        assertTrue(actual.size == 1)
        assertTrue(actual.first().addresses.size == 2)
        assertTrue {
            actual.first()
                .addresses[SendMessageSample.RecipientEmail] is SendMessagePackage.Address.Internal
        }
        assertTrue {
            actual.first()
                .addresses[SendMessageSample.RecipientAliasEmail] is SendMessagePackage.Address.ExternalCleartext
        }
        kotlin.test.assertEquals(actual.first().type, PackageType.ProtonMail.type + PackageType.Cleartext.type)
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
            areAllAttachmentsSigned = true
        )

        // Then

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
            areAllAttachmentsSigned = true
        )

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

        assertEquals(listOf(expected), actual)

        // make sure we don't leak keys, because everything should be encrypted
        assertEquals(null, actual.first().attachmentKeys)
        assertEquals(null, actual.first().bodyKey)
    }

    private fun expectPublicKeyEncryptSessionKey(): PublicKey {
        mockkStatic(PublicKey::encryptSessionKey)
        return mockk {
            every {
                encryptSessionKey(cryptoContextMock, SendMessageSample.BodySessionKey)
            } returns SendMessageSample.RecipientBodyKeyPacket
        }
    }

}
