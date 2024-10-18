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

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

internal object HeaderValueSerializer : KSerializer<HeaderValue> {

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("HeaderValue") {
        element<String>("stringValue")
        element<List<String>>("listValue")
    }

    override fun serialize(encoder: Encoder, value: HeaderValue) {
        when (value) {
            is HeaderValue.StringValue -> encoder.encodeString(value.value)
            is HeaderValue.ListValue ->
                encoder.encodeSerializableValue(ListSerializer(String.serializer()), value.values)
        }
    }

    override fun deserialize(decoder: Decoder): HeaderValue {
        val jsonDecoder = decoder as? JsonDecoder
            ?: throw SerializationException(
                """
                    Invalid cast to JsonDecoder: 
                    Expected JsonDecoder, got '${decoder::class}' instead.
                """.trimIndent()
            )

        return when (val element = jsonDecoder.decodeJsonElement()) {
            is JsonPrimitive -> HeaderValue.StringValue(element.content)
            is JsonArray -> HeaderValue.ListValue(element.map { it.jsonPrimitive.content })
            else -> throw SerializationException(
                """
                    Unknown JSON element type: 
                        Expected JsonPrimitive/JsonArray, got '${element::class}' instead.
                """.trimIndent()
            )
        }
    }
}
