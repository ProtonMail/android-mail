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

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import ch.protonmail.android.mailcommon.presentation.compose.OfficialBadge
import ch.protonmail.android.mailcommon.presentation.compose.SmallNonClickableIcon
import ch.protonmail.android.mailcommon.presentation.model.string
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailMessageUiModel
import ch.protonmail.android.maildetail.presentation.previewdata.ConversationDetailCollapsedMessageHeaderPreviewData
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.ProtonTheme3
import me.proton.core.compose.theme.captionNorm
import me.proton.core.compose.theme.defaultNorm
import me.proton.core.compose.theme.overlineNorm
import me.proton.core.presentation.R.drawable
import me.proton.core.util.kotlin.EMPTY_STRING
import me.proton.core.util.kotlin.exhaustive

@Composable
internal fun ConversationDetailCollapsedMessageHeader(
    uiModel: ConversationDetailMessageUiModel.Collapsed,
    modifier: Modifier = Modifier
) {
    val fontWeight = if (uiModel.isUnread) FontWeight.Bold else FontWeight.Normal
    val fontColor = if (uiModel.isUnread) ProtonTheme.colors.textNorm else ProtonTheme.colors.textWeak

    ConstraintLayout(
        modifier = modifier
            .testTag(ConversationDetailCollapsedMessageHeaderTestTags.RootItem)
            .padding(ProtonDimens.SmallSpacing)
            .padding(ProtonDimens.ExtraSmallSpacing)
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
            modifier = Modifier
                .padding(end = ProtonDimens.SmallSpacing)
                .constrainAs(avatarRef) {
                    centerVerticallyTo(parent)
                },
            avatarUiModel = uiModel.avatar
        )

        ForwardedIcon(
            modifier = Modifier.constrainAs(forwardedIconRef) {
                centerVerticallyTo(parent)
            },
            uiModel = uiModel,
            fontColor = fontColor
        )

        RepliedIcon(
            modifier = Modifier.constrainAs(repliedIconRef) {
                centerVerticallyTo(parent)
            },
            uiModel = uiModel,
            fontColor = fontColor
        )

        Sender(
            modifier = Modifier.constrainAs(senderRef) {
                width = Dimension.preferredWrapContent
                centerVerticallyTo(parent)
            },
            uiModel = uiModel,
            fontWeight = fontWeight,
            fontColor = fontColor
        )

        Expiration(
            modifier = Modifier.constrainAs(expirationRef) {
                visibility = visibleWhen(uiModel.expiration != null)
                centerVerticallyTo(parent)
            },
            uiModel = uiModel
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
                visibility = visibleWhen(uiModel.isStarred)
                centerVerticallyTo(parent)
            }
        )

        AttachmentIcon(
            fontColor = fontColor,
            modifier = Modifier.constrainAs(attachmentIconRef) {
                visibility = visibleWhen(uiModel.hasAttachments)
                centerVerticallyTo(parent)
            }
        )

        LocationIcon(
            uiModel = uiModel,
            fontColor = fontColor,
            modifier = Modifier.constrainAs(locationIconRef) {
                centerVerticallyTo(parent)
            }
        )

        Time(
            modifier = Modifier.constrainAs(timeRef) {
                centerVerticallyTo(parent)
            },
            uiModel = uiModel,
            fontWeight = fontWeight,
            fontColor = fontColor
        )
    }
}

@Composable
private fun AttachmentIcon(fontColor: Color, modifier: Modifier) {
    Icon(
        modifier = modifier
            .testTag(ConversationDetailCollapsedMessageHeaderTestTags.AttachmentIcon)
            .size(ProtonDimens.SmallIconSize),
        painter = painterResource(id = drawable.ic_proton_paper_clip),
        tint = fontColor,
        contentDescription = NO_CONTENT_DESCRIPTION
    )
}

@Composable
private fun Expiration(uiModel: ConversationDetailMessageUiModel.Collapsed, modifier: Modifier) {
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
            modifier = Modifier
                .testTag(ConversationDetailCollapsedMessageHeaderTestTags.ExpirationIcon)
                .size(MailDimens.TinyIcon),
            painter = painterResource(id = drawable.ic_proton_hourglass),
            tint = ProtonTheme.colors.iconNorm,
            contentDescription = NO_CONTENT_DESCRIPTION
        )
        Text(
            modifier = Modifier.testTag(ConversationDetailCollapsedMessageHeaderTestTags.ExpirationText),
            text = uiModel.expiration?.string() ?: EMPTY_STRING,
            style = ProtonTheme.typography.overlineNorm
        )
    }
}

@Composable
private fun ForwardedIcon(
    uiModel: ConversationDetailMessageUiModel.Collapsed,
    fontColor: Color,
    modifier: Modifier
) {
    when (uiModel.forwardedIcon) {
        ConversationDetailMessageUiModel.ForwardedIcon.None -> Box(modifier)
        ConversationDetailMessageUiModel.ForwardedIcon.Forwarded -> SmallNonClickableIcon(
            modifier = modifier.testTag(ConversationDetailCollapsedMessageHeaderTestTags.ForwardedIcon),
            iconId = drawable.ic_proton_arrow_right,
            iconColor = fontColor
        )
    }.exhaustive
}

@Composable
private fun LocationIcon(
    uiModel: ConversationDetailMessageUiModel.Collapsed,
    fontColor: Color,
    modifier: Modifier
) {
    Icon(
        modifier = modifier
            .testTag(ConversationDetailCollapsedMessageHeaderTestTags.Location)
            .size(ProtonDimens.SmallIconSize),
        painter = painterResource(id = uiModel.locationIcon.icon),
        tint = uiModel.locationIcon.color ?: fontColor,
        contentDescription = NO_CONTENT_DESCRIPTION
    )
}

@Composable
private fun RepliedIcon(
    uiModel: ConversationDetailMessageUiModel.Collapsed,
    fontColor: Color,
    modifier: Modifier
) {
    when (uiModel.repliedIcon) {
        ConversationDetailMessageUiModel.RepliedIcon.None -> Box(modifier)
        ConversationDetailMessageUiModel.RepliedIcon.Replied -> SmallNonClickableIcon(
            modifier = modifier
                .testTag(ConversationDetailCollapsedMessageHeaderTestTags.RepliedIcon)
                .padding(horizontal = MailDimens.TinySpacing),
            iconId = drawable.ic_proton_arrow_up_and_left,
            iconColor = fontColor
        )

        ConversationDetailMessageUiModel.RepliedIcon.RepliedAll -> SmallNonClickableIcon(
            modifier = modifier
                .testTag(ConversationDetailCollapsedMessageHeaderTestTags.RepliedAllIcon)
                .padding(horizontal = MailDimens.TinySpacing),
            iconId = drawable.ic_proton_arrows_up_and_left,
            iconColor = fontColor
        )
    }.exhaustive
}

@Composable
private fun Sender(
    uiModel: ConversationDetailMessageUiModel.Collapsed,
    fontWeight: FontWeight,
    fontColor: Color,
    modifier: Modifier
) {
    Row(
        modifier = modifier.padding(horizontal = MailDimens.TinySpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = modifier
                .testTag(ConversationDetailCollapsedMessageHeaderTestTags.Sender)
                .weight(1f, fill = false),
            text = uiModel.sender.participantName,
            fontWeight = fontWeight,
            color = fontColor,
            style = ProtonTheme.typography.defaultNorm,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1
        )
        if (uiModel.sender.shouldShowOfficialBadge) {
            OfficialBadge()
        }
    }
}

@Composable
private fun StarIcon(modifier: Modifier) {
    Icon(
        modifier = modifier
            .testTag(ConversationDetailCollapsedMessageHeaderTestTags.StarIcon)
            .size(ProtonDimens.SmallIconSize),
        painter = painterResource(id = drawable.ic_proton_star_filled),
        tint = ProtonTheme.colors.notificationWarning,
        contentDescription = NO_CONTENT_DESCRIPTION
    )
}

@Composable
private fun Time(
    uiModel: ConversationDetailMessageUiModel.Collapsed,
    fontWeight: FontWeight,
    fontColor: Color,
    modifier: Modifier
) {
    Text(
        modifier = modifier
            .testTag(ConversationDetailCollapsedMessageHeaderTestTags.Time)
            .padding(start = ProtonDimens.ExtraSmallSpacing),
        text = uiModel.shortTime.string(),
        fontWeight = fontWeight,
        color = fontColor,
        style = ProtonTheme.typography.captionNorm,
        maxLines = 1
    )
}

private fun visibleWhen(isVisible: Boolean) = if (isVisible) Visibility.Visible else Visibility.Gone

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CdCollapsedMessageHeaderPreview(
    @PreviewParameter(ConversationDetailCollapsedMessageHeaderPreviewData::class)
    uiModel: ConversationDetailMessageUiModel.Collapsed
) {
    ProtonTheme3 {
        ProtonTheme {
            ConversationDetailCollapsedMessageHeader(uiModel = uiModel)
        }
    }
}

object ConversationDetailCollapsedMessageHeaderTestTags {

    const val RootItem = "ConversationDetailCollapsedMessageHeaderRootItem"
    const val AttachmentIcon = "AttachmentIcon"
    const val ForwardedIcon = "ForwardedIcon"
    const val RepliedIcon = "RepliedIcon"
    const val RepliedAllIcon = "RepliedAllIcon"
    const val StarIcon = "StarIcon"
    const val Sender = "Sender"
    const val ExpirationIcon = "ExpirationIcon"
    const val ExpirationText = "ExpirationText"
    const val Location = "Location"
    const val Time = "Time"
}
