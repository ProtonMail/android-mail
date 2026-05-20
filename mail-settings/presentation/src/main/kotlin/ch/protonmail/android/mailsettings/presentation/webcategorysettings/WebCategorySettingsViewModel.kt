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

package ch.protonmail.android.mailsettings.presentation.webcategorysettings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.mailsettings.domain.usecase.HandleCloseWebSettings
import ch.protonmail.android.mailsettings.presentation.ObserveWebSettingsStateFlow
import ch.protonmail.android.mailsettings.presentation.websettings.WebSettingsState
import ch.protonmail.android.mailsettings.presentation.websettings.model.WebSettingsAction
import ch.protonmail.android.mailsettings.presentation.websettings.model.WebSettingsOperation
import ch.protonmail.android.mailsettings.presentation.websettings.toEmailCategoriesSettingsUrl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WebCategorySettingsViewModel @Inject constructor(
    observeWebSettings: ObserveWebSettingsStateFlow,
    private val handleCloseWebSettings: HandleCloseWebSettings
) : ViewModel() {

    val state: Flow<WebSettingsState> = observeWebSettings(viewModelScope) { sessionId, theme, config ->
        config.toEmailCategoriesSettingsUrl(sessionId, theme)
    }

    internal fun submit(action: WebSettingsOperation) {
        viewModelScope.launch {
            when (action) {
                is WebSettingsAction.OnCloseWebSettings -> handleCloseWebSettings()
            }
        }
    }
}

