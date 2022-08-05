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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.protonmail.android.mailcommon.presentation.model.string
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemType
import ch.protonmail.android.mailmailbox.presentation.R
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxItemUiModel
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme

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
            .padding(4.dp)
            .fillMaxWidth()
            .combinedClickable(onClick = { onItemClicked(item) }, onLongClick = onOpenSelectionMode)
    ) {
        Row {
            ContactAvatar()

            Column(
                modifier = Modifier
                    .padding(ProtonDimens.SmallSpacing)
                    .fillMaxWidth()
            ) {
                MailboxItemFirstRow(item = item)
                MailboxItemSecondRow(item = item)
                MailboxItemThirdRow(item = item)
            }

        }
    }
}

@Composable
private fun MailboxItemFirstRow(
    modifier: Modifier = Modifier,
    item: MailboxItemUiModel
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val fontWeight = if (item.isRead) FontWeight.Normal else FontWeight.Bold
        Row(
            modifier = modifier.weight(.8f),
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
            Text(
                modifier = Modifier,
                text = item.participants.joinToString { it },
                fontWeight = fontWeight,
                color = colorResource(id = R.color.text_weak),
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                fontSize = 16.sp
            )
        }

        Text(
            modifier = Modifier.weight(.2f),
            text = item.time.string(),
            fontWeight = fontWeight,
            color = colorResource(id = R.color.text_weak),
            maxLines = 1,
            fontSize = 12.sp,
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun MailboxItemSecondRow(
    modifier: Modifier = Modifier,
    item: MailboxItemUiModel
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = modifier.weight(.8f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val fontWeight = if (item.isRead) FontWeight.Normal else FontWeight.Bold
            Text(
                text = item.subject,
                fontWeight = fontWeight,
                color = colorResource(id = R.color.text_weak),
                maxLines = 1,
                fontSize = 14.sp
            )
            item.numMessages?.let { numMessages ->
                Box(
                    modifier = Modifier
                        .padding(ProtonDimens.ExtraSmallSpacing)
                        .border(0.5.dp, colorResource(id = R.color.text_weak), shape = RoundedCornerShape(4.dp)),
                ) {
                    Text(
                        modifier = Modifier
                            .padding(horizontal = 5.dp),
                        text = numMessages.toString(),
                        fontWeight = fontWeight,
                        color = colorResource(id = R.color.text_weak),
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Row(
            modifier = Modifier.weight(.2f),
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
}

@Composable
private fun MailboxItemThirdRow(
    modifier: Modifier = Modifier,
    item: MailboxItemUiModel
) {
    Row(modifier = modifier) {
        val fontWeight = if (item.isRead) FontWeight.Normal else FontWeight.Bold
        Text(
            text = when (item.type) {
                MailboxItemType.Message -> "Message "
                MailboxItemType.Conversation -> "Conversation "
            } + "Labels: ${item.labels.map { it.name }}",
            fontWeight = fontWeight,
            color = colorResource(id = R.color.text_weak),
            maxLines = 1,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun MailboxItemIcon(
    @DrawableRes iconId: Int,
    tintId: Int = R.color.icon_weak
) {
    Icon(
        modifier = Modifier.size(16.dp),
        painter = painterResource(id = iconId),
        contentDescription = null,
        tint = colorResource(id = tintId)
    )
}

@Composable
fun ContactAvatar(
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.padding(16.dp)) {
        Text(
            text = "A",
            fontSize = 20.sp
        )
    }
}

@Preview(
    name = "Mailbox Item"
)
@Composable
fun PreviewMailboxItem() {
    ProtonTheme {
        MailboxItem(
            modifier = Modifier,
            item = MailboxPreviewData.mailboxItem,
            onItemClicked = {},
            onOpenSelectionMode = {}
        )
    }
}
