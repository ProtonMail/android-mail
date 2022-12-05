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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import ch.protonmail.android.mailcommon.presentation.NO_CONTENT_DESCRIPTION
import ch.protonmail.android.mailcommon.presentation.compose.Avatar
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import ch.protonmail.android.mailcommon.presentation.compose.SmallNonClickableIcon
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.model.string
import ch.protonmail.android.maillabel.presentation.model.MailboxItemLabelUiModel
import ch.protonmail.android.maillabel.presentation.ui.MailboxItemLabels
import ch.protonmail.android.mailmailbox.presentation.R
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxItemUiModel
import ch.protonmail.android.mailmailbox.presentation.mailbox.previewdata.MailboxItemUiModelPreviewData
import kotlinx.collections.immutable.ImmutableList
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.captionWeak
import me.proton.core.compose.theme.defaultWeak
import me.proton.core.compose.theme.overline

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MailboxItem(
    modifier: Modifier = Modifier,
    item: MailboxItemUiModel,
    onItemClicked: (MailboxItemUiModel) -> Unit,
    onOpenSelectionMode: () -> Unit
) {
    Box(
        modifier = modifier
            .combinedClickable(onClick = { onItemClicked(item) }, onLongClick = onOpenSelectionMode)
            .padding(
                start = ProtonDimens.SmallSpacing,
                end = ProtonDimens.DefaultSpacing,
                top = ProtonDimens.ExtraSmallSpacing,
                bottom = ProtonDimens.ExtraSmallSpacing
            )
            .fillMaxWidth()
    ) {
        val fontWeight = if (item.isRead) FontWeight.Normal else FontWeight.Bold
        val fontColor = if (item.isRead) ProtonTheme.colors.textWeak else ProtonTheme.colors.textNorm

        MailboxItemLayout(
            avatar = { Avatar(avatarUiModel = item.avatar) },
            actionIcons = { ActionIcons(item = item, iconColor = fontColor) },
            participants = {
                Participants(participants = item.participants, fontWeight = fontWeight, fontColor = fontColor)
            },
            time = { Time(time = item.time, fontWeight = fontWeight, fontColor = fontColor) },
            locationIcons = { LocationIcons(iconResIds = item.locationIconResIds, iconColor = fontColor) },
            subject = { Subject(subject = item.subject, fontWeight = fontWeight, fontColor = fontColor) },
            count = { Count(count = item.numMessages, fontWeight = fontWeight, fontColor = fontColor) },
            icons = { Icons(item = item, iconColor = fontColor) },
            expirationLabel = { ExpirationLabel(hasExpirationTime = item.shouldShowExpirationLabel) },
            labels = { Labels(labels = item.labels) }
        )
    }
}


@Composable
private fun MailboxItemLayout(
    modifier: Modifier = Modifier,
    avatar: @Composable () -> Unit,
    actionIcons: @Composable () -> Unit,
    participants: @Composable () -> Unit,
    time: @Composable () -> Unit,
    locationIcons: @Composable () -> Unit,
    subject: @Composable () -> Unit,
    count: @Composable () -> Unit,
    icons: @Composable () -> Unit,
    expirationLabel: @Composable () -> Unit,
    labels: @Composable () -> Unit
) {
    ConstraintLayout(modifier = modifier.fillMaxWidth()) {

        val (
            avatarRef,
            actionIconsRef,
            participantsRef,
            timeRef,
            locationIconsRef,
            subjectRef,
            countRef, iconsRef,
            expirationLabelRef,
            labelsRef
        ) = createRefs()

        Box(
            modifier = modifier.constrainAs(avatarRef) {
                top.linkTo(parent.top)
                start.linkTo(parent.start)
            }
        ) { avatar() }

        Box(
            modifier = modifier.constrainAs(actionIconsRef) {
                top.linkTo(parent.top, margin = ProtonDimens.SmallSpacing)
                bottom.linkTo(subjectRef.top)
                start.linkTo(avatarRef.end, margin = ProtonDimens.SmallSpacing)
                end.linkTo(participantsRef.start, margin = ProtonDimens.ExtraSmallSpacing)
            }
        ) { actionIcons() }

        Box(
            modifier = modifier.constrainAs(participantsRef) {
                horizontalChainWeight = 0f
                width = Dimension.fillToConstraints
                top.linkTo(parent.top, margin = ProtonDimens.SmallSpacing)
                bottom.linkTo(subjectRef.top)
                start.linkTo(actionIconsRef.end)
                end.linkTo(timeRef.start)
            }
        ) { participants() }

        Box(
            modifier = modifier.constrainAs(timeRef) {
                top.linkTo(parent.top, margin = ProtonDimens.SmallSpacing)
                bottom.linkTo(subjectRef.top)
                end.linkTo(parent.end)
            },
            contentAlignment = Alignment.CenterEnd
        ) { time() }

        Box(
            modifier = modifier.constrainAs(locationIconsRef) {
                horizontalChainWeight = 0f
                top.linkTo(subjectRef.top)
                bottom.linkTo(subjectRef.bottom)
                start.linkTo(avatarRef.end, margin = ProtonDimens.SmallSpacing)
                end.linkTo(subjectRef.start, margin = ProtonDimens.ExtraSmallSpacing)
            }
        ) { locationIcons() }

        Box(
            modifier = modifier.constrainAs(subjectRef) {
                horizontalChainWeight = 0f
                width = Dimension.fillToConstraints
                top.linkTo(participantsRef.bottom)
                bottom.linkTo(labelsRef.top)
                start.linkTo(locationIconsRef.end)
                end.linkTo(countRef.start)
            }
        ) { subject() }

        Box(
            modifier = modifier.constrainAs(countRef) {
                horizontalChainWeight = 0f
                top.linkTo(subjectRef.top)
                bottom.linkTo(subjectRef.bottom)
                start.linkTo(subjectRef.end, margin = ProtonDimens.ExtraSmallSpacing)
                end.linkTo(iconsRef.start, margin = ProtonDimens.ExtraSmallSpacing)
            }
        ) { count() }

        Box(
            modifier = modifier.constrainAs(iconsRef) {
                top.linkTo(subjectRef.top)
                bottom.linkTo(subjectRef.bottom)
                end.linkTo(parent.end)
            }
        ) { icons() }

        Box(
            modifier = modifier.constrainAs(expirationLabelRef) {
                top.linkTo(subjectRef.bottom, margin = ProtonDimens.SmallSpacing)
                start.linkTo(avatarRef.end, margin = ProtonDimens.SmallSpacing)
            }
        ) { expirationLabel() }

        Box(
            modifier = modifier.constrainAs(labelsRef) {
                width = Dimension.preferredWrapContent
                top.linkTo(subjectRef.bottom, margin = ProtonDimens.SmallSpacing)
                start.linkTo(expirationLabelRef.end, margin = ProtonDimens.ExtraSmallSpacing)
                end.linkTo(parent.end)
            }
        ) { labels() }
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
        modifier = modifier,
        text = participants.joinToString(),
        overflow = TextOverflow.Ellipsis,
        maxLines = 1,
        style = ProtonTheme.typography.defaultWeak.copy(fontWeight = fontWeight, color = fontColor)
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
        modifier = modifier,
        text = time.string(),
        maxLines = 1,
        textAlign = TextAlign.End,
        style = ProtonTheme.typography.overline.copy(fontWeight = fontWeight, color = fontColor)
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
        modifier = modifier,
        text = subject,
        overflow = TextOverflow.Ellipsis,
        maxLines = 1,
        style = ProtonTheme.typography.captionWeak.copy(fontWeight = fontWeight, color = fontColor)
    )
}

@Composable
private fun Count(
    modifier: Modifier = Modifier,
    count: Int?,
    fontWeight: FontWeight,
    fontColor: Color
) {
    if (count == null) {
        return
    }

    val stroke = BorderStroke(MailDimens.ThinBorder, ProtonTheme.colors.textNorm)
    Box(
        modifier = modifier
            .padding(ProtonDimens.ExtraSmallSpacing)
            .border(stroke, ProtonTheme.shapes.small)
    ) {
        Text(
            modifier = Modifier.padding(horizontal = ProtonDimens.ExtraSmallSpacing),
            text = count.toString(),
            overflow = TextOverflow.Ellipsis,
            style = ProtonTheme.typography.overline.copy(fontWeight = fontWeight, color = fontColor)
        )
    }
}

@Composable
private fun Icons(
    modifier: Modifier = Modifier,
    item: MailboxItemUiModel,
    iconColor: Color
) {
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
            SmallNonClickableIcon(iconId = R.drawable.ic_proton_star_filled, tintId = R.color.sunglow)
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
                contentDescription = NO_CONTENT_DESCRIPTION
            )
        }
    }
}

@Composable
private fun Labels(
    modifier: Modifier = Modifier,
    labels: ImmutableList<MailboxItemLabelUiModel>
) {
    if (labels.isNotEmpty()) {
        MailboxItemLabels(modifier = modifier, labels = labels)
    }
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
