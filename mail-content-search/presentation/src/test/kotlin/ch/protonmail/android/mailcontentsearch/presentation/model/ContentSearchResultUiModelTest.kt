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

package ch.protonmail.android.mailcontentsearch.presentation.model

import kotlin.test.Test
import kotlin.test.assertEquals

class ContentSearchResultUiModelTest {

    @Test
    fun `should join the query words and the matched terms`() {
        // When
        val actual = buildHighlightText(query = "new  governmnt", searchMatchedTerms = listOf("government"))

        // Then
        assertEquals("new governmnt government", actual)
    }

    @Test
    fun `should drop terms that repeat a word ignoring case`() {
        // When
        val actual = buildHighlightText(query = "Policy", searchMatchedTerms = listOf("policy", "POLICY", "budget"))

        // Then
        assertEquals("Policy budget", actual)
    }

    @Test
    fun `should be empty without a query or matched terms`() {
        // When
        val actual = buildHighlightText(query = " ", searchMatchedTerms = emptyList())

        // Then
        assertEquals("", actual)
    }
}
