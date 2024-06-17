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

package ch.protonmail.android.mailcontact.domain.usecase

import ch.protonmail.android.mailcontact.domain.extractProperty
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.test.runTest
import org.junit.Test

@Suppress("MaxLineLength")
class VCardUtilsTest {

    @Test
    fun `extracts GENDER property handling folded lines`() = runTest {
        // Given
        val vCardWithGenderPropertyAndFoldedLines = """
            BEGIN:VCARD
            VERSION:4.0
            PRODID:ez-vcard 0.11.3
            GENDER:aDamTsT random VALUE that is VERY LONG SO IT'S LONGER THAN IT FITS I
             NTO the very limited line length inside the vcard standard
            ANNIVERSARY:20240621
            END:VCARD
        """.trimIndent()

        val expectedGenderValue =
            "aDamTsT random VALUE that is VERY LONG SO IT'S LONGER THAN IT FITS INTO the very limited line length inside the vcard standard"

        // When
        val actual = vCardWithGenderPropertyAndFoldedLines.extractProperty("GENDER")

        // Then
        assertEquals(expectedGenderValue, actual)
    }

}
