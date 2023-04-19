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

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.constraintlayout.compose.ConstrainScope
import androidx.constraintlayout.compose.ConstrainedLayoutReference
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.constraintlayout.compose.Visibility
import ch.protonmail.android.mailcommon.presentation.NO_CONTENT_DESCRIPTION
import ch.protonmail.android.mailcommon.presentation.compose.Avatar
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import ch.protonmail.android.mailcommon.presentation.compose.SmallNonClickableIcon
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.model.string
import ch.protonmail.android.maildetail.presentation.R
import ch.protonmail.android.maildetail.presentation.model.MessageDetailHeaderUiModel
import ch.protonmail.android.maildetail.presentation.model.ParticipantUiModel
import ch.protonmail.android.maildetail.presentation.previewdata.MessageDetailHeaderPreview
import ch.protonmail.android.maildetail.presentation.previewdata.MessageDetailHeaderPreviewProvider
import ch.protonmail.android.maillabel.presentation.model.LabelUiModel
import ch.protonmail.android.maillabel.presentation.ui.LabelsList
import kotlinx.collections.immutable.ImmutableList
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.caption
import me.proton.core.compose.theme.captionWeak
import me.proton.core.compose.theme.defaultSmall
import me.proton.core.compose.theme.defaultSmallStrong

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MessageDetailHeader(
    modifier: Modifier = Modifier,
    uiModel: MessageDetailHeaderUiModel,
    initiallyExpanded: Boolean = false,
    showFeatureMissingSnackbar: () -> Unit = { },
) {
    val isExpanded = rememberSaveable(inputs = arrayOf()) {
        mutableStateOf(initiallyExpanded)
    }

    AnimatedContent(
        targetState = isExpanded.value,
        transitionSpec = {
            fadeIn() with
                fadeOut() using
                SizeTransform()
        }
    ) { targetState ->
        MessageDetailHeaderLayout(
            modifier = modifier,
            uiModel = uiModel,
            isExpanded = targetState,
            onClick = { isExpanded.value = !isExpanded.value },
            showFeatureMissingSnackbar = showFeatureMissingSnackbar,
        )
    }
}

@Composable
private fun MessageDetailHeaderLayout(
    modifier: Modifier = Modifier,
    uiModel: MessageDetailHeaderUiModel,
    isExpanded: Boolean,
    onClick: () -> Unit,
    showFeatureMissingSnackbar: () -> Unit,
) {
    ConstraintLayout(
        modifier = modifier
            .fillMaxWidth()
            .background(ProtonTheme.colors.backgroundNorm)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = !isExpanded
            ) { onClick() }
            .padding(
                start = ProtonDimens.SmallSpacing + ProtonDimens.ExtraSmallSpacing,
                end = ProtonDimens.DefaultSpacing,
                top = ProtonDimens.SmallSpacing,
                bottom = ProtonDimens.DefaultSpacing
            )
            .testTag(MessageDetailHeaderTestTags.RootItem)
    ) {
        val (
            avatarRef,
            senderNameRef,
            senderAddressRef,
            iconsRef,
            timeRef,
            moreButtonRef,
            allRecipientsRef,
            toRecipientsTitleRef,
            toRecipientsRef,
            ccRecipientsTitleRef,
            ccRecipientsRef,
            bccRecipientsTitleRef,
            bccRecipientsRef,
            spacerRef,
            labelsRef,
            extendedTimeRef
        ) = createRefs()

        val (
            locationRef,
            sizeRef,
            hideDetailsRef
        ) = createRefs()

        Avatar(
            modifier = modifier.constrainAs(avatarRef) {
                top.linkTo(parent.top, margin = ProtonDimens.SmallSpacing)
                start.linkTo(parent.start)
            },
            avatarUiModel = uiModel.avatar
        )

        SenderName(
            modifier = modifier.constrainAs(senderNameRef) {
                width = Dimension.fillToConstraints
                top.linkTo(parent.top, margin = ProtonDimens.SmallSpacing)
                start.linkTo(avatarRef.end, margin = ProtonDimens.SmallSpacing)
                end.linkTo(iconsRef.start, margin = ProtonDimens.SmallSpacing)
            },
            participantUiModel = uiModel.sender
        )

        SenderAddress(
            modifier = modifier.constrainAs(senderAddressRef) {
                width = Dimension.fillToConstraints
                top.linkTo(senderNameRef.bottom, margin = ProtonDimens.ExtraSmallSpacing)
                start.linkTo(avatarRef.end, margin = ProtonDimens.SmallSpacing)
                end.linkTo(moreButtonRef.start, margin = ProtonDimens.SmallSpacing)
            },
            participantUiModel = uiModel.sender,
            isExpanded = isExpanded,
            showFeatureMissingSnackbar = showFeatureMissingSnackbar
        )

        Icons(
            modifier = modifier
                .testTag(MessageDetailHeaderTestTags.Icons)
                .constrainAs(iconsRef) {
                    top.linkTo(timeRef.top)
                    bottom.linkTo(timeRef.bottom)
                    end.linkTo(timeRef.start, margin = ProtonDimens.ExtraSmallSpacing)
                },
            uiModel = uiModel,
            isExpanded = isExpanded
        )

        Time(
            modifier = modifier.constrainAs(timeRef) {
                top.linkTo(parent.top, margin = ProtonDimens.SmallSpacing)
                end.linkTo(parent.end)
            },
            time = uiModel.time
        )

        MoreButton(
            modifier = modifier.constrainAs(moreButtonRef) {
                top.linkTo(timeRef.bottom, margin = ProtonDimens.ExtraSmallSpacing)
                end.linkTo(parent.end)
            },
            onClick = showFeatureMissingSnackbar
        )

        AllRecipients(
            modifier = modifier.constrainAs(allRecipientsRef) {
                width = Dimension.fillToConstraints
                top.linkTo(senderAddressRef.bottom, margin = ProtonDimens.ExtraSmallSpacing)
                start.linkTo(avatarRef.end, margin = ProtonDimens.SmallSpacing)
                end.linkTo(moreButtonRef.start, margin = ProtonDimens.SmallSpacing)
                visibility = visibleWhen(!isExpanded)
            },
            allRecipients = uiModel.allRecipients
        )

        RecipientsTitle(
            modifier = modifier
                .testTag(MessageDetailHeaderTestTags.ToRecipientsText)
                .constrainAs(toRecipientsTitleRef) {
                    constrainRecipientsTitle(
                        reference = toRecipientsRef,
                        recipients = uiModel.toRecipients,
                        isExpanded = isExpanded,
                        hasUndisclosedRecipients = uiModel.shouldShowUndisclosedRecipients
                    )
                },
            recipientsTitle = R.string.to
        )
        Recipients(
            modifier = modifier
                .testTag(MessageDetailHeaderTestTags.ToRecipientsList)
                .constrainAs(toRecipientsRef) {
                    constrainRecipients(
                        topReference = allRecipientsRef,
                        startReference = avatarRef,
                        endReference = moreButtonRef,
                        recipients = uiModel.toRecipients,
                        isExpanded = isExpanded,
                        hasUndisclosedRecipients = uiModel.shouldShowUndisclosedRecipients
                    )
                },
            recipients = uiModel.toRecipients,
            hasUndisclosedRecipients = uiModel.shouldShowUndisclosedRecipients,
            showFeatureMissingSnackbar = showFeatureMissingSnackbar
        )

        RecipientsTitle(
            modifier = modifier
                .testTag(MessageDetailHeaderTestTags.CcRecipientsText)
                .constrainAs(ccRecipientsTitleRef) {
                    constrainRecipientsTitle(
                        reference = ccRecipientsRef,
                        recipients = uiModel.ccRecipients,
                        isExpanded = isExpanded
                    )
                },
            recipientsTitle = R.string.cc
        )
        Recipients(
            modifier = modifier
                .testTag(MessageDetailHeaderTestTags.CcRecipientsList)
                .constrainAs(ccRecipientsRef) {
                    constrainRecipients(
                        topReference = toRecipientsRef,
                        startReference = avatarRef,
                        endReference = moreButtonRef,
                        recipients = uiModel.ccRecipients,
                        isExpanded = isExpanded
                    )
                },
            recipients = uiModel.ccRecipients,
            showFeatureMissingSnackbar = showFeatureMissingSnackbar
        )

        RecipientsTitle(
            modifier = modifier
                .testTag(MessageDetailHeaderTestTags.BccRecipientsText)
                .constrainAs(bccRecipientsTitleRef) {
                    constrainRecipientsTitle(
                        reference = bccRecipientsRef,
                        recipients = uiModel.bccRecipients,
                        isExpanded = isExpanded
                    )
                },
            recipientsTitle = R.string.bcc
        )
        Recipients(
            modifier = modifier
                .testTag(MessageDetailHeaderTestTags.BccRecipientsList)
                .constrainAs(bccRecipientsRef) {
                    constrainRecipients(
                        topReference = ccRecipientsRef,
                        startReference = avatarRef,
                        endReference = moreButtonRef,
                        recipients = uiModel.bccRecipients,
                        isExpanded = isExpanded
                    )
                },
            recipients = uiModel.bccRecipients,
            showFeatureMissingSnackbar = showFeatureMissingSnackbar
        )

        Spacer(
            modifier = modifier
                .constrainAs(spacerRef) {
                    top.linkTo(bccRecipientsRef.bottom)
                    visibility = visibleWhen(isExpanded)
                }
                .height(ProtonDimens.SmallSpacing)
        )

        Labels(
            modifier = modifier.constrainAs(labelsRef) {
                constrainExtendedHeaderRow(
                    topReference = spacerRef,
                    endReference = moreButtonRef,
                    isExpanded = isExpanded,
                    topMargin = if (isExpanded) ProtonDimens.SmallSpacing else ProtonDimens.ExtraSmallSpacing
                )
                visibility = visibleWhen(uiModel.labels.isNotEmpty())
            },
            uiModels = uiModel.labels,
            isExpanded = isExpanded
        )

        ExtendedHeaderRow(
            modifier = modifier
                .testTag(MessageDetailHeaderTestTags.ExtendedTimeRow)
                .constrainAs(extendedTimeRef) {
                    constrainExtendedHeaderRow(
                        topReference = labelsRef,
                        endReference = moreButtonRef,
                        isExpanded = isExpanded
                    )
                },
            icon = R.drawable.ic_proton_calendar_grid,
            text = uiModel.extendedTime.string()
        )

        ExtendedHeaderRow(
            modifier = modifier
                .testTag(MessageDetailHeaderTestTags.ExtendedFolderRow)
                .constrainAs(locationRef) {
                    constrainExtendedHeaderRow(
                        topReference = extendedTimeRef,
                        endReference = moreButtonRef,
                        isExpanded = isExpanded
                    )
                },
            icon = uiModel.location.icon,
            iconColor = uiModel.location.color,
            text = uiModel.location.name
        )

        ExtendedHeaderRow(
            modifier = modifier
                .testTag(MessageDetailHeaderTestTags.ExtendedSizeRow)
                .constrainAs(sizeRef) {
                    constrainExtendedHeaderRow(
                        topReference = locationRef,
                        endReference = moreButtonRef,
                        isExpanded = isExpanded
                    )
                },
            icon = R.drawable.ic_proton_filing_cabinet,
            text = uiModel.size
        )

        // Display it when the handling is implemented https://jira.protontech.ch/browse/MAILANDR-214
        //        ExtendedHeaderRow(
        //            modifier = modifier.constrainAs(trackerProtectionInfoRef) {
        //                constrainExtendedHeaderRow(
        //                    topReference = sizeRef,
        //                    endReference = moreButtonRef,
        //                    isExpanded = isExpanded
        //                )
        //            },
        //            icon = R.drawable.ic_proton_shield,
        //            text = "Placeholder text"
        //        )

        // Display it when the handling is implemented https://jira.protontech.ch/browse/MAILANDR-213
        //        ExtendedHeaderRow(
        //            modifier = modifier.constrainAs(encryptionInfoRef) {
        //                constrainExtendedHeaderRow(
        //                    topReference = trackerProtectionInfoRef,
        //                    endReference = moreButtonRef,
        //                    isExpanded = isExpanded
        //                )
        //            },
        //            icon = uiModel.encryptionPadlock,
        //            text = uiModel.encryptionInfo
        //        )

        HideDetails(
            modifier = modifier
                .constrainAs(hideDetailsRef) {
                    top.linkTo(
                        sizeRef.bottom,
                        margin = ProtonDimens.SmallSpacing,
                        goneMargin = ProtonDimens.SmallSpacing
                    )
                    start.linkTo(avatarRef.end, margin = ProtonDimens.SmallSpacing)
                    visibility = visibleWhen(isExpanded)
                }
                .clickable { onClick() }
        )
    }
}

@Composable
private fun SenderName(
    modifier: Modifier = Modifier,
    participantUiModel: ParticipantUiModel
) {
    Text(
        modifier = modifier.testTag(MessageDetailHeaderTestTags.SenderName),
        text = participantUiModel.participantName,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = ProtonTheme.typography.defaultSmallStrong
    )
}

@Composable
private fun SenderAddress(
    modifier: Modifier = Modifier,
    participantUiModel: ParticipantUiModel,
    isExpanded: Boolean,
    showFeatureMissingSnackbar: () -> Unit
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Display the padlock once the handling is implemented https://jira.protontech.ch/browse/MAILANDR-213
        // SmallNonClickableIcon(iconId = participantUiModel.participantPadlock)
        // Spacer(modifier = Modifier.width(ProtonDimens.ExtraSmallSpacing))
        ParticipantText(
            modifier = Modifier.testTag(MessageDetailHeaderTestTags.SenderAddress),
            text = participantUiModel.participantAddress,
            textColor = ProtonTheme.colors.interactionNorm,
            clickable = isExpanded,
            onClick = showFeatureMissingSnackbar
        )
    }
}

@Composable
private fun Icons(
    modifier: Modifier = Modifier,
    uiModel: MessageDetailHeaderUiModel,
    isExpanded: Boolean
) {
    Row(modifier = modifier) {
        // Display it when the handling is implemented https://jira.protontech.ch/browse/MAILANDR-214
        //        if (uiModel.shouldShowTrackerProtectionIcon && !isExpanded) {
        //            SmallNonClickableIcon(iconId = R.drawable.ic_proton_shield)
        //        }
        if (uiModel.shouldShowAttachmentIcon) {
            SmallNonClickableIcon(iconId = R.drawable.ic_proton_paper_clip)
        }
        if (uiModel.shouldShowStar) {
            SmallNonClickableIcon(iconId = R.drawable.ic_proton_star_filled, tintId = R.color.notification_warning)
        }
        if (!isExpanded) {
            if (uiModel.location.color != null) {
                SmallNonClickableIcon(iconId = uiModel.location.icon, iconColor = uiModel.location.color)
            } else {
                SmallNonClickableIcon(iconId = uiModel.location.icon)
            }
        }
    }
}

@Composable
private fun Time(
    modifier: Modifier = Modifier,
    time: TextUiModel
) {
    Text(
        modifier = modifier.testTag(MessageDetailHeaderTestTags.Time),
        text = time.string(),
        maxLines = 1,
        style = ProtonTheme.typography.captionWeak
    )
}

@Composable
private fun MoreButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Icon(
        modifier = modifier
            .testTag(MessageDetailHeaderTestTags.MoreButton)
            .clickable(
                onClickLabel = stringResource(id = R.string.more_button_content_description),
                role = Role.Button,
                onClick = onClick
            ),
        painter = painterResource(id = R.drawable.ic_proton_three_dots_horizontal),
        tint = ProtonTheme.colors.iconWeak,
        contentDescription = NO_CONTENT_DESCRIPTION
    )
}

@Composable
private fun AllRecipients(
    modifier: Modifier = Modifier,
    allRecipients: TextUiModel
) {
    Row(modifier = modifier) {
        Text(
            modifier = Modifier
                .testTag(MessageDetailHeaderTestTags.AllRecipientsText)
                .padding(end = ProtonDimens.ExtraSmallSpacing),
            text = stringResource(R.string.to),
            style = ProtonTheme.typography.caption
        )
        Text(
            modifier = Modifier.testTag(MessageDetailHeaderTestTags.AllRecipientsValue),
            text = allRecipients.string(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = ProtonTheme.typography.captionWeak
        )
    }
}

@Composable
private fun Recipients(
    modifier: Modifier = Modifier,
    recipients: List<ParticipantUiModel>,
    hasUndisclosedRecipients: Boolean = false,
    showFeatureMissingSnackbar: () -> Unit
) {
    Column(modifier = modifier) {
        if (hasUndisclosedRecipients) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ParticipantText(
                    text = stringResource(id = R.string.undisclosed_recipients),
                    clickable = false,
                    onClick = showFeatureMissingSnackbar
                )
            }
        }
        recipients.forEachIndexed { index, participant ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (participant.participantName.isNotEmpty()) {
                    ParticipantText(
                        text = participant.participantName,
                        clickable = false,
                        onClick = showFeatureMissingSnackbar
                    )
                    Spacer(modifier = Modifier.width(ProtonDimens.ExtraSmallSpacing))
                }
                // Display the padlock once the handling is implemented https://jira.protontech.ch/browse/MAILANDR-213
                // SmallNonClickableIcon(iconId = participant.participantPadlock)
                // Spacer(modifier = Modifier.width(ProtonDimens.ExtraSmallSpacing))
                ParticipantText(
                    modifier = Modifier.testTag(MessageDetailHeaderTestTags.ParticipantText),
                    text = participant.participantAddress,
                    textColor = ProtonTheme.colors.interactionNorm,
                    onClick = showFeatureMissingSnackbar
                )
            }
            if (index != recipients.size - 1) {
                Spacer(modifier = Modifier.height(ProtonDimens.SmallSpacing))
            }
        }
    }
}

@Composable
private fun RecipientsTitle(
    modifier: Modifier = Modifier,
    @StringRes recipientsTitle: Int
) {
    Text(modifier = modifier, text = stringResource(id = recipientsTitle), style = ProtonTheme.typography.caption)
}

@Composable
private fun ParticipantText(
    modifier: Modifier = Modifier,
    text: String,
    textColor: Color = ProtonTheme.colors.textNorm,
    clickable: Boolean = true,
    onClick: () -> Unit
) {
    val textModifier = modifier.apply {
        if (clickable) {
            clickable(onClickLabel = text, onClick = onClick)
        }
    }

    Text(
        text = text,
        modifier = textModifier,
        color = textColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = ProtonTheme.typography.caption
    )
}

@Composable
private fun Labels(
    modifier: Modifier,
    uiModels: ImmutableList<LabelUiModel>,
    isExpanded: Boolean
) {
    val iconAlpha = animateFloatAsState(if (isExpanded) 1f else 0f).value
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        SmallNonClickableIcon(
            modifier = Modifier
                .testTag(MessageDetailHeaderTestTags.LabelIcon)
                .alpha(iconAlpha)
                .padding(top = MailDimens.TinySpacing),
            iconId = R.drawable.ic_proton_tag
        )
        Spacer(modifier = Modifier.width(ProtonDimens.SmallSpacing))
        LabelsList(
            modifier = Modifier.testTag(MessageDetailHeaderTestTags.LabelsList),
            labels = uiModels,
            isExpanded = isExpanded
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
        if (iconColor != null) {
            SmallNonClickableIcon(iconId = icon, iconColor = iconColor)
        } else {
            SmallNonClickableIcon(iconId = icon)
        }
        Spacer(modifier = Modifier.width(ProtonDimens.SmallSpacing))
        Text(
            modifier = Modifier.testTag(MessageDetailHeaderTestTags.ExtendedHeaderText),
            text = text,
            style = ProtonTheme.typography.captionWeak
        )
    }
}

@Composable
private fun HideDetails(
    modifier: Modifier = Modifier
) {
    Text(
        modifier = modifier.testTag(MessageDetailHeaderTestTags.ExtendedHideDetails),
        text = stringResource(id = R.string.hide_details),
        color = ProtonTheme.colors.interactionNorm,
        style = ProtonTheme.typography.defaultSmall
    )
}

private fun ConstrainScope.constrainRecipientsTitle(
    reference: ConstrainedLayoutReference,
    recipients: List<ParticipantUiModel>,
    isExpanded: Boolean,
    hasUndisclosedRecipients: Boolean = false
) {
    top.linkTo(reference.top)
    end.linkTo(reference.start, margin = ProtonDimens.SmallSpacing)
    visibility = visibleWhen((recipients.isNotEmpty() || hasUndisclosedRecipients) && isExpanded)
}

private fun ConstrainScope.constrainRecipients(
    topReference: ConstrainedLayoutReference,
    startReference: ConstrainedLayoutReference,
    endReference: ConstrainedLayoutReference,
    recipients: List<ParticipantUiModel>,
    isExpanded: Boolean,
    hasUndisclosedRecipients: Boolean = false
) {
    width = Dimension.fillToConstraints
    top.linkTo(
        topReference.bottom,
        margin = ProtonDimens.SmallSpacing,
        goneMargin = ProtonDimens.SmallSpacing
    )
    start.linkTo(startReference.end, margin = ProtonDimens.SmallSpacing)
    end.linkTo(endReference.start, margin = ProtonDimens.SmallSpacing)
    visibility = visibleWhen((recipients.isNotEmpty() || hasUndisclosedRecipients) && isExpanded)
}

private fun ConstrainScope.constrainExtendedHeaderRow(
    topReference: ConstrainedLayoutReference,
    endReference: ConstrainedLayoutReference,
    isExpanded: Boolean,
    topMargin: Dp = ProtonDimens.SmallSpacing
) {
    width = Dimension.fillToConstraints
    top.linkTo(
        topReference.bottom,
        margin = topMargin,
        goneMargin = topMargin
    )
    start.linkTo(parent.start, margin = ProtonDimens.DefaultSpacing)
    end.linkTo(endReference.end)
    visibility = visibleWhen(isExpanded)
}

private fun visibleWhen(isVisible: Boolean) = if (isVisible) Visibility.Visible else Visibility.Gone

@Preview(showBackground = true)
@Composable
fun MessageDetailHeaderPreview(
    @PreviewParameter(MessageDetailHeaderPreviewProvider::class) preview: MessageDetailHeaderPreview
) {
    ProtonTheme {
        MessageDetailHeader(
            uiModel = preview.uiModel,
            initiallyExpanded = preview.initiallyExpanded,
        )
    }
}

object MessageDetailHeaderTestTags {

    const val RootItem = "MessageDetailHeaderRootItem"
    const val SenderName = "SenderName"
    const val Time = "Time"
    const val SenderAddress = "SenderAddress"
    const val Icons = "Icons"
    const val MoreButton = "MoreButton"
    const val AllRecipientsText = "AllRecipientsText"
    const val AllRecipientsValue = "AllRecipientsValue"
    const val ToRecipientsText = "ToRecipientsText"
    const val ToRecipientsList = "ToRecipientsList"
    const val CcRecipientsText = "CcRecipientsText"
    const val CcRecipientsList = "CcRecipientsList"
    const val BccRecipientsText = "BccRecipientsText"
    const val BccRecipientsList = "BccRecipientsList"
    const val ParticipantText = "ParticipantText"
    const val LabelIcon = "LabelIcon"
    const val LabelsList = "LabelsList"

    const val ExtendedTimeRow = "ExtendedTimeRow"
    const val ExtendedFolderRow = "ExtendedFolderRow"
    const val ExtendedSizeRow = "ExtendedSizeRow"
    const val ExtendedHeaderText = "ExtendedHeaderText"
    const val ExtendedHideDetails = "ExtendedHideDetails"
}
