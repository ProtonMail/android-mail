/*
 * Copyright (c) 2026 Proton Technologies AG
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

package ch.protonmail.android.mailmessage.data.mapper

import ch.protonmail.android.mailcommon.data.mapper.LocalSearchMatchPosition
import ch.protonmail.android.mailmessage.domain.model.SearchMatchField
import ch.protonmail.android.mailmessage.domain.model.SearchMatchPosition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SearchMatchPositionMapperTest {

    @Test
    fun `should map each metadata attribute to its field`() {
        // Given
        val expected = mapOf(
            "subject" to SearchMatchField.Subject,
            "from" to SearchMatchField.From,
            "to" to SearchMatchField.To,
            "cc" to SearchMatchField.Cc,
            "bcc" to SearchMatchField.Bcc
        )

        // When
        val actual = expected.keys.associateWith { attribute ->
            LocalSearchMatchPosition(attribute, 0u, 0u, 1u).toSearchMatchPosition()?.field
        }

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `should keep the value index and offsets`() {
        // Given
        val local = LocalSearchMatchPosition("subject", 1u, 4u, 14u)

        // When
        val actual = local.toSearchMatchPosition()

        // Then
        assertEquals(SearchMatchPosition(SearchMatchField.Subject, valueIndex = 1, start = 4, end = 14), actual)
    }

    @Test
    fun `should drop positions outside the message metadata`() {
        // Given
        val local = LocalSearchMatchPosition("body", 0u, 0u, 5u)

        // When
        val actual = local.toSearchMatchPosition()

        // Then
        assertNull(actual)
    }
}
