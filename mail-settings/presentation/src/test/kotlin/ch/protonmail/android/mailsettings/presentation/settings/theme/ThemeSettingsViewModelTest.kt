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

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import ch.protonmail.android.mailsettings.domain.model.Theme
import ch.protonmail.android.mailsettings.domain.model.Theme.DARK
import ch.protonmail.android.mailsettings.domain.model.Theme.LIGHT
import ch.protonmail.android.mailsettings.domain.model.Theme.SYSTEM_DEFAULT
import ch.protonmail.android.mailsettings.domain.repository.ThemeRepository
import ch.protonmail.android.mailsettings.presentation.R.string
import ch.protonmail.android.mailsettings.presentation.settings.theme.ThemeSettingsState.Loading
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ThemeSettingsViewModelTest {

    private val themePreferenceFlow = MutableSharedFlow<Theme>()
    private val themeRepository = mockk<ThemeRepository> {
        coEvery {
            this@mockk.observe()
        } returns themePreferenceFlow
    }

    private lateinit var viewModel: ThemeSettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())

        viewModel = ThemeSettingsViewModel(
            themeRepository
        )
    }

    @Test
    fun `state returns themes with Default selected when saved theme preference is SYSTEM_DEFAULT`() = runTest {
        viewModel.state.test {
            // Given
            initialStateEmitted()

            // When
            themePreferenceFlow.emit(SYSTEM_DEFAULT)

            // Then
            val expectedThemes = listOf(
                systemDefaultTheme(true),
                lightTheme(),
                darkTheme()
            )
            assertEquals(ThemeSettingsState.Data(expectedThemes), awaitItem())
        }
    }

    @Test
    fun `state returns themes with Light selected when saved theme preference is LIGHT`() = runTest {
        viewModel.state.test {
            // Given
            initialStateEmitted()

            // When
            themePreferenceFlow.emit(LIGHT)

            // Then
            val expectedThemes = listOf(
                systemDefaultTheme(),
                lightTheme(true),
                darkTheme()
            )
            assertEquals(ThemeSettingsState.Data(expectedThemes), awaitItem())
        }
    }

    @Test
    fun `state returns themes with Dark selected when saved theme preference is DARK`() = runTest {
        viewModel.state.test {
            // Given
            initialStateEmitted()

            // When
            themePreferenceFlow.emit(DARK)

            // Then
            val expectedThemes = listOf(
                systemDefaultTheme(),
                lightTheme(),
                darkTheme(true)
            )
            assertEquals(ThemeSettingsState.Data(expectedThemes), awaitItem())
        }
    }

    @Test
    fun `state is updated when repository emits an updated theme preference`() = runTest {
        viewModel.state.test {
            // Given
            initialStateEmitted()
            // When
            themePreferenceFlow.emit(DARK)
            // Then
            assertEquals(
                ThemeSettingsState.Data(
                    listOf(
                        systemDefaultTheme(),
                        lightTheme(),
                        darkTheme(true)
                    )
                ),
                awaitItem()
            )

            // When
            themePreferenceFlow.emit(LIGHT)
            // Then
            assertEquals(
                ThemeSettingsState.Data(
                    listOf(
                        systemDefaultTheme(),
                        lightTheme(true),
                        darkTheme()
                    )
                ),
                awaitItem()
            )
        }
    }

    @Test
    fun `updates theme on repository when themeSelected`() = runTest {
        // Given
        coEvery { themeRepository.update(any()) } just Runs

        // When
        viewModel.onThemeSelected(Theme.LIGHT)

        // Then
        coVerify { themeRepository.update(Theme.LIGHT) }
    }

    private fun systemDefaultTheme(isCurrent: Boolean = false) = ThemeUiModel(
        SYSTEM_DEFAULT,
        string.mail_settings_system_default,
        isCurrent
    )

    private fun lightTheme(isCurrent: Boolean = false) = ThemeUiModel(
        LIGHT,
        string.mail_settings_theme_light,
        isCurrent
    )

    private fun darkTheme(isCurrent: Boolean = false) = ThemeUiModel(
        DARK,
        string.mail_settings_theme_dark,
        isCurrent
    )

    private suspend fun ReceiveTurbine<ThemeSettingsState>.initialStateEmitted() {
        awaitItem() as Loading
    }
}
