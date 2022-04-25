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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.mailsettings.domain.model.AppLanguage
import ch.protonmail.android.mailsettings.domain.repository.AppLanguageRepository
import ch.protonmail.android.mailsettings.presentation.settings.language.LanguageSettingsState.Loading
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.proton.core.compose.viewmodel.stopTimeoutMillis
import javax.inject.Inject

@HiltViewModel
class LanguageSettingsViewModel @Inject constructor(
    languageRepository: AppLanguageRepository
) : ViewModel() {

    val state: Flow<LanguageSettingsState> = languageRepository.observe()
        .mapLatest { selectedLang ->
            val languages = getAppLanguageUiModels(selectedLang).sortedBy { it.name }
            val isSystemDefault = selectedLang == null
            LanguageSettingsState.Data(isSystemDefault, languages)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(stopTimeoutMillis),
            Loading
        )

    fun onLanguageSelected(language: LanguageUiModel) = viewModelScope.launch {
    }

    fun onSystemDefaultSelected() = viewModelScope.launch {
    }

    private fun getAppLanguageUiModels(selectedAppLanguage: AppLanguage?) =
        AppLanguage.values().map {
            LanguageUiModel(
                language = it,
                isSelected = it == selectedAppLanguage,
                name = it.langName
            )
        }

}
