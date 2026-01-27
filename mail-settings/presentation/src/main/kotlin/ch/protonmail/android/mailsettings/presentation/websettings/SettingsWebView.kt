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

package ch.protonmail.android.mailsettings.presentation.websettings

import android.annotation.SuppressLint
import android.net.http.SslError
import android.os.Build
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import ch.protonmail.android.mailsettings.presentation.websettings.UpsellJsHelper.UpsellInterfaceName
import ch.protonmail.android.mailsettings.presentation.websettings.UpsellJsHelper.upsellJavascriptCode
import ch.protonmail.android.mailupselling.domain.model.UpsellingEntryPoint
import ch.protonmail.android.mailupselling.presentation.model.UpsellingVisibility
import com.google.accompanist.web.AccompanistWebChromeClient
import com.google.accompanist.web.AccompanistWebViewClient
import com.google.accompanist.web.WebView
import com.google.accompanist.web.rememberWebViewState
import timber.log.Timber

@SuppressLint("JavascriptInterface")
@Composable
fun SettingWebView(
    modifier: Modifier = Modifier,
    onUpsell: (UpsellingEntryPoint.Feature, UpsellingVisibility) -> Unit = { entryPoint, _ ->
        Timber.d("Unhandled Upsell entry point $entryPoint")
    },
    state: WebSettingsState.Data
) {

    val isSystemInDarkTheme = isSystemInDarkTheme()
    val isUpsellEligible = state.upsellingVisibility != UpsellingVisibility.Hidden

    // Resolve system theme only if the URL contains the placeholder
    val resolvedUrl = remember(state) {
        if (state.webSettingsUrl.contains(SYSTEM_DEFAULT_THEME_PLACEHOLDER)) {
            val systemParam = if (isSystemInDarkTheme) DARK_THEME_URI_PARAM else LIGHT_THEME_URI_PARAM
            state.webSettingsUrl.replace(SYSTEM_DEFAULT_THEME_PLACEHOLDER, systemParam)
        } else {
            state.webSettingsUrl
        }
    }

    val jsInterface = remember {
        WebSettingsJavaScriptInterface(onUpsell, state.upsellingVisibility)
    }

    val client = remember(state) {
        object : AccompanistWebViewClient() {

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)

                if (isUpsellEligible) {
                    // Evaluate custom JS to make the Upsell compatible with the existing iOS support from the webview.
                    view?.evaluateJavascript(upsellJavascriptCode, null)
                }
            }

            override fun onReceivedSslError(
                view: WebView?,
                handler: SslErrorHandler?,
                error: SslError?
            ) {
                Timber.e("web-settings: onReceivedSslError: $error")
                super.onReceivedSslError(view, handler, error)
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                Timber.e("web-settings: onReceivedError: $error")
                super.onReceivedError(view, request, error)
            }

            override fun onReceivedHttpError(
                view: WebView?,
                request: WebResourceRequest?,
                errorResponse: WebResourceResponse?
            ) {
                errorResponse?.let {
                    Timber.e("web-settings: HTTP error: ${it.statusCode}, ${it.reasonPhrase}, URL: ${request?.url}")
                }
                super.onReceivedHttpError(view, request, errorResponse)
            }
        }
    }

    val chromeClient = remember {
        object : AccompanistWebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                Timber.d("web-settings: onProgressChanged: $newProgress")
                super.onProgressChanged(view, newProgress)
            }
        }
    }

    val webViewState = rememberWebViewState(url = resolvedUrl)

    val cookiesReady = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        WebViewCookieHelper.setSessionCookie(
            url = resolvedUrl,
            sessionId = state.sessionId
        )
        cookiesReady.value = true
    }

    DisposableEffect(Unit) {
        onDispose {
            WebViewCookieHelper.clearSessionCookie(
                url = resolvedUrl
            )
        }
    }

    if (cookiesReady.value) {
        Column(modifier = modifier.fillMaxSize()) {
            WebView(
                onCreated = {
                    it.settings.javaScriptEnabled = true
                    it.settings.domStorageEnabled = false
                    it.settings.loadWithOverviewMode = true
                    it.settings.allowFileAccess = false
                    it.settings.allowContentAccess = false
                    it.settings.useWideViewPort = true
                    it.settings.safeBrowsingEnabled = true
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        it.settings.isAlgorithmicDarkeningAllowed = true
                    }
                    if (isUpsellEligible) {
                        it.addJavascriptInterface(jsInterface, UpsellInterfaceName)
                    }

                    val cookieManager = CookieManager.getInstance()
                    cookieManager.setAcceptThirdPartyCookies(it, true)
                },
                captureBackPresses = true,
                state = webViewState,
                modifier = Modifier.fillMaxSize(),
                client = client,
                chromeClient = chromeClient
            )
        }
    }
}

