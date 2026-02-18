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

package ch.protonmail.android.mailcomposer.presentation.ui.chips.item

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ChipColors
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.design.compose.theme.bodyMediumNorm

@Composable
internal fun ChipItem.textStyle(): TextStyle = when (this) {
    is ChipItem.Invalid -> ProtonTheme.typography.bodyMediumNorm.copy(
        color = ProtonTheme.colors.notificationError
    )

    else -> ProtonTheme.typography.bodyMediumNorm
}

@Composable
internal fun ChipItem.suggestionsTextStyle() = ProtonTheme.typography.bodyMediumNorm

@Composable
internal fun assistChipColor(): ChipColors {
    return AssistChipDefaults.assistChipColors()
        .copy(containerColor = ProtonTheme.colors.backgroundNorm)
}

@Composable
internal fun inputChipBorder(chipItem: ChipItem): BorderStroke {
    val borderColor = when (chipItem) {
        is ChipItem.Invalid -> ProtonTheme.colors.notificationError
        is ChipItem.Counter,
        is ChipItem.Group,
        is ChipItem.Validating,
        is ChipItem.Valid -> ProtonTheme.colors.borderStrong
    }

    return BorderStroke(
        width = ProtonDimens.OutlinedBorderSize,
        color = borderColor
    )
}

@Composable
internal fun suggestionChipColor(): ChipColors {

    return SuggestionChipDefaults.suggestionChipColors().copy(
        containerColor = ProtonTheme.colors.backgroundNorm
    )
}
