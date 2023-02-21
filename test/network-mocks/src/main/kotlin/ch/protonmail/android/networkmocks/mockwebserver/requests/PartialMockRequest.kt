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
 * A data class representing a **partial** [MockRequest].
 *
 * @param remotePath The remote path of the API call.
 * @param localFilePath The path of the local file to use as response.
 */
data class PartialMockRequest(
    val remotePath: String,
    val localFilePath: String
)

/**
 * Creates a [PartialMockRequest] by pairing a given `String`, representing a remote path,
 * to another `String` containing the local file path.
 */
infix fun String.respondWith(localFilePath: String) = PartialMockRequest(this, localFilePath)

/**
 * Creates a [MockRequest] from a [PartialMockRequest] with a defined `statusCode`.
 */
infix fun PartialMockRequest.withStatusCode(statusCode: Int): MockRequest =
    MockRequest(this.remotePath, this.localFilePath, statusCode)
