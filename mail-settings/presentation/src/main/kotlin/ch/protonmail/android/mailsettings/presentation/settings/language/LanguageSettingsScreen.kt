/*
 * Copyright (c) 2021 Proton Technologies AG
 * This file is part of Proton Technologies AG and ProtonMail.
 *
 * ProtonMail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonMail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonMail.  If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.mailsettings.presentation.settings.language

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
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
            LanguageSettingsScreen(
                modifier = modifier,
                onBackClick = onBackClick,
                onLanguageSelected = viewModel::onLanguageSelected,
                onSystemDefaultSelected = viewModel::onSystemDefaultSelected,
                state = state
            )
        }
        is Loading -> Unit
    }
}

@Composable
fun LanguageSettingsScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    onLanguageSelected: (AppLanguage) -> Unit,
    onSystemDefaultSelected: () -> Unit,
    state: Data
) {
    Scaffold(
        modifier = modifier.testTag(TEST_TAG_LANGUAGE_SETTINGS_SCREEN),
        topBar = {
            ProtonSettingsTopBar(
                title = stringResource(id = R.string.mail_settings_app_language),
                onBackClick = onBackClick
            )
        },
        content = {
            Column(
                modifier = Modifier
                    .testTag(TEST_TAG_LANG_SETTINGS_SCREEN_SCROLL_COL)
                    .verticalScroll(rememberScrollState())
            ) {
                ProtonSettingsRadioItem(
                    name = stringResource(id = string.mail_settings_system_default),
                    isSelected = state.isSystemDefault,
                    onItemSelected = { onSystemDefaultSelected() }
                )

                state.languages.forEach { language ->
                    ProtonSettingsRadioItem(
                        name = language.name,
                        isSelected = language.isSelected,
                        onItemSelected = { onLanguageSelected(language.language) }
                    )
                }
            }
        }
    )
}

@Preview(name = "Theme settings screen")
@Composable
fun previewThemeSettingsScreen() {
    LanguageSettingsScreen(
        onBackClick = {},
        onLanguageSelected = {},
        onSystemDefaultSelected = {},
        state = Data(
            isSystemDefault = true,
            languages = listOf()
        )
    )
}

fun NavGraphBuilder.addLanguageSettings(navController: NavHostController, route: String) =
    composable(
        route = route
    ) {
        LanguageSettingsScreen(
            modifier = Modifier,
            onBackClick = { navController.popBackStack() }
        )
    }
