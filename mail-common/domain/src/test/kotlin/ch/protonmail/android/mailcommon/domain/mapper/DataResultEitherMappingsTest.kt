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

import java.net.UnknownHostException
import android.util.Log
import app.cash.turbine.test
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.model.NetworkError
import ch.protonmail.android.mailcommon.domain.model.ProtonError
import ch.protonmail.android.test.utils.TestTree
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.ResponseSource
import me.proton.core.network.data.ProtonErrorException
import me.proton.core.network.domain.ApiException
import me.proton.core.network.domain.ApiResult
import me.proton.core.util.kotlin.EMPTY_STRING
import retrofit2.HttpException
import timber.log.Timber
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

internal class DataResultEitherMappingsTest {

    private val testTree = TestTree()

    @BeforeTest
    fun setUp() {
        Timber.plant(testTree)
    }

    @Test
    fun `emits data result value on success`() = runTest {
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
    fun `does log and return unknown local error for unhandled local error`() = runTest {
        // given
        val message = "an error occurred"
        val dataResult = DataResult.Error.Local(message, cause = Exception("Unknown exception"))
        val input = flowOf(dataResult)
        // when
        input.mapToEither().test {
            // then
            assertEquals(DataError.Local.Unknown.left(), awaitItem())
            verifyErrorLogged("UNHANDLED LOCAL ERROR caused by result: $dataResult")
            awaitComplete()
        }
    }

    @Test
    fun `does emit not found network error for remote http code 404`() = runTest {
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
    fun `does log and return unknown http error for unhandled http error`() = runTest {
        // given
        val dataResult = DataResult.Error.Remote(message = null, cause = null, httpCode = 456)
        val input = flowOf(dataResult)
        // when
        input.mapToEither().test {
            // then
            val expected = DataError.Remote.Http(NetworkError.Unknown).left()
            assertEquals(expected, awaitItem())
            verifyErrorLogged("UNHANDLED NETWORK ERROR caused by result: $dataResult")
            awaitComplete()
        }
    }

    @Test
    fun `does log and return unknown proton error for unhandled proton error`() = runTest {
        // given
        val dataResult = DataResult.Error.Remote(message = null, cause = null, protonCode = 123)
        val input = flowOf(dataResult)
        // when
        input.mapToEither().test {
            // then
            val expected = DataError.Remote.Proton(ProtonError.Unknown).left()
            assertEquals(expected, awaitItem())
            verifyErrorLogged("UNHANDLED PROTON ERROR caused by result: $dataResult")
            awaitComplete()
        }
    }

    @Test
    fun `does log and return unknown remote error for unhandled remote error`() = runTest {
        // given
        val message = "an error occurred"
        val dataResult = DataResult.Error.Remote(message = message, cause = Exception("Unknown exception"))
        val input = flowOf(dataResult)
        // when
        input.mapToEither().test {
            // then
            assertEquals(DataError.Remote.Unknown.left(), awaitItem())
            verifyErrorLogged("UNHANDLED REMOTE ERROR caused by result: $dataResult")
            awaitComplete()
        }
    }

    @Test
    fun `does log and return Unknown remote error nested exception is unknown`() = runTest {
        // given
        val cause = ApiException(
            ApiResult.Error.Http(
                cause = Exception("Unknown exception"),
                httpCode = 0,
                message = EMPTY_STRING
            )
        )
        val dataResult = DataResult.Error.Remote(message = null, cause = cause)
        val expectedError = DataError.Remote.Unknown
        val input = flowOf(dataResult)
        // when
        input.mapToEither().test {
            // then
            assertEquals(expectedError.left(), awaitItem())
            verifyErrorLogged("UNHANDLED REMOTE ERROR caused by result: $dataResult")
            awaitComplete()
        }
    }

    @Test
    fun `does handle nested UnknownHostException`() = runTest {
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

    @Test
    fun `does handle nested remote Proton Exception mapping it to Proton Error`() = runTest {
        // given
        val protonException = ProtonErrorException(
            response = mockk(),
            protonData = ApiResult.Error.ProtonData(2063, "Base64 data has bad format")
        )
        val input = flowOf(
            DataResult.Error.Remote(
                message = "Wrapping error message",
                cause = ApiException(
                    ApiResult.Error.Http(
                        httpCode = 0,
                        message = "",
                        cause = protonException
                    )
                ),
                httpCode = 0
            )
        )
        // when
        input.mapToEither().test {
            // then
            assertEquals(DataError.Remote.Proton(ProtonError.Base64Format).left(), awaitItem())
            awaitComplete()
        }
    }

    private fun verifyErrorLogged(message: String) {
        val loggedError = TestTree.Log(
            priority = Log.ERROR,
            message = message,
            tag = null,
            t = null
        )
        assertEquals(loggedError, testTree.logs.lastOrNull())
    }
}
