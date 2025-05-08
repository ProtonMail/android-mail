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
import ch.protonmail.android.mailcommon.domain.usecase.ResolveUserAddress
import ch.protonmail.android.mailcomposer.domain.usecase.FindLocalDraft
import ch.protonmail.android.mailcomposer.domain.usecase.ObserveMessageExpirationTime
import ch.protonmail.android.mailcomposer.domain.usecase.ObserveMessagePassword
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
import me.proton.core.account.domain.repository.AccountRepository
import me.proton.core.auth.domain.repository.AuthRepository
import me.proton.core.domain.entity.UserId
import me.proton.core.key.domain.entity.key.PublicKey
import me.proton.core.mailsendpreferences.domain.model.SendPreferences
import me.proton.core.mailsendpreferences.domain.usecase.ObtainSendPreferences
import me.proton.core.mailsettings.domain.entity.MimeType
import me.proton.core.mailsettings.domain.entity.PackageType
import me.proton.core.user.domain.entity.AddressId
import me.proton.core.util.kotlin.toInt
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SendMessageTest {

    private val accountRepository = mockk<AccountRepository>()
    private val authRepository = mockk<AuthRepository>()
    private val messageRemoteDataSource = mockk<MessageRemoteDataSource>()
    private val resolveUserAddress = mockk<ResolveUserAddress>()
    private val generateMessagePackages = mockk<GenerateMessagePackages>()
    private val findLocalDraft = mockk<FindLocalDraft>()
    private val obtainSendPreferences = mockk<ObtainSendPreferences>()
    private val observeMailSettings = mockk<ObserveMailSettings>()
    private val getAttachmentFiles = mockk<GetAttachmentFiles>()
    private val observeMessagePassword = mockk<ObserveMessagePassword>()
    private val observeMessageExpirationTime = mockk<ObserveMessageExpirationTime>()

    private val sendMessage = SendMessage(
        accountRepository,
        authRepository,
        messageRemoteDataSource,
        resolveUserAddress,
        generateMessagePackages,
        findLocalDraft,
        obtainSendPreferences,
        observeMailSettings,
        getAttachmentFiles,
        observeMessagePassword,
        observeMessageExpirationTime
    )

    private val userId = UserAddressSample.PrimaryAddress.userId
    private val baseRecipient = Participant("address@proton.me", "name")
    private val sampleMessage = generateDraftMessage(toRecipients = listOf(baseRecipient))
    private val messageId = sampleMessage.message.messageId
    private val recipients = sampleMessage.message.run { toList + ccList + bccList }.map { it.address }

    @AfterTest
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `when local draft is not found upon sending, the error is propagated to the calling site`() = runTest {
        // Given
        coEvery { findLocalDraft.invoke(userId, messageId) } returns null

        // When
        val result = sendMessage(userId, messageId)

        // Then
        assertEquals(SendMessage.Error.DraftNotFound.left(), result)
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
        } returns ResolveUserAddress.Error.UserAddressNotFound.left()

        // When
        val result = sendMessage(userId, messageId)

        // Then
        assertEquals(SendMessage.Error.SenderAddressNotFound.left(), result)
    }

    @Test
    fun `when send preferences cannot be fetched, the error is propagated to the calling site`() = runTest {
        // Given
        expectFindLocalDraftSucceeds()
        expectResolveUserAddressSucceeds()
        expectObserveMailSettingsReturnsNull()
        coEvery {
            obtainSendPreferences(userId, recipients)
        } returns mapOf(Pair(baseRecipient.address, ObtainSendPreferences.Result.Error.AddressDisabled))

        // When
        val result = sendMessage(userId, messageId)

        // Then
        assertEquals(
            SendMessage.Error.SendPreferences(
                mapOf(
                    baseRecipient.address to ObtainSendPreferences.Result.Error.AddressDisabled
                )
            ).left(),
            result
        )
    }

    @Test
    fun `when attachment reading fails, the error is propagated to the calling site`() = runTest {
        // Given
        val sendPreferences = generateSendPreferences(encrypt = true, sign = true, pgpScheme = PackageType.PgpMime)
        val expectedError = SendMessage.Error.DownloadingAttachments.left()

        expectFindLocalDraftSucceeds()
        expectResolveUserAddressSucceeds()
        expectObserveMailSettingsReturnsNull()
        expectObtainSendPreferencesSucceeds { sendPreferences }
        coEvery {
            getAttachmentFiles(userId, messageId, sampleMessage.messageBody.attachments.map { it.attachmentId })
        } returns GetAttachmentFiles.Error.DraftNotFound.left()

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
        expectObtainSendPreferencesSucceeds { sendPreferences }
        expectReadAttachmentsFromStorageSucceeds(attachmentIds, attachments)
        val messagePackages = expectGenerateMessagePackagesSucceeds(sendPreferences, attachments)
        expectSendOperationSucceeds(messagePackages)
        expectMessagePasswordDoesNotExist()
        expectMessageExpirationTimeDoesNotExist()

        // When
        sendMessage(userId, messageId)

        // Then
        coVerify(exactly = attachmentIds.size) { getAttachmentFiles(userId, messageId, any()) }
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
        expectObtainSendPreferencesSucceeds { sendPreferences }
        expectReadAttachmentsFromStorageSucceeds(attachmentIds, attachments)
        val messagePackages = expectGenerateMessagePackagesSucceeds(sendPreferences, attachments)
        expectSendOperationSucceeds(messagePackages)
        expectMessagePasswordDoesNotExist()
        expectMessageExpirationTimeDoesNotExist()

        // When
        sendMessage(userId, messageId)

        // Then
        coVerify(exactly = attachmentIds.size) { getAttachmentFiles(userId, messageId, any()) }
    }

    @Test
    fun `should not read attachments for non MIME messages`() = runTest {
        // Given
        val sendPreferences = generateSendPreferences(encrypt = true, sign = true, pgpScheme = PackageType.ProtonMail)

        expectFindLocalDraftSucceeds()
        expectResolveUserAddressSucceeds()
        expectObserveMailSettingsReturnsNull()
        expectObtainSendPreferencesSucceeds { sendPreferences }
        val messagePackages = expectGenerateMessagePackagesSucceeds(sendPreferences)
        expectSendOperationSucceeds(messagePackages)
        expectMessagePasswordDoesNotExist()
        expectMessageExpirationTimeDoesNotExist()

        // When
        sendMessage(userId, messageId)

        // Then
        coVerify(exactly = 0) { getAttachmentFiles(any(), any(), any()) }
    }

    @Test
    fun `when message packages fail to be generated, the error is propagated to the calling site`() = runTest {
        // Given
        val senderAddress = UserAddressSample.PrimaryAddress
        val sendPreferences = expectObtainSendPreferencesSucceeds { generateSendPreferences() }
        val expectedError = SendMessage.Error.GeneratingPackages.left()

        expectFindLocalDraftSucceeds()
        expectResolveUserAddressSucceeds()
        expectObserveMailSettingsReturnsNull()
        expectMessagePasswordDoesNotExist()
        coEvery {
            generateMessagePackages(
                senderAddress,
                sampleMessage,
                sendPreferences.forMessagePackages(),
                emptyMap(),
                null,
                null
            )
        } returns GenerateMessagePackages.Error.GeneratingPackages("generateMessagePackages exception").left()

        // When
        val result = sendMessage(userId, messageId)

        // Then
        assertEquals(expectedError, result)
    }

    @Test
    fun `when remote data source fails to send the message, the error is propagated to the calling site`() = runTest {
        // Given
        val sendPreferences = expectObtainSendPreferencesSucceeds { generateSendPreferences() }
        val expectedError = SendMessage.Error.SendingToApi(
            DataError.Remote.Proton(ProtonError.Unknown, "api msg")
        ).left()

        expectFindLocalDraftSucceeds()
        expectResolveUserAddressSucceeds()
        expectObserveMailSettingsReturnsNull()
        expectMessagePasswordDoesNotExist()
        expectMessageExpirationTimeDoesNotExist()
        val messagePackages = expectGenerateMessagePackagesSucceeds(sendPreferences)
        coEvery {
            messageRemoteDataSource.send(
                userId,
                messageId.id,
                generateSendMessageBody(messagePackages)
            )
        } returns DataError.Remote.Proton(ProtonError.fromProtonCode(503), "api msg").left()

        // When
        val result = sendMessage(userId, messageId)

        // Then
        assertEquals(expectedError, result)
    }

    @Test
    fun `when sending goes through, the success is propagated to the calling site`() = runTest {
        // Given
        val sendPreferences = expectObtainSendPreferencesSucceeds { generateSendPreferences() }
        expectFindLocalDraftSucceeds()
        expectResolveUserAddressSucceeds()
        expectObserveMailSettingsReturnsNull()
        val messagePackages = expectGenerateMessagePackagesSucceeds(sendPreferences)
        expectSendOperationSucceeds(messagePackages)
        expectMessagePasswordDoesNotExist()
        expectMessageExpirationTimeDoesNotExist()

        // When
        val result = sendMessage(userId, messageId)

        // Then
        assertEquals(Unit.right(), result)
    }

    @Test
    fun `when requiring send preference passing the same address multiple times only one preference is returned`() =
        runTest {
            // Given
            val recipient = Participant("duplicatedRecipient@pm.me", "name")
            val expectedMessage = expectFindLocalDraftSucceeds {
                generateDraftMessage(listOf(recipient), listOf(recipient))
            }
            val addresses = expectedMessage.message.run { toList + ccList + bccList }.map { it.address }
            val sendPreferences = expectObtainSendPreferencesSucceeds(recipientAddresses = addresses) {
                generateSendPreferences(emailAddress = recipient.address)
            }
            expectResolveUserAddressSucceeds(addressId = expectedMessage.message.addressId)
            expectObserveMailSettingsReturnsNull()
            val messagePackages = expectGenerateMessagePackagesSucceeds(
                sendPreferences = sendPreferences, expectedMessage = expectedMessage
            )
            expectSendOperationSucceeds(messagePackages)
            expectMessagePasswordDoesNotExist()
            expectMessageExpirationTimeDoesNotExist()

            // When
            val result = sendMessage(userId, messageId)

            // Then
            assertEquals(Unit.right(), result)
        }

    @Test
    fun `when requiring send preference passing the same canonical address multiple times only one pref is returned`() =
        runTest {
            // Given
            val recipientUppercase = Participant("DUPLICATEDRECIPIENT@pm.me", "name")
            val recipientLowercase = Participant("duplicatedrecipient@pm.me", "name")
            val expectedMessage = expectFindLocalDraftSucceeds {
                generateDraftMessage(listOf(recipientUppercase), listOf(recipientLowercase))
            }
            val addresses = expectedMessage.message.run { toList + ccList + bccList }.map { it.address }
            val sendPreferences = expectObtainSendPreferencesSucceeds(recipientAddresses = addresses) {
                generateSendPreferences(emailAddress = recipientLowercase.address)
            }
            expectResolveUserAddressSucceeds(addressId = expectedMessage.message.addressId)
            expectObserveMailSettingsReturnsNull()
            val messagePackages = expectGenerateMessagePackagesSucceeds(
                sendPreferences = sendPreferences, expectedMessage = expectedMessage
            )
            expectSendOperationSucceeds(messagePackages)
            expectMessagePasswordDoesNotExist()
            expectMessageExpirationTimeDoesNotExist()

            // When
            val result = sendMessage(userId, messageId)

            // Then
            assertEquals(Unit.right(), result)
        }

    @Test
    fun `when obtain send preferences throws an exception, the error is propagated to the calling site`() = runTest {
        // Given
        expectFindLocalDraftSucceeds()
        expectResolveUserAddressSucceeds()
        expectObserveMailSettingsReturnsNull()
        expectObtainSendPreferencesThrows(recipients) { Exception("Unexpected exception from core lib") }

        // When
        val result = sendMessage(userId, messageId)

        // Then
        assertEquals(SendMessage.Error.SendPreferences(emptyMap()).left(), result)
    }

    @Test
    fun `when message password exists, get random modulus`() = runTest {
        // Given
        val sendPreferences = expectObtainSendPreferencesSucceeds { generateSendPreferences() }
        expectFindLocalDraftSucceeds()
        expectResolveUserAddressSucceeds()
        expectObserveMailSettingsReturnsNull()
        val messagePackages = expectGenerateMessagePackagesSucceeds(sendPreferences, doesMessagePasswordExist = true)
        expectSendOperationSucceeds(messagePackages)
        expectMessagePasswordExists()
        expectGetSessionIdSucceeds()
        expectGetRandomModulusSucceeds()
        expectMessageExpirationTimeDoesNotExist()

        // When
        val result = sendMessage(userId, messageId)

        // Then
        assertEquals(Unit.right(), result)
        coVerify {
            accountRepository.getSessionIdOrNull(userId)
            authRepository.randomModulus(SendMessageSample.SessionId)
        }
    }

    @Test
    fun `when message expiration time is set, include it in the send message request body`() = runTest {
        // Given
        val messageExpiresInSeconds = SendMessageSample.MessageExpirationTime.expiresIn.inWholeSeconds
        val sendPreferences = expectObtainSendPreferencesSucceeds { generateSendPreferences() }
        expectFindLocalDraftSucceeds()
        expectResolveUserAddressSucceeds()
        expectObserveMailSettingsReturnsNull()
        val messagePackages = expectGenerateMessagePackagesSucceeds(sendPreferences)
        expectSendOperationSucceeds(messagePackages, messageExpiresInSeconds)
        expectMessagePasswordDoesNotExist()
        expectMessageExpirationTimeExists()

        // When
        val result = sendMessage(userId, messageId)

        // Then
        val sendMessageBody = generateSendMessageBody(messagePackages, messageExpiresInSeconds)
        coVerify { messageRemoteDataSource.send(userId, messageId.id, sendMessageBody) }
        assertEquals(Unit.right(), result)
    }

    private fun expectFindLocalDraftSucceeds(expected: () -> MessageWithBody = { sampleMessage }) = expected().also {
        coEvery { findLocalDraft.invoke(userId, messageId) } returns it
    }

    private fun expectResolveUserAddressSucceeds(
        userId: UserId = this.userId,
        addressId: AddressId = sampleMessage.message.addressId
    ) {
        coEvery { resolveUserAddress(userId, addressId) } returns UserAddressSample.PrimaryAddress.right()
    }

    private fun expectObserveMailSettingsReturnsNull() {
        coEvery { observeMailSettings.invoke(userId) } returns flowOf(null)
    }

    private fun expectObtainSendPreferencesThrows(
        recipientAddresses: List<String> = this.recipients,
        expected: () -> Exception
    ) = expected().also { exception ->
        coEvery { obtainSendPreferences(userId, recipientAddresses) } answers { throw exception }
    }

    private fun expectObtainSendPreferencesSucceeds(
        recipientAddresses: List<String> = this.recipients,
        expected: () -> Map<String, ObtainSendPreferences.Result>
    ) = expected().also {
        coEvery { obtainSendPreferences(userId, recipientAddresses) } returns it
    }

    private fun expectReadAttachmentsFromStorageSucceeds(
        attachmentIds: List<AttachmentId>,
        attachments: Map<AttachmentId, File>
    ) {
        coEvery {
            getAttachmentFiles(userId, messageId, attachmentIds)
        } returns attachments.right()
    }

    private fun expectGenerateMessagePackagesSucceeds(
        sendPreferences: Map<String, ObtainSendPreferences.Result>,
        attachments: Map<AttachmentId, File> = emptyMap(),
        expectedMessage: MessageWithBody = sampleMessage,
        doesMessagePasswordExist: Boolean = false
    ) = generateSendPackages(UserAddressSample.PrimaryAddress.email).also { messagePackages ->
        coEvery {
            generateMessagePackages(
                UserAddressSample.PrimaryAddress,
                expectedMessage,
                sendPreferences.forMessagePackages(),
                attachments,
                if (doesMessagePasswordExist) SendMessageSample.MessagePassword else null,
                if (doesMessagePasswordExist) SendMessageSample.Modulus else null
            )
        } returns messagePackages.right()
    }

    private fun expectSendOperationSucceeds(messagePackages: List<SendMessagePackage>, expiresIn: Long = 0) {
        coEvery {
            messageRemoteDataSource.send(
                userId,
                messageId.id,
                generateSendMessageBody(messagePackages, expiresIn)
            )
        } returns SendMessageResponse(200, mockk()).right()
    }

    private fun expectMessagePasswordExists() {
        coEvery { observeMessagePassword(userId, messageId) } returns flowOf(SendMessageSample.MessagePassword)
    }

    private fun expectMessagePasswordDoesNotExist() {
        coEvery { observeMessagePassword(userId, messageId) } returns flowOf(null)
    }

    private fun expectMessageExpirationTimeExists() {
        coEvery {
            observeMessageExpirationTime(userId, messageId)
        } returns flowOf(SendMessageSample.MessageExpirationTime)
    }

    private fun expectMessageExpirationTimeDoesNotExist() {
        coEvery { observeMessageExpirationTime(userId, messageId) } returns flowOf(null)
    }

    private fun expectGetSessionIdSucceeds() {
        coEvery { accountRepository.getSessionIdOrNull(userId) } returns SendMessageSample.SessionId
    }

    private fun expectGetRandomModulusSucceeds() {
        coEvery { authRepository.randomModulus(SendMessageSample.SessionId) } returns SendMessageSample.Modulus
    }

    private fun generateDraftMessage(
        toRecipients: List<Recipient>,
        ccRecipients: List<Recipient> = emptyList()
    ): MessageWithBody {
        val message = MessageSample.build(toList = toRecipients, ccList = ccRecipients)
        val messageBody = MessageBodyTestData.messageBodyWithAttachment
        return MessageWithBodySample.Invoice.copy(message = message, messageBody = messageBody)
    }

    private fun generateSendPreferences(
        encrypt: Boolean = true,
        sign: Boolean = true,
        pgpScheme: PackageType = PackageType.ProtonMail,
        mimeType: MimeType = MimeType.Html,
        publicKey: PublicKey? = null,
        emailAddress: String = baseRecipient.address
    ): Map<String, ObtainSendPreferences.Result> {
        return mapOf(
            Pair(
                emailAddress,
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

    private fun generateSendMessageBody(packages: List<SendMessagePackage>, expiresIn: Long = 0) = SendMessageBody(
        expiresIn = expiresIn,
        autoSaveContacts = false.toInt(),
        packages = packages
    )
}
