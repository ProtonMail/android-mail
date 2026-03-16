/*
 * Copyright (c) 2022 Proton Technologies AG
 * This file is part of Proton Technologies AG and Proton Mail.
 *
 * Proton Mail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation), either version 3 of the License), or
 * (at your option) any later version.
 *
 * Proton Mail is distributed in the hope that it will be useful),
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Mail. If not), see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.mailsettings.presentation.settings.language

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import ch.protonmail.android.mailcommon.presentation.Effect
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
import org.junit.Assert
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
    fun `state has ordered languages with Default selected when no language preference is saved`() = runTest {
        viewModel.state.test {
            // Given
            initialStateEmitted()

            // When
            languagePreferenceFlow.emit(null)

            // Then
            val expected = LanguageSettingsState.Data(
                SystemDefaultLanguage,
                appLanguagesSorted()
            )

            assertEquals(expected, awaitItem())
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
            val expected = LanguageSettingsState.Data(
                UserSelectedLanguage(AppLanguage.CATALAN),
                appLanguagesSorted()
            )

            assertEquals(expected, awaitItem())
        }
    }

    @Test
    fun `onLanguageSelected saves selected lang in repository`() = runTest {
        // Given
        justRun { languageRepository.save(AppLanguage.CATALAN) }

        // When
        viewModel.onLanguageSelected(UserSelectedLanguage(AppLanguage.CATALAN))

        // Then
        verify { languageRepository.save(AppLanguage.CATALAN) }
    }

    @Test
    fun `onDefaultSelected deletes previous language preference from repository`() = runTest {
        // Given
        justRun { languageRepository.clear() }

        // When
        viewModel.onLanguageSelected(SystemDefaultLanguage)

        // Then
        verify { languageRepository.clear() }
    }

    @Test
    fun `close effect emitted when theme is updated`() = runTest {
        // Given
        justRun { languageRepository.save(AppLanguage.CATALAN) }

        // When
        viewModel.onLanguageSelected(UserSelectedLanguage(AppLanguage.CATALAN))
        // then
        Assert.assertEquals(
            LanguageSettingsEffects(close = Effect.of(Unit)),
            viewModel.effects.value
        )
    }

    // Ordering is defined by L18N team
    private fun appLanguagesSorted() = listOf(
        SystemDefaultLanguage,
        UserSelectedLanguage(AppLanguage.ENGLISH),
        UserSelectedLanguage(AppLanguage.GERMAN),
        UserSelectedLanguage(AppLanguage.FRENCH),
        UserSelectedLanguage(AppLanguage.SPANISH),
        UserSelectedLanguage(AppLanguage.SPANISH_LATIN_AMERICA),
        UserSelectedLanguage(AppLanguage.ITALIAN),
        UserSelectedLanguage(AppLanguage.DUTCH),
        UserSelectedLanguage(AppLanguage.POLISH),
        UserSelectedLanguage(AppLanguage.PORTUGUESE_BRAZILIAN),
        UserSelectedLanguage(AppLanguage.RUSSIAN),
        UserSelectedLanguage(AppLanguage.KOREAN),
        UserSelectedLanguage(AppLanguage.JAPANESE),
        UserSelectedLanguage(AppLanguage.CATALAN),
        UserSelectedLanguage(AppLanguage.CZECH),
        UserSelectedLanguage(AppLanguage.DANISH),
        UserSelectedLanguage(AppLanguage.FINNISH),
        UserSelectedLanguage(AppLanguage.INDONESIAN),
        UserSelectedLanguage(AppLanguage.PORTUGUESE),
        UserSelectedLanguage(AppLanguage.ROMANIAN),
        UserSelectedLanguage(AppLanguage.SWEDISH),
        UserSelectedLanguage(AppLanguage.TURKISH),
        UserSelectedLanguage(AppLanguage.CHINESE_SIMPLIFIED),
        UserSelectedLanguage(AppLanguage.CHINESE_TRADITIONAL),
        UserSelectedLanguage(AppLanguage.HUNGARIAN),
        UserSelectedLanguage(AppLanguage.NORWEGIAN_BOKMAL),
        UserSelectedLanguage(AppLanguage.SLOVAK),
        UserSelectedLanguage(AppLanguage.SLOVENIAN),
        UserSelectedLanguage(AppLanguage.GREEK),
        UserSelectedLanguage(AppLanguage.BELARUSIAN),
        UserSelectedLanguage(AppLanguage.UKRAINIAN)
    )

    private suspend fun ReceiveTurbine<LanguageSettingsState>.initialStateEmitted() {
        awaitItem() as Loading
    }
}
