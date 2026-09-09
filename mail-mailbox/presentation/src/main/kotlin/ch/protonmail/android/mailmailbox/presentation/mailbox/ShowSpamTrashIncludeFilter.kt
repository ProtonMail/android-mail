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

package ch.protonmail.android.mailmailbox.presentation.mailbox

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring.DampingRatioLowBouncy
import androidx.compose.animation.core.Spring.StiffnessHigh
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import ch.protonmail.android.design.compose.modifier.drawHalo
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.design.compose.theme.labelLargeNorm
import ch.protonmail.android.mailmailbox.presentation.R
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.ShowSpamTrashIncludeFilterState
import ch.protonmail.android.mailcommon.presentation.R as CommonR

@Composable
fun ShowSpamTrashIncludeFilter(
    modifier: Modifier = Modifier,
    state: ShowSpamTrashIncludeFilterState,
    onFilterEnabled: () -> Unit,
    onFilterDisabled: () -> Unit
) {
    when (state) {
        is ShowSpamTrashIncludeFilterState.Loading,
        is ShowSpamTrashIncludeFilterState.Data.Hidden -> Unit

        is ShowSpamTrashIncludeFilterState.Data.Shown -> {
            FilterChip(
                modifier = modifier
                    .drawHalo(
                        color = ProtonTheme.colors.shadowWeak,
                        blurRadiusPx = with(LocalDensity.current) { 12.dp.toPx() },
                        shape = ProtonTheme.shapes.huge
                    )
                    .animateContentSize(
                        animationSpec = spring(
                            stiffness = StiffnessHigh,
                            dampingRatio = DampingRatioLowBouncy,
                            visibilityThreshold = IntSize.VisibilityThreshold
                        )
                    ),
                colors = chipColors().copy(
                    containerColor = ProtonTheme.colors.backgroundInvertedSecondary,
                    selectedContainerColor = ProtonTheme.colors.interactionBrandDefaultPressed
                ),
                shape = ProtonTheme.shapes.huge,
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = state.enabled,
                    borderWidth = ProtonDimens.OutlinedBorderSize,
                    borderColor = ProtonTheme.colors.borderNorm
                ),
                selected = state.enabled,
                onClick = {
                    if (state.enabled) {
                        onFilterDisabled()
                    } else {
                        onFilterEnabled()
                    }
                },
                trailingIcon = addCloseIconForEnabledState(state),
                elevation = null,
                leadingIcon = {},
                label = {
                    Text(
                        stringResource(R.string.include_spam_trash_text),
                        fontWeight = FontWeight.Medium,
                        style = if (state.enabled) {
                            ProtonTheme.typography.labelLargeNorm.copy(
                                color = ProtonTheme.colors.textInverted
                            )
                        } else {
                            ProtonTheme.typography.labelLargeNorm.copy(
                                color = ProtonTheme.colors.textWeak
                            )
                        }
                    )
                }
            )
        }
    }
}

@Composable
private fun addCloseIconForEnabledState(state: ShowSpamTrashIncludeFilterState.Data.Shown): @Composable (() -> Unit)? {
    return if (state.enabled) {
        {
            Icon(
                modifier = Modifier.size(ProtonDimens.IconSize.Small),
                painter = painterResource(CommonR.drawable.ic_material_close),
                contentDescription = null,
                tint = ProtonTheme.colors.textInverted
            )
        }
    } else {
        null
    }
}

@Composable
private fun chipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = ProtonTheme.colors.iconAccent,
    selectedLabelColor = ProtonTheme.colors.textInverted,
    containerColor = ProtonTheme.colors.backgroundSecondary,
    labelColor = ProtonTheme.colors.textAccent
)

@Preview(showBackground = true)
@Composable
fun InactiveShowSpamTrashFilterButtonPreview() {
    ShowSpamTrashIncludeFilter(
        state = ShowSpamTrashIncludeFilterState.Data.Shown(false),
        onFilterEnabled = {},
        onFilterDisabled = {}
    )
}

@Preview(showBackground = true)
@Composable
fun ActiveShowSpamTrashFilterPreview() {
    ShowSpamTrashIncludeFilter(
        state = ShowSpamTrashIncludeFilterState.Data.Shown(true),
        onFilterEnabled = {},
        onFilterDisabled = {}
    )
}
