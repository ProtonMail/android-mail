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
import ch.protonmail.android.mailcomposer.domain.repository.MessagePasswordRepository
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
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

    val userId = UserIdTestData.userId
    val messageId = MessageIdSample.NewDraftWithSubjectAndBody

    private val keyStoreCrypto = mockk<KeyStoreCrypto>()
    private val messagePasswordRepository = mockk<MessagePasswordRepository>()

    private val saveMessagePassword = SaveMessagePassword(keyStoreCrypto, messagePasswordRepository)

    @Test
    fun `should return unit when message password is encrypted and stored successfully`() = runTest {
        // Given
        val password = "password"
        val passwordHint = "password hint"
        val encryptedPassword = "encryptedPassword"
        every { keyStoreCrypto.encrypt(password) } returns encryptedPassword
        coEvery {
            messagePasswordRepository.saveMessagePassword(
                MessagePassword(userId, messageId, encryptedPassword, passwordHint)
            )
        } returns Unit.right()

        // When
        val actual = saveMessagePassword(userId, messageId, password, passwordHint)

        // Then
        assertEquals(Unit.right(), actual)
    }

    @Test
    fun `should return encryption error when password encryption fails`() = runTest {
        // Given
        val password = "password"
        val passwordHint = "password hint"
        every { keyStoreCrypto.encrypt(password) } throws CryptoException()

        // When
        val actual = saveMessagePassword(userId, messageId, password, passwordHint)

        // Then
        assertEquals(DataError.Local.EncryptionError.left(), actual)
    }

    @Test
    fun `should return error when saving of encrypted password fails`() = runTest {
        // Given
        val password = "password"
        val passwordHint = "password hint"
        val encryptedPassword = "encryptedPassword"
        every { keyStoreCrypto.encrypt(password) } returns encryptedPassword
        coEvery {
            messagePasswordRepository.saveMessagePassword(
                MessagePassword(userId, messageId, encryptedPassword, passwordHint)
            )
        } returns DataError.Local.Unknown.left()

        // When
        val actual = saveMessagePassword(userId, messageId, password, passwordHint)

        // Then
        assertEquals(DataError.Local.Unknown.left(), actual)
    }
}
