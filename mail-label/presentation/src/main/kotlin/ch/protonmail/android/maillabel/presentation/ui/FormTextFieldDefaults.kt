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

package ch.protonmail.android.maillabel.presentation.ui

import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import me.proton.core.compose.theme.ProtonTheme

@Composable
internal fun formTextFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedTextColor = ProtonTheme.colors.textNorm,
    focusedContainerColor = ProtonTheme.colors.backgroundSecondary,
    unfocusedContainerColor = ProtonTheme.colors.backgroundSecondary,
    focusedLabelColor = ProtonTheme.colors.textNorm,
    unfocusedLabelColor = ProtonTheme.colors.textNorm,
    disabledLabelColor = ProtonTheme.colors.textDisabled,
    errorLabelColor = ProtonTheme.colors.notificationError,
    unfocusedBorderColor = Color.Transparent,
    errorBorderColor = ProtonTheme.colors.notificationError,
    focusedBorderColor = ProtonTheme.colors.brandNorm
)
