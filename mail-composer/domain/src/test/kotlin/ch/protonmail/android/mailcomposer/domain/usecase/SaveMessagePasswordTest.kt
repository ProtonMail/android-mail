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

package ch.protonmail.android.mailcomposer.domain.usecase

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcomposer.domain.model.MessagePassword
import ch.protonmail.android.mailcomposer.domain.model.SenderEmail
import ch.protonmail.android.mailcomposer.domain.repository.MessagePasswordRepository
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.mailmessage.domain.sample.MessageWithBodySample
import ch.protonmail.android.test.utils.FakeTransactor
import ch.protonmail.android.testdata.user.UserIdTestData
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.crypto.common.keystore.KeyStoreCrypto
import me.proton.core.crypto.common.pgp.exception.CryptoException
import kotlin.test.Test
import kotlin.test.assertEquals

class SaveMessagePasswordTest {

    private val userId = UserIdTestData.userId
    private val messageId = MessageIdSample.NewDraftWithSubjectAndBody
    private val senderEmail = SenderEmail("sender@pm.me")

    private val getLocalDraft = mockk<GetLocalDraft>()
    private val keyStoreCrypto = mockk<KeyStoreCrypto>()
    private val messagePasswordRepository = mockk<MessagePasswordRepository>()
    private val messageRepository = mockk<MessageRepository>()
    private val saveDraft = mockk<SaveDraft>()
    private val transactor = FakeTransactor()

    private val saveMessagePassword = SaveMessagePassword(
        getLocalDraft = getLocalDraft,
        keyStoreCrypto = keyStoreCrypto,
        messagePasswordRepository = messagePasswordRepository,
        messageRepository = messageRepository,
        saveDraft = saveDraft,
        transactor = transactor
    )

    @Test
    fun `should return unit when draft exists and message password is encrypted and stored successfully`() = runTest {
        // Given
        val password = "password"
        val passwordHint = "password hint"
        val encryptedPassword = "encryptedPassword"
        expectDraftAlreadyExists()
        every { keyStoreCrypto.encrypt(password) } returns encryptedPassword
        coEvery {
            messagePasswordRepository.saveMessagePassword(
                MessagePassword(
                    userId,
                    MessageWithBodySample.EmptyDraft.message.messageId,
                    encryptedPassword,
                    passwordHint
                )
            )
        } returns Unit.right()

        // When
        val actual = saveMessagePassword(userId, messageId, senderEmail, password, passwordHint)

        // Then
        assertEquals(Unit.right(), actual)
    }

    @Test
    fun `should return unit when draft is saved and message password is encrypted and stored successfully`() = runTest {
        // Given
        val password = "password"
        val passwordHint = "password hint"
        val encryptedPassword = "encryptedPassword"
        expectDraftDoesNotExist()
        coEvery { saveDraft(MessageWithBodySample.EmptyDraft, userId) } returns true
        every { keyStoreCrypto.encrypt(password) } returns encryptedPassword
        coEvery {
            messagePasswordRepository.saveMessagePassword(
                MessagePassword(
                    userId,
                    MessageWithBodySample.EmptyDraft.message.messageId,
                    encryptedPassword,
                    passwordHint
                )
            )
        } returns Unit.right()

        // When
        val actual = saveMessagePassword(userId, messageId, senderEmail, password, passwordHint)

        // Then
        assertEquals(Unit.right(), actual)
    }

    @Test
    fun `should return error when getting the local draft has failed `() = runTest {
        // Given
        val password = "password"
        val passwordHint = "password hint"
        coEvery {
            getLocalDraft(userId, messageId, senderEmail)
        } returns GetLocalDraft.Error.ResolveUserAddressError.left()

        // When
        val actual = saveMessagePassword(userId, messageId, senderEmail, password, passwordHint)

        // Then
        assertEquals(DataError.Local.NoDataCached.left(), actual)
    }

    @Test
    fun `should return error when saving draft has failed`() = runTest {
        // Given
        val password = "password"
        val passwordHint = "password hint"
        expectDraftDoesNotExist()
        coEvery { saveDraft(MessageWithBodySample.EmptyDraft, userId) } returns false

        // When
        val actual = saveMessagePassword(userId, messageId, senderEmail, password, passwordHint)

        // Then
        assertEquals(DataError.Local.Unknown.left(), actual)
    }

    @Test
    fun `should return encryption error when password encryption fails`() = runTest {
        // Given
        val password = "password"
        val passwordHint = "password hint"
        expectDraftAlreadyExists()
        every { keyStoreCrypto.encrypt(password) } throws CryptoException()

        // When
        val actual = saveMessagePassword(userId, messageId, senderEmail, password, passwordHint)

        // Then
        assertEquals(DataError.Local.EncryptionError.left(), actual)
    }

    @Test
    fun `should return error when saving of encrypted password fails`() = runTest {
        // Given
        val password = "password"
        val passwordHint = "password hint"
        val encryptedPassword = "encryptedPassword"
        expectDraftAlreadyExists()
        every { keyStoreCrypto.encrypt(password) } returns encryptedPassword
        coEvery {
            messagePasswordRepository.saveMessagePassword(
                MessagePassword(
                    userId,
                    MessageWithBodySample.EmptyDraft.message.messageId,
                    encryptedPassword,
                    passwordHint
                )
            )
        } returns DataError.Local.Unknown.left()

        // When
        val actual = saveMessagePassword(userId, messageId, senderEmail, password, passwordHint)

        // Then
        assertEquals(DataError.Local.Unknown.left(), actual)
    }

    @Test
    fun `should return unit when message password is encrypted and updated successfully`() = runTest {
        // Given
        val password = "password"
        val passwordHint = "password hint"
        val encryptedPassword = "encryptedPassword"
        expectDraftAlreadyExists()
        every { keyStoreCrypto.encrypt(password) } returns encryptedPassword
        coEvery {
            messagePasswordRepository.updateMessagePassword(
                userId, MessageWithBodySample.EmptyDraft.message.messageId, encryptedPassword, passwordHint
            )
        } returns Unit.right()

        // When
        val actual = saveMessagePassword(
            userId, messageId, senderEmail, password, passwordHint, SaveMessagePasswordAction.Update
        )

        // Then
        assertEquals(Unit.right(), actual)
    }

    @Test
    fun `should return error when updating of encrypted password fails`() = runTest {
        // Given
        val password = "password"
        val passwordHint = "password hint"
        val encryptedPassword = "encryptedPassword"
        expectDraftAlreadyExists()
        every { keyStoreCrypto.encrypt(password) } returns encryptedPassword
        coEvery {
            messagePasswordRepository.updateMessagePassword(
                userId, MessageWithBodySample.EmptyDraft.message.messageId, encryptedPassword, passwordHint
            )
        } returns DataError.Local.Unknown.left()

        // When
        val actual = saveMessagePassword(
            userId, messageId, senderEmail, password, passwordHint, SaveMessagePasswordAction.Update
        )

        // Then
        assertEquals(DataError.Local.Unknown.left(), actual)
    }

    private fun expectDraftAlreadyExists() {
        coEvery { getLocalDraft(userId, messageId, senderEmail) } returns MessageWithBodySample.EmptyDraft.right()
        coEvery {
            messageRepository.getLocalMessageWithBody(userId, MessageWithBodySample.EmptyDraft.message.messageId)
        } returns MessageWithBodySample.EmptyDraft
    }

    private fun expectDraftDoesNotExist() {
        coEvery { getLocalDraft(userId, messageId, senderEmail) } returns MessageWithBodySample.EmptyDraft.right()
        coEvery {
            messageRepository.getLocalMessageWithBody(userId, MessageWithBodySample.EmptyDraft.message.messageId)
        } returns null
    }
}
