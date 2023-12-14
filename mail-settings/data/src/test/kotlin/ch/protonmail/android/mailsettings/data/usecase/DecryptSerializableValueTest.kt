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

internal class DecryptSerializableValueTest {

    private val keyStoreCrypto = mockk<KeyStoreCrypto>()
    private val decryptSerializableValue = DecryptSerializableValue(keyStoreCrypto)

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `when decryption and deserialization succeed, the value is returned`() = runTest {
        // Given
        val expectedValue = ToDecrypt("DecryptedString")
        every { keyStoreCrypto.decrypt(StringToDecrypt) } returns DecryptedString

        // When
        val actual = decryptSerializableValue<ToDecrypt>(StringToDecrypt)

        // Then
        assertEquals(actual, expectedValue.right())
    }

    @Test
    fun `when decryption fails and throws then a generic decoding error is returned`() = runTest {
        // Given
        val expectedError = DecryptSerializableValue.DecodingError("this is an error")
        every { keyStoreCrypto.decrypt(StringToDecrypt) } throws IOException(expectedError.message)

        // When
        val actual = decryptSerializableValue<String>(StringToDecrypt)

        // Then
        assertEquals(actual, expectedError.left())
    }

    @Test
    fun `when decoding fails then a generic decoding error is returned`() = runTest {
        // Given
        every { keyStoreCrypto.decrypt(StringToDecrypt) } returns DecryptedString

        // When
        val actual = decryptSerializableValue<Int>(StringToDecrypt)

        // Then
        assertIs<Either.Left<DecryptSerializableValue.DecodingError>>(actual)
    }

    private companion object {

        const val StringToDecrypt = "StringToDecrypt"
        const val DecryptedString = "\"DecryptedString\""
    }

    @JvmInline
    @Serializable
    private value class ToDecrypt(val value: String)
}
