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

import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import ch.protonmail.android.mailcommon.domain.usecase.GetAppLocale
import ch.protonmail.android.mailcommon.domain.usecase.GetLocalisedCalendar
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailmailbox.presentation.R
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class FormatShortTimeTest {

    private val getLocalisedCalendar = mockk<GetLocalisedCalendar>()
    private val getAppLocale = mockk<GetAppLocale>()

    private val formatter = FormatShortTime(
        getLocalisedCalendar,
        getAppLocale
    )

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
    fun `when the message is from the current day and locale is Italian show time of the message in 24 hours format`() {
        // Given
        givenCurrentTimeAndLocale(1_658_853_752.seconds, Locale.ITALIAN) // Tue Jul 26 18:42:35 CEST 2022
        val itemTime = 1_658_853_643L // Tue Jul 26 18:40:44 CEST 2022
        // When
        val actual = formatter.invoke(itemTime.seconds)
        // Then
        assertIs<TextUiModel.Text>(actual, actual.toString())
        assertEquals(TextUiModel.Text("18:40"), actual)
    }

    @Test
    fun `when the message is from the current day and locale is English show time of the message in 12 hours format`() {
        // Given
        givenCurrentTimeAndLocale(1_658_853_752.seconds, Locale.ENGLISH) // Tue Jul 26 18:42:35 CEST 2022
        val itemTime = 1_658_853_643L // Tue Jul 26 18:40:44 CEST 2022
        // When
        val actual = formatter.invoke(itemTime.seconds)
        // Then
        assertIs<TextUiModel.Text>(actual, actual.toString())
        assertEquals(TextUiModel.Text("6:40 PM"), actual)
    }

    @Test
    fun `when the message is from yesterday show localized 'yesterday' string`() {
        // Given
        givenCurrentTimeAndLocale(1_658_853_752.seconds, Locale.UK) // Tue Jul 26 18:42:35 CEST 2022
        val itemTime = 1_658_772_437 // Mon Jul 25 20:07:17 CEST 2022
        // When
        val actual = formatter.invoke(itemTime.seconds)
        // Then
        assertIs<TextUiModel.TextRes>(actual, actual.toString())
        assertEquals(TextUiModel.TextRes(R.string.yesterday), actual)
    }

    @Test
    fun `when the message is from the current week and older than yesterday show week day`() {
        // Given
        givenCurrentTimeAndLocale(1_658_994_137.seconds, Locale.UK) // Thu Jul 28 09:42:17 CEST 2022
        val itemTime = 1_658_772_437 // Mon Jul 25 20:07:17 CEST 2022
        // When
        val actual = formatter.invoke(itemTime.seconds)
        // Then
        assertIs<TextUiModel.Text>(actual, actual.toString())
        assertEquals(TextUiModel.Text("Monday"), actual)
    }

    @Test
    fun `when the message is of a previous year and same week-of-year as current week show full date`() {
        // Given
        givenCurrentTimeAndLocale(1_709_557_304.seconds, Locale.UK) // Mon Mar 04 14:01:44 CET 2024 (week 10)
        val itemTime = 1_678_107_704 // Mon Mar 06 14:01:44 CET 2023 (week 10)
        // When
        val actual = formatter.invoke(itemTime.seconds)
        // Then
        assertIs<TextUiModel.Text>(actual, actual.toString())
        assertEquals(TextUiModel.Text("6 Mar 2023"), actual)
    }

    @Test
    fun `when the year changed and message is from the current week and older than yesterday show week day`() {
        // Given
        givenCurrentTimeAndLocale(1_640_995_200.seconds, Locale.UK) // Sat Jan 01 2022 01:00:00 CEST
        val itemTime = 1_640_760_408 // Wed Dec 29 2021 07:46:48 CEST
        // When
        val actual = formatter.invoke(itemTime.seconds)
        // Then
        assertIs<TextUiModel.Text>(actual, actual.toString())
        assertEquals(TextUiModel.Text("Wednesday"), actual)
    }

    @Test
    fun `when the message is from the current year and older than current week show day and month`() {
        // Given
        givenCurrentTimeAndLocale(1_658_994_137.seconds, Locale.FRENCH) // Thu Jul 28 09:42:17 CEST 2022
        val itemTime = 1_647_852_004 // Mon Mar 21 09:40:04 CEST 2022
        // When
        val actual = formatter.invoke(itemTime.seconds)
        // Then
        assertIs<TextUiModel.Text>(actual, actual.toString())
        assertEquals(TextUiModel.Text("21 mars 2022"), actual)
    }

    @Test
    fun `when showing day and month ensure they are formatted based on the current locale`() {
        // Given
        givenCurrentTimeAndLocale(1_658_994_137.seconds, Locale.US) // Thu Jul 28 09:42:17 CEST 2022
        val itemTime = 1_647_852_004 // Mon Mar 21 09:40:04 CEST 2022
        // When
        val actual = formatter.invoke(itemTime.seconds)
        // Then
        assertIs<TextUiModel.Text>(actual, actual.toString())
        assertEquals(TextUiModel.Text("Mar 21, 2022"), actual)
    }

    @Test
    fun `when the message is from before the current year show the day month and year`() {
        // Given
        givenCurrentTimeAndLocale(1_658_994_137.seconds, Locale.UK) // Thu Jul 28 09:42:17 CEST 2022
        val itemTime = 1_634_119_200 // Wed Oct 13 12:00:00 CEST 2021
        // When
        val actual = formatter.invoke(itemTime.seconds)
        // Then
        assertIs<TextUiModel.Text>(actual, actual.toString())
        assertEquals(TextUiModel.Text("13 Oct 2021"), actual)
    }

    @Test
    fun `when the message is from Dec 31st and today is Jan 1st show yesterday`() {
        // Given
        givenCurrentTimeAndLocale(1_640_995_200.seconds, Locale.UK) // Sat Jan 01 2022 01:00:00 CEST
        val itemTime = 1_640_989_800 // Fri Dec 31 2021 23:30:00 CEST
        // When
        val actual = formatter.invoke(itemTime.seconds)
        // Then
        assertIs<TextUiModel.TextRes>(actual, actual.toString())
        assertEquals(TextUiModel.TextRes(R.string.yesterday), actual)
    }

    @Test
    fun `when showing day month and year ensure they are formatted based on current locale`() {
        // Given
        givenCurrentTimeAndLocale(1_658_994_137.seconds, Locale.US) // Thu Jul 28 09:42:17 CEST 2022
        val itemTime = 1_631_518_804 // Mon Sep 13 09:40:04 CEST 2021
        // When
        val actual = formatter.invoke(itemTime.seconds)
        // Then
        assertIs<TextUiModel.Text>(actual, actual.toString())
        assertEquals(TextUiModel.Text("Sep 13, 2021"), actual)
    }

    @Test
    fun `all instances of calendar are created considering the current locale`() {
        // Given
        mockkStatic(Calendar::class)
        givenCurrentTimeAndLocale(1_658_994_137.seconds, Locale.TAIWAN) // Thu Jul 28 09:42:17 CEST 2022
        val itemTime = 1_631_518_804 // Mon Sep 13 09:40:04 CEST 2022
        // When
        formatter.invoke(itemTime.seconds)
        // Then
        val slot = mutableListOf<Locale>()
        verify { Calendar.getInstance(capture(slot)) }
        assertTrue(slot.isNotEmpty())
        assertTrue(slot.all { it == Locale.TAIWAN })
    }

    private fun givenCurrentTimeAndLocale(currentTime: Duration, locale: Locale) {
        val calendar = Calendar.getInstance(locale)
        calendar.time = Date(currentTime.inWholeMilliseconds)

        every { getLocalisedCalendar() } returns calendar
        every { getAppLocale() } returns locale
    }
}
