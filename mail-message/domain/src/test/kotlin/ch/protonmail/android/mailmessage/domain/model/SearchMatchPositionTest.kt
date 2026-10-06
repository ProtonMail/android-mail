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

package ch.protonmail.android.mailmessage.domain.model

import ch.protonmail.android.testdata.message.MessageTestData.buildMessage
import kotlin.test.Test
import kotlin.test.assertEquals

class SearchMatchPositionTest {

    @Test
    fun `should return the subject word at the fuzzy match position`() {
        // Given
        val message = buildMessage(id = "1", subject = "New government policy")

        // When
        val terms = message.searchMatchedTerms(listOf(position(SearchMatchField.Subject, start = 4, end = 14)))

        // Then
        assertEquals(listOf("government"), terms)
    }

    @Test
    fun `should read positions as UTF-8 byte offsets`() {
        // Given
        val message = buildMessage(id = "1", subject = "Café gouvernement")

        // When
        val terms = message.searchMatchedTerms(listOf(position(SearchMatchField.Subject, start = 6, end = 18)))

        // Then
        assertEquals(listOf("gouvernement"), terms)
    }

    @Test
    fun `should read the sender position in the sender address`() {
        // Given
        val message = buildMessage(id = "1", sender = Sender("alice@example.com", "Alice"))

        // When
        val terms = message.searchMatchedTerms(listOf(position(SearchMatchField.From, start = 0, end = 5)))

        // Then
        assertEquals(listOf("alice"), terms)
    }

    @Test
    fun `should read recipient positions in the comma joined addresses`() {
        // Given
        val message = buildMessage(
            id = "1",
            toList = listOf(Recipient("a@example.com", "A"), Recipient("b@example.com", "B"))
        )

        // When
        val terms = message.searchMatchedTerms(listOf(position(SearchMatchField.To, start = 15, end = 28)))

        // Then
        assertEquals(listOf("b@example.com"), terms)
    }

    @Test
    fun `should ignore positions outside the first value`() {
        // Given
        val message = buildMessage(id = "1", toList = listOf(Recipient("a@example.com", "A")))

        // When
        val terms = message.searchMatchedTerms(
            listOf(position(SearchMatchField.To, start = 0, end = 13, valueIndex = 1))
        )

        // Then
        assertEquals(emptyList(), terms)
    }

    @Test
    fun `should ignore empty and out of range positions`() {
        // Given
        val message = buildMessage(id = "1", subject = "Short")

        // When
        val terms = message.searchMatchedTerms(
            listOf(
                position(SearchMatchField.Subject, start = 2, end = 40),
                position(SearchMatchField.Subject, start = 3, end = 3),
                position(SearchMatchField.Subject, start = -1, end = 2)
            )
        )

        // Then
        assertEquals(emptyList(), terms)
    }

    private fun position(
        field: SearchMatchField,
        start: Int,
        end: Int,
        valueIndex: Int = 0
    ) = SearchMatchPosition(field = field, valueIndex = valueIndex, start = start, end = end)
}
