/*
 * Copyright (c) 2025 Proton Technologies AG
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

package ch.protonmail.android.debug

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ch.protonmail.android.R
import ch.protonmail.android.design.compose.component.appbar.ProtonMediumTopAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ApplicationDebugScreen(
    modifier: Modifier = Modifier,
    actions: ApplicationDebugScreen.Actions
) {

    Scaffold(
        modifier = modifier,
        topBar = {
            ProtonMediumTopAppBar(
                title = { Text(text = stringResource(R.string.app_debug_screen)) },
                navigationIcon = {
                    IconButton(onClick = actions.onBackClick) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(id = R.string.presentation_back)
                        )
                    }
                }
            )
        },
        content = { paddingValues ->
            ApplicationDebugScreenList(
                modifier = Modifier.padding(paddingValues),
                actions = actions
            )
        }
    )
}
