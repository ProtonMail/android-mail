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

package ch.protonmail.android.uicomponents.chips.icons

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.vectorResource
import ch.protonmail.android.uicomponents.R
import ch.protonmail.android.uicomponents.chips.ChipsTestTags
import ch.protonmail.android.uicomponents.chips.item.ChipItem
import me.proton.core.compose.theme.ProtonTheme

@Composable
internal fun LeadingChipIcon(chipItem: ChipItem) {
    when (chipItem) {
        is ChipItem.Invalid -> Icon(
            imageVector = ImageVector.vectorResource(R.drawable.ic_proton_exclamation_circle),
            null,
            modifier = Modifier.testTag(ChipsTestTags.InputChipLeadingIcon),
            tint = ProtonTheme.colors.textInverted
        )

        else -> Unit
    }
}
