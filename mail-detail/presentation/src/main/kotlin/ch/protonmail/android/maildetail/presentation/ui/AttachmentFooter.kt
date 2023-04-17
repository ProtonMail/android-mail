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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import ch.protonmail.android.mailcommon.presentation.ui.MailDivider
import ch.protonmail.android.maildetail.presentation.R
import ch.protonmail.android.maildetail.presentation.R.plurals
import ch.protonmail.android.maildetail.presentation.extensions.getTotalAttachmentByteSizeReadable
import ch.protonmail.android.maildetail.presentation.model.MessageBodyAttachmentsUiModel
import ch.protonmail.android.maildetail.presentation.sample.AttachmentUiModelSample
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultSmall
import me.proton.core.compose.theme.defaultSmallStrong
import me.proton.core.presentation.R.drawable

@Composable
fun AttachmentFooter(
    modifier: Modifier = Modifier,
    messageBodyAttachmentsUiModel: MessageBodyAttachmentsUiModel,
    onShowAllAttachments: () -> Unit
) {
    val attachments = messageBodyAttachmentsUiModel.attachments
    Column(modifier = modifier.fillMaxWidth()) {
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
                modifier = Modifier.size(ProtonDimens.SmallIconSize),
                painter = painterResource(id = drawable.ic_proton_paper_clip),
                tint = ProtonTheme.colors.iconWeak,
                contentDescription = ""
            )
            Spacer(modifier = Modifier.width(ProtonDimens.ExtraSmallSpacing))
            Text(
                text = pluralStringResource(plurals.attachment_count_label, attachments.size, attachments.size),
                style = ProtonTheme.typography.defaultSmall
            )
            Spacer(modifier = Modifier.width(ProtonDimens.ExtraSmallSpacing))
            Text(
                text = attachments.getTotalAttachmentByteSizeReadable(LocalContext.current),
                style = ProtonTheme.typography.defaultSmall.copy(color = ProtonTheme.colors.textHint)
            )
        }
        attachments.take(messageBodyAttachmentsUiModel.limit).forEach {
            AttachmentItem(attachmentUiModel = it)
        }
        if (attachments.size > messageBodyAttachmentsUiModel.limit) {
            Box(
                modifier = Modifier
                    .padding(ProtonDimens.ExtraSmallSpacing)
                    .padding(horizontal = ProtonDimens.SmallSpacing)
            ) {
                Text(
                    modifier = Modifier
                        .clickable { onShowAllAttachments() }
                        .padding(ProtonDimens.SmallSpacing),
                    text = stringResource(
                        id = R.string.attachment_show_more_label,
                        attachments.size - messageBodyAttachmentsUiModel.limit
                    ),
                    style = ProtonTheme.typography.defaultSmallStrong.copy(color = ProtonTheme.colors.brandNorm)
                )
            }
        }
        Spacer(modifier = Modifier.height(ProtonDimens.SmallSpacing))
    }
}

@Composable
@Preview(showBackground = true)
@Suppress("MagicNumber")
fun AttachmentFooterMultiAttachmentsPreview() {
    AttachmentFooter(
        messageBodyAttachmentsUiModel = MessageBodyAttachmentsUiModel(
            limit = 1,
            attachments = listOf(
                AttachmentUiModelSample.invoice,
                AttachmentUiModelSample.document
            )
        ),
        onShowAllAttachments = {}
    )
}

@Composable
@Preview(showBackground = true)
@Suppress("MagicNumber")
fun AttachmentFooterSingleAttachmentPreview() {
    AttachmentFooter(
        messageBodyAttachmentsUiModel = MessageBodyAttachmentsUiModel(
            limit = 1,
            attachments = listOf(
                AttachmentUiModelSample.invoice
            )
        ),
        onShowAllAttachments = {}
    )
}
