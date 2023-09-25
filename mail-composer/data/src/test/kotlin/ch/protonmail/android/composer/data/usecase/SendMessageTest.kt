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
import ch.protonmail.android.mailmessage.domain.model.MessageWithBody
import ch.protonmail.android.mailmessage.domain.model.Participant
import ch.protonmail.android.mailmessage.domain.model.Recipient
import ch.protonmail.android.mailmessage.domain.sample.MessageSample
import ch.protonmail.android.mailmessage.domain.sample.MessageWithBodySample
import ch.protonmail.android.mailsettings.domain.usecase.ObserveMailSettings
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.mailsendpreferences.domain.model.SendPreferences
import me.proton.core.mailsendpreferences.domain.usecase.ObtainSendPreferences
import me.proton.core.mailsettings.domain.entity.MimeType
import me.proton.core.mailsettings.domain.entity.PackageType
import me.proton.core.util.kotlin.toInt
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class SendMessageTest {

    private val messageRemoteDataSource = mockk<MessageRemoteDataSource>()
    private val resolveUserAddress = mockk<ResolveUserAddress>()
    private val generateMessagePackages = mockk<GenerateMessagePackages>()
    private val findLocalDraft = mockk<FindLocalDraft>()
    private val obtainSendPreferences = mockk<ObtainSendPreferences>()
    private val observeMailSettings = mockk<ObserveMailSettings>()

    private val sendMessage = SendMessage(
        messageRemoteDataSource,
        resolveUserAddress,
        generateMessagePackages,
        findLocalDraft,
        obtainSendPreferences,
        observeMailSettings
    )

    private val userId = UserAddressSample.PrimaryAddress.userId
    private val baseRecipient = Participant("address@proton.me", "name")
    private val sampleMessage = generateDraftMessage(recipients = listOf(baseRecipient))
    private val messageId = sampleMessage.message.messageId
    private val recipients = sampleMessage.message.run { toList + ccList + bccList }.map { it.address }

    @After
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
        coEvery { findLocalDraft.invoke(userId, messageId) } answers { sampleMessage }
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
        coEvery { findLocalDraft.invoke(userId, messageId) } answers { sampleMessage }
        coEvery {
            resolveUserAddress(userId, sampleMessage.message.addressId)
        } answers { UserAddressSample.PrimaryAddress.right() }
        coEvery { observeMailSettings.invoke(userId) } answers { flowOf(null) }
        coEvery { obtainSendPreferences(userId, recipients) } answers {
            mapOf(Pair(baseRecipient.address, ObtainSendPreferences.Result.Error.AddressDisabled))
        }

        // When
        val result = sendMessage(userId, messageId)

        // Then
        assertEquals(DataError.MessageSending.SendPreferences.left(), result)
    }

    @Test
    fun `when message packages fail to be generated, the error is propagated to the calling site`() = runTest {
        // Given
        val senderAddress = UserAddressSample.PrimaryAddress
        val sendPreferences = generateSendPreferences()
        val expectedError = DataError.MessageSending.GeneratingPackages.left()

        coEvery { findLocalDraft.invoke(userId, messageId) } answers { sampleMessage }
        coEvery { resolveUserAddress(userId, sampleMessage.message.addressId) } answers { senderAddress.right() }
        coEvery { observeMailSettings.invoke(userId) } answers { flowOf(null) }
        coEvery { obtainSendPreferences(userId, recipients) } answers { sendPreferences }
        coEvery {
            generateMessagePackages(
                senderAddress,
                sampleMessage,
                sendPreferences.forMessagePackages()
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
        val senderAddress = UserAddressSample.PrimaryAddress
        val sendPreferences = generateSendPreferences()
        val messagePackages = generateSendPackages(senderAddress.email)
        val expectedError = DataError.Remote.Proton(ProtonError.fromProtonCode(503)).left()

        coEvery { findLocalDraft.invoke(userId, messageId) } answers { sampleMessage }
        coEvery { resolveUserAddress(userId, sampleMessage.message.addressId) } answers { senderAddress.right() }
        coEvery { observeMailSettings.invoke(userId) } answers { flowOf(null) }
        coEvery { obtainSendPreferences(userId, recipients) } answers { sendPreferences }
        coEvery {
            generateMessagePackages(
                senderAddress,
                sampleMessage,
                sendPreferences.forMessagePackages()
            )
        } answers { messagePackages.right() }
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
        val senderAddress = UserAddressSample.PrimaryAddress
        val sendPreferences = generateSendPreferences()
        val messagePackages = generateSendPackages(senderAddress.email)
        val mockedSuccess = SendMessageResponse(200, mockk()).right()
        val expectedResult = Unit.right()

        coEvery { findLocalDraft.invoke(userId, messageId) } answers { sampleMessage }
        coEvery { resolveUserAddress(userId, sampleMessage.message.addressId) } answers { senderAddress.right() }
        coEvery { observeMailSettings.invoke(userId) } answers { flowOf(null) }
        coEvery { obtainSendPreferences(userId, recipients) } answers { sendPreferences }
        coEvery {
            generateMessagePackages(
                senderAddress,
                sampleMessage,
                sendPreferences.forMessagePackages()
            )
        } answers { messagePackages.right() }
        coEvery {
            messageRemoteDataSource.send(
                userId,
                messageId.id,
                generateSendMessageBody(messagePackages)
            )
        } answers { mockedSuccess }

        // When
        val result = sendMessage(userId, messageId)

        // Then
        assertEquals(expectedResult, result)
    }


    private fun generateDraftMessage(recipients: List<Recipient>): MessageWithBody {
        val message = MessageSample.build(toList = recipients)
        return MessageWithBodySample.Invoice.copy(message = message)
    }

    private fun generateSendPreferences(): Map<String, ObtainSendPreferences.Result> {
        return mapOf(
            Pair(
                baseRecipient.address,
                ObtainSendPreferences.Result.Success(
                    SendPreferences(
                        encrypt = false,
                        sign = false,
                        pgpScheme = PackageType.PgpMime,
                        mimeType = MimeType.Html,
                        publicKey = null
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
