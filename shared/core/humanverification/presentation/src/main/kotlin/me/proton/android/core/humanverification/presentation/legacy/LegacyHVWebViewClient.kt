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

import android.annotation.SuppressLint
import android.net.Uri
import android.net.http.SslError
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.core.net.toUri
import me.proton.android.core.humanverification.presentation.webview.ProtonWebViewClient
import me.proton.android.core.humanverification.presentation.LogTag
import me.proton.android.core.humanverification.presentation.WebResponseError
import me.proton.core.util.kotlin.CoreLogger
import me.proton.core.util.kotlin.takeIfNotBlank

internal class LegacyHVWebViewClient(
    private val originalHost: String?,
    private val alternativeHost: String?,
    private val extraHeaders: List<Pair<String, String>>,
    private val networkRequestOverrider: LegacyNetworkRequestOverrider,
    private val onWebError: (response: WebResponseError?) -> Unit
) : ProtonWebViewClient(extraHeaders) {

    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
        return when {
            request.method != "GET" -> {
                // It's not possible to override a POST request, because
                // WebResourceRequest doesn't provide access to POST body.
                null
            }

            request.url.isAlternativeHost() -> overrideForDoH(request, extraHeaders)
            extraHeaders.isNotEmpty() -> overrideWithExtraHeaders(request, extraHeaders)
            else -> null
        }
    }

    @SuppressLint("WebViewClientOnReceivedSslError")
    override fun onReceivedSslError(
        view: WebView,
        handler: SslErrorHandler,
        error: SslError
    ) {
        if (error.isAlternativeSelfSignedCert()) {
            handler.proceed()
        } else {
            handler.cancel()
        }
    }

    override fun onReceivedHttpError(
        view: WebView?,
        request: WebResourceRequest?,
        errorResponse: WebResourceResponse?
    ) {
        super.onReceivedHttpError(view, request, errorResponse)
        val logMessage = "Request failed (HTTP): ${request?.method} ${request?.url} with " +
            "status ${errorResponse?.statusCode} ${errorResponse?.reasonPhrase}"
        CoreLogger.i(LogTag.HV_REQUEST_ERROR, logMessage)
        onWebError(errorResponse?.let { WebResponseError.Http(it) })
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
        onWebError(error?.let { WebResponseError.Resource(it) })
    }

    private fun overrideWithExtraHeaders(
        request: WebResourceRequest,
        extraHeaders: List<Pair<String, String>>
    ): WebResourceResponse? = overrideRequest(
        request.url.toString(),
        request.method,
        headers = request.requestHeaders.toList() + extraHeaders,
        acceptSelfSignedCertificates = false
    )

    private fun overrideForDoH(
        request: WebResourceRequest,
        extraHeaders: List<Pair<String, String>>
    ): WebResourceResponse? {
        // This allows a custom redirection to HumanVerificationApiHost Url from the DoH one.
        // Must be skipped for the internal captcha request and any Proton API requests.
        val dohHeader = if (request.url.isLoadCaptchaUrl() || request.url.isProtonApiUrl()) {
            null
        } else {
            "X-PM-DoH-Host" to originalHost
        }
        return overrideRequest(
            request.url.toString(),
            request.method,
            headers = request.requestHeaders.toList() + extraHeaders + listOfNotNull(dohHeader),
            acceptSelfSignedCertificates = true
        ).also {
            if (it?.statusCode !in HTTP_SUCCESS_RANGE) {
                onWebError(it?.let { WebResponseError.Http(it) })
            }
        }
    }

    private fun overrideRequest(
        url: String,
        method: String,
        headers: List<Pair<String, String>>,
        acceptSelfSignedCertificates: Boolean
    ): WebResourceResponse? = runCatching {
        CoreLogger.i(
            LogTag.DEFAULT,
            "$method $url headers=$headers acceptSelfSignedCertificates=$acceptSelfSignedCertificates"
        )

        val response = networkRequestOverrider.overrideRequest(
            url,
            method,
            headers,
            acceptSelfSignedCertificates
        )

        if (response.httpStatusCode !in HTTP_SUCCESS_RANGE) {
            val logMessage = "Request with override failed: $method $url with " +
                "code ${response.httpStatusCode} ${response.reasonPhrase}"
            CoreLogger.i(LogTag.HV_REQUEST_ERROR, logMessage)
        }

        // We need to remove the CSP header for DoH to work
        val needsCspRemoval = response.responseHeaders.containsKey(CSP_HEADER)
        val filteredHeaders = if (needsCspRemoval) {
            response.responseHeaders.toMutableMap().apply { remove(CSP_HEADER) }
        } else {
            response.responseHeaders
        }

        // Copy the set-cookie headers from the overridden request into the default cookie manager
        // to ensure they are sent on requests the web app makes
        val cookieHeaders =
            response.responseHeaders.filter { (key) -> key.lowercase() == "set-cookie" }
        val cookieManager = CookieManager.getInstance()
        cookieHeaders.entries.forEach { entry -> cookieManager.setCookie(url, entry.value) }

        // HTTP/2 removed Reason-Phrase from the spec, but the constructor
        // for WebResourceResponse would throw if it received a blank string.
        val reasonPhrase = response.reasonPhrase.takeIfNotBlank() ?: "UNKNOWN"

        WebResourceResponse(
            response.mimeType,
            response.encoding,
            response.httpStatusCode,
            reasonPhrase,
            filteredHeaders,
            response.contents
        )
    }.onFailure {
        CoreLogger.e(LogTag.DEFAULT, it, "Cannot load url=$url")
    }.getOrNull()

    private fun SslError.isAlternativeSelfSignedCert(): Boolean = when {
        !url.toUri().isAlternativeHost() -> false
        primaryError == SslError.SSL_UNTRUSTED -> certificate.isTrustedByLeafSPKIPinning()
        else -> false
    }

    private fun Uri.isAlternativeHost(): Boolean = if (alternativeHost != null) {
        host == alternativeHost
    } else false

    private fun Uri.isLoadCaptchaUrl() = path?.endsWith("/core/v4/captcha") == true
    private fun Uri.isProtonApiUrl() = path?.startsWith("/api/") == true

    companion object {

        private const val CSP_HEADER = "content-security-policy"
        private val HTTP_SUCCESS_RANGE = 200 until 400
    }
}
