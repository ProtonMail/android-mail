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

package ch.protonmail.android.networkmocks.mockwebserver

import java.util.logging.Logger
import ch.protonmail.android.networkmocks.mockwebserver.requests.MockRequest
import ch.protonmail.android.networkmocks.mockwebserver.response.generateAssetNotFoundResponse
import ch.protonmail.android.networkmocks.mockwebserver.response.generateResponse
import ch.protonmail.android.networkmocks.mockwebserver.response.generateUnhandledPathResponse
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest

/**
 * A custom [Dispatcher] for [MockWebServer] that checks for matches between the incoming [RecordedRequest.path]
 * and a list of known [MockRequest]s. For each request, it will log whether the related asset has been found.
 *
 * In order to define a custom routing strategy, the [MockNetworkDispatcher] shall be instantiated,
 * populated with [MockRequest]s and assigned to the [MockWebServer] for every test.
 *
 * Further optimizations can be done on a test suite level, for instance by defining convenience methods
 * or default dispatchers that can be enriched with additional mock requests definitions.
 *
 * Example:
 *  ```kotlin
 * class MyTestSuite {
 *    @Inject
 *    lateinit var mockWebServer: MockWebServer
 *
 *    // Other rules and setup code.
 *
 *    @Test
 *    fun testCase() {
 *        // Simple usage and definition of a custom dispatcher.
 *        mockWebServer.dispatcher = MockNetworkDispatcher().apply {
 *            addMockRequests(
 *                 "/api/v1/path" respondWith "/api/v1/localPath" withStatusCode 200,
 *                 "/api/v1/path2" respondWith "/api/v1/localPath2" withStatusCode 401
 *             )
 *        }
 *
 *        // Test execution code.
 *    }
 * }
 *  ```
 * For further examples, please check `MockNetworkDispatcherTests.kt`.
 *
 * @param assetsRootPath a custom root path for the assets.
 */
class MockNetworkDispatcher(
    private val assetsRootPath: String = DEFAULT_ASSETS_ROOT_PATH
) : Dispatcher() {

    private val knownRequests = mutableListOf<MockRequest>()

    override fun dispatch(request: RecordedRequest): MockResponse {
        // This should never happen, just leveraging smart cast with Elvis operator here.
        val remotePath = request.path
            ?: throw UnsupportedMockNetworkDispatcherException("❌ Handling requests with `null` path is unsupported.")

        val validRequest = knownRequests
            .asSequence()
            .sortedByDescending { it.priority.value }
            .find { mockRequest ->
                when {
                    mockRequest.ignoreQueryParams && mockRequest.wildcardMatch -> remotePath.stripQueryParams()
                        .wildcardMatches(mockRequest.remotePath)

                    mockRequest.ignoreQueryParams -> remotePath.stripQueryParams() == mockRequest.remotePath

                    mockRequest.wildcardMatch -> remotePath.wildcardMatches(mockRequest.remotePath)

                    else -> remotePath == mockRequest.remotePath
                }
            } ?: run {
            logger.severe("⚠️ Unknown path '$remotePath', check the mocked network definitions.")
            return generateUnhandledPathResponse(remotePath)
        }

        if (validRequest.serveOnce) knownRequests.remove(validRequest)
        logger.info("✅ Match found for '$remotePath'.")

        val body = getBodyFromLocalAsset(validRequest.localFilePath) ?: run {
            logger.severe("⚠️ Unable to retrieve content for asset '${validRequest.localFilePath}.")
            return generateAssetNotFoundResponse(validRequest.localFilePath)
        }

        logger.info("➡️ Serving ${request.method} $remotePath with $validRequest.")
        return generateResponse(validRequest.statusCode, body, validRequest.networkDelay)
    }

    /**
     * Adds an arbitrary number of [MockRequest] to the known requests list.
     */
    fun addMockRequests(vararg requests: MockRequest) {
        knownRequests.addAll(requests)
    }

    private fun getBodyFromLocalAsset(path: String): String? {
        val fullPath = assetsRootPath + path
        val byteStream = this.javaClass.classLoader.getResourceAsStream(fullPath)

        // Propagate the nullability and handle it at the call site.
        return byteStream?.readBytes()?.toString(Charsets.UTF_8)
    }

    private fun String.stripQueryParams(): String = substringBefore("?")

    private fun String.wildcardMatches(value: String): Boolean {
        val paths = split("/")
        val valuePaths = value.split("/")

        if (paths.size != valuePaths.size) return false

        for (pathPair in paths zip valuePaths) {
            if (pathPair.first == "*" || pathPair.second == "*") continue
            if (pathPair.first != pathPair.second) return false
        }

        return true
    }

    companion object {

        private val logger = Logger.getLogger(this::class.java.name)
        private const val DEFAULT_ASSETS_ROOT_PATH = "assets/network-mocks"
    }
}
