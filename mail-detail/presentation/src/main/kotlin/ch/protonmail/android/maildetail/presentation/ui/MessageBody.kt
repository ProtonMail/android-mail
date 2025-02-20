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
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ch.protonmail.android.mailcommon.presentation.ConsumableLaunchedEffect
import ch.protonmail.android.mailcommon.presentation.NO_CONTENT_DESCRIPTION
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.system.LocalDeviceCapabilitiesProvider
import ch.protonmail.android.mailcommon.presentation.ui.MailDivider
import ch.protonmail.android.maildetail.presentation.R
import ch.protonmail.android.maildetail.presentation.model.MessageBodyState
import ch.protonmail.android.mailmessage.domain.model.AttachmentId
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.usecase.GetEmbeddedImageResult
import ch.protonmail.android.mailmessage.presentation.model.MessageBodyExpandCollapseMode
import ch.protonmail.android.mailmessage.presentation.model.MessageBodyUiModel
import ch.protonmail.android.mailmessage.presentation.model.ViewModePreference
import ch.protonmail.android.mailmessage.presentation.ui.AttachmentFooter
import ch.protonmail.android.mailmessage.presentation.ui.MessageBodyWebView
import me.proton.core.compose.component.ProtonButton
import me.proton.core.compose.component.ProtonSolidButton
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultSmallWeak

@Composable
@Suppress("LongParameterList", "LongMethod")
fun MessageBody(
    modifier: Modifier = Modifier,
    messageBodyUiModel: MessageBodyUiModel,
    actions: MessageBody.Actions,
    onMessageBodyLoaded: (messageId: MessageId, height: Int) -> Unit = { _, _ -> },
    expandCollapseMode: MessageBodyExpandCollapseMode
) {
    val hasWebView = LocalDeviceCapabilitiesProvider.current.hasWebView

    when {
        messageBodyUiModel.shouldShowEmbeddedImagesBanner && messageBodyUiModel.shouldShowRemoteContentBanner -> {
            MessageBodyButtonBanner(
                bannerText = R.string.message_body_embedded_and_remote_content_banner_text,
                buttonText = R.string.message_body_load_embedded_and_remote_content_button_text,
                onButtonClicked = { actions.onLoadRemoteAndEmbeddedContent(messageBodyUiModel.messageId) }
            )
        }

        messageBodyUiModel.shouldShowEmbeddedImagesBanner -> {
            MessageBodyButtonBanner(
                bannerText = R.string.message_body_embedded_images_banner_text,
                buttonText = R.string.message_body_load_embedded_images_button_text,
                onButtonClicked = { actions.onLoadEmbeddedImages(messageBodyUiModel.messageId) }
            )
        }

        messageBodyUiModel.shouldShowRemoteContentBanner -> {
            MessageBodyButtonBanner(
                bannerText = R.string.message_body_remote_content_banner_text,
                buttonText = R.string.message_body_load_remote_content_button_text,
                onButtonClicked = { actions.onLoadRemoteContent(messageBodyUiModel.messageId) }
            )
        }
    }

    if (messageBodyUiModel.shouldShowOpenInProtonCalendar) {
        OpenInProtonCalendarBanner(
            modifier = Modifier.testTag(MessageBodyTestTags.MessageBodyBannerProtonCalendar),
            onOpenInProtonCalendarClick = { actions.onOpenInProtonCalendar(messageBodyUiModel.messageId) }
        )
    }

    MailDivider(modifier = Modifier.padding(top = ProtonDimens.SmallSpacing))

    ConsumableLaunchedEffect(messageBodyUiModel.replyEffect) {
        actions.onEffectConsumed(messageBodyUiModel.messageId, MessageBody.DoOnDisplayedEffect.Reply)
    }

    ConsumableLaunchedEffect(messageBodyUiModel.replyAllEffect) {
        actions.onEffectConsumed(messageBodyUiModel.messageId, MessageBody.DoOnDisplayedEffect.ReplyAll)
    }

    ConsumableLaunchedEffect(messageBodyUiModel.forwardEffect) {
        actions.onEffectConsumed(messageBodyUiModel.messageId, MessageBody.DoOnDisplayedEffect.Forward)
    }

    if (hasWebView) {
        MessageBodyWebView(
            modifier = modifier,
            messageBodyUiModel = messageBodyUiModel,
            bodyDisplayMode = expandCollapseMode,
            webViewActions = MessageBodyWebView.Actions(
                onMessageBodyLinkClicked = actions.onMessageBodyLinkClicked,
                onMessageBodyLinkLongClicked = {}, // Deferred init to MessageBodyWebView.
                onExpandCollapseButtonCLicked = actions.onExpandCollapseButtonClicked,
                loadEmbeddedImage = actions.loadEmbeddedImage,
                onPrint = actions.onPrint,
                onViewEntireMessageClicked = actions.onViewEntireMessageClicked,
                onReply = actions.onReply,
                onReplyAll = actions.onReplyAll,
                onForward = actions.onForward
            ),
            onMessageBodyLoaded = onMessageBodyLoaded
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
    } else {
        MessageBodyNoWebView(
            modifier = modifier
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
fun MessageBodyButtonBanner(
    @StringRes bannerText: Int,
    @StringRes buttonText: Int,
    onButtonClicked: () -> Unit
) {
    MessageBanner(
        icon = R.drawable.ic_proton_image,
        iconTint = ProtonTheme.colors.iconWeak,
        text = TextUiModel.TextRes(bannerText),
        textStyle = ProtonTheme.typography.defaultSmallWeak,
        backgroundColor = ProtonTheme.colors.backgroundSecondary
    ) {
        ProtonButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = { onButtonClicked() },
            colors = ButtonDefaults.buttonColors(backgroundColor = ProtonTheme.colors.backgroundSecondary),
            elevation = ButtonDefaults.elevation(defaultElevation = 0.dp),
            shape = ProtonTheme.shapes.small,
            border = BorderStroke(Dp.Hairline, ProtonTheme.colors.textWeak)
        ) {
            Text(
                text = stringResource(id = buttonText),
                style = ProtonTheme.typography.defaultSmallWeak
            )
        }
    }
}

object MessageBody {

    sealed interface DoOnDisplayedEffect {
        data object Reply : DoOnDisplayedEffect
        data object ReplyAll : DoOnDisplayedEffect
        data object Forward : DoOnDisplayedEffect
    }

    data class Actions(
        val onExpandCollapseButtonClicked: () -> Unit,
        val onMessageBodyLinkClicked: (uri: Uri) -> Unit,
        val onShowAllAttachments: () -> Unit,
        val onAttachmentClicked: (attachmentId: AttachmentId) -> Unit,
        val loadEmbeddedImage: (messageId: MessageId, contentId: String) -> GetEmbeddedImageResult?,
        val onReply: (MessageId) -> Unit,
        val onReplyAll: (MessageId) -> Unit,
        val onForward: (MessageId) -> Unit,
        val onEffectConsumed: (MessageId, DoOnDisplayedEffect) -> Unit,
        val onLoadRemoteContent: (MessageId) -> Unit,
        val onLoadEmbeddedImages: (MessageId) -> Unit,
        val onLoadRemoteAndEmbeddedContent: (MessageId) -> Unit,
        val onOpenInProtonCalendar: (MessageId) -> Unit,
        val onPrint: (MessageId) -> Unit,
        val onViewEntireMessageClicked: (MessageId, Boolean, Boolean, ViewModePreference) -> Unit
    )
}

object MessageBodyTestTags {

    const val WebViewAlternative = "MessageBodyWithoutWebView"
    const val MessageBodyBanner = "MessageBodyBanner"
    const val MessageBodyBannerIcon = "MessageBodyBannerIcon"
    const val MessageBodyBannerText = "MessageBodyBannerText"
    const val MessageActionsRootItem = "MessageActionsRootItem"
    const val MessageReplyButton = "MessageReplyButton"
    const val MessageReplyAllButton = "MessageReplyAllButton"
    const val MessageForwardButton = "MessageForwardButton"
    const val MessageBodyBannerProtonCalendar = "MessageBodyBannerProtonCalendar"
}
