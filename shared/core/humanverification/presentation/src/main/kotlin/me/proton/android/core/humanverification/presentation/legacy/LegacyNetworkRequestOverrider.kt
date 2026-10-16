/*
 * Copyright (c) 2025 Proton Technologies AG
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

package me.proton.android.core.humanverification.presentation.legacy

import java.io.InputStream
import androidx.annotation.VisibleForTesting
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

private val HTTP_CLIENT_TIMEOUT = 30.seconds.toJavaDuration()

internal class LegacyNetworkRequestOverrider(
    private val okHttpClient: OkHttpClient
) {

    private val insecureClient: OkHttpClient by lazy {
        with(okHttpClient.newBuilder()) {
            initSPKIleafPinning(this, ALTERNATIVE_API_SPKI_PINS)
            connectTimeout(HTTP_CLIENT_TIMEOUT)
            readTimeout(HTTP_CLIENT_TIMEOUT)
            writeTimeout(HTTP_CLIENT_TIMEOUT)
        }.build()
    }

    fun overrideRequest(
        url: String,
        method: String,
        headers: List<Pair<String, String>>,
        acceptSelfSignedCertificates: Boolean = false,
        body: InputStream? = null,
        bodyType: String? = null
    ): Result {
        val request = createRequest(url, method, headers, body, bodyType)
        val client = if (acceptSelfSignedCertificates) insecureClient else okHttpClient
        val response = client.newCall(request).execute()
        val responseBody = response.body
        val mimeType = response.header("content-type", responseBody?.contentType()?.type)
            ?.substringBefore(";")
        val encoding = response.header("content-encoding", "utf-8")
        return Result(
            mimeType = mimeType,
            encoding = encoding,
            contents = responseBody?.byteStream(),
            httpStatusCode = response.code,
            reasonPhrase = response.message,
            responseHeaders = response.headers.toMap()
        )
    }

    @VisibleForTesting
    internal fun createRequest(
        url: String,
        method: String,
        headers: List<Pair<String, String>>,
        body: InputStream? = null,
        bodyType: String? = null
    ): Request {
        val preparedBody = body?.readBytes()?.toRequestBody(bodyType?.toMediaTypeOrNull())
        return Request.Builder()
            .url(url)
            .method(method, preparedBody)
            .apply { headers.forEach { addHeader(it.first, it.second) } }
            .build()
    }

    data class Result(
        val mimeType: String?,
        val encoding: String?,
        val contents: InputStream?,
        val httpStatusCode: Int,
        val reasonPhrase: String,
        val responseHeaders: Map<String, String>
    )
}
