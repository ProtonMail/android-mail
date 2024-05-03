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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ExposedDropdownMenuBox
import androidx.compose.material.ExposedDropdownMenuDefaults
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import ch.protonmail.android.mailcommon.presentation.NO_CONTENT_DESCRIPTION
import ch.protonmail.android.mailcomposer.presentation.R
import ch.protonmail.android.mailcomposer.presentation.ui.SetExpirationTimeBottomSheetContent.MaxDays
import ch.protonmail.android.mailcomposer.presentation.ui.SetExpirationTimeBottomSheetContent.MaxHours
import me.proton.core.compose.component.ProtonOutlinedTextField
import me.proton.core.compose.component.ProtonRawListItem
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultSmallStrongNorm
import me.proton.core.compose.theme.defaultStrongNorm
import me.proton.core.compose.theme.defaultStrongUnspecified
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

@Composable
fun SetExpirationTimeBottomSheetContent(expirationTime: Duration, onDoneClick: (Duration) -> Unit) {

    val selectedItem = remember { mutableStateOf(ExpirationTime.from(expirationTime)) }
    val selectedCustomDays = remember {
        mutableStateOf(
            if (selectedItem.value == ExpirationTime.Custom) {
                expirationTime.inWholeDays
            } else 0
        )
    }
    val selectedCustomHours = remember {
        mutableStateOf(
            if (selectedItem.value == ExpirationTime.Custom) {
                expirationTime.inWholeHours - expirationTime.inWholeDays.days.inWholeHours
            } else 0
        )
    }

    Column {
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
                modifier = Modifier.clickable(role = Role.Button) {
                    val valueToBeSaved = if (selectedItem.value == ExpirationTime.Custom) {
                        (selectedCustomDays.value * 24 + selectedCustomHours.value).hours
                    } else selectedItem.value.duration
                    onDoneClick(valueToBeSaved)
                },
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
                        ExpirationTime.Custom -> R.string.composer_bottom_bar_expiration_time_custom
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
            if (selectedItem.value == ExpirationTime.Custom) {
                item {
                    Row(
                        modifier = Modifier.padding(
                            horizontal = ProtonDimens.DefaultSpacing,
                            vertical = ProtonDimens.SmallSpacing
                        )
                    ) {
                        ExpirationTimeDropdownMenu(
                            modifier = Modifier.weight(1f),
                            initialValue = TextFieldValue(selectedCustomDays.value.toString()),
                            type = ExpirationTimeDropdownMenuType.Days,
                            onSelectionChanged = { selectedCustomDays.value = it.toLong() }
                        )
                        Spacer(modifier = Modifier.width(ProtonDimens.DefaultSpacing))
                        ExpirationTimeDropdownMenu(
                            modifier = Modifier.weight(1f),
                            initialValue = TextFieldValue(selectedCustomHours.value.toString()),
                            type = ExpirationTimeDropdownMenuType.Hours,
                            onSelectionChanged = { selectedCustomHours.value = it.toLong() }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun ExpirationTimeDropdownMenu(
    initialValue: TextFieldValue,
    type: ExpirationTimeDropdownMenuType,
    modifier: Modifier = Modifier,
    onSelectionChanged: (Int) -> Unit
) {
    val expanded = remember { mutableStateOf(false) }

    val label = type.name
    val maxValue = if (type == ExpirationTimeDropdownMenuType.Days) MaxDays else MaxHours

    Column(modifier = modifier) {
        Text(
            text = label,
            style = ProtonTheme.typography.defaultSmallStrongNorm
        )
        Spacer(modifier = Modifier.height(ProtonDimens.SmallSpacing))
        ExposedDropdownMenuBox(
            expanded = expanded.value,
            onExpandedChange = { expanded.value = !expanded.value }
        ) {
            ProtonOutlinedTextField(
                value = initialValue,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded.value) }
            )
            ExposedDropdownMenu(
                expanded = expanded.value,
                onDismissRequest = { expanded.value = false }
            ) {
                for (value in 0..maxValue) {
                    DropdownMenuItem(
                        text = { Text(text = value.toString()) },
                        onClick = {
                            onSelectionChanged(value)
                            expanded.value = false
                        }
                    )
                }
            }
        }
    }
}

object SetExpirationTimeBottomSheetContent {

    const val MaxDays = 28
    const val MaxHours = 23
}

enum class ExpirationTimeDropdownMenuType { Days, Hours }

enum class ExpirationTime(val duration: Duration) {
    None(Duration.ZERO),
    OneHour(1.hours),
    OneDay(1.days),
    ThreeDays(3.days),
    OneWeek(7.days),
    Custom(Duration.INFINITE);

    companion object {

        fun from(duration: Duration) = values().find { it.duration == duration } ?: Custom
    }
}
