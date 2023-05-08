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

package ch.protonmail.android.maildetail.presentation.ui

import android.net.Uri
import android.os.Build
import android.webkit.WebResourceRequest
import android.webkit.WebSettings.FORCE_DARK_OFF
import android.webkit.WebSettings.FORCE_DARK_ON
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import ch.protonmail.android.mailcommon.presentation.NO_CONTENT_DESCRIPTION
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import ch.protonmail.android.mailcommon.presentation.system.LocalDeviceCapabilitiesProvider
import ch.protonmail.android.mailcommon.presentation.compose.pxToDp
import ch.protonmail.android.maildetail.presentation.R
import ch.protonmail.android.maildetail.presentation.model.MessageBodyState
import ch.protonmail.android.maildetail.presentation.model.MessageBodyUiModel
import ch.protonmail.android.mailmessage.domain.entity.MessageId
import com.google.accompanist.web.AccompanistWebChromeClient
import com.google.accompanist.web.AccompanistWebViewClient
import com.google.accompanist.web.WebView
import com.google.accompanist.web.rememberWebViewStateWithHTMLData
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.proton.core.compose.component.ProtonSolidButton
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultSmallWeak

@Composable
@Suppress("LongParameterList", "LongMethod")
internal fun MessageBody(
fun MessageBody(
    modifier: Modifier = Modifier,
    messageBodyUiModel: MessageBodyUiModel,
    actions: MessageBody.Actions,
    messageId: MessageId? = null,
    webViewHeight: Int? = null,
    onMessageBodyLoaded: (messageId: MessageId, height: Int) -> Unit = { _, _ -> },
) {
    val hasWebView = LocalDeviceCapabilitiesProvider.current.hasWebView

    if (hasWebView) {
        MessageBodyWebView(
            modifier = modifier,
            messageBodyUiModel = messageBodyUiModel,
            actions = actions,
            messageId = messageId,
            webViewHeight = webViewHeight,
            onMessageBodyLoaded = onMessageBodyLoaded
        )
    } else {
        MessageBodyNoWebView(
            modifier = modifier
        )
    }
}

@Composable
internal fun MessageBodyWebView(
    modifier: Modifier = Modifier,
    messageBodyUiModel: MessageBodyUiModel,
    actions: MessageBody.Actions,
    messageId: MessageId? = null,
    webViewHeight: Int? = null,
    onMessageBodyLoaded: (messageId: MessageId, height: Int) -> Unit = { _, _ -> },
) {
    val scope = rememberCoroutineScope()
    var contentLoaded by remember { mutableStateOf(false) }
    val state = rememberWebViewStateWithHTMLData(
        data = messageBodyUiModel.messageBody,
        mimeType = messageBodyUiModel.mimeType.value
    )

    val client = remember {
        object : AccompanistWebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                request?.let { actions.onMessageBodyLinkClicked(it.url) }
                return true
            }
        }
    }

    val chromeClient = remember {
        object : AccompanistWebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                if (newProgress == 100) {
                    contentLoaded = true
                }
            }
        }
    }

    val isSystemInDarkTheme = isSystemInDarkTheme()

    WebView(
        onCreated = {
            it.settings.builtInZoomControls = true
            it.settings.displayZoomControls = false
            it.settings.javaScriptEnabled = false
            it.settings.safeBrowsingEnabled = true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                it.settings.isAlgorithmicDarkeningAllowed = true
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                it.settings.forceDark = if (isSystemInDarkTheme) FORCE_DARK_ON else FORCE_DARK_OFF
            }
        },
        captureBackPresses = false,
        state = state,
        modifier = with(
            modifier
                .testTag(MessageBodyTestTags.WebView)
                .fillMaxWidth()
        ) {
            if (webViewHeight == null) {
                onSizeChanged { size ->
                    if (messageId != null && size.height >= 0 && contentLoaded) {
                        scope.launch {
                            delay(WEB_PAGE_CONTENT_LOAD_TIMEOUT)
                            onMessageBodyLoaded(messageId, size.height)
                        }
                    }
                }
            } else {
                height(webViewHeight.pxToDp())
            }
        },
        client = client,
        chromeClient = chromeClient
    )
    if (messageBodyUiModel.attachments != null && messageBodyUiModel.attachments.attachments.isNotEmpty()) {
        AttachmentFooter(
            modifier = Modifier.background(color = ProtonTheme.colors.backgroundNorm),
            messageBodyAttachmentsUiModel = messageBodyUiModel.attachments,
            onShowAllAttachments = actions.onShowAllAttachments
        )
    }
}

@Composable
internal fun MessageBodyNoWebView(
    modifier: Modifier = Modifier
) {
    Text(
        modifier = modifier
            .testTag(MessageBodyTestTags.WebViewAlternative)
            .padding(ProtonDimens.MediumSpacing),
        text = stringResource(id = R.string.message_body_error_no_webview),
    )
}

@Composable
internal fun MessageBodyLoadingError(
    modifier: Modifier = Modifier,
    messageBodyState: MessageBodyState.Error.Data,
    onReload: () -> Unit
) {
    val isNetworkError = messageBodyState.isNetworkError
    val errorMessage = stringResource(
        if (isNetworkError) {
            R.string.error_offline_loading_message
        } else {
            R.string.error_loading_message
        }
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(ProtonTheme.colors.backgroundNorm)
            .padding(horizontal = ProtonDimens.MediumSpacing, vertical = MailDimens.ExtraLargeSpacing),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            modifier = Modifier
                .size(MailDimens.ErrorIconBoxSize)
                .background(ProtonTheme.colors.backgroundSecondary, ProtonTheme.shapes.large)
                .padding(ProtonDimens.MediumSpacing),
            painter = painterResource(id = R.drawable.ic_proton_exclamation_circle),
            contentDescription = NO_CONTENT_DESCRIPTION,
            tint = ProtonTheme.colors.iconHint
        )
        Text(
            modifier = Modifier.padding(top = ProtonDimens.DefaultSpacing),
            text = errorMessage,
            textAlign = TextAlign.Center,
            style = ProtonTheme.typography.defaultSmallWeak
        )
        if (!isNetworkError) {
            ProtonSolidButton(
                modifier = Modifier.padding(top = ProtonDimens.DefaultSpacing),
                onClick = { onReload() }
            ) {
                Text(text = stringResource(id = R.string.reload))
            }
        }
    }
}

object MessageBody {

    data class Actions(
        val onMessageBodyLinkClicked: (uri: Uri) -> Unit,
        val onShowAllAttachments: () -> Unit
    )
}

object MessageBodyTestTags {

    const val WebView = "MessageBodyWebView"
    const val WebViewAlternative = "MessageBodyWithoutWebView"
}

private const val WEB_PAGE_CONTENT_LOAD_TIMEOUT = 500L
