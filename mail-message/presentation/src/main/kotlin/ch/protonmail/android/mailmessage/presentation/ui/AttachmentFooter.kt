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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import ch.protonmail.android.mailcommon.presentation.ui.MailDivider
import ch.protonmail.android.mailmessage.domain.model.AttachmentId
import ch.protonmail.android.mailmessage.presentation.R
import ch.protonmail.android.mailmessage.presentation.extension.getTotalAttachmentByteSizeReadable
import ch.protonmail.android.mailmessage.presentation.model.AttachmentGroupUiModel
import ch.protonmail.android.mailmessage.presentation.sample.AttachmentUiModelSample
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultSmall
import me.proton.core.compose.theme.defaultSmallStrongUnspecified
import me.proton.core.presentation.R.drawable

@Composable
fun AttachmentFooter(
    modifier: Modifier = Modifier,
    messageBodyAttachmentsUiModel: AttachmentGroupUiModel,
    actions: AttachmentFooter.Actions
) {
    val attachments = messageBodyAttachmentsUiModel.attachments
    Column(
        modifier = modifier
            .testTag(AttachmentFooterTestTags.Root)
            .fillMaxWidth()
    ) {
        MailDivider()
        Row(
            modifier = modifier
                .padding(ProtonDimens.SmallSpacing)
                .padding(
                    start = ProtonDimens.ExtraSmallSpacing,
                    top = ProtonDimens.ExtraSmallSpacing,
                    end = ProtonDimens.ExtraSmallSpacing
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = Modifier
                    .testTag(AttachmentFooterTestTags.PaperClipIcon)
                    .size(ProtonDimens.SmallIconSize),
                painter = painterResource(id = drawable.ic_proton_paper_clip),
                tint = ProtonTheme.colors.iconWeak,
                contentDescription = ""
            )
            Spacer(modifier = Modifier.width(ProtonDimens.ExtraSmallSpacing))
            Text(
                modifier = Modifier.testTag(AttachmentFooterTestTags.SummaryText),
                text = pluralStringResource(R.plurals.attachment_count_label, attachments.size, attachments.size),
                style = ProtonTheme.typography.defaultSmall
            )
            Spacer(modifier = Modifier.width(ProtonDimens.ExtraSmallSpacing))
            Text(
                modifier = Modifier.testTag(AttachmentFooterTestTags.SummarySize),
                text = attachments.getTotalAttachmentByteSizeReadable(LocalContext.current),
                style = ProtonTheme.typography.defaultSmall.copy(color = ProtonTheme.colors.textHint)
            )
        }
        attachments.take(messageBodyAttachmentsUiModel.limit).forEachIndexed { index, item ->
            AttachmentItem(
                modifier = Modifier.testTag("${AttachmentFooterTestTags.Item}$index"),
                attachmentUiModel = item,
                onAttachmentItemClicked = actions.onAttachmentClicked,
                onAttachmentItemDeleteClicked = actions.onAttachmentDeleteClicked
            )
        }
        if (attachments.size > messageBodyAttachmentsUiModel.limit) {
            Box(
                modifier = Modifier
                    .padding(ProtonDimens.ExtraSmallSpacing)
                    .padding(horizontal = ProtonDimens.SmallSpacing)
            ) {
                Text(
                    modifier = Modifier
                        .testTag(AttachmentFooterTestTags.ShowMoreItems)
                        .clickable { actions.onShowAllAttachments() }
                        .padding(ProtonDimens.SmallSpacing),
                    text = stringResource(
                        id = R.string.attachment_show_more_label,
                        attachments.size - messageBodyAttachmentsUiModel.limit
                    ),
                    style = ProtonTheme.typography.defaultSmallStrongUnspecified,
                    color = ProtonTheme.colors.textAccent
                )
            }
        }
        Spacer(modifier = Modifier.height(ProtonDimens.SmallSpacing))
    }
}

object AttachmentFooter {
    data class Actions(
        val onShowAllAttachments: () -> Unit,
        val onAttachmentClicked: (attachmentId: AttachmentId) -> Unit,
        val onAttachmentDeleteClicked: (attachmentId: AttachmentId) -> Unit = {}
    ) {

        companion object {

            val Empty = Actions(
                onShowAllAttachments = {},
                onAttachmentClicked = {},
                onAttachmentDeleteClicked = {}
            )
        }
    }
}

@Composable
@Preview(showBackground = true)
@Suppress("MagicNumber")
fun AttachmentFooterMultiAttachmentsPreview() {
    AttachmentFooter(
        messageBodyAttachmentsUiModel = AttachmentGroupUiModel(
            limit = 1,
            attachments = listOf(
                AttachmentUiModelSample.invoice,
                AttachmentUiModelSample.document
            )
        ),
        actions = AttachmentFooter.Actions.Empty
    )
}

@Composable
@Preview(showBackground = true)
@Suppress("MagicNumber")
fun AttachmentFooterSingleAttachmentPreview() {
    AttachmentFooter(
        messageBodyAttachmentsUiModel = AttachmentGroupUiModel(
            limit = 1,
            attachments = listOf(
                AttachmentUiModelSample.invoice
            )
        ),
        actions = AttachmentFooter.Actions.Empty
    )
}

object AttachmentFooterTestTags {

    const val Root = "AttachmentsRootItem"
    const val PaperClipIcon = "AttachmentsPaperClipIcon"
    const val SummaryText = "AttachmentsSummaryText"
    const val SummarySize = "AttachmentsSummarySize"
    const val Item = "AttachmentItem"
    const val ShowMoreItems = "AttachmentsShowMoreItems"
}
