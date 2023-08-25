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

package ch.protonmail.android.mailcomposer.presentation.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import ch.protonmail.android.mailcommon.presentation.NO_CONTENT_DESCRIPTION
import ch.protonmail.android.mailcomposer.presentation.R
import me.proton.core.compose.component.ProtonRawListItem
import me.proton.core.compose.theme.ProtonDimens

@Composable
fun AddAttachmentsBottomSheetContent(onImportFromSelected: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(vertical = ProtonDimens.DefaultSpacing)) {
        ProtonRawListItem(
            modifier = Modifier
                .clickable { onImportFromSelected() }
                .height(ProtonDimens.ListItemHeight)
                .padding(horizontal = ProtonDimens.DefaultSpacing)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_proton_folder_open),
                contentDescription = NO_CONTENT_DESCRIPTION
            )
            Spacer(modifier = Modifier.width(ProtonDimens.DefaultSpacing))
            Text(
                modifier = Modifier.weight(1f),
                text = stringResource(id = R.string.add_attachments_bottom_sheet_import_from),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
