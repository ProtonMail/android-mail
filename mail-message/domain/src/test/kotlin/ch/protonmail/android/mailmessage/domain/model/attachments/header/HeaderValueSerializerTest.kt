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

import io.mockk.mockk
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

internal class HeaderValueSerializerTest {

    private val json = Json { encodeDefaults = true }

    @Test
    fun `should serialize string value`() {
        // Given
        val stringValue = HeaderValue.StringValue("test-value")

        // When
        val serialized = json.encodeToString(HeaderValueSerializer, stringValue)

        // Then
        assertEquals("\"test-value\"", serialized)
    }

    @Test
    fun `should deserialize string value`() {
        // Given
        val jsonString = "\"test-value\""

        // When
        val deserialized = json.decodeFromString(HeaderValueSerializer, jsonString)

        // Then
        assertTrue(deserialized is HeaderValue.StringValue)
        assertEquals("test-value", deserialized.value)
    }

    @Test
    fun `should serialize list value`() {
        // Given
        val listValue = HeaderValue.ListValue(listOf("item1", "item2", "item3"))

        // When
        val serialized = json.encodeToString(HeaderValueSerializer, listValue)

        // Then
        assertEquals("[\"item1\",\"item2\",\"item3\"]", serialized)
    }

    @Test
    fun `should deserialize list value`() {
        // Given
        val jsonString = "[\"item1\",\"item2\",\"item3\"]"

        // When
        val deserialized = json.decodeFromString(HeaderValueSerializer, jsonString)

        // Then
        assertTrue(deserialized is HeaderValue.ListValue)
        assertEquals(listOf("item1", "item2", "item3"), deserialized.values)
    }

    @Test
    fun `should throw throws exception when serializing invalid value`() {
        // Given
        val invalidJson = "{\"invalid\":\"data\"}"

        // When + Then
        assertFailsWith<SerializationException> {
            json.decodeFromString(HeaderValueSerializer, invalidJson)
        }
    }

    @Test
    fun `should serialize and deserialize string value`() {
        // Given
        val stringValue = HeaderValue.StringValue("test-value")

        // When
        val serialized = json.encodeToString(HeaderValueSerializer, stringValue)
        val deserialized = json.decodeFromString(HeaderValueSerializer, serialized)

        // Then
        assertEquals(stringValue, deserialized)
    }

    @Test
    fun `should serialize and deserialize list value`() {
        // Given
        val listValue = HeaderValue.ListValue(listOf("item1", "item2", "item3"))

        // WHen
        val serialized = json.encodeToString(HeaderValueSerializer, listValue)
        val deserialized = json.decodeFromString(HeaderValueSerializer, serialized)

        // Then
        assertEquals(listValue, deserialized)
    }

    @Test
    fun `should throw exception when deserializing with an invalid decoder type`() {
        // Given
        val invalidDecoder = mockk<Decoder>()

        // When + Then
        val exception = assertFailsWith<SerializationException> {
            HeaderValueSerializer.deserialize(invalidDecoder)
        }

        // Then
        assertTrue(exception.message?.contains("Invalid cast to JsonDecoder") == true)
        assertTrue(exception.message?.contains("Expected JsonDecoder, got '${invalidDecoder::class}' instead.") == true)
    }
}
