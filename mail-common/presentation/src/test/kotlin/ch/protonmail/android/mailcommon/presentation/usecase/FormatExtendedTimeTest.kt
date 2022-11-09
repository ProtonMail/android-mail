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

package ch.protonmail.android.mailcommon.presentation.usecase

import java.util.Locale
import java.util.TimeZone
import ch.protonmail.android.mailcommon.domain.usecase.GetAppLocale
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Before
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

class FormatExtendedTimeTest {

    private val getAppLocale: GetAppLocale = mockk()

    private val formatExtendedTime = FormatExtendedTime(getAppLocale)

    @Before
    fun setUp() {
        mockkStatic(TimeZone::class)
        every { TimeZone.getDefault() } returns TimeZone.getTimeZone("Europe/Zurich")
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `format the date and time according to the app locale`() {
        // Given
        val expectedResult = TextUiModel.Text("08/11/2022, 17:16")
        every { getAppLocale() } returns Locale.UK
        // When
        val result = formatExtendedTime(1_667_924_198.seconds)
        // Then
        assertEquals(expectedResult, result)
    }
}
