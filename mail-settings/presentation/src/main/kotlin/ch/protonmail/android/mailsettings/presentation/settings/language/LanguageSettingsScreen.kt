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

package ch.protonmail.android.mailsettings.presentation.settings.language

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import ch.protonmail.android.mailsettings.domain.model.AppLanguage
import ch.protonmail.android.mailsettings.presentation.R
import ch.protonmail.android.mailsettings.presentation.R.string
import ch.protonmail.android.mailsettings.presentation.settings.language.LanguageSettingsState.Data
import ch.protonmail.android.mailsettings.presentation.settings.language.LanguageSettingsState.Loading
import me.proton.core.compose.component.ProtonSettingsRadioItem
import me.proton.core.compose.component.ProtonSettingsTopBar
import me.proton.core.compose.flow.rememberAsState

const val TEST_TAG_LANGUAGE_SETTINGS_SCREEN = "LanguageSettingsScreenTestTag"
const val TEST_TAG_LANG_SETTINGS_SCREEN_SCROLL_COL = "LanguageSettingsScreenColumnTestTag"

@Composable
fun LanguageSettingsScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    viewModel: LanguageSettingsViewModel = hiltViewModel()
) {
    when (
        val state = rememberAsState(
            flow = viewModel.state,
            initial = Loading
        ).value
    ) {
        is Data -> {
            val actions = LanguageSettingsScreen.Actions(
                onBackClick = onBackClick,
                onLanguageSelected = viewModel::onLanguageSelected,
                onSystemDefaultSelected = viewModel::onSystemDefaultSelected
            )
            LanguageSettingsScreen(
                modifier = modifier,
                state = state,
                actions = actions
            )
        }
        is Loading -> Unit
    }
}

@Composable
fun LanguageSettingsScreen(
    modifier: Modifier = Modifier,
    state: Data,
    actions: LanguageSettingsScreen.Actions
) {
    Scaffold(
        modifier = modifier.testTag(TEST_TAG_LANGUAGE_SETTINGS_SCREEN),
        topBar = {
            ProtonSettingsTopBar(
                title = stringResource(id = R.string.mail_settings_app_language),
                onBackClick = actions.onBackClick
            )
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .selectableGroup()
                    .padding(paddingValues)
                    .testTag(TEST_TAG_LANG_SETTINGS_SCREEN_SCROLL_COL)
                    .verticalScroll(rememberScrollState())
            ) {
                ProtonSettingsRadioItem(
                    name = stringResource(id = string.mail_settings_system_default),
                    isSelected = state.isSystemDefault,
                    onItemSelected = { actions.onSystemDefaultSelected() }
                )

                state.languages.forEach { language ->
                    ProtonSettingsRadioItem(
                        name = language.name,
                        isSelected = language.isSelected,
                        onItemSelected = { actions.onLanguageSelected(language.language) }
                    )
                }
            }
        }
    )
}

object LanguageSettingsScreen {

    data class Actions(
        val onBackClick: () -> Unit,
        val onLanguageSelected: (AppLanguage) -> Unit,
        val onSystemDefaultSelected: () -> Unit
    ) {

        companion object {

            val Empty = Actions(
                onBackClick = {},
                onLanguageSelected = {},
                onSystemDefaultSelected = {}
            )
        }
    }
}

@Composable
@Preview(name = "Theme settings screen")
private fun LanguageSettingsScreenPreview() {
    LanguageSettingsScreen(
        state = Data(
            isSystemDefault = true,
            languages = listOf()
        ),
        actions = LanguageSettingsScreen.Actions.Empty
    )
}
