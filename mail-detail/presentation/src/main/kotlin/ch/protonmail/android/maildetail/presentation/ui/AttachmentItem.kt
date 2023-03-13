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

import android.text.format.Formatter
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import ch.protonmail.android.maildetail.presentation.model.AttachmentUiModel
import ch.protonmail.android.maildetail.presentation.sample.AttachmentUiModelSample
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.captionHint
import me.proton.core.compose.theme.defaultSmall
import me.proton.core.presentation.R.drawable

@Composable
fun AttachmentItem(attachmentUiModel: AttachmentUiModel) {
    Row(
        modifier = Modifier
            .padding(horizontal = ProtonDimens.SmallSpacing, vertical = ProtonDimens.ExtraSmallSpacing)
            .padding(horizontal = ProtonDimens.ExtraSmallSpacing)
            .border(
                width = MailDimens.DefaultBorder,
                color = ProtonTheme.colors.interactionWeakNorm,
                shape = ProtonTheme.shapes.large
            )
            .padding(ProtonDimens.SmallSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = drawable.ic_proton_file_pdf_24),
            contentDescription = null,
        )
        Spacer(modifier = Modifier.width(ProtonDimens.SmallSpacing))
        Text(
            modifier = Modifier.weight(1f, fill = false),
            style = ProtonTheme.typography.defaultSmall,
            text = attachmentUiModel.fileName,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            modifier = Modifier.padding(end = ProtonDimens.SmallSpacing),
            text = ".${attachmentUiModel.extension}",
            style = ProtonTheme.typography.defaultSmall
        )
        Text(
            text = Formatter.formatShortFileSize(LocalContext.current, attachmentUiModel.size),
            style = ProtonTheme.typography.captionHint
        )
    }
}

@Composable
@Preview(showBackground = true)
fun AttachmentItemPreview() {
    ProtonTheme {
        AttachmentItem(AttachmentUiModelSample.invoice)
    }
}

@Composable
@Preview(showBackground = true)
fun AttachmentItemTruncationPreview() {
    ProtonTheme {
        AttachmentItem(AttachmentUiModelSample.documentWithReallyLongFileName)
    }
}