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

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.graphics.Color
import android.webkit.ConsoleMessage
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.ProgressIndicatorDefaults
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarResult
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.web.AccompanistWebChromeClient
import com.google.accompanist.web.WebView
import com.google.accompanist.web.rememberWebViewState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import me.proton.android.core.humanverification.presentation.legacy.LegacyHVWebViewClient
import me.proton.android.core.humanverification.presentation.legacy.LegacyNetworkRequestOverrider
import me.proton.core.compose.component.DeferredCircularProgressIndicator
import me.proton.core.compose.component.ProtonCloseButton
import me.proton.core.compose.component.ProtonSnackbarHost
import me.proton.core.compose.component.ProtonSnackbarHostState
import me.proton.core.compose.component.ProtonSnackbarType
import me.proton.core.compose.component.ProtonTextButton
import me.proton.core.compose.component.appbar.ProtonTopAppBar
import me.proton.core.compose.theme.LocalColors
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultStrongNorm
import me.proton.core.compose.util.LaunchOnScreenView
import me.proton.core.compose.util.LaunchResumeEffect
import me.proton.core.util.kotlin.CoreLogger
import okhttp3.OkHttpClient
import kotlin.time.Duration.Companion.milliseconds

private const val JS_INTERFACE_NAME = "AndroidInterface"
private const val WEB_VIEW_MAX_COLOR_COMPONENT = 255

@Composable
@Suppress("UseComposableActions")
fun HumanVerificationScreen(
    modifier: Modifier = Modifier,
    onCancel: () -> Unit,
    onHelpClicked: () -> Unit,
    onSuccess: (String) -> Unit = {},
    url: String,
    originalHost: String?,
    alternativeHost: String?,
    defaultCountry: String? = null,
    recoveryPhone: String? = null,
    locale: String? = null,
    headers: List<Pair<String, String>>? = null,
    viewModel: HumanVerificationViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchOnScreenView(enqueue = viewModel::onScreenView)

    HumanVerificationScreen(
        modifier = modifier,
        onCloseClicked = { viewModel.perform(HumanVerificationAction.Cancel()) },
        onCancel = onCancel,
        onIdle = {
            viewModel.perform(
                HumanVerificationAction.Load(
                    url = url,
                    originalHost = originalHost,
                    alternativeHost = alternativeHost,
                    defaultCountry = defaultCountry,
                    recoveryPhone = recoveryPhone,
                    locale = locale,
                    headers = headers
                )
            )
        },
        onBackClicked = { viewModel.perform(HumanVerificationAction.Cancel()) },
        onHelpClicked = onHelpClicked,
        onSuccess = onSuccess,
        onHVMessage = { viewModel.perform(HumanVerificationAction.HVMessage(it)) },
        onWebError = { viewModel.perform(HumanVerificationAction.WebError(it)) },
        headers = headers,
        state = state,
        events = viewModel.uiEvent
    )
}

@Composable
@Suppress("UseComposableActions")
fun HumanVerificationScreen(
    modifier: Modifier = Modifier,
    onCloseClicked: () -> Unit = {},
    onBackClicked: () -> Unit = {},
    onCancel: () -> Unit = {},
    onIdle: () -> Unit = {},
    onHelpClicked: () -> Unit = {},
    onHVMessage: (HV3ResponseMessage) -> Unit = {},
    onWebError: (response: WebResponseError?) -> Unit = {},
    onSuccess: (String) -> Unit = {},
    headers: List<Pair<String, String>>?,
    state: HumanVerificationViewState,
    events: Flow<HumanVerificationViewEvent>
) {
    val generalErrorMessage = stringResource(R.string.human_verification_general_error)
    val retryText = stringResource(R.string.presentation_retry)
    val snackbarHostState = remember { ProtonSnackbarHostState() }

    LaunchResumeEffect(events) {
        events.collect { event ->
            when (event) {
                is HumanVerificationViewEvent.HvNotification -> snackbarHostState.showSnackbar(
                    type = when (event.messageType) {
                        HV3ResponseMessage.MessageType.Success -> ProtonSnackbarType.SUCCESS
                        HV3ResponseMessage.MessageType.Error -> ProtonSnackbarType.ERROR
                        else -> ProtonSnackbarType.NORM
                    },
                    message = event.message
                )

                is HumanVerificationViewEvent.Success -> onSuccess(event.token)
            }
        }
    }

    LaunchedEffect(state) {
        when (state) {
            is HumanVerificationViewState.Load -> Unit
            is HumanVerificationViewState.Cancel -> onCancel()
            is HumanVerificationViewState.Idle -> onIdle()
            is HumanVerificationViewState.GenericError -> snackbarHostState.showSnackbar(
                type = ProtonSnackbarType.ERROR,
                message = state.message ?: generalErrorMessage,
                duration = SnackbarDuration.Indefinite,
                actionLabel = retryText
            ).let {
                if (it == SnackbarResult.ActionPerformed) {
                    onIdle()
                }
            }
        }
    }

    HumanVerificationScaffold(
        modifier = modifier,
        onCloseClicked = onCloseClicked,
        onBackClicked = onBackClicked,
        onHelpClicked = onHelpClicked,
        onHVMessage = onHVMessage,
        onWebError = onWebError,
        headers = headers,
        state = state,
        snackbarHostState = snackbarHostState
    )
}

@Composable
@Suppress("UseComposableActions")
fun HumanVerificationScaffold(
    modifier: Modifier = Modifier,
    onCloseClicked: () -> Unit = {},
    onBackClicked: () -> Unit,
    onHelpClicked: () -> Unit = {},
    onHVMessage: (HV3ResponseMessage) -> Unit,
    onWebError: (response: WebResponseError?) -> Unit,
    headers: List<Pair<String, String>>?,
    state: HumanVerificationViewState = HumanVerificationViewState.Idle,
    snackbarHostState: ProtonSnackbarHostState = remember { ProtonSnackbarHostState() }
) {
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            ProtonTopAppBar(
                title = {
                    Text(text = stringResource(id = R.string.human_verification_title))
                },
                navigationIcon = {
                    ProtonCloseButton(onCloseClicked = onCloseClicked)
                },
                actions = {
                    ProtonTextButton(
                        onClick = onHelpClicked
                    ) {
                        Text(
                            text = stringResource(id = R.string.human_verification_help),
                            color = ProtonTheme.colors.textAccent,
                            style = ProtonTheme.typography.defaultStrongNorm
                        )
                    }
                },
                backgroundColor = LocalColors.current.backgroundNorm,
                windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
            )
        },
        snackbarHost = {
            ProtonSnackbarHost(snackbarHostState)
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            HumanVerificationView(
                modifier = modifier,
                onBackClicked = onBackClicked,
                onHVMessage = onHVMessage,
                onWebError = onWebError,
                headers = headers,
                state = state
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Suppress("UseComposableActions")
@Composable
private fun HumanVerificationView(
    modifier: Modifier = Modifier,
    onBackClicked: () -> Unit,
    onHVMessage: (HV3ResponseMessage) -> Unit,
    onWebError: (response: WebResponseError?) -> Unit,
    headers: List<Pair<String, String>>?,
    state: HumanVerificationViewState = HumanVerificationViewState.Idle
) {
    when (state) {
        is HumanVerificationViewState.Load -> {
            HumanVerificationWebView(
                modifier = modifier,
                headers = headers,
                onHVMessage = onHVMessage,
                onWebError = onWebError,
                state = state
            )
        }

        is HumanVerificationViewState.Idle -> DeferredCircularProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .semantics { testTag = "progressIndicator" },
            defer = 0.milliseconds
        )

        else -> Unit
    }

    BackHandler(true) {
        onBackClicked()
    }
}

@Composable
private fun HumanVerificationWebView(
    modifier: Modifier = Modifier,
    onHVMessage: (HV3ResponseMessage) -> Unit,
    onWebError: (response: WebResponseError?) -> Unit,
    headers: List<Pair<String, String>>?,
    state: HumanVerificationViewState.Load
) {
    val scope = rememberCoroutineScope()
    var progress by remember { mutableIntStateOf(0) }
    val animatedProgress by animateFloatAsState(
        targetValue = progress / 100.0f,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec
    )
    val webViewState = rememberWebViewState(url = state.fullUrl, headers.orEmpty().toMap())
    val webViewClient = remember {
        LegacyHVWebViewClient(
            originalHost = state.originalHost,
            alternativeHost = state.alternativeHost,
            extraHeaders = state.extraHeaders.orEmpty(),
            networkRequestOverrider = LegacyNetworkRequestOverrider(OkHttpClient()),
            onWebError = onWebError
        )
    }
    val webChromeClient = remember {
        object : AccompanistWebChromeClient() {
            override fun onConsoleMessage(message: ConsoleMessage): Boolean {
                CoreLogger.i(
                    LogTag.DEFAULT,
                    "Web console: ${message.message()} -- file ${message.sourceId()}(${message.lineNumber()})"
                )
                return true
            }

            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                progress = newProgress
            }
        }
    }
    WebView(
        state = webViewState,
        onCreated = { webView ->
            WebView.setWebContentsDebuggingEnabled(state.isWebViewDebuggingEnabled)

            @SuppressLint("SetJavaScriptEnabled")
            webView.settings.javaScriptEnabled = true
            webView.settings.domStorageEnabled = true
            webView.setBackgroundColor(
                Color.argb(
                    1,
                    WEB_VIEW_MAX_COLOR_COMPONENT,
                    WEB_VIEW_MAX_COLOR_COMPONENT,
                    WEB_VIEW_MAX_COLOR_COMPONENT
                )
            )
            webView.addJavascriptInterface(VerificationJSInterface(scope, onHVMessage), JS_INTERFACE_NAME)
        },
        modifier = modifier.fillMaxSize(),
        client = webViewClient,
        chromeClient = webChromeClient
    )

    AnimatedVisibility(
        visible = animatedProgress < 1.0f,
        modifier = Modifier.fillMaxWidth()
    ) {
        LinearProgressIndicator(
            progress = animatedProgress,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview(name = "Light mode", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
internal fun HumanVerificationScreenPreview() {
    ProtonTheme {
        HumanVerificationScreen(
            state = HumanVerificationViewState.Idle,
            events = emptyFlow(),
            headers = emptyList()
        )
    }
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
internal fun HumanVerificationScreenDarkPreview() {
    ProtonTheme {
        HumanVerificationScreen(
            state = HumanVerificationViewState.Idle,
            events = emptyFlow(),
            headers = emptyList()
        )
    }
}
