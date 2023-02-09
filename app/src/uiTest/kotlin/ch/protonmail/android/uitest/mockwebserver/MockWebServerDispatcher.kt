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

package ch.protonmail.android.uitest.mockwebserver

import java.io.InputStream
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import timber.log.Timber

/**
 * Default [Dispatcher] for [MockWebServer].
 *
 * Fetches assets using the [RecordedRequest.path]
 * It will try to match first the full path, then the path without query parameters.
 * If no asset is found, returns an empty [MockResponse].
 *
 * For each request, it will print to log whether the related asset has been found.
 */
class MockWebServerDispatcher : Dispatcher() {

    private val classLoader = javaClass.classLoader

    override fun dispatch(request: RecordedRequest): MockResponse {
        val inputStream = getAssetInputStreamOrNull(request)
        log(request, isAssetAvailable = inputStream != null)
        return inputStream?.use { MockResponse().setBody(it.readBytes().toString(Charsets.UTF_8)) }
            ?: MockResponse()
    }

    private fun getAssetInputStreamOrNull(request: RecordedRequest): InputStream? {
        val fullPath = request.path?.substringAfter("/")
        val noParamsPath = fullPath?.substringBefore("?")
        return classLoader?.getResourceAsStream("assets/mock/$fullPath.json")
            ?: classLoader?.getResourceAsStream("assets/mock/$noParamsPath.json")
    }

    private fun log(request: RecordedRequest, isAssetAvailable: Boolean) {
        val response = if (isAssetAvailable) "✅ Response from assets" else "⚠️️ Empty response, asset not found"
        Timber.i("Request path ${request.path} >> $response.")
    }
}
