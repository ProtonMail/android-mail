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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import ch.protonmail.android.mailcommon.presentation.NO_CONTENT_DESCRIPTION
import ch.protonmail.android.mailcomposer.presentation.R
import me.proton.core.compose.component.ProtonRawListItem
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultStrongNorm
import me.proton.core.compose.theme.defaultStrongUnspecified

@Composable
fun SetExpirationTimeBottomSheetContent(onDoneClick: () -> Unit) {

    val selectedItem = rememberSaveable { mutableStateOf(ExpirationTime.None) }

    Row(
        modifier = Modifier
            .padding(ProtonDimens.DefaultSpacing)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stringResource(id = R.string.composer_expiration_time_bottom_sheet_title),
            style = ProtonTheme.typography.defaultStrongNorm
        )
        Text(
            modifier = Modifier.clickable(role = Role.Button) { onDoneClick() },
            text = stringResource(id = R.string.composer_expiration_time_bottom_sheet_done),
            style = ProtonTheme.typography.defaultStrongUnspecified,
            color = ProtonTheme.colors.interactionNorm
        )
    }

    Divider()

    LazyColumn {
        items(items = ExpirationTime.values()) { item ->
            ProtonRawListItem(
                modifier = Modifier
                    .selectable(selected = item == selectedItem.value) { selectedItem.value = item }
                    .height(ProtonDimens.ListItemHeight)
                    .padding(horizontal = ProtonDimens.DefaultSpacing)
            ) {
                val textRes = when (item) {
                    ExpirationTime.None -> R.string.composer_bottom_bar_expiration_time_none
                    ExpirationTime.OneHour -> R.string.composer_bottom_bar_expiration_time_one_hour
                    ExpirationTime.OneDay -> R.string.composer_bottom_bar_expiration_time_one_day
                    ExpirationTime.ThreeDays -> R.string.composer_bottom_bar_expiration_time_three_days
                    ExpirationTime.OneWeek -> R.string.composer_bottom_bar_expiration_time_one_week
                }
                Text(
                    modifier = Modifier.weight(1f),
                    text = stringResource(id = textRes),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (item == selectedItem.value) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_proton_checkmark),
                        contentDescription = NO_CONTENT_DESCRIPTION,
                        tint = ProtonTheme.colors.interactionNorm
                    )
                }
            }
        }
    }
}

enum class ExpirationTime { None, OneHour, OneDay, ThreeDays, OneWeek }
