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
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import ch.protonmail.android.mailcommon.presentation.compose.pxToDp
import ch.protonmail.android.mailmessage.domain.model.AttachmentId
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.usecase.GetEmbeddedImageResult
import ch.protonmail.android.mailmessage.presentation.extension.isEmbeddedImage
import ch.protonmail.android.mailmessage.presentation.extension.isRemoteContent
import ch.protonmail.android.mailmessage.presentation.model.MessageBodyUiModel
import com.google.accompanist.web.AccompanistWebChromeClient
import com.google.accompanist.web.AccompanistWebViewClient
import com.google.accompanist.web.WebView
import com.google.accompanist.web.rememberWebViewStateWithHTMLData
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.proton.core.compose.theme.ProtonTheme

@Composable
fun MessageBodyWebView(
    modifier: Modifier = Modifier,
    messageBodyUiModel: MessageBodyUiModel,
    actions: MessageBodyWebView.Actions,
    webViewHeight: Int? = null,
    onMessageBodyLoaded: (messageId: MessageId, height: Int) -> Unit = { _, _ -> }
) {
    val scope = rememberCoroutineScope()
    var contentLoaded by remember { mutableStateOf(false) }
    val state = rememberWebViewStateWithHTMLData(
        data = messageBodyUiModel.messageBody,
        mimeType = messageBodyUiModel.mimeType.value
    )
    val messageId = messageBodyUiModel.messageId

    val client = remember {
        object : AccompanistWebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                request?.let { actions.onMessageBodyLinkClicked(it.url) }
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

    Column(modifier = modifier) {
        WebView(
            onCreated = {
                it.settings.builtInZoomControls = true
                it.settings.displayZoomControls = false
                it.settings.javaScriptEnabled = false
                it.settings.safeBrowsingEnabled = true
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    it.settings.isAlgorithmicDarkeningAllowed = true
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    it.settings.forceDark =
                        if (isSystemInDarkTheme) WebSettings.FORCE_DARK_ON else WebSettings.FORCE_DARK_OFF
                }
            },
            captureBackPresses = false,
            state = state,
            modifier = with(
                Modifier
                    .testTag(MessageBodyWebViewTestTags.WebView)
                    .fillMaxWidth()
            ) {
                if (webViewHeight == null) {
                    onSizeChanged { size ->
                        if (size.height >= 0 && contentLoaded) {
                            scope.launch {
                                delay(WEB_PAGE_CONTENT_LOAD_TIMEOUT)
                                onMessageBodyLoaded(messageId, size.height)
                            }
                        }
                    }
                } else if (webViewHeight < WEB_VIEW_FIXED_MAX_HEIGHT) {
                    height(webViewHeight.pxToDp())
                } else {
                    this
                }
            },
            client = client,
            chromeClient = chromeClient
        )
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

object MessageBodyWebView {

    data class Actions(
        val onMessageBodyLinkClicked: (uri: Uri) -> Unit,
        val onShowAllAttachments: () -> Unit,
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
