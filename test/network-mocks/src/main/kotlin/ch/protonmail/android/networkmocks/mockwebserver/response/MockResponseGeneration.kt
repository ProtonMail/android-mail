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
import ch.protonmail.android.networkmocks.mockwebserver.requests.MimeType
import ch.protonmail.android.networkmocks.mockwebserver.requests.MockRequestLocalPath
import ch.protonmail.android.networkmocks.mockwebserver.requests.RemoteRequest
import okhttp3.Headers
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.SocketPolicy
import okio.Buffer

/**
 * Delegate to [MockResponse] creation that will force custom JSON content
 * specifying that a local asset cannot be found by the Dispatcher.
 */
internal fun generateAssetNotFoundResponse(path: MockRequestLocalPath): MockResponse {
    val body = """
        {
            "error": "ASSET_NOT_FOUND",
            "cause": "No local asset found at path '$path'."
        }
    """.trimIndent()

    return generateResponse(statusCode = 404, content = body.toByteArray(), mimeType = MimeType.Json)
}

/**
 * Delegate to [MockResponse] creation that will force custom JSON content
 * specifying that a route is currently not handled by the Dispatcher.
 */
internal fun generateUnhandledRemoteRequestResponse(request: RemoteRequest): MockResponse {
    val body = """
        {
            "error": "ROUTE_NOT_FOUND",
            "cause": "No route found for '$request'."
        }
    """.trimIndent()

    return generateResponse(statusCode = 404, content = body.toByteArray(), mimeType = MimeType.Json)
}

/**
 * Generic function to generate a [MockResponse] that takes care of headers, status code, body and delay (if any).
 *
 * @param statusCode the response status code.
 * @param content the response body as [ByteArray].
 * @param networkDelay the network delay (defaults to 0ms).
 */
internal fun generateResponse(
    statusCode: Int,
    content: ByteArray,
    mimeType: MimeType,
    networkDelay: Long = 0L
): MockResponse {
    return MockResponse().apply {
        setResponseCode(statusCode)

        when (statusCode) {
            // If it's a 204, do not set headers/body.
            204 -> setHeadersDelay(networkDelay, TimeUnit.MILLISECONDS)
            else -> {
                val headers = Headers.headersOf(
                    "Content-Type", mimeType.value,
                    "Content-Length", content.size.toString()
                )

                val bufferedContent = Buffer().write(content)

                setHeaders(headers)
                setBodyDelay(networkDelay, TimeUnit.MILLISECONDS)

                // Make sure the buffer is closed.
                bufferedContent.use { setBody(it) }
            }
        }
    }
}

/**
 * Generates a no network response, simulating a scenario where the connection is established
 * but a disconnection occurs mid-response.
 *
 * This is useful to replicate low/no network scenarios.
 */
internal fun generateNoNetworkResponse(): MockResponse {
    // Size is irrelevant here and completely arbitrary.
    val byteArrayStub = ByteArray(1024)

    return MockResponse().apply {
        setBody(Buffer().write(byteArrayStub))
        setSocketPolicy(SocketPolicy.DISCONNECT_DURING_RESPONSE_BODY)
    }
}
