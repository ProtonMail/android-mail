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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.constraintlayout.compose.Visibility
import androidx.constraintlayout.compose.atLeast
import ch.protonmail.android.mailcommon.presentation.NO_CONTENT_DESCRIPTION
import ch.protonmail.android.mailcommon.presentation.compose.Avatar
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import ch.protonmail.android.mailcommon.presentation.compose.SmallNonClickableIcon
import ch.protonmail.android.mailcommon.presentation.model.string
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailMessageUiModel
import ch.protonmail.android.maildetail.presentation.previewdata.ConversationDetailCollapsedMessageHeaderPreviewData
import ch.protonmail.android.maildetail.presentation.ui.ConversationDetailCollapsedMessageHeader.AttachmentIconTestTag
import ch.protonmail.android.maildetail.presentation.ui.ConversationDetailCollapsedMessageHeader.ForwardedIconTestTag
import ch.protonmail.android.maildetail.presentation.ui.ConversationDetailCollapsedMessageHeader.MinLabelsWidth
import ch.protonmail.android.maildetail.presentation.ui.ConversationDetailCollapsedMessageHeader.RepliedAllIconTestTag
import ch.protonmail.android.maildetail.presentation.ui.ConversationDetailCollapsedMessageHeader.RepliedIconTestTag
import ch.protonmail.android.maildetail.presentation.ui.ConversationDetailCollapsedMessageHeader.StarIconTestTag
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.ProtonTheme3
import me.proton.core.compose.theme.caption
import me.proton.core.compose.theme.default
import me.proton.core.compose.theme.overline
import me.proton.core.presentation.R.drawable
import me.proton.core.util.kotlin.EMPTY_STRING
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
        ConstraintLayout(
            modifier = Modifier
                .padding(ProtonDimens.SmallSpacing)
                .fillMaxWidth()
        ) {
            val (
                avatarRef,
                forwardedIconRef,
                repliedIconRef,
                senderRef,
                expirationRef,
                labelsRef,
                starIconRef,
                attachmentIconRef,
                locationIconRef,
                timeRef
            ) = createRefs()

            createHorizontalChain(
                avatarRef,
                forwardedIconRef,
                repliedIconRef,
                senderRef,
                expirationRef,
                labelsRef,
                starIconRef,
                attachmentIconRef,
                locationIconRef,
                timeRef
            )

            Avatar(
                modifier = Modifier.constrainAs(avatarRef) {
                    centerVerticallyTo(parent)
                },
                avatarUiModel = message.avatar
            )

            ForwardedIcon(
                modifier = Modifier.constrainAs(forwardedIconRef) {
                    centerVerticallyTo(parent)
                },
                message = message,
                fontColor = fontColor
            )

            RepliedIcon(
                modifier = Modifier.constrainAs(repliedIconRef) {
                    centerVerticallyTo(parent)
                },
                message = message,
                fontColor = fontColor
            )

            Sender(
                modifier = Modifier.constrainAs(senderRef) {
                    width = Dimension.preferredWrapContent
                    centerVerticallyTo(parent)
                },
                message = message,
                fontWeight = fontWeight,
                fontColor = fontColor
            )

            Expiration(
                modifier = Modifier.constrainAs(expirationRef) {
                    visibility = visibleWhen(message.expiration != null)
                    centerVerticallyTo(parent)
                },
                message = message
            )

            Text(
                modifier = Modifier.constrainAs(labelsRef) {
                    visibility = Visibility.Invisible
                    width = Dimension.fillToConstraints.atLeast(1.dp)
                    centerVerticallyTo(parent)
                },
                text = "Labels",
                overflow = TextOverflow.Ellipsis,
                maxLines = 1
            )

            StarIcon(
                modifier = Modifier.constrainAs(starIconRef) {
                    visibility = visibleWhen(message.isStarred)
                    centerVerticallyTo(parent)
                }
            )

            AttachmentIcon(
                fontColor = fontColor,
                modifier = Modifier.constrainAs(attachmentIconRef) {
                    visibility = visibleWhen(message.hasAttachments)
                    centerVerticallyTo(parent)
                }
            )

            LocationIcon(
                modifier = Modifier.constrainAs(locationIconRef) {
                    centerVerticallyTo(parent)
                },
                message = message,
                fontColor = fontColor
            )

            Time(
                modifier = Modifier.constrainAs(timeRef) {
                    centerVerticallyTo(parent)
                },
                message = message,
                fontWeight = fontWeight,
                fontColor = fontColor
            )
        }
    }
}

@Composable
private fun AttachmentIcon(
    fontColor: Color,
    modifier: Modifier
) {
    Icon(
        modifier = modifier.testTag(AttachmentIconTestTag).size(ProtonDimens.SmallIconSize),
        painter = painterResource(id = drawable.ic_proton_paper_clip),
        tint = fontColor,
        contentDescription = NO_CONTENT_DESCRIPTION
    )
}

@Composable
private fun Expiration(message: ConversationDetailMessageUiModel.Collapsed, modifier: Modifier) {
    Row(
        modifier = modifier
            .padding(horizontal = ProtonDimens.ExtraSmallSpacing)
            .background(
                color = ProtonTheme.colors.interactionWeakNorm,
                shape = ProtonTheme.shapes.large
            )
            .padding(ProtonDimens.ExtraSmallSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            modifier = Modifier.size(MailDimens.TinyIcon),
            painter = painterResource(id = drawable.ic_proton_hourglass),
            contentDescription = NO_CONTENT_DESCRIPTION
        )
        Text(
            text = message.expiration?.string() ?: EMPTY_STRING,
            style = ProtonTheme.typography.overline
        )
    }
}

@Composable
private fun ForwardedIcon(
    message: ConversationDetailMessageUiModel.Collapsed,
    fontColor: Color,
    modifier: Modifier
) {
    when (message.forwardedIcon) {
        ConversationDetailMessageUiModel.ForwardedIcon.None -> Box(modifier)
        ConversationDetailMessageUiModel.ForwardedIcon.Forwarded -> SmallNonClickableIcon(
            modifier = modifier.testTag(ForwardedIconTestTag),
            iconId = drawable.ic_proton_arrow_right,
            iconColor = fontColor
        )
    }.exhaustive
}

@Composable
private fun LocationIcon(
    message: ConversationDetailMessageUiModel.Collapsed,
    fontColor: Color,
    modifier: Modifier
) {
    Icon(
        modifier = modifier.size(ProtonDimens.SmallIconSize),
        painter = painterResource(id = message.locationIcon.icon),
        tint = fontColor,
        contentDescription = NO_CONTENT_DESCRIPTION
    )
}

@Composable
private fun RepliedIcon(
    message: ConversationDetailMessageUiModel.Collapsed,
    fontColor: Color,
    modifier: Modifier
) {
    when (message.repliedIcon) {
        ConversationDetailMessageUiModel.RepliedIcon.None -> Box(modifier)
        ConversationDetailMessageUiModel.RepliedIcon.Replied -> SmallNonClickableIcon(
            modifier = modifier.testTag(RepliedIconTestTag),
            iconId = drawable.ic_proton_arrow_up_and_left,
            iconColor = fontColor
        )
        ConversationDetailMessageUiModel.RepliedIcon.RepliedAll -> SmallNonClickableIcon(
            modifier = modifier.testTag(RepliedAllIconTestTag),
            iconId = drawable.ic_proton_arrows_up_and_left,
            iconColor = fontColor
        )
    }.exhaustive
}

@Composable
private fun Sender(
    message: ConversationDetailMessageUiModel.Collapsed,
    fontWeight: FontWeight,
    fontColor: Color,
    modifier: Modifier
) {
    Text(
        modifier = modifier,
        text = message.sender,
        fontWeight = fontWeight,
        color = fontColor,
        style = ProtonTheme.typography.default,
        overflow = TextOverflow.Ellipsis,
        maxLines = 1
    )
}

@Composable
private fun StarIcon(modifier: Modifier) {
    Icon(
        modifier = modifier.testTag(StarIconTestTag).size(ProtonDimens.SmallIconSize),
        painter = painterResource(id = drawable.ic_proton_star_filled),
        tint = ProtonTheme.colors.notificationWarning,
        contentDescription = NO_CONTENT_DESCRIPTION
    )
}

@Composable
private fun Time(
    message: ConversationDetailMessageUiModel.Collapsed,
    fontWeight: FontWeight,
    fontColor: Color,
    modifier: Modifier
) {
    Text(
        modifier = modifier.padding(horizontal = ProtonDimens.ExtraSmallSpacing),
        text = message.shortTime.string(),
        fontWeight = fontWeight,
        color = fontColor,
        style = ProtonTheme.typography.caption,
        maxLines = 1
    )
}

private fun visibleWhen(isVisible: Boolean) = if (isVisible) Visibility.Visible else Visibility.Gone

object ConversationDetailCollapsedMessageHeader {

    const val AttachmentIconTestTag = "attachment_icon"
    const val ForwardedIconTestTag = "forwarded_icon"
    const val RepliedAllIconTestTag = "replied_all_icon"
    const val RepliedIconTestTag = "replied_icon"
    const val StarIconTestTag = "star_icon"
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
