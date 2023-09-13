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

package ch.protonmail.android.networkmocks.mockwebserver.requests

/**
 * A data class representing a mock request.
 *
 * @param remoteRequest The remote API call request (method + path).
 * @param localFilePath The path of the local file to use as response.
 * @param statusCode The status code to return.
 * @param ignoreQueryParams Whether query parameters shall be ignored for this request.
 * @param wildcardMatch Whether wildcards can be used as match for this request.
 * @param serveOnce Whether this response shall be used only once.
 * @param networkDelay The amount of delay to introduce when serving the response (in milliseconds).
 * @param simulateNoNetwork Whether the response shall simulate a no-network scenario.
 * @param priority A priority value to define whether the request shall override existing requests with the same path.
 */
data class MockRequest(
    val remoteRequest: RemoteRequest,
    val localFilePath: MockRequestLocalPath,
    val statusCode: Int,
    val mimeType: MimeType = MimeType.Json,
    val ignoreQueryParams: Boolean = false,
    val wildcardMatch: Boolean = false,
    val serveOnce: Boolean = false,
    val networkDelay: Long = 0L,
    val simulateNoNetwork: Boolean = false,
    val priority: MockPriority = MockPriority.Lowest
)

/**
 * Returns a new [MockRequest] with an updated `mimeType` value.
 */
infix fun MockRequest.withMimeType(mimeType: MimeType) = this.copy(mimeType = mimeType)

/**
 * Returns a new [MockRequest] with an updated `ignoreQueryParams` value.
 */
infix fun MockRequest.ignoreQueryParams(value: Boolean) = this.copy(ignoreQueryParams = value)

/**
 * Returns a new [MockRequest] with an updated `wildcardMatch` value.
 */
infix fun MockRequest.matchWildcards(value: Boolean) = this.copy(wildcardMatch = value)

/**
 * Returns a new [MockRequest] with an updated `serveOnce` value.
 */
infix fun MockRequest.serveOnce(value: Boolean): MockRequest = this.copy(serveOnce = value)

/**
 * Returns a new [MockRequest] with an updated `networkDelay` value.
 */
infix fun MockRequest.withNetworkDelay(value: Long) = this.copy(networkDelay = value)

/**
 * Returns a new [MockRequest] with an updated `priority` value.
 */
infix fun MockRequest.withPriority(value: MockPriority) = this.copy(priority = value)
