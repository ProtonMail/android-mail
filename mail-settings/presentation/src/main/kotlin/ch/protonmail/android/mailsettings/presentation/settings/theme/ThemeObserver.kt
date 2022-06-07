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

import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
import ch.protonmail.android.mailsettings.domain.model.Theme
import ch.protonmail.android.mailsettings.domain.model.Theme.DARK
import ch.protonmail.android.mailsettings.domain.model.Theme.LIGHT
import ch.protonmail.android.mailsettings.domain.model.Theme.SYSTEM_DEFAULT
import ch.protonmail.android.mailsettings.domain.repository.ThemeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ThemeObserver @Inject constructor(
    private val themeRepository: ThemeRepository,
    @ThemeObserverCoroutineScope
    private val coroutineScope: CoroutineScope
) {

    fun start() {
        themeRepository.observe()
            .onEach { applyDefaultTheme(it) }
            .launchIn(coroutineScope)
    }

    private suspend fun applyDefaultTheme(theme: Theme) = when (theme) {
        SYSTEM_DEFAULT -> setNightModeFollowSystem()
        DARK -> setNightModeEnabled()
        LIGHT -> setNightModeDisabled()
    }

    private suspend fun setNightModeFollowSystem() = withContext(Dispatchers.Main) {
        AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_FOLLOW_SYSTEM)
    }

    private suspend fun setNightModeEnabled() = withContext(Dispatchers.Main) {
        AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_YES)
    }

    private suspend fun setNightModeDisabled() = withContext(Dispatchers.Main) {
        AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_NO)
    }
}
