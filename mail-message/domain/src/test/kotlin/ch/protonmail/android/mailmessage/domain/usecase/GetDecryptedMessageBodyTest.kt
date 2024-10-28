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

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailmessage.domain.model.AttachmentId
import ch.protonmail.android.mailmessage.domain.model.DecryptedMessageBody
import ch.protonmail.android.mailmessage.domain.model.GetDecryptedMessageBodyError
import ch.protonmail.android.mailmessage.domain.model.MessageAttachment
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.MessageWithBody
import ch.protonmail.android.mailmessage.domain.model.MimeType
import ch.protonmail.android.mailmessage.domain.repository.AttachmentRepository
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import ch.protonmail.android.testdata.message.MessageBodyTestData
import ch.protonmail.android.testdata.message.MessageTestData
import ch.protonmail.android.testdata.user.UserIdTestData
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import me.proton.core.crypto.common.context.CryptoContext
import me.proton.core.crypto.common.keystore.EncryptedByteArray
import me.proton.core.crypto.common.keystore.PlainByteArray
import me.proton.core.crypto.common.pgp.DecryptedMimeAttachment
import me.proton.core.crypto.common.pgp.DecryptedMimeBody
import me.proton.core.crypto.common.pgp.DecryptedMimeMessage
import me.proton.core.crypto.common.pgp.PGPCrypto
import me.proton.core.crypto.common.pgp.decryptTextOrNull
import me.proton.core.crypto.common.pgp.exception.CryptoException
import me.proton.core.key.domain.entity.key.PrivateKey
import me.proton.core.user.domain.UserAddressManager
import me.proton.core.user.domain.entity.UserAddress
import me.proton.core.user.domain.entity.UserAddressKey
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
class GetDecryptedMessageBodyTest(
    private val testName: String,
    private val testInput: TestInput
) {

    private val messageId = MessageId("messageId")
    private val decryptedMessageBody = "Decrypted message body."
    private val armoredPrivateKey = "armoredPrivateKey"
    private val armoredPublicKey = "armoredPublicKey"
    private val encryptedPassphrase = EncryptedByteArray("encryptedPassphrase".encodeToByteArray())
    private val decryptedPassphrase = PlainByteArray("decryptedPassPhrase".encodeToByteArray())
    private val unlockedPrivateKey = "unlockedPrivateKey".encodeToByteArray()

    private val pgpCryptoMock = mockk<PGPCrypto> {
        every { getPublicKey(armoredPrivateKey) } returns armoredPublicKey
        every { unlock(armoredPrivateKey, decryptedPassphrase.array) } returns mockk(relaxUnitFun = true) {
            every { value } returns unlockedPrivateKey
        }
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
    private val userAddressManager = mockk<UserAddressManager>()
    private val parseMimeAttachmentHeaders = mockk<ParseMimeAttachmentHeaders> {
        every { this@mockk.invoke(decryptedMimeAttachment.headers) } returns parsedMimeAttachmentHeaders
    }
    private val provideNewAttachmentId = mockk<ProvideNewAttachmentId> {
        every { this@mockk.invoke() } returns mimeAttachmentId
    }
    private val attachmentRepository = mockk<AttachmentRepository> {
        coEvery {
            saveMimeAttachment(
                UserIdTestData.userId,
                MessageBodyTestData.multipartMixedMessageBody.messageId,
                mimeAttachmentId,
                mimeAttachmentContent,
                mimeMessageAttachment
            )
        } returns Unit.right()
    }
    private val messageRepository = mockk<MessageRepository>()
    private val testDispatcher: TestDispatcher by lazy { StandardTestDispatcher() }

    private val getDecryptedMessageBody = GetDecryptedMessageBody(
        attachmentRepository,
        cryptoContext,
        messageRepository,
        parseMimeAttachmentHeaders,
        provideNewAttachmentId,
        userAddressManager,
        testDispatcher
    )

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `when repository gets message body and decryption is successful then the decrypted message body is returned`() =
        runTest {
            // Given
            val expectedUserAddress = mockGetUserAddressSucceeds()
            mockDecryptionIsSuccessful()
            val expected = DecryptedMessageBody(
                testInput.messageWithBody.message.messageId,
                decryptedMessageBody,
                testInput.mimeType,
                testInput.mimeAttachments,
                expectedUserAddress
            ).right()
            coEvery {
                messageRepository.getMessageWithBody(UserIdTestData.userId, messageId)
            } returns testInput.messageWithBody.right()

            // When
            val actual = getDecryptedMessageBody(UserIdTestData.userId, messageId)

            // Then
            assertEquals(expected, actual, testName)
        }

    @Test
    fun `when repository gets message body and decryption has failed then a decryption error is returned`() = runTest {
        // Given
        val expected = GetDecryptedMessageBodyError.Decryption(
            messageId, MessageBodyTestData.RAW_ENCRYPTED_MESSAGE_BODY
        ).left()
        coEvery {
            messageRepository.getMessageWithBody(UserIdTestData.userId, messageId)
        } returns testInput.messageWithBody.right()
        mockGetUserAddressSucceeds()
        mockDecryptionFails()

        // When
        val actual = getDecryptedMessageBody(UserIdTestData.userId, messageId)

        // Then
        assertEquals(expected, actual, testName)
    }

    @Test
    fun `when repository gets message body and user address is null then a decryption error is returned`() = runTest {
        // Given
        val expected = GetDecryptedMessageBodyError.Decryption(
            messageId,
            MessageBodyTestData.RAW_ENCRYPTED_MESSAGE_BODY
        ).left()
        coEvery {
            messageRepository.getMessageWithBody(UserIdTestData.userId, messageId)
        } returns testInput.messageWithBody.right()
        expectGetUserAddressFails()

        // When
        val actual = getDecryptedMessageBody(UserIdTestData.userId, messageId)

        // Then
        assertEquals(expected, actual, testName)
    }

    @Test
    fun `when repository method returns an error then the use case returns the error`() = runTest {
        // Given
        val expected = GetDecryptedMessageBodyError.Data(DataError.Local.NoDataCached).left()
        coEvery {
            messageRepository.getMessageWithBody(UserIdTestData.userId, messageId)
        } returns DataError.Local.NoDataCached.left()

        // When
        val actual = getDecryptedMessageBody(UserIdTestData.userId, messageId)

        // Then
        assertEquals(expected, actual, testName)
    }

    private fun mockDecryptionIsSuccessful() {
        if (testInput.mimeType == MimeType.MultipartMixed) {
            every {
                pgpCryptoMock.decryptMimeMessage(
                    message = MessageBodyTestData.RAW_ENCRYPTED_MESSAGE_BODY,
                    unlockedKeys = any()
                )
            } returns DecryptedMimeMessage(
                emptyList(),
                DecryptedMimeBody(testInput.mimeType.value, decryptedMessageBody),
                listOf(decryptedMimeAttachment)
            )
        } else {
            every {
                pgpCryptoMock.decryptTextOrNull(
                    message = MessageBodyTestData.RAW_ENCRYPTED_MESSAGE_BODY,
                    unlockedKey = any()
                )
            } returns decryptedMessageBody
        }
    }

    private fun mockGetUserAddressSucceeds(): UserAddress {
        val expectedUserAddress = mockk<UserAddress> {
            every { keys } returns listOf(userAddressKey)
        }
        coEvery {
            userAddressManager.getAddress(UserIdTestData.userId, MessageTestData.message.addressId)
        } returns expectedUserAddress
        return expectedUserAddress
    }

    private fun expectGetUserAddressFails() {
        coEvery {
            userAddressManager.getAddress(UserIdTestData.userId, MessageTestData.message.addressId)
        } returns null
    }

    private fun mockDecryptionFails() {
        if (testInput.mimeType == MimeType.MultipartMixed) {
            every {
                pgpCryptoMock.decryptMimeMessage(
                    message = MessageBodyTestData.RAW_ENCRYPTED_MESSAGE_BODY,
                    unlockedKeys = any()
                )
            } throws CryptoException()
        } else {
            every {
                pgpCryptoMock.decryptTextOrNull(
                    message = MessageBodyTestData.RAW_ENCRYPTED_MESSAGE_BODY,
                    unlockedKey = any()
                )
            } throws CryptoException()
        }
    }

    companion object {

        private val mimeAttachmentId = AttachmentId("attachmentId")
        private const val mimeAttachmentHeaders = "mimeAttachmentHeaders"
        private val mimeAttachmentContent = "mimeAttachmentContent".encodeToByteArray()
        private val decryptedMimeAttachment = DecryptedMimeAttachment(mimeAttachmentHeaders, mimeAttachmentContent)
        private val mimeMessageAttachment = MessageAttachment(
            attachmentId = mimeAttachmentId,
            name = "image.png",
            size = mimeAttachmentContent.size.toLong(),
            mimeType = "image/png",
            disposition = "attachment",
            keyPackets = null,
            signature = null,
            encSignature = null,
            headers = emptyMap()
        )
        private val parsedMimeAttachmentHeaders = JsonObject(
            mapOf(
                "Content-Disposition" to JsonPrimitive("attachment"),
                "filename" to JsonPrimitive("image.png"),
                "Content-Transfer-Encoding" to JsonPrimitive("base64"),
                "Content-Type" to JsonPrimitive("image/png"),
                "name" to JsonPrimitive("image.png")
            )
        )

        private val testInputList = listOf(
            TestInput(
                MimeType.Html,
                MessageWithBody(MessageTestData.message, MessageBodyTestData.htmlMessageBody)
            ),
            TestInput(
                MimeType.MultipartMixed,
                MessageWithBody(MessageTestData.message, MessageBodyTestData.multipartMixedMessageBody),
                listOf(mimeMessageAttachment)
            )
        )

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): Collection<Array<Any>> {
            return testInputList
                .map { testInput ->
                    val testName = """
                        Message type: ${testInput.mimeType}
                    """.trimIndent()
                    arrayOf(testName, testInput)
                }
        }
    }

    data class TestInput(
        val mimeType: MimeType,
        val messageWithBody: MessageWithBody,
        val mimeAttachments: List<MessageAttachment> = emptyList()
    )
}
