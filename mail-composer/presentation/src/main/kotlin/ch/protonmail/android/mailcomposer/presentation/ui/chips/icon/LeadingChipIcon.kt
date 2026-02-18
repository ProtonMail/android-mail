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

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.mailcomposer.presentation.ui.chips.item.ChipItem
import ch.protonmail.android.mailpadlocks.presentation.model.EncryptionInfoUiModel
import ch.protonmail.android.uicomponents.R

@Composable
internal fun LeadingChipIcon(chipItem: ChipItem) {
    val iconState = chipItem.toIconState()

    Crossfade(
        targetState = iconState,
        animationSpec = tween(durationMillis = 200),
        label = "ChipIconCrossfade"
    ) { state ->
        Box(
            contentAlignment = Alignment.Center
        ) {
            when (state) {
                is ChipIconState.Invalid -> Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_proton_exclamation_circle),
                    contentDescription = null,
                    modifier = Modifier.size(ProtonDimens.IconSize.Small),
                    tint = ProtonTheme.colors.notificationError
                )

                is ChipIconState.Lock -> Icon(
                    imageVector = ImageVector.vectorResource(state.icon),
                    contentDescription = null,
                    modifier = Modifier.size(ProtonDimens.IconSize.Small),
                    tint = colorResource(state.color)
                )

                ChipIconState.Loading -> CircularProgressIndicator(
                    modifier = Modifier.size(ProtonDimens.IconSize.Small),
                    strokeWidth = 2.dp,
                    color = ProtonTheme.colors.iconWeak
                )

                is ChipIconState.Group -> Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_proton_users),
                    contentDescription = null,
                    modifier = Modifier.size(ProtonDimens.IconSize.Small),
                    tint = runCatching { Color(state.colorHex.toColorInt()) }
                        .getOrDefault(ProtonTheme.colors.iconWeak)
                )

                ChipIconState.None -> Unit
            }
        }
    }
}

private fun ChipItem.toIconState(): ChipIconState = when (this) {
    is ChipItem.Invalid -> ChipIconState.Invalid
    is ChipItem.Validating -> ChipIconState.Loading
    is ChipItem.Valid -> when (val info = encryptionInfo) {
        is EncryptionInfoUiModel.WithLock -> ChipIconState.Lock(info.icon, info.color)
        EncryptionInfoUiModel.NoLock -> ChipIconState.None
    }

    is ChipItem.Counter -> ChipIconState.None
    is ChipItem.Group -> ChipIconState.Group(colorHex = this.color)
}

private sealed interface ChipIconState {
    data object Invalid : ChipIconState
    data object Loading : ChipIconState
    data class Lock(val icon: Int, val color: Int) : ChipIconState
    data class Group(val colorHex: String) : ChipIconState
    data object None : ChipIconState
}
