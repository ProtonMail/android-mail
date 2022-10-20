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

package ch.protonmail.android.mailcommon.domain.usecase

import java.util.Calendar
import java.util.Locale
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Test
import kotlin.test.assertEquals

class GetLocalisedCalendarTest {

    private val getAppLocale = mockk<GetAppLocale> {
        every { this@mockk.invoke() } returns Locale.CHINA
    }

    private val getLocalisedCalendar = GetLocalisedCalendar(getAppLocale)

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `gets calendar instance based on current locale`() {
        // Given
        mockkStatic(Calendar::class)
        // When
        getLocalisedCalendar()
        // Then
        val slot = slot<Locale>()
        verify { Calendar.getInstance(capture(slot)) }
        assertEquals(Locale.CHINA, slot.captured)
    }
}
