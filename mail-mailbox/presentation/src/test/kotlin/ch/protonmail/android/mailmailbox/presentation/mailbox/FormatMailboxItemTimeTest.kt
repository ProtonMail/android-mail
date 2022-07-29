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

package ch.protonmail.android.mailmailbox.presentation.mailbox

import java.util.Calendar
import java.util.Date
import java.util.Locale
import ch.protonmail.android.mailcommon.domain.usecase.GetDefaultCalendar
import ch.protonmail.android.mailcommon.domain.usecase.GetDefaultLocale
import ch.protonmail.android.mailmailbox.presentation.R
import ch.protonmail.android.mailmailbox.presentation.mailbox.usecase.FormatMailboxItemTime
import ch.protonmail.android.mailmailbox.presentation.mailbox.usecase.FormatMailboxItemTime.Result
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class FormatMailboxItemTimeTest {

    private val getDefaultCalendar = mockk<GetDefaultCalendar>()
    private val getDefaultLocale = mockk<GetDefaultLocale>()

    private val formatter = FormatMailboxItemTime(
        getDefaultCalendar,
        getDefaultLocale
    )

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `when the message is from the current day and locale is Italian show time of the message in 24 hours format`() {
        givenCurrentTimeAndLocale(1658853752.seconds, Locale.ITALIAN) // Tue Jul 26 18:42:35 CEST 2022
        val itemTime = 1658853643L // Tue Jul 26 18:40:44 CEST 2022

        val actual = formatter.invoke(itemTime.seconds)

        assertIs<Result.Localized>(actual, actual.toString())
        assertEquals(Result.Localized("18:40"), actual)
    }

    @Test
    fun `when the message is from the current day and locale is English show time of the message in 12 hours format`() {
        givenCurrentTimeAndLocale(1658853752.seconds, Locale.ENGLISH) // Tue Jul 26 18:42:35 CEST 2022
        val itemTime = 1658853643L // Tue Jul 26 18:40:44 CEST 2022

        val actual = formatter.invoke(itemTime.seconds)

        assertIs<Result.Localized>(actual, actual.toString())
        assertEquals(Result.Localized("6:40 PM"), actual)
    }

    @Test
    fun `when the message is from yesterday show localized 'yesterday' string`() {
        givenCurrentTimeAndLocale(1658853752.seconds, Locale.UK) // Tue Jul 26 18:42:35 CEST 2022
        val itemTime = 1658772437 // Mon Jul 25 20:07:17 CEST 2022

        val actual = formatter.invoke(itemTime.seconds)

        assertIs<Result.Localizable>(actual, actual.toString())
        assertEquals(Result.Localizable(R.string.yesterday), actual)
    }

    @Test
    fun `when the message is from the current week and older than yesterday show week day`() {
        givenCurrentTimeAndLocale(1658994137.seconds, Locale.UK) // Thu Jul 28 09:42:17 CEST 2022
        val itemTime = 1658772437 // Mon Jul 25 20:07:17 CEST 2022

        val actual = formatter.invoke(itemTime.seconds)

        assertIs<Result.Localized>(actual, actual.toString())
        assertEquals(Result.Localized("Monday"), actual)
    }

    @Test
    fun `when the message is from the current year and older than current week show day and month`() {
        givenCurrentTimeAndLocale(1658994137.seconds, Locale.FRENCH) // Thu Jul 28 09:42:17 CEST 2022
        val itemTime = 1647852004 // Mon Mar 21 09:40:04 CEST 2022

        val actual = formatter.invoke(itemTime.seconds)

        assertIs<Result.Localized>(actual, actual.toString())
        assertEquals(Result.Localized("21 mars"), actual)
    }

    @Test
    fun `when showing day and month ensure they are formatted based on the current locale`() {
        givenCurrentTimeAndLocale(1658994137.seconds, Locale.US) // Thu Jul 28 09:42:17 CEST 2022
        val itemTime = 1647852004 // Mon Mar 21 09:40:04 CEST 2022

        val actual = formatter.invoke(itemTime.seconds)

        assertIs<Result.Localized>(actual, actual.toString())
        assertEquals(Result.Localized("Mar 21"), actual)
    }

    @Test
    fun `when the message is from before the current year show the day month and year`() {
        givenCurrentTimeAndLocale(1658994137.seconds, Locale.UK) // Thu Jul 28 09:42:17 CEST 2022
        val itemTime = 1631518804 // Mon Sep 13 09:40:04 CEST 2022

        val actual = formatter.invoke(itemTime.seconds)

        assertIs<Result.Localized>(actual, actual.toString())
        assertEquals(Result.Localized("13 Sep 2021"), actual)
    }

    @Test
    fun `when showing day month and year ensure they are formatted based on current locale`() {
        givenCurrentTimeAndLocale(1658994137.seconds, Locale.US) // Thu Jul 28 09:42:17 CEST 2022
        val itemTime = 1631518804 // Mon Sep 13 09:40:04 CEST 2022

        val actual = formatter.invoke(itemTime.seconds)

        assertIs<Result.Localized>(actual, actual.toString())
        assertEquals(Result.Localized("Sep 13, 2021"), actual)
    }

    @Test
    fun `all instances of calendar are created considering the current locale`() {
        mockkStatic(Calendar::class)
        givenCurrentTimeAndLocale(1658994137.seconds, Locale.TAIWAN) // Thu Jul 28 09:42:17 CEST 2022
        val itemTime = 1631518804 // Mon Sep 13 09:40:04 CEST 2022

        formatter.invoke(itemTime.seconds)

        val slot = mutableListOf<Locale>()
        verify { Calendar.getInstance(capture(slot)) }
        assertTrue(slot.isNotEmpty())
        assertTrue(slot.all { it == Locale.TAIWAN })
    }

    private fun givenCurrentTimeAndLocale(currentTime: Duration, locale: Locale) {
        val calendar = Calendar.getInstance(locale)
        calendar.time = Date(currentTime.inWholeMilliseconds)

        every { getDefaultCalendar() } returns calendar
        every { getDefaultLocale() } returns locale
    }
}
