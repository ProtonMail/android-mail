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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ch.protonmail.android.design.compose.component.ProtonCenteredProgress
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.mailattachments.domain.model.AttachmentId
import ch.protonmail.android.mailattachments.domain.model.AttachmentOpenMode
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import ch.protonmail.android.mailcommon.presentation.compose.pxToDp
import ch.protonmail.android.mailcommon.presentation.model.AvatarUiModel
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailMessageUiModel
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailMessageUiModel.Collapsed
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailMessageUiModel.Expanded
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailMessageUiModel.Expanding
import ch.protonmail.android.maildetail.presentation.model.MessageIdUiModel
import ch.protonmail.android.maildetail.presentation.model.ParticipantUiModel
import ch.protonmail.android.maildetail.presentation.model.RsvpWidgetUiModel
import ch.protonmail.android.maildetail.presentation.sample.ConversationDetailMessageUiModelSample
import ch.protonmail.android.maildetail.presentation.ui.ConversationDetailItem.previewActions
import ch.protonmail.android.maildetail.presentation.ui.footer.MessageDetailFooter
import ch.protonmail.android.maildetail.presentation.ui.header.MessageDetailHeader
import ch.protonmail.android.maildetail.presentation.ui.rsvpwidget.RsvpWidget
import ch.protonmail.android.maildetail.presentation.ui.rsvpwidget.RsvpWidgetError
import ch.protonmail.android.maildetail.presentation.ui.rsvpwidget.RsvpWidgetLoading
import ch.protonmail.android.mailmessage.domain.model.MessageBodyImage
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.MessageThemeOptions
import ch.protonmail.android.mailmessage.domain.model.RsvpAnswer
import ch.protonmail.android.mailmessage.presentation.model.ViewModePreference
import ch.protonmail.android.mailmessage.presentation.ui.ParticipantAvatar
import ch.protonmail.android.mailpadlocks.presentation.model.EncryptionInfoUiModel
import ch.protonmail.android.mailtrackingprotection.presentation.model.BlockedElementsUiModel

@Composable
@Suppress("LongParameterList")
fun ConversationDetailItem(
    uiModel: ConversationDetailMessageUiModel,
    actions: ConversationDetailItem.Actions,
    modifier: Modifier = Modifier,
    downloadingAttachmentId: AttachmentId? = null,
    // the last message in the conversation has nothing stacked below it, so its bottom corners are rounded too;
    // every other card keeps a square bottom so it connects seamlessly to the card overlapping it below
    isLastMessage: Boolean = true,
    onMessageBodyLoadFinished: (messageId: MessageId, height: Int) -> Unit,
    // we won't bother waiting for the heights to be calculated as we already know, this can happen when you scroll
    // back to an expanded item. We don't want to re-animate the card into view and we don't need to wait for load
    previouslyLoadedHeight: Int? = null,
    // we need to know when the parent view has finished resizing and scrolling as it calculates and adjusts item
    // heights. We should only reveal the expanded card once we have finished loading the parent view and
    // have calculated all the heights
    finishedResizing: Boolean
) {
    val avatarActions = ParticipantAvatar.Actions.Empty.copy(
        onAvatarImageLoadRequested = actions.onAvatarImageLoadRequested
    )
    val cardShape = if (isLastMessage) ProtonTheme.shapes.large else ProtonTheme.shapes.conversation
    // The bottom padding exists so the next card can overlap into it via the -32.dp stacking arrangement. The last
    // card has nothing stacked below it, so it only needs the header Row's own vertical padding - otherwise it shows
    // a large empty gap at the bottom.
    val collapsedHeaderBottomPadding = if (isLastMessage) 0.dp else MailDimens.ConversationCollapseHeaderOverlapHeight

    when (uiModel) {
        is Collapsed -> {
            ConversationDetailCard(shape = cardShape, modifier = modifier) {
                ConversationDetailCollapsedMessageHeader(
                    uiModel = uiModel,
                    avatarActions = avatarActions,
                    modifier = Modifier
                        .padding(bottom = collapsedHeaderBottomPadding)
                        .clickable {
                            when (uiModel.isDraft) {
                                true -> actions.onOpenComposer(uiModel.messageId)
                                else -> actions.onExpand(uiModel.messageId)
                            }
                        }
                )
            }
        }

        is Expanding -> {
            ConversationDetailCard(shape = cardShape, modifier = modifier) {
                ConversationDetailExpandingItem(
                    uiModel = uiModel,
                    avatarActions = avatarActions
                )
            }
        }

        is Expanded -> {
            ConversationDetailCard(shape = cardShape, modifier = modifier) {
                ConversationDetailExpandedItem(
                    uiModel = uiModel,
                    actions = actions,
                    downloadingAttachmentId = downloadingAttachmentId,
                    onMessageBodyLoadFinished = onMessageBodyLoadFinished,
                    cachedWebContentHeight = previouslyLoadedHeight,
                    finishedResizing = finishedResizing
                )
            }
        }
    }
}

@Composable
private fun ConversationDetailCard(
    shape: Shape,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = MailDimens.DefaultBorder,
                color = ProtonTheme.colors.borderNorm,
                shape = shape
            )
            // The .shadow() modifier both draws the shadow and clips the card to `shape` (clip defaults to true for a
            // non-zero elevation). Clipping here - rather than via the ElevatedCard `shape` param - is deliberate:
            // a non-uniform shape (rounded top, square bottom) on the Card itself breaks touch handling on long
            // messages (Reply/Reply all become unresponsive, google issue 429387112 / ET-4775). A modifier-level clip
            // leaves the seam corners square AND transparent, so stacked cards keep a continuous vertical border
            // (this replaces the old opaque-background Box hack that painted over the previous card's border).
            .shadow(
                elevation = if (isSystemInDarkTheme()) {
                    ProtonDimens.ShadowElevation.Raised
                } else {
                    ProtonDimens.ShadowElevation.Lifted
                },
                shape = shape,
                ambientColor = ProtonTheme.colors.shadowSoft,
                spotColor = ProtonTheme.colors.shadowSoft
            ),
        shape = RectangleShape,
        colors = CardDefaults.elevatedCardColors(
            containerColor = ProtonTheme.colors.backgroundNorm
        ),
        // Keep the card's own elevation at 0: the shadow is already drawn by the .shadow() modifier above, and a
        // non-zero elevation makes Material3 apply a tonal-elevation tint to the container that makes the fill diverge
        // from backgroundNorm (visible as a corner "break" in dark mode).
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 0.dp
        ),
        content = content
    )
}

@Composable
private fun ConversationDetailExpandingItem(
    uiModel: Expanding,
    avatarActions: ParticipantAvatar.Actions,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
    ) {
        ConversationDetailCollapsedMessageHeader(
            uiModel = uiModel.collapsed,
            avatarActions = avatarActions
        )
        ProtonCenteredProgress(modifier = Modifier.padding(ProtonDimens.Spacing.Massive))
    }
}

@Composable
private fun ColumnScope.ConversationDetailExpandedItem(
    uiModel: Expanded,
    actions: ConversationDetailItem.Actions,
    downloadingAttachmentId: AttachmentId? = null,
    onMessageBodyLoadFinished: (messageId: MessageId, height: Int) -> Unit,
    // we've already seen this card expanded and so we don't want to re-animate the card into view
    cachedWebContentHeight: Int? = null,
    // we need to know when the parent view has finished resizing and scrolling as it calculates and adjusts item
    // heights. We should only reveal the expanded card once we have finished loading the parent view and
    // have calculated all the heights
    finishedResizing: Boolean
) {
    val id = uiModel.messageId
    // we are likely scrolling back to the view in the list
    val viewPreviouslyLoaded = remember(id) { cachedWebContentHeight != null }
    val isExpanding = rememberSaveable(id) { mutableStateOf(!viewPreviouslyLoaded) }
    val isWebViewLoading = remember(id) { mutableStateOf(true) }
    val columnHeight = rememberSaveable(id) { mutableIntStateOf(0) }

    // play reveal animation on first load, do not play if we are expanding content or if we are scrolling back to
    // the content.
    // (We know that we are scrolling back to previously loaded content if we have a cachedHeight )
    val playRevealAnimations = cachedWebContentHeight == null && !isExpanding.value && finishedResizing
    // hide the footer whilst we are expanding the content so that the footer does not overlay the expanding content
    val showFooter = !isExpanding.value && finishedResizing
    val showLoadingSpinner = !finishedResizing

    val headerActions = MessageDetailHeader.Actions.Empty.copy(
        onReply = actions.onReply,
        onReplyAll = actions.onReplyAll,
        onMore = actions.onMoreMessageActionsClick,
        onAvatarClicked = actions.onAvatarClicked,
        onAvatarImageLoadRequested = actions.onAvatarImageLoadRequested,
        onParticipantClicked = { participantUiModel, avatarUiModel, messageIdUiModel ->
            actions.onParticipantClicked(participantUiModel, avatarUiModel, messageIdUiModel)
        },
        onShowFeatureMissingSnackbar = actions.showFeatureMissingSnackbar,
        onCollapseMessage = actions.onCollapse,
        onBlockedTrackersClick = actions.onBlockedTrackersClick,
        onEncryptionInfoClick = actions.onEncryptionInfoClick
    )

    MessageDetailHeader(
        uiModel = uiModel.messageDetailHeaderUiModel,
        headerActions = headerActions
    )
    Box {
        // Although we have a loading (expanding) card and this is the expanded state, we need to show a loader here
        // whilst the webview is loading content and resizing
        //
        // it's important to use AnimatedVisibility instead of alpha here because AnimatedVisibility actually removes
        // the view after it's invisible and so won't be read out by screen-readers. Also the in-build look-ahead means
        // that the column will be shown at the same time (without a bounce to 0px whilst the card briefly has no
        // content to show)
        this@ConversationDetailExpandedItem.AnimatedVisibility(
            visible = showLoadingSpinner,
            exit = fadeOut(tween()), content = {
                ProtonCenteredProgress(
                    modifier = Modifier
                        .padding(ProtonDimens.Spacing.Massive)
                )
            }
        )

        val itemState by remember(
            isWebViewLoading.value,
            viewPreviouslyLoaded,
            cachedWebContentHeight,
            isExpanding.value,
            showLoadingSpinner
        ) {
            derivedStateOf {
                when {
                    isWebViewLoading.value && viewPreviouslyLoaded ->
                        ItemState.ReLoading(cachedHeight = cachedWebContentHeight ?: 0)

                    isExpanding.value ->
                        ItemState.Expanding

                    showLoadingSpinner ->
                        ItemState.Loading

                    else ->
                        ItemState.Visible
                }
            }
        }

        Column(
            // only reveal the content of this card once the webview content has loaded and resizing has finished
            modifier = Modifier
                .reveal(
                    id = id.id,
                    itemState = itemState,
                    snap = viewPreviouslyLoaded
                )
                .onSizeChanged {
                    columnHeight.intValue = it.height
                }
        ) {
            when (uiModel.messageRsvpWidgetUiModel) {
                is RsvpWidgetUiModel.Hidden -> Unit
                is RsvpWidgetUiModel.Loading -> RsvpWidgetLoading()
                is RsvpWidgetUiModel.Error -> RsvpWidgetError(
                    onRetry = { actions.onRetryRsvpEventLoading(uiModel.messageId) }
                )

                is RsvpWidgetUiModel.Shown -> RsvpWidget(
                    uiModel = uiModel.messageRsvpWidgetUiModel.event,
                    actions = RsvpWidget.Actions(
                        onOpenInProtonCalendar = { actions.onOpenInProtonCalendar(uiModel.messageId) },
                        onAnswerRsvpEvent = { actions.onAnswerRsvpEvent(uiModel.messageId, it) },
                        onMessage = actions.onMessage
                    )
                )
            }

            MessageBanners(
                messageBannersUiModel = uiModel.messageBannersUiModel,
                onMarkMessageAsLegitimate = { isPhishing ->
                    actions.onMarkMessageAsLegitimate(uiModel.messageId, isPhishing)
                },
                onUnblockSender = {
                    actions.onUnblockSender(
                        uiModel.messageId,
                        uiModel.messageDetailHeaderUiModel.sender.participantAddress
                    )
                },
                onCancelScheduleMessage = { actions.onEditScheduleSendMessage(uiModel.messageId) },
                onUnsnoozeMessage = { actions.onUnsnoozeMessage() },
                onUnsubscribeFromNewsletter = { actions.onUnsubscribeFromNewsletter(uiModel.messageId) }
            )
            MessageBody(
                messageBodyUiModel = uiModel.messageBodyUiModel,
                downloadingAttachmentId = downloadingAttachmentId,
                actions = MessageBody.Actions(
                    onMessageBodyLinkClicked = { actions.onMessageBodyLinkClicked(uiModel.messageId, it) },
                    onShowAllAttachments = { actions.onShowAllAttachmentsForMessage(uiModel.messageId) },
                    onAttachmentClicked = { openMode, attachmentId ->
                        actions.onAttachmentClicked(openMode, uiModel.messageId, attachmentId)
                    },
                    onToggleAttachmentsExpandCollapseMode = {
                        actions.onToggleAttachmentsExpandCollapseMode(uiModel.messageId)
                    },
                    onExpandCollapseButtonClicked = {
                        actions.onBodyExpandCollapseButtonClicked(uiModel.messageId)
                        isExpanding.value = true
                    },
                    loadImage = actions.loadImage,
                    onReply = actions.onReply,
                    onReplyAll = actions.onReplyAll,
                    onForward = actions.onForward,
                    onLoadRemoteContent = { actions.onLoadRemoteContent(it) },
                    onLoadEmbeddedImages = { actions.onLoadEmbeddedImages(it) },
                    onLoadRemoteAndEmbeddedContent = { actions.onLoadRemoteAndEmbeddedContent(it) },
                    onPrint = { actions.onPrint(it) },
                    onDownloadImage = { messageId, imageUrl -> actions.onDownloadImage(messageId, imageUrl) },
                    onLoadImagesAfterImageProxyFailure = actions.onLoadImagesAfterImageProxyFailure,
                    onViewEntireMessageClicked = actions.onViewEntireMessageClicked
                ),
                onMessageBodyLoaded = { id: MessageId, i: Int ->
                    // now that the webview is loaded send the more recent height so it can be cached
                    onMessageBodyLoadFinished(id, columnHeight.intValue)
                    isExpanding.value = false
                    isWebViewLoading.value = false
                }
            )
        }
    }
    // Weight - to bring buttons to the bottom of the page
    Spacer(modifier = Modifier.weight(1f))
    MessageDetailFooter(
        modifier = Modifier
            .show(
                isVisible = showFooter,
                // important, we should not animate if we are recreating this view whilst scrolling
                shouldAnimate = playRevealAnimations
            ),
        uiModel = uiModel.messageDetailFooterUiModel,
        actions = MessageDetailFooter.Actions.fromConversationDetailItemActions(actions)
    )
}

@Preview(showBackground = true)
@Composable
fun ConversationDetailItemCollapsedPreview() {
    ConversationDetailItem(
        ConversationDetailMessageUiModelSample.ExpiringInvitation,
        actions = previewActions,
        modifier = Modifier,
        onMessageBodyLoadFinished = { id: MessageId, i: Int -> },
        finishedResizing = true
    )
}

@Preview(showBackground = true)
@Composable
fun ConversationDetailItemExpandedPreview() {
    ConversationDetailItem(
        ConversationDetailMessageUiModelSample.AugWeatherForecastExpanded,
        actions = previewActions,
        modifier = Modifier,
        onMessageBodyLoadFinished = { id: MessageId, i: Int -> },
        finishedResizing = true
    )
}

object ConversationDetailItem {
    data class Actions(
        val onCollapse: (MessageIdUiModel) -> Unit,
        val onExpand: (MessageIdUiModel) -> Unit,
        val onOpenComposer: (MessageIdUiModel) -> Unit,
        val onMessageBodyLinkClicked: (messageId: MessageIdUiModel, url: Uri) -> Unit,
        val onOpenMessageBodyLink: (url: Uri) -> Unit,
        val onShowAllAttachmentsForMessage: (MessageIdUiModel) -> Unit,
        val onAttachmentClicked: (AttachmentOpenMode, MessageIdUiModel, AttachmentId) -> Unit,
        val onToggleAttachmentsExpandCollapseMode: (MessageIdUiModel) -> Unit,
        val showFeatureMissingSnackbar: () -> Unit,
        val loadImage: (messageId: MessageId?, url: String) -> MessageBodyImage?,
        val onReply: (MessageId) -> Unit,
        val onReplyAll: (MessageId) -> Unit,
        val onForward: (MessageId) -> Unit,
        val onScrollRequestCompleted: (MessageIdUiModel) -> Unit,
        val onBodyExpandCollapseButtonClicked: (MessageIdUiModel) -> Unit,
        val onMoreMessageActionsClick: (MessageId, MessageThemeOptions) -> Unit,
        val onLoadRemoteContent: (MessageId) -> Unit,
        val onLoadEmbeddedImages: (MessageId) -> Unit,
        val onLoadRemoteAndEmbeddedContent: (MessageId) -> Unit,
        val onOpenInProtonCalendar: (MessageIdUiModel) -> Unit,
        val onPrint: (MessageId) -> Unit,
        val onAvatarClicked: (ParticipantUiModel, AvatarUiModel, MessageIdUiModel?) -> Unit,
        val onAvatarImageLoadRequested: (AvatarUiModel) -> Unit,
        val onParticipantClicked: (ParticipantUiModel, AvatarUiModel?, MessageIdUiModel?) -> Unit,
        val onMarkMessageAsLegitimate: (MessageIdUiModel, Boolean) -> Unit,
        val onUnblockSender: (MessageIdUiModel, String) -> Unit,
        val onEditScheduleSendMessage: (MessageIdUiModel) -> Unit,
        val onRetryRsvpEventLoading: (MessageIdUiModel) -> Unit,
        val onAnswerRsvpEvent: (MessageIdUiModel, RsvpAnswer) -> Unit,
        val onMessage: (String) -> Unit,
        val onUnsnoozeMessage: () -> Unit,
        val onUnsubscribeFromNewsletter: (MessageIdUiModel) -> Unit,
        val onDownloadImage: (MessageId, String) -> Unit,
        val onLoadImagesAfterImageProxyFailure: (MessageId) -> Unit,
        val onViewEntireMessageClicked: (MessageId, Boolean, Boolean, ViewModePreference) -> Unit,
        val onBlockedTrackersClick: (BlockedElementsUiModel?) -> Unit,
        val onEncryptionInfoClick: (EncryptionInfoUiModel.WithLock) -> Unit
    )

    val previewActions = Actions(
        {},
        {},
        {},
        { model: MessageIdUiModel, uri: Uri -> },
        {},
        {},
        { openMode: AttachmentOpenMode, model: MessageIdUiModel, attachmentId: AttachmentId -> },
        {},
        {},
        { id1: MessageId?, string1: String -> null },
        {},
        {},
        {},
        {},
        {},
        { id: MessageId, options: MessageThemeOptions -> },
        {},
        {},
        {},
        {},
        {},
        { model: ParticipantUiModel, model1: AvatarUiModel, message: MessageIdUiModel? -> },
        {},
        { participant: ParticipantUiModel, avatar: AvatarUiModel?, message: MessageIdUiModel? -> },
        { model: MessageIdUiModel, bool: Boolean -> },
        { model: MessageIdUiModel, string: String -> },
        { model: MessageIdUiModel -> },
        {},
        { _, _ -> },
        {},
        onUnsnoozeMessage = { },
        onUnsubscribeFromNewsletter = {},
        onDownloadImage = { _, _ -> },
        onLoadImagesAfterImageProxyFailure = {},
        onViewEntireMessageClicked = { _, _, _, _ -> },
        onBlockedTrackersClick = {},
        onEncryptionInfoClick = {}
    )
}

@Composable
fun Modifier.show(isVisible: Boolean, shouldAnimate: Boolean): Modifier {
    val targetState = if (isVisible) 1f else 0f
    val animatedAlpha by animateFloatAsState(
        targetValue = targetState,
        label = "alpha"
    )
    return this.graphicsLayer {
        alpha = if (shouldAnimate) animatedAlpha else targetState
    }
}

@Composable
fun Modifier.reveal(
    id: String,
    itemState: ItemState,
    snap: Boolean
): Modifier {
    var lastHeightPx by rememberSaveable(id) { mutableIntStateOf(0) }

    val holdHeightPx: Int? = when (itemState) {
        ItemState.Loading -> 0

        is ItemState.ReLoading -> itemState.cachedHeight

        ItemState.Expanding -> lastHeightPx

        ItemState.Visible -> null
    }

    // Cache height when actually visible
    val cacheHeightModifier = if (itemState == ItemState.Visible) {
        Modifier.onSizeChanged { size ->
            if (size.height > 0) lastHeightPx = size.height
        }
    } else {
        Modifier
    }

    val animationModifier = if (snap) Modifier else Modifier.animateContentSize()

    val heightModifier = when (holdHeightPx) {
        null -> Modifier
        else -> Modifier.requiredHeight(holdHeightPx.pxToDp())
    }

    return this
        .clipToBounds()
        .then(animationModifier)
        .then(heightModifier)
        .then(cacheHeightModifier)
}

sealed class ItemState {
    object Loading : ItemState()
    object Visible : ItemState()
    object Expanding : ItemState()
    data class ReLoading(val cachedHeight: Int = 0) : ItemState()
}

object ConversationDetailItemTestTags {

    const val CollapseAnchor = "ConversationDetailItemCollapseAnchor"
}
