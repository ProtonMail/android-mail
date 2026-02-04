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

package ch.protonmail.android.maildetail.presentation.ui.header

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import ch.protonmail.android.design.compose.component.ProtonOutlinedIconButton
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.design.compose.theme.bodyMediumNorm
import ch.protonmail.android.design.compose.theme.bodySmallNorm
import ch.protonmail.android.design.compose.theme.titleMediumNorm
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens.MessageDetailsHeader.DetailsTitleWidth
import ch.protonmail.android.mailcommon.presentation.compose.OfficialBadge
import ch.protonmail.android.mailcommon.presentation.compose.SmallNonClickableIcon
import ch.protonmail.android.mailcommon.presentation.model.AvatarUiModel
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.model.string
import ch.protonmail.android.maildetail.presentation.R
import ch.protonmail.android.maildetail.presentation.model.MessageDetailHeaderUiModel
import ch.protonmail.android.maildetail.presentation.model.MessageIdUiModel
import ch.protonmail.android.maildetail.presentation.model.ParticipantUiModel
import ch.protonmail.android.maildetail.presentation.previewdata.MessageDetailHeaderPreview
import ch.protonmail.android.maildetail.presentation.previewdata.MessageDetailHeaderPreviewProvider
import ch.protonmail.android.maildetail.presentation.ui.ConversationDetailItemTestTags
import ch.protonmail.android.maildetail.presentation.ui.common.SingleLineRecipientNames
import ch.protonmail.android.maillabel.presentation.model.LabelUiModel
import ch.protonmail.android.maillabel.presentation.ui.LabelsList
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.MessageThemeOptions
import ch.protonmail.android.mailmessage.presentation.ui.ParticipantAvatar
import ch.protonmail.android.mailpadlocks.presentation.EncryptionInfoSection
import ch.protonmail.android.mailpadlocks.presentation.model.EncryptionInfoUiModel
import ch.protonmail.android.mailtrackingprotection.presentation.model.BlockedElementsUiModel
import ch.protonmail.android.mailtrackingprotection.presentation.ui.BlockedTrackingElements
import ch.protonmail.android.uicomponents.thenIf
import kotlinx.collections.immutable.ImmutableList

@Composable
fun MessageDetailHeader(
    modifier: Modifier = Modifier,
    uiModel: MessageDetailHeaderUiModel,
    initiallyExpanded: Boolean = false,
    headerActions: MessageDetailHeader.Actions
) {
    val isExpanded = rememberSaveable(inputs = arrayOf()) {
        mutableStateOf(initiallyExpanded)
    }

    val actions = headerActions.copy(
        onClick = { isExpanded.value = !isExpanded.value }
    )

    AnimatedContent(
        targetState = isExpanded.value,
        transitionSpec = {
            fadeIn() togetherWith
                fadeOut() using
                SizeTransform()
        }
    ) { targetState ->

        Box {
            Box(
                modifier = Modifier
                    .testTag(ConversationDetailItemTestTags.CollapseAnchor)
                    .clickable { headerActions.onCollapseMessage(uiModel.messageIdUiModel) }
                    .fillMaxWidth()
                    .height(MailDimens.ConversationMessageCollapseBarHeight)
            )

            MessageDetailHeaderLayout(
                modifier = modifier,
                uiModel = uiModel,
                isExpanded = targetState,
                actions = actions
            )
        }
    }
}

@Composable
private fun MessageDetailHeaderLayout(
    modifier: Modifier = Modifier,
    uiModel: MessageDetailHeaderUiModel,
    isExpanded: Boolean,
    actions: MessageDetailHeader.Actions
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = ProtonDimens.Spacing.Large,
                vertical = ProtonDimens.Spacing.ModeratelyLarger
            )
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { actions.onCollapseMessage(uiModel.messageIdUiModel) }
                )
        ) {
            ParticipantAvatar(
                avatarUiModel = uiModel.avatar,
                avatarImageUiModel = uiModel.avatarImage,
                actions = ParticipantAvatar.Actions(
                    onAvatarClicked = {
                        actions.onAvatarClicked(
                            uiModel.sender,
                            uiModel.avatar, uiModel.messageIdUiModel
                        )
                    },
                    onAvatarImageLoadRequested = { actions.onAvatarImageLoadRequested(it) },
                    onAvatarImageLoadFailed = { }
                )
            )
            Spacer(modifier = Modifier.width(ProtonDimens.Spacing.Large))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                SenderNameRow(
                    modifier = Modifier
                        .fillMaxWidth(),
                    participantUiModel = uiModel.sender, icons = {
                        Icons(uiModel = uiModel)
                        Spacer(modifier.width(ProtonDimens.Spacing.Compact))
                        Time(time = uiModel.time)
                    }
                )
                Row {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        ParticipantAddress(
                            participantUiModel = uiModel.sender,
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1
                        )
                        Spacer(modifier.height(ProtonDimens.Spacing.Small))
                        AllRecipients(
                            allRecipients = uiModel.allRecipients,
                            hasUndisclosedRecipients = uiModel.shouldShowUndisclosedRecipients,
                            onClick = actions.onClick,
                            isExpanded = isExpanded
                        )
                        if (uiModel.labels.isNotEmpty()) {
                            Spacer(modifier.height(ProtonDimens.Spacing.Compact))
                            Labels(modifier = Modifier, uiModels = uiModel.labels)
                        }
                    }
                    Spacer(modifier = modifier.size(ProtonDimens.Spacing.Large))
                    MessageDetailHeaderActions(
                        modifier = modifier
                            .padding(top = ProtonDimens.Spacing.Standard)
                            .testTag(MessageDetailHeaderTestTags.ActionsRootItem),
                        uiModel = uiModel,
                        actions = actions
                    )
                }
            }
        }
        if (isExpanded) {
            Spacer(modifier = Modifier.height(ProtonDimens.Spacing.Large))
            MessageDetailHeaderCard(
                uiModel = uiModel,
                actions = actions
            )
        }
        Spacer(modifier = Modifier.height(ProtonDimens.Spacing.Standard))
    }
}

@Composable
private fun SenderNameRow(
    modifier: Modifier = Modifier,
    participantUiModel: ParticipantUiModel,
    style: TextStyle = ProtonTheme.typography.titleMediumNorm,
    icons: @Composable () -> Unit
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.testTag(MessageDetailHeaderTestTags.SenderName),
                text = participantUiModel.participantName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = style
            )
            if (participantUiModel.shouldShowOfficialBadge) {
                OfficialBadge()
            }
        }
        icons()
    }
}

@Composable
private fun ParticipantAddress(
    modifier: Modifier = Modifier,
    participantUiModel: ParticipantUiModel,
    textStyle: TextStyle = ProtonTheme.typography.bodyMediumNorm,
    textColor: Color = ProtonTheme.colors.textNorm,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Visible
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            text = participantUiModel.participantAddress,
            modifier = modifier.testTag(MessageDetailHeaderTestTags.ParticipantValue),
            maxLines = maxLines,
            color = if (participantUiModel.shouldShowAddressInRed) ProtonTheme.colors.notificationError else textColor,
            style = textStyle,
            overflow = overflow
        )
    }
}

@Composable
private fun Icons(modifier: Modifier = Modifier, uiModel: MessageDetailHeaderUiModel) {
    Row(modifier = modifier) {
        if (uiModel.shouldShowAttachmentIcon) {
            SmallNonClickableIcon(iconId = R.drawable.ic_proton_paper_clip, iconColor = ProtonTheme.colors.iconNorm)
        }
        if (uiModel.shouldShowStar) {
            SmallNonClickableIcon(
                iconId = R.drawable.ic_proton_star_filled,
                iconColor = ProtonTheme.colors.starSelected
            )
        }
    }
}

@Composable
private fun Time(modifier: Modifier = Modifier, time: TextUiModel) {
    Text(
        modifier = modifier.testTag(MessageDetailHeaderTestTags.Time),
        text = time.string(),
        maxLines = 1,
        style = ProtonTheme.typography.bodySmallNorm
    )
}

@Composable
private fun AllRecipients(
    modifier: Modifier = Modifier,
    allRecipients: ImmutableList<ParticipantUiModel>,
    hasUndisclosedRecipients: Boolean,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier
                .testTag(MessageDetailHeaderTestTags.AllRecipientsText)
                .padding(end = ProtonDimens.Spacing.Small),
            text = stringResource(R.string.to),
            style = ProtonTheme.typography.bodyMediumNorm
        )
        Row(
            modifier = Modifier.weight(1f, fill = false),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SingleLineRecipientNames(
                modifier = Modifier.weight(1f, fill = false),
                textStyle = ProtonTheme.typography.bodyMediumNorm,
                recipients = allRecipients,
                hasUndisclosedRecipients = hasUndisclosedRecipients
            )

            Spacer(modifier = Modifier.width(ProtonDimens.Spacing.Small))

            val expandCollapseIconRes = if (isExpanded) {
                R.drawable.ic_proton_chevron_up_filled
            } else {
                R.drawable.ic_proton_chevron_down_filled
            }

            Icon(
                modifier = Modifier
                    .size(MailDimens.MessageDetailsHeader.CollapseExpandButtonSize),
                painter = painterResource(id = expandCollapseIconRes),
                contentDescription = null,
                tint = ProtonTheme.colors.iconNorm
            )
        }
    }
}

@Composable
private fun ParticipantText(
    modifier: Modifier = Modifier,
    participantUiModel: ParticipantUiModel,
    textColor: Color = ProtonTheme.colors.textNorm,
    shouldBreak: Boolean = false
) {

    val participantMeText = stringResource(id = R.string.recipient_me)
    val nameText =
        if (participantUiModel.isPrimaryUser) {
            participantMeText
        } else {
            participantUiModel.participantName.ifBlank { participantUiModel.participantAddress }
        }

    Text(
        text = nameText,
        modifier = modifier,
        color = textColor,
        style = ProtonTheme.typography.bodySmall,
        maxLines = if (shouldBreak) Int.MAX_VALUE else 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
internal fun MessageDetailHeaderButton(
    modifier: Modifier = Modifier,
    @DrawableRes iconResource: Int,
    @StringRes contentDescriptionResource: Int,
    onClick: () -> Unit
) {
    ProtonOutlinedIconButton(
        modifier = modifier,
        buttonSize = MailDimens.MessageDetailsHeader.ButtonSize,
        shape = ProtonTheme.shapes.mediumLarge,
        border = BorderStroke(MailDimens.DefaultBorder, ProtonTheme.colors.borderNorm),
        onClick = onClick
    ) {
        Icon(
            modifier = Modifier.size(MailDimens.MessageDetailsHeader.ButtonIconSize),
            painter = painterResource(id = iconResource),
            tint = ProtonTheme.colors.iconNorm,
            contentDescription = stringResource(contentDescriptionResource)
        )
    }
}

@Composable
private fun Labels(modifier: Modifier, uiModels: ImmutableList<LabelUiModel>) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {

        LabelsList(
            modifier = Modifier.testTag(MessageDetailHeaderTestTags.LabelsList),
            labels = uiModels
        )
    }
}

@Composable
private fun MessageDetailHeaderCard(
    uiModel: MessageDetailHeaderUiModel,
    actions: MessageDetailHeader.Actions,
    modifier: Modifier = Modifier
) {
    Card(
        shape = ProtonTheme.shapes.extraLarge,
        border = BorderStroke(ProtonDimens.OutlinedBorderSize, ProtonTheme.colors.borderNorm),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(ProtonDimens.Spacing.Medium),
            verticalArrangement = Arrangement.spacedBy(ProtonDimens.Spacing.Large)
        ) {
            SenderDetails(
                uiModel.sender, uiModel.avatar,
                uiModel.messageIdUiModel,
                actions
            )
            RecipientsSection(uiModel, actions)

            EncryptionInfoSection(
                messageId = MessageId(uiModel.messageIdUiModel.id),
                onMoreInfoClick = { actions.onEncryptionInfoClick(it) }
            )

            BlockedTrackingElements(
                MessageId(uiModel.messageIdUiModel.id),
                onBlockedTrackersClick = { actions.onBlockedTrackersClick(it) },
                onNoBlockedTrackersClick = { actions.onBlockedTrackersClick(null) }
            )

            ExtendedHeaderSection(uiModel)
        }
    }
}

@Composable
private fun SenderDetails(
    senderUiModel: ParticipantUiModel,
    avatarUiModel: AvatarUiModel,
    messageIdUiModel: MessageIdUiModel,
    actions: MessageDetailHeader.Actions
) {
    Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
        Text(
            modifier = Modifier
                .width(DetailsTitleWidth)
                .padding(end = ProtonDimens.Spacing.Tiny)
                .thenIf(senderUiModel.shouldShowOfficialBadge) {
                    Modifier.padding(top = ProtonDimens.Spacing.Tiny)
                },
            text = stringResource(R.string.message_details_header_from),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = ProtonTheme.typography.bodySmallNorm
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { actions.onParticipantClicked(senderUiModel, avatarUiModel, messageIdUiModel) }
                )
        ) {
            SenderNameRow(
                participantUiModel = senderUiModel,
                style = ProtonTheme.typography.bodySmallNorm,
                icons = { }
            )
            Spacer(modifier = Modifier.height(ProtonDimens.Spacing.Tiny))
            ParticipantAddress(
                participantUiModel = senderUiModel,
                textColor = ProtonTheme.colors.textAccent,
                textStyle = ProtonTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun RecipientsSection(uiModel: MessageDetailHeaderUiModel, actions: MessageDetailHeader.Actions) {
    Column(verticalArrangement = Arrangement.spacedBy(ProtonDimens.Spacing.Small)) {
        RecipientsTitleAndList(
            title = stringResource(R.string.message_details_header_to),
            recipients = uiModel.toRecipients,
            messageIdUiModel = uiModel.messageIdUiModel,
            hasUndisclosedRecipients = uiModel.shouldShowUndisclosedRecipients,
            actions = actions
        )
        RecipientsTitleAndList(
            title = stringResource(R.string.message_details_header_cc),
            recipients = uiModel.ccRecipients,
            messageIdUiModel = uiModel.messageIdUiModel,
            actions = actions
        )
        RecipientsTitleAndList(
            title = stringResource(R.string.message_details_header_bcc),
            recipients = uiModel.bccRecipients,
            messageIdUiModel = uiModel.messageIdUiModel,
            actions = actions
        )
    }
}

@Composable
private fun RecipientsTitleAndList(
    title: String,
    recipients: ImmutableList<ParticipantUiModel>,
    messageIdUiModel: MessageIdUiModel,
    hasUndisclosedRecipients: Boolean = false,
    actions: MessageDetailHeader.Actions
) {
    if (recipients.isNotEmpty() || hasUndisclosedRecipients) {
        Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
            Text(
                modifier = Modifier
                    .width(DetailsTitleWidth)
                    .padding(bottom = ProtonDimens.Spacing.Tiny)
                    .padding(end = ProtonDimens.Spacing.Tiny),
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = ProtonTheme.typography.bodySmallNorm
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(ProtonDimens.Spacing.Standard)
            ) {
                recipients.forEach { recipient ->
                    Column(
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { actions.onParticipantClicked(recipient, null, messageIdUiModel) }
                        )
                    ) {
                        ParticipantText(
                            participantUiModel = recipient,
                            textColor = ProtonTheme.colors.textNorm
                        )
                        Spacer(modifier = Modifier.height(ProtonDimens.Spacing.Tiny))
                        ParticipantAddress(
                            participantUiModel = recipient,
                            textColor = ProtonTheme.colors.textAccent,
                            textStyle = ProtonTheme.typography.bodySmall
                        )
                    }
                }
                if (hasUndisclosedRecipients) {
                    Text(
                        text = stringResource(R.string.undisclosed_recipients),
                        style = ProtonTheme.typography.bodySmallNorm
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(ProtonDimens.Spacing.Small))
    }
}

@Composable
private fun ExtendedHeaderSection(uiModel: MessageDetailHeaderUiModel) {
    Column(verticalArrangement = Arrangement.spacedBy(ProtonDimens.Spacing.ModeratelyLarge)) {
        ExtendedHeaderRow(
            icon = R.drawable.ic_proton_calendar_today,
            text = uiModel.extendedTime.string()
        )
        ExtendedHeaderRow(
            icon = uiModel.location.icon,
            iconColor = uiModel.location.color,
            text = uiModel.location.name.string()
        )
    }
}

@Composable
private fun ExtendedHeaderRow(
    modifier: Modifier = Modifier,
    text: String,
    @DrawableRes icon: Int,
    iconColor: Color? = null
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.width(DetailsTitleWidth),
            contentAlignment = Alignment.Center
        ) {
            if (iconColor != null) {
                SmallNonClickableIcon(
                    modifier = Modifier.testTag(MessageDetailHeaderTestTags.ExtendedHeaderIcon),
                    iconId = icon,
                    iconColor = iconColor
                )
            } else {
                SmallNonClickableIcon(
                    modifier = Modifier.testTag(MessageDetailHeaderTestTags.ExtendedHeaderIcon),
                    iconId = icon,
                    iconColor = ProtonTheme.colors.iconNorm
                )
            }
        }
        Text(
            modifier = Modifier
                .wrapContentWidth()
                .testTag(MessageDetailHeaderTestTags.ExtendedHeaderText),
            text = text,
            style = ProtonTheme.typography.bodySmallNorm
        )
    }
}

object MessageDetailHeader {
    data class Actions(
        val onClick: () -> Unit,
        val onReply: (MessageId) -> Unit,
        val onReplyAll: (MessageId) -> Unit,
        val onShowFeatureMissingSnackbar: () -> Unit,
        val onMore: (MessageId, MessageThemeOptions) -> Unit,
        val onAvatarClicked: (ParticipantUiModel, AvatarUiModel, MessageIdUiModel) -> Unit,
        val onAvatarImageLoadRequested: (AvatarUiModel) -> Unit,
        val onAvatarImageLoadFailed: () -> Unit,
        val onParticipantClicked: (ParticipantUiModel, AvatarUiModel?, MessageIdUiModel) -> Unit,
        val onCollapseMessage: (MessageIdUiModel) -> Unit,
        val onBlockedTrackersClick: (BlockedElementsUiModel?) -> Unit,
        val onEncryptionInfoClick: (EncryptionInfoUiModel.WithLock) -> Unit
    ) {

        companion object {

            val Empty = Actions(
                onClick = {},
                onReply = {},
                onReplyAll = {},
                onShowFeatureMissingSnackbar = {},
                onMore = { _, _ -> },
                onAvatarClicked = { _, _, _ -> },
                onAvatarImageLoadRequested = { },
                onAvatarImageLoadFailed = { },
                onParticipantClicked = { _, _, _ -> },
                onCollapseMessage = {},
                onBlockedTrackersClick = {},
                onEncryptionInfoClick = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MessageDetailHeaderPreview(
    @PreviewParameter(MessageDetailHeaderPreviewProvider::class) preview: MessageDetailHeaderPreview
) {
    ProtonTheme {
        MessageDetailHeader(
            uiModel = preview.uiModel,
            initiallyExpanded = preview.initiallyExpanded,
            headerActions = MessageDetailHeader.Actions.Empty
        )
    }
}
