/*
 * Copyright (c) 2026 Proton Technologies AG
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

package ch.protonmail.android.mailcomposer.presentation.ui.chips.icon

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.mailcomposer.presentation.ui.chips.ChipsTestTags
import ch.protonmail.android.mailcomposer.presentation.ui.chips.item.ChipItem

@Composable
internal fun TrailingChipIcon(chipItem: ChipItem) {
    val tint = when (chipItem) {
        is ChipItem.Invalid -> ProtonTheme.colors.textInverted
        is ChipItem.Counter,
        is ChipItem.Group,
        is ChipItem.Validating,
        is ChipItem.Valid -> ProtonTheme.colors.textNorm
    }

    Icon(
        Icons.Default.Clear,
        modifier = Modifier
            .testTag(ChipsTestTags.InputChipTrailingIcon)
            .size(16.dp),
        tint = tint,
        contentDescription = ""
    )
}
