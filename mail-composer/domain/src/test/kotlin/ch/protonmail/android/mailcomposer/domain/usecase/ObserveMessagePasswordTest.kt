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

import app.cash.turbine.test
import arrow.core.right
import ch.protonmail.android.mailcomposer.domain.model.MessagePassword
import ch.protonmail.android.mailcomposer.domain.repository.MessagePasswordRepository
import ch.protonmail.android.mailmessage.domain.model.DraftAction
import ch.protonmail.android.mailmessage.domain.model.DraftState
import ch.protonmail.android.mailmessage.domain.model.DraftSyncState
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.repository.DraftStateRepository
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.test.utils.FakeTransactor
import ch.protonmail.android.testdata.user.UserIdTestData
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.crypto.common.keystore.KeyStoreCrypto
import me.proton.core.crypto.common.pgp.exception.CryptoException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ObserveMessagePasswordTest {

    private val userId = UserIdTestData.userId
    private val messageId = MessageIdSample.NewDraftWithSubjectAndBody
    private val apiMessageId = MessageId("apiMessageId")

    private val draftStateRepository = mockk<DraftStateRepository>()
    private val keyStoreCrypto = mockk<KeyStoreCrypto>()
    private val messagePasswordRepository = mockk<MessagePasswordRepository>()
    private val transactor = FakeTransactor()

    private val observeMessagePassword = ObserveMessagePassword(
        draftStateRepository = draftStateRepository,
        keyStoreCrypto = keyStoreCrypto,
        messagePasswordRepository = messagePasswordRepository,
        transactor = transactor
    )

    @Test
    fun `should return decrypted password for message id when password is emitted and decryption is successful`() =
        runTest {
            // Given
            val encryptedPassword = "encryptedPassword"
            val decryptedPassword = "decryptedPassword"
            val hint = "hint"
            val messagePassword = MessagePassword(userId, messageId, encryptedPassword, hint)
            expectApiMessageIdDoesNotExist()
            coEvery {
                messagePasswordRepository.observeMessagePassword(userId, messageId)
            } returns flowOf(messagePassword)
            every { keyStoreCrypto.decrypt(encryptedPassword) } returns decryptedPassword

            // When
            observeMessagePassword(userId, messageId).test {
                // Then
                val expected = messagePassword.copy(password = decryptedPassword)
                assertEquals(expected, awaitItem())
                awaitComplete()
            }
        }

    @Test
    fun `should return decrypted password for api message id when password is emitted and decryption is successful`() =
        runTest {
            // Given
            val encryptedPassword = "encryptedPassword"
            val decryptedPassword = "decryptedPassword"
            val hint = "hint"
            val messagePassword = MessagePassword(userId, apiMessageId, encryptedPassword, hint)
            expectApiMessageIdExists()
            coEvery {
                messagePasswordRepository.observeMessagePassword(userId, apiMessageId)
            } returns flowOf(messagePassword)
            every { keyStoreCrypto.decrypt(encryptedPassword) } returns decryptedPassword

            // When
            observeMessagePassword(userId, messageId).test {
                // Then
                val expected = messagePassword.copy(password = decryptedPassword)
                assertEquals(expected, awaitItem())
                awaitComplete()
            }
        }

    @Test
    fun `should return null when password is emitted but was not decrypted successfully`() = runTest {
        // Given
        val encryptedPassword = "encryptedPassword"
        val hint = "hint"
        val messagePassword = MessagePassword(userId, messageId, encryptedPassword, hint)
        expectApiMessageIdDoesNotExist()
        coEvery { messagePasswordRepository.observeMessagePassword(userId, messageId) } returns flowOf(messagePassword)
        every { keyStoreCrypto.decrypt(encryptedPassword) } throws CryptoException()

        // When
        observeMessagePassword(userId, messageId).test {
            // Then
            assertNull(awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `should return null when message password does not exist`() = runTest {
        // Given
        expectApiMessageIdDoesNotExist()
        coEvery { messagePasswordRepository.observeMessagePassword(userId, messageId) } returns flowOf(null)

        // When
        observeMessagePassword(userId, messageId).test {
            // Then
            assertNull(awaitItem())
            awaitComplete()
        }
    }

    private fun expectApiMessageIdExists() {
        coEvery {
            draftStateRepository.observe(userId, messageId)
        } returns flowOf(
            DraftState(
                userId = userId,
                messageId = messageId,
                apiMessageId = apiMessageId,
                state = DraftSyncState.Synchronized,
                action = DraftAction.Compose,
                sendingError = null,
                sendingStatusConfirmed = false
            ).right()
        )
    }

    private fun expectApiMessageIdDoesNotExist() {
        coEvery {
            draftStateRepository.observe(userId, messageId)
        } returns flowOf(
            DraftState(
                userId = userId,
                messageId = messageId,
                apiMessageId = null,
                state = DraftSyncState.Synchronized,
                action = DraftAction.Compose,
                sendingError = null,
                sendingStatusConfirmed = false
            ).right()
        )
    }
}
