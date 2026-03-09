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

package me.proton.android.core.humanverification.presentation

import java.io.ByteArrayInputStream
import android.net.http.SslError
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import kotlinx.coroutines.runBlocking
import me.proton.android.core.humanverification.presentation.webview.ProtonWebViewClient
import me.proton.core.util.kotlin.CoreLogger
import me.proton.core.util.kotlin.annotation.ExcludeFromCoverage
import me.proton.core.util.kotlin.takeIfNotBlank
import uniffi.mail_uniffi.ChallengeLoader
import uniffi.mail_uniffi.ChallengeLoaderGetResult
import uniffi.mail_uniffi.Header
import uniffi.mail_uniffi.HumanVerificationViewLoadingStatus
import uniffi.mail_uniffi.Query
import kotlin.text.Charsets.UTF_8

/** Used to override HTTP headers to access captcha iframe on debug from outside the VPN */
class HumanVerificationWebViewClient(
    headers: List<Pair<String, String>>?,
    private val loader: ChallengeLoader,
    private val onResourceLoadingError: (response: WebResponseError?) -> Unit
) : ProtonWebViewClient(headers) {

    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
        runBlocking {
            val host = request.url.host
            val path = request.url.path
            when {
                host == null -> null
                path == null -> null
                else -> when (
                    val result =
                        loader.get(
                            base = host,
                            path = path,
                            header = request.requestHeaders?.map { Header(it.key, it.value) } ?: emptyList(),
                            query = request.url.queryParameterNames.map {
                                Query(key = it, `val` = request.url.getQueryParameter(it))
                            }
                        )
                ) {
                    is ChallengeLoaderGetResult.Error -> null
                    is ChallengeLoaderGetResult.Ok -> {
                        val response = result.v1
                        val reasonPhrase = response.reason?.takeIfNotBlank() ?: "UNKNOWN"
                        val isAlternativeUrl = false // we assume no alt routing for now
                        val filteredHeaders = response.headers.filterNot {
                            it.key.contains(CSP_HEADER) && isAlternativeUrl
                        }.associate {
                            it.key to it.`val`
                        }

                        WebResourceResponse(
                            response.contentType,
                            UTF_8.name(),
                            response.status.toInt(),
                            reasonPhrase,
                            filteredHeaders,
                            ByteArrayInputStream(response.contents)
                        )
                    }
                }
            }
        }

    override fun onReceivedHttpError(
        view: WebView?,
        request: WebResourceRequest?,
        errorResponse: WebResourceResponse?
    ) {
        super.onReceivedHttpError(view, request, errorResponse)
        val logMessage = "Request failed: ${request?.method} ${request?.url} with " +
            "status ${errorResponse?.statusCode} ${errorResponse?.reasonPhrase}"
        CoreLogger.i(LogTag.HV_REQUEST_ERROR, logMessage)
        onResourceLoadingError(errorResponse?.let { WebResponseError.Http(it) })
    }

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?
    ) {
        super.onReceivedError(view, request, error)
        val logMessage = "Request failed: ${request?.method} ${request?.url} with " +
            "code ${error?.errorCode} ${error?.description}"
        CoreLogger.i(LogTag.HV_REQUEST_ERROR, logMessage)
        onResourceLoadingError(error?.let { WebResponseError.Resource(it) })
    }

    companion object {

        private const val CSP_HEADER = "content-security-policy"
    }
}

@ExcludeFromCoverage
sealed class WebResponseError {

    data class Http(val response: WebResourceResponse) : WebResponseError()
    data class Ssl(val error: SslError) : WebResponseError()
    data class Resource(val error: WebResourceError) : WebResponseError()
}

@Suppress("MagicNumber")
internal fun WebResponseError?.toHumanVerificationViewLoadingStatus(): HumanVerificationViewLoadingStatus =
    when (this) {
        is WebResponseError.Http -> when (response.statusCode) {
            in 200..299 -> HumanVerificationViewLoadingStatus.HTTP2XX
            400 -> HumanVerificationViewLoadingStatus.HTTP400
            404 -> HumanVerificationViewLoadingStatus.HTTP404
            422 -> HumanVerificationViewLoadingStatus.HTTP422
            in 400..499 -> HumanVerificationViewLoadingStatus.HTTP4XX
            in 500..599 -> HumanVerificationViewLoadingStatus.HTTP5XX
            else -> HumanVerificationViewLoadingStatus.CONNECTION_ERROR
        }

        is WebResponseError.Ssl -> HumanVerificationViewLoadingStatus.SSL_ERROR
        is WebResponseError.Resource -> HumanVerificationViewLoadingStatus.CONNECTION_ERROR
        else -> HumanVerificationViewLoadingStatus.CONNECTION_ERROR
    }
