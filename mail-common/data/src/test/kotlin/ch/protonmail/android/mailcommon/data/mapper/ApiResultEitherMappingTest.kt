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
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.model.NetworkError
import me.proton.core.network.domain.ApiResult
import org.json.JSONException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

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
    fun `throws exception on parse error`() {
        // given
        val cause = JSONException("message")
        val apiResult = ApiResult.Error.Parse(cause)

        // when / then
        val actualException = assertFailsWith<JSONException> { apiResult.toEither() }
        assertEquals(cause, actualException)
    }

    @Test
    fun `throws runtime exception on parse error without a cause`() {
        // given
        val apiResult = ApiResult.Error.Parse(null)

        // when / then
        val actualException = assertFailsWith<RuntimeException> { apiResult.toEither() }
        assertEquals("Parse error without cause", actualException.message)
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
}
