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

package ch.protonmail.android.mailsettings.presentation.accountsettings.identity.model

import ch.protonmail.android.mailsettings.domain.model.SignatureValue
import kotlin.test.Test
import kotlin.test.assertEquals

class SignatureValueExtensionKtTest {

    @Test
    fun `should add new lines where there are div tags in the signature`() {
        // Given
        val signatureValue = SignatureValue("<div>Signature first line</div><div>Signature second line</div>")

        // When
        val actual = signatureValue.toPlainText()

        // Then
        val expected = """
            Signature first line
            Signature second line
        """.trimIndent()
        assertEquals(expected, actual)
    }

    @Test
    fun `should not add new lines when div tags contain br tags inside`() {
        // Given
        val signatureValue = SignatureValue(
            "<div>Signature first line</div><div><br></div><div>Signature second line after empty line</div>"
        )

        // When
        val actual = signatureValue.toPlainText()

        // Then
        val expected = """
            Signature first line

            Signature second line after empty line
        """.trimIndent()
        assertEquals(expected, actual)
    }
}
