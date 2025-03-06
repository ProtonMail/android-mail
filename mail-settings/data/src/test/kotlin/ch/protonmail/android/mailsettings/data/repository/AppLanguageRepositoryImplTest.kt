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
import app.cash.turbine.test
import ch.protonmail.android.mailsettings.domain.model.AppLanguage
import ch.protonmail.android.mailsettings.domain.model.AppLanguage.PORTUGUESE_BRAZILIAN
import ch.protonmail.android.mailsettings.domain.model.AppLanguage.CHINESE_TRADITIONAL
import ch.protonmail.android.mailsettings.domain.model.AppLanguage.FRENCH
import ch.protonmail.android.mailsettings.domain.repository.AppLanguageRepository
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.util.Locale
import ch.protonmail.android.mailcommon.domain.repository.AppLocaleRepository

class AppLanguageRepositoryImplTest {

    private lateinit var languageRepository: AppLanguageRepository
    private lateinit var appLocaleRepository: AppLocaleRepository

    @Before
    fun setUp() {
        mockkStatic(AppCompatDelegate::class)
        every { AppCompatDelegate.setApplicationLocales(any()) } just Runs
        appLocaleRepository = mockAppLocaleRepository()
        languageRepository = AppLanguageRepositoryImpl(appLocaleRepository)
    }

    @After
    fun tearDown() {
        unmockkStatic(AppCompatDelegate::class)
        // LocaleListCompat is statically mocked by some specific tests.
        // Unmocked here to ensure this happens independently from those tests' result
        unmockkStatic(LocaleListCompat::class)
    }

    @Test
    fun `emits null when there is no saved preference`() = runTest {
        // When
        languageRepository.observe().test {
            // Then
            assertNull(awaitItem())
        }
    }

    @Test
    fun `setApplicationLocales on AppCompatDelegate when a new preference is saved`() = runTest {
        // When
        languageRepository.save(FRENCH)
        // Then
        val expected = LocaleListCompat.create(Locale.FRENCH)
        verify { AppCompatDelegate.setApplicationLocales(expected) }
        verify { appLocaleRepository.refresh() }
    }

    @Test
    fun `setApplicationLocales on AppCompatDelegate when a new preference is saved for language with subtag`() =
        runTest {
            // When
            languageRepository.save(CHINESE_TRADITIONAL)
            // Then
            val expected = LocaleListCompat.create(Locale.TRADITIONAL_CHINESE)
            verify { AppCompatDelegate.setApplicationLocales(expected) }
            verify { appLocaleRepository.refresh() }
        }

    @Test
    fun `emits new appLanguage when a new preference is saved`() = runTest {
        languageRepository.observe().test {
            // Given (initial state is null, no preference saved)
            assertNull(awaitItem())
            // When
            languageRepository.save(PORTUGUESE_BRAZILIAN)
            // Then
            assertEquals(PORTUGUESE_BRAZILIAN, awaitItem())
            verify { appLocaleRepository.refresh() }
        }
    }

    @Test
    fun `clearAppLanguage calls AppCompatDelegate with an empty locale list`() = runTest {
        // Given
        mockkStatic(LocaleListCompat::class)
        val emptyLocaleList = mockk<LocaleListCompat>()
        every { LocaleListCompat.getEmptyLocaleList() } returns emptyLocaleList

        // When
        languageRepository.clear()
        // Then
        // IMPORTANT!! The "empty locale" to remove the saved preference
        // needs to be generated using `LocaleListCompat.getEmptyLocaleList()`
        // As generating it otherwise (ie. `LocaleCompat.create()`) **causes the app
        // to enter a restart loop** !!
        verify { LocaleListCompat.getEmptyLocaleList() }
        verify { AppCompatDelegate.setApplicationLocales(emptyLocaleList) }
        verify { appLocaleRepository.refresh() }
    }

    @Test
    fun `emits null appLanguage when language preference is cleared`() = runTest {
        // Given (initial state is GERMAN)
        every { AppCompatDelegate.getApplicationLocales() } returns LocaleListCompat.create(Locale.GERMAN)

        languageRepository.observe().test {
            // Given (initial state emitted is GERMAN)
            assertEquals(AppLanguage.GERMAN, awaitItem())
            // When
            languageRepository.clear()
            // Then
            assertNull(awaitItem())
            verify { appLocaleRepository.refresh() }
        }
    }

    private fun mockAppLocaleRepository(): AppLocaleRepository {
        mockkStatic(AppLocaleRepository::refresh)
        return mockk {
            every { refresh() } just Runs
        }
    }
}
