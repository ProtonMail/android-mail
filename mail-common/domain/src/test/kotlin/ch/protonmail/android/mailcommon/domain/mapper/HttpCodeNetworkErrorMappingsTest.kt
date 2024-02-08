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

package ch.protonmail.android.mailcommon.domain.mapper

import ch.protonmail.android.mailcommon.domain.model.NetworkError
import kotlin.test.Test
import kotlin.test.assertEquals

internal class HttpCodeNetworkErrorMappingsTest {

    @Test
    fun `does return unauthorized for 401 errors`() {
        // given
        val expected = NetworkError.Unauthorized

        // when
        val result = NetworkError.fromHttpCode(401)

        // then
        assertEquals(expected, result)
    }

    @Test
    fun `does return forbidden for 403 errors`() {
        // given
        val expected = NetworkError.Forbidden

        // when
        val result = NetworkError.fromHttpCode(403)

        // then
        assertEquals(expected, result)
    }

    @Test
    fun `does return not found for 404 errors`() {
        // given
        val expected = NetworkError.NotFound

        // when
        val result = NetworkError.fromHttpCode(404)

        // then
        assertEquals(expected, result)
    }

    @Test
    fun `does return unprocessable entity for 422 errors`() {
        // given
        val expected = NetworkError.UnprocessableEntity

        // when
        val result = NetworkError.fromHttpCode(422)

        // then
        assertEquals(expected, result)
    }

    @Test
    fun `does return server error for 5xx errors`() {
        // given
        val expected = NetworkError.ServerError

        // when
        for (httpCode in 500..599) {
            val result = NetworkError.fromHttpCode(httpCode)

            // then
            assertEquals(expected, result)
        }
    }

    @Test
    fun `does return Unknown for unknown errors`() {
        // given
        val unknownCodes = generateRandoms(0..399) +
            generateRandoms(405..421) +
            generateRandoms(423..499) +
            generateRandoms(600..999)
        // when
        for (httpCode in unknownCodes) {
            val result = NetworkError.fromHttpCode(httpCode)
            // then
            assertEquals(NetworkError.Unknown, result)
        }
    }

    private fun generateRandoms(range: IntRange, howMany: Int = 100): Set<Int> = (0 until howMany).map {
        range.random()
    }.toSet()
}
