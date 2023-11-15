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

package ch.protonmail.android.mailcommon.data.repository

import java.util.Locale
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class AppLocaleRepositoryImplTest {

    private lateinit var localeRepository: AppLocaleRepositoryImpl
    private lateinit var context: Context

    @Before
    fun setUp() {
        mockkConstructor(IntentFilter::class)
        every { anyConstructed<IntentFilter>().addAction(any()) } returns mockk()
        mockkStatic(AppCompatDelegate::class)
        context = mockk(relaxed = true)
        localeRepository = AppLocaleRepositoryImpl(context)
    }

    @After
    fun tearDown() {
        unmockkStatic(AppCompatDelegate::class)
    }

    @Test
    fun `when there is a saved preferred locale it is returned`() {
        // Given
        val savedLocale = Locale.ITALIAN
        every { AppCompatDelegate.getApplicationLocales() } returns LocaleListCompat.create(savedLocale)
        // When
        val actual = localeRepository.current()
        // Then
        assertEquals(savedLocale, actual)
    }

    @Test
    fun `when there are multiple saved preferred locales the first is returned`() {
        // Given
        val firstLocale = Locale.GERMAN
        val secondLocale = Locale.ITALIAN
        every { AppCompatDelegate.getApplicationLocales() } returns LocaleListCompat.create(firstLocale, secondLocale)
        // When
        val actual = localeRepository.current()
        // Then
        assertEquals(firstLocale, actual)
    }

    @Test
    fun `when there is no saved preferred locale the system default locale is returned`() {
        // Given
        val systemLocale = Locale.getDefault()
        every { AppCompatDelegate.getApplicationLocales() } returns LocaleListCompat.getEmptyLocaleList()
        // When
        val actual = localeRepository.current()
        // Then
        assertEquals(systemLocale, actual)
    }

    @Test
    fun `refresh() should refresh the cached locale`() {
        // Given
        val newLocale = Locale.CANADA
        every { AppCompatDelegate.getApplicationLocales() } returns LocaleListCompat.create(newLocale)

        // When
        localeRepository.refresh()

        // Then
        val actual = localeRepository.current()
        assertEquals(newLocale, actual)
    }

    @Test
    fun `onReceive() should refresh the cached locale`() {
        // Given
        val newLocale = Locale.UK
        every { AppCompatDelegate.getApplicationLocales() } returns LocaleListCompat.create(newLocale)

        // When
        localeRepository.onReceive(context, Intent())

        // Then
        // Ensure that the onReceive method correctly updates the cached locale
        val actual = localeRepository.current()
        assertEquals(newLocale, actual)
    }
}
