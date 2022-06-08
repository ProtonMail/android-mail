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

package ch.protonmail.android.mailsettings.presentation.settings.theme

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import ch.protonmail.android.mailsettings.domain.model.Theme
import ch.protonmail.android.mailsettings.presentation.R
import ch.protonmail.android.mailsettings.presentation.settings.theme.ThemeSettingsState.Data
import ch.protonmail.android.mailsettings.presentation.settings.theme.ThemeSettingsState.Loading
import me.proton.core.compose.component.ProtonSettingsRadioItem
import me.proton.core.compose.component.ProtonSettingsTopBar
import me.proton.core.compose.flow.rememberAsState

const val TEST_TAG_THEME_SETTINGS_SCREEN = "ThemeSettingsScreenTestTag"

@Composable
fun ThemeSettingsScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    viewModel: ThemeSettingsViewModel = hiltViewModel()
) {
    when (
        val state = rememberAsState(
            flow = viewModel.state,
            initial = Loading
        ).value
    ) {
        is Data -> {
            ThemeSettingsScreen(
                modifier = modifier,
                onBackClick = onBackClick,
                onThemeSelected = viewModel::onThemeSelected,
                state = state
            )
        }
        is Loading -> Unit
    }
}

@Composable
fun ThemeSettingsScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    onThemeSelected: (Theme) -> Unit,
    state: Data
) {
    Scaffold(
        modifier = modifier.testTag(TEST_TAG_THEME_SETTINGS_SCREEN),
        topBar = {
            ProtonSettingsTopBar(
                title = stringResource(id = R.string.mail_settings_theme),
                onBackClick = onBackClick
            )
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .selectableGroup()
                    .padding(paddingValues)
            ) {
                state.themes.forEach { theme ->
                    ProtonSettingsRadioItem(
                        name = stringResource(id = theme.name),
                        isSelected = theme.isSelected,
                        onItemSelected = { onThemeSelected(theme.id) }
                    )
                }
            }
        }
    )
}

@Preview(name = "Theme settings screen")
@Composable
fun previewThemeSettingsScreen() {
    ThemeSettingsScreen(
        onBackClick = {},
        onThemeSelected = {},
        state = Data(
            listOf(
                ThemeUiModel(
                    Theme.SYSTEM_DEFAULT,
                    R.string.mail_settings_system_default,
                    true
                ),
                ThemeUiModel(
                    Theme.LIGHT,
                    R.string.mail_settings_theme_light,
                    false
                ),
                ThemeUiModel(
                    Theme.DARK,
                    R.string.mail_settings_theme_dark,
                    false
                )
            )
        )
    )
}
