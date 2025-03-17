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

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import ch.protonmail.android.mailsettings.domain.model.AppLanguage
import ch.protonmail.android.mailsettings.domain.repository.AppLanguageRepository
import ch.protonmail.android.mailsettings.presentation.settings.language.LanguageSettingsState.Loading
import io.mockk.coEvery
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class LanguageSettingsViewModelTest {

    private val languagePreferenceFlow = MutableSharedFlow<AppLanguage?>()
    private val languageRepository = mockk<AppLanguageRepository> {
        coEvery { this@mockk.observe() } returns languagePreferenceFlow
    }

    private lateinit var viewModel: LanguageSettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())

        viewModel = LanguageSettingsViewModel(
            languageRepository
        )
    }

    @Test
    fun `state has alphabetically ordered languages with Default selected when no language preference is saved`() =
        runTest {
            viewModel.state.test {
                // Given
                initialStateEmitted()

                // When
                languagePreferenceFlow.emit(null)

                // Then
                val expected = allAppLanguagesWithSelected(null)
                assertEquals(LanguageSettingsState.Data(true, expected), awaitItem())
            }
        }

    @Test
    fun `state has languages list with preferred language selected when a preference is saved`() = runTest {
        viewModel.state.test {
            // Given
            initialStateEmitted()

            // When
            languagePreferenceFlow.emit(AppLanguage.CATALAN)

            // Then
            val expected = allAppLanguagesWithSelected(AppLanguage.CATALAN)
            assertEquals(LanguageSettingsState.Data(false, expected), awaitItem())
        }
    }

    @Test
    fun `onLanguageSelected saves selected lang in repository`() = runTest {
        // Given
        justRun { languageRepository.save(AppLanguage.DANISH) }

        // When
        viewModel.onLanguageSelected(AppLanguage.DANISH)

        // Then
        verify { languageRepository.save(AppLanguage.DANISH) }
    }

    @Test
    fun `onDefaultSelected deletes previous language preference from repository`() = runTest {
        // Given
        justRun { languageRepository.clear() }

        // When
        viewModel.onSystemDefaultSelected()

        // Then
        verify { languageRepository.clear() }
    }

    private fun allAppLanguagesWithSelected(selectedLang: AppLanguage?) = appLanguages().map { language ->
        LanguageUiModel(
            language = language,
            isSelected = language == selectedLang,
            name = language.langName
        )
    }

    private fun appLanguages() = listOf(
        AppLanguage.ENGLISH,
        AppLanguage.GERMAN,
        AppLanguage.FRENCH,
        AppLanguage.DUTCH,
        AppLanguage.SPANISH,
        AppLanguage.SPANISH_LATIN_AMERICA,
        AppLanguage.ITALIAN,
        AppLanguage.POLISH,
        AppLanguage.PORTUGUESE_BRAZILIAN,
        AppLanguage.RUSSIAN,
        AppLanguage.TURKISH,
        AppLanguage.CATALAN,
        AppLanguage.CZECH,
        AppLanguage.DANISH,
        AppLanguage.FINNISH,
        AppLanguage.CROATIAN,
        AppLanguage.HUNGARIAN,
        AppLanguage.INDONESIAN,
        AppLanguage.KABYLE,
        AppLanguage.NORWEGIAN_BOKMAL,
        AppLanguage.PORTUGUESE,
        AppLanguage.ROMANIAN,
        AppLanguage.SLOVAK,
        AppLanguage.SLOVENIAN,
        AppLanguage.SWEDISH,
        AppLanguage.GREEK,
        AppLanguage.BELARUSIAN,
        AppLanguage.UKRAINIAN,
        AppLanguage.GEORGIAN,
        AppLanguage.HINDI,
        AppLanguage.KOREAN,
        AppLanguage.JAPANESE,
        AppLanguage.CHINESE_SIMPLIFIED,
        AppLanguage.CHINESE_TRADITIONAL
    )

    private suspend fun ReceiveTurbine<LanguageSettingsState>.initialStateEmitted() {
        awaitItem() as Loading
    }
}
