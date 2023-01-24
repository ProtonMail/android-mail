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

package ch.protonmail.android.mailmessage.data.remote.resource

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Test
import kotlin.test.assertEquals

class MailToResourceTest {

    private val address = """3n.009a.2948.ta0ao44s21.0@unsubscribe.netline.com"""
    private val subject = "Unsubscribe"
    private val body = """You will be unsubscribed from this list within ten days of sending this reply"""

    @Test
    fun `maps mail to json without subject successfully`() {
        // given
        val mailToJson = """
            {
              "ToList": [
                "$address"
              ],
              "Body": "$body"
            }
        """.trimIndent()
        val expected = MailToResource(listOf(address), null, body)

        // when
        val actual = Json.decodeFromString<MailToResource>(mailToJson)

        // then
        assertEquals(expected, actual)
    }

    @Test
    fun `maps mail to json without body successfully`() {
        // given
        val mailToJson = """
            {
              "ToList": [
                "$address"
              ],
              "Subject": "$subject"
            }
        """.trimIndent()
        val expected = MailToResource(listOf(address), subject, null)

        // when
        val actual = Json.decodeFromString<MailToResource>(mailToJson)

        // then
        assertEquals(expected, actual)
    }

    @Test
    fun `maps complete mail to json successfully`() {
        // given
        val mailToJson = """
            {
              "ToList": [
                "$address"
              ],
              "Subject": "$subject",
              "Body": "$body"
            }
        """.trimIndent()
        val expected = MailToResource(listOf(address), subject, body)

        // when
        val actual = Json.decodeFromString<MailToResource>(mailToJson)

        // then
        assertEquals(expected, actual)
    }
}
