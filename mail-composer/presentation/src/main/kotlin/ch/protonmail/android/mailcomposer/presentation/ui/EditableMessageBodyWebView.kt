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

package ch.protonmail.android.mailcomposer.presentation.ui

import java.io.ByteArrayInputStream
import android.annotation.SuppressLint
import android.content.Context
import android.view.ViewGroup.LayoutParams
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.mailcommon.presentation.ConsumableLaunchedEffect
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.compose.pxToDp
import ch.protonmail.android.mailcomposer.presentation.model.DraftDisplayBodyUiModel
import ch.protonmail.android.mailcomposer.presentation.model.editor.CursorPosition
import ch.protonmail.android.mailcomposer.presentation.model.editor.EditorViewDrawingState
import ch.protonmail.android.mailcomposer.presentation.ui.util.ComposerFocusUtils
import ch.protonmail.android.mailmessage.domain.model.MessageBodyImage
import ch.protonmail.android.mailmessage.domain.model.MimeType
import ch.protonmail.android.mailmessage.presentation.extension.isEmbeddedImage
import ch.protonmail.android.mailmessage.presentation.extension.isRemoteContent
import ch.protonmail.android.mailmessage.presentation.ui.showInDarkMode
import ch.protonmail.android.mailmessage.presentation.ui.showInLightMode
import ch.protonmail.android.uicomponents.keyboardVisibilityAsState
import kotlinx.coroutines.delay
import org.json.JSONObject
import timber.log.Timber

// Needed to allow "remember" on javascript interface
@SuppressLint("JavascriptInterface")
@Composable
fun EditableMessageBodyWebView(
    modifier: Modifier = Modifier,
    messageBodyUiModel: DraftDisplayBodyUiModel,
    shouldRequestFocus: Effect<Unit>,
    focusRequester: FocusRequester,
    injectInlineAttachments: Effect<List<String>>,
    stripInlineAttachment: Effect<String>,
    refreshBody: Effect<DraftDisplayBodyUiModel>,
    webViewActions: EditableMessageBodyWebView.Actions,
    viewportCoordinateAlignmentEnabled: Boolean
) {

    val isSystemInDarkTheme = isSystemInDarkTheme()

    var webView by remember { mutableStateOf<WebView?>(null) }
    var currentCursorPosition by remember { mutableStateOf(CursorPosition(0f, 0f)) }
    var currentLineHeightPx by remember { mutableStateOf(0f) }
    var webViewHeightPx by remember { mutableIntStateOf(0) }
    var bodyContentVersion by remember { mutableIntStateOf(0) }
    val isKeyboardVisible by keyboardVisibilityAsState()

    LaunchedEffect(key1 = webViewHeightPx, key2 = isKeyboardVisible) {
        webViewActions.onWebViewParamsChanged(
            EditorViewDrawingState(
                heightPx = webViewHeightPx.toFloat(),
                cursorPosition = currentCursorPosition,
                lineHeightPx = currentLineHeightPx,
                bodyContentVersion = bodyContentVersion,
                isKeyboardVisible = isKeyboardVisible
            )
        )
    }

    fun onCursorPositionChanged(positionPx: Float, lineHeightPx: Float) {
        currentCursorPosition = CursorPosition(topPx = positionPx, bottomPx = positionPx + lineHeightPx)
        currentLineHeightPx = lineHeightPx

        webViewActions.onWebViewParamsChanged(
            EditorViewDrawingState(
                heightPx = webViewHeightPx.toFloat(),
                cursorPosition = currentCursorPosition,
                lineHeightPx = currentLineHeightPx,
                bodyContentVersion = bodyContentVersion,
                isKeyboardVisible = isKeyboardVisible
            )
        )
    }

    val javascriptCallback = remember {
        JavascriptCallback(
            sanitizePastedText = { mimeType, text ->
                webViewActions.sanitizePastedText(mimeType, text)
            },
            onMessageBodyChanged = { body ->
                webViewActions.onMessageBodyChanged(body)
                bodyContentVersion++
            },
            onCursorPositionChanged = ::onCursorPositionChanged,
            onInlineImageRemoved = webViewActions.onInlineImageRemoved,
            onInlineImageClicked = webViewActions.onInlineImageClicked,
            onInlineImagePasted = webViewActions.onInlineImagePasted
        )
    }

    val contentLoadingFinished = remember { mutableStateOf(false) }
    val client = object : WebViewClient() {

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            contentLoadingFinished.value = true
        }

        override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
            return if (request?.isRemoteContent() == true || request?.isEmbeddedImage() == true) {
                request.url?.toString()?.let { url ->
                    webViewActions.loadImage(url)?.let {
                        WebResourceResponse(it.mimeType, "", ByteArrayInputStream(it.data))
                    }
                } ?: super.shouldInterceptRequest(view, request)
            } else {
                super.shouldInterceptRequest(view, request)
            }
        }
    }

    webView?.let { wv ->
        LaunchedEffect(isSystemInDarkTheme) {
            configureDarkLightMode(wv, isSystemInDarkTheme)
        }
        LaunchedEffect(wv) {
            Timber.d("editor-webview: setting initial value on webview")
            wv.loadDataWithBaseURL(
                null,
                messageBodyUiModel.value, MimeType.Html.value, "utf-8", null
            )

            wv.ignoreLongTapOnImages()
        }

        LaunchedEffect(wv, viewportCoordinateAlignmentEnabled) {
            wv.evaluateJavascript(
                "window.EditorViewportState.setViewportCoordinateAlignmentEnabled(" +
                    "$viewportCoordinateAlignmentEnabled);",
                null
            )
        }

        ConsumableLaunchedEffect(stripInlineAttachment) { contentId ->
            Timber.d("editor-webview: requested to strip inline image from composer... $contentId")
            wv.evaluateJavascript("stripInlineImage(${JSONObject.quote(contentId)});") {
                Timber.d("editor-webview: stripped inline image with cid $contentId from webview")
            }
        }

        ConsumableLaunchedEffect(refreshBody) { refreshedBody ->
            Timber.d("editor-webview: requested to refresh the draft body...")
            wv.loadDataWithBaseURL(
                null,
                refreshedBody.value, MimeType.Html.value, "utf-8", null
            )
        }

        if (contentLoadingFinished.value) {
            ConsumableLaunchedEffect(injectInlineAttachments) { contentIds ->
                contentIds.forEach { contentId ->
                    wv.evaluateJavascript("injectInlineImage(${JSONObject.quote(contentId)});") {
                        Timber.d("editor-webview: injected inline image with cid $contentId into webview")
                    }
                    @Suppress("MagicNumber")
                    delay(100) // Small delay between injections
                }
            }

            ConsumableLaunchedEffect(shouldRequestFocus) {
                Timber.d("editor-webview: webview is requesting focus...")
                focusRequester.requestFocus()
            }
        }
    }

    BoxWithConstraints(modifier) {
        // WebView changes it's layout strategy based on
        // it's layoutParams. We convert from Compose Modifier to
        // layout params here.
        val width = if (constraints.hasFixedWidth) LayoutParams.MATCH_PARENT else LayoutParams.WRAP_CONTENT
        val height = if (constraints.hasFixedHeight) LayoutParams.MATCH_PARENT else LayoutParams.WRAP_CONTENT
        val layoutParams = FrameLayout.LayoutParams(width, height)
        val context = LocalContext.current
        val surfaceColor = ProtonTheme.colors.backgroundNorm.toArgb()

        AndroidView(
            factory = { context ->
                webViewActions.onBuildWebView.invoke(context).apply {
                    this.settings.builtInZoomControls = true
                    this.settings.displayZoomControls = false
                    this.settings.javaScriptEnabled = true
                    this.settings.safeBrowsingEnabled = true
                    this.settings.allowContentAccess = false
                    this.settings.allowFileAccess = false
                    this.settings.loadWithOverviewMode = true
                    this.settings.useWideViewPort = true
                    this.layoutParams = layoutParams
                    this.addJavascriptInterface(javascriptCallback, JAVASCRIPT_CALLBACK_INTERFACE_NAME)
                    this.webViewClient = client
                    this.setBackgroundColor(surfaceColor)

                    isFocusable = true
                    isFocusableInTouchMode = true
                    webView = this
                }
            },
            modifier = Modifier
                .onSizeChanged(
                    onSizeChanged = { size ->
                        webViewHeightPx = size.height
                    }
                )
                .heightIn(max = (WEB_VIEW_FIXED_MAX_HEIGHT - 1).pxToDp())
                .focusRequester(focusRequester)
                .onFocusEvent { event ->
                    if (event.hasFocus) {
                        webView?.let { wv ->
                            Timber.d("editor-webview: focusing html element and showing keyboard...")
                            ComposerFocusUtils.focusEditorAndShowKeyboard(wv, context)
                        }
                    }
                },
            onRelease = {
                Timber.d("editor-webview: webview is leaving composition for good.")
                webView = null
            }
        )
    }
}

private fun WebView.ignoreLongTapOnImages() = this.setOnLongClickListener { view ->
    val imageTypes = listOf(WebView.HitTestResult.IMAGE_TYPE, WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE)
    val result = (view as WebView).hitTestResult
    val longTappedViewIsImage = imageTypes.contains(result.type)

    return@setOnLongClickListener longTappedViewIsImage
}

private fun configureDarkLightMode(webView: WebView, isInDarkTheme: Boolean) {
    if (isInDarkTheme) {
        webView.showInDarkMode()
    } else {
        webView.showInLightMode()
    }
}

object EditableMessageBodyWebView {

    data class Actions(
        val sanitizePastedText: (mimeType: String?, text: String) -> String,
        val loadImage: (contentId: String) -> MessageBodyImage?,
        val onMessageBodyChanged: (body: String) -> Unit,
        val onWebViewParamsChanged: (params: EditorViewDrawingState) -> Unit,
        val onBuildWebView: (Context) -> WebView,
        val onInlineImageRemoved: (String) -> Unit,
        val onInlineImageClicked: (String) -> Unit,
        val onInlineImagePasted: (String) -> Unit
    )

    val contentMimeTypes = arrayOf("image/*")
}

// Max constraint for WebView height. If the height is greater
// than this value, we will not fix the height of the WebView or it will crash.
// (Limit set in androidx.compose.ui.unit.Constraints)
private const val WEB_VIEW_FIXED_MAX_HEIGHT = 262_143
