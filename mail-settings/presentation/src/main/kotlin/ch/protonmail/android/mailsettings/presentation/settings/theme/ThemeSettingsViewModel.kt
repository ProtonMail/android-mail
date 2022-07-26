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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.mailsettings.domain.model.Theme
import ch.protonmail.android.mailsettings.domain.model.Theme.DARK
import ch.protonmail.android.mailsettings.domain.model.Theme.LIGHT
import ch.protonmail.android.mailsettings.domain.model.Theme.SYSTEM_DEFAULT
import ch.protonmail.android.mailsettings.domain.repository.ThemeRepository
import ch.protonmail.android.mailsettings.presentation.R
import ch.protonmail.android.mailsettings.presentation.settings.theme.ThemeSettingsState.Loading
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.proton.core.compose.viewmodel.stopTimeoutMillis
import javax.inject.Inject

@HiltViewModel
class ThemeSettingsViewModel @Inject constructor(
    private val themeRepository: ThemeRepository
) : ViewModel() {

    val state: Flow<ThemeSettingsState> = themeRepository
        .observe()
        .mapLatest { currentTheme ->
            ThemeSettingsState.Data(
                Theme.values().map { theme ->
                    ThemeUiModel(
                        id = theme,
                        name = nameStringResourceBy(theme),
                        isSelected = currentTheme == theme
                    )
                }
            )
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(stopTimeoutMillis),
            Loading
        )

    fun onThemeSelected(theme: Theme) = viewModelScope.launch {
        themeRepository.update(theme)
    }

    private fun nameStringResourceBy(theme: Theme) = when (theme) {
        SYSTEM_DEFAULT -> R.string.mail_settings_system_default
        LIGHT -> R.string.mail_settings_theme_light
        DARK -> R.string.mail_settings_theme_dark
    }
}
