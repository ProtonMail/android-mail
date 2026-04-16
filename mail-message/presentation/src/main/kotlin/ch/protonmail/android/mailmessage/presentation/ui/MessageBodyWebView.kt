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
import android.content.Context
import android.net.Uri
import android.view.MotionEvent
import android.view.ViewGroup.LayoutParams
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.mailcommon.presentation.ConsumableLaunchedEffect
import ch.protonmail.android.mailcommon.presentation.NO_CONTENT_DESCRIPTION
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import ch.protonmail.android.mailcommon.presentation.extension.copyTextToClipboard
import ch.protonmail.android.mailcommon.presentation.extension.openShareIntentForUri
import ch.protonmail.android.mailcommon.presentation.ui.MailDivider
import ch.protonmail.android.mailmessage.domain.model.MessageBodyImage
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.MimeType
import ch.protonmail.android.mailmessage.presentation.R
import ch.protonmail.android.mailmessage.presentation.extension.isEmbeddedImage
import ch.protonmail.android.mailmessage.presentation.extension.isRemoteContent
import ch.protonmail.android.mailmessage.presentation.model.MessageBodyUiModel
import ch.protonmail.android.mailmessage.presentation.model.ViewModePreference
import ch.protonmail.android.mailmessage.presentation.model.webview.MessageBodyWebViewOperation
import ch.protonmail.android.mailmessage.presentation.viewmodel.MessageBodyWebViewViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import timber.log.Timber

@OptIn(FlowPreview::class)
@Composable
fun MessageBodyWebView(
    modifier: Modifier = Modifier,
    messageBodyUiModel: MessageBodyUiModel,
    webViewActions: MessageBodyWebView.Actions,
    onBuildWebView: (Context) -> ZoomableWebView,
    shouldAllowViewingEntireMessage: Boolean = true,
    onMessageBodyLoaded: (messageId: MessageId, height: Int) -> Unit = { _, _ -> },
    viewModel: MessageBodyWebViewViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    val webViewInteractionState = viewModel.state.collectAsStateWithLifecycle().value
    val longClickDialogState = remember { mutableStateOf(false) }
    val messageId = messageBodyUiModel.messageId
    val longClickImageDialogState = remember { mutableStateOf(false) }
    val shouldShowViewEntireMessageButton = remember { mutableStateOf(false) }

    // During loading phase, WebView size can change multiple times. It may both
    // increase and decrease in size. We will track measured heights and loading state to decide
    // on the final height when it's loaded.
    var lastMeasuredWebViewHeight by remember(messageId) { mutableIntStateOf(0) }

    val actions = webViewActions.copy(
        onMessageBodyLinkLongClicked = {
            viewModel.submit(MessageBodyWebViewOperation.MessageBodyWebViewAction.LongClickLink(it))
        },
        onMessageBodyImageLongClicked = {
            viewModel.submit(MessageBodyWebViewOperation.MessageBodyWebViewAction.LongClickImage(it))
        }
    )

    val longClickDialogActions = MessageWebViewLongPressDialog.Actions(
        onCopyClicked = { uri ->
            context.copyTextToClipboard(
                label = context.getString(R.string.message_link_long_click_copy_description),
                text = uri.toString()
            )
            longClickDialogState.value = false
        },
        onShareClicked = { uri ->
            context.openShareIntentForUri(uri, context.getString(R.string.message_link_long_click_share_via))
            longClickDialogState.value = false
        },
        onDismissed = { longClickDialogState.value = false }
    )


    val isSystemInDarkTheme = isSystemInDarkTheme()

    val contentLoaded = remember(messageId) { mutableStateOf(false) }

    val client = remember(messageId) {
        object : WebViewClient() {

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                request?.let {
                    actions.onMessageBodyLinkClicked(it.url)
                }
                return true
            }

            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                return if (request?.isRemoteContent() == true || request?.isEmbeddedImage() == true) {
                    request.url?.toString()?.let { url ->
                        actions.loadImage(messageId, url)?.let {
                            WebResourceResponse(it.mimeType, "", ByteArrayInputStream(it.data))
                        }
                    } ?: super.shouldInterceptRequest(view, request)
                } else {
                    super.shouldInterceptRequest(view, request)
                }
            }

            // Kept only for logging/debugging.
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Timber.d("message-webview: onPageFinished")
            }

            // Using onPageCommitVisible improves perceived loading time since
            // onPageFinished fires much later (after most resources load), delaying
            // when the WebView can be shown.
            override fun onPageCommitVisible(view: WebView?, url: String?) {
                super.onPageCommitVisible(view, url)
                Timber.d("message-webview: onPageCommitVisible")
                contentLoaded.value = true
            }
        }
    }

    val webView = remember(messageId) { onBuildWebView(context) }

    ConsumableLaunchedEffect(messageBodyUiModel.reloadMessageEffect) {
        webView.reload()
    }

    LaunchedEffect(messageId, messageBodyUiModel.messageBody, messageBodyUiModel.viewModePreference) {
        Timber.d("message-webview: setting initial value on webview ${webView.hashCode()} ($messageId)")
        webView.loadDataWithBaseURL(null, messageBodyUiModel.messageBody, MimeType.Html.value, "utf-8", null)
    }

    LaunchedEffect(messageId) {
        combine(
            snapshotFlow { lastMeasuredWebViewHeight }
                // allow measuring passes and webview to settle
                .debounce(timeoutMillis = WEB_PAGE_CONTENT_LOAD_TIMEOUT),
            // also listen for changes in content loaded, there can be multiple calls to this
            snapshotFlow { contentLoaded.value }
                .filter { it }
        ) { measuredHeight, isLoaded ->
            // in order to get the settled height after the webpage has loaded
            // For empty messages, we can get 0 height
            measuredHeight
        }.collectLatest { height ->
            onMessageBodyLoaded(messageId, height)
        }
    }

    DisposableEffect(messageId) {
        onDispose {
            webViewCleanup(webView, messageId)
        }
    }

    Column(modifier) {
        val webViewMaxHeight = if (messageBodyUiModel.shouldRestrictWebViewHeight) {
            WEB_VIEW_FIXED_MAX_HEIGHT_RESTRICTED
        } else {
            WEB_VIEW_FIXED_MAX_HEIGHT
        }

        Box {
            AndroidView(
                factory = { context ->
                    FrameLayout(context).apply {
                        addView(
                            webView,
                            FrameLayout.LayoutParams(
                                LayoutParams.MATCH_PARENT,
                                LayoutParams.WRAP_CONTENT
                            )
                        )
                    }
                },
                update = { container ->
                    val webview = container.getChildAt(0) as? ZoomableWebView ?: return@AndroidView

                    webview.settings.builtInZoomControls = true
                    webview.settings.displayZoomControls = false
                    webview.settings.javaScriptEnabled = false
                    webview.settings.safeBrowsingEnabled = true
                    webview.settings.allowContentAccess = false
                    webview.settings.allowFileAccess = false
                    webview.settings.loadWithOverviewMode = true
                    webview.settings.useWideViewPort = true
                    webview.isVerticalScrollBarEnabled = false
                    webview.webViewClient = client

                    // Set max height restriction directly on the WebView
                    webview.maxHeight = webViewMaxHeight

                    configureDarkLightMode(webview, isSystemInDarkTheme, messageBodyUiModel.viewModePreference)
                    configureLongClick(
                        webview,
                        actions.onMessageBodyLinkLongClicked,
                        actions.onMessageBodyImageLongClicked
                    )
                    configureOnTouchListener(webview)
                },
                modifier = Modifier
                    .testTag(MessageBodyWebViewTestTags.WebView)
                    .fillMaxWidth()
                    .onSizeChanged {
                        lastMeasuredWebViewHeight = it.height
                        shouldShowViewEntireMessageButton.value = shouldAllowViewingEntireMessage &&
                            it.height >= webViewMaxHeight - 1
                    }
            )

            if (shouldShowViewEntireMessageButton.value && messageBodyUiModel.shouldRestrictWebViewHeight) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    ProtonTheme.colors.backgroundNorm
                                )
                            )
                        )
                )
            }
        }


        if (messageBodyUiModel.shouldShowExpandCollapseButton) {
            ExpandCollapseBodyButton(
                modifier = Modifier.offset(x = ProtonDimens.Spacing.Standard),
                onClick = { actions.onExpandCollapseButtonCLicked() }
            )
        }

        if (shouldShowViewEntireMessageButton.value && messageBodyUiModel.shouldRestrictWebViewHeight) {
            ViewEntireMessageButton(
                onClick = {
                    actions.onViewEntireMessageClicked(
                        messageId,
                        messageBodyUiModel.shouldShowEmbeddedImagesBanner.not(),
                        messageBodyUiModel.shouldShowRemoteContentBanner.not(),
                        messageBodyUiModel.viewModePreference
                    )
                }
            )
        }

    }

    if (longClickDialogState.value && webViewInteractionState.lastFocusedUri != null) {
        MessageWebViewLongPressDialog(
            actions = longClickDialogActions,
            linkUri = webViewInteractionState.lastFocusedUri
        )
    }

    if (longClickImageDialogState.value && webViewInteractionState.lastFocusedUri != null) {
        MessageWebViewImageLongPressDialog(
            imageUri = webViewInteractionState.lastFocusedUri,
            onDownloadClicked = { uri ->
                val imageUrl = uri.toString()
                actions.onDownloadImage(messageId, imageUrl)
                longClickImageDialogState.value = false
            },
            onDismissed = { longClickImageDialogState.value = false }
        )
    }

    ConsumableLaunchedEffect(webViewInteractionState.longClickLinkEffect) {
        longClickDialogState.value = true
    }

    ConsumableLaunchedEffect(webViewInteractionState.longClickImageEffect) {
        longClickImageDialogState.value = true
    }
}

private fun configureLongClick(
    view: WebView,
    onLinkLongClick: (uri: Uri) -> Unit,
    onImageLongClick: (uri: Uri) -> Unit
) {
    view.setOnLongClickListener {
        val result = (it as WebView).hitTestResult
        val type = result.type

        if (listOf(WebView.HitTestResult.EMAIL_TYPE, WebView.HitTestResult.SRC_ANCHOR_TYPE).contains(type)) {
            val uri = runCatching { Uri.parse(result.extra) }.getOrNull() ?: return@setOnLongClickListener false
            onLinkLongClick(uri)
            return@setOnLongClickListener true
        }

        if (listOf(WebView.HitTestResult.IMAGE_TYPE, WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE).contains(type)) {
            val uri = runCatching { Uri.parse(result.extra) }.getOrNull() ?: return@setOnLongClickListener false
            onImageLongClick(uri)
            return@setOnLongClickListener true
        }

        false
    }
}

private fun configureDarkLightMode(
    webView: WebView,
    isInDarkTheme: Boolean,
    viewModePreference: ViewModePreference
) {
    if (isInDarkTheme && viewModePreference == ViewModePreference.LightMode) {
        webView.showInLightMode()
    }
    // In other cases, media query in Html content will be used to render correctly
}

private fun configureOnTouchListener(webView: ZoomableWebView) {
    webView.setOnTouchListener { view, motionEvent ->
        if (motionEvent.pointerCount > 1) {
            // Multitouch (zoom), disallow parent intercepting
            view.parent.requestDisallowInterceptTouchEvent(true)
        } else if (
            motionEvent.action == MotionEvent.ACTION_UP ||
            motionEvent.action == MotionEvent.ACTION_CANCEL
        ) {
            // Re-allow intercept after zoom ends
            view.parent.requestDisallowInterceptTouchEvent(false)
        }

        // This is needed for accessibility
        if (motionEvent.action == MotionEvent.ACTION_UP) {
            view.performClick()
        }

        false // Let the WebView handle the event
    }
}

private fun webViewCleanup(webView: WebView, messageId: MessageId) {
    runCatching {
        webView.stopLoading()
        webView.loadUrl("about:blank")
        webView.destroy()
    }.onFailure { e ->
        Timber.e(e, "Failed to clean up webView: $messageId")
    }
}

@Composable
private fun ViewEntireMessageButton(onClick: () -> Unit) {
    MailDivider()
    Text(
        modifier = Modifier
            .padding(ProtonDimens.Spacing.Large)
            .clickable(
                role = Role.Button,
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ),
        text = stringResource(id = R.string.button_view_entire_message),
        style = ProtonTheme.typography.bodyLarge,
        color = ProtonTheme.colors.textAccent
    )

}

@Composable
private fun ExpandCollapseBodyButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        modifier = modifier
            .padding(ProtonDimens.Spacing.Small)
            .height(MessageBodyDimens.ExpandButtonHeight)
            .width(MessageBodyDimens.ExpandButtonWidth),
        contentPadding = PaddingValues(0.dp),
        shape = RoundedCornerShape(MessageBodyDimens.ExpandButtonRoundedCornerPercent),
        border = BorderStroke(MailDimens.DefaultBorder, ProtonTheme.colors.shade20),
        colors = ButtonDefaults.buttonColors().copy(containerColor = ProtonTheme.colors.backgroundNorm),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
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
        val onMessageBodyLinkLongClicked: (uri: Uri) -> Unit,
        val onMessageBodyImageLongClicked: (uri: Uri) -> Unit,
        val onExpandCollapseButtonCLicked: () -> Unit,
        val loadImage: (messageId: MessageId, url: String) -> MessageBodyImage?,
        val onPrint: (MessageId) -> Unit,
        val onDownloadImage: (messageId: MessageId, imageUrl: String) -> Unit,
        val onViewEntireMessageClicked: (MessageId, Boolean, Boolean, ViewModePreference) -> Unit
    )
}

object MessageBodyWebViewTestTags {

    const val WebView = "MessageBodyWebView"
}

private const val WEB_PAGE_CONTENT_LOAD_TIMEOUT = 250L

// Max constraint for WebView height. If the height is greater
// than this value, we will not fix the height of the WebView or it will crash.
// (Limit set in androidx.compose.ui.unit.Constraints)
private const val WEB_VIEW_FIXED_MAX_HEIGHT = 262_143
private const val WEB_VIEW_FIXED_MAX_HEIGHT_RESTRICTED = 16_383
