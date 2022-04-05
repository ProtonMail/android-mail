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

package ch.protonmail.android.mailsettings.presentation.settings.theme

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import ch.protonmail.android.mailsettings.presentation.R
import me.proton.core.compose.component.ProtonSettingsRadioItem
import me.proton.core.compose.component.ProtonSettingsTopBar
import timber.log.Timber

const val TEST_TAG_THEME_SETTINGS_SCREEN = "ThemeSettingsScreenTestTag"

@Composable
fun ThemeSettingsScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
) {
    Scaffold(
        modifier = modifier.testTag(TEST_TAG_THEME_SETTINGS_SCREEN),
        topBar = {
            ProtonSettingsTopBar(
                title = stringResource(id = R.string.mail_settings_theme),
                onBackClick = onBackClick
            )
        },
        content = {
            val themeOptions = listOf(
                stringResource(id = R.string.mail_settings_system_default),
                stringResource(id = R.string.mail_settings_theme_light),
                stringResource(id = R.string.mail_settings_theme_dark)
            )
            Column {
                themeOptions.forEach { option ->
                    ProtonSettingsRadioItem(
                        name = option,
                        isSelected = option == "System default",
                        onItemSelected = { Timber.d("Theme selected: $it") }
                    )
                }
            }
        }
    )
}

@Preview(name = "Theme settings screen")
@Composable
fun previewConversationModeSettingsScreen() {
    ThemeSettingsScreen(
        onBackClick = {}
    )
}
