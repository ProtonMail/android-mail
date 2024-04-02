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

package ch.protonmail.android.mailmessage.presentation.ui

import java.io.ByteArrayInputStream
import android.net.Uri
import android.os.Build
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import ch.protonmail.android.mailcommon.presentation.NO_CONTENT_DESCRIPTION
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import ch.protonmail.android.mailcommon.presentation.compose.pxToDp
import ch.protonmail.android.mailmessage.domain.model.AttachmentId
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.MimeType
import ch.protonmail.android.mailmessage.domain.usecase.GetEmbeddedImageResult
import ch.protonmail.android.mailmessage.presentation.R
import ch.protonmail.android.mailmessage.presentation.extension.isEmbeddedImage
import ch.protonmail.android.mailmessage.presentation.extension.isRemoteContent
import ch.protonmail.android.mailmessage.presentation.model.MessageBodyExpandCollapseMode
import ch.protonmail.android.mailmessage.presentation.model.MessageBodyUiModel
import com.google.accompanist.web.AccompanistWebChromeClient
import com.google.accompanist.web.AccompanistWebViewClient
import com.google.accompanist.web.WebView
import com.google.accompanist.web.rememberWebViewStateWithHTMLData
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme

@Composable
fun MessageBodyWebView(
    modifier: Modifier = Modifier,
    messageBodyUiModel: MessageBodyUiModel,
    bodyDisplayMode: MessageBodyExpandCollapseMode,
    actions: MessageBodyWebView.Actions,
    onMessageBodyLoaded: (messageId: MessageId, height: Int) -> Unit = { _, _ -> }
) {
    val scope = rememberCoroutineScope()
    var contentLoaded by remember { mutableStateOf(false) }
    val state = rememberWebViewStateWithHTMLData(
        data = if (bodyDisplayMode == MessageBodyExpandCollapseMode.Collapsed) {
            messageBodyUiModel.messageBodyWithoutQuote
        } else {
            messageBodyUiModel.messageBody
        },
        mimeType = MimeType.Html.value
    )
    val messageId = messageBodyUiModel.messageId

    val client = remember(messageBodyUiModel.shouldShowRemoteContent, messageBodyUiModel.shouldShowEmbeddedImages) {
        object : AccompanistWebViewClient() {

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                request?.let {
                    actions.onMessageBodyLinkClicked(it.url)
                }
                return true
            }

            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                return if (!messageBodyUiModel.shouldShowRemoteContent && request?.isRemoteContent() == true) {
                    WebResourceResponse("", "", null)
                } else if (messageBodyUiModel.shouldShowEmbeddedImages && request?.isEmbeddedImage() == true) {
                    actions.loadEmbeddedImage(messageId, "<${request.url.schemeSpecificPart}>")?.let {
                        WebResourceResponse(it.mimeType, "", ByteArrayInputStream(it.data))
                    }
                } else {
                    super.shouldInterceptRequest(view, request)
                }
            }

            override fun onLoadResource(view: WebView?, url: String?) {
                if (!messageBodyUiModel.shouldShowRemoteContent && url?.isRemoteContent() == true) {
                    return
                }
                super.onLoadResource(view, url)
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

    // This is will on be used if the WebView height is higher than the max constraint.
    var webViewHeightPx by remember { mutableIntStateOf(0) }

    Column(modifier = modifier) {
        key(client) {
            WebView(
                onCreated = {
                    it.settings.builtInZoomControls = true
                    it.settings.displayZoomControls = false
                    it.settings.javaScriptEnabled = false
                    it.settings.safeBrowsingEnabled = true
                    it.settings.allowContentAccess = false
                    it.settings.allowFileAccess = false
                    it.settings.loadWithOverviewMode = true
                    it.settings.useWideViewPort = true
                    configureDarkLightMode(it, isSystemInDarkTheme)
                },
                captureBackPresses = false,
                state = state,
                modifier = with(
                    Modifier
                        .testTag(MessageBodyWebViewTestTags.WebView)
                        .padding(ProtonDimens.DefaultSpacing)
                        .fillMaxWidth()
                        // There are no guarantees onSizeChanged will not be re-invoked with the same size.
                        // We need to take our own measures to avoid callback with the same size.
                        .onSizeChanged { size ->
                            if (size.height >= 0 && contentLoaded) {
                                scope.launch {
                                    delay(WEB_PAGE_CONTENT_LOAD_TIMEOUT)
                                    if (webViewHeightPx != size.height) {
                                        onMessageBodyLoaded(messageId, size.height)
                                        webViewHeightPx = size.height
                                    }
                                }
                            }
                        }
                ) {
                    if (webViewHeightPx < WEB_VIEW_FIXED_MAX_HEIGHT) {
                        this
                    } else {
                        height((WEB_VIEW_FIXED_MAX_HEIGHT - 1).pxToDp())
                    }
                },
                client = client,
                chromeClient = chromeClient
            )
        }
        if (bodyDisplayMode != MessageBodyExpandCollapseMode.NotApplicable) {
            ExpandCollapseBodyButton(
                modifier = Modifier.offset(x = ProtonDimens.SmallSpacing),
                onClick = { actions.onExpandCollapseButtonCLicked() }
            )
        }
        val attachmentsUiModel = messageBodyUiModel.attachments
        if (attachmentsUiModel != null && attachmentsUiModel.attachments.isNotEmpty()) {
            AttachmentFooter(
                modifier = Modifier.background(color = ProtonTheme.colors.backgroundNorm),
                messageBodyAttachmentsUiModel = attachmentsUiModel,
                actions = AttachmentFooter.Actions(
                    onShowAllAttachments = actions.onShowAllAttachments,
                    onAttachmentClicked = actions.onAttachmentClicked
                )
            )
        }
    }
}

private fun configureDarkLightMode(webView: WebView, shouldDarkenWebView: Boolean) {
    if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            webView.settings.isAlgorithmicDarkeningAllowed = shouldDarkenWebView
        } else {
            WebSettingsCompat.setAlgorithmicDarkeningAllowed(
                webView.settings, shouldDarkenWebView
            )
        }
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        webView.settings.forceDark =
            if (shouldDarkenWebView) WebSettings.FORCE_DARK_ON else WebSettings.FORCE_DARK_OFF
    }
}

@Composable
private fun ExpandCollapseBodyButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        modifier = modifier
            .padding(ProtonDimens.ExtraSmallSpacing)
            .height(MessageBodyDimens.ExpandButtonHeight)
            .width(MessageBodyDimens.ExpandButtonWidth),
        contentPadding = PaddingValues(0.dp),
        shape = RoundedCornerShape(MessageBodyDimens.ExpandButtonRoundedCornerPercent),
        border = BorderStroke(MailDimens.DefaultBorder, ProtonTheme.colors.shade20),
        colors = ButtonDefaults.buttonColors(backgroundColor = ProtonTheme.colors.backgroundNorm),
        elevation = ButtonDefaults.elevation(defaultElevation = 0.dp),
        onClick = { onClick() }
    ) {
        Icon(
            modifier = Modifier
                .size(MessageBodyDimens.ExpandButtonHeight)
                .align(Alignment.CenterVertically),
            painter = painterResource(id = R.drawable.ic_proton_three_dots_horizontal),
            tint = ProtonTheme.colors.iconNorm,
            contentDescription = NO_CONTENT_DESCRIPTION
        )
    }
}

@Preview
@Composable
private fun ExpandCollapseBodyButtonPreview() {
    ProtonTheme {
        ExpandCollapseBodyButton(onClick = {})
    }
}


object MessageBodyWebView {

    data class Actions(
        val onMessageBodyLinkClicked: (uri: Uri) -> Unit,
        val onShowAllAttachments: () -> Unit,
        val onExpandCollapseButtonCLicked: () -> Unit,
        val onAttachmentClicked: (attachmentId: AttachmentId) -> Unit,
        val loadEmbeddedImage: (messageId: MessageId, contentId: String) -> GetEmbeddedImageResult?
    )

}

object MessageBodyWebViewTestTags {

    const val WebView = "MessageBodyWebView"
}

private const val WEB_PAGE_CONTENT_LOAD_TIMEOUT = 500L

// Max constraint for WebView height. If the height is greater
// than this value, we will not fix the height of the WebView or it will crash.
// (Limit set in androidx.compose.ui.unit.Constraints)
private const val WEB_VIEW_FIXED_MAX_HEIGHT = 262_143
