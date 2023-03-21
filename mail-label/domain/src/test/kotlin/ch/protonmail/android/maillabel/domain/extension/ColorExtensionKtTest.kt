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

package ch.protonmail.android.maillabel.domain.extension

import org.junit.Assert.assertEquals
import org.junit.Test

class ColorExtensionKtTest {

    @Test
    fun `verify that short hex is converted to hex format`() {
        // Given
        val actual = "#f09"

        // When
        val result = actual.normalizeColorHex()

        // Then
        assertEquals("#ff0099", result)
    }

    @Test
    fun `verify that hex is stays hex`() {
        // Given
        val actual = "#ff0099"

        // When
        val result = actual.normalizeColorHex()

        // Then
        assertEquals("#ff0099", result)
    }
}
