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

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.model.string
import ch.protonmail.android.maillabel.presentation.model.MailboxItemLabelUiModel
import ch.protonmail.android.maillabel.presentation.ui.MailboxItemLabels
import ch.protonmail.android.mailmailbox.presentation.R
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxItemUiModel
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.captionWeak
import me.proton.core.compose.theme.defaultWeak
import me.proton.core.compose.theme.headline
import me.proton.core.compose.theme.overline

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MailboxItem(
    modifier: Modifier = Modifier,
    item: MailboxItemUiModel,
    onItemClicked: (MailboxItemUiModel) -> Unit,
    onOpenSelectionMode: () -> Unit
) {
    Card(
        modifier = modifier
            .padding(ProtonDimens.ExtraSmallSpacing)
            .fillMaxWidth()
            .combinedClickable(onClick = { onItemClicked(item) }, onLongClick = onOpenSelectionMode)
    ) {
        val fontWeight = if (item.isRead) FontWeight.Normal else FontWeight.Bold

        MailboxItemLayout(
            avatar = { Avatar(participants = item.participants, fontWeight = fontWeight) },
            actionIcons = { ActionIcons(item = item) },
            participants = { Participants(participants = item.participants, fontWeight = fontWeight) },
            time = { Time(time = item.time, fontWeight = fontWeight) },
            subject = { Subject(subject = item.subject, fontWeight = fontWeight) },
            count = { Count(count = item.numMessages, fontWeight = fontWeight) },
            icons = { Icons(item = item) },
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
    subject: @Composable () -> Unit,
    count: @Composable () -> Unit,
    icons: @Composable () -> Unit,
    labels: @Composable () -> Unit
) {
    ConstraintLayout(modifier = modifier.fillMaxWidth()) {

        val spacing = ProtonDimens.ExtraSmallSpacing
        val (
            avatarRef, actionIconsRef, participantsRef, timeRef, subjectRef, countRef, iconsRef, labelsRef
        ) = createRefs()

        Box(
            modifier = modifier.constrainAs(avatarRef) {
                top.linkTo(parent.top)
                start.linkTo(parent.start)
                bottom.linkTo(parent.bottom)
            }
        ) { avatar() }

        Box(
            modifier = modifier.constrainAs(actionIconsRef) {
                top.linkTo(parent.top)
                bottom.linkTo(subjectRef.top)
                start.linkTo(avatarRef.end)
            }
        ) { actionIcons() }

        Box(
            modifier = modifier.constrainAs(participantsRef) {
                horizontalChainWeight = 0f
                width = Dimension.fillToConstraints
                top.linkTo(parent.top)
                bottom.linkTo(subjectRef.top)
                start.linkTo(actionIconsRef.end)
                end.linkTo(timeRef.start)
            }
        ) { participants() }

        Box(
            modifier = modifier.constrainAs(timeRef) {
                top.linkTo(parent.top)
                bottom.linkTo(subjectRef.top)
                end.linkTo(parent.end, margin = spacing)
            },
            contentAlignment = Alignment.CenterEnd
        ) { time() }

        Box(
            modifier = modifier.constrainAs(subjectRef) {
                horizontalChainWeight = 0f
                width = Dimension.fillToConstraints
                top.linkTo(participantsRef.bottom, margin = spacing)
                bottom.linkTo(labelsRef.top)
                start.linkTo(avatarRef.end)
                end.linkTo(countRef.start)
            }
        ) { subject() }

        Box(
            modifier = modifier.constrainAs(countRef) {
                horizontalChainWeight = 0f
                top.linkTo(participantsRef.bottom, margin = spacing)
                bottom.linkTo(labelsRef.top)
                start.linkTo(subjectRef.end, margin = spacing)
                end.linkTo(iconsRef.start, margin = spacing)
            }
        ) { count() }

        Box(
            modifier = modifier.constrainAs(iconsRef) {
                top.linkTo(participantsRef.bottom, margin = spacing)
                bottom.linkTo(labelsRef.top)
                end.linkTo(parent.end, margin = spacing)
            }
        ) { icons() }

        Box(
            modifier = modifier.constrainAs(labelsRef) {
                width = Dimension.preferredWrapContent
                top.linkTo(subjectRef.bottom, margin = spacing)
                bottom.linkTo(parent.bottom, margin = spacing)
                start.linkTo(avatarRef.end)
                end.linkTo(parent.end)
            }
        ) { labels() }
    }
}

@Composable
private fun Avatar(
    modifier: Modifier = Modifier,
    participants: List<String>,
    fontWeight: FontWeight
) {
    Box(modifier = modifier.padding(ProtonDimens.DefaultSpacing)) {
        Text(
            text = participants.firstOrNull()?.first()?.uppercase() ?: "*",
            style = ProtonTheme.typography.headline.copy(fontWeight = fontWeight)
        )
    }
}

@Composable
private fun ActionIcons(
    modifier: Modifier = Modifier,
    item: MailboxItemUiModel
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
            MailboxItemIcon(R.drawable.ic_proton_arrow_up_and_left)
        }
        if (item.shouldShowRepliedAllIcon) {
            MailboxItemIcon(R.drawable.ic_proton_arrows_up_and_left)
        }
        if (item.shouldShowForwardedIcon) {
            MailboxItemIcon(R.drawable.ic_proton_arrow_right)
        }
    }
}

@Composable
private fun Participants(
    modifier: Modifier = Modifier,
    participants: List<String>,
    fontWeight: FontWeight
) {
    Text(
        modifier = modifier,
        text = participants.joinToString(),
        overflow = TextOverflow.Ellipsis,
        maxLines = 1,
        style = ProtonTheme.typography.defaultWeak.copy(fontWeight = fontWeight)
    )

}

@Composable
private fun Time(
    modifier: Modifier = Modifier,
    time: TextUiModel,
    fontWeight: FontWeight
) {
    Text(
        modifier = modifier,
        text = time.string(),
        maxLines = 1,
        textAlign = TextAlign.End,
        style = ProtonTheme.typography.overline.copy(
            fontWeight = fontWeight,
            color = ProtonTheme.colors.textWeak
        )
    )
}

@Composable
private fun Subject(
    modifier: Modifier = Modifier,
    subject: String,
    fontWeight: FontWeight
) {
    Text(
        modifier = modifier,
        text = subject,
        overflow = TextOverflow.Ellipsis,
        maxLines = 1,
        style = ProtonTheme.typography.captionWeak.copy(fontWeight = fontWeight)
    )
}

@Composable
private fun Count(
    modifier: Modifier = Modifier,
    count: Int?,
    fontWeight: FontWeight
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
            style = ProtonTheme.typography.overline.copy(
                fontWeight = fontWeight,
                color = ProtonTheme.colors.textWeak
            )
        )
    }
}

@Composable
private fun Icons(
    modifier: Modifier = Modifier,
    item: MailboxItemUiModel
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.End
    ) {
        if (item.shouldShowAttachmentIcon) {
            MailboxItemIcon(iconId = R.drawable.ic_proton_paper_clip)
        }
        if (item.showStar) {
            MailboxItemIcon(iconId = R.drawable.ic_proton_star_filled, tintId = R.color.sunglow)
        }
    }
}

@Composable
private fun Labels(
    modifier: Modifier = Modifier,
    labels: List<MailboxItemLabelUiModel>
) {
    if (labels.isNotEmpty()) {
        MailboxItemLabels(modifier = modifier, labels = labels)
    }
}

@Composable
private fun MailboxItemIcon(
    @DrawableRes iconId: Int,
    tintId: Int = R.color.icon_weak
) {
    Icon(
        modifier = Modifier.size(ProtonDimens.SmallIconSize),
        painter = painterResource(id = iconId),
        contentDescription = null,
        tint = colorResource(id = tintId)
    )
}

@Preview(
    name = "Mailbox Item"
)
@Composable
private fun MailboxItemPreview() {
    ProtonTheme {
        MailboxItem(
            modifier = Modifier,
            item = MailboxPreviewData.mailboxItem,
            onItemClicked = {},
            onOpenSelectionMode = {}
        )
    }
}
