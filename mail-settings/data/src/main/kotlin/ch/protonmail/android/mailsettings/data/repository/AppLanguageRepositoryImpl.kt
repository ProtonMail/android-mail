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

package ch.protonmail.android.mailsettings.data.repository

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import ch.protonmail.android.mailcommon.domain.repository.AppLocaleRepository
import ch.protonmail.android.mailsettings.domain.model.AppLanguage
import ch.protonmail.android.mailsettings.domain.repository.AppLanguageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import javax.inject.Inject

class AppLanguageRepositoryImpl @Inject constructor(private val appLocaleRepository: AppLocaleRepository) :
    AppLanguageRepository {

    private val languagePreferenceFlow = MutableSharedFlow<AppLanguage?>(replay = 1)

    override fun observe(): Flow<AppLanguage?> {
        val savedAppLocales = AppCompatDelegate.getApplicationLocales()
        val languageTag = savedAppLocales[0]?.toLanguageTag()

        languagePreferenceFlow.tryEmit(AppLanguage.fromTag(languageTag))

        return languagePreferenceFlow
    }

    override fun save(language: AppLanguage) {
        val locales = LocaleListCompat.forLanguageTags(language.langTag)
        AppCompatDelegate.setApplicationLocales(locales)
        languagePreferenceFlow.tryEmit(language)

        appLocaleRepository.refresh()
    }

    override fun clear() {
        val emptyLocales = LocaleListCompat.getEmptyLocaleList()
        AppCompatDelegate.setApplicationLocales(emptyLocales)
        languagePreferenceFlow.tryEmit(null)

        appLocaleRepository.refresh()
    }

}
