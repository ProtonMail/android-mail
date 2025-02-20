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
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ch.protonmail.android.mailcommon.presentation.ConsumableLaunchedEffect
import ch.protonmail.android.mailcommon.presentation.NO_CONTENT_DESCRIPTION
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import ch.protonmail.android.mailcommon.presentation.compose.pxToDp
import ch.protonmail.android.mailcommon.presentation.extension.copyTextToClipboard
import ch.protonmail.android.mailcommon.presentation.extension.openShareIntentForUri
import ch.protonmail.android.mailcommon.presentation.ui.MailDivider
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.MimeType
import ch.protonmail.android.mailmessage.domain.usecase.GetEmbeddedImageResult
import ch.protonmail.android.mailmessage.presentation.R
import ch.protonmail.android.mailmessage.presentation.extension.getSecuredWebResourceResponse
import ch.protonmail.android.mailmessage.presentation.extension.isEmbeddedImage
import ch.protonmail.android.mailmessage.presentation.extension.isRemoteContent
import ch.protonmail.android.mailmessage.presentation.extension.isRemoteUnsecuredContent
import ch.protonmail.android.mailmessage.presentation.model.MessageBodyExpandCollapseMode
import ch.protonmail.android.mailmessage.presentation.model.MessageBodyUiModel
import ch.protonmail.android.mailmessage.presentation.model.ViewModePreference
import ch.protonmail.android.mailmessage.presentation.model.webview.MessageBodyWebViewOperation
import ch.protonmail.android.mailmessage.presentation.viewmodel.MessageBodyWebViewViewModel
import com.google.accompanist.web.AccompanistWebViewClient
import com.google.accompanist.web.LoadingState
import com.google.accompanist.web.WebView
import com.google.accompanist.web.rememberWebViewStateWithHTMLData
import kotlinx.coroutines.delay
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultUnspecified

@Composable
fun MessageBodyWebView(
    modifier: Modifier = Modifier,
    messageBodyUiModel: MessageBodyUiModel,
    bodyDisplayMode: MessageBodyExpandCollapseMode,
    webViewActions: MessageBodyWebView.Actions,
    shouldAllowViewingEntireMessage: Boolean = true,
    onMessageBodyLoaded: (messageId: MessageId, height: Int) -> Unit = { _, _ -> },
    viewModel: MessageBodyWebViewViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    val state = rememberWebViewStateWithHTMLData(
        data = if (bodyDisplayMode == MessageBodyExpandCollapseMode.Collapsed) {
            messageBodyUiModel.messageBodyWithoutQuote
        } else {
            messageBodyUiModel.messageBody
        },
        mimeType = MimeType.Html.value
    )

    val webViewInteractionState = viewModel.state.collectAsState().value
    val longClickDialogState = remember { mutableStateOf(false) }

    val actions = webViewActions.copy(
        onMessageBodyLinkLongClicked = {
            viewModel.submit(MessageBodyWebViewOperation.MessageBodyWebViewAction.LongClickLink(it))
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

    val messageId = messageBodyUiModel.messageId

    val isSystemInDarkTheme = isSystemInDarkTheme()

    val shouldShowViewEntireMessageButton = remember { mutableStateOf(false) }

    var webView by remember { mutableStateOf<WebView?>(null) }
    LaunchedEffect(key1 = messageBodyUiModel.viewModePreference) {
        webView?.let {
            configureDarkLightMode(it, isSystemInDarkTheme, messageBodyUiModel.viewModePreference)
        }
    }

    ConsumableLaunchedEffect(messageBodyUiModel.printEffect) {
        webView?.let {
            actions.onPrint(messageId)
        }
    }

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
                } else if (request?.isRemoteUnsecuredContent() == true) {
                    request.getSecuredWebResourceResponse()
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

    var webViewHeightPx by remember { mutableStateOf(0) }

    LaunchedEffect(key1 = state.loadingState, key2 = webViewHeightPx) {
        if (state.loadingState == LoadingState.Finished && webViewHeightPx > 0) {
            // The purpose of this delay is to prevent multiple calls to onMessageBodyLoaded. WebView height
            // may continue to change after the content is loaded, so we wait a bit to make sure the height is stable.
            // If this block is called again, current coroutine will be cancelled automatically
            delay(WEB_PAGE_CONTENT_LOAD_TIMEOUT)

            onMessageBodyLoaded(messageId, webViewHeightPx)
        }
    }

    Column(modifier = modifier) {
        key(client) {
            val webViewMaxHeight = if (messageBodyUiModel.shouldRestrictWebViewHeight) {
                WEB_VIEW_FIXED_MAX_HEIGHT_RESTRICTED
            } else {
                WEB_VIEW_FIXED_MAX_HEIGHT
            }

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
                    configureDarkLightMode(it, isSystemInDarkTheme, messageBodyUiModel.viewModePreference)
                    configureLongClick(it, actions.onMessageBodyLinkLongClicked)
                    webView = it
                },
                captureBackPresses = false,
                state = state,
                modifier = Modifier
                    .testTag(MessageBodyWebViewTestTags.WebView)
                    .fillMaxWidth()
                    .heightIn(max = (webViewMaxHeight - 1).pxToDp())
                    .onSizeChanged { size ->
                        webViewHeightPx = size.height
                        if (size.height >= webViewMaxHeight - 1 && shouldAllowViewingEntireMessage) {
                            shouldShowViewEntireMessageButton.value = true
                        }
                    },
                client = client
            )
        }

        if (bodyDisplayMode != MessageBodyExpandCollapseMode.NotApplicable) {
            ExpandCollapseBodyButton(
                modifier = Modifier.offset(x = ProtonDimens.SmallSpacing),
                onClick = { actions.onExpandCollapseButtonCLicked() }
            )
        }

        if (shouldShowViewEntireMessageButton.value && messageBodyUiModel.shouldRestrictWebViewHeight) {
            ViewEntireMessageButton(
                onClick = {
                    actions.onViewEntireMessageClicked(
                        messageId,
                        messageBodyUiModel.shouldShowEmbeddedImages,
                        messageBodyUiModel.shouldShowRemoteContent,
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

    ConsumableLaunchedEffect(webViewInteractionState.longClickLinkEffect) {
        longClickDialogState.value = true
    }
}

private fun configureLongClick(view: WebView, onLongClick: (uri: Uri) -> Unit) {
    view.setOnLongClickListener {
        val result = (it as WebView).hitTestResult
        val type = result.type

        if (listOf(WebView.HitTestResult.EMAIL_TYPE, WebView.HitTestResult.SRC_ANCHOR_TYPE).contains(type)) {
            val uri = runCatching { Uri.parse(result.extra) }.getOrNull() ?: return@setOnLongClickListener false
            onLongClick(uri)
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
    if (isInDarkTheme) {
        configureDarkLightModeWhenInDarkTheme(webView, viewModePreference)
    } else {
        webView.showInLightMode()
    }
}

private fun configureDarkLightModeWhenInDarkTheme(webView: WebView, viewModePreference: ViewModePreference) {
    if (viewModePreference == ViewModePreference.LightMode) {
        webView.showInLightMode()
    } else {
        webView.showInDarkMode()
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

@Composable
private fun ViewEntireMessageButton(onClick: () -> Unit) {
    MailDivider()
    Text(
        modifier = Modifier
            .padding(ProtonDimens.DefaultSpacing)
            .clickable(
                enabled = true,
                onClickLabel = null,
                role = Role.Button,
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ),
        text = stringResource(id = R.string.button_view_entire_message),
        style = ProtonTheme.typography.defaultUnspecified,
        color = ProtonTheme.colors.textAccent
    )
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
        val onExpandCollapseButtonCLicked: () -> Unit,
        val loadEmbeddedImage: (messageId: MessageId, contentId: String) -> GetEmbeddedImageResult?,
        val onPrint: (MessageId) -> Unit,
        val onViewEntireMessageClicked: (MessageId, Boolean, Boolean, ViewModePreference) -> Unit,
        val onReply: (MessageId) -> Unit,
        val onReplyAll: (MessageId) -> Unit,
        val onForward: (MessageId) -> Unit
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
// (Limit as seen in the error messages from the crashes)
private const val WEB_VIEW_FIXED_MAX_HEIGHT_RESTRICTED = 4096
