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
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ch.protonmail.android.mailcommon.presentation.NO_CONTENT_DESCRIPTION
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import ch.protonmail.android.mailcommon.presentation.system.LocalDeviceCapabilitiesProvider
import ch.protonmail.android.maildetail.presentation.R
import ch.protonmail.android.maildetail.presentation.model.MessageBodyState
import ch.protonmail.android.maildetail.presentation.model.MessageIdUiModel
import ch.protonmail.android.mailmessage.domain.model.AttachmentId
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.usecase.GetEmbeddedImageResult
import ch.protonmail.android.mailmessage.presentation.model.MessageBodyUiModel
import ch.protonmail.android.mailmessage.presentation.ui.MessageBodyWebView
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
            actions = MessageBodyWebView.Actions(
                actions.onMessageBodyLinkClicked,
                actions.onShowAllAttachments,
                actions.onAttachmentClicked,
                actions.loadEmbeddedImage
            ),
            webViewHeight = webViewHeight,
            onMessageBodyLoaded = onMessageBodyLoaded
        )
    } else {
        MessageBodyNoWebView(
            modifier = modifier
        )
    }

    if (showReplyActionsFeatureFlag) {
        MessageActionButtons(messageId = MessageIdUiModel(messageBodyUiModel.messageId.id), callbacks = actions)
    }
}

@Composable
private fun MessageActionButtons(
    modifier: Modifier = Modifier,
    messageId: MessageIdUiModel,
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
            onClick = { callbacks.onReply(MessageId(messageId.id)) },
            iconResource = R.drawable.ic_proton_reply,
            textResource = R.string.action_reply
        )
        MessageActionButton(
            modifier = Modifier.weight(1f, false),
            onClick = { callbacks.onReplyAll(MessageId(messageId.id)) },
            iconResource = R.drawable.ic_proton_reply_all,
            textResource = R.string.action_reply_all
        )
        MessageActionButton(
            modifier = Modifier.weight(1f, false),
            onClick = { callbacks.onForward(MessageId(messageId.id)) },
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

    const val WebViewAlternative = "MessageBodyWithoutWebView"
    const val MessageBodyBanner = "MessageBodyBanner"
    const val MessageBodyBannerIcon = "MessageBodyBannerIcon"
    const val MessageBodyBannerText = "MessageBodyBannerText"
}
