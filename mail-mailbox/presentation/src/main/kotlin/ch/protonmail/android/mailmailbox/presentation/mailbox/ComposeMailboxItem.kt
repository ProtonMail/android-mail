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

package ch.protonmail.android.mailmailbox.presentation.mailbox

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.design.compose.theme.bodyLargeNorm
import ch.protonmail.android.design.compose.theme.bodySmallNorm
import ch.protonmail.android.mailattachments.presentation.model.AttachmentIdUiModel
import ch.protonmail.android.mailcommon.presentation.compose.SmallClickableIcon
import ch.protonmail.android.mailcommon.presentation.compose.SmallNonClickableIcon
import ch.protonmail.android.mailcommon.presentation.extension.isItemRead
import ch.protonmail.android.mailcommon.presentation.extension.tintColor
import ch.protonmail.android.mailcommon.presentation.model.AvatarImageUiModel
import ch.protonmail.android.mailcommon.presentation.model.AvatarUiModel
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.model.string
import ch.protonmail.android.maillabel.presentation.model.LabelUiModel
import ch.protonmail.android.maillabel.presentation.ui.LabelsList
import ch.protonmail.android.mailmailbox.presentation.R
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.ExpiryInformationUiModel
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxItemLocationUiModel
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxItemUiModel
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.ParticipantsUiModel
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.toSemanticsReadout
import ch.protonmail.android.mailmailbox.presentation.mailbox.previewdata.MailboxItemUiModelPreviewData
import ch.protonmail.android.mailmessage.presentation.ui.ParticipantAvatar
import ch.protonmail.android.mailsnooze.presentation.model.SnoozeStatusUiModel
import ch.protonmail.android.uicomponents.text.MultiWordHighlightedText
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
fun MailboxItem(
    modifier: Modifier = Modifier,
    actions: ComposeMailboxItem.Actions,
    item: MailboxItemUiModel,
    avatarImageUiModel: AvatarImageUiModel,
    downloadingAttachmentId: AttachmentIdUiModel? = null,
    selectionMode: Boolean = false,
    isSelected: Boolean = false,
    isSelectable: Boolean = true,
    accessibilitySwipeActions: ImmutableList<CustomAccessibilityAction>,
    highlightText: String = ""
) {
    Box(
        modifier = modifier
            .semantics {
                customActions = accessibilitySwipeActions
                role = Role.Button
                isItemRead = item.isRead
            }
            .combinedClickable(
                onClick = { actions.onItemClicked(item) },
                onLongClick = {
                    if (isSelectable) {
                        actions.onItemLongClicked(item)
                    }
                }
            )
            .padding(start = ProtonDimens.Spacing.Tiny)
            .background(
                color = ProtonTheme.colors.backgroundNorm,
                shape = ProtonTheme.shapes.extraLarge
            )
            .fillMaxWidth()
            .clip(
                shape = if (isSelected) {
                    RoundedCornerShape(
                        topStart = ProtonDimens.CornerRadius.ExtraLarge,
                        topEnd = 0.dp,
                        bottomStart = ProtonDimens.CornerRadius.ExtraLarge,
                        bottomEnd = 0.dp
                    )
                } else ProtonTheme.shapes.extraLarge
            )
            .background(
                color = if (isSelected) ProtonTheme.colors.interactionBrandWeakNorm
                else ProtonTheme.colors.backgroundNorm
            )

    ) {
        Row(
            modifier = Modifier
                .padding(
                    start = ProtonDimens.Spacing.ModeratelyLarge,
                    end = ProtonDimens.Spacing.Large,
                    top = ProtonDimens.Spacing.Medium,
                    bottom = ProtonDimens.Spacing.Medium
                )
        ) {
            val fontWeight = if (item.isRead) FontWeight.Normal else FontWeight.Bold
            val fontColor = if (item.isRead) ProtonTheme.colors.textWeak else ProtonTheme.colors.textNorm
            val iconColor = if (item.isRead) ProtonTheme.colors.iconWeak else ProtonTheme.colors.iconNorm
            val timeFontColor = if (item.displaySnoozeReminder) ProtonTheme.colors.notificationWarning else fontColor
            val avatarActions = ParticipantAvatar.Actions(
                onAvatarClicked = { actions.onAvatarClicked(item) },
                onAvatarImageLoadRequested = { actions.onAvatarImageLoadRequested(item) },
                onAvatarImageLoadFailed = { actions.onAvatarImageLoadFailed(item) }
            )

            ParticipantAvatar(
                modifier = Modifier.align(Alignment.CenterVertically),
                avatarUiModel = if (selectionMode) {
                    (item.avatar as? AvatarUiModel.ParticipantAvatar)?.copy(selected = isSelected) ?: item.avatar
                } else {
                    item.avatar
                },
                avatarImageUiModel = avatarImageUiModel,
                actions = avatarActions
            )
            Column(
                modifier = Modifier
                    .padding(
                        start = ProtonDimens.Spacing.Large
                    )
            ) {
                val talkbackUnreadReadout = stringResource(R.string.mailbox_talkback_readout_unread)
                val time = item.time.string()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = buildString {
                                if (!item.isRead) {
                                    append("$talkbackUnreadReadout. ")
                                }
                                append(item.participants.toSemanticsReadout())
                                append(" $time")
                            }
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ActionIcons(
                        item = item,
                        iconColor = iconColor,
                        modifier = Modifier.padding(end = ProtonDimens.Spacing.Small)
                    )

                    val participantsFontWeight = if (item.isRead) FontWeight.Normal else FontWeight.Bold
                    Participants(
                        modifier = Modifier
                            .weight(1f),
                        participants = item.participants,
                        fontWeight = participantsFontWeight,
                        fontColor = fontColor,
                        count = item.numMessages,
                        iconColor = iconColor,
                        highlightText = highlightText
                    )

                    if (item.shouldShowAttachmentIcon) {
                        SmallNonClickableIcon(iconId = R.drawable.ic_proton_paper_clip, iconColor = iconColor)
                        Spacer(Modifier.size(ProtonDimens.Spacing.Small))
                    }

                    if (item.shouldShowCalendarIcon) {
                        SmallNonClickableIcon(iconId = R.drawable.ic_proton_calendar_grid, iconColor = iconColor)
                        Spacer(Modifier.size(ProtonDimens.Spacing.Small))
                    }

                    Time(time = item.time, fontWeight = fontWeight, fontColor = timeFontColor)
                }
                Spacer(modifier = Modifier.size(ProtonDimens.Spacing.Tiny))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LocationIcons(
                        iconResIds = item.locations,
                        iconColor = iconColor,
                        modifier = Modifier.padding(end = ProtonDimens.Spacing.Tiny)
                    )
                    Row(
                        Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Subject(
                            subject = item.subject,
                            fontWeight = fontWeight,
                            fontColor = fontColor,
                            highlightText = highlightText,
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .padding(end = ProtonDimens.Spacing.Medium)
                        )
                    }
                    Icons(
                        item = item,
                        isStarClickable = !selectionMode,
                        onStarClicked = actions.onStarClicked
                    )
                }

                if (item.expiryInformation is ExpiryInformationUiModel.HasExpiry) {

                    Row(
                        modifier = Modifier
                            .padding(top = ProtonDimens.Spacing.Small, bottom = ProtonDimens.Spacing.Small)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ExpiryInformation(
                            textUiModel = item.expiryInformation.expiryText,
                            urgent = item.expiryInformation.isLessThanOneHour,
                            fontColor = fontColor
                        )
                    }
                }

                if (item.snoozedUntil is SnoozeStatusUiModel.SnoozeStatus) {

                    Row(
                        modifier = Modifier
                            .padding(top = ProtonDimens.Spacing.Small, bottom = ProtonDimens.Spacing.Small)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ExpiryInformation(
                            textUiModel = item.snoozedUntil.formattedDateText,
                            urgent = item.snoozedUntil.highlight,
                            fontColor = ProtonTheme.colors.notificationWarning,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                if (item.attachments.any { it.includeInPreview }) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = ProtonDimens.Spacing.Standard),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AttachmentList(
                            attachments = item.attachments,
                            textColor = fontColor,
                            onAttachmentClicked = actions.onAttachmentClicked,
                            downloadingAttachmentId = downloadingAttachmentId
                        )
                    }
                }
                if (item.labels.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .padding(top = ProtonDimens.Spacing.Standard)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Labels(labels = item.labels)
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionIcons(
    modifier: Modifier = Modifier,
    item: MailboxItemUiModel,
    iconColor: Color
) {
    val someIconShown = item.shouldShowRepliedIcon || item.shouldShowRepliedAllIcon || item.shouldShowForwardedIcon
    if (!someIconShown) {
        return
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (item.shouldShowRepliedIcon) {
            SmallNonClickableIcon(
                modifier = Modifier.padding(end = ProtonDimens.Spacing.Small),
                iconId = R.drawable.ic_proton_arrow_up_and_left, iconColor = iconColor
            )
        }
        if (item.shouldShowRepliedAllIcon) {
            SmallNonClickableIcon(
                modifier = Modifier.padding(end = ProtonDimens.Spacing.Small),
                iconId = R.drawable.ic_proton_arrows_up_and_left, iconColor = iconColor
            )
        }
        if (item.shouldShowForwardedIcon) {
            SmallNonClickableIcon(
                modifier = Modifier.padding(end = ProtonDimens.Spacing.Small),
                iconId = R.drawable.ic_proton_arrow_right, iconColor = iconColor
            )
        }
    }
}

@Composable
private fun Participants(
    modifier: Modifier = Modifier,
    participants: ParticipantsUiModel,
    count: Int? = null,
    fontWeight: FontWeight,
    fontColor: Color,
    iconColor: Color,
    highlightText: String
) {
    when (participants) {
        is ParticipantsUiModel.Participants -> {
            ParticipantsListWithMessageCount(
                modifier = modifier.wrapContentSize(),
                participants = participants,
                messageCount = count,
                fontWeight = fontWeight,
                fontColor = fontColor,
                iconColor = iconColor,
                highlightText = highlightText
            )
        }

        is ParticipantsUiModel.NoParticipants -> {
            Text(
                modifier = modifier.testTag(ParticipantsListTestTags.NoParticipant),
                text = participants.message.string(),
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                style = ProtonTheme.typography.bodyLargeNorm.copy(fontWeight = fontWeight, color = fontColor)
            )
        }
    }
}

@Composable
private fun Time(
    modifier: Modifier = Modifier,
    time: TextUiModel,
    fontWeight: FontWeight,
    fontColor: Color
) {
    Text(
        modifier = modifier.testTag(MailboxItemTestTags.Date),
        text = time.string(),
        maxLines = 1,
        textAlign = TextAlign.End,
        style = ProtonTheme.typography.bodySmallNorm.copy(fontWeight = fontWeight, color = fontColor)
    )
}

@Composable
private fun LocationIcons(
    modifier: Modifier = Modifier,
    iconResIds: ImmutableList<MailboxItemLocationUiModel>,
    iconColor: Color
) {
    if (iconResIds.isEmpty()) {
        return
    }

    Row(
        modifier = modifier.testTag(MailboxItemTestTags.LocationIcons),
        horizontalArrangement = Arrangement.Start
    ) {
        iconResIds.forEach {
            SmallNonClickableIcon(
                modifier = Modifier
                    .padding(end = ProtonDimens.Spacing.Tiny)
                    .semantics { tintColor = it.color },
                iconId = it.icon,
                iconColor = it.color ?: iconColor
            )
        }
    }
}

@Composable
private fun Subject(
    modifier: Modifier = Modifier,
    subject: String,
    fontWeight: FontWeight,
    fontColor: Color,
    highlightText: String
) {
    if (highlightText.isEmpty()) {
        Text(
            modifier = modifier.testTag(MailboxItemTestTags.Subject),
            text = subject,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            style = ProtonTheme.typography.bodyMedium.copy(fontWeight = fontWeight, color = fontColor)
        )
    } else {
        MultiWordHighlightedText(
            modifier = modifier.testTag(MailboxItemTestTags.Subject),
            text = subject,
            highlight = highlightText,
            maxLines = 1,
            highlightBackgroundColor = ProtonTheme.colors.searchHighlightBackground,
            highlightTextColor = ProtonTheme.colors.searchHighlightText,
            style = ProtonTheme.typography.bodyMedium.copy(fontWeight = fontWeight, color = fontColor),
            overflow = TextOverflow.Ellipsis
        )

    }
}

@Composable
private fun Icons(
    modifier: Modifier = Modifier,
    item: MailboxItemUiModel,
    isStarClickable: Boolean,
    onStarClicked: (MailboxItemUiModel) -> Unit
) {

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.End
    ) {
        StarIcon(
            isStarred = item.isStarred,
            isClickable = isStarClickable,
            onClick = { onStarClicked(item) }
        )
    }
}

@Composable
private fun StarIcon(
    isStarred: Boolean,
    isClickable: Boolean,
    onClick: () -> Unit
) {
    val iconId = if (isStarred) R.drawable.ic_proton_star_filled else R.drawable.ic_proton_star
    val iconColor = if (isStarred) ProtonTheme.colors.starSelected else ProtonTheme.colors.starDefault
    val talkbackStarredReadout = stringResource(R.string.mailbox_talkback_readout_starred)
    val talkbackNotStarredReadout = stringResource(R.string.mailbox_talkback_readout_not_starred)
    if (isClickable) {
        SmallClickableIcon(
            modifier = Modifier.semantics {
                contentDescription = if (isStarred) {
                    talkbackStarredReadout
                } else talkbackNotStarredReadout
            },
            iconId = iconId,
            iconColor = iconColor,
            onClick = onClick,
            iconSize = ProtonDimens.IconSize.Medium
        )
    } else {
        SmallNonClickableIcon(
            iconId = iconId,
            iconColor = iconColor,
            iconSize = ProtonDimens.IconSize.Medium
        )
    }
}

@Composable
private fun ExpiryInformation(
    modifier: Modifier = Modifier,
    textUiModel: TextUiModel,
    urgent: Boolean,
    fontColor: Color,
    fontWeight: FontWeight = FontWeight.Normal
) {

    val color = if (urgent) ProtonTheme.colors.notificationError else fontColor
    Text(
        modifier = modifier.testTag(MailboxItemTestTags.ExpiryInformation),
        text = textUiModel.string(),
        overflow = TextOverflow.Ellipsis,
        maxLines = 1,
        fontWeight = fontWeight,
        style = ProtonTheme.typography.bodyMedium.copy(color = color)
    )
}

@Composable
private fun Labels(modifier: Modifier = Modifier, labels: ImmutableList<LabelUiModel>) {
    LabelsList(
        modifier = modifier.testTag(MailboxItemTestTags.LabelsList),
        labels = labels
    )
}

object ComposeMailboxItem {
    data class Actions(
        val onItemClicked: (MailboxItemUiModel) -> Unit,
        val onItemLongClicked: (MailboxItemUiModel) -> Unit,
        val onAvatarClicked: (MailboxItemUiModel) -> Unit,
        val onAvatarImageLoadRequested: (MailboxItemUiModel) -> Unit,
        val onAvatarImageLoadFailed: (MailboxItemUiModel) -> Unit,
        val onStarClicked: (MailboxItemUiModel) -> Unit,
        val onAttachmentClicked: (AttachmentIdUiModel) -> Unit
    ) {

        companion object {

            val Empty = Actions(
                onAvatarClicked = {},
                onAvatarImageLoadRequested = {},
                onAvatarImageLoadFailed = {},
                onItemLongClicked = {},
                onItemClicked = {},
                onStarClicked = {},
                onAttachmentClicked = {}
            )
        }
    }
}

@Composable
@Preview(showBackground = true)
private fun DroidConMailboxItemPreview() {
    ProtonTheme {
        MailboxItem(
            modifier = Modifier,
            item = MailboxItemUiModelPreviewData.Conversation.DroidConLondon,
            actions = ComposeMailboxItem.Actions.Empty,
            avatarImageUiModel = AvatarImageUiModel.NotLoaded,
            accessibilitySwipeActions = persistentListOf()
        )
    }
}

@Composable
@Preview(showBackground = true)
private fun DroidConWithoutCountMailboxItemPreview() {
    ProtonTheme {
        MailboxItem(
            modifier = Modifier,
            item = MailboxItemUiModelPreviewData.Conversation.DroidConLondonWithZeroMessages,
            actions = ComposeMailboxItem.Actions.Empty,
            avatarImageUiModel = AvatarImageUiModel.NotLoaded,
            accessibilitySwipeActions = persistentListOf()
        )
    }
}

@Composable
@Preview(showBackground = true)
private fun WeatherMailboxItemPreview() {
    ProtonTheme {
        MailboxItem(
            modifier = Modifier,
            item = MailboxItemUiModelPreviewData.Conversation.WeatherForecast,
            actions = ComposeMailboxItem.Actions.Empty,
            avatarImageUiModel = AvatarImageUiModel.NotLoaded,
            accessibilitySwipeActions = persistentListOf()
        )
    }
}

@Composable
@Preview(showBackground = true)
private fun LongRecipientItemPreview() {
    ProtonTheme {
        MailboxItem(
            modifier = Modifier,
            item = MailboxItemUiModelPreviewData.Conversation.MultipleRecipientWithLabel,
            actions = ComposeMailboxItem.Actions.Empty,
            avatarImageUiModel = AvatarImageUiModel.NotLoaded,
            accessibilitySwipeActions = persistentListOf()
        )
    }
}

@Composable
@Preview(showBackground = true)
private fun LongRecipientItemWithExpiryPreview() {
    val itemWithExpiry = MailboxItemUiModelPreviewData.Conversation.MultipleRecipientWithLabel.copy(
        expiryInformation = ExpiryInformationUiModel.HasExpiry(
            TextUiModel.PluralisedText(R.plurals.expires_in_minutes, 10),
            true
        )
    )
    ProtonTheme {
        MailboxItem(
            modifier = Modifier,
            item = itemWithExpiry,
            actions = ComposeMailboxItem.Actions.Empty,
            avatarImageUiModel = AvatarImageUiModel.NotLoaded,
            accessibilitySwipeActions = persistentListOf()
        )
    }
}

@Composable
@Preview(showBackground = true)
private fun SnoozedUntilPreview() {
    val itemWithExpiry = MailboxItemUiModelPreviewData.Conversation.MultipleRecipientWithLabel.copy(
        snoozedUntil = SnoozeStatusUiModel.SnoozeStatus(
            TextUiModel.TextResWithArgs(
                R.string.snooze_sheet_success, listOf("Sat 10th")
            ),
            true
        )
    )
    ProtonTheme {
        MailboxItem(
            modifier = Modifier,
            item = itemWithExpiry,
            actions = ComposeMailboxItem.Actions.Empty,
            avatarImageUiModel = AvatarImageUiModel.NotLoaded,
            accessibilitySwipeActions = persistentListOf()
        )
    }
}

@Composable
@Preview(showBackground = true)
private fun LongSubjectItemPreview() {
    ProtonTheme {
        MailboxItem(
            modifier = Modifier,
            item = MailboxItemUiModelPreviewData.Conversation.LongSubjectWithIcons,
            actions = ComposeMailboxItem.Actions.Empty,
            avatarImageUiModel = AvatarImageUiModel.NotLoaded,
            accessibilitySwipeActions = persistentListOf()
        )
    }
}

@Composable
@Preview(showBackground = true)
private fun LongSubjectWithIconItemPreview() {
    ProtonTheme {
        MailboxItem(
            modifier = Modifier,
            item = MailboxItemUiModelPreviewData.Conversation.LongSubjectWithoutIcons,
            actions = ComposeMailboxItem.Actions.Empty,
            avatarImageUiModel = AvatarImageUiModel.NotLoaded,
            accessibilitySwipeActions = persistentListOf()
        )
    }
}

@Composable
@Preview(showBackground = true)
private fun NoRecipientIconItemPreview() {
    ProtonTheme {
        MailboxItem(
            modifier = Modifier,
            item = MailboxItemUiModelPreviewData.Conversation.NoParticipant,
            actions = ComposeMailboxItem.Actions.Empty,
            avatarImageUiModel = AvatarImageUiModel.NotLoaded,
            accessibilitySwipeActions = persistentListOf()
        )
    }
}

object MailboxItemTestTags {

    const val ItemRow = "MailboxItemRow"
    const val LocationIcons = "LocationIcons"
    const val LabelsList = "LabelsList"
    const val Subject = "Subject"
    const val Date = "Date"
    const val Count = "Count"
    const val ExpiryInformation = "ExpiryInformation"
}
