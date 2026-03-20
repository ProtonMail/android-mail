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

package ch.protonmail.android.mailmessage.presentation.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.design.compose.theme.titleSmallNorm
import ch.protonmail.android.mailattachments.domain.model.AttachmentId
import ch.protonmail.android.mailattachments.domain.model.AttachmentOpenMode
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import ch.protonmail.android.mailmessage.domain.model.AttachmentListExpandCollapseMode
import ch.protonmail.android.mailmessage.presentation.R
import ch.protonmail.android.mailmessage.presentation.extension.getTotalAttachmentByteSizeReadable
import ch.protonmail.android.mailmessage.presentation.model.attachment.AttachmentGroupUiModel
import ch.protonmail.android.mailmessage.presentation.sample.AttachmentMetadataUiModelSamples
import me.proton.core.presentation.R.drawable

@Composable
fun AttachmentList(
    modifier: Modifier = Modifier,
    messageAttachmentsUiModel: AttachmentGroupUiModel,
    actions: AttachmentList.Actions,
    downloadingAttachmentId: AttachmentId? = null
) {
    when (messageAttachmentsUiModel.expandCollapseMode) {

        AttachmentListExpandCollapseMode.NotApplicable -> DefaultAttachmentList(
            modifier = modifier,
            messageAttachmentsUiModel = messageAttachmentsUiModel,
            actions = actions,
            downloadingAttachmentId = downloadingAttachmentId
        )

        else -> ExpandableAttachmentList(
            modifier = modifier,
            messageAttachmentsUiModel = messageAttachmentsUiModel,
            actions = actions,
            downloadingAttachmentId = downloadingAttachmentId
        )
    }
}

@Composable
fun ExpandableAttachmentList(
    modifier: Modifier = Modifier,
    messageAttachmentsUiModel: AttachmentGroupUiModel,
    actions: AttachmentList.Actions,
    downloadingAttachmentId: AttachmentId? = null
) {
    Column(
        modifier = modifier
            .testTag(AttachmentListTestTags.Root)
            .fillMaxWidth()
            .padding(
                vertical = ProtonDimens.Spacing.ModeratelyLarge,
                horizontal = ProtonDimens.Spacing.Large
            ),
        verticalArrangement = Arrangement.spacedBy(ProtonDimens.Spacing.Standard)
    ) {

        AttachmentListHeader(
            messageAttachmentsUiModel = messageAttachmentsUiModel,
            onClick = {
                actions.onToggleExpandCollapseMode()
            }
        )
        AnimatedVisibility(
            visible = messageAttachmentsUiModel.expandCollapseMode == AttachmentListExpandCollapseMode.Expanded
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(ProtonDimens.Spacing.Standard)
            ) {
                messageAttachmentsUiModel.attachments.forEachIndexed { index, item ->
                    AttachmentItem(
                        modifier = Modifier.testTag("${AttachmentListTestTags.Item}$index"),
                        attachmentUiModel = item,
                        onAttachmentItemClicked = actions.onAttachmentClicked,
                        onAttachmentItemDeleteClicked = actions.onAttachmentDeleteClicked,
                        isDownloading = downloadingAttachmentId?.id == item.id.value
                    )
                }
            }
        }
    }
}

@Composable
fun DefaultAttachmentList(
    modifier: Modifier = Modifier,
    messageAttachmentsUiModel: AttachmentGroupUiModel,
    actions: AttachmentList.Actions,
    downloadingAttachmentId: AttachmentId? = null
) {
    Column(
        modifier = modifier
            .testTag(AttachmentListTestTags.Root)
            .fillMaxWidth()
            .padding(
                vertical = ProtonDimens.Spacing.ModeratelyLarge,
                horizontal = ProtonDimens.Spacing.Large
            ),
        verticalArrangement = Arrangement.spacedBy(ProtonDimens.Spacing.Standard)
    ) {

        messageAttachmentsUiModel.attachments.forEachIndexed { index, item ->
            AttachmentItem(
                modifier = Modifier.testTag("${AttachmentListTestTags.Item}$index"),
                attachmentUiModel = item,
                onAttachmentItemClicked = actions.onAttachmentClicked,
                onAttachmentItemDeleteClicked = actions.onAttachmentDeleteClicked,
                isDownloading = downloadingAttachmentId?.id == item.id.value
            )
        }
    }
}

@Composable
fun AttachmentListHeader(
    modifier: Modifier = Modifier,
    messageAttachmentsUiModel: AttachmentGroupUiModel,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = ProtonTheme.colors.interactionWeakNorm,
                shape = ProtonTheme.shapes.huge
            )
            .clip(ProtonTheme.shapes.huge)
            .clickable {
                onClick()
            }
            .padding(
                top = ProtonDimens.Spacing.MediumLight,
                bottom = ProtonDimens.Spacing.MediumLight,
                start = ProtonDimens.Spacing.Medium,
                end = ProtonDimens.Spacing.Large
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = Modifier
                    .size(ProtonDimens.IconSize.Medium)
                    .testTag(AttachmentListTestTags.PaperClipIcon),
                painter = painterResource(id = drawable.ic_proton_paper_clip),
                tint = ProtonTheme.colors.iconWeak,
                contentDescription = pluralStringResource(
                    R.plurals.attachment_count_label,
                    messageAttachmentsUiModel.attachments.size,
                    messageAttachmentsUiModel.attachments.size
                )
            )
            Spacer(modifier = Modifier.width(ProtonDimens.Spacing.Medium))
            Text(
                text = messageAttachmentsUiModel.attachments.size.toString(),
                style = ProtonTheme.typography.titleSmallNorm
            )
            Spacer(modifier = Modifier.width(ProtonDimens.Spacing.Small))
            Text(
                text = stringResource(id = R.string.attachments),
                style = ProtonTheme.typography.titleSmall.copy(color = ProtonTheme.colors.textWeak)
            )
        }

        Text(
            text = messageAttachmentsUiModel.attachments.getTotalAttachmentByteSizeReadable(LocalContext.current),
            style = ProtonTheme.typography.bodyMedium.copy(color = ProtonTheme.colors.textHint)
        )
        Spacer(modifier = Modifier.width(ProtonDimens.Spacing.MediumLight))

        val expandCollapseIconRes = if (
            messageAttachmentsUiModel.expandCollapseMode == AttachmentListExpandCollapseMode.Collapsed
        ) {
            R.drawable.ic_chevron_tiny_up
        } else {
            R.drawable.ic_chevron_tiny_down
        }

        Icon(
            modifier = Modifier
                .size(MailDimens.MessageDetailsHeader.CollapseExpandButtonSize),
            painter = painterResource(id = expandCollapseIconRes),
            contentDescription = null,
            tint = ProtonTheme.colors.iconWeak
        )
    }
}

object AttachmentList {
    data class Actions(
        val onShowAllAttachments: () -> Unit,
        val onAttachmentClicked: (mode: AttachmentOpenMode, attachmentId: AttachmentId) -> Unit,
        val onToggleExpandCollapseMode: () -> Unit = {},
        val onAttachmentDeleteClicked: (attachmentId: AttachmentId) -> Unit = {}
    ) {

        companion object {

            val Empty = Actions(
                onShowAllAttachments = {},
                onAttachmentClicked = { _, _ -> },
                onToggleExpandCollapseMode = {},
                onAttachmentDeleteClicked = {}
            )
        }
    }
}

@Composable
@Preview(showBackground = true)
@Suppress("MagicNumber")
fun AttachmentListMultiAttachmentsPreview() {
    ProtonTheme {
        AttachmentList(
            messageAttachmentsUiModel = AttachmentGroupUiModel(
                limit = 5,
                attachments = listOf(
                    AttachmentMetadataUiModelSamples.Invoice,
                    AttachmentMetadataUiModelSamples.Document
                )
            ),
            actions = AttachmentList.Actions.Empty
        )

    }
}

@Composable
@Preview(showBackground = true)
@Suppress("MagicNumber")
fun AttachmentListSingleAttachmentPreview() {
    ProtonTheme {
        AttachmentList(
            messageAttachmentsUiModel = AttachmentGroupUiModel(
                limit = 1,
                attachments = listOf(
                    AttachmentMetadataUiModelSamples.Invoice
                )
            ),
            actions = AttachmentList.Actions.Empty
        )

    }
}


@Composable
@Preview(showBackground = true)
@Suppress("MagicNumber")
fun AttachmentListExpandedPreview() {
    ProtonTheme {
        AttachmentList(
            messageAttachmentsUiModel = AttachmentGroupUiModel(
                attachments = listOf(
                    AttachmentMetadataUiModelSamples.Invoice,
                    AttachmentMetadataUiModelSamples.Document,
                    AttachmentMetadataUiModelSamples.Image,
                    AttachmentMetadataUiModelSamples.InvoiceWithBinaryContentType,
                    AttachmentMetadataUiModelSamples.DocumentWithMultipleDots
                ),
                expandCollapseMode = AttachmentListExpandCollapseMode.Expanded
            ),
            actions = AttachmentList.Actions.Empty
        )

    }
}


object AttachmentListTestTags {

    const val Root = "AttachmentsRootItem"
    const val PaperClipIcon = "AttachmentsPaperClipIcon"
    const val SummaryText = "AttachmentsSummaryText"
    const val SummarySize = "AttachmentsSummarySize"
    const val Item = "AttachmentItem"
}
