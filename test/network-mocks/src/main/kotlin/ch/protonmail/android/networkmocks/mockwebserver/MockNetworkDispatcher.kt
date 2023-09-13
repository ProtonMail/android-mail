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
import ch.protonmail.android.networkmocks.assets.RawAssets
import ch.protonmail.android.networkmocks.mockwebserver.requests.MockRequest
import ch.protonmail.android.networkmocks.mockwebserver.requests.MockRequestLocalPath
import ch.protonmail.android.networkmocks.mockwebserver.requests.RemoteRequest
import ch.protonmail.android.networkmocks.mockwebserver.requests.RequestMethod
import ch.protonmail.android.networkmocks.mockwebserver.response.generateAssetNotFoundResponse
import ch.protonmail.android.networkmocks.mockwebserver.response.generateNoNetworkResponse
import ch.protonmail.android.networkmocks.mockwebserver.response.generateResponse
import ch.protonmail.android.networkmocks.mockwebserver.response.generateUnhandledRemoteRequestResponse
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
 *        mockWebServer.dispatcher extendWith MockNetworkDispatcher().apply {
 *            addMockRequests(
 *                 get("/api/v1/path") respondWith "/api/v1/localPath" withStatusCode 200,
 *                 get("/api/v1/path2") respondWith "/api/v1/localPath2" withStatusCode 401
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
    private val assetsRootPath: String = DefaultAssetsRootPath
) : Dispatcher() {

    private val knownRequests = mutableListOf<MockRequest>()
    val requestsList: List<MockRequest> = knownRequests

    @SuppressWarnings("ReturnCount")
    override fun dispatch(request: RecordedRequest): MockResponse {
        val path = checkNotNull(request.path) { "❌ Handling requests with `null` path is unsupported." }
        val rawMethod = checkNotNull(request.method) { "❌ Handling requests with `null` method is unsupported." }
        val remoteRequest = RemoteRequest(path, RequestMethod.valueOf(rawMethod))

        val validRequest = findMatchingRequests(remoteRequest) ?: run {
            logger.severe("⚠️ Unknown path for '$remoteRequest', check the mocked network definitions.")
            return generateUnhandledRemoteRequestResponse(remoteRequest)
        }

        if (validRequest.serveOnce) knownRequests.remove(validRequest)
        logger.info("✅ Match found for '$remoteRequest'.")

        if (validRequest.simulateNoNetwork) {
            logger.info("📶 Simulating no network response for '$remoteRequest'.")
            return generateNoNetworkResponse()
        }

        val content = getRawContentFromLocalAsset(validRequest.localFilePath) ?: run {
            logger.severe("⚠️ Unable to retrieve content for asset '${validRequest.localFilePath}.")
            return generateAssetNotFoundResponse(validRequest.localFilePath)
        }

        logger.info("➡️ Serving $remoteRequest with $validRequest.")
        return generateResponse(validRequest.statusCode, content, validRequest.mimeType, validRequest.networkDelay)
    }

    /**
     * Adds an arbitrary number of [MockRequest] to the known requests list.
     */
    fun addMockRequests(vararg requests: MockRequest) {
        knownRequests.addAll(requests)
    }

    private fun findMatchingRequests(remoteRequest: RemoteRequest): MockRequest? {
        return knownRequests
            .asSequence()
            .filter { it.remoteRequest.method == remoteRequest.method }
            .sortedByDescending { it.priority.value }
            .find { mockRequest ->
                when {
                    mockRequest.ignoreQueryParams && mockRequest.wildcardMatch -> remoteRequest.stripQueryParams()
                        .wildcardMatches(mockRequest.remoteRequest)

                    mockRequest.ignoreQueryParams -> remoteRequest.stripQueryParams() == mockRequest.remoteRequest

                    mockRequest.wildcardMatch -> remoteRequest.wildcardMatches(mockRequest.remoteRequest)

                    else -> remoteRequest == mockRequest.remoteRequest
                }
            }
    }

    private fun getRawContentFromLocalAsset(localPath: MockRequestLocalPath): ByteArray? =
        RawAssets.getRawContentForPath(assetsRootPath + localPath.path)

    private fun RemoteRequest.stripQueryParams() = copy(path = path.substringBefore("?"))

    private fun RemoteRequest.wildcardMatches(value: RemoteRequest): Boolean {
        val paths = path.split("/")
        val valuePaths = value.path.split("/")

        if (paths.size != valuePaths.size) return false

        for (pathPair in paths zip valuePaths) {
            if (pathPair.first == "*" || pathPair.second == "*") continue
            if (pathPair.first != pathPair.second) return false
        }

        return true
    }

    companion object {

        private val logger = Logger.getLogger(this::class.java.name)
        private const val DefaultAssetsRootPath = "assets/network-mocks"
    }
}
