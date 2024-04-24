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

package ch.protonmail.android.mailcontact.presentation.contactlist.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ch.protonmail.android.mailcontact.presentation.model.ContactListItemUiModel
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultSmallStrongUnspecified

@Composable
internal fun ContactListHeaderItem(modifier: Modifier = Modifier, header: ContactListItemUiModel.Header) {
    Text(
        text = header.value,
        modifier = modifier.padding(
            start = ProtonDimens.DefaultSpacing,
            end = ProtonDimens.DefaultSpacing,
            top = ProtonDimens.MediumSpacing,
            bottom = ProtonDimens.SmallSpacing
        ),
        style = ProtonTheme.typography.defaultSmallStrongUnspecified,
        color = ProtonTheme.colors.brandNorm
    )
    Divider()
}
