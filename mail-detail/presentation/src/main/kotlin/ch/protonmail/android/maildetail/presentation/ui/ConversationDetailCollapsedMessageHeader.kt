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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import ch.protonmail.android.mailcommon.presentation.NO_CONTENT_DESCRIPTION
import ch.protonmail.android.mailcommon.presentation.compose.Avatar
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import ch.protonmail.android.mailcommon.presentation.compose.SmallNonClickableIcon
import ch.protonmail.android.mailcommon.presentation.model.string
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailMessageUiModel
import ch.protonmail.android.maildetail.presentation.previewdata.ConversationDetailCollapsedMessageHeaderPreviewData
import ch.protonmail.android.maildetail.presentation.ui.ConversationDetailCollapsedMessageHeader.ForwardedIconTestTag
import ch.protonmail.android.maildetail.presentation.ui.ConversationDetailCollapsedMessageHeader.RepliedAllIconTestTag
import ch.protonmail.android.maildetail.presentation.ui.ConversationDetailCollapsedMessageHeader.RepliedIconTestTag
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.ProtonTheme3
import me.proton.core.compose.theme.default
import me.proton.core.compose.theme.overline
import me.proton.core.presentation.R.drawable
import me.proton.core.util.kotlin.exhaustive

@Composable
internal fun ConversationDetailCollapsedMessageHeader(message: ConversationDetailMessageUiModel.Collapsed) {
    val fontWeight = if (message.isUnread) FontWeight.Bold else FontWeight.Normal
    val fontColor = if (message.isUnread) ProtonTheme.colors.textNorm else ProtonTheme.colors.textWeak

    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = ProtonTheme.colors.backgroundNorm
        )
    ) {
        Row(
            modifier = Modifier
                .padding(ProtonDimens.SmallSpacing)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Avatar(avatarUiModel = message.avatar)
            Spacer(modifier = Modifier.width(ProtonDimens.SmallSpacing))
            ForwardedIcon(message, fontColor)
            RepliedIcon(message, fontColor).exhaustive
            Sender(message, fontWeight, fontColor)
            Expiration(message)
        }
    }
}

@Composable
private fun Expiration(message: ConversationDetailMessageUiModel.Collapsed) {
    if (message.expiration != null) {
        Row(
            modifier = Modifier
                .padding(horizontal = ProtonDimens.ExtraSmallSpacing)
                .background(
                    color = ProtonTheme.colors.interactionWeakNorm,
                    shape = ProtonTheme.shapes.medium
                )
                .padding(horizontal = ProtonDimens.ExtraSmallSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = Modifier.size(MailDimens.TinyIcon),
                painter = painterResource(id = drawable.ic_proton_hourglass),
                contentDescription = NO_CONTENT_DESCRIPTION
            )
            Text(
                text = message.expiration.string(),
                style = ProtonTheme.typography.overline
            )
        }
    }
}

@Composable
private fun ForwardedIcon(
    message: ConversationDetailMessageUiModel.Collapsed,
    fontColor: Color
) {
    when (message.forwardedIcon) {
        ConversationDetailMessageUiModel.ForwardedIcon.None -> Unit
        ConversationDetailMessageUiModel.ForwardedIcon.Forwarded -> SmallNonClickableIcon(
            modifier = Modifier.testTag(ForwardedIconTestTag),
            iconId = drawable.ic_proton_arrow_right,
            iconColor = fontColor
        )
    }.exhaustive
}

@Composable
private fun RepliedIcon(
    message: ConversationDetailMessageUiModel.Collapsed,
    fontColor: Color
) {
    when (message.repliedIcon) {
        ConversationDetailMessageUiModel.RepliedIcon.None -> Unit
        ConversationDetailMessageUiModel.RepliedIcon.Replied -> SmallNonClickableIcon(
            modifier = Modifier.testTag(RepliedIconTestTag),
            iconId = drawable.ic_proton_arrow_up_and_left,
            iconColor = fontColor
        )
        ConversationDetailMessageUiModel.RepliedIcon.RepliedAll -> SmallNonClickableIcon(
            modifier = Modifier.testTag(RepliedAllIconTestTag),
            iconId = drawable.ic_proton_arrows_up_and_left,
            iconColor = fontColor
        )
    }
}

@Composable
private fun Sender(
    message: ConversationDetailMessageUiModel.Collapsed,
    fontWeight: FontWeight,
    fontColor: Color
) {
    Text(
        text = message.sender,
        fontWeight = fontWeight,
        color = fontColor,
        style = ProtonTheme.typography.default
    )
}

object ConversationDetailCollapsedMessageHeader {

    const val ForwardedIconTestTag = "forwarded_icon"
    const val RepliedAllIconTestTag = "replied_all_icon"
    const val RepliedIconTestTag = "replied_icon"
}

@Preview
@Composable
private fun CdCollapsedMessageHeaderPreview(
    @PreviewParameter(ConversationDetailCollapsedMessageHeaderPreviewData::class)
    message: ConversationDetailMessageUiModel.Collapsed
) {
    ProtonTheme3 {
        ProtonTheme {
            ConversationDetailCollapsedMessageHeader(message = message)
        }
    }
}
