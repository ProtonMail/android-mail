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

package ch.protonmail.android.mailmessage.domain.model.attachments.header

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class HeaderValueConverterTest {

    private val converter = HeaderValueConverter()

    @Test
    fun `convertToString with string values`() {
        // Given
        val headers = mapOf("Header1" to HeaderValue.StringValue("value1"))

        // When
        val serialized = converter.convertToString(headers)

        // Then
        assertEquals("{\"Header1\":\"value1\"}", serialized)
    }

    @Test
    fun `convertToString with list values`() {
        // Given
        val headers = mapOf("Header1" to HeaderValue.ListValue(listOf("item1", "item2")))

        // When
        val serialized = converter.convertToString(headers)

        // Then
        assertEquals("{\"Header1\":[\"item1\",\"item2\"]}", serialized)
    }

    @Test
    fun `convertToMap with string values`() {
        // Given
        val jsonString = "{\"Header1\":\"value1\"}"

        // When
        val deserialized = converter.convertToMap(jsonString)
        val header = deserialized["Header1"]

        // Then
        assertTrue(header is HeaderValue.StringValue)
        assertEquals("value1", header.value)
    }

    @Test
    fun `convertToMap with list values`() {
        // Given
        val jsonString = "{\"Header1\":[\"item1\",\"item2\"]}"

        // When
        val deserialized = converter.convertToMap(jsonString)
        val header = deserialized["Header1"]

        // Then
        assertTrue(header is HeaderValue.ListValue)
        assertEquals(listOf("item1", "item2"), header.values)
    }

    @Test
    fun `round-trip conversion with string value`() {
        // Given
        val headers = mapOf("Header1" to HeaderValue.StringValue("value1"))

        // When
        val jsonString = converter.convertToString(headers)
        val deserializedHeaders = converter.convertToMap(jsonString)

        // Then
        assertEquals(headers, deserializedHeaders)
    }

    @Test
    fun `round-trip conversion with list value`() {
        // Given
        val headers = mapOf("Header1" to HeaderValue.ListValue(listOf("item1", "item2")))

        // When
        val jsonString = converter.convertToString(headers)
        val deserializedHeaders = converter.convertToMap(jsonString)

        // Then
        assertEquals(headers, deserializedHeaders)
    }
}
