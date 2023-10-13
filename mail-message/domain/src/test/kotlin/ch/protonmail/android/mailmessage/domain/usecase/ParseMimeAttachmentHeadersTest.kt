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

package ch.protonmail.android.mailmessage.domain.usecase

import ch.protonmail.android.mailmessage.domain.usecase.ParseMimeAttachmentHeadersTest.TestData.InvalidAttachmentHeaders
import ch.protonmail.android.mailmessage.domain.usecase.ParseMimeAttachmentHeadersTest.TestData.ValidAttachmentHeaders
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

class ParseMimeAttachmentHeadersTest {

    private val parseMimeAttachmentHeaders = ParseMimeAttachmentHeaders()

    @Test
    fun `should parse valid headers correctly`() {
        // Given
        val expected = JsonObject(
            mapOf(
                "Content-Disposition" to JsonPrimitive("attachment"),
                "filename" to JsonPrimitive("image.png"),
                "Content-Transfer-Encoding" to JsonPrimitive("base64"),
                "Content-Type" to JsonPrimitive("image/png"),
                "name" to JsonPrimitive("image.png")
            )
        )

        // When
        val actual = parseMimeAttachmentHeaders(ValidAttachmentHeaders)

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `should ignore invalid headers`() {
        // Given
        val expected = JsonObject(
            mapOf(
                "Content-Type" to JsonPrimitive("image/png")
            )
        )

        // When
        val actual = parseMimeAttachmentHeaders(InvalidAttachmentHeaders)

        // Then
        assertEquals(expected, actual)
    }

    object TestData {

        val ValidAttachmentHeaders = """
            Content-Disposition: attachment; filename="image.png"
            Content-Transfer-Encoding: base64
            Content-Type: image/png; name="image.png"
        """.trimIndent()

        val InvalidAttachmentHeaders = """
            Content-Disposition attachment; filename="image.png"
            Content-Transfer-Encoding:
            Content-Type: image/png; "image.png"
        """.trimIndent()
    }
}
