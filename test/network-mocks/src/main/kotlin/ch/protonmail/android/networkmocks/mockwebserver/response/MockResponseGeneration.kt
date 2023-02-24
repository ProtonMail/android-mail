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

package ch.protonmail.android.networkmocks.mockwebserver.response

import java.util.concurrent.TimeUnit
import okhttp3.Headers
import okhttp3.mockwebserver.MockResponse

/**
 * Delegate to [MockResponse] creation that will force custom JSON content
 * specifying that a local asset cannot be found by the Dispatcher.
 */
internal fun generateAssetNotFoundResponse(forPath: String): MockResponse {
    val body = """
        {
            "error": "ASSET_NOT_FOUND",
            "cause": "No local asset found at path '$forPath'."
        }
    """.trimIndent()

    return generateResponse(statusCode = 404, body = body)
}

/**
 * Delegate to [MockResponse] creation that will force custom JSON content
 * specifying that a route is currently not handled by the Dispatcher.
 */
internal fun generateUnhandledPathResponse(forPath: String): MockResponse {
    val body = """
        {
            "error": "PATH_NOT_FOUND",
            "cause": "No route found for path '$forPath'."
        }
    """.trimIndent()

    return generateResponse(statusCode = 404, body = body)
}

/**
 * Generic function to generate a [MockResponse] that takes care of headers, status code, body and delay (if any).
 *
 * @param statusCode the response status code.
 * @param body the response body.
 * @param networkDelay the network delay (defaults to 0ms).
 */
internal fun generateResponse(
    statusCode: Int,
    body: String,
    networkDelay: Long = 0L
): MockResponse {
    return MockResponse().apply {
        setResponseCode(statusCode)

        when (statusCode) {
            // If it's a 204, do not set headers/body.
            204 -> setHeadersDelay(networkDelay, TimeUnit.MILLISECONDS)
            else -> {
                // Content-Type is always JSON, for the time being.
                val headers = Headers.headersOf(
                    "Content-Type", "application/json",
                    "Content-Length", body.length.toString()
                )

                setHeaders(headers)
                setBodyDelay(networkDelay, TimeUnit.MILLISECONDS)
                setBody(body)
            }
        }
    }
}
