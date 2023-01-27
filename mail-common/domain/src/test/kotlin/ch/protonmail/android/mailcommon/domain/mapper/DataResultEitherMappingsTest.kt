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

import java.io.IOException
import java.net.UnknownHostException
import app.cash.turbine.test
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.model.NetworkError
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.ResponseSource
import me.proton.core.network.domain.ApiException
import me.proton.core.network.domain.ApiResult
import me.proton.core.util.kotlin.EMPTY_STRING
import retrofit2.HttpException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

internal class DataResultEitherMappingsTest {

    @Test
    fun `emits Right on success`() = runTest {
        // given
        val string1 = "hello"
        val string2 = "world"
        val input = flowOf(
            DataResult.Success(ResponseSource.Local, string1),
            DataResult.Success(ResponseSource.Remote, string2)
        )

        // when
        input.mapToEither().test {

            // then
            assertEquals(string1.right(), awaitItem())
            assertEquals(string2.right(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `does not emit anything on loading`() = runTest {
        // given
        val string = "hello"
        val input = flowOf(
            DataResult.Processing(ResponseSource.Remote),
            DataResult.Success(ResponseSource.Remote, string)
        )

        // when
        input.mapToEither().test {

            // then
            assertEquals(string.right(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `does throw exception with message from data result for unhandled local error`() = runTest {
        // given
        val message = "an error occurred"
        val dataResult = DataResult.Error.Local(message, cause = null)
        val input = flowOf(dataResult)

        // when
        input.mapToEither().test {

            // then
            awaitError().assertIs<MappingRuntimeException>(
                expectedMessage = "Unhandled local error $dataResult, message = $message"
            )
        }
    }

    @Test
    fun `does throw exception from data result cause for unhandled local error`() = runTest {
        // given
        val message = "an error occurred"
        val cause = IOException("an error occurred")
        val dataResult = DataResult.Error.Local(message = null, cause = cause)
        val input = flowOf(dataResult)

        // when
        input.mapToEither().test {

            // then
            awaitError().assertIs<IOException>(expectedMessage = message)
        }
    }

    @Test
    fun `does throw exception with no message provided for unhandled local error`() = runTest {
        // given
        val cause = IOException()
        val dataResult = DataResult.Error.Local(message = null, cause = cause)
        val input = flowOf(dataResult)

        // when
        input.mapToEither().test {

            // then
            awaitError().assertIs<IOException>(expectedMessage = null)
        }
    }

    @Test
    fun `does emit http error`() = runTest {
        // given
        val input = flowOf(DataResult.Error.Remote(message = null, cause = null, httpCode = 404))

        // when
        input.mapToEither().test {

            // then
            assertEquals(DataError.Remote.Http(NetworkError.NotFound).left(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `does throw exception for unhandled proton error`() = runTest {
        // given
        val dataResult = DataResult.Error.Remote(message = null, cause = null, protonCode = 123)
        val input = flowOf(dataResult)

        // when
        input.mapToEither().test {

            // then
            awaitError().assertIs<MappingRuntimeException>(
                expectedMessage = "Unhandled remote error $dataResult, message = $DATA_RESULT_NO_MESSAGE_PROVIDED"
            )
        }
    }

    @Test
    fun `does throw exception with message from data result for unhandled remote error`() =
        runTest {
            // given
            val message = "an error occurred"
            val dataResult = DataResult.Error.Remote(message = message, cause = null)
            val input = flowOf(dataResult)

            // when
            input.mapToEither().test {

                // then
                awaitError().assertIs<MappingRuntimeException>(
                    expectedMessage = "Unhandled remote error $dataResult, message = $message"
                )
            }
        }

    @Test
    fun `does throw exception from data result cause for unhandled remote error`() = runTest {
        // given
        val message = "an error occurred"
        val cause = IOException(message)
        val dataResult = DataResult.Error.Remote(message = null, cause = cause)
        val input = flowOf(dataResult)

        // when
        input.mapToEither().test {

            // then
            awaitError().assertIs<IOException>(expectedMessage = message)
        }
    }

    @Test
    fun `does throw exception with no message provided for unhandled remote error`() = runTest {
        // given
        val dataResult = DataResult.Error.Remote(message = null, cause = null)
        val input = flowOf(dataResult)

        // when
        input.mapToEither().test {

            // then
            awaitError().assertIs<MappingRuntimeException>(
                expectedMessage = "Unhandled remote error $dataResult, message = $DATA_RESULT_NO_MESSAGE_PROVIDED"
            )
        }
    }

    @Test
    fun `does handle UnknownHostException`() = runTest {
        // given
        val cause = ApiException(
            ApiResult.Error.Http(
                cause = UnknownHostException(),
                httpCode = 0,
                message = EMPTY_STRING
            )
        )
        val dataResult = DataResult.Error.Remote(message = null, cause = cause)
        val expectedError = DataError.Remote.Http(NetworkError.NoNetwork)
        val input = flowOf(dataResult)

        // when
        input.mapToEither().test {

            // then
            assertEquals(expectedError.left(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `does handle nested Http Exceptions mapping them to http errors`() = runTest {
        // given
        val httpException = mockk<HttpException> {
            every { message } returns "HTTP 505 HTTP Version Not Supported"
            every { code() } returns 505
        }
        val input = flowOf(
            DataResult.Error.Remote(
                message = "HTTP 505 HTTP Version Not Supported",
                cause = ApiException(ApiResult.Error.Parse(httpException)),
                httpCode = 0
            )
        )

        // when
        input.mapToEither().test {

            // then
            assertEquals(DataError.Remote.Http(NetworkError.ServerError).left(), awaitItem())
            awaitComplete()
        }
    }
}

private inline fun <reified T> Throwable.assertIs(
    expectedMessage: String?
) {
    assertIs<T>(value = this)
    assertEquals(expected = expectedMessage, actual = message)
}
