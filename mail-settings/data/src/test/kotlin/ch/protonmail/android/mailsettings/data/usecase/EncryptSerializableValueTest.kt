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

package ch.protonmail.android.mailsettings.data.usecase

import java.io.IOException
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import me.proton.core.crypto.common.keystore.KeyStoreCrypto
import org.junit.After
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

internal class EncryptSerializableValueTest {

    private val keyStoreCrypto = mockk<KeyStoreCrypto>()
    private val encryptSerializableValue = EncryptSerializableValue(keyStoreCrypto)

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `when serialization and encryption succeed, no error is propagated`() = runTest {
        // Given
        every { keyStoreCrypto.encrypt(DecryptedString) } returns EncryptedString

        // When
        val actual = encryptSerializableValue(toEncrypt)

        // Then
        assertEquals(EncryptedString.right(), actual)
    }

    @Test
    fun `when encryption fails and throws then a generic encoding error is returned`() = runTest {
        // Given
        val expectedError = EncryptSerializableValue.EncodingError("this is an error")
        every { keyStoreCrypto.encrypt(DecryptedString) } throws IOException(expectedError.message)

        // When
        val actual = encryptSerializableValue(toEncrypt)

        // Then
        assertEquals(actual, expectedError.left())
    }

    @Test
    fun `when encoding fails then a generic encoding error is returned`() = runTest {
        // Given
        val toEncrypt = NonSerializableClass("some random value")

        // When
        val actual = encryptSerializableValue(toEncrypt)

        // Then
        assertIs<Either.Left<EncryptSerializableValue.EncodingError>>(actual)
    }

    private companion object {

        val toEncrypt = ToEncrypt("DecryptedString")
        const val EncryptedString = "EncryptedString"
        const val DecryptedString = "\"DecryptedString\""
    }

    @JvmInline
    @Serializable
    private value class ToEncrypt(val value: String)

    @JvmInline
    private value class NonSerializableClass(val value: String)
}
