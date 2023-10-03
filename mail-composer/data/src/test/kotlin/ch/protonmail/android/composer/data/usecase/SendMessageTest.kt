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
import ch.protonmail.android.composer.data.remote.MessageRemoteDataSource
import ch.protonmail.android.composer.data.remote.resource.SendMessageBody
import ch.protonmail.android.composer.data.remote.resource.SendMessagePackage
import ch.protonmail.android.composer.data.remote.response.SendMessageResponse
import ch.protonmail.android.composer.data.sample.SendMessageSample
import ch.protonmail.android.mailcommon.domain.mapper.fromProtonCode
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.model.ProtonError
import ch.protonmail.android.mailcommon.domain.sample.UserAddressSample
import ch.protonmail.android.mailcomposer.domain.usecase.FindLocalDraft
import ch.protonmail.android.mailcomposer.domain.usecase.ResolveUserAddress
import ch.protonmail.android.mailmessage.domain.model.AttachmentId
import ch.protonmail.android.mailmessage.domain.model.MessageWithBody
import ch.protonmail.android.mailmessage.domain.model.Participant
import ch.protonmail.android.mailmessage.domain.model.Recipient
import ch.protonmail.android.mailmessage.domain.sample.MessageSample
import ch.protonmail.android.mailmessage.domain.sample.MessageWithBodySample
import ch.protonmail.android.mailsettings.domain.usecase.ObserveMailSettings
import ch.protonmail.android.testdata.message.MessageBodyTestData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.key.domain.entity.key.PublicKey
import me.proton.core.mailsendpreferences.domain.model.SendPreferences
import me.proton.core.mailsendpreferences.domain.usecase.ObtainSendPreferences
import me.proton.core.mailsettings.domain.entity.MimeType
import me.proton.core.mailsettings.domain.entity.PackageType
import me.proton.core.util.kotlin.toInt
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SendMessageTest {

    private val messageRemoteDataSource = mockk<MessageRemoteDataSource>()
    private val resolveUserAddress = mockk<ResolveUserAddress>()
    private val generateMessagePackages = mockk<GenerateMessagePackages>()
    private val findLocalDraft = mockk<FindLocalDraft>()
    private val obtainSendPreferences = mockk<ObtainSendPreferences>()
    private val observeMailSettings = mockk<ObserveMailSettings>()
    private val readAttachmentsFromStorage = mockk<ReadAttachmentsFromStorage>()

    private val sendMessage = SendMessage(
        messageRemoteDataSource,
        resolveUserAddress,
        generateMessagePackages,
        findLocalDraft,
        obtainSendPreferences,
        observeMailSettings,
        readAttachmentsFromStorage
    )

    private val userId = UserAddressSample.PrimaryAddress.userId
    private val baseRecipient = Participant("address@proton.me", "name")
    private val sampleMessage = generateDraftMessage(recipients = listOf(baseRecipient))
    private val messageId = sampleMessage.message.messageId
    private val recipients = sampleMessage.message.run { toList + ccList + bccList }.map { it.address }

    @AfterTest
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `when local draft is not found upon sending, the error is propagated to the calling site`() = runTest {
        // Given
        coEvery { findLocalDraft.invoke(userId, messageId) } answers { null }

        // When
        val result = sendMessage(userId, messageId)

        // Then
        assertEquals(DataError.MessageSending.DraftNotFound.left(), result)
    }

    @Test
    fun `when send address cannot be resolved, the error is propagated to the calling site`() = runTest {
        // Given
        expectFindLocalDraftSucceeds()
        coEvery {
            resolveUserAddress(
                userId,
                sampleMessage.message.addressId
            )
        } answers { ResolveUserAddress.Error.UserAddressNotFound.left() }

        // When
        val result = sendMessage(userId, messageId)

        // Then
        assertEquals(DataError.MessageSending.SenderAddressNotFound.left(), result)
    }

    @Test
    fun `when send preferences cannot be fetched, the error is propagated to the calling site`() = runTest {
        // Given
        expectFindLocalDraftSucceeds()
        expectResolveUserAddressSucceeds()
        expectObserveMailSettingsReturnsNull()
        coEvery { obtainSendPreferences(userId, recipients) } answers {
            mapOf(Pair(baseRecipient.address, ObtainSendPreferences.Result.Error.AddressDisabled))
        }

        // When
        val result = sendMessage(userId, messageId)

        // Then
        assertEquals(DataError.MessageSending.SendPreferences.left(), result)
    }

    @Test
    fun `when attachment reading fails, the error is propagated to the calling site`() = runTest {
        // Given
        val sendPreferences = generateSendPreferences(encrypt = true, sign = true, pgpScheme = PackageType.PgpMime)
        val expectedError = DataError.Local.NoDataCached.left()

        expectFindLocalDraftSucceeds()
        expectResolveUserAddressSucceeds()
        expectObserveMailSettingsReturnsNull()
        expectObtainSendPreferencesSucceeds(sendPreferences)
        coEvery {
            readAttachmentsFromStorage(userId, messageId, sampleMessage.messageBody.attachments.map { it.attachmentId })
        } returns DataError.Local.NoDataCached.left()

        // When
        val result = sendMessage(userId, messageId)

        // Then
        assertEquals(expectedError, result)
    }

    @Test
    fun `should read attachments for PGP MIME messages`() = runTest {
        // Given
        val sendPreferences = generateSendPreferences(encrypt = true, sign = true, pgpScheme = PackageType.PgpMime)
        val attachmentIds = sampleMessage.messageBody.attachments.map { it.attachmentId }
        val attachments = attachmentIds.associateWith { File.createTempFile("file", "txt") }

        expectFindLocalDraftSucceeds()
        expectResolveUserAddressSucceeds()
        expectObserveMailSettingsReturnsNull()
        expectObtainSendPreferencesSucceeds(sendPreferences)
        expectReadAttachmentsFromStorageSucceeds(attachmentIds, attachments)
        val messagePackages = expectGenerateMessagePackagesSucceeds(sendPreferences, attachments)
        expectSendOperationSucceeds(messagePackages)

        // When
        sendMessage(userId, messageId)

        // Then
        coVerify(exactly = attachmentIds.size) { readAttachmentsFromStorage(userId, messageId, any()) }
    }

    @Test
    fun `should read attachments for Clear MIME messages`() = runTest {
        // Given
        val sendPreferences = generateSendPreferences(encrypt = false, sign = true, pgpScheme = PackageType.ClearMime)
        val attachmentIds = sampleMessage.messageBody.attachments.map { it.attachmentId }
        val attachments = attachmentIds.associateWith { File.createTempFile("file", "txt") }

        expectFindLocalDraftSucceeds()
        expectResolveUserAddressSucceeds()
        expectObserveMailSettingsReturnsNull()
        expectObtainSendPreferencesSucceeds(sendPreferences)
        expectReadAttachmentsFromStorageSucceeds(attachmentIds, attachments)
        val messagePackages = expectGenerateMessagePackagesSucceeds(sendPreferences, attachments)
        expectSendOperationSucceeds(messagePackages)

        // When
        sendMessage(userId, messageId)

        // Then
        coVerify(exactly = attachmentIds.size) { readAttachmentsFromStorage(userId, messageId, any()) }
    }

    @Test
    fun `should not read attachments for non MIME messages`() = runTest {
        // Given
        val sendPreferences = generateSendPreferences(encrypt = true, sign = true, pgpScheme = PackageType.ProtonMail)

        expectFindLocalDraftSucceeds()
        expectResolveUserAddressSucceeds()
        expectObserveMailSettingsReturnsNull()
        expectObtainSendPreferencesSucceeds(sendPreferences)
        val messagePackages = expectGenerateMessagePackagesSucceeds(sendPreferences)
        expectSendOperationSucceeds(messagePackages)

        // When
        sendMessage(userId, messageId)

        // Then
        coVerify(exactly = 0) { readAttachmentsFromStorage(any(), any(), any()) }
    }

    @Test
    fun `when message packages fail to be generated, the error is propagated to the calling site`() = runTest {
        // Given
        val senderAddress = UserAddressSample.PrimaryAddress
        val sendPreferences = generateSendPreferences()
        val expectedError = DataError.MessageSending.GeneratingPackages.left()

        expectFindLocalDraftSucceeds()
        expectResolveUserAddressSucceeds()
        expectObserveMailSettingsReturnsNull()
        expectObtainSendPreferencesSucceeds(sendPreferences)
        coEvery {
            generateMessagePackages(
                senderAddress,
                sampleMessage,
                sendPreferences.forMessagePackages(),
                emptyMap()
            )
        } answers { expectedError }

        // When
        val result = sendMessage(userId, messageId)

        // Then
        assertEquals(expectedError, result)
    }

    @Test
    fun `when remote data source fails to send the message, the error is propagated to the calling site`() = runTest {
        // Given
        val sendPreferences = generateSendPreferences()
        val expectedError = DataError.Remote.Proton(ProtonError.fromProtonCode(503)).left()

        expectFindLocalDraftSucceeds()
        expectResolveUserAddressSucceeds()
        expectObserveMailSettingsReturnsNull()
        expectObtainSendPreferencesSucceeds(sendPreferences)
        val messagePackages = expectGenerateMessagePackagesSucceeds(sendPreferences)
        coEvery {
            messageRemoteDataSource.send(
                userId,
                messageId.id,
                generateSendMessageBody(messagePackages)
            )
        } answers { expectedError }

        // When
        val result = sendMessage(userId, messageId)

        // Then
        assertEquals(expectedError, result)
    }

    @Test
    fun `when sending goes through, the success is propagated to the calling site`() = runTest {
        val sendPreferences = generateSendPreferences()
        val expectedResult = Unit.right()

        expectFindLocalDraftSucceeds()
        expectResolveUserAddressSucceeds()
        expectObserveMailSettingsReturnsNull()
        expectObtainSendPreferencesSucceeds(sendPreferences)
        val messagePackages = expectGenerateMessagePackagesSucceeds(sendPreferences)
        expectSendOperationSucceeds(messagePackages)

        // When
        val result = sendMessage(userId, messageId)

        // Then
        assertEquals(expectedResult, result)
    }

    private fun expectFindLocalDraftSucceeds() {
        coEvery { findLocalDraft.invoke(userId, messageId) } answers { sampleMessage }
    }

    private fun expectResolveUserAddressSucceeds() {
        coEvery {
            resolveUserAddress(userId, sampleMessage.message.addressId)
        } answers { UserAddressSample.PrimaryAddress.right() }
    }

    private fun expectObserveMailSettingsReturnsNull() {
        coEvery { observeMailSettings.invoke(userId) } answers { flowOf(null) }
    }

    private fun expectObtainSendPreferencesSucceeds(sendPreferences: Map<String, ObtainSendPreferences.Result>) {
        coEvery { obtainSendPreferences(userId, recipients) } answers { sendPreferences }
    }

    private fun expectReadAttachmentsFromStorageSucceeds(
        attachmentIds: List<AttachmentId>,
        attachments: Map<AttachmentId, File>
    ) {
        coEvery {
            readAttachmentsFromStorage(userId, messageId, attachmentIds)
        } returns attachments.right()
    }

    private fun expectGenerateMessagePackagesSucceeds(
        sendPreferences: Map<String, ObtainSendPreferences.Result>,
        attachments: Map<AttachmentId, File> = emptyMap()
    ) = generateSendPackages(UserAddressSample.PrimaryAddress.email).also { messagePackages ->
        coEvery {
            generateMessagePackages(
                UserAddressSample.PrimaryAddress,
                sampleMessage,
                sendPreferences.forMessagePackages(),
                attachments
            )
        } answers { messagePackages.right() }
    }

    private fun expectSendOperationSucceeds(messagePackages: List<SendMessagePackage>) {
        coEvery {
            messageRemoteDataSource.send(
                userId,
                messageId.id,
                generateSendMessageBody(messagePackages)
            )
        } answers { SendMessageResponse(200, mockk()).right() }
    }

    private fun generateDraftMessage(recipients: List<Recipient>): MessageWithBody {
        val message = MessageSample.build(toList = recipients)
        val messageBody = MessageBodyTestData.messageBodyWithAttachment
        return MessageWithBodySample.Invoice.copy(message = message, messageBody = messageBody)
    }

    private fun generateSendPreferences(
        encrypt: Boolean = true,
        sign: Boolean = true,
        pgpScheme: PackageType = PackageType.ProtonMail,
        mimeType: MimeType = MimeType.Html,
        publicKey: PublicKey? = null
    ): Map<String, ObtainSendPreferences.Result> {
        return mapOf(
            Pair(
                baseRecipient.address,
                ObtainSendPreferences.Result.Success(
                    SendPreferences(
                        encrypt = encrypt,
                        sign = sign,
                        pgpScheme = pgpScheme,
                        mimeType = mimeType,
                        publicKey = publicKey
                    )
                )
            )
        )
    }

    private fun Map<String, ObtainSendPreferences.Result>.forMessagePackages(): Map<String, SendPreferences> {
        return this.map {
            require(it.value is ObtainSendPreferences.Result.Success) { "Unsupported send preferences" }
            Pair(it.key, (it.value as? ObtainSendPreferences.Result.Success)!!.sendPreferences)
        }.toMap()
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun generateSendPackages(recipient: String): List<SendMessagePackage> {
        return listOf(
            SendMessagePackage(
                addresses = mapOf(recipient to SendMessagePackage.Address.ExternalCleartext(signature = false.toInt())),
                mimeType = SendMessageSample.SendPreferences.Cleartext.mimeType.value,
                body = Base64.encode(SendMessageSample.EncryptedBodyDataPacket),
                type = PackageType.Cleartext.type
            )
        )
    }

    private fun generateSendMessageBody(packages: List<SendMessagePackage>) = SendMessageBody(false.toInt(), packages)
}
