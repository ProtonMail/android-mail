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

package ch.protonmail.android.networkmocks

import ch.protonmail.android.networkmocks.mockwebserver.MockNetworkDispatcher
import ch.protonmail.android.networkmocks.mockwebserver.requests.MockPriority
import ch.protonmail.android.networkmocks.mockwebserver.requests.ignoreQueryParams
import ch.protonmail.android.networkmocks.mockwebserver.requests.matchWildcards
import ch.protonmail.android.networkmocks.mockwebserver.requests.respondWith
import ch.protonmail.android.networkmocks.mockwebserver.requests.serveOnce
import ch.protonmail.android.networkmocks.mockwebserver.requests.withPriority
import ch.protonmail.android.networkmocks.mockwebserver.requests.withStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

internal class MockNetworkDispatcherTests {

    private val client = OkHttpClient.Builder().build()
    private val mockWebServer = MockWebServer()
    private val url: HttpUrl = mockWebServer.url("/")

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `when the dispatcher handles a mock request, the response content type is always application json`() {
        // Given
        val expectedContentType = "application/json"
        val request = buildRequest("api/v1/test")

        mockWebServer.dispatcher = mockNetworkDispatcher {
            addMockRequests(
                "/api/v1/test" respondWith "/api/v1/test_1.json" withStatusCode 200
            )
        }

        // When
        val response = runBlocking { performRequest(request) }

        // Then
        assertEquals(expectedContentType, response.headers["Content-Type"])
    }

    @Test
    fun `when a mock request is forced to return a non-200 code, the dispatcher serves a non-200 code response`() {
        // Given
        val expectedStatusCode = 500
        val request = buildRequest("api/v1/test")

        mockWebServer.dispatcher = mockNetworkDispatcher {
            addMockRequests(
                "/api/v1/test" respondWith "/api/v1/test_1.json" withStatusCode 500
            )
        }

        // When
        val response = runBlocking { performRequest(request) }

        // Then
        assertEquals(expectedStatusCode, response.code)
    }

    @Test
    fun `when a request is forced to return a 204 code, the dispatcher serves a response with no-content`() {
        // Given
        val expectedStatusCode = 204
        val request = buildRequest("api/v1/test")

        mockWebServer.dispatcher = mockNetworkDispatcher {
            addMockRequests(
                "/api/v1/test" respondWith "/api/v1/test_1.json" withStatusCode 204
            )
        }

        // When
        val response = runBlocking { performRequest(request) }

        // Then
        assertEquals(expectedStatusCode, response.code)
        assertTrue(response.body?.string()?.isEmpty()!!)
    }

    @Test
    fun `when query parameters are ignored, the dispatcher matches against a generic path`() {
        // Given
        val expectedStatusCode = 200
        val expectedBody = """{ "a": 1 }"""
        val request = buildRequest("api/v1/test?query1=abc&query2=cde&query3=ababab")

        mockWebServer.dispatcher = mockNetworkDispatcher {
            addMockRequests(
                "/api/v1/test" respondWith "/api/v1/test_1.json" withStatusCode 200 ignoreQueryParams true
            )
        }

        // When
        val response = runBlocking { performRequest(request) }

        // Then
        assertEquals(expectedStatusCode, response.code)
        assertEquals(expectedBody, response.body?.string())
    }

    @Test
    fun `when wildcard matching paths, the dispatcher matches against a generic path`() {
        // Given
        val expectedStatusCode = 200
        val expectedBody = """{ "a": 1 }"""
        val request = buildRequest("api/random_version/test")

        mockWebServer.dispatcher = mockNetworkDispatcher {
            addMockRequests(
                "/api/*/test" respondWith "/api/v1/test_1.json" withStatusCode 200 matchWildcards true
            )
        }

        // When
        val response = runBlocking { performRequest(request) }

        // Then
        assertEquals(expectedStatusCode, response.code)
        assertEquals(expectedBody, response.body?.string())
    }

    @Test
    fun `when wildcard matching and ignoring query params, the dispatcher matches against a generic path`() {
        // Given
        val expectedStatusCode = 200
        val expectedBody = """{ "a": 1 }"""
        val request = buildRequest("api/random_version/test?key1=value1")

        mockWebServer.dispatcher = mockNetworkDispatcher {
            addMockRequests(
                "/api/*/test" respondWith "/api/v1/test_1.json" withStatusCode 200
                    ignoreQueryParams true matchWildcards true
            )
        }

        // When
        val response = runBlocking { performRequest(request) }

        // Then
        assertEquals(expectedStatusCode, response.code)
        assertEquals(expectedBody, response.body?.string())
    }

    @Test
    fun `when the mock request is set to be served once, the dispatcher serves the request only once`() {
        // Given
        val firstExpectedStatusCode = 200
        val expectedBody = """{ "a": 1 }"""
        val secondExpectedStatusCode = 404

        val request = buildRequest("api/v1/test")

        mockWebServer.dispatcher = mockNetworkDispatcher {
            addMockRequests(
                "/api/v1/test" respondWith "/api/v1/test_1.json" withStatusCode 200 serveOnce true
            )
        }

        // When
        val firstResponse = runBlocking { performRequest(request) }
        val secondResponse = runBlocking { performRequest(request) }

        // Then
        assertEquals(firstExpectedStatusCode, firstResponse.code)
        assertEquals(expectedBody, firstResponse.body?.string())
        assertEquals(secondExpectedStatusCode, secondResponse.code)
        assertNotEquals(expectedBody, secondResponse.body?.string())
    }

    @Test
    fun `when a mock request is served once, multiple different responses are handled properly for a given path`() {
        // Given
        val firstExpectedStatusCode = 200
        val firstExpectedBody = """{ "a": 1 }"""
        val secondExpectedStatusCode = 500
        val secondExpectedBody = """{ "b" : 2 }"""

        val request = buildRequest("api/v1/test")

        mockWebServer.dispatcher = mockNetworkDispatcher {
            addMockRequests(
                "/api/v1/test" respondWith "/api/v1/test_1.json" withStatusCode 200 serveOnce true,
                "/api/v1/test" respondWith "/api/v1/test_2.json" withStatusCode 500 serveOnce true
            )
        }

        // When
        val firstResponse = runBlocking { performRequest(request) }
        val secondResponse = runBlocking { performRequest(request) }

        // Then
        assertEquals(firstExpectedStatusCode, firstResponse.code)
        assertEquals(firstExpectedBody, firstResponse.body?.string())
        assertEquals(secondExpectedStatusCode, secondResponse.code)
        assertNotEquals(secondExpectedBody, secondResponse.body?.string())
    }

    @Test
    fun `when a request is not mapped, the dispatcher returns a 404 status code`() {
        // Given
        val expectedStatusCode = 404
        val request = buildRequest("api/v2/test")

        mockWebServer.dispatcher = mockNetworkDispatcher {
            addMockRequests(
                "/api/v1/test" respondWith "/api/v1/test_1.json" withStatusCode 200
            )
        }

        // When
        val response = runBlocking { performRequest(request) }

        // Then
        assertEquals(expectedStatusCode, response.code)
    }

    @Test
    fun `when a request is mapped with an invalid local file path, the dispatcher returns a 404 status code`() {
        // Given
        val expectedStatusCode = 404
        val request = buildRequest("api/v1/test")

        mockWebServer.dispatcher = mockNetworkDispatcher {
            addMockRequests(
                "/api/v1/test" respondWith "/api/v1/unknown_file.json" withStatusCode 200
            )
        }

        // When
        val response = runBlocking { performRequest(request) }

        // Then
        assertEquals(expectedStatusCode, response.code)
    }

    @Test
    fun `when a request with the same path but higher priority is defined, it overrides any other defined before`() {
        // Given
        val expectedStatusCode = 500
        val expectedBody = """{ "c": 3 }"""

        val request = buildRequest("api/v1/test")

        mockWebServer.dispatcher = mockNetworkDispatcher {
            addMockRequests(
                "/api/v1/*" respondWith "/api/v1/test_1.json" withStatusCode 200 matchWildcards true,
                "/api/v1/test" respondWith "/api/v1/test_2.json" withStatusCode 200,
                "/api/v1/test" respondWith "/api/v1/test_3.json" withStatusCode 500 withPriority MockPriority.High
            )
        }

        // When
        val response = runBlocking { performRequest(request) }

        // Then
        assertEquals(expectedStatusCode, response.code)
        assertEquals(expectedBody, response.body?.string())
    }

    private fun mockNetworkDispatcher(func: MockNetworkDispatcher.() -> Unit) =
        MockNetworkDispatcher(assetsRootPath = "assets/mock").apply(func)

    private fun buildRequest(path: String) = Request.Builder().url("${url}$path").build()

    private suspend fun performRequest(request: Request): Response = withContext(Dispatchers.IO) {
        client.newCall(request).execute()
    }
}
