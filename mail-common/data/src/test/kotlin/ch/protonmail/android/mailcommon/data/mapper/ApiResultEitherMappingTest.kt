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
import ch.protonmail.android.mailcommon.domain.model.ProtonError
import ch.protonmail.android.test.utils.rule.LoggingTestRule
import io.mockk.every
import io.mockk.mockkStatic
import me.proton.core.network.domain.ApiResult
import org.json.JSONException
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals

class ApiResultEitherMappingTest {

    @get:Rule
    val loggingTestRule = LoggingTestRule()

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
    fun `returns and logs Parse Network Error on parse error`() {
        // given
        val cause = JSONException("message")
        val apiResult = ApiResult.Error.Parse(cause)

        // when
        val actual = apiResult.toEither()

        // then
        val expected = DataError.Remote.Http(NetworkError.Parse, "No error message found")
        assertEquals(expected.left(), actual)
        loggingTestRule.assertErrorLogged("Unexpected parse error, caused by: $cause")
    }

    @Test
    fun `returns no internet on no internet error`() {
        // given
        val apiResult = ApiResult.Error.NoInternet()

        // when
        val result = apiResult.toEither()

        // then
        val expected = DataError.Remote.Http(NetworkError.NoNetwork, "No error message found", isRetryable = true)
        assertEquals(expected.left(), result)
    }

    @Test
    fun `returns unreachable on connection error`() {
        // given
        val apiResult = ApiResult.Error.Connection(isConnectedToNetwork = false)

        // when
        val result = apiResult.toEither()

        // then
        val expected = DataError.Remote.Http(NetworkError.Unreachable, "No error message found", isRetryable = true)
        assertEquals(expected.left(), result)
    }

    @Test
    fun `returns no network on connection error due to unknown host exception`() {
        // given
        val apiResult = ApiResult.Error.Connection(
            isConnectedToNetwork = false,
            cause = UnknownHostException(
                "Unable to resolve host \"mail-api.proton.me\": No address associated with hostname"
            )
        )

        // when
        val result = apiResult.toEither()

        // then
        val expected = DataError.Remote.Http(NetworkError.NoNetwork, "No error message found", isRetryable = true)
        assertEquals(expected.left(), result)
    }

    @Test
    fun `returns network error for http errors`() {
        // given
        mockkStatic(NetworkError.Companion::fromHttpCode) {
            every { NetworkError.fromHttpCode(any()) } returns NetworkError.Unreachable
            val protonData = ApiResult.Error.ProtonData(-1, "protonError")
            val apiResult = ApiResult.Error.Http(404, "message", protonData)

            // when
            val result = apiResult.toEither()

            // then
            val expected = DataError.Remote.Http(NetworkError.Unreachable, "message - protonError")
            assertEquals(expected.left(), result)
        }
    }

    @Test
    fun `returns proton error for http 422 http error containing 'Message Draft Not Draft' Proton Error Code`() {
        // given
        mockkStatic(NetworkError.Companion::fromHttpCode) {
            every { NetworkError.fromHttpCode(any()) } returns NetworkError.UnprocessableEntity
            val protonData = ApiResult.Error.ProtonData(
                15_034, "Message Already Sent"
            )
            val apiResult = ApiResult.Error.Http(422, "none", protonData)

            // when
            val result = apiResult.toEither()

            // then
            val expected = DataError.Remote.Proton(ProtonError.MessageUpdateDraftNotDraft, "Message Already Sent")
            assertEquals(expected.left(), result)
        }
    }

    @Test
    fun `returns proton error for selected http 422 http errors`() {
        // given
        val codesAndMessages = listOf(
            2011 to ("sending limit reached" to ProtonError.SendingLimitReached),
            2022 to ("num of recipients too large" to ProtonError.NumOfRecipientsTooLarge),
            2024 to ("file is too large" to ProtonError.AttachmentTooLarge),
            2511 to ("paid sub is needed" to ProtonError.PaidSubscriptionRequired)
        )

        mockkStatic(NetworkError.Companion::fromHttpCode) {
            codesAndMessages.forEach { (code, msgAndType) ->
                every { NetworkError.fromHttpCode(any()) } returns NetworkError.UnprocessableEntity
                val (message, type) = msgAndType
                val protonData = ApiResult.Error.ProtonData(
                    code, message
                )
                val apiResult = ApiResult.Error.Http(422, "none", protonData)

                // when
                val result = apiResult.toEither()

                // then
                val expected = DataError.Remote.Proton(type, message)
                assertEquals(expected.left(), result)
            }
        }
    }
}
