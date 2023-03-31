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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import ch.protonmail.android.mailcommon.presentation.NO_CONTENT_DESCRIPTION
import ch.protonmail.android.mailcommon.presentation.compose.Avatar
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import ch.protonmail.android.mailcommon.presentation.compose.SmallNonClickableIcon
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.model.string
import ch.protonmail.android.maillabel.presentation.model.LabelUiModel
import ch.protonmail.android.maillabel.presentation.ui.LabelsList
import ch.protonmail.android.mailmailbox.presentation.R
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxItemUiModel
import ch.protonmail.android.mailmailbox.presentation.mailbox.previewdata.MailboxItemUiModelPreviewData
import kotlinx.collections.immutable.ImmutableList
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.caption
import me.proton.core.compose.theme.default
import me.proton.core.compose.theme.defaultSmall

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MailboxItem(
    modifier: Modifier = Modifier,
    item: MailboxItemUiModel,
    onItemClicked: (MailboxItemUiModel) -> Unit,
    onOpenSelectionMode: () -> Unit
) {
    Row(
        modifier = modifier
            .testTag(MailboxItemTestTags.ItemRow)
            .combinedClickable(onClick = { onItemClicked(item) }, onLongClick = onOpenSelectionMode)
            .padding(
                start = ProtonDimens.DefaultSpacing,
                end = ProtonDimens.DefaultSpacing,
                top = ProtonDimens.ExtraSmallSpacing,
                bottom = ProtonDimens.ExtraSmallSpacing
            )
            .fillMaxWidth()
    ) {
        val fontWeight = if (item.isRead) FontWeight.Normal else FontWeight.Bold
        val fontColor = if (item.isRead) ProtonTheme.colors.textWeak else ProtonTheme.colors.textNorm
        val iconColor = if (item.isRead) ProtonTheme.colors.iconWeak else ProtonTheme.colors.iconNorm

        Avatar(
            avatarUiModel = item.avatar,
            modifier = Modifier.padding(top = ProtonDimens.SmallSpacing, end = ProtonDimens.ExtraSmallSpacing)
        )
        Column(modifier = Modifier.padding(start = ProtonDimens.SmallSpacing, top = ProtonDimens.SmallSpacing)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                ActionIcons(
                    item = item,
                    iconColor = fontColor,
                    modifier = Modifier.padding(end = ProtonDimens.ExtraSmallSpacing)
                )
                Participants(
                    participants = item.participants,
                    fontWeight = fontWeight,
                    fontColor = fontColor,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = ProtonDimens.ExtraSmallSpacing)
                )
                Time(time = item.time, fontWeight = fontWeight, fontColor = fontColor)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LocationIcons(
                    iconResIds = item.locationIconResIds,
                    iconColor = fontColor,
                    modifier = Modifier.padding(end = ProtonDimens.ExtraSmallSpacing)
                )
                Row(
                    Modifier
                        .weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Subject(
                        subject = item.subject,
                        fontWeight = fontWeight,
                        fontColor = fontColor,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .padding(end = ProtonDimens.ExtraSmallSpacing)
                    )
                    Count(
                        count = item.numMessages,
                        fontWeight = fontWeight,
                        fontColor = fontColor,
                        iconColor = iconColor
                    )
                }
                Icons(
                    item = item,
                    iconColor = fontColor,
                    modifier = Modifier.padding(start = ProtonDimens.ExtraSmallSpacing)
                )
            }
            Row(
                modifier = Modifier
                    .padding(top = ProtonDimens.ExtraSmallSpacing, bottom = ProtonDimens.ExtraSmallSpacing)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ExpirationLabel(
                    hasExpirationTime = item.shouldShowExpirationLabel,
                    modifier = Modifier.padding(end = ProtonDimens.ExtraSmallSpacing)
                )
                Labels(labels = item.labels)
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
            SmallNonClickableIcon(R.drawable.ic_proton_arrow_up_and_left, iconColor = iconColor)
        }
        if (item.shouldShowRepliedAllIcon) {
            SmallNonClickableIcon(R.drawable.ic_proton_arrows_up_and_left, iconColor = iconColor)
        }
        if (item.shouldShowForwardedIcon) {
            SmallNonClickableIcon(R.drawable.ic_proton_arrow_right, iconColor = iconColor)
        }
    }
}

@Composable
private fun Participants(
    modifier: Modifier = Modifier,
    participants: ImmutableList<String>,
    fontWeight: FontWeight,
    fontColor: Color
) {
    Text(
        modifier = modifier.testTag(MailboxItemTestTags.Participants),
        text = participants.joinToString(),
        overflow = TextOverflow.Ellipsis,
        maxLines = 1,
        style = ProtonTheme.typography.default.copy(fontWeight = fontWeight, color = fontColor)
    )

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
        style = ProtonTheme.typography.caption.copy(fontWeight = fontWeight, color = fontColor)
    )
}

@Composable
private fun LocationIcons(
    modifier: Modifier = Modifier,
    iconResIds: ImmutableList<Int>,
    iconColor: Color
) {
    if (iconResIds.isEmpty()) {
        return
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Start
    ) {
        iconResIds.forEach { SmallNonClickableIcon(iconId = it, iconColor) }
    }
}

@Composable
private fun Subject(
    modifier: Modifier = Modifier,
    subject: String,
    fontWeight: FontWeight,
    fontColor: Color
) {
    Text(
        modifier = modifier.testTag(MailboxItemTestTags.Subject),
        text = subject,
        overflow = TextOverflow.Ellipsis,
        maxLines = 1,
        style = ProtonTheme.typography.defaultSmall.copy(fontWeight = fontWeight, color = fontColor)
    )
}

@Composable
private fun Count(
    modifier: Modifier = Modifier,
    count: Int?,
    fontWeight: FontWeight,
    fontColor: Color,
    iconColor: Color
) {
    if (count == null) {
        return
    }

    val stroke = BorderStroke(MailDimens.DefaultBorder, iconColor)
    Box(
        modifier = modifier
            .border(stroke, ProtonTheme.shapes.small)
    ) {
        Text(
            modifier = Modifier.padding(horizontal = ProtonDimens.ExtraSmallSpacing),
            text = count.toString(),
            overflow = TextOverflow.Ellipsis,
            style = ProtonTheme.typography.caption.copy(fontWeight = fontWeight, color = fontColor)
        )
    }
}

@Composable
private fun Icons(
    modifier: Modifier = Modifier,
    item: MailboxItemUiModel,
    iconColor: Color
) {

    if (item.hasIconsToShow().not()) return

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.End
    ) {
        if (item.shouldShowCalendarIcon) {
            SmallNonClickableIcon(iconId = R.drawable.ic_proton_calendar_grid, iconColor = iconColor)
        }
        if (item.shouldShowAttachmentIcon) {
            SmallNonClickableIcon(iconId = R.drawable.ic_proton_paper_clip, iconColor = iconColor)
        }
        if (item.showStar) {
            SmallNonClickableIcon(iconId = R.drawable.ic_proton_star_filled, tintId = R.color.notification_warning)
        }
    }
}

@Composable
private fun ExpirationLabel(
    modifier: Modifier = Modifier,
    hasExpirationTime: Boolean
) {
    if (hasExpirationTime) {
        Box(
            modifier = modifier
                .background(ProtonTheme.colors.interactionWeakNorm, ProtonTheme.shapes.large)
                .size(ProtonDimens.SmallIconSize)
                .padding(MailDimens.TinySpacing),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_proton_hourglass),
                tint = ProtonTheme.colors.iconNorm,
                contentDescription = NO_CONTENT_DESCRIPTION
            )
        }
    }
}

@Composable
private fun Labels(
    modifier: Modifier = Modifier,
    labels: ImmutableList<LabelUiModel>
) {
    LabelsList(modifier = modifier, labels = labels)
}


@Composable
@Preview(showBackground = true)
private fun DroidConMailboxItemPreview() {
    ProtonTheme {
        MailboxItem(
            modifier = Modifier,
            item = MailboxItemUiModelPreviewData.Conversation.DroidConLondon,
            onItemClicked = {},
            onOpenSelectionMode = {}
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
            onItemClicked = {},
            onOpenSelectionMode = {}
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
            onItemClicked = {},
            onOpenSelectionMode = {}
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
            onItemClicked = {},
            onOpenSelectionMode = {}
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
            onItemClicked = {},
            onOpenSelectionMode = {}
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
            onItemClicked = {},
            onOpenSelectionMode = {}
        )
    }
}

object MailboxItemTestTags {

    const val ItemRow = "MailboxItemRow"
    const val Participants = "Participants"
    const val Subject = "Subject"
    const val Date = "Date"
}
