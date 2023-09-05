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

import java.io.ByteArrayInputStream
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings.FORCE_DARK_OFF
import android.webkit.WebSettings.FORCE_DARK_ON
import android.webkit.WebView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ch.protonmail.android.mailcommon.presentation.NO_CONTENT_DESCRIPTION
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import ch.protonmail.android.mailcommon.presentation.compose.pxToDp
import ch.protonmail.android.mailcommon.presentation.system.LocalDeviceCapabilitiesProvider
import ch.protonmail.android.maildetail.domain.usecase.GetEmbeddedImageResult
import ch.protonmail.android.maildetail.presentation.R
import ch.protonmail.android.maildetail.presentation.extensions.isEmbeddedImage
import ch.protonmail.android.maildetail.presentation.extensions.isRemoteContent
import ch.protonmail.android.maildetail.presentation.model.MessageBodyState
import ch.protonmail.android.maildetail.presentation.model.MessageBodyUiModel
import ch.protonmail.android.mailmessage.domain.model.AttachmentId
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.presentation.ui.AttachmentFooter
import com.google.accompanist.web.AccompanistWebChromeClient
import com.google.accompanist.web.AccompanistWebViewClient
import com.google.accompanist.web.WebView
import com.google.accompanist.web.rememberWebViewStateWithHTMLData
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.proton.core.compose.component.ProtonSolidButton
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultSmallNorm
import me.proton.core.compose.theme.defaultSmallWeak

@Composable
@Suppress("LongParameterList", "LongMethod")
fun MessageBody(
    modifier: Modifier = Modifier,
    messageBodyUiModel: MessageBodyUiModel,
    actions: MessageBody.Actions,
    webViewHeight: Int? = null,
    onMessageBodyLoaded: (messageId: MessageId, height: Int) -> Unit = { _, _ -> },
    showReplyActionsFeatureFlag: Boolean
) {
    val hasWebView = LocalDeviceCapabilitiesProvider.current.hasWebView

    when {
        messageBodyUiModel.shouldShowEmbeddedImagesBanner && messageBodyUiModel.shouldShowRemoteContentBanner -> {
            MessageBodyBanner(text = stringResource(id = R.string.message_body_embedded_and_remote_content_banner_text))
        }
        messageBodyUiModel.shouldShowEmbeddedImagesBanner -> {
            MessageBodyBanner(text = stringResource(id = R.string.message_body_embedded_images_banner_text))
        }
        messageBodyUiModel.shouldShowRemoteContentBanner -> {
            MessageBodyBanner(text = stringResource(id = R.string.message_body_remote_content_banner_text))
        }
    }

    if (hasWebView) {
        MessageBodyWebView(
            modifier = modifier,
            messageBodyUiModel = messageBodyUiModel,
            actions = actions,
            webViewHeight = webViewHeight,
            onMessageBodyLoaded = onMessageBodyLoaded
        )
    } else {
        MessageBodyNoWebView(
            modifier = modifier
        )
    }

    if (showReplyActionsFeatureFlag) {
        MessageActionButtons(messageId = messageBodyUiModel.messageId, callbacks = actions)
    }
}

@Composable
private fun MessageActionButtons(
    modifier: Modifier = Modifier,
    messageId: MessageId,
    callbacks: MessageBody.Actions
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(ProtonDimens.SmallSpacing)
            .padding(top = ProtonDimens.DefaultSpacing),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        MessageActionButton(
            modifier = Modifier.weight(1f, false),
            onClick = { callbacks.onReply(messageId) },
            iconResource = R.drawable.ic_proton_reply,
            textResource = R.string.action_reply
        )
        MessageActionButton(
            modifier = Modifier.weight(1f, false),
            onClick = { callbacks.onReplyAll(messageId) },
            iconResource = R.drawable.ic_proton_reply_all,
            textResource = R.string.action_reply_all
        )
        MessageActionButton(
            modifier = Modifier.weight(1f, false),
            onClick = { callbacks.onForward(messageId) },
            iconResource = R.drawable.ic_proton_forward,
            textResource = R.string.action_forward
        )
    }
}

@Composable
private fun MessageActionButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    @DrawableRes iconResource: Int,
    @StringRes textResource: Int
) {
    Button(
        modifier = modifier,
        shape = RoundedCornerShape(MailDimens.ActionButtonShapeRadius),
        border = BorderStroke(.5.dp, ProtonTheme.colors.shade20),
        colors = ButtonDefaults.buttonColors(backgroundColor = ProtonTheme.colors.backgroundNorm),
        elevation = ButtonDefaults.elevation(defaultElevation = 0.dp),
        onClick = { onClick() }
    ) {
        Icon(
            modifier = Modifier.padding(end = ProtonDimens.ExtraSmallSpacing),
            painter = painterResource(id = iconResource),
            tint = ProtonTheme.colors.iconNorm,
            contentDescription = null
        )
        Text(
            text = stringResource(textResource),
            style = ProtonTheme.typography.defaultSmallNorm
        )
    }
}

@Composable
internal fun MessageBodyWebView(
    modifier: Modifier = Modifier,
    messageBodyUiModel: MessageBodyUiModel,
    actions: MessageBody.Actions,
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
                if (SDK_INT >= VERSION_CODES.TIRAMISU) {
                    it.settings.isAlgorithmicDarkeningAllowed = true
                } else if (SDK_INT >= VERSION_CODES.Q) {
                    it.settings.forceDark = if (isSystemInDarkTheme) FORCE_DARK_ON else FORCE_DARK_OFF
                }
            },
            captureBackPresses = false,
            state = state,
            modifier = with(
                Modifier
                    .testTag(MessageBodyTestTags.WebView)
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
        if (messageBodyUiModel.attachments != null && messageBodyUiModel.attachments.attachments.isNotEmpty()) {
            AttachmentFooter(
                modifier = Modifier.background(color = ProtonTheme.colors.backgroundNorm),
                messageBodyAttachmentsUiModel = messageBodyUiModel.attachments,
                actions = AttachmentFooter.Actions(
                    onShowAllAttachments = actions.onShowAllAttachments,
                    onAttachmentClicked = actions.onAttachmentClicked
                )
            )
        }
    }
}

@Composable
internal fun MessageBodyNoWebView(modifier: Modifier = Modifier) {
    Text(
        modifier = modifier
            .testTag(MessageBodyTestTags.WebViewAlternative)
            .padding(ProtonDimens.MediumSpacing),
        text = stringResource(id = R.string.message_body_error_no_webview)
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

@Composable
private fun MessageBodyBanner(modifier: Modifier = Modifier, text: String) {
    Row(
        modifier = modifier
            .testTag(MessageBodyTestTags.MessageBodyBanner)
            .padding(ProtonDimens.DefaultSpacing)
            .background(color = ProtonTheme.colors.backgroundSecondary, shape = ProtonTheme.shapes.medium)
            .padding(ProtonDimens.DefaultSpacing)
    ) {
        Icon(
            modifier = Modifier.testTag(MessageBodyTestTags.MessageBodyBannerIcon),
            painter = painterResource(id = R.drawable.ic_proton_image),
            contentDescription = NO_CONTENT_DESCRIPTION,
            tint = ProtonTheme.colors.iconWeak
        )
        Spacer(modifier = Modifier.width(ProtonDimens.SmallSpacing))
        Text(
            modifier = Modifier.testTag(MessageBodyTestTags.MessageBodyBannerText),
            text = text,
            style = ProtonTheme.typography.defaultSmallWeak
        )
    }
}

object MessageBody {

    data class Actions(
        val onMessageBodyLinkClicked: (uri: Uri) -> Unit,
        val onShowAllAttachments: () -> Unit,
        val onAttachmentClicked: (attachmentId: AttachmentId) -> Unit,
        val loadEmbeddedImage: (messageId: MessageId, contentId: String) -> GetEmbeddedImageResult?,
        val onReply: (MessageId) -> Unit,
        val onReplyAll: (MessageId) -> Unit,
        val onForward: (MessageId) -> Unit
    )
}

object MessageBodyTestTags {

    const val WebView = "MessageBodyWebView"
    const val WebViewAlternative = "MessageBodyWithoutWebView"
    const val MessageBodyBanner = "MessageBodyBanner"
    const val MessageBodyBannerIcon = "MessageBodyBannerIcon"
    const val MessageBodyBannerText = "MessageBodyBannerText"
}

private const val WEB_PAGE_CONTENT_LOAD_TIMEOUT = 500L

// Max constraint for WebView height. If the height is greater
// than this value, we will not fix the height of the WebView or it will crash.
// (Limit set in androidx.compose.ui.unit.Constraints)
private const val WEB_VIEW_FIXED_MAX_HEIGHT = 262_143
