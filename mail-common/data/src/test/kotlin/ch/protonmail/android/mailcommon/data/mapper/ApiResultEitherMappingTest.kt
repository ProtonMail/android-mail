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
package ch.protonmail.android.mailcommon.data.mapper

import java.net.UnknownHostException
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.mapper.fromHttpCode
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.model.NetworkError
import io.mockk.every
import io.mockk.mockkStatic
import me.proton.core.network.domain.ApiResult
import org.json.JSONException
import kotlin.test.Test
import kotlin.test.assertEquals

class ApiResultEitherMappingTest {

    @Test
    fun `returns Right on success`() {
        // given
        val apiResult = ApiResult.Success("value")

        // when
        val result = apiResult.toEither()

        // then
        assertEquals("value".right(), result)
    }

    @Test
    fun `returns Parse Network Error on parse error`() {
        // given
        val cause = JSONException("message")
        val apiResult = ApiResult.Error.Parse(cause)

        // when
        val actual = apiResult.toEither()

        // then
        assertEquals(DataError.Remote.Http(NetworkError.Parse).left(), actual)
    }

    @Test
    fun `returns no internet on no internet error`() {
        // given
        val apiResult = ApiResult.Error.NoInternet()

        // when
        val result = apiResult.toEither()

        // then
        assertEquals(DataError.Remote.Http(NetworkError.NoNetwork).left(), result)
    }

    @Test
    fun `returns unreachable on connection error`() {
        // given
        val apiResult = ApiResult.Error.Connection(potentialBlock = false)

        // when
        val result = apiResult.toEither()

        // then
        assertEquals(DataError.Remote.Http(NetworkError.Unreachable).left(), result)
    }

    @Test
    fun `returns no network on connection error due to unknown host exception`() {
        // given
        val apiResult = ApiResult.Error.Connection(
            potentialBlock = false,
            cause = UnknownHostException(
                "Unable to resolve host \"mail-api.proton.me\": No address associated with hostname"
            )
        )

        // when
        val result = apiResult.toEither()

        // then
        assertEquals(DataError.Remote.Http(NetworkError.NoNetwork).left(), result)
    }

    @Test
    fun `returns network error for http errors`() {
        // given
        mockkStatic(NetworkError.Companion::fromHttpCode) {
            every { NetworkError.fromHttpCode(any()) } returns NetworkError.Unreachable
            val apiResult = ApiResult.Error.Http(404, "message")

            // when
            val result = apiResult.toEither()

            // then
            assertEquals(DataError.Remote.Http(NetworkError.Unreachable).left(), result)
        }
    }
}
