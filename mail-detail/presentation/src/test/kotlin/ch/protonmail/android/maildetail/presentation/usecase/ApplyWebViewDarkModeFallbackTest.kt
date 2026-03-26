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

package ch.protonmail.android.maildetail.presentation.usecase

import ch.protonmail.android.mailmessage.domain.model.MessageBodyTransformations
import ch.protonmail.android.mailmessage.domain.model.MessageTheme
import ch.protonmail.android.mailmessage.domain.model.MessageThemeOptions
import ch.protonmail.android.mailmessage.domain.usecase.IsWebViewMediaQuerySupported
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplyWebViewDarkModeFallbackTest {

    private val isWebViewMediaQuerySupported = mockk<IsWebViewMediaQuerySupported>()
    private val isDarkModeEnabled = mockk<IsDarkModeEnabled>()

    private lateinit var applyWebViewDarkModeFallback: ApplyWebViewDarkModeFallback

    @Before
    fun setUp() {
        applyWebViewDarkModeFallback = ApplyWebViewDarkModeFallback(
            isWebViewMediaQuerySupported = isWebViewMediaQuerySupported,
            isDarkModeEnabled = isDarkModeEnabled
        )
    }

    @Test
    fun `returns original transformations when theme override is Light`() {
        // Given
        val transformations = MessageBodyTransformations.MessageDetailsDefaults.copy(
            messageThemeOptions = MessageThemeOptions(
                currentTheme = MessageTheme.Dark,
                themeOverride = MessageTheme.Light,
                supportsDarkModeViaMediaQuery = true
            )
        )

        // When
        val result = applyWebViewDarkModeFallback(transformations)

        // Then
        assertEquals(transformations, result)
        verify(exactly = 0) { isDarkModeEnabled() }
        verify(exactly = 0) { isWebViewMediaQuerySupported() }
    }

    @Test
    fun `returns original transformations when dark mode is not enabled`() {
        // Given
        val transformations = MessageBodyTransformations.MessageDetailsDefaults.copy(
            messageThemeOptions = MessageThemeOptions(
                currentTheme = MessageTheme.Dark,
                themeOverride = null,
                supportsDarkModeViaMediaQuery = true
            )
        )
        every { isDarkModeEnabled() } returns false

        // When
        val result = applyWebViewDarkModeFallback(transformations)

        // Then
        assertEquals(transformations, result)
        verify(exactly = 1) { isDarkModeEnabled() }
        verify(exactly = 0) { isWebViewMediaQuerySupported() }
    }

    @Test
    fun `returns original transformations when webview media query is supported`() {
        // Given
        val transformations = MessageBodyTransformations.MessageDetailsDefaults.copy(
            messageThemeOptions = MessageThemeOptions(
                currentTheme = MessageTheme.Dark,
                themeOverride = null,
                supportsDarkModeViaMediaQuery = true
            )
        )
        every { isDarkModeEnabled() } returns true
        every { isWebViewMediaQuerySupported() } returns true

        // When
        val result = applyWebViewDarkModeFallback(transformations)

        // Then
        assertEquals(transformations, result)
        verify(exactly = 1) { isDarkModeEnabled() }
        verify(exactly = 1) { isWebViewMediaQuerySupported() }
    }

    @Test
    fun `updates existing theme options when dark mode is enabled and media query is not supported`() {
        // Given
        val transformations = MessageBodyTransformations.MessageDetailsDefaults.copy(
            messageThemeOptions = MessageThemeOptions(
                currentTheme = MessageTheme.Dark,
                themeOverride = null,
                supportsDarkModeViaMediaQuery = true
            )
        )
        every { isDarkModeEnabled() } returns true
        every { isWebViewMediaQuerySupported() } returns false

        // When
        val result = applyWebViewDarkModeFallback(transformations)

        // Then
        assertEquals(
            transformations.copy(
                messageThemeOptions = transformations.messageThemeOptions?.copy(
                    supportsDarkModeViaMediaQuery = false
                )
            ),
            result
        )
        verify(exactly = 1) { isDarkModeEnabled() }
        verify(exactly = 1) { isWebViewMediaQuerySupported() }
    }

    @Test
    fun `creates dark theme options when none exist and fallback is needed`() {
        // Given
        val transformations = MessageBodyTransformations.MessageDetailsDefaults.copy(
            messageThemeOptions = null
        )
        every { isDarkModeEnabled() } returns true
        every { isWebViewMediaQuerySupported() } returns false

        // When
        val result = applyWebViewDarkModeFallback(transformations)

        // Then
        assertEquals(
            transformations.copy(
                messageThemeOptions = MessageThemeOptions(
                    currentTheme = MessageTheme.Dark,
                    themeOverride = null,
                    supportsDarkModeViaMediaQuery = false
                )
            ),
            result
        )
        verify(exactly = 1) { isDarkModeEnabled() }
        verify(exactly = 1) { isWebViewMediaQuerySupported() }
    }
}
