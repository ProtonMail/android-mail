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

package ch.protonmail.android.mailsettings.presentation.settings.customizetoolbar.ui

import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.model.string
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultSmallStrongUnspecified

@Composable
internal fun ToolbarTypeTabs(
    selectedTabIdx: Int,
    tabs: List<TextUiModel>,
    modifier: Modifier = Modifier,
    onSelected: (Int) -> Unit
) {
    TabRow(
        containerColor = ProtonTheme.colors.backgroundNorm,
        modifier = modifier,
        selectedTabIndex = selectedTabIdx,
        contentColor = ProtonTheme.colors.textNorm,
        indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
                color = ProtonTheme.colors.brandNorm,
                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIdx])
            )
        }
    ) {
        tabs.forEachIndexed { index, titleModel ->
            Tab(
                selected = selectedTabIdx == index,
                text = {
                    Text(
                        text = titleModel.string(),
                        style = ProtonTheme.typography.defaultSmallStrongUnspecified,
                        color = ProtonTheme.colors.textNorm
                    )
                },
                onClick = {
                    onSelected(index)
                }
            )
        }
    }
}
